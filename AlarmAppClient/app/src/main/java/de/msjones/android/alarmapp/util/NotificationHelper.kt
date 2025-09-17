package de.msjones.android.alarmapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import de.msjones.android.alarmapp.MainActivity
import de.msjones.android.alarmapp.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val SERVICE_NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mqtt_service_channel"
        const val CHANNEL_NAME = "MQTT Service"
        const val MESSAGE_CHANNEL_ID = "mqtt_message_channel"
        const val MESSAGE_CHANNEL_NAME = "MQTT Messages"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val soundUri = "android.resource://${context.packageName}/${R.raw.piepser}".toUri()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "ServiceChannel"
                setSound(soundUri, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                )
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // --- Message Channel ---
            val messages = NotificationChannel(
                MESSAGE_CHANNEL_ID, MESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AlarmChannel"
                enableVibration(true)
                setSound(soundUri, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                )
            }
            notificationManager.createNotificationChannel(messages)
        }
    }
    /** Baut die Foreground-Service-Notification */
    fun buildServiceNotification(content: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("JF Alarm")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /** Update Foreground-Service Notification */
    fun updateServiceNotification(content: String) {
        val notification = buildServiceNotification(content)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    /** Zeigt eine eingehende Nachricht mit Ton und Popup */
    fun showIncomingMessage(message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("Neue Nachricht")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
