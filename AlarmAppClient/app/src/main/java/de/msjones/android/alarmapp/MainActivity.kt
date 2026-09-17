package de.msjones.android.alarmapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.msjones.android.alarmapp.data.ServerSettings
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus
import de.msjones.android.alarmapp.service.MessagingService
import de.msjones.android.alarmapp.ui.MessageListScreen
import de.msjones.android.alarmapp.ui.MessageViewModel
import de.msjones.android.alarmapp.ui.SettingsScreen
import de.msjones.android.alarmapp.ui.theme.JFAlarmTheme
import kotlinx.coroutines.launch

/**
 * Einstiegsaktivität der JF Alarm App mit Nachrichtenliste und Einstellungen.
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore

    private val reqNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        store = SettingsStore.getInstance(this)

        if (Build.VERSION.SDK_INT >= 33) {
            reqNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            store.migrateIfNeeded()
            store.initializeDefaultIfEmpty(
                ServerSettings(
                    host = "localhost",
                    port = 1883,
                    username = "reader",
                    password = "reader",
                    topic = "JF/Alarm/KB"
                )
            )
        }

        // Globale Service-Ereignisse (Stop-All) auch ohne geöffnete Settings verarbeiten.
        lifecycleScope.launch {
            MessagingEventBus.events.collect { event ->
                when (event) {
                    is MessagingEvent.StopAllConnections -> {
                        stopMessagingService()
                        store.disableAllConnections()
                        store.clearConnectionStatus()
                    }
                    is MessagingEvent.AuthError -> {
                        store.setConnectionError(event.errorMessage)
                    }
                    is MessagingEvent.ConnectionState -> {
                        persistConnectionState(event.status, event.message)
                    }
                    else -> Unit
                }
            }
        }

        setContent {
            JFAlarmTheme {
                val navController = rememberNavController()
                val msgViewModel: MessageViewModel = viewModel()

                val connections by store.flow.collectAsState(initial = emptyList())

                NavHost(navController = navController, startDestination = "messages") {
                    composable("messages") {
                        MessageListScreen(
                            viewModel = msgViewModel,
                            onSettingsClick = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            connections = connections,
                            onSaveConnection = { settings ->
                                lifecycleScope.launch {
                                    store.saveConnection(settings)
                                    if (settings.isActive && settings.host.isNotBlank()) {
                                        startMessagingService(settings)
                                    }
                                }
                            },
                            onDeleteConnection = { id ->
                                lifecycleScope.launch {
                                    val existing = store.getConnectionsSnapshot().find { it.id == id }
                                    if (existing?.isActive == true) {
                                        stopMessagingConnection(id)
                                    }
                                    store.deleteConnection(id)
                                }
                            },
                            onToggleConnection = { connection, enabled ->
                                lifecycleScope.launch {
                                    store.setConnectionEnabled(connection.id, enabled)
                                    if (enabled) {
                                        startMessagingService(connection.copy(isActive = true))
                                    } else {
                                        stopMessagingConnection(connection.id)
                                    }
                                }
                            },
                            onConnectionFailed = { connectionId ->
                                lifecycleScope.launch {
                                    store.setConnectionEnabled(connectionId, false)
                                    stopMessagingConnection(connectionId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Speichert den Verbindungsstatus dauerhaft im SettingsStore.
     */
    private suspend fun persistConnectionState(status: String, stateMessage: String) {
        if (status.isEmpty() || stateMessage.isEmpty()) return
        when (status.uppercase()) {
            "CONNECTED" -> store.setConnected(stateMessage)
            "DISCONNECTED" -> store.setDisconnected(stateMessage)
            "ERROR" -> store.setConnectionError(stateMessage)
            else -> store.setConnectionStatus(status, stateMessage)
        }
    }

    /**
     * Beendet den Messaging-Foreground-Service.
     */
    private fun stopMessagingService() {
        stopService(Intent(this, MessagingService::class.java))
    }

    /**
     * Trennt eine einzelne MQTT-Verbindung im laufenden Dienst.
     *
     * @param connectionId Kennung der zu trennenden Verbindung
     */
    private fun stopMessagingConnection(connectionId: String) {
        if (!MessagingService.isRunning()) {
            return
        }
        val intent = Intent(this, MessagingService::class.java).apply {
            putExtra(MessagingService.EXTRA_ACTION, MessagingService.ACTION_DISCONNECT)
            putExtra(MessagingService.EXTRA_CONNECTION_ID, connectionId)
        }
        startForegroundService(intent)
    }

    /**
     * Startet den Messaging-Service für eine gespeicherte Verbindung.
     *
     * @param settings Verbindungsparameter zum MQTT-Broker
     */
    private fun startMessagingService(settings: ServerSettings) {
        val intent = Intent(this, MessagingService::class.java).apply {
            putExtra(MessagingService.EXTRA_HOST, settings.host)
            putExtra(MessagingService.EXTRA_PORT, settings.port)
            putExtra(MessagingService.EXTRA_USERNAME, settings.username)
            putExtra(MessagingService.EXTRA_PASSWORD, settings.password)
            putExtra(MessagingService.EXTRA_TOPIC, settings.topic)
            putExtra(MessagingService.EXTRA_CONNECTION_ID, settings.id)
            putExtra(MessagingService.EXTRA_SSL, settings.ssl)
            putExtra(MessagingService.EXTRA_ACTION, MessagingService.ACTION_CONNECT)
        }
        startForegroundService(intent)
    }
}
