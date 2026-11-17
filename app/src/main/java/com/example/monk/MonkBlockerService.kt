package com.example.monk

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.DetoxRuntimeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MonkBlockerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var lastShieldedPackage: String? = null
    private var lastShieldTimestamp: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val notification = buildNotification("Monk Mode Focus Shield is active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startUsageMonitorLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val appCtx = applicationContext
        serviceScope.launch {
            val dao = AppDatabase.getInstance(appCtx).workspaceDao()
            val rules = dao.getAllBlockerRules()
            val session = dao.getActiveFocusSession()
            val detox = DetoxRuntimeStore.read(appCtx)
            if (MonkBlockerEngine.hasAnyActiveMonitoringNeed(rules, session, detoxState = detox)) {
                MonkWatchdogWorker.enqueueImmediateCheck(appCtx)
                val restartIntent = Intent(appCtx, MonkBlockerService::class.java)
                val pending = PendingIntent.getService(
                    appCtx,
                    7091,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                am?.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1_500L,
                    pending
                )
            }
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startUsageMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).workspaceDao()
            val homePkgs = MonkBlockerEngine.resolveHomeLauncherPackages(applicationContext)
            while (isActive) {
                try {
                    val rules = dao.getAllBlockerRules()
                    val session = dao.getActiveFocusSession()
                    val now = System.currentTimeMillis()
                    val detox = DetoxRuntimeStore.read(applicationContext, now)
                    if (!MonkBlockerEngine.hasAnyActiveMonitoringNeed(rules, session, now, detox)) {
                        stopSelf()
                        break
                    }

                    val foregroundPkg = queryCurrentForegroundPackage(now)
                    if (!foregroundPkg.isNullOrBlank()) {
                        val match = MonkBlockerEngine.evaluatePackageBlock(
                            packageName = foregroundPkg,
                            selfPackageName = packageName,
                            rules = rules,
                            activeFocusSession = session,
                            nowMillis = now,
                            detoxState = detox,
                            homeLauncherPackages = homePkgs
                        )
                        if (match != null && shouldTriggerShield(foregroundPkg, now)) {
                            lastShieldedPackage = foregroundPkg
                            lastShieldTimestamp = now
                            MonkBlockerEngine.launchFocusShield(applicationContext, match)
                        }
                    }
                } catch (_: Exception) {
                    // Keep monitor resilient
                }
                delay(1_400L)
            }
        }
    }

    private fun shouldTriggerShield(pkg: String, nowMs: Long): Boolean {
        return pkg != lastShieldedPackage || (nowMs - lastShieldTimestamp) > 2_500L
    }

    private fun queryCurrentForegroundPackage(nowMs: Long): String? {
        val status = MonkBlockerEngine.readPermissionsStatus(this)
        if (!status.hasUsageStats) return null
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        return try {
            val events = usm.queryEvents(nowMs - 10_000L, nowMs)
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    @Suppress("DEPRECATION")
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }
                if (isForeground && event.timeStamp >= latestTime) {
                    latestTime = event.timeStamp
                    latestPkg = event.packageName
                }
            }
            latestPkg
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.monk_service_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps scheduled app blocks and Monk Mode active"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(subtitle: String): Notification {
        val openPending = PendingIntent.getActivity(
            this,
            901,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Monk Mode Active")
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "monk_mode_shield_channel"
        private const val NOTIFICATION_ID = 4401
    }
}
