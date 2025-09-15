package de.msjones.android.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import de.msjones.android.alarmapp.worker.MessagingServiceStarterWorker

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            // kleine Verzögerung, damit System Boot abgeschlossen hat
            val workRequest: WorkRequest = OneTimeWorkRequestBuilder<MessagingServiceStarterWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
