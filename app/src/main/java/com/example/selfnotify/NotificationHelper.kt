package com.example.selfnotify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles all notification work:
 * - creating the notification channels ("types")
 * - showing a notification
 * - scheduling / cancelling one-time scheduled notifications (AlarmManager)
 * - persisting pending notifications so they survive app restarts / reboots
 */
object NotificationHelper {

    private const val PREFS_NAME = "self_notify_pending"
    private const val PREF_KEY = "pending_notifications"

    const val CHANNEL_HIGH = "high"
    const val CHANNEL_DEFAULT = "default"
    const val CHANNEL_LOW = "low"
    const val CHANNEL_SILENT = "silent"

    /** Fires directly as a heads-up banner with default sound/vibration emphasis. */
    const val CHANNEL_HIGH_LABEL = "High priority (heads-up)"

    /** Normal notification in the shade, with default sound. */
    const val CHANNEL_DEFAULT_LABEL = "Default"

    /** Quiet notification, low importance, ordered below others. */
    const val CHANNEL_LOW_LABEL = "Low priority"

    /** Quiet notification with no sound at all. */
    const val CHANNEL_SILENT_LABEL = "Silent (no sound)"

    /** Channel ids for each notification "type" the user can pick. */
    val CHANNEL_LABELS = linkedMapOf(
        CHANNEL_HIGH to CHANNEL_HIGH_LABEL,
        CHANNEL_DEFAULT to CHANNEL_DEFAULT_LABEL,
        CHANNEL_LOW to CHANNEL_LOW_LABEL,
        CHANNEL_SILENT to CHANNEL_SILENT_LABEL,
    )

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val high = NotificationChannel(CHANNEL_HIGH, CHANNEL_HIGH_LABEL, NotificationManager.IMPORTANCE_HIGH)
        val default = NotificationChannel(CHANNEL_DEFAULT, CHANNEL_DEFAULT_LABEL, NotificationManager.IMPORTANCE_DEFAULT)
        val low = NotificationChannel(CHANNEL_LOW, CHANNEL_LOW_LABEL, NotificationManager.IMPORTANCE_LOW)
        val silent = NotificationChannel(CHANNEL_SILENT, CHANNEL_SILENT_LABEL, NotificationManager.IMPORTANCE_LOW)
            .apply { setSound(null, null) }

        nm.createNotificationChannels(listOf(high, default, low, silent))
    }

    /** Immediately shows a notification with the given id/title/text on the given channel. */
    fun show(context: Context, id: Int, title: String, text: String, channelId: String) {
        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked; nothing we can do here.
        }
    }

    /**
     * Schedules a notification: registers the alarm AND persists it,
     * replacing any previously stored entry with the same id.
     */
    fun schedule(context: Context, sn: ScheduledNotification) {
        setAlarm(context, sn)
        val updated = listScheduled(context).filterNot { it.id == sn.id } + sn
        saveAll(context, updated)
    }

    /** Registers the alarm only (used to re-arm after a boot, without duplicating storage). */
    fun setAlarm(context: Context, sn: ScheduledNotification) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmIntent(context, sn)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sn.time, pi)
        } else {
            // Fallback when the user has not granted exact alarms: still fires, timing may drift slightly.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sn.time, pi)
        }
    }

    fun cancel(context: Context, sn: ScheduledNotification) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmIntent(context, sn))
        saveAll(context, listScheduled(context).filterNot { it.id == sn.id })
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listScheduled(context).forEach { am.cancel(alarmIntent(context, it)) }
        saveAll(context, emptyList())
    }

    /** Removes a stored notification (called after it actually fires). */
    fun removeById(context: Context, id: Int) {
        saveAll(context, listScheduled(context).filterNot { it.id == id })
    }

    fun listScheduled(context: Context): List<ScheduledNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { ScheduledNotification.fromJson(array.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAll(context: Context, list: List<ScheduledNotification>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY, array.toString())
            .apply()
    }

    private fun alarmIntent(context: Context, sn: ScheduledNotification): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            sn.id,
            Intent(context, AlarmReceiver::class.java)
                .setAction("com.example.selfnotify.ACTION_FIRE")
                .putExtra(AlarmReceiver.EXTRA_ID, sn.id)
                .putExtra(AlarmReceiver.EXTRA_TITLE, sn.title)
                .putExtra(AlarmReceiver.EXTRA_TEXT, sn.text)
                .putExtra(AlarmReceiver.EXTRA_CHANNEL, sn.channelId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
