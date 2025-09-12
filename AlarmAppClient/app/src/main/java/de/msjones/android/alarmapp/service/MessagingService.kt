package de.msjones.android.alarmapp.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagingService : LifecycleService() {

    private lateinit var helper: NotificationHelper
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        helper = NotificationHelper(this)

        // Vordergrundservice starten
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            helper.buildServiceNotification("Starte …")
        )

        val store = SettingsStore(this)

        // Service-Job starten
        job = lifecycleScope.launch {
            val s = store.flow.first()
            if (s.host.isBlank() || s.username.isBlank()) {
                helper.updateServiceNotification("Bitte Serverdaten speichern.")
                return@launch
            }

            val client = AmqpClient(
                cfg = AmqpConfig(s.host, s.port, s.username, s.password, s.queue),
                onMessage = { message -> helper.showIncomingMessage(message) },
                onState = { state -> helper.updateServiceNotification(state) }
            )

            // Netzwerkoperationen auf IO-Dispatcher auslagern
            withContext(Dispatchers.IO) {
                client.keepAlive(stopFlag = { !coroutineContext.isActive })
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}
