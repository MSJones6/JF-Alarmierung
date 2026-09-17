package de.msjones.android.alarmapp.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.service.MessagingService

/**
 * Startet nach dem Geräteneustart den Messaging-Service für alle gespeicherten Verbindungen.
 */
class MessagingServiceStarterWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Lädt gespeicherte Verbindungen und startet den Foreground-Service.
     *
     * @return Erfolg oder Retry bei Fehlern
     */
    override suspend fun doWork(): Result {
        return try {
            val store = SettingsStore.getInstance(applicationContext)
            store.migrateIfNeeded()
            val connections = store.getConnectionsSnapshot()

            if (connections.isEmpty()) {
                return Result.success()
            }

            connections.forEach { settings ->
                val serviceIntent = Intent(applicationContext, MessagingService::class.java).apply {
                    putExtra(MessagingService.EXTRA_HOST, settings.host)
                    putExtra(MessagingService.EXTRA_PORT, settings.port)
                    putExtra(MessagingService.EXTRA_USERNAME, settings.username)
                    putExtra(MessagingService.EXTRA_PASSWORD, settings.password)
                    putExtra(MessagingService.EXTRA_TOPIC, settings.topic)
                    putExtra(MessagingService.EXTRA_CONNECTION_ID, settings.id)
                    putExtra(MessagingService.EXTRA_SSL, settings.ssl)
                }
                applicationContext.startForegroundService(serviceIntent)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
