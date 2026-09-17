package de.msjones.android.alarmapp.service

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.msjones.android.alarmapp.util.ConnectionPhase
import de.msjones.android.alarmapp.util.ConnectionStatusTexts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Kapselt eine MQTT-3-Verbindung inkl. Subscribe, Auto-Reconnect und Status-Callbacks.
 */
class MqttClientWrapper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val serverUri: String,
    private val clientId: String,
    private val user: String,
    private val pass: String,
    private val topic: String,
    private val onMessage: (String) -> Unit,
    private val onState: (String) -> Unit,
    private val onAuthError: ((String) -> Unit)? = null
) {

    private var client: Mqtt3AsyncClient? = null
    private val isConnected = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private val hasBeenConnected = AtomicBoolean(false)
    private val displayHost = serverUri.substringAfter("://")

    /**
     * Stellt die MQTT-Verbindung her und abonniert das konfigurierte Topic.
     * Schlägt der erste Versuch fehl, bleibt der Zustand „Verbinden“ und es wird erneut versucht.
     */
    suspend fun connect() {
        stopped.set(false)
        hasBeenConnected.set(false)
        emitPhase(ConnectionPhase.CONNECTING)
        createClient()

        var delayMs = INITIAL_RETRY_DELAY_MS
        while (!stopped.get() && coroutineContext.isActive) {
            try {
                if (attemptConnect()) {
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (stopped.get()) {
                    return
                }
                if (isAuthFailure(e.message)) {
                    onAuthError?.invoke("Falscher Benutzername oder Passwort")
                    return
                }
                emitConnectingWithHint(e)
            }
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }
    }

    /**
     * Baut den MQTT-Client inkl. Connect- und Disconnect-Listener.
     */
    private fun createClient() {
        val protocol = if (serverUri.startsWith("ssl://")) "ssl" else "tcp"
        val host = serverUri.substringAfter("${protocol}://").substringBefore(":")
        val port = serverUri.substringAfterLast(":").toInt()

        val builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(host)
            .serverPort(port)
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener { _ ->
                if (stopped.get()) {
                    return@addConnectedListener
                }
                isConnected.set(true)
                hasBeenConnected.set(true)
                lifecycleOwner.lifecycleScope.launch {
                    subscribe(topic)
                }
            }
            .addDisconnectedListener { disconnectContext ->
                isConnected.set(false)
                if (stopped.get() || disconnectContext.source == MqttDisconnectSource.USER) {
                    return@addDisconnectedListener
                }
                if (isAuthFailure(disconnectContext.cause.message)) {
                    disconnectContext.reconnector.reconnect(false)
                    onAuthError?.invoke("Falscher Benutzername oder Passwort")
                    return@addDisconnectedListener
                }
                val phase = if (hasBeenConnected.get()) {
                    ConnectionPhase.RECONNECTING
                } else {
                    ConnectionPhase.CONNECTING
                }
                emitPhase(phase)
            }
            .transportConfig()
            .mqttConnectTimeout(15, TimeUnit.SECONDS)
            .socketConnectTimeout(10, TimeUnit.SECONDS)
            .applyTransportConfig()

        if (protocol == "ssl") {
            builder.sslWithDefaultConfig()
        }

        client = builder.buildAsync()
    }

    /**
     * Führt einen einzelnen Connect-Versuch aus.
     *
     * @return true bei erfolgreichem ConnAck, sonst false
     */
    private suspend fun attemptConnect(): Boolean {
        val connAck: Mqtt3ConnAck = withTimeout(20_000) {
            client?.connectWith()
                ?.simpleAuth()
                ?.username(user)
                ?.password(pass.toByteArray())
                ?.applySimpleAuth()
                ?.keepAlive(45)
                ?.send()
                ?.await() ?: throw Exception("Verbindung fehlgeschlagen")
        }

        if (connAck.returnCode.isError) {
            val errorMessage = "Verbindung abgelehnt: ${connAck.returnCode}"
            if (isAuthFailure(connAck.returnCode.toString()) || isAuthFailure(errorMessage)) {
                onAuthError?.invoke("Falscher Benutzername oder Passwort")
                stopped.set(true)
                return false
            }
            emitConnectingWithHint(Exception(errorMessage))
            return false
        }
        return true
    }

    /**
     * Abonniert ein Topic und leitet eingehende Payloads an [onMessage] weiter.
     *
     * @param topic MQTT-Topic-Filter
     */
    private suspend fun subscribe(topic: String) {
        if (stopped.get()) {
            return
        }
        try {
            client?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                ?.callback { publish: Mqtt3Publish ->
                    val payloadBytes = publish.payload
                        .map { buffer ->
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            bytes
                        }
                        .orElse(ByteArray(0))

                    val message = String(payloadBytes, StandardCharsets.UTF_8)

                    lifecycleOwner.lifecycleScope.launch {
                        onMessage(message)
                    }
                }
                ?.send()
                ?.await()
            emitPhase(ConnectionPhase.ACTIVE)
        } catch (_: Exception) {
            if (stopped.get()) {
                return
            }
            val phase = if (hasBeenConnected.get()) {
                ConnectionPhase.RECONNECTING
            } else {
                ConnectionPhase.CONNECTING
            }
            emitPhase(phase)
        }
    }

    /**
     * Verhindert Reconnect-Meldungen und trennt die Verbindung.
     *
     * @param emitState ob Statusänderungen an [onState] gemeldet werden sollen
     */
    suspend fun disconnectAndWait(emitState: Boolean = true) {
        stopped.set(true)
        try {
            client?.disconnect()?.await()
            isConnected.set(false)
            if (emitState) {
                emitPhase(ConnectionPhase.OFFLINE)
            }
        } catch (_: Exception) {
            if (emitState) {
                emitPhase(ConnectionPhase.OFFLINE)
            }
        }
    }

    /** Gibt an, ob der Client aktuell verbunden ist. */
    fun isConnected(): Boolean = isConnected.get()

    /**
     * Meldet eine Phase inkl. lesbarem Text an [onState].
     *
     * @param phase neue Verbindungsphase
     */
    private fun emitPhase(phase: ConnectionPhase) {
        val status = when (phase) {
            ConnectionPhase.OFFLINE -> "OFFLINE"
            ConnectionPhase.CONNECTING -> "CONNECTING"
            ConnectionPhase.ACTIVE -> "SUBSCRIBED"
            ConnectionPhase.RECONNECTING -> "RECONNECTING"
        }
        onState("$status:${ConnectionStatusTexts.phaseMessage(phase, displayHost)}")
    }

    /**
     * Meldet „Verbinden“ mit einem kurzen Fehlerhinweis.
     *
     * @param error aufgetretener Verbindungsfehler
     */
    private fun emitConnectingWithHint(error: Exception) {
        val hint = connectingHint(error)
        val base = ConnectionStatusTexts.phaseMessage(ConnectionPhase.CONNECTING, displayHost)
        onState("CONNECTING:$base – $hint")
    }

    /**
     * Kürzt einen Verbindungsfehler auf einen Notification-Hinweis.
     *
     * @param error aufgetretener Fehler
     * @return kurzer Hinweistext
     */
    private fun connectingHint(error: Exception): String {
        val message = error.message ?: "Unbekannter Fehler"
        val msgLower = message.lowercase()
        val isUnknownHost = error.javaClass.name.contains("UnknownHostException") ||
            msgLower.contains("unknown host") ||
            msgLower.contains("no address associated")
        return when {
            isUnknownHost -> "nicht erreichbar"
            msgLower.contains("connection refused") || msgLower.contains("connectexception") ->
                "nicht erreichbar"
            msgLower.contains("timeout") || error is kotlinx.coroutines.TimeoutCancellationException ->
                "Zeitüberschreitung"
            else -> message
        }
    }

    /**
     * Prüft, ob eine Meldung auf einen Authentifizierungsfehler hindeutet.
     *
     * @param message Fehlertext oder null
     * @return true bei Auth-Fehler
     */
    private fun isAuthFailure(message: String?): Boolean {
        val msgLower = message.orEmpty().lowercase()
        return msgLower.contains("not authorized") ||
            msgLower.contains("authentication failed") ||
            msgLower.contains("bad username") ||
            msgLower.contains("bad user name") ||
            msgLower.contains("bad user") ||
            msgLower.contains("identifier rejected") ||
            msgLower.contains("bad credentials") ||
            msgLower.contains("not_authorized") ||
            msgLower.contains("bad_user") ||
            (msgLower.contains("bad") && msgLower.contains("auth"))
    }

    companion object {
        private const val INITIAL_RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }
}
