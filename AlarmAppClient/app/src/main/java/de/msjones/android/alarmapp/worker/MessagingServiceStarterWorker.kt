package de.msjones.android.alarmapp.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.msjones.android.alarmapp.service.MessagingService

class MessagingServiceStarterWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val serviceIntent = Intent(applicationContext, MessagingService::class.java)
            applicationContext.startForegroundService(serviceIntent)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
