package com.example.selfnotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every stored scheduled notification after the device reboots,
 * because alarms scheduled with AlarmManager are cleared on shutdown.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val now = System.currentTimeMillis()
        NotificationHelper.listScheduled(context)
            .filter { it.time > now }
            .forEach { NotificationHelper.setAlarm(context, it) }
    }
}
