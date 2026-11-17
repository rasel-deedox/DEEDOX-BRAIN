package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CloudTeamInvitePreview
import com.example.data.local.TeamEmailInviteEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.repository.TeamAnalyticsCalculator
import com.example.data.repository.WorkspaceBlock
import com.example.ui.components.LiquidTimePickerDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import java.util.Locale

@Composable
internal fun teamFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LiquidTheme.palette.cardSurfaceTop,
    unfocusedContainerColor = LiquidTheme.palette.cardSurfaceTop,
    focusedBorderColor = CrimsonAccent,
    unfocusedBorderColor = LiquidTheme.palette.inputBorder,
    focusedTextColor = LiquidTheme.palette.primaryInk,
    unfocusedTextColor = LiquidTheme.palette.primaryInk
)

internal fun formatTeamTime12h(hour24: Int, minute: Int): String {
    val amPm = if (hour24 < 12) "AM" else "PM"
    val h12 = when {
        hour24 % 12 == 0 -> 12
        else -> hour24 % 12
    }
    return String.format(Locale.US, "%02d:%02d %s", h12, minute.coerceIn(0, 59), amPm)
}

internal fun parseTeamTime12h(timeStr: String): Pair<Int, Int> {
    val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
        .find(timeStr.trim()) ?: return 10 to 0
    val rawHour = match.groupValues[1].toIntOrNull() ?: 10
    val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
    val isPm = match.groupValues[3].equals("PM", ignoreCase = true)
    val hour24 = when {
        isPm && rawHour < 12 -> rawHour + 12
        !isPm && rawHour == 12 -> 0
        else -> rawHour.coerceIn(0, 23)
    }
    return hour24 to minute
}

