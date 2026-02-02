package de.msjones.android.alarmapp.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
        const val ACTION_STOP_ALL = "STOP_ALL_CONNECTIONS"
    }

    override fun onCreate() {
        super.onCreate()
        helper = NotificationHelper(this)
        settingsStore = SettingsStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Foreground-Service starten
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            helper.buildServiceNotification("Service startet …")
        )

        // Read settings from intent
        val host = intent?.getStringExtra(EXTRA_HOST)
        val port = intent?.getIntExtra(EXTRA_PORT, 1883) ?: 1883
        val username = intent?.getStringExtra(EXTRA_USERNAME) ?: ""
        val password = intent?.getStringExtra(EXTRA_PASSWORD) ?: ""
        val topic = intent?.getStringExtra(EXTRA_TOPIC) ?: "JF/Alarm"
        val connectionId = intent?.getStringExtra(EXTRA_CONNECTION_ID) ?: "unknown"

        // MQTT-Verbindung im Hintergrund aufbauen
        job = lifecycleScope.launch(Dispatchers.IO) {
            if (host.isNullOrBlank()) {
                helper.updateServiceNotification("Bitte Serverdaten speichern.")
                return@launch
            }

            val serverUri = "tcp://${host}:${port}"
            
            // Disconnect existing client if any
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
                        // Parse status prefix
                        val (status, message) = if (state.contains(":")) {
                            val parts = state.split(":", limit = 2)
                            parts[0] to parts[1]
                        } else {
                            "INFO" to state
                        }
                        
                        // Store connection status persistently
                        when (status.uppercase()) {
                            "CONNECTED" -> settingsStore.setConnected(message)
                            "DISCONNECTED" -> settingsStore.setDisconnected(message)
                            "ERROR" -> {
                                settingsStore.setConnectionError(message)
                                // Broadcast stop all connections event
                                lifecycleScope.launch {
                                    val stopIntent = Intent(ACTION_STOP_ALL)
                                    LocalBroadcastManager.getInstance(this@MessagingService).sendBroadcast(stopIntent)
                                }
                            }
                            else -> settingsStore.setConnectionStatus(status, message)
                        }
                        
                        // Update notification
                        helper.updateServiceNotification(message)
                        
                        // Broadcast state to UI
                        val intent = Intent("CONNECTION_STATE")
                        intent.putExtra("state_status", status)
                        intent.putExtra("state_message", message)
                        LocalBroadcastManager.getInstance(this@MessagingService).sendBroadcast(intent)
                    }
                },
                onAuthError = { errorMessage ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val detailedError = "Verbindung $host:$port - $errorMessage"
                        helper.updateServiceNotification(detailedError)
                        // Broadcast auth error to UI
                        val intent = Intent("AUTH_ERROR")
                        intent.putExtra("error_message", detailedError)
                        LocalBroadcastManager.getInstance(this@MessagingService).sendBroadcast(intent)
                    }
                }
            )

            // Verbindung aufbauen
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
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    private fun handleIncomingMessage(msg: String) {
        // Nachricht parsen: Alarmstichwort###Ort###Sonstiges
        val parts = msg.split("###", limit = 3)
        val keyword = parts.getOrNull(0)?.trim() ?: ""
        val location = parts.getOrNull(1)?.trim() ?: ""
        val extras = parts.getOrNull(2)?.trim() ?: ""
        
        // Notification zeigen
        helper.showIncomingMessage(keyword, location, extras)

        // Broadcast für MainActivity
        val intent = Intent("NEW_MESSAGE")
        intent.putExtra("keyword", keyword)
        intent.putExtra("location", location)
        intent.putExtra("extras", extras)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

}
