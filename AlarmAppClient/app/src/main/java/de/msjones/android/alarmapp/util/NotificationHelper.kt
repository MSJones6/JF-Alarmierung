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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Erzeugt Notification-Channels sowie Service-, Status- und Alarm-Benachrichtigungen.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val SERVICE_NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mqtt_service_status_channel"
        const val CHANNEL_NAME = "MQTT Service"
        const val MESSAGE_CHANNEL_ID = "mqtt_message_channel"
        const val MESSAGE_CHANNEL_NAME = "MQTT Messages"
        const val STATUS_CHANNEL_ID = "mqtt_connection_status_channel"
        const val STATUS_CHANNEL_NAME = "MQTT Verbindungsstatus"
        const val STATUS_NOTIFICATION_ID_BASE = 3000

        private const val LEGACY_SERVICE_CHANNEL_ID = "mqtt_service_channel"
    }

    private val messageNotificationId = AtomicInteger(2000)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Legt die benötigten Notification-Channels an und entfernt den alten, stillen Service-Kanal.
     */
    private fun createNotificationChannels() {
        val soundUri = "android.resource://${context.packageName}/${R.raw.piepser}".toUri()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(LEGACY_SERVICE_CHANNEL_ID)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Zeigt den Verbindungsstatus des Alarmdienstes"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            val statusChannel = NotificationChannel(
                STATUS_CHANNEL_ID, STATUS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Meldungen zu nicht erreichbaren oder fehlgeschlagenen Verbindungen"
            }
            notificationManager.createNotificationChannel(statusChannel)

            val messages = NotificationChannel(
                MESSAGE_CHANNEL_ID, MESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AlarmChannel"
                enableVibration(true)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(messages)
        }
    }

    /**
     * Baut die dauerhafte Foreground-Service-Notification.
     *
     * @param content sichtbarer Statustext, z. B. „1 von 2 Verbindungen aktiv“
     * @return Notification für [startForeground]
     */
    fun buildServiceNotification(content: String): Notification {
        val pendingIntent = activityPendingIntent()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("JF Alarm")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Aktualisiert die Foreground-Service-Notification mit einem neuen Statustext.
     *
     * @param content sichtbarer Statustext
     */
    fun updateServiceNotification(content: String) {
        val notification = buildServiceNotification(content)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    /**
     * Zeigt eine wegklickbare Status-Notification für eine einzelne Verbindung.
     *
     * @param connectionId Kennung der betroffenen Verbindung
     * @param title kurze Überschrift, z. B. „Server nicht erreichbar“
     * @param message ausführliche Beschreibung
     */
    fun showStatusNotification(connectionId: String, title: String, message: String) {
        val pendingIntent = activityPendingIntent()
        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(statusNotificationId(connectionId), notification)
    }

    /**
     * Entfernt die Status-Notification einer Verbindung, z. B. nach erfolgreichem Verbindungsaufbau.
     *
     * @param connectionId Kennung der Verbindung
     */
    fun cancelStatusNotification(connectionId: String) {
        notificationManager.cancel(statusNotificationId(connectionId))
    }

    /**
     * Zeigt eine eingehende Alarmnachricht mit Ton und Popup.
     *
     * @param keyword Alarmstichwort
     * @param location Einsatzort
     * @param extras weitere Angaben
     */
    fun showIncomingMessage(keyword: String, location: String, extras: String) {
        val pendingIntent = activityPendingIntent()

        val contentText = buildString {
            if (location.isNotBlank()) {
                append("Ort: $location")
            }
            if (extras.isNotBlank()) {
                if (isNotBlank()) append("\n")
                append(extras)
            }
        }

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(keyword)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(messageNotificationId.incrementAndGet(), notification)
    }

    /**
     * Erzeugt den PendingIntent zum Öffnen der Hauptaktivität.
     *
     * @return unveränderlicher Activity-PendingIntent
     */
    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Berechnet eine stabile Notification-ID für die Statusmeldung einer Verbindung.
     *
     * @param connectionId Kennung der Verbindung
     * @return Notification-ID im Statusbereich
     */
    private fun statusNotificationId(connectionId: String): Int {
        return STATUS_NOTIFICATION_ID_BASE + (connectionId.hashCode() and 0x7FFF)
    }
}