@Composable
fun CreateTeamDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, customInviteCode: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    val fieldColors = teamFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Create Team Workspace", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Team Name (e.g. Core Dev Team)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("team_create_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Goal / Sprint Mission") },
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = customCode,
                    onValueChange = { customCode = it.uppercase() },
                    label = { Text("Invite Code (blank = auto DEEDOX-xxx)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = if (isBusy) "Creating..." else "Create Team",
                enabled = name.isNotBlank() && !isBusy,
                onClick = { onCreate(name.trim(), description.trim(), customCode.trim()) },
                modifier = Modifier.testTag("team_confirm_create_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

@Composable
fun JoinByInviteCodeDialog(
    isBusy: Boolean,
    preview: CloudTeamInvitePreview?,
    onLookup: (String) -> Unit,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    val palette = LiquidTheme.palette
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Join by Invite Code", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Invite Code (e.g. DEEDOX-849)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("team_join_code_input")
                )
                OutlinedButton(
                    onClick = { onLookup(code.trim()) },
                    enabled = code.trim().length >= 3 && !isBusy,
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CrimsonAccent)
                    } else {
                        Text("Check Invite Code", color = palette.primaryInk)
                    }
                }
                if (preview != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(preview.teamName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            Text("Capacity: ${preview.maxMembers} seats • Code: ${preview.inviteCode}", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Join Team",
                enabled = code.trim().length >= 3 && !isBusy,
                onClick = { onJoin(code.trim()) },
                modifier = Modifier.testTag("team_confirm_join_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

@Composable
fun EditMemberRoleStatusDialog(
    member: TeamMemberEntity,
    isCurrentUserOwner: Boolean,
    isCurrentUserAdminOrOwner: Boolean = isCurrentUserOwner,
    canRemoveTarget: Boolean = false,
    onDismiss: () -> Unit,
    onRemoveMember: (() -> Unit)? = null,
    onSave: (roleTag: String, customStatus: String, newRole: String?) -> Unit
) {
    val palette = LiquidTheme.palette
    var roleTag by remember { mutableStateOf(member.roleTag.ifBlank { if (member.role == "OWNER") "Team Lead" else "Member" }) }
    var customStatus by remember { mutableStateOf(member.customStatus.ifBlank { "Available" }) }
    var permissionRole by remember { mutableStateOf(member.role) }

    val rolePresets = listOf("Team Lead", "Lead Developer", "UI/UX Designer", "Marketing", "QA Tester", "Researcher")
    val statusPresets = listOf("Available", "Working on API", "In Focus", "Reviewing PRs", "Deep Work")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Role & Status • ${member.displayName}",
                color = palette.primaryInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = roleTag,
                    onValueChange = { roleTag = it },
                    label = { Text("Custom Role Tag (e.g. Lead Developer)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_role_tag_input")
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rolePresets.forEach { preset ->
                        val selected = roleTag.equals(preset, ignoreCase = true)
                        Surface(
                            onClick = { roleTag = preset },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) palette.primaryButtonBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customStatus,
                    onValueChange = { customStatus = it },
                    label = { Text("Live Status Pill (e.g. Working on API)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_custom_status_input")
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusPresets.forEach { preset ->
                        val selected = customStatus.equals(preset, ignoreCase = true)
                        Surface(
                            onClick = { customStatus = preset },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) palette.accentSoftBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) CrimsonAccent else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isCurrentUserAdminOrOwner && member.role != "OWNER") {
                    Text("Workspace Permission", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ADMIN", "MEMBER").forEach { r ->
                            val selected = permissionRole == r
                            Surface(
                                onClick = { permissionRole = r },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = if (r == "ADMIN") "Admin" else "Member",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (canRemoveTarget && onRemoveMember != null) {
                    OutlinedButton(
                        onClick = onRemoveMember,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, CrimsonAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remove_member_button")
                    ) {
                        MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp)
                        Text(" Remove Member", color = CrimsonAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Save Profile",
                onClick = {
                    onSave(
                        roleTag.trim(),
                        customStatus.trim().ifBlank { "Available" },
                        if (isCurrentUserAdminOrOwner && member.role != "OWNER" && permissionRole != member.role) permissionRole else null
                    )
                },
                modifier = Modifier.testTag("save_member_profile_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

@Composable
fun ScheduleTaskDialog(
    members: List<TeamMemberEntity>,
    initialDay: String,
    existingTask: TeamSharedTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        dayOfWeek: String,
        dateLabel: String,
        startTime: String,
        endTime: String,
        assignedMembers: List<TeamMemberEntity>,
        priority: String,
        status: String,
        deadlineMillis: Long?,
        linkedDriveUrl: String?
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember { mutableStateOf(existingTask?.title.orEmpty()) }
    var desc by remember { mutableStateOf(existingTask?.description.orEmpty()) }
    var dayOfWeek by remember {
        mutableStateOf(
            existingTask?.dayOfWeek?.takeIf { it in TeamAnalyticsCalculator.TEAM_WEEK_DAYS }
                ?: initialDay.takeIf { it in TeamAnalyticsCalculator.TEAM_WEEK_DAYS }
                ?: "Mon"
        )
    }
    var startTime by remember { mutableStateOf(existingTask?.startTime?.ifBlank { "10:00 AM" } ?: "10:00 AM") }
    var endTime by remember { mutableStateOf(existingTask?.endTime?.ifBlank { "12:30 PM" } ?: "12:30 PM") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: "MEDIUM") }
    var status by remember { mutableStateOf(existingTask?.let { TeamAnalyticsCalculator.resolvedStatus(it) } ?: "todo") }
    var deadlineMillis by remember { mutableStateOf(existingTask?.deadlineMillis) }
    var linkedDriveUrl by remember { mutableStateOf(existingTask?.linkedDriveUrl.orEmpty()) }

    val initialAssignedUids = remember(existingTask) {
        if (existingTask != null) {
            TeamAnalyticsCalculator.assignedUids(existingTask).toSet()
        } else {
            members.firstOrNull()?.uid?.let { setOf(it) } ?: emptySet()
        }
    }
    val selectedUids = remember { mutableStateListOf<String>().apply { addAll(initialAssignedUids) } }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (existingTask == null) "Schedule Team Task" else "Edit Schedule Task",
                color = palette.primaryInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_task_title_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deliverable Notes (optional)") },
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Day of Week (Sat – Fri)", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TeamAnalyticsCalculator.TEAM_WEEK_DAYS.forEach { d ->
                        val selected = dayOfWeek == d
                        Surface(
                            onClick = { dayOfWeek = d },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) palette.primaryButtonBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.testTag("dialog_day_$d")
                        ) {
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Text("Time Slot", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_start_time_btn")
                    ) {
                        Text("Start: $startTime", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_end_time_btn")
                    ) {
                        Text("End: $endTime", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                    }
                }

                Text("Deadline Preset", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = remember { System.currentTimeMillis() }
                    listOf(
                        "None" to null,
                        "Today" to now,
                        "Tomorrow" to (now + 86_400_000L),
                        "+3 Days" to (now + 3L * 86_400_000L),
                        "+7 Days" to (now + 7L * 86_400_000L)
                    ).forEach { (label, target) ->
                        val active = if (target == null) deadlineMillis == null else deadlineMillis != null && kotlin.math.abs((deadlineMillis ?: 0L) - target) < 43_200_000L
                        Surface(
                            onClick = { deadlineMillis = target },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) palette.accentSoftBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, if (active) CrimsonAccent else palette.specularRimOutline)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) CrimsonAccent else palette.primaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }

                if (members.isNotEmpty()) {
                    Text("Tag @Members (multi-select)", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { member ->
                            val selected = member.uid in selectedUids
                            Surface(
                                onClick = {
                                    if (selected) selectedUids.remove(member.uid)
                                    else selectedUids.add(member.uid)
                                },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.accentSoftBg else palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline),
                                modifier = Modifier.testTag("assign_member_${member.uid}")
                            ) {
                                Text(
                                    text = "@${member.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) CrimsonAccent else palette.primaryInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Text("Kanban Stage & Priority", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TeamAnalyticsCalculator.KANBAN_STATUSES.forEach { key ->
                        val selected = status == key
                        Surface(
                            onClick = { status = key },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) palette.primaryButtonBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = TeamAnalyticsCalculator.statusLabel(key),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        val selected = priority == p
                        Surface(
                            onClick = { priority = p },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) CrimsonAccent else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = p,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = linkedDriveUrl,
                    onValueChange = { linkedDriveUrl = it },
                    label = { Text("Linked Google Drive / Doc URL (optional)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_task_drive_url_input")
                )
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = if (existingTask == null) "Schedule Task" else "Save Changes",
                enabled = title.isNotBlank(),
                onClick = {
                    val assigned = members.filter { it.uid in selectedUids }
                    onSave(
                        title.trim(),
                        desc.trim(),
                        dayOfWeek,
                        "$dayOfWeek · $startTime - $endTime",
                        startTime,
                        endTime,
                        assigned,
                        priority,
                        status,
                        deadlineMillis,
                        linkedDriveUrl.trim().takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier.testTag("team_confirm_task_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )

    if (showStartPicker) {
        val (h, m) = parseTeamTime12h(startTime)
        LiquidTimePickerDialog(
            initialHour = h,
            initialMinute = m,
            title = "Start Time",
            onConfirm = { hour, min ->
                startTime = formatTeamTime12h(hour, min)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        val (h, m) = parseTeamTime12h(endTime)
        LiquidTimePickerDialog(
            initialHour = h,
            initialMinute = m,
            title = "End Time",
            onConfirm = { hour, min ->
                endTime = formatTeamTime12h(hour, min)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun SharedNotionNoteDialog(
    existingDoc: TeamSharedDocEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, url: String?, blocksJson: String, tagsCsv: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember { mutableStateOf(existingDoc?.title.orEmpty()) }
    var tagsCsv by remember { mutableStateOf(existingDoc?.tagsCsv.orEmpty()) }
    var url by remember { mutableStateOf(existingDoc?.resourceUrl.orEmpty()) }

    val initialBlocks = remember(existingDoc) {
        val decoded = existingDoc?.blocksJson?.let { WorkspaceBlock.parseList(it) }.orEmpty()
        if (decoded.isNotEmpty()) {
            decoded
        } else if (!existingDoc?.content.isNullOrBlank()) {
            listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = existingDoc!!.content))
        } else {
            listOf(
                WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Overview"),
                WorkspaceBlock(type = WorkspaceBlock.TYPE_CHECKLIST, text = "Define sprint deliverables", checked = false)
            )
        }
    }
    val blocks = remember { mutableStateListOf<WorkspaceBlock>().apply { addAll(initialBlocks) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (existingDoc == null) "New Shared Team Note" else "Co-Edit Shared Note",
                color = palette.primaryInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_doc_title_input")
                )

                Text("Notion Blocks", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                blocks.forEachIndexed { index, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (block.type == WorkspaceBlock.TYPE_CHECKLIST) {
                            Checkbox(
                                checked = block.checked,
                                onCheckedChange = { checked ->
                                    blocks[index] = block.copy(checked = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CrimsonAccent,
                                    uncheckedColor = palette.secondaryInk,
                                    checkmarkColor = Color.White
                                )
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.recessedTrayBg
                            ) {
                                Text(
                                    text = when (block.type) {
                                        WorkspaceBlock.TYPE_H1 -> "H1"
                                        WorkspaceBlock.TYPE_H2 -> "H2"
                                        WorkspaceBlock.TYPE_BULLET -> "•"
                                        WorkspaceBlock.TYPE_CALLOUT -> "!"
                                        WorkspaceBlock.TYPE_CODE -> "</>"
                                        else -> "T"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = block.text,
                            onValueChange = { next -> blocks[index] = block.copy(text = next) },
                            placeholder = { Text("Write block...") },
                            colors = teamFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        if (blocks.size > 1) {
                            IconButton(onClick = { blocks.removeAt(index) }) {
                                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Remove block")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        WorkspaceBlock.TYPE_TEXT to "+ Text",
                        WorkspaceBlock.TYPE_CHECKLIST to "+ Checklist",
                        WorkspaceBlock.TYPE_H2 to "+ Heading",
                        WorkspaceBlock.TYPE_BULLET to "+ Bullet",
                        WorkspaceBlock.TYPE_CALLOUT to "+ Callout"
                    ).forEach { (typeKey, label) ->
                        OutlinedButton(
                            onClick = { blocks.add(WorkspaceBlock(type = typeKey, text = "")) },
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                        }
                    }
                }

                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text("Tags (comma-separated, e.g. api, launch)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Reference URL (optional)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = if (existingDoc == null) "Publish Note" else "Save Update",
                enabled = title.isNotBlank(),
                onClick = {
                    val cleanedBlocks = blocks.filter { it.text.isNotBlank() }
                        .ifEmpty { listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = title.trim())) }
                    val plainSummary = cleanedBlocks.joinToString("\n") { b ->
                        when (b.type) {
                            WorkspaceBlock.TYPE_CHECKLIST -> "${if (b.checked) "[x]" else "[ ]"} ${b.text}"
                            WorkspaceBlock.TYPE_BULLET -> "• ${b.text}"
                            else -> b.text
                        }
                    }
                    onSave(
                        title.trim(),
                        plainSummary,
                        url.trim().takeIf { it.isNotBlank() },
                        WorkspaceBlock.encodeList(cleanedBlocks),
                        tagsCsv.trim()
                    )
                },
                modifier = Modifier.testTag("team_confirm_doc_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

@Composable
fun InviteByEmailDialog(
    teamName: String,
    pendingInvites: List<TeamEmailInviteEntity>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onInvite: (email: String, role: String, roleTag: String) -> Unit,
    onRevokeInvite: (TeamEmailInviteEntity) -> Unit
) {
    val palette = LiquidTheme.palette
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    var roleTag by remember { mutableStateOf("Member") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add Member by Gmail • $teamName", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Teammate Gmail (e.g. alex@gmail.com)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_email_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MEMBER" to "Member", "ADMIN" to "Admin").forEach { (r, lbl) ->
                        val active = role == r
                        Surface(
                            onClick = { role = r },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (active) palette.primaryButtonContent else palette.primaryInk, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = roleTag,
                    onValueChange = { roleTag = it },
                    label = { Text("Role Tag (e.g. Lead Developer, Growth)") },
                    singleLine = true,
                    colors = teamFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (pendingInvites.isNotEmpty()) {
                    Text("Pending Gmail Invites (${pendingInvites.size})", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                    pendingInvites.forEach { inv ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(inv.invitedEmail, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                                Text("${inv.invitedRole} · ${inv.roleTag}", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                            }
                            TextButton(onClick = { onRevokeInvite(inv) }) {
                                Text("Revoke", color = CrimsonAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = if (isBusy) "Sending..." else "Send Gmail Invite",
                enabled = email.contains("@") && email.contains(".") && !isBusy,
                onClick = { onInvite(email.trim(), role, roleTag.trim().ifBlank { "Member" }) },
                modifier = Modifier.testTag("confirm_email_invite_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = palette.secondaryInk) }
        }
    )
}
