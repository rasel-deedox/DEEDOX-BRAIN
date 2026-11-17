package com.example.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.example.R
import com.example.data.local.SmartReminderEntity
import kotlin.math.abs

object ReminderScheduler {

    const val CHANNEL_ID_REMINDERS = "smart_reminders_channel"
    const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
    const val ACTION_DISMISS_RINGTONE = "com.example.ACTION_DISMISS_RINGTONE"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    const val RECURRENCE_ONCE = "ONCE"
    const val RECURRENCE_EVERY_2_HOURS = "EVERY_2_HOURS"
    const val RECURRENCE_DAILY = "DAILY"
    const val RECURRENCE_WEEKLY = "WEEKLY"
    const val RECURRENCE_CUSTOM_MINUTES = "CUSTOM_MINUTES"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val existing = nm.getNotificationChannel(CHANNEL_ID_REMINDERS)
            if (existing == null) {
                val defaultSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val channel = NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    context.getString(R.string.reminder_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for library content reminders, personal tasks, and Monk Mode schedules"
                    enableVibration(true)
                    setSound(
                        defaultSound,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(context: Context, reminder: SmartReminderEntity) {
        ensureNotificationChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildTriggerPendingIntent(context, reminder.id)
        am.cancel(pendingIntent)

        if (!reminder.isEnabled || reminder.deletedAt != null) return

        val triggerAt = computeValidTriggerMillis(reminder)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(buildTriggerPendingIntent(context, reminderId))
    }

    fun computeValidTriggerMillis(reminder: SmartReminderEntity, nowMs: Long = System.currentTimeMillis()): Long {
        if (reminder.triggerAtMillis > nowMs) return reminder.triggerAtMillis
        return computeNextRecurrenceMillis(
            currentTriggerMs = reminder.triggerAtMillis,
            recurrenceType = reminder.recurrenceType,
            intervalMinutes = reminder.intervalMinutes,
            nowMs = nowMs
        ) ?: (nowMs + 5_000L)
    }

    fun computeNextRecurrenceMillis(
        currentTriggerMs: Long,
        recurrenceType: String,
        intervalMinutes: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Long? {
        val stepMs = when (recurrenceType) {
            RECURRENCE_EVERY_2_HOURS -> 2L * 60L * 60_000L
            RECURRENCE_DAILY -> 24L * 60L * 60_000L
            RECURRENCE_WEEKLY -> 7L * 24L * 60L * 60_000L
            RECURRENCE_CUSTOM_MINUTES -> intervalMinutes.coerceAtLeast(1).toLong() * 60_000L
            else -> return null
        }
        var next = currentTriggerMs + stepMs
        while (next <= nowMs) {
            next += stepMs
        }
        return next
    }

    private fun buildTriggerPendingIntent(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            abs(reminderId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
