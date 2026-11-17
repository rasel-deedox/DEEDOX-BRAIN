package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.repository.PollVotesCodec
import com.example.data.repository.TeamAnalyticsCalculator
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TeamTaskViewMode(val label: String) {
    SCHEDULE("Schedule"),
    KANBAN("Kanban"),
    TABLE("Table")
}

internal fun teamStatusColor(status: String): Color = when (status) {
    "done" -> Color(0xFF10B981)
    "review" -> Color(0xFF8B5CF6)
    "in_progress" -> Color(0xFFF59E0B)
    "backlog" -> Color(0xFF64748B)
    else -> CrimsonAccent
}

internal fun formatDeadlineLabel(deadlineMillis: Long?): String? {
    if (deadlineMillis == null || deadlineMillis <= 0L) return null
    return SimpleDateFormat("MMM d", Locale.US).format(Date(deadlineMillis))
}

@Composable
fun TeamTaskCard(
    task: TeamSharedTaskEntity,
    onCycleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette
    val taskStatus = TeamAnalyticsCalculator.resolvedStatus(task)
    val statusLabel = TeamAnalyticsCalculator.statusLabel(taskStatus)
    val statusColor = teamStatusColor(taskStatus)
    val assignedNames = TeamAnalyticsCalculator.assignedNames(task)
    val deadlineStr = remember(task.deadlineMillis) { formatDeadlineLabel(task.deadlineMillis) }

    TactileGlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 22.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                        Text(
                            text = "${task.dayOfWeek} · ${task.startTime} – ${task.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                        Text(
                            text = task.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (deadlineStr != null) {
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                            Text(
                                text = "Due $deadlineStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (!task.sourceBusinessId.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                            Text(
                                text = "Business OS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Surface(
                    onClick = onCycleStatus,
                    shape = RoundedCornerShape(9999.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor),
                    modifier = Modifier.testTag("task_status_pill_${task.id}")
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk,
                textDecoration = if (taskStatus == "done") TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk
                )
            }

            if (!task.linkedDriveUrl.isNullOrBlank()) {
                Surface(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.linkedDriveUrl))) }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.frostedPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalIcon(type = MinimalIconType.OPEN_EXTERNAL, tint = CrimsonAccent, size = 12.dp)
                        Text(
                            text = "Open Linked Drive / Doc",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonAccent
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (assignedNames.isEmpty()) {
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                            Text(
                                text = "Unassigned",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        assignedNames.forEach { name ->
                            Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                                Text(
                                    text = "@$name",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentSoftInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        MinimalIcon(type = MinimalIconType.EDIT, tint = palette.primaryInk, size = 16.dp, contentDescription = "Edit task")
                    }
                    IconButton(onClick = onDelete) {
                        MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete scheduled task")
                    }
                }
            }
        }
    }
}

