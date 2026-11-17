package com.example.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_DISMISS_RINGTONE) {
            stopActiveRingtone()
            val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID)
            if (!reminderId.isNullOrBlank()) {
                val nm = context.getSystemService(NotificationManager::class.java)
                nm?.cancel(abs(reminderId.hashCode()))
            }
            return
        }

        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val dao = db.workspaceDao()
                val reminder = dao.getReminderById(reminderId)
                if (reminder != null && reminder.isEnabled && reminder.deletedAt == null) {
                    ReminderScheduler.ensureNotificationChannel(appContext)
                    playCustomRingtone(appContext, reminder.ringtoneUri)
                    postReminderNotification(appContext, reminder.id, reminder.title, reminder.notes, reminder.mode)

                    val now = System.currentTimeMillis()
                    val nextTrigger = ReminderScheduler.computeNextRecurrenceMillis(
                        currentTriggerMs = reminder.triggerAtMillis,
                        recurrenceType = reminder.recurrenceType,
                        intervalMinutes = reminder.intervalMinutes,
                        nowMs = now
                    )
                    val updated = if (nextTrigger != null) {
                        reminder.copy(
                            triggerAtMillis = nextTrigger,
                            lastTriggeredAt = now,
                            updatedAt = now
                        )
                    } else {
                        reminder.copy(
                            isEnabled = false,
                            lastTriggeredAt = now,
                            updatedAt = now
                        )
                    }
                    dao.upsertReminder(updated)
                    if (updated.isEnabled) {
                        ReminderScheduler.scheduleReminder(appContext, updated)
                    }
                }
            } catch (_: Exception) {
                // Prevent receiver crash if DB or audio subsystem fails
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        notes: String,
        mode: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val openPending = PendingIntent.getActivity(
            context,
            abs(reminderId.hashCode()) + 11,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_DISMISS_RINGTONE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context,
            abs(reminderId.hashCode()) + 22,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefix = when (mode) {
            "LIBRARY_CONTENT" -> "Library Reminder"
            "MONK_SCHEDULE" -> "Monk Mode Alert"
            else -> "Smart Reminder"
        }
        val body = notes.ifBlank { "Tap to open AI Library" }

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$prefix: $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setDeleteIntent(dismissPending)
            .addAction(0, "Silence", dismissPending)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(abs(reminderId.hashCode()), notification)
    }

    companion object {
        @Volatile
        private var activePlayer: MediaPlayer? = null

        fun playCustomRingtone(context: Context, ringtoneUriString: String?) {
            stopActiveRingtone()
            val uri = ringtoneUriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = false
                    prepare()
                    start()
                }
                player.setOnCompletionListener {
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                    if (activePlayer === it) activePlayer = null
                }
                activePlayer = player
            } catch (_: Exception) {
                try {
                    val fallback = RingtoneManager.getRingtone(context, uri)
                    fallback?.play()
                } catch (_: Exception) {
                }
            }
        }

        fun stopActiveRingtone() {
            try {
                activePlayer?.stop()
            } catch (_: Exception) {
            }
            try {
                activePlayer?.release()
            } catch (_: Exception) {
            }
            activePlayer = null
        }
    }
}
