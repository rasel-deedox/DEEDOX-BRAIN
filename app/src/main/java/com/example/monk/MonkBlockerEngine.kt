package com.example.monk

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.example.data.local.AppDatabase
import com.example.data.local.BlockerRuleEntity
import com.example.data.local.DetoxRuntimeStore
import com.example.data.local.DetoxSessionState
import com.example.data.local.FocusSessionEntity
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class BlockerMatchResult(
    val ruleId: String,
    val ruleTitle: String,
    val blockedPackage: String,
    val mode: String,
    val remainingMillis: Long?
)

data class MonkPermissionsStatus(
    val hasNotifications: Boolean,
    val hasExactAlarms: Boolean,
    val hasUsageStats: Boolean,
    val hasOverlay: Boolean,
    val hasAccessibility: Boolean,
    val hasBatteryExemption: Boolean
)

object MonkBlockerEngine {

    const val MODE_SCHEDULED = "SCHEDULED"
    const val MODE_COUNTDOWN_DELAY = "COUNTDOWN_DELAY"
    const val MODE_INSTANT = "INSTANT"
    const val MODE_DIGITAL_DETOX = "DIGITAL_DETOX"
    const val DETOX_RULE_ID = "digital_detox_lock"

    private const val PREFS_NAME = "monk_blocker_runtime"
    private const val KEY_CACHE_PACKAGES = "cached_blocked_pkgs_json"

    private val SAFE_SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    fun parsePackages(json: String): Set<String> {
        if (json.isBlank() || json == "[]") return emptySet()
        return try {
            val arr = JSONArray(json)
            buildSet {
                for (i in 0 until arr.length()) {
                    val pkg = arr.optString(i).trim()
                    if (pkg.isNotBlank()) add(pkg)
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun encodePackages(packages: Collection<String>): String {
        val arr = JSONArray()
        packages.distinct().sorted().forEach { arr.put(it) }
        return arr.toString()
    }

    fun parseDaysOfWeek(csv: String): Set<Int> {
        if (csv.isBlank()) return setOf(1, 2, 3, 4, 5, 6, 7)
        return csv.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
            .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
    }

    fun isoDayOfWeek(calendar: Calendar): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun isScheduledWindowActive(
        rule: BlockerRuleEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val minuteNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayIso = isoDayOfWeek(cal)
        val allowedDays = parseDaysOfWeek(rule.daysOfWeekCsv)
        if (dayIso !in allowedDays) return false

        val start = rule.startMinuteOfDay.coerceIn(0, 1439)
        val end = rule.endMinuteOfDay.coerceIn(0, 1439)
        return when {
            start == end -> true
            start < end -> minuteNow in start until end
            else -> minuteNow >= start || minuteNow < end // Overnight window
        }
    }

    fun isRuleActiveNow(
        rule: BlockerRuleEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!rule.isEnabled) return false
        val expired = rule.activeUntilMillis != null && nowMillis > rule.activeUntilMillis
        if (expired) return false

        return when (rule.mode) {
            MODE_INSTANT -> true
            MODE_COUNTDOWN_DELAY -> {
                val triggerAt = rule.delayTriggerAtMillis ?: return false
                nowMillis >= triggerAt
            }
            MODE_SCHEDULED -> isScheduledWindowActive(rule, nowMillis)
            else -> false
        }
    }

    fun evaluatePackageBlock(
        packageName: String,
        selfPackageName: String,
        rules: List<BlockerRuleEntity>,
        activeFocusSession: FocusSessionEntity?,
        nowMillis: Long = System.currentTimeMillis(),
        detoxState: DetoxSessionState = DetoxSessionState(),
        homeLauncherPackages: Set<String> = emptySet()
    ): BlockerMatchResult? {
        if (packageName.isBlank() ||
            packageName == selfPackageName ||
            packageName in SAFE_SYSTEM_PACKAGES ||
            packageName in homeLauncherPackages
        ) {
            return null
        }

        val sessionActive = activeFocusSession != null && activeFocusSession.endedAt == null &&
            nowMillis < (activeFocusSession.startedAt + activeFocusSession.plannedMinutes * 60_000L)

        // Check explicitly active rules first
        for (rule in rules) {
            if (!rule.isEnabled) continue
            val pkgs = parsePackages(rule.blockedPackagesJson)
            if (packageName !in pkgs) continue

            if (isRuleActiveNow(rule, nowMillis)) {
                val remaining = rule.activeUntilMillis?.let { (it - nowMillis).coerceAtLeast(0L) }
                return BlockerMatchResult(
                    ruleId = rule.id,
                    ruleTitle = rule.title,
                    blockedPackage = packageName,
                    mode = rule.mode,
                    remainingMillis = remaining
                )
            }

            // During an active Focus Session, enforce any enabled rule's selected apps immediately
            if (sessionActive && activeFocusSession != null) {
                val endAt = activeFocusSession.startedAt + activeFocusSession.plannedMinutes * 60_000L
                return BlockerMatchResult(
                    ruleId = rule.id,
                    ruleTitle = activeFocusSession.label,
                    blockedPackage = packageName,
                    mode = "FOCUS_SESSION",
                    remainingMillis = (endAt - nowMillis).coerceAtLeast(0L)
                )
            }
        }

        // Digital Detox full lockdown blocks all non-whitelisted apps
        if (detoxState.isRunningAt(nowMillis)) {
            if (packageName !in detoxState.whitelistedSet()) {
                return BlockerMatchResult(
                    ruleId = DETOX_RULE_ID,
                    ruleTitle = "${detoxState.durationHours}h Digital Detox Lockdown",
                    blockedPackage = packageName,
                    mode = MODE_DIGITAL_DETOX,
                    remainingMillis = (detoxState.endsAtMillis - nowMillis).coerceAtLeast(0L)
                )
            }
        }
        return null
    }

    fun hasAnyActiveMonitoringNeed(
        rules: List<BlockerRuleEntity>,
        activeFocusSession: FocusSessionEntity?,
        nowMillis: Long = System.currentTimeMillis(),
        detoxState: DetoxSessionState = DetoxSessionState()
    ): Boolean {
        if (detoxState.isRunningAt(nowMillis)) return true
        if (activeFocusSession != null && activeFocusSession.endedAt == null) return true
        return rules.any { it.isEnabled && (it.activeUntilMillis == null || nowMillis <= it.activeUntilMillis) }
    }

    fun resolveHomeLauncherPackages(context: Context): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { it.activityInfo?.packageName?.trim()?.takeIf { p -> p.isNotBlank() } }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 1439)
        val h24 = clamped / 60
        val min = clamped % 60
        val suffix = if (h24 >= 12) "PM" else "AM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format("%d:%02d %s", h12, min, suffix)
    }

    fun readPermissionsStatus(context: Context): MonkPermissionsStatus {
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        val hasExact = com.example.reminders.ReminderScheduler.canScheduleExact(context)

        val hasUsage = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = if (appOps != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                }
            } else AppOpsManager.MODE_IGNORED
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }

