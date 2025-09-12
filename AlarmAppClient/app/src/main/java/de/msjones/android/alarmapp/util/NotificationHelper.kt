package de.msjones.android.alarmapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import de.msjones.android.alarmapp.MainActivity
import de.msjones.android.alarmapp.R

class NotificationHelper(private val ctx: Context) {

    companion object {
        const val CHANNEL_ID_SERVICE = "conn_status"
        const val CHANNEL_ID_MESSAGES = "incoming_messages"
        const val SERVICE_NOTIFICATION_ID = 1001
        private const val MESSAGE_NOTIFICATION_ID = 2001
    }

    private val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val service = NotificationChannel(
                CHANNEL_ID_SERVICE, "Verbindungsstatus",
                NotificationManager.IMPORTANCE_MIN
            )
            val messages = NotificationChannel(
                CHANNEL_ID_MESSAGES, "Eingehende Nachrichten",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                enableVibration(true)
            }
            nm.createNotificationChannel(service)
            nm.createNotificationChannel(messages)
        }
    }

    fun buildServiceNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Queue Client")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    fun updateServiceNotification(state: String) {
        nm.notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(state))
    }

    fun showIncomingMessage(message: String) {
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Neue Nachricht")
            .setContentText(message.take(64))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        nm.notify(MESSAGE_NOTIFICATION_ID, notif)
    }
}
