package com.example.monk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MonkBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val dao = db.workspaceDao()

                // Reschedule all active Smart Reminders
                dao.getEnabledReminders().forEach { reminder ->
                    ReminderScheduler.scheduleReminder(appContext, reminder)
                }

                // Restore Monk Mode anti-kill monitoring if any rules or sessions are active
                val rules = dao.getAllBlockerRules()
                val session = dao.getActiveFocusSession()
                if (MonkBlockerEngine.hasAnyActiveMonitoringNeed(rules, session)) {
                    MonkWatchdogWorker.scheduleWatchdog(appContext)
                    MonkWatchdogWorker.enqueueImmediateCheck(appContext)
                    MonkBlockerEngine.ensureMonitorRunning(appContext)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
