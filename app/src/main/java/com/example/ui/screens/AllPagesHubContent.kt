package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppLogo
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

@Immutable
data class LauncherMetrics(
    val monkSummary: String = "App Shield • Standby",
    val isDetoxActive: Boolean = false,
    val workspaceSummary: String = "LV 1 • 0 Notes",
    val startupSummary: String = "Launch Ventures",
    val moneySummary: String = "$0.00 Net",
    val teamSummary: String = "Live Sync Workspace"
)

@Composable
fun AllPagesLauncherGrid(
    metrics: LauncherMetrics,
    onOpenMonkMode: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenStartupOs: () -> Unit,
    onOpenMoneyLedger: () -> Unit,
    onOpenTeamCollab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("all_pages_bento_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TactileGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 26.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogo(
                    size = 38.dp,
                    cornerRadius = 12.dp,
                    testTag = "hub_launcher_app_logo"
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Personal OS Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Launch focus shields, Obsidian notes, startups, finance & live team rooms",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.accentSoftBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Text(
                        text = "5 OS Apps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentSoftInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Row 1: Monk Mode & Digital Detox | Habits & Voice Notepad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoModuleCard(
                title = "Monk Mode & Detox",
                subtitle = "App blocker, timers & strict digital detox lock",
                badgeText = metrics.monkSummary,
                iconType = MinimalIconType.SHIELD,
                highlightDot = metrics.isDetoxActive,
                testTag = "launcher_tile_monk_mode",
                onClick = onOpenMonkMode,
                modifier = Modifier.weight(1f)
            )
            BentoModuleCard(
                title = "Habits & Notepad",
                subtitle = "XP streaks, voice memos & [[Obsidian]] backlinks",
                badgeText = metrics.workspaceSummary,
                iconType = MinimalIconType.NOTEBOOK,
                highlightDot = false,
                testTag = "launcher_tile_workspace",
                onClick = onOpenWorkspace,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Startup & Business OS | Money & Finance Ledger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoModuleCard(
                title = "Startup & Business OS",
                subtitle = "6-phase roadmap, Kanban sprints & AI specs",
                badgeText = metrics.startupSummary,
                iconType = MinimalIconType.SPARKLE,
                highlightDot = false,
                testTag = "launcher_tile_startup_os",
                onClick = onOpenStartupOs,
                modifier = Modifier.weight(1f)
            )
            BentoModuleCard(
                title = "Money & Ledger",
                subtitle = "Custom sectors, date filters & live cashflow",
                badgeText = metrics.moneySummary,
                iconType = MinimalIconType.SLIDERS,
                highlightDot = false,
                testTag = "launcher_tile_money_ledger",
                onClick = onOpenMoneyLedger,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Featured Team & Group Collaboration Launcher
        FeaturedCrimsonBronzeCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launcher_tile_team_collab"),
            cornerRadius = 28.dp,
            onClick = onOpenTeamCollab
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.frostedPillBg)
                        .border(1.dp, palette.specularRimOutline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = MinimalIconType.SHARE,
                        active = true,
                        tint = CrimsonAccent,
                        size = 22.dp,
                        contentDescription = "Team Collaboration"
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Team & Group Collab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.frostedPillBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = metrics.teamSummary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Real-time shared docs, @member task assignment, invite codes & instant cross-device team alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MinimalIcon(
                    type = MinimalIconType.ARROW_UP_RIGHT,
                    active = true,
                    tint = palette.primaryInk,
                    size = 20.dp,
                    contentDescription = "Open Team Workspace"
                )
            }
        }
    }
}

@Composable
private fun BentoModuleCard(
    title: String,
    subtitle: String,
    badgeText: String,
    iconType: MinimalIconType,
    highlightDot: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(
        modifier = modifier.testTag(testTag),
        cornerRadius = 26.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accentSoftBg)
                        .border(1.dp, palette.specularRimOutline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = iconType,
                        active = true,
                        tint = palette.accentSoftInk,
                        size = 20.dp,
                        contentDescription = title
                    )
                }
                MinimalIcon(
                    type = MinimalIconType.ARROW_UP_RIGHT,
                    active = false,
                    tint = palette.secondaryInk,
                    size = 18.dp,
                    contentDescription = null
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = if (highlightDot) CrimsonAccent.copy(alpha = 0.14f) else palette.recessedTrayBg,
                border = BorderStroke(
                    1.dp,
                    if (highlightDot) CrimsonAccent.copy(alpha = 0.45f) else palette.specularRimOutline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (highlightDot) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CrimsonAccent)
                        )
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (highlightDot) CrimsonAccent else palette.primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
