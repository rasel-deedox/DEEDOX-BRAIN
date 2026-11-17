package com.example.monk

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class LaunchableAppInfo(
    val packageName: String,
    val label: String,
    val iconUri: String? = null,
    val isSuggestedDistraction: Boolean = false
)

object InstalledAppsRepository {

    private val DISTRACTION_KEYWORDS = listOf(
        "instagram", "tiktok", "musically", "youtube", "facebook", "twitter",
        "x.android", "reddit", "snapchat", "netflix", "twitch", "discord",
        "pinterest", "threads", "whatsapp", "telegram", "game", "chrome",
        "browser", "roblox", "pubg", "spotify", "reel", "shorts"
    )

    suspend fun loadLaunchableApps(context: Context): List<LaunchableAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPkg = context.packageName
        val iconCacheDir = File(context.cacheDir, "app_icons").apply { mkdirs() }

        val discovered = LinkedHashMap<String, String>()

        // 1) All launcher activities (MATCH_ALL ensures Android 11+ returns all launchable apps declared in <queries>)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }
        for (info in resolvedLauncher) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == selfPkg || discovered.containsKey(pkg)) continue
            val label = runCatching { info.loadLabel(pm)?.toString()?.trim() }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: pkg
            discovered[pkg] = label
        }

        // 2) Also include all launchable installed user apps via getInstalledApplications
        val installedApps = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        }.getOrDefault(emptyList())

        for (appInfo in installedApps) {
            val pkg = appInfo.packageName ?: continue
            if (pkg == selfPkg || discovered.containsKey(pkg)) continue
            val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val hasLaunchIntent = pm.getLaunchIntentForPackage(pkg) != null
            if (!isUserApp && !hasLaunchIntent) continue
            val label = runCatching { pm.getApplicationLabel(appInfo)?.toString()?.trim() }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: pkg
            discovered[pkg] = label
        }

        val apps = ArrayList<LaunchableAppInfo>(discovered.size)
        for ((pkg, label) in discovered) {
            val iconUri = cacheAppIcon(pm, pkg, iconCacheDir)
            val lower = "$pkg $label".lowercase()
            val suggested = DISTRACTION_KEYWORDS.any { lower.contains(it) }
            apps.add(
                LaunchableAppInfo(
                    packageName = pkg,
                    label = label,
                    iconUri = iconUri,
                    isSuggestedDistraction = suggested
                )
            )
        }

        apps.sortedWith(
            compareByDescending<LaunchableAppInfo> { it.isSuggestedDistraction }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun cacheAppIcon(
        pm: PackageManager,
        packageName: String,
        iconCacheDir: File
    ): String? {
        val safeName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(iconCacheDir, "$safeName.png")
        if (outFile.exists() && outFile.length() > 0L) {
            return Uri.fromFile(outFile).toString()
        }
        return runCatching {
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable, targetSizePx = 96)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, stream)
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable, targetSizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val src = drawable.bitmap
            return if (src.width == targetSizePx && src.height == targetSizePx) {
                src.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                Bitmap.createScaledBitmap(src, targetSizePx, targetSizePx, true)
            }
        }
        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
