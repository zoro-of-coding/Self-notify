package com.example.selfnotify

import org.json.JSONObject

/**
 * A notification that can be shown right away or scheduled for a specific time.
 *
 * @param id         Unique id used for the pending intent, the notification id,
 *                   and to identify it in storage.
 * @param title      Notification title.
 * @param text       Notification body text.
 * @param channelId  Which notification channel (importance/type) to use.
 * @param time       Epoch milliseconds when the scheduled notification should fire.
 */
data class ScheduledNotification(
    val id: Int,
    val title: String,
    val text: String,
    val channelId: String,
    val time: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("text", text)
        put("channel", channelId)
        put("time", time)
    }

    companion object {
        fun fromJson(o: JSONObject): ScheduledNotification = ScheduledNotification(
            id = o.optInt("id", -1),
            title = o.optString("title", ""),
            text = o.optString("text", ""),
            channelId = o.optString("channel", NotificationHelper.CHANNEL_DEFAULT),
            time = o.optLong("time", 0L),
        )
    }
}
