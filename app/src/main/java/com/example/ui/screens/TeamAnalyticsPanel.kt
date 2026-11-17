package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.repository.DayVelocityStat
import com.example.data.repository.MemberPerformanceStat
import com.example.data.repository.TeamAnalyticsSummary
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.TactileGlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

@Composable
fun TeamMemberAvatar(
    displayName: String,
    photoUrl: String,
    isOnline: Boolean,
    size: Dp = 38.dp,
    showOnlineDot: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val initials = displayName.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "M" }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(palette.accentSoftBg)
                .border(1.2.dp, palette.specularRimOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "$displayName avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentSoftInk
                )
            }
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.28f).coerceAtLeast(8.dp))
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF10B981) else palette.mutedIcon)
                    .border(1.5.dp, palette.cardSurfaceTop, CircleShape)
            )
        }
    }
}

@Composable
fun TeamCompletionGaugeCard(
    summary: TeamAnalyticsSummary,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val animatedSweep by animateFloatAsState(
        targetValue = summary.completionRatio.coerceIn(0f, 1f) * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "team_completion_sweep"
    )

    FeaturedCrimsonBronzeCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_analytics_gauge_card"),
        cornerRadius = 26.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = palette.recessedTrayBg
                Canvas(modifier = Modifier.size(104.dp)) {
                    val strokeWidth = 11.dp.toPx()
                    val pad = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = CrimsonAccent,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.completionPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Team Sprint Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )
                Text(
                    text = "${summary.doneTasks} of ${summary.totalTasks} scheduled tasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnalyticsCountPill(
                        label = "Done",
                        count = summary.doneTasks,
                        accent = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCountPill(
                        label = "Review",
                        count = summary.reviewTasks,
                        accent = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCountPill(
                        label = "Active",
                        count = (summary.inProgressTasks - summary.reviewTasks).coerceAtLeast(0),
                        accent = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnalyticsCountPill(
                        label = "To Do",
                        count = (summary.todoTasks - summary.backlogTasks).coerceAtLeast(0),
                        accent = CrimsonAccent,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCountPill(
                        label = "Backlog",
                        count = summary.backlogTasks,
                        accent = Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCountPill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = palette.cardSurfaceTop,
        border = BorderStroke(1.dp, palette.specularRimOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryInk,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MemberPerformanceBarsCard(
    memberStats: List<MemberPerformanceStat>,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_member_performance_card"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Member Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            if (memberStats.isEmpty()) {
                Text(
                    text = "Assign weekly schedule tasks to track individual completion progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk
                )
            } else {
                memberStats.forEach { stat ->
                    val animatedRatio by animateFloatAsState(
                        targetValue = stat.completionRatio.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        label = "member_bar_${stat.uid}"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamMemberAvatar(
                                    displayName = stat.displayName,
                                    photoUrl = stat.photoUrl,
                                    isOnline = true,
                                    showOnlineDot = false,
                                    size = 26.dp
                                )
                                Column {
                                    Text(
                                        text = stat.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primaryInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (stat.roleTag.isNotBlank()) {
                                        Text(
                                            text = stat.roleTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${stat.completedCount}/${stat.assignedCount} tasks",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (stat.assignedCount > 0 && stat.completedCount == stat.assignedCount) {
                                    Color(0xFF10B981)
                                } else {
                                    CrimsonAccent
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(palette.recessedTrayBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (animatedRatio > 0f) animatedRatio.coerceAtLeast(0.04f) else 0f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(
                                        if (stat.assignedCount > 0 && stat.completedCount == stat.assignedCount) {
                                            Color(0xFF10B981)
                                        } else {
                                            CrimsonAccent
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyVelocityChartCard(
    weeklyVelocity: List<DayVelocityStat>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val maxDayCount = (weeklyVelocity.maxOfOrNull { it.totalCount } ?: 1).coerceAtLeast(1)

    TactileGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_weekly_velocity_card"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Velocity (Sat – Fri)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Tap a day bar to inspect scheduled tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VelocityLegendDot(label = "Done", color = Color(0xFF10B981))
                    VelocityLegendDot(label = "Scheduled", color = CrimsonAccent.copy(alpha = 0.35f))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyVelocity.forEach { dayStat ->
                    val totalHeightFraction = (dayStat.totalCount.toFloat() / maxDayCount.toFloat()).coerceIn(0f, 1f)
                    val doneHeightFraction = (dayStat.doneCount.toFloat() / maxDayCount.toFloat()).coerceIn(0f, 1f)
                    val animTotal by animateFloatAsState(
                        targetValue = totalHeightFraction,
                        animationSpec = tween(450, easing = FastOutSlowInEasing),
                        label = "vel_total_${dayStat.dayOfWeek}"
                    )
                    val animDone by animateFloatAsState(
                        targetValue = doneHeightFraction,
                        animationSpec = tween(550, easing = FastOutSlowInEasing),
                        label = "vel_done_${dayStat.dayOfWeek}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDaySelected(dayStat.dayOfWeek) }
                            .padding(vertical = 4.dp)
                            .testTag("velocity_bar_${dayStat.dayOfWeek}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = if (dayStat.totalCount > 0) "${dayStat.doneCount}/${dayStat.totalCount}" else "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.58f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.recessedTrayBg),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (animTotal > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animTotal.coerceAtLeast(0.08f))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CrimsonAccent.copy(alpha = 0.35f))
                                )
                            }
                            if (animDone > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animDone.coerceAtLeast(0.08f))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                        Text(
                            text = dayStat.dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VelocityLegendDot(label: String, color: Color) {
    val palette = LiquidTheme.palette
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
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
