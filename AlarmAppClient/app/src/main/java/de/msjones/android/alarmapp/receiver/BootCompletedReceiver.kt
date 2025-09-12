package de.msjones.android.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.msjones.android.alarmapp.service.MessagingService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val svc = Intent(context, MessagingService::class.java)
            context.startForegroundService(svc)
        }
    }
}
