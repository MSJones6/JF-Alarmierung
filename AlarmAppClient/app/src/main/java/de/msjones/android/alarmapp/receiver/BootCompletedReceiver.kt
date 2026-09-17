package de.msjones.android.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import de.msjones.android.alarmapp.worker.MessagingServiceStarterWorker
import java.util.concurrent.TimeUnit

/**
 * Startet nach BOOT_COMPLETED zeitverzögert den Messaging-Service über WorkManager.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    /**
     * Plant den Start des Messaging-Services, sobald das Gerät fertig gebootet hat.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest: WorkRequest = OneTimeWorkRequestBuilder<MessagingServiceStarterWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
