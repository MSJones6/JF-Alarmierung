package de.msjones.android.alarmapp

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.msjones.android.alarmapp.data.ServerSettings
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.service.MessagingService
import de.msjones.android.alarmapp.ui.MessageListScreen
import de.msjones.android.alarmapp.ui.MessageViewModel
import de.msjones.android.alarmapp.ui.SettingsScreen
import de.msjones.android.alarmapp.ui.theme.JFAlarmTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore
    private lateinit var msgViewModel: MessageViewModel
    private var messageReceiver: BroadcastReceiver? = null

    private val reqNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = SettingsStore(this)

        // Benachrichtigungsrechte anfragen (nur ab Android 13)
        if (Build.VERSION.SDK_INT >= 33) {
            reqNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            // Initialize default connection if empty
            store.initializeDefaultIfEmpty(
                ServerSettings(
                    host = "localhost",
                    port = 1883,
                    username = "guest",
                    password = "guest",
                    topic = "JF/Alarm"
                )
            )
        }

        setContent {
            JFAlarmTheme {
                val navController = rememberNavController()
                msgViewModel = viewModel()

                val connections by store.flow.collectAsState(initial = emptyList())
                val activeConnectionId by store.activeConnectionId.collectAsState(initial = null)

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
                            activeConnectionId = activeConnectionId,
                            onSaveConnection = { s ->
                                lifecycleScope.launch {
                                    store.saveConnection(s)
                                    // Set as active if it's the first connection
                                    if (connections.isEmpty()) {
                                        store.setActiveConnection(s.id)
                                    }
                                }
                            },
                            onDeleteConnection = { id ->
                                lifecycleScope.launch {
                                    store.deleteConnection(id)
                                    // If deleted connection was active, clear it
                                    if (activeConnectionId == id) {
                                        store.clearActiveConnection()
                                    }
                                }
                            },
                            onSetActiveConnection = { id ->
                                lifecycleScope.launch {
                                    // Update isActive field for all connections
                                    val allConnections = store.flow.first()
                                    for (connection in allConnections) {
                                        val updated = connection.copy(isActive = connection.id == id)
                                        store.saveConnection(updated)
                                    }
                                    store.setActiveConnection(id)
                                }
                            },
                            onStartAllServices = {
                                // Start service for each connection
                                connections.forEach { connection ->
                                    startMessagingService(connection)
                                }
                            },
                            onStopAllServices = {
                                // Stop all services by sending stop intent for each
                                stopService(
                                    Intent(
                                        this@MainActivity,
                                        MessagingService::class.java
                                    )
                                )
                            },
                            onServiceFailed = {
                                // Service failed to start (e.g., auth error)
                                // Stop all services
                                stopService(
                                    Intent(
                                        this@MainActivity,
                                        MessagingService::class.java
                                    )
                                )
                            },
                            isServiceRunning = isMessagingServiceRunning()
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerMessageReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterMessageReceiver()
    }

    private fun registerMessageReceiver() {
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "NEW_MESSAGE") {
                    val keyword = intent.getStringExtra("keyword") ?: ""
                    val location = intent.getStringExtra("location") ?: ""
                    val extras = intent.getStringExtra("extras") ?: ""
                    
                    if (keyword.isNotBlank() || location.isNotBlank() || extras.isNotBlank()) {
                        msgViewModel.addMessage(keyword, location, extras)
                    }
                }
            }
        }
        val filter = IntentFilter("NEW_MESSAGE")
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver!!, filter)
    }

    private fun unregisterMessageReceiver() {
        messageReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
            messageReceiver = null
        }
    }

    private fun isMessagingServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        return services.any { service ->
            service.service.className == MessagingService::class.java.name
        }
    }

    private fun startMessagingService(settings: ServerSettings) {
        val intent = Intent(this, MessagingService::class.java).apply {
            putExtra(MessagingService.EXTRA_HOST, settings.host)
            putExtra(MessagingService.EXTRA_PORT, settings.port)
            putExtra(MessagingService.EXTRA_USERNAME, settings.username)
            putExtra(MessagingService.EXTRA_PASSWORD, settings.password)
            putExtra(MessagingService.EXTRA_TOPIC, settings.topic)
            putExtra(MessagingService.EXTRA_CONNECTION_ID, settings.id)
        }
        startForegroundService(intent)
    }
}