        val hasOverlay = Settings.canDrawOverlays(context)

        val hasAccessibility = try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val expected = "${context.packageName}/${MonkAccessibilityService::class.java.name}"
            val shortName = "${context.packageName}/.monk.MonkAccessibilityService"
            enabled.contains(expected) || enabled.contains(shortName)
        } catch (_: Exception) {
            false
        }

        val hasBattery = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } catch (_: Exception) {
            false
        }

        return MonkPermissionsStatus(
            hasNotifications = hasNotif,
            hasExactAlarms = hasExact,
            hasUsageStats = hasUsage,
            hasOverlay = hasOverlay,
            hasAccessibility = hasAccessibility,
            hasBatteryExemption = hasBattery
        )
    }

    suspend fun evaluateFromDatabase(context: Context, packageName: String): BlockerMatchResult? {
        val db = AppDatabase.getInstance(context.applicationContext)
        val dao = db.workspaceDao()
        val rules = dao.getAllBlockerRules()
        val session = dao.getActiveFocusSession()
        val now = System.currentTimeMillis()
        val detox = DetoxRuntimeStore.read(context, now)
        val homePkgs = resolveHomeLauncherPackages(context)
        return evaluatePackageBlock(
            packageName = packageName,
            selfPackageName = context.packageName,
            rules = rules,
            activeFocusSession = session,
            nowMillis = now,
            detoxState = detox,
            homeLauncherPackages = homePkgs
        )
    }

    fun launchFocusShield(context: Context, match: BlockerMatchResult) {
        val intent = Intent(context, FocusShieldActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(FocusShieldActivity.EXTRA_RULE_ID, match.ruleId)
            putExtra(FocusShieldActivity.EXTRA_RULE_TITLE, match.ruleTitle)
            putExtra(FocusShieldActivity.EXTRA_BLOCKED_PACKAGE, match.blockedPackage)
            putExtra(FocusShieldActivity.EXTRA_BLOCK_MODE, match.mode)
            putExtra(FocusShieldActivity.EXTRA_REMAINING_MS, match.remainingMillis ?: -1L)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun ensureMonitorRunning(context: Context) {
        val appCtx = context.applicationContext
        MonkWatchdogWorker.scheduleWatchdog(appCtx)
        try {
            val serviceIntent = Intent(appCtx, MonkBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(serviceIntent)
            } else {
                appCtx.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // If Android 15 background start limit blocks direct start, Watchdog/Accessibility still enforces
        }
    }

    fun stopMonitorIfIdle(context: Context) {
        val appCtx = context.applicationContext
        try {
            appCtx.stopService(Intent(appCtx, MonkBlockerService::class.java))
        } catch (_: Exception) {
        }
    }
}
