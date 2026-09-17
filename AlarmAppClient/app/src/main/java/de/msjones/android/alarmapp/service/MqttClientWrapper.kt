package de.msjones.android.alarmapp.service

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.msjones.android.alarmapp.util.ConnectionAuthDetector
import de.msjones.android.alarmapp.util.ConnectionPhase
import de.msjones.android.alarmapp.util.ConnectionStatusTexts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
    private val initialFailed = AtomicBoolean(false)
    private val displayHost = serverUri.substringAfter("://")
    private var waitTicker: Job? = null

    /**
     * Stellt die MQTT-Verbindung einmalig her und abonniert das konfigurierte Topic.
     * Schlägt der Erstversuch fehl, wird kein automatischer Retry gestartet.
     */
    suspend fun connect() {
        stopped.set(false)
        hasBeenConnected.set(false)
        initialFailed.set(false)
        createClient()
        startWaitTicker(ConnectionPhase.CONNECTING, CONNECT_TIMEOUT_SECONDS)
        try {
            if (!attemptConnect()) {
                failInitialConnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (stopped.get()) {
                return
            }
            if (ConnectionAuthDetector.isAuthFailure(e.message)) {
                stopWaitTicker()
                onAuthError?.invoke("Falscher Benutzername oder Passwort")
                return
            }
            failInitialConnect()
        }
    }

    /**
     * Baut den MQTT-Client inkl. Connect- und Disconnect-Listener.
     * Der Reconnect wartet zuerst [RECONNECT_INITIAL_DELAY_SECONDS] und höchstens
     * [RECONNECT_MAX_DELAY_SECONDS] Sekunden zwischen den Versuchen.
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
            .simpleAuth()
            .username(user)
            .password(pass.toByteArray())
            .applySimpleAuth()
            .automaticReconnect()
            .initialDelay(RECONNECT_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
            .maxDelay(RECONNECT_MAX_DELAY_SECONDS, TimeUnit.SECONDS)
            .applyAutomaticReconnect()
            .addConnectedListener { _ ->
                if (stopped.get()) {
                    return@addConnectedListener
                }
                isConnected.set(true)
                hasBeenConnected.set(true)
                stopWaitTicker()
                lifecycleOwner.lifecycleScope.launch {
                    subscribe(topic)
                }
            }
            .addDisconnectedListener { disconnectContext ->
                isConnected.set(false)
                if (stopped.get() || disconnectContext.source == MqttDisconnectSource.USER) {
                    disconnectContext.reconnector.reconnect(false)
                    return@addDisconnectedListener
                }
                if (!hasBeenConnected.get()) {
                    disconnectContext.reconnector.reconnect(false)
                    return@addDisconnectedListener
                }
                disconnectContext.reconnector.reconnect(true)
                val waitSeconds = reconnectWaitSeconds(disconnectContext.reconnector.getDelay(TimeUnit.SECONDS))
                startWaitTicker(ConnectionPhase.RECONNECTING, waitSeconds)
            }
            .transportConfig()
            .mqttConnectTimeout(MQTT_CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .socketConnectTimeout(SOCKET_CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
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
        val connAck: Mqtt3ConnAck = withTimeout(CONNECT_TIMEOUT_MS) {
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
            if (ConnectionAuthDetector.isAuthFailure(connAck.returnCode.toString()) ||
                ConnectionAuthDetector.isAuthFailure(errorMessage)
            ) {
                stopWaitTicker()
                onAuthError?.invoke("Falscher Benutzername oder Passwort")
                stopped.set(true)
                return false
            }
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
            if (hasBeenConnected.get()) {
                startWaitTicker(ConnectionPhase.RECONNECTING, DEFAULT_RECONNECT_WAIT_SECONDS)
            } else {
                failInitialConnect()
            }
        }
    }

    /**
     * Verhindert Reconnect-Meldungen und trennt die Verbindung.
     *
     * @param emitState ob Statusänderungen an [onState] gemeldet werden sollen
     */
    suspend fun disconnectAndWait(emitState: Boolean = true) {
        stopped.set(true)
        stopWaitTicker()
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
     * Beendet den Erstversuch mit einer klaren Fehlermeldung ohne Reconnect.
     */
    private fun failInitialConnect() {
        if (!initialFailed.compareAndSet(false, true)) {
            return
        }
        stopWaitTicker()
        stopped.set(true)
        onState("ERROR:${ConnectionStatusTexts.noConnectionPossible(displayHost)}")
    }

    /**
     * Startet die sekundengenaue Warteanzeige für Verbinden oder Reconnect.
     *
     * @param phase aktuelle Phase
     * @param totalSeconds Gesamtwartezeit
     */
    private fun startWaitTicker(phase: ConnectionPhase, totalSeconds: Int) {
        stopWaitTicker()
        val seconds = totalSeconds.coerceAtLeast(1)
        waitTicker = lifecycleOwner.lifecycleScope.launch {
            for (remaining in seconds downTo 1) {
                if (stopped.get()) {
                    return@launch
                }
                emitWaiting(phase, remaining)
                delay(WAIT_TICK_MS)
            }
        }
    }

    /**
     * Stoppt die Warteanzeige.
     */
    private fun stopWaitTicker() {
        waitTicker?.cancel()
        waitTicker = null
    }

    /**
     * Meldet eine Phase inkl. lesbarem Text an [onState].
     *
     * @param phase neue Verbindungsphase
     */
    private fun emitPhase(phase: ConnectionPhase) {
        val status = statusCode(phase)
        onState("$status:${ConnectionStatusTexts.phaseMessage(phase, displayHost)}")
    }

    /**
     * Meldet eine Phase mit Restwartezeit an [onState].
     *
     * @param phase aktuelle Phase
     * @param remainingSeconds noch zu wartende Sekunden
     */
    private fun emitWaiting(phase: ConnectionPhase, remainingSeconds: Int) {
        val status = statusCode(phase)
        onState(
            "$status:${ConnectionStatusTexts.waitingMessage(phase, displayHost, remainingSeconds)}"
        )
    }

    /**
     * Liefert den MQTT-Statuscode zu einer Phase.
     *
     * @param phase Verbindungsphase
     * @return Statuscode für Events und UI
     */
    private fun statusCode(phase: ConnectionPhase): String {
        return when (phase) {
            ConnectionPhase.OFFLINE -> "OFFLINE"
            ConnectionPhase.CONNECTING -> "CONNECTING"
            ConnectionPhase.ACTIVE -> "SUBSCRIBED"
            ConnectionPhase.RECONNECTING -> "RECONNECTING"
        }
    }

    /**
     * Begrenzt die Reconnect-Warteanzeige auf die konfigurierte Maximalpause.
     *
     * @param delaySeconds Delay aus dem HiveMQ-Reconnector
     * @return Anzeigezeit in Sekunden
     */
    private fun reconnectWaitSeconds(delaySeconds: Long): Int {
        return delaySeconds.toInt().coerceIn(
            RECONNECT_INITIAL_DELAY_SECONDS.toInt(),
            RECONNECT_MAX_DELAY_SECONDS.toInt()
        )
    }

    companion object {
        const val CONNECT_TIMEOUT_SECONDS = 20
        const val SOCKET_CONNECT_TIMEOUT_SECONDS = 10
        const val MQTT_CONNECT_TIMEOUT_SECONDS = 15
        /** Erste Pause vor einem Reconnect in Sekunden. */
        const val RECONNECT_INITIAL_DELAY_SECONDS = 1L
        /** Maximale Pause zwischen Reconnect-Versuchen in Sekunden. */
        const val RECONNECT_MAX_DELAY_SECONDS = 30L
        private const val CONNECT_TIMEOUT_MS = CONNECT_TIMEOUT_SECONDS * 1000L
        private const val WAIT_TICK_MS = 1_000L
        private const val DEFAULT_RECONNECT_WAIT_SECONDS = 1
    }
}
