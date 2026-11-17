package com.example.monk

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import java.util.concurrent.TimeUnit

class MonkWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).workspaceDao()
        val rules = dao.getAllBlockerRules()
        val session = dao.getActiveFocusSession()

        if (MonkBlockerEngine.hasAnyActiveMonitoringNeed(rules, session)) {
            try {
                val intent = Intent(applicationContext, MonkBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }
            } catch (_: Exception) {
                // Service start may be restricted from background on newer APIs; AccessibilityService still guards
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "monk_blocker_watchdog_periodic"
        private const val UNIQUE_IMMEDIATE_NAME = "monk_blocker_watchdog_now"

        fun scheduleWatchdog(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            val periodic = PeriodicWorkRequestBuilder<MonkWatchdogWorker>(15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )
        }

        fun enqueueImmediateCheck(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            val oneTime = OneTimeWorkRequestBuilder<MonkWatchdogWorker>().build()
            wm.enqueueUniqueWork(
                UNIQUE_IMMEDIATE_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTime
            )
        }
    }
}
