package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.minimumInteractiveComponentSize
import coil.compose.AsyncImage
import com.example.data.cloud.CloudDeviceSession
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.ItemEntity
import com.example.data.util.UrlNormalizer
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.TelemetryCrimsonDot
import com.example.ui.theme.TelemetryModerateDot
import com.example.ui.theme.TelemetryOptimalDot
import com.example.ui.theme.TerracottaBottom
import com.example.ui.theme.TerracottaTop
import com.example.ui.viewmodel.LibrarySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

@Immutable
private data class HeatmapCell(
    val dateLabel: String,
    val count: Int,
    val level: Int
)

private fun greetingForCurrentHour(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

private fun dayKeyForMillis(millis: Long, reusableCal: Calendar = Calendar.getInstance()): Long {
    reusableCal.timeInMillis = millis
    reusableCal.set(Calendar.HOUR_OF_DAY, 0)
    reusableCal.set(Calendar.MINUTE, 0)
    reusableCal.set(Calendar.SECOND, 0)
    reusableCal.set(Calendar.MILLISECOND, 0)
    return reusableCal.timeInMillis
}

@Composable
fun ProfileAvatarBadge(
    avatarValue: String,
    initial: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val gradientColors = remember(avatarValue, palette) {
        when (avatarValue) {
            "preset:crimson" -> listOf(Color(0xFFE53935), Color(0xFF8E1B18))
            "preset:obsidian" -> listOf(Color(0xFF3A3533), Color(0xFF141214))
            "preset:pearl" -> listOf(Color(0xFFE8DFD3), Color(0xFFC2B3A1))
            else -> listOf(palette.bronzeCardTop, palette.accentTerracottaBottom)
        }
    }
    val textColor = if (avatarValue == "preset:pearl") Color(0xFF181715) else Color.White
    val isImageUri = avatarValue.startsWith("http", ignoreCase = true) ||
        avatarValue.startsWith("file:", ignoreCase = true) ||
        avatarValue.startsWith("content:", ignoreCase = true)

    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = palette.accentCrimson.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors))
            .border(1.4.dp, palette.specularBorderTop, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isImageUri) {
            AsyncImage(
                model = avatarValue,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial.take(1).ifBlank { "A" }.uppercase(),
                style = if (size >= 60.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun HomeHeaderBar(
    userDisplayName: String,
    profileAvatar: String = "preset:terracotta",
    isSignedIn: Boolean,
    isOnline: Boolean,
    themeMode: AppThemeMode,
    syncBadgeState: SyncBadgeState,
    notificationCount: Int,
    showFiltersExpanded: Boolean,
    onProfileAvatarClick: () -> Unit = {},
    onToggleFiltersExpanded: () -> Unit,
    onBellClick: () -> Unit,
    onOpenDeviceSyncModal: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onOpenSyncSettings: () -> Unit
) {
    val palette = LiquidTheme.palette
    val firstName = remember(userDisplayName) {
        userDisplayName.trim().substringBefore(' ').ifBlank { "Creator" }
    }
    val initial = firstName.take(1).uppercase()
    val greeting = remember { greetingForCurrentHour() }
    val avatarInteraction = remember { MutableInteractionSource() }
    val avatarPressed by avatarInteraction.collectIsPressedAsState()
    val avatarScale by animateFloatAsState(
        targetValue = if (avatarPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "homeAvatarScale"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = avatarInteraction,
                        indication = androidx.compose.material3.ripple(),
                        onClick = onProfileAvatarClick
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Open profile, avatar, and account settings"
                    }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
                    .testTag("home_profile_avatar_button")
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                    }
                ) {
                    ProfileAvatarBadge(
                        avatarValue = profileAvatar,
                        initial = initial,
                        size = 46.dp
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(palette.canvasTop)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (isSignedIn) TelemetryOptimalDot else palette.accentCrimson)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.primaryInk,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dot = if (isOnline) TelemetryOptimalDot else TelemetryCrimsonDot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dot)
                        )
                        Text(
                            text = if (isSignedIn) "Live Sync Active" else "Local Vault • Offline FTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingGlassIconButton(
                    iconType = MinimalIconType.SLIDERS,
                    contentDescription = "Toggle quick filters",
                    active = showFiltersExpanded,
                    tint = if (showFiltersExpanded) palette.accentCrimson else palette.primaryInk,
                    onClick = onToggleFiltersExpanded,
                    modifier = Modifier.testTag("home_sliders_filter_button")
                )

                Box {
                    FloatingGlassIconButton(
                        iconType = MinimalIconType.BELL,
                        contentDescription = "Inbox & transfer notifications",
                        active = notificationCount > 0,
                        tint = palette.primaryInk,
                        onClick = onBellClick,
                        modifier = Modifier.testTag("home_notifications_bell")
                    )
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(18.dp)
                                .shadow(6.dp, CircleShape, ambientColor = palette.accentCrimson, spotColor = palette.accentCrimson)
                                .clip(CircleShape)
                                .background(palette.accentCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 9) "9+" else "$notificationCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                FloatingGlassIconButton(
                    iconType = MinimalIconType.DEVICE_SYNC,
                    contentDescription = "Sync and send to PC or phone",
                    tint = palette.primaryInk,
                    onClick = onOpenDeviceSyncModal,
                    modifier = Modifier.testTag("home_device_sync_button")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "You're building\nsomething great.",
                style = MaterialTheme.typography.displaySmall,
                color = palette.primaryInk,
                modifier = Modifier.weight(1f)
            )
            AppLogo(
                size = 42.dp,
                cornerRadius = 13.dp,
                testTag = "home_header_app_logo"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidThemePillSwitcher(
                selectedMode = themeMode,
                onSelectMode = onSelectThemeMode,
                modifier = Modifier.weight(1f)
            )
            SyncStatusPill(
                badgeState = syncBadgeState,
                onClick = onOpenDeviceSyncModal
            )
            Surface(
                onClick = onOpenSyncSettings,
                shape = RoundedCornerShape(9999.dp),
                color = palette.frostedPillBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier.testTag("home_settings_pill")
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
fun StreakAndHeatmapSection(
    snapshot: LibrarySnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val countsByDay = remember(snapshot.items, snapshot.notes, snapshot.auditLogs) {
        val cal = Calendar.getInstance()
        val map = HashMap<Long, Int>(64)
        snapshot.items.forEach {
            val k = dayKeyForMillis(it.createdAt, cal)
            map[k] = (map[k] ?: 0) + 1
        }
        snapshot.notes.forEach {
            val k = dayKeyForMillis(it.updatedAt, cal)
            map[k] = (map[k] ?: 0) + 1
        }
        snapshot.auditLogs.forEach {
            val k = dayKeyForMillis(it.createdAt, cal)
            map[k] = (map[k] ?: 0) + 1
        }
        map
    }

    val totalActiveDays = countsByDay.size
    val currentStreak = remember(countsByDay) {
        val cal = Calendar.getInstance()
        val today = dayKeyForMillis(cal.timeInMillis, cal)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dayKeyForMillis(cal.timeInMillis, cal)
        var cursor = when {
            (countsByDay[today] ?: 0) > 0 -> today
            (countsByDay[yesterday] ?: 0) > 0 -> yesterday
            else -> return@remember 0
        }
        var streak = 0
        cal.timeInMillis = cursor
        while ((countsByDay[cursor] ?: 0) > 0 && streak < 365) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cursor = dayKeyForMillis(cal.timeInMillis, cal)
        }
        streak
    }

    val matrixDays = remember(countsByDay) {
        val totalCells = 15 * 7 // 15 columns x 7 rows = 105 days
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("MMM d", Locale.US)
        val endLabel = fmt.format(Date(cal.timeInMillis))
        cal.add(Calendar.DAY_OF_YEAR, -(totalCells - 1))
        val startLabel = fmt.format(Date(cal.timeInMillis))

        val list = ArrayList<HeatmapCell>(totalCells)
        for (offset in 0 until totalCells) {
            val key = dayKeyForMillis(cal.timeInMillis, cal)
            val count = countsByDay[key] ?: 0
            val level = when {
                count == 0 -> 0
                count == 1 -> 1
                count == 2 -> 2
                count <= 4 -> 3
                else -> 4
            }
            list.add(HeatmapCell(dateLabel = fmt.format(Date(key)), count = count, level = level))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list to "$startLabel – $endLabel"
    }

    var selectedDayTooltip by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Two side-by-side glass stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TactileGlassCard(modifier = Modifier.weight(1f), cornerRadius = 26.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Current Streaks",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryInk
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = if (currentStreak == 1) "day" else "days",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accentSoftInk,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            TactileGlassCard(modifier = Modifier.weight(1f), cornerRadius = 26.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total Active Days",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryInk
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$totalActiveDays",
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "${snapshot.items.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        // 7x15 Activity Heatmap Matrix Card
        TactileGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_activity_heatmap"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activity Matrix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = selectedDayTooltip ?: matrixDays.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Less", style = MaterialTheme.typography.labelSmall, color = palette.mutedIcon)
                        palette.heatmapLevels.forEach { lvlColor ->
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(lvlColor)
                            )
                        }
                        Text("More", style = MaterialTheme.typography.labelSmall, color = palette.mutedIcon)
                    }
                }

                // Single-pass Canvas 15x7 grid + single tap detector (replaces 105 layout/clickable nodes)
                val cells = matrixDays.first
                val heatmapLevels = palette.heatmapLevels
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp * 7 + 4.dp * 6)
                        .pointerInput(cells) {
                            detectTapGestures { offset ->
                                val cols = 15
                                val rows = 7
                                val gapPx = 4.dp.toPx()
                                val cellW = (size.width - gapPx * (cols - 1)) / cols
                                val cellH = (size.height - gapPx * (rows - 1)) / rows
                                val col = (offset.x / (cellW + gapPx)).toInt().coerceIn(0, cols - 1)
                                val row = (offset.y / (cellH + gapPx)).toInt().coerceIn(0, rows - 1)
                                val cell = cells[col * rows + row]
                                selectedDayTooltip = "${cell.dateLabel} • ${cell.count} activity event(s)"
                            }
                        }
                        .drawWithCache {
                            val cols = 15
                            val rows = 7
                            val gapPx = 4.dp.toPx()
                            val cellW = ((size.width - gapPx * (cols - 1)) / cols).coerceAtLeast(1f)
                            val cellH = ((size.height - gapPx * (rows - 1)) / rows).coerceAtLeast(1f)
                            val cellSize = Size(cellW, cellH)
                            val cr = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            val cellOffsets = Array(cols * rows) { idx ->
                                val col = idx / rows
                                val row = idx % rows
                                Offset(col * (cellW + gapPx), row * (cellH + gapPx))
                            }
                            onDrawBehind {
                                for (idx in cells.indices) {
                                    drawRoundRect(
                                        color = heatmapLevels[cells[idx].level],
                                        topLeft = cellOffsets[idx],
                                        size = cellSize,
                                        cornerRadius = cr
                                    )
                                }
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun AiAssistantAndFocusCards(
    snapshot: LibrarySnapshot,
    inboxCount: Int,
    isOnline: Boolean,
    isAiBusy: Boolean,
    onAskAiClick: () -> Unit,
    onOrganizeInboxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val totalCount = snapshot.items.size
    val organizedCount = (totalCount - inboxCount).coerceAtLeast(0)
    val organizedPct = if (totalCount == 0) 0 else ((organizedCount * 100f) / totalCount).toInt().coerceIn(0, 100)

    val last5DaysCapsules = remember(snapshot.items) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("dd", Locale.US)
        val itemDayCounts = HashMap<Long, Int>(8)
        snapshot.items.forEach {
            val k = dayKeyForMillis(it.createdAt, cal)
            itemDayCounts[k] = (itemDayCounts[k] ?: 0) + 1
        }
        cal.timeInMillis = System.currentTimeMillis()
        cal.add(Calendar.DAY_OF_YEAR, -4)
        val counts = List(5) {
            val dayKey = dayKeyForMillis(cal.timeInMillis, cal)
            val label = fmt.format(Date(dayKey))
            val c = itemDayCounts[dayKey] ?: 0
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label to c
        }
        val maxVal = (counts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        counts.map { (label, c) ->
            val frac = if (c == 0) 0.22f else (0.32f + 0.68f * (c.toFloat() / maxVal)).coerceIn(0.22f, 1f)
            label to frac
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Your AI Assistant (with Custom 3D Frosted Glass Orb Illustration)
        TactileGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_open_assistant_card"),
            cornerRadius = 28.dp,
            onClick = onAskAiClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "$inboxCount unsorted items • $totalCount total saved • ${snapshot.collections.size} collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                    AccentActionPill(
                        text = "Ask AI Anything",
                        enabled = true,
                        onClick = onAskAiClick,
                        modifier = Modifier.testTag("home_ask_ai_pill")
                    )
                }

                // Custom 3D Glass Orb with Glowing "AI" Crimson-Terracotta Core (cached shaders)
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithCache {
                                val c = Offset(size.width * 0.5f, size.height * 0.5f)
                                val outerR = (size.minDimension * 0.52f).coerceAtLeast(1f)
                                val coreR = (size.minDimension * 0.28f).coerceAtLeast(1f)
                                val outerBrush = Brush.radialGradient(
                                    colors = listOf(
                                        palette.accentCrimson.copy(alpha = 0.42f),
                                        palette.accentTerracottaTop.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    center = c,
                                    radius = outerR
                                )
                                val coreBrush = Brush.linearGradient(
                                    listOf(palette.accentTerracottaTop, palette.accentTerracottaBottom)
                                )
                                val ringTopLeft = Offset(size.width * 0.08f, size.height * 0.30f)
                                val ringSize = Size(size.width * 0.84f, size.height * 0.40f)
                                val ringStroke = Stroke(width = 1.6.dp.toPx())
                                onDrawBehind {
                                    drawCircle(brush = outerBrush, center = c, radius = outerR)
                                    drawCircle(brush = coreBrush, center = c, radius = coreR)
                                    drawOval(
                                        color = palette.specularBorderTop,
                                        topLeft = ringTopLeft,
                                        size = ringSize,
                                        style = ringStroke
                                    )
                                }
                            }
                    )
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        // Card 2: Today's Focus & Progress (5 Vertical Liquid Glass Capsule Gauges)
        TactileGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Focus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = if (totalCount == 0) "Capture items to visualize daily vault flow"
                            else "$organizedCount of $totalCount resources organized cleanly",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                    Text(
                        text = "$organizedPct%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.primaryInk
                    )
                }

                VerticalLiquidCapsuleGauges(values = last5DaysCapsules)

                CrimsonActionPill(
                    text = if (isAiBusy) "Planning Organization..." else "Organize Inbox Using AI",
                    enabled = !isAiBusy && snapshot.items.isNotEmpty() && isOnline && snapshot.settings.aiEnabled,
                    onClick = onOrganizeInboxClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_organize_inbox_button")
                )
            }
        }
    }
}

