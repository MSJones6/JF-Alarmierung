package de.msjones.android.alarmapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.data.ServerSettings
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.service.MessagingService
import de.msjones.android.alarmapp.ui.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore

    private val reqNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inhalt darf auch unter Statusleiste/NavigationBar liegen,
        // Insets regeln wir selbst im Compose-Layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        store = SettingsStore(this)

        // Benachrichtigungsrechte anfragen (nur ab Android 13 notwendig)
        if (Build.VERSION.SDK_INT >= 33) {
            reqNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            // gespeicherte Werte laden oder Defaults setzen
            val initial: ServerSettings = try {
                store.flow.first()
            } catch (e: Exception) {
                // Fallback-Defaults
                ServerSettings(
                    host = "localhost",
                    port = 5672,
                    username = "guest",
                    password = "guest",
                    queue = "mobile_notifications"
                )
            }

            setContent {
                SettingsScreen(
                    initial = initial,
                    onSave = { s -> lifecycleScope.launch { store.save(s) } },
                    onStartService = { startMessagingService() },
                    onStopService = {
                        stopService(Intent(this@MainActivity, MessagingService::class.java))
                    }
                )
            }
        }
    }

    private fun startMessagingService() {
        val intent = Intent(this, MessagingService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
