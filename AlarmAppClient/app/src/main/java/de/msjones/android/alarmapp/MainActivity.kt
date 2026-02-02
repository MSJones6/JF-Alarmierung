package de.msjones.android.alarmapp

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore

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
                    val msgViewModel: MessageViewModel = viewModel()

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
                                isServiceRunning = isMessagingServiceRunning()
                            )
                        }
                    }
                }
            }
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