@Composable
fun QuickCaptureAndBentoGrid(
    onSaveQuickInput: (String) -> Unit,
    onOpenFullCapture: () -> Unit,
    onSpeakCapture: () -> Unit,
    onUploadMedia: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenSearch: () -> Unit,
    onFilterNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var quickInput by remember { mutableStateOf("") }
    val detectedLink = remember(quickInput) { UrlNormalizer.extractFirstWebUrl(quickInput) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Capture Frosted Glass Box
        TactileGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Capture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )

                OutlinedTextField(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    placeholder = {
                        Text(
                            text = "Paste a video link, URL, or note...",
                            color = palette.secondaryInk
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (pasted.isNotEmpty()) {
                                    quickInput = pasted
                                }
                            },
                            modifier = Modifier.testTag("quick_capture_paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = "Paste link from clipboard",
                                tint = CrimsonAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (quickInput.isNotBlank()) {
                                onSaveQuickInput(quickInput.trim())
                                quickInput = ""
                            } else {
                                onOpenFullCapture()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.specularRimOutline,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_capture_input")
                )

                AnimatedVisibility(visible = detectedLink != null) {
                    Text(
                        text = "Link detected • Title, description & main thumbnail will auto-scrape on save",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentSoftInk
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapturePillAction(
                        label = if (quickInput.isBlank()) "Type" else "Save",
                        iconType = MinimalIconType.EDIT,
                        highlighted = quickInput.isNotBlank(),
                        onClick = {
                            if (quickInput.isNotBlank()) {
                                onSaveQuickInput(quickInput.trim())
                                quickInput = ""
                            } else {
                                onOpenFullCapture()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_capture_type_button")
                    )
                    CapturePillAction(
                        label = "Speak",
                        iconType = MinimalIconType.MIC,
                        highlighted = false,
                        onClick = onSpeakCapture,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_capture_speak_button")
                    )
                    CapturePillAction(
                        label = "Upload",
                        iconType = MinimalIconType.PLUS,
                        highlighted = false,
                        onClick = onUploadMedia,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_capture_upload_button")
                    )
                }
            }
        }

        // 2x3 Suggested Actions Bento Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Suggested Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoActionTile(
                    title = "Research Topic",
                    subtitle = "Ask AI vault",
                    iconType = MinimalIconType.SPARKLE,
                    onClick = onOpenAiAssistant,
                    modifier = Modifier.weight(1f)
                )
                BentoActionTile(
                    title = "Generated Notes",
                    subtitle = "Browse notes",
                    iconType = MinimalIconType.EDIT,
                    onClick = onFilterNotes,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoActionTile(
                    title = "Voice Capture",
                    subtitle = "Speech to vault",
                    iconType = MinimalIconType.MIC,
                    onClick = onSpeakCapture,
                    modifier = Modifier.weight(1f)
                )
                BentoActionTile(
                    title = "Analyze Image",
                    subtitle = "Photo Picker",
                    iconType = MinimalIconType.LAYERS,
                    onClick = onUploadMedia,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoActionTile(
                    title = "Deep Search",
                    subtitle = "FTS + AI index",
                    iconType = MinimalIconType.SEARCH,
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f)
                )
                BentoActionTile(
                    title = "Save Link",
                    subtitle = "Capture URL",
                    iconType = MinimalIconType.BOOKMARK,
                    onClick = onOpenFullCapture,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CapturePillAction(
    label: String,
    iconType: MinimalIconType,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = if (highlighted) palette.primaryButtonBg else palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = 4.dp,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            MinimalIcon(
                type = iconType,
                active = highlighted,
                tint = if (highlighted) palette.primaryButtonContent else palette.primaryInk,
                cutoutColor = if (highlighted) palette.primaryButtonBg else palette.frostedPillBg,
                size = 16.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) palette.primaryButtonContent else palette.primaryInk,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BentoActionTile(
    title: String,
    subtitle: String,
    iconType: MinimalIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accentSoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = iconType,
                        active = true,
                        tint = palette.accentSoftInk,
                        cutoutColor = palette.accentSoftBg,
                        size = 18.dp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(palette.recessedTrayBg),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = MinimalIconType.ARROW_UP_RIGHT,
                        tint = palette.secondaryInk,
                        size = 14.dp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AccordionsAndTelemetrySection(
    snapshot: LibrarySnapshot,
    inboxCount: Int,
    localCount: Int,
    syncedCount: Int,
    connectedDevices: List<CloudDeviceSession>,
    onOpenItem: (String) -> Unit,
    onOpenDeviceSyncModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var recordExpanded by remember { mutableStateOf(false) }
    var syncExpanded by remember { mutableStateOf(false) }

    val trendPoints = remember(snapshot.items) {
        val now = System.currentTimeMillis()
        val buckets = MutableList(7) { 0f }
        snapshot.items.forEach { item ->
            val daysAgo = ((now - item.createdAt) / 86_400_000L).toInt().coerceIn(0, 6)
            buckets[6 - daysAgo] += 1f
        }
        val maxVal = (buckets.maxOrNull() ?: 1f).coerceAtLeast(1f)
        buckets.map { (it / maxVal).coerceIn(0.12f, 1f) }
    }

    val total = snapshot.items.size
    val organizedCount = (total - inboxCount).coerceAtLeast(0)
    val organizedProgress = if (total == 0) 0f else (organizedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Collapsible Accordion 1: Record (Recent Captures)
        TactileGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            onClick = { recordExpanded = !recordExpanded }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinimalIcon(type = MinimalIconType.LAYERS, active = true, tint = palette.accentSoftInk, size = 18.dp)
                        Text(
                            text = "Record: Recent Captures (${snapshot.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                    Text(
                        text = if (recordExpanded) "Hide" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSoftInk
                    )
                }

                AnimatedVisibility(
                    visible = recordExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (snapshot.items.isEmpty()) {
                            Text(
                                text = "No captures recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        } else {
                            snapshot.items.take(4).forEach { item ->
                                Surface(
                                    onClick = { onOpenItem(item.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = palette.primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = formatRelativeDate(item.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Accordion 2: Sync History (Connected Devices)
        TactileGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            onClick = { syncExpanded = !syncExpanded }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinimalIcon(type = MinimalIconType.DEVICE_SYNC, active = true, tint = palette.primaryInk, size = 18.dp)
                        Text(
                            text = "Sync History: Connected Devices (${connectedDevices.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                    Text(
                        text = if (syncExpanded) "Hide" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSoftInk
                    )
                }

                AnimatedVisibility(
                    visible = syncExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Outbox events pending: ${snapshot.pendingSyncCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                        connectedDevices.take(3).forEach { dev ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (dev.status == "online") TelemetryOptimalDot else palette.mutedIcon)
                                )
                                Text(
                                    text = "${dev.deviceName} (${dev.platform}) • ${dev.status}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryInk
                                )
                            }
                        }
                        Surface(
                            onClick = onOpenDeviceSyncModal,
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.primaryButtonBg
                        ) {
                            Text(
                                text = "Open Sync & Device Beam",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryButtonContent,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Featured Warm Crimson-Bronze Gradient Telemetry Card
        FeaturedCrimsonBronzeCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_telemetry_card"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library Telemetry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline)
                    ) {
                        Text(
                            text = "7D Trend",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSoftInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3 Clean Metadata Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryColumn(label = "Total Saved", value = "$total")
                    TelemetryColumn(label = "Local Refs", value = "$localCount")
                    TelemetryColumn(label = "Cloud Synced", value = "$syncedCount")
                }

                // Segmented Horizontal Dashed Trend Line (cached Path & Stroke)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .drawWithCache {
                            val stepX = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                            val yPad = 4.dp.toPx()
                            val path = Path()
                            val dotCenters = Array(trendPoints.size) { idx ->
                                val x = idx * stepX
                                val y = size.height * (1f - trendPoints[idx] * 0.78f) - yPad
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                Offset(x, y)
                            }
                            val stroke = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                            val dotR = 3.5.dp.toPx()
                            onDrawBehind {
                                drawPath(
                                    path = path,
                                    color = palette.accentCrimson,
                                    style = stroke
                                )
                                for (center in dotCenters) {
                                    drawCircle(
                                        color = palette.accentTerracottaTop,
                                        radius = dotR,
                                        center = center
                                    )
                                }
                            }
                        }
                )

                // Priority Telemetry Strip + Accent Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryDotLabel(color = TelemetryCrimsonDot, label = "Unsorted ($inboxCount)")
                        TelemetryDotLabel(color = TelemetryOptimalDot, label = "Organized ($organizedCount)")
                        TelemetryDotLabel(color = TelemetryModerateDot, label = "Pending (${snapshot.pendingSyncCount})")
                    }
                    LinearProgressIndicator(
                        progress = { organizedProgress },
                        color = palette.accentCrimson,
                        trackColor = palette.barTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(9999.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryColumn(label: String, value: String) {
    val palette = LiquidTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryInk
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = palette.primaryInk
        )
    }
}

@Composable
private fun TelemetryDotLabel(color: Color, label: String) {
    val palette = LiquidTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryInk
        )
    }
}
