package de.msjones.android.alarmapp.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.data.AlarmMessageParser
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground-Service, der MQTT-Verbindungen hält und Alarmnachrichten empfängt.
 */
class MessagingService : LifecycleService() {

    private lateinit var helper: NotificationHelper
    private lateinit var settingsStore: SettingsStore
    private val clientWrappers = mutableMapOf<String, MqttClientWrapper>()
    private var job: Job? = null

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_TOPIC = "topic"
        const val EXTRA_CONNECTION_ID = "connection_id"
        const val EXTRA_SSL = "ssl"

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
            helper.buildServiceNotification("Service startet …")
        )

        val host = intent?.getStringExtra(EXTRA_HOST)
        val port = intent?.getIntExtra(EXTRA_PORT, 1883) ?: 1883
        val username = intent?.getStringExtra(EXTRA_USERNAME) ?: ""
        val password = intent?.getStringExtra(EXTRA_PASSWORD) ?: ""
        val topic = intent?.getStringExtra(EXTRA_TOPIC) ?: "JF/Alarm/KB"
        val connectionId = intent?.getStringExtra(EXTRA_CONNECTION_ID) ?: "unknown"
        val ssl = intent?.getBooleanExtra(EXTRA_SSL, false) ?: false

        job = lifecycleScope.launch(Dispatchers.IO) {
            if (host.isNullOrBlank()) {
                helper.updateServiceNotification("Bitte Serverdaten speichern.")
                return@launch
            }

            val protocol = if (ssl) "ssl" else "tcp"
            val serverUri = "${protocol}://${host}:${port}"

            clientWrappers[connectionId]?.disconnectAndWait()

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

                        when (status.uppercase()) {
                            "CONNECTED" -> settingsStore.setConnected(message)
                            "DISCONNECTED" -> settingsStore.setDisconnected(message)
                            "ERROR" -> {
                                settingsStore.setConnectionError(message)
                                MessagingEventBus.tryEmit(MessagingEvent.StopAllConnections)
                            }
                            else -> settingsStore.setConnectionStatus(status, message)
                        }

                        helper.updateServiceNotification(message)
                        MessagingEventBus.tryEmit(
                            MessagingEvent.ConnectionState(status, message)
                        )
                    }
                },
                onAuthError = { errorMessage ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val detailedError = "Verbindung $host:$port - $errorMessage"
                        helper.updateServiceNotification(detailedError)
                        MessagingEventBus.tryEmit(MessagingEvent.AuthError(detailedError))
                    }
                }
            )

            clientWrappers[connectionId]?.connect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            clientWrappers.values.forEach { it.disconnectAndWait() }
            clientWrappers.clear()
        }
        running.set(false)
        MessagingEventBus.tryEmit(MessagingEvent.ServiceRunningState(false))
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
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
