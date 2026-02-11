package de.msjones.android.alarmapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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

        store = SettingsStore(this)

        // StatusBar Farbe + Icons einstellen
        val window = window
        window.statusBarColor = ContextCompat.getColor(this, R.color.black) // gewünschte Farbe
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // helle Icons
        WindowCompat.setDecorFitsSystemWindows(window, true) // Layout startet unterhalb StatusBar

        // Benachrichtigungsrechte anfragen (nur ab Android 13)
        if (Build.VERSION.SDK_INT >= 33) {
            reqNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            // gespeicherte Werte laden oder Defaults setzen
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
                // Compose UI
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


    @Composable
    fun SetStatusBar() {
        val systemUiController = rememberSystemUiController()
        val statusBarColor = Color.Black // gewünschte Farbe

        SideEffect {
            systemUiController.setStatusBarColor(
                color = statusBarColor,
                darkIcons = false // false = helle Icons
            )
        }
    }
}
