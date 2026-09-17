package de.msjones.android.alarmapp.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.data.AlarmMessageParser
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus
import de.msjones.android.alarmapp.util.ConnectionPhase
import de.msjones.android.alarmapp.util.ConnectionStatusTexts
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground-Service, der MQTT-Verbindungen unabhängig voneinander hält und Alarmnachrichten empfängt.
 */
class MessagingService : LifecycleService() {

    private lateinit var helper: NotificationHelper
    private lateinit var settingsStore: SettingsStore
    private val clientWrappers = ConcurrentHashMap<String, MqttClientWrapper>()
    private val connectionJobs = ConcurrentHashMap<String, Job>()
    private val connectionStatuses = ConcurrentHashMap<String, String>()
    private val connectionMessages = ConcurrentHashMap<String, String>()

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_TOPIC = "topic"
        const val EXTRA_CONNECTION_ID = "connection_id"
        const val EXTRA_SSL = "ssl"
        const val EXTRA_ACTION = "action"
        const val ACTION_CONNECT = "connect"
        const val ACTION_DISCONNECT = "disconnect"

        private val running = AtomicBoolean(false)

        /** Gibt an, ob der Messaging-Service derzeit aktiv ist. */
        fun isRunning(): Boolean = running.get()
    }

    override fun onCreate() {
        super.onCreate()
        helper = NotificationHelper(this)
        settingsStore = SettingsStore.getInstance(this)
        running.set(true)
        MessagingEventBus.tryEmit(MessagingEvent.ServiceRunningState(true))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            helper.buildServiceNotification(buildServiceStatusMessage())
        )

        val action = intent?.getStringExtra(EXTRA_ACTION) ?: ACTION_CONNECT
        val connectionId = intent?.getStringExtra(EXTRA_CONNECTION_ID) ?: "unknown"

        if (action == ACTION_DISCONNECT) {
            disconnectConnection(connectionId, emitDisconnected = true)
            return START_STICKY
        }

        val host = intent?.getStringExtra(EXTRA_HOST)
        val port = intent?.getIntExtra(EXTRA_PORT, 1883) ?: 1883
        val username = intent?.getStringExtra(EXTRA_USERNAME) ?: ""
        val password = intent?.getStringExtra(EXTRA_PASSWORD) ?: ""
        val topic = intent?.getStringExtra(EXTRA_TOPIC) ?: "JF/Alarm/KB"
        val ssl = intent?.getBooleanExtra(EXTRA_SSL, false) ?: false

        connectionJobs.remove(connectionId)?.cancel()
        val connectingHost = if (host.isNullOrBlank()) "" else "$host:$port"
        rememberStatus(
            connectionId,
            "CONNECTING",
            ConnectionStatusTexts.waitingMessage(
                ConnectionPhase.CONNECTING,
                connectingHost,
                MqttClientWrapper.CONNECT_TIMEOUT_SECONDS
            )
        )
        helper.updateServiceNotification(buildServiceStatusMessage())
        MessagingEventBus.tryEmit(
            MessagingEvent.ConnectionState(
                "CONNECTING",
                ConnectionStatusTexts.waitingMessage(
                    ConnectionPhase.CONNECTING,
                    connectingHost,
                    MqttClientWrapper.CONNECT_TIMEOUT_SECONDS
                ),
                connectionId
            )
        )
        connectionJobs[connectionId] = lifecycleScope.launch(Dispatchers.IO) {
            if (host.isNullOrBlank()) {
                helper.updateServiceNotification("Bitte Serverdaten speichern.")
                return@launch
            }

            val protocol = if (ssl) "ssl" else "tcp"
            val serverUri = "${protocol}://${host}:${port}"

            clientWrappers.remove(connectionId)?.disconnectAndWait(emitState = false)

            clientWrappers[connectionId] = MqttClientWrapper(
                context = this@MessagingService,
                lifecycleOwner = this@MessagingService,
                serverUri = serverUri,
                clientId = "AndroidClient-${connectionId}-${System.nanoTime()}",
                user = username,
                pass = password,
                topic = topic,
                onMessage = { msg ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        handleIncomingMessage(msg)
                    }
                },
                onState = { state ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val (status, message) = if (state.contains(":")) {
                            val parts = state.split(":", limit = 2)
                            parts[0] to parts[1]
                        } else {
                            "INFO" to state
                        }

                        rememberStatus(connectionId, status, message)
                        when (status.uppercase()) {
                            "SUBSCRIBED" -> helper.cancelStatusNotification(connectionId)
                            "OFFLINE", "DISCONNECTED" ->
                                helper.cancelStatusNotification(connectionId)
                            "ERROR" -> {
                                settingsStore.setConnectionEnabled(connectionId, false)
                                MessagingEventBus.tryEmit(
                                    MessagingEvent.ConnectionState(status, message, connectionId)
                                )
                                helper.updateServiceNotification(buildServiceStatusMessage())
                                disconnectConnection(connectionId, emitDisconnected = false)
                                return@launch
                            }
                        }

                        helper.updateServiceNotification(buildServiceStatusMessage())
                        MessagingEventBus.tryEmit(
                            MessagingEvent.ConnectionState(status, message, connectionId)
                        )
                    }
                },
                onAuthError = { errorMessage ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val detailedError = "Verbindung $host:$port - $errorMessage"
                        settingsStore.setConnectionEnabled(connectionId, false)
                        rememberStatus(connectionId, "ERROR", detailedError)
                        helper.updateServiceNotification(buildServiceStatusMessage())
                        MessagingEventBus.tryEmit(
                            MessagingEvent.AuthError(detailedError, connectionId)
                        )
                        disconnectConnection(connectionId, emitDisconnected = true)
                    }
                }
            )

            clientWrappers[connectionId]?.connect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        connectionJobs.values.forEach { it.cancel() }
        connectionJobs.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            clientWrappers.values.forEach { it.disconnectAndWait(emitState = false) }
            clientWrappers.clear()
        }
        running.set(false)
        settingsStore.clearAllRuntimeStatuses()
        MessagingEventBus.tryEmit(MessagingEvent.ServiceRunningState(false))
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    /**
     * Trennt genau eine MQTT-Verbindung und beendet den Dienst, wenn keine mehr übrig ist.
     *
     * @param connectionId Kennung der zu trennenden Verbindung
     * @param emitDisconnected ob ein DISCONNECTED-Ereignis für die UI gesendet werden soll
     */
    private fun disconnectConnection(connectionId: String, emitDisconnected: Boolean) {
        connectionJobs.remove(connectionId)?.cancel()
        rememberStatus(connectionId, "OFFLINE", "Offline")
        helper.cancelStatusNotification(connectionId)
        lifecycleScope.launch(Dispatchers.IO) {
            clientWrappers.remove(connectionId)?.disconnectAndWait(emitState = false)
            if (emitDisconnected) {
                MessagingEventBus.tryEmit(
                    MessagingEvent.ConnectionState("OFFLINE", "Offline", connectionId)
                )
            }
            if (clientWrappers.isEmpty()) {
                stopSelf()
            } else {
                helper.updateServiceNotification(buildServiceStatusMessage())
            }
        }
    }

    /**
     * Baut den Notification-Text anhand der Phasen aller gespeicherten Verbindungen.
     *
     * @return Statuszeile für die Vordergrund-Benachrichtigung
     */
    private fun buildServiceStatusMessage(): String {
        val connections = settingsStore.getConnectionsSnapshot()
        return ConnectionStatusTexts.displaySummary(
            connections,
            connectionStatuses,
            connectionMessages
        )
    }

    /**
     * Merkt sich Statuscode und Anzeigetext einer Verbindung.
     *
     * @param connectionId Kennung der Verbindung
     * @param status Statuscode
     * @param message Anzeigetext oder leer
     */
    private fun rememberStatus(connectionId: String, status: String, message: String = "") {
        connectionStatuses[connectionId] = status
        if (message.isNotBlank()) {
            connectionMessages[connectionId] = message
        } else {
            connectionMessages.remove(connectionId)
        }
        settingsStore.setRuntimeStatus(connectionId, status, message)
    }

    /**
     * Verarbeitet eine eingehende MQTT-Nachricht und benachrichtigt UI sowie Notification.
     *
     * @param msg Rohpayload vom Broker
     */
    private fun handleIncomingMessage(msg: String) {
        val parsed = AlarmMessageParser.parse(msg)
        helper.showIncomingMessage(parsed.keyword, parsed.location, parsed.extras)
        MessagingEventBus.tryEmit(
            MessagingEvent.NewMessage(parsed.keyword, parsed.location, parsed.extras)
        )
    }
}
