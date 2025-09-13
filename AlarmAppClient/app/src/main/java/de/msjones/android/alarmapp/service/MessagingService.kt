package de.msjones.android.alarmapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.msjones.android.alarmapp.MainActivity
import de.msjones.android.alarmapp.R
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagingService : LifecycleService() {

    private lateinit var settingsStore: SettingsStore
    private var job: Job? = null

    companion object {
        const val SERVICE_CHANNEL_ID = "service_channel"
        const val SERVICE_CHANNEL_NAME = "Service-Benachrichtigungen"

        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val ALARM_CHANNEL_NAME = "Alarmierungen"

        const val SERVICE_NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)

        createServiceNotificationChannel()
        createAlarmNotificationChannel()

        val serviceNotification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification_small)
            .setContentTitle("Messaging Service")
            .setContentText("Warte auf Alarmmeldungen…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, serviceNotification)

        job = lifecycleScope.launch {
            val settings = settingsStore.flow.first()
            if (settings.host.isBlank() || settings.username.isBlank()) {
                updateServiceNotification("Fehlende Serverdaten.")
                return@launch
            }

            val client = AmqpClient(
                cfg = AmqpConfig(settings.host, settings.port, settings.username, settings.password, settings.queue),
                onMessage = { message ->
                    showAlarmNotification("Neue Alarmierung", message)
                },
                onState = { state ->
                    updateServiceNotification(state)
                }
            )

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

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Benachrichtigungen für den laufenden Service"
                setSound(null, null)
                enableVibration(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.piepser}")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für eingehende Alarme"
                setSound(soundUri, audioAttributes)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showAlarmNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification_small)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.piepser}")
            builder.setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }

    private fun updateServiceNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification_small)
            .setContentTitle("Alarmierungsdienst")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSound(null)
            .build()

        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }
}