fun LazyListScope.teamKanbanBoardItems(
    tasks: List<TeamSharedTaskEntity>,
    selectedStage: String,
    onSelectStage: (String) -> Unit,
    onSetTaskStatus: (TeamSharedTaskEntity, String) -> Unit,
    onEditTask: (TeamSharedTaskEntity) -> Unit,
    onDeleteTask: (TeamSharedTaskEntity) -> Unit
) {
    item(key = "team_kanban_stage_strip", contentType = "kanban_strip") {
        val palette = LiquidTheme.palette
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeamAnalyticsCalculator.KANBAN_STATUSES.forEach { stage ->
                val count = tasks.count { TeamAnalyticsCalculator.resolvedStatus(it) == stage }
                val active = selectedStage == stage
                val stageColor = teamStatusColor(stage)
                Surface(
                    onClick = { onSelectStage(stage) },
                    shape = RoundedCornerShape(9999.dp),
                    color = if (active) stageColor else palette.frostedPillBg,
                    border = BorderStroke(1.dp, if (active) stageColor else palette.specularRimOutline),
                    modifier = Modifier.testTag("team_kanban_stage_$stage")
                ) {
                    Text(
                        text = "${TeamAnalyticsCalculator.statusLabel(stage)} ($count)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else palette.primaryInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    val stageTasks = tasks.filter { TeamAnalyticsCalculator.resolvedStatus(it) == selectedStage }
    if (stageTasks.isEmpty()) {
        item(key = "team_kanban_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "No tasks in ${TeamAnalyticsCalculator.statusLabel(selectedStage)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Create a task or move cards cleanly across Backlog -> To Do -> In Progress -> Review -> Done.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
            }
        }
    } else {
        items(items = stageTasks, key = { "kb_${it.id}" }, contentType = { "kanban_card" }) { task ->
            val palette = LiquidTheme.palette
            val stageIdx = TeamAnalyticsCalculator.KANBAN_STATUSES.indexOf(selectedStage)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TeamTaskCard(
                    task = task,
                    onCycleStatus = {
                        val next = TeamAnalyticsCalculator.KANBAN_STATUSES.getOrNull(stageIdx + 1)
                            ?: TeamAnalyticsCalculator.KANBAN_STATUSES.first()
                        onSetTaskStatus(task, next)
                    },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stageIdx > 0) {
                        val prev = TeamAnalyticsCalculator.KANBAN_STATUSES[stageIdx - 1]
                        OutlinedButton(
                            onClick = { onSetTaskStatus(task, prev) },
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("<- ${TeamAnalyticsCalculator.statusLabel(prev)}", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                        }
                    }
                    if (stageIdx in 0 until TeamAnalyticsCalculator.KANBAN_STATUSES.lastIndex) {
                        val next = TeamAnalyticsCalculator.KANBAN_STATUSES[stageIdx + 1]
                        TactilePrimaryPillButton(
                            text = "Move -> ${TeamAnalyticsCalculator.statusLabel(next)}",
                            onClick = { onSetTaskStatus(task, next) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamStructuredTableSection(
    tasks: List<TeamSharedTaskEntity>,
    onCycleStatus: (TeamSharedTaskEntity) -> Unit,
    onEditTask: (TeamSharedTaskEntity) -> Unit,
    onDeleteTask: (TeamSharedTaskEntity) -> Unit
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette

    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("team_structured_table"),
        cornerRadius = 22.dp
    ) {
        if (tasks.isEmpty()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("No rows in task database", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryInk)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(palette.recessedTrayBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Task", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(170.dp))
                    Text("Owners", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(120.dp))
                    Text("Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(74.dp))
                    Text("Deadline", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(86.dp))
                    Text("Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(100.dp))
                    Text("Drive / Source", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(110.dp))
                    Text("Actions", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.width(84.dp))
                }
                tasks.forEach { task ->
                    val status = TeamAnalyticsCalculator.resolvedStatus(task)
                    val sColor = teamStatusColor(status)
                    val owners = TeamAnalyticsCalculator.assignedNames(task).joinToString(", ") { "@$it" }.ifBlank { "Unassigned" }
                    val deadline = formatDeadlineLabel(task.deadlineMillis) ?: task.dayOfWeek
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.primaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(170.dp))
                        Text(owners, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp))
                        Text(task.priority, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (task.priority == "HIGH") CrimsonAccent else palette.secondaryInk, modifier = Modifier.width(74.dp))
                        Text(deadline, style = MaterialTheme.typography.labelSmall, color = palette.primaryInk, modifier = Modifier.width(86.dp))
                        Surface(
                            onClick = { onCycleStatus(task) },
                            shape = RoundedCornerShape(9999.dp),
                            color = sColor.copy(alpha = 0.16f),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(
                                text = TeamAnalyticsCalculator.statusLabel(status),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = sColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Box(modifier = Modifier.width(110.dp)) {
                            when {
                                !task.linkedDriveUrl.isNullOrBlank() -> Surface(
                                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.linkedDriveUrl))) } },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.accentSoftBg
                                ) {
                                    Text("Drive Link", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                !task.sourceBusinessId.isNullOrBlank() -> Text("Business OS", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
                                else -> Text("—", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                            }
                        }
                        Row(modifier = Modifier.width(84.dp)) {
                            IconButton(onClick = { onEditTask(task) }) {
                                MinimalIcon(type = MinimalIconType.EDIT, tint = palette.primaryInk, size = 15.dp, contentDescription = "Edit row")
                            }
                            IconButton(onClick = { onDeleteTask(task) }) {
                                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete row")
                            }
                        }
                    }
                    HorizontalDivider(color = palette.specularRimOutline.copy(alpha = 0.4f))
                }
            }
        }
    }
}

fun LazyListScope.teamActivityFeedItems(activities: List<TeamActivityEntity>) {
    if (activities.isEmpty()) {
        item(key = "team_activity_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No live activity logged yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                    Text("Every page edit, Kanban move, member invite, poll, and Business OS sync appears here in real time.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                }
            }
        }
    } else {
        items(items = activities, key = { it.id }, contentType = { "activity" }) { act ->
            val palette = LiquidTheme.palette
            val timeStr = remember(act.createdAtMillis) {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(act.createdAtMillis))
            }
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                        Text(
                            text = act.actionType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(act.summary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                        Text("${act.actorName} • $timeStr", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamPollNoticeCard(
    notif: TeamNotificationEntity,
    currentUserUid: String?,
    onVotePoll: (optionIndex: Int) -> Unit
) {
    val palette = LiquidTheme.palette
    val timeStr = remember(notif.createdAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(notif.createdAtMillis))
    }
    val taggedNames = remember(notif.taggedNamesCsv) {
        notif.taggedNamesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val options = remember(notif.pollOptionsJson) { PollVotesCodec.parseOptions(notif.pollOptionsJson) }
    val votes = remember(notif.pollVotesJson) { PollVotesCodec.parseVotes(notif.pollVotesJson) }
    val totalVotes = votes.size
    val myVoteIdx = currentUserUid?.let { votes[it] }

    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (notif.noticeType == "poll") {
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                            Text("POLL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                        }
                    }
                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                }
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
            }
            if (notif.message.isNotBlank()) {
                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
            }
            if (notif.noticeType == "poll" && options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { idx, opt ->
                        val count = votes.values.count { it == idx }
                        val ratio = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
                        val isMine = myVoteIdx == idx
                        Surface(
                            onClick = { onVotePoll(idx) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isMine) palette.accentSoftBg else palette.recessedTrayBg,
                            border = BorderStroke(1.dp, if (isMine) CrimsonAccent else palette.specularRimOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("poll_option_${notif.id}_$idx")
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (isMine) "✓ $opt" else opt,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isMine) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isMine) CrimsonAccent else palette.primaryInk
                                    )
                                    Text("$count (${(ratio * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(palette.cardSurfaceTop)
                                ) {
                                    if (ratio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio.coerceIn(0.05f, 1f))
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(9999.dp))
                                                .background(if (isMine) CrimsonAccent else palette.primaryInk.copy(alpha = 0.55f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text("$totalVotes total vote(s) • Tap an option to vote", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                }
            }
            if (taggedNames.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    taggedNames.forEach { name ->
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                            Text("@$name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Text("Posted by ${notif.senderName}", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
        }
    }
}
