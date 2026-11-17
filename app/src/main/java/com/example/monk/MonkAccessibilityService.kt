package com.example.monk

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MonkAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastCheckedPkg: String? = null

    @Volatile
    private var lastCheckedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()?.trim().orEmpty()
        if (pkg.isBlank() || pkg == packageName) return

        val now = System.currentTimeMillis()
        if (pkg == lastCheckedPkg && (now - lastCheckedAt) < 1_000L) return
        lastCheckedPkg = pkg
        lastCheckedAt = now

        scope.launch {
            try {
                val match = MonkBlockerEngine.evaluateFromDatabase(applicationContext, pkg)
                if (match != null) {
                    MonkBlockerEngine.launchFocusShield(applicationContext, match)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
