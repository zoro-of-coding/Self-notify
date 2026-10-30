package com.example.selfnotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires the scheduled notification and removes it from the stored list.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationHelper.CHANNEL_DEFAULT

        NotificationHelper.show(context, id, title, text, channel)
        NotificationHelper.removeById(context, id)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_CHANNEL = "channel"
    }
}
