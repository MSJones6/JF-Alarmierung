package de.msjones.android.alarmapp.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MessagingService : LifecycleService() {

    private lateinit var helper: NotificationHelper
    private var mqttClientWrapper: MqttClientWrapper? = null
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        helper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Foreground-Service starten
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            helper.buildServiceNotification("Service startet …")
        )

        val store = SettingsStore(this)

        // MQTT-Verbindung im Hintergrund aufbauen
        job = lifecycleScope.launch(Dispatchers.IO) {
            val s = store.flow.first()  // Einstellungen laden
            if (s.host.isBlank()) {
                helper.updateServiceNotification("Bitte Serverdaten speichern.")
                return@launch
            }

            val serverUri = "tcp://${s.host}:${s.port}"

            mqttClientWrapper = MqttClientWrapper(
                context = this@MessagingService,
                serverUri = serverUri,
                clientId = "AndroidClient-${System.currentTimeMillis()}",
                user = s.username,
                pass = s.password,
                topic = s.topic,
                onMessage = { msg ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        helper.showIncomingMessage(msg)
                    }
                },
                onState = { state ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        helper.updateServiceNotification(state)
                    }
                }
            )

            // Verbindung aufbauen
            mqttClientWrapper?.connect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            mqttClientWrapper?.disconnect()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}
