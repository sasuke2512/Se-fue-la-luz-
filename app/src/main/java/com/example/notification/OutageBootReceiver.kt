package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.OutageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OutageBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val active = OutageDatabase.getInstance(appContext)
                    .outageDao()
                    .getActiveOutageOnce()
                if (active != null) {
                    OutageNotificationHelper.showOngoingOutageNotification(
                        context = appContext,
                        startTimeMillis = active.startTimeMillis
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
