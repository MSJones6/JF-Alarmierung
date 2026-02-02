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
            val initial: ServerSettings = try {
                store.flow.first()
            } catch (e: Exception) {
                ServerSettings(
                    host = "localhost",
                    port = 1883,
                    username = "guest",
                    password = "guest",
                    topic = "JF/Alarm"
                )
            }

            setContent {
                JFAlarmTheme {
                    val navController = rememberNavController()
                    msgViewModel = viewModel()

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
                                initial = initial,
                                onSave = { s -> lifecycleScope.launch { store.save(s) } },
                                onStartService = { startMessagingService() },
                                onStopService = {
                                    stopService(
                                        Intent(
                                            this@MainActivity,
                                            MessagingService::class.java
                                        )
                                    )
                                },
                                onServiceFailed = {
                                    // Service failed to start (e.g., auth error)
                                    // The switch will be reset by the broadcast receiver
                                },
                                isServiceRunning = isMessagingServiceRunning()
                            )
                        }
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

    private fun startMessagingService() {
        val intent = Intent(this, MessagingService::class.java)
        startForegroundService(intent)
    }
}
