package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiPagePolishMode
import com.example.data.cloud.CloudTeamInvitePreview
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamEmailInviteEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.local.TeamWorkspaceEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.PollVotesCodec
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.TeamAnalyticsCalculator
import com.example.data.repository.WorkspaceBlock
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

private enum class TeamSectionTab(val label: String) {
    SCHEDULE("Tasks"),
    NOTES("Pages"),
    NOTICES("Notices"),
    ACTIVITY("Activity"),
    ANALYTICS("Analytics")
}

@Composable
fun TeamCollabScreen(
    isSignedIn: Boolean,
    currentUserUid: String?,
    teams: List<TeamWorkspaceEntity>,
    activeTeamId: String?,
    members: List<TeamMemberEntity>,
    tasks: List<TeamSharedTaskEntity>,
    docs: List<TeamSharedDocEntity>,
    notifications: List<TeamNotificationEntity>,
    latestAlertBanner: TeamNotificationEntity?,
    invitePreview: CloudTeamInvitePreview?,
    isBusy: Boolean,
    teamError: String?,
    onGoogleSignInClick: () -> Unit,
    onSelectTeam: (String) -> Unit,
    onCreateTeam: (name: String, description: String, customInviteCode: String, onDone: () -> Unit) -> Unit,
    onLookupInviteCode: (String) -> Unit,
    onJoinTeamByCode: (String, () -> Unit) -> Unit,
    onExpandTeamLimit: (TeamWorkspaceEntity) -> Unit,
    onUpdateMemberProfile: (
        teamId: String,
        member: TeamMemberEntity,
        roleTag: String,
        customStatus: String,
        newRole: String?
    ) -> Unit,
    onSaveScheduleTask: (
        teamId: String,
        title: String,
        desc: String,
        dayOfWeek: String,
        dateLabel: String,
        startTime: String,
        endTime: String,
        assignedMembers: List<TeamMemberEntity>,
        priority: String,
        status: String,
        existingTask: TeamSharedTaskEntity?
    ) -> Unit,
    onCycleTaskStatus: (TeamSharedTaskEntity) -> Unit,
    onSetTaskStatus: (TeamSharedTaskEntity, String) -> Unit,
    onDeleteSharedTask: (TeamSharedTaskEntity) -> Unit,
    onSaveSharedNote: (
        teamId: String,
        title: String,
        content: String,
        url: String?,
        blocksJson: String,
        tagsCsv: String,
        existingDoc: TeamSharedDocEntity?
    ) -> Unit,
    onDeleteSharedDoc: (TeamSharedDocEntity) -> Unit,
    onSendTeamAlert: (
        teamId: String,
        title: String,
        message: String,
        taggedMembers: List<TeamMemberEntity>
    ) -> Unit,
    onDismissAlertBanner: () -> Unit,
    onClearTeamError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    activities: List<TeamActivityEntity> = emptyList(),
    emailInvites: List<TeamEmailInviteEntity> = emptyList(),
    incomingEmailInvites: List<TeamEmailInviteEntity> = emptyList(),
    onInviteByEmail: (team: TeamWorkspaceEntity, email: String, role: String, roleTag: String, onDone: () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onAcceptEmailInvite: (TeamEmailInviteEntity) -> Unit = {},
    onRevokeEmailInvite: (TeamEmailInviteEntity) -> Unit = {},
    onRemoveMember: (teamId: String, member: TeamMemberEntity) -> Unit = { _, _ -> },
    onTogglePinSharedDoc: (TeamSharedDocEntity) -> Unit = {},
    onVotePoll: (teamId: String, noticeId: String, optionIndex: Int) -> Unit = { _, _, _ -> },
    onSaveTaskExtended: ((
        teamId: String,
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
        linkedDriveUrl: String?,
        existingTask: TeamSharedTaskEntity?
    ) -> Unit)? = null,
    onSavePageExtended: ((
        teamId: String,
        title: String,
        content: String,
        url: String?,
        blocksJson: String,
        tagsCsv: String,
        parentId: String?,
        icon: String,
        isPinned: Boolean,
        existingDoc: TeamSharedDocEntity?
    ) -> Unit)? = null,
    onSendAlertExtended: ((
        teamId: String,
        title: String,
        message: String,
        taggedMembers: List<TeamMemberEntity>,
        noticeType: String,
        pollOptionsJson: String
    ) -> Unit)? = null,
    playbackState: VoicePlaybackState = VoicePlaybackState(),
    onPolishPageWithAi: (suspend (String, List<WorkspaceBlock>, AiPagePolishMode) -> Result<List<WorkspaceBlock>>)? = null,
    onLogClientPaymentToLedger: (clientName: String, projectName: String, amountCents: Long) -> Unit = { _, _, _ -> },
    onScheduleInlineReminder: (teamId: String, ReminderBlockData) -> Unit = { _, _ -> },
    onSyncScheduleBlock: (teamId: String, ScheduleBlockData, List<TeamMemberEntity>) -> Unit = { _, _, _ -> },
    onLogFocusTimerSeconds: (String, Long) -> Unit = { _, _ -> },
    onStartInlineVoiceRecord: () -> Unit = {},
    onStopInlineVoiceRecord: () -> Pair<String, Long>? = { null },
    onToggleVoicePlayback: (String) -> Unit = {},
    onSeekVoicePlayback: (String, Long) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LiquidTheme.palette
    var activeTab by remember { mutableStateOf(TeamSectionTab.SCHEDULE) }
    var taskViewMode by remember { mutableStateOf(TeamTaskViewMode.SCHEDULE) }
    var selectedKanbanStage by remember { mutableStateOf("todo") }
    var selectedDayFilter by remember { mutableStateOf("All") }
    var selectedMemberFilterUid by remember { mutableStateOf<String?>(null) }
    var showTeamDropdown by remember { mutableStateOf(false) }

    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }
    var showInviteEmailDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TeamSharedTaskEntity?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<TeamSharedDocEntity?>(null) }
    var newNoteParentId by remember { mutableStateOf<String?>(null) }
    var newNoteInitialTitle by remember { mutableStateOf("") }
    var pageSearchQuery by remember { mutableStateOf("") }
    var editingMember by remember { mutableStateOf<TeamMemberEntity?>(null) }

    var composerType by remember { mutableStateOf("notice") }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }
    val noticeTaggedUids = remember { mutableStateListOf<String>() }

    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val activeTeam = remember(teams, activeTeamId) {
        teams.find { it.id == activeTeamId } ?: teams.firstOrNull()
    }
    val currentUserMember = remember(members, currentUserUid) {
        members.find { it.uid == currentUserUid }
    }
    val isCurrentUserOwner = activeTeam?.ownerId == currentUserUid
    val isCurrentUserAdminOrOwner = isCurrentUserOwner || currentUserMember?.role == "ADMIN"

    val analyticsSummary = remember(members, tasks) {
        TeamAnalyticsCalculator.compute(members, tasks)
    }
    val filteredTasks = remember(tasks, selectedDayFilter, selectedMemberFilterUid) {
        tasks.filter { task ->
            val dayMatches = selectedDayFilter == "All" || task.dayOfWeek == selectedDayFilter
            val memberMatches = selectedMemberFilterUid == null ||
                selectedMemberFilterUid in TeamAnalyticsCalculator.assignedUids(task)
            dayMatches && memberMatches
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .testTag("team_collab_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "team_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingGlassIconButton(
                        iconType = MinimalIconType.BACK,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.testTag("team_back_button")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Team & Group OS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                        if (isSignedIn && activeTeam != null) {
                            Box {
                                Surface(
                                    onClick = { showTeamDropdown = true },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .testTag("team_selector_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CrimsonAccent))
                                        Text(activeTeam.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("▾", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                    }
                                }
                                DropdownMenu(expanded = showTeamDropdown, onDismissRequest = { showTeamDropdown = false }, containerColor = palette.secondaryPillBg) {
                                    teams.forEach { team ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${team.name} (${team.inviteCode})",
                                                    color = if (team.id == activeTeam.id) CrimsonAccent else palette.primaryInk,
                                                    fontWeight = if (team.id == activeTeam.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                onSelectTeam(team.id)
                                                showTeamDropdown = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("+ Create New Team", color = CrimsonAccent, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showTeamDropdown = false
                                            onClearTeamError()
                                            showCreateTeamDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            Text("Notion pages, Kanban, schedules, Gmail invites & live polls", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                        }
                    }
                    if (isSignedIn) {
                        OutlinedButton(
                            onClick = {
                                onClearTeamError()
                                showJoinTeamDialog = true
                            },
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.testTag("team_join_code_button")
                        ) {
                            Text("Join", color = palette.primaryInk, style = MaterialTheme.typography.labelMedium)
                        }
                        TactilePrimaryPillButton(
                            text = "+ Team",
                            iconType = MinimalIconType.PLUS,
                            onClick = {
                                onClearTeamError()
                                showCreateTeamDialog = true
                            },
                            modifier = Modifier.testTag("team_create_button")
                        )
                    }
                }
            }

            if (isSignedIn && incomingEmailInvites.isNotEmpty()) {
                items(items = incomingEmailInvites, key = { "in_inv_${it.id}" }, contentType = { "email_invite" }) { inv ->
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth().testTag("incoming_email_invite_${inv.id}"), cornerRadius = 22.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gmail Team Invite • ${inv.teamName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                Text("Invited by ${inv.invitedByName.ifBlank { "Admin" }} as ${inv.roleTag} (${inv.invitedRole})", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TactilePrimaryPillButton(
                                    text = "Accept",
                                    onClick = { onAcceptEmailInvite(inv) },
                                    modifier = Modifier.testTag("accept_email_invite_${inv.id}")
                                )
                            }
                        }
                    }
                }
            }

            if (latestAlertBanner != null) {
                item(key = "team_live_alert_banner", contentType = "alert_banner") {
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth().testTag("team_live_alert_banner"), cornerRadius = 22.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                MinimalIcon(type = MinimalIconType.BELL, active = true, tint = CrimsonAccent, size = 20.dp)
                                Column {
                                    Text("${latestAlertBanner.senderName}: ${latestAlertBanner.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                    Text(latestAlertBanner.message, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                }
                            }
                            TextButton(onClick = onDismissAlertBanner) { Text("Dismiss", color = CrimsonAccent) }
                        }
                    }
                }
            }

            if (!teamError.isNullOrBlank()) {
                item(key = "team_error_card", contentType = "error") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(teamError, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent, modifier = Modifier.weight(1f))
                            TextButton(onClick = onClearTeamError) { Text("Dismiss", color = palette.secondaryInk) }
                        }
                    }
                }
            }

            if (!isSignedIn) {
                item(key = "team_signin_guard", contentType = "auth_guard") {
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Sign In to Sync Live Team Rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            Text("Multi-team switching, Gmail member invites, nested Notion pages, Kanban boards, polls, and live activity sync with your Google account.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryInk)
                            TactilePrimaryPillButton(
                                text = "Sign In with Google",
                                iconType = MinimalIconType.CLOUD_SYNC,
                                onClick = onGoogleSignInClick,
                                modifier = Modifier.testTag("team_google_signin_button")
                            )
                        }
                    }
                }
            } else if (activeTeam == null) {
                item(key = "team_empty", contentType = "empty") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Start or Join a Team Workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            Text("Create multiple team rooms with invite codes like DEEDOX-849, or join an existing group.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryInk)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TactilePrimaryPillButton(text = "+ Create Team", onClick = { showCreateTeamDialog = true }, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { showJoinTeamDialog = true }, shape = RoundedCornerShape(9999.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                                    Text("Join by Code", color = palette.primaryInk)
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "team_workspace_hero_${activeTeam.id}", contentType = "team_hero") {
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(activeTeam.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                    if (activeTeam.description.isNotBlank()) {
                                        Text(activeTeam.description, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(9999.dp), color = palette.frostedPillBg, border = BorderStroke(1.dp, palette.specularRimOutline)) {
                                    Text("${members.size.coerceAtLeast(1)} / ${activeTeam.maxMembers} seats", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg, border = BorderStroke(1.dp, palette.specularRimOutline)) {
                                    Text("Invite Code: ${activeTeam.inviteCode}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.accentSoftInk, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                                }
                                if (isCurrentUserAdminOrOwner) {
                                    OutlinedButton(
                                        onClick = { showInviteEmailDialog = true },
                                        shape = RoundedCornerShape(9999.dp),
                                        modifier = Modifier.testTag("team_invite_gmail_button")
                                    ) {
                                        Text("+ Invite Gmail", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Join my team \"${activeTeam.name}\" in Personal AI Library using Invite Code: ${activeTeam.inviteCode}")
                                        }
                                        runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share Team Invite")) }
                                    },
                                    shape = RoundedCornerShape(9999.dp)
                                ) {
                                    MinimalIcon(type = MinimalIconType.SHARE, tint = palette.primaryInk, size = 14.dp)
                                    Text(" Share", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                                }
                                if (isCurrentUserOwner) {
                                    OutlinedButton(
                                        onClick = { onExpandTeamLimit(activeTeam) },
                                        shape = RoundedCornerShape(9999.dp),
                                        modifier = Modifier.testTag("team_expand_capacity_button")
                                    ) {
                                        Text("+5 Seats", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
                                    }
                                }
                            }

                            if (members.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Team Roster (${members.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                    Text("Tap member to edit role or remove", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                }
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    members.forEach { member ->
                                        val canEdit = isCurrentUserAdminOrOwner || member.uid == currentUserUid
                                        Surface(
                                            onClick = { if (canEdit) editingMember = member },
                                            shape = RoundedCornerShape(20.dp),
                                            color = palette.cardSurfaceTop,
                                            border = BorderStroke(1.dp, palette.specularRimOutline),
                                            modifier = Modifier.testTag("member_roster_card_${member.uid}")
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                TeamMemberAvatar(displayName = member.displayName, photoUrl = member.photoUrl, isOnline = member.isOnline, size = 38.dp)
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(member.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                                        if (member.role == "OWNER" || member.role == "ADMIN") {
                                                            Text(member.role, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                                                        }
                                                    }
                                                    if (member.email.isNotBlank()) {
                                                        Text(member.email, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                                                            Text(member.roleTag.ifBlank { if (member.role == "OWNER") "Team Lead" else "Member" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                        }
                                                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                                                            Text(member.customStatus.ifBlank { "Available" }, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "team_section_tabs", contentType = "segment_switcher") {
                    RecessedFramingBezel(modifier = Modifier.fillMaxWidth(), cornerRadius = 9999.dp, innerPadding = PaddingValues(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TeamSectionTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                Surface(
                                    onClick = { activeTab = tab },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (selected) palette.primaryButtonBg else Color.Transparent,
                                    border = if (selected) BorderStroke(1.dp, palette.specularRimOutline) else null,
                                    modifier = Modifier.testTag("team_tab_${tab.name.lowercase()}")
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeTab) {
                    TeamSectionTab.SCHEDULE -> {
                        item(key = "team_task_view_mode", contentType = "view_mode") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TeamTaskViewMode.entries.forEach { mode ->
                                    val selected = taskViewMode == mode
                                    Surface(
                                        onClick = { taskViewMode = mode },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selected) palette.accentSoftBg else palette.frostedPillBg,
                                        border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline),
                                        modifier = Modifier.weight(1f).testTag("team_task_view_${mode.name.lowercase()}")
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(mode.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) CrimsonAccent else palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }

                        if (members.isNotEmpty()) {
                            item(key = "team_member_filter_strip", contentType = "member_filter") {
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = { selectedMemberFilterUid = null },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selectedMemberFilterUid == null) palette.accentSoftBg else palette.cardSurfaceTop,
                                        border = BorderStroke(1.dp, if (selectedMemberFilterUid == null) CrimsonAccent else palette.specularRimOutline),
                                        modifier = Modifier.testTag("member_filter_all")
                                    ) {
                                        Text("All Members", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (selectedMemberFilterUid == null) CrimsonAccent else palette.primaryInk, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                                    }
                                    members.forEach { m ->
                                        val selected = selectedMemberFilterUid == m.uid
                                        Surface(
                                            onClick = { selectedMemberFilterUid = if (selected) null else m.uid },
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (selected) palette.accentSoftBg else palette.cardSurfaceTop,
                                            border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline),
                                            modifier = Modifier.testTag("member_filter_${m.uid}")
                                        ) {
                                            Row(modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                TeamMemberAvatar(displayName = m.displayName, photoUrl = m.photoUrl, isOnline = m.isOnline, showOnlineDot = false, size = 22.dp)
                                                Text(m.displayName, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) CrimsonAccent else palette.primaryInk)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "team_tasks_add", contentType = "action") {
                            TactilePrimaryPillButton(
                                text = "+ Add / Schedule Team Task",
                                iconType = MinimalIconType.PLUS,
                                onClick = {
                                    editingTask = null
                                    showScheduleDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().testTag("team_add_task_button")
                            )
                        }

                        when (taskViewMode) {
                            TeamTaskViewMode.KANBAN -> {
                                teamKanbanBoardItems(
                                    tasks = filteredTasks,
                                    selectedStage = selectedKanbanStage,
                                    onSelectStage = { selectedKanbanStage = it },
                                    onSetTaskStatus = onSetTaskStatus,
                                    onEditTask = { editingTask = it; showScheduleDialog = true },
                                    onDeleteTask = { task ->
                                        pendingDeleteTitle = "Delete Scheduled Task?"
                                        pendingDeleteMessage = "Remove \"${task.title}\" for all team members?"
                                        pendingDeleteAction = { onDeleteSharedTask(task) }
                                    }
                                )
                            }
                            TeamTaskViewMode.TABLE -> {
                                item(key = "team_table_view", contentType = "table") {
                                    TeamStructuredTableSection(
                                        tasks = filteredTasks,
                                        onCycleStatus = onCycleTaskStatus,
                                        onEditTask = { editingTask = it; showScheduleDialog = true },
                                        onDeleteTask = { task ->
                                            pendingDeleteTitle = "Delete Scheduled Task?"
                                            pendingDeleteMessage = "Remove \"${task.title}\" for all team members?"
                                            pendingDeleteAction = { onDeleteSharedTask(task) }
                                        }
                                    )
                                }
                            }
                            TeamTaskViewMode.SCHEDULE -> {
                                item(key = "team_day_strip", contentType = "day_strip") {
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        (listOf("All") + TeamAnalyticsCalculator.TEAM_WEEK_DAYS).forEach { day ->
                                            val selected = selectedDayFilter == day
                                            val count = if (day == "All") tasks.size else tasks.count { it.dayOfWeek == day }
                                            Surface(
                                                onClick = { selectedDayFilter = day },
                                                shape = RoundedCornerShape(9999.dp),
                                                color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                                modifier = Modifier.testTag("day_filter_$day")
                                            ) {
                                                Text(
                                                    text = if (count > 0) "$day ($count)" else day,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (filteredTasks.isEmpty()) {
                                    item(key = "team_tasks_empty", contentType = "empty") {
                                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("No scheduled tasks match this filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                                Text("Tap + Add / Schedule Team Task to assign a Sat–Fri time slot and tag @Members.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                            }
                                        }
                                    }
                                } else {
                                    items(items = filteredTasks, key = { it.id }, contentType = { "team_task" }) { task ->
                                        TeamTaskCard(
                                            task = task,
                                            onCycleStatus = { onCycleTaskStatus(task) },
                                            onEdit = { editingTask = task; showScheduleDialog = true },
                                            onDelete = {
                                                pendingDeleteTitle = "Delete Scheduled Task?"
                                                pendingDeleteMessage = "Remove \"${task.title}\" for all team members?"
                                                pendingDeleteAction = { onDeleteSharedTask(task) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TeamSectionTab.NOTES -> {
                        teamNotionPagesSection(
                            docs = docs,
                            searchQuery = pageSearchQuery,
                            onSearchQueryChange = { pageSearchQuery = it },
                            onCreateRootPage = {
                                editingNote = null
                                newNoteParentId = null
                                showNoteDialog = true
                            },
                            onCreateSubPage = { parentDoc ->
                                editingNote = null
                                newNoteParentId = parentDoc.id
                                showNoteDialog = true
                            },
                            onOpenPage = { doc ->
                                editingNote = doc
                                newNoteParentId = doc.parentId
                                showNoteDialog = true
                            },
                            onTogglePin = onTogglePinSharedDoc,
                            onRequestDeletePage = { doc ->
                                pendingDeleteTitle = "Delete Shared Page?"
                                pendingDeleteMessage = "Remove \"${doc.title}\" from team workspace?"
                                pendingDeleteAction = { onDeleteSharedDoc(doc) }
                            }
                        )
                    }

                    TeamSectionTab.NOTICES -> {
                        item(key = "team_alert_composer", contentType = "composer") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Notice Board & Interactive Polls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("notice" to "Notice", "poll" to "Poll").forEach { (k, lbl) ->
                                                val active = composerType == k
                                                Surface(
                                                    onClick = { composerType = k },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (active) CrimsonAccent else palette.recessedTrayBg
                                                ) {
                                                    Text(lbl, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (active) Color.White else palette.secondaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = alertTitle,
                                        onValueChange = { alertTitle = it },
                                        label = { Text(if (composerType == "poll") "Poll Question" else "Notice Title (e.g. Deploy Freeze)") },
                                        singleLine = true,
                                        colors = teamFieldColors(),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("team_alert_title_input")
                                    )
                                    OutlinedTextField(
                                        value = alertMessage,
                                        onValueChange = { alertMessage = it },
                                        label = { Text(if (composerType == "poll") "Context / Deadline (optional)" else "Message pushed to tagged/team devices") },
                                        colors = teamFieldColors(),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("team_alert_message_input")
                                    )
                                    if (composerType == "poll") {
                                        OutlinedTextField(value = pollOption1, onValueChange = { pollOption1 = it }, label = { Text("Option 1") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = pollOption2, onValueChange = { pollOption2 = it }, label = { Text("Option 2") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = pollOption3, onValueChange = { pollOption3 = it }, label = { Text("Option 3 (optional)") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                                    }
                                    if (members.isNotEmpty()) {
                                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            members.forEach { m ->
                                                val tagged = m.uid in noticeTaggedUids
                                                Surface(
                                                    onClick = { if (tagged) noticeTaggedUids.remove(m.uid) else noticeTaggedUids.add(m.uid) },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (tagged) palette.accentSoftBg else palette.cardSurfaceTop,
                                                    border = BorderStroke(1.dp, if (tagged) CrimsonAccent else palette.specularRimOutline),
                                                    modifier = Modifier.testTag("notice_tag_${m.uid}")
                                                ) {
                                                    Text("@${m.displayName}", style = MaterialTheme.typography.labelSmall, fontWeight = if (tagged) FontWeight.Bold else FontWeight.Normal, color = if (tagged) CrimsonAccent else palette.primaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                                }
                                            }
                                        }
                                    }
                                    val canPost = alertTitle.isNotBlank() && (if (composerType == "poll") pollOption1.isNotBlank() && pollOption2.isNotBlank() else alertMessage.isNotBlank())
                                    TactilePrimaryPillButton(
                                        text = if (composerType == "poll") "Launch Live Poll" else "Post Notice & Notify",
                                        iconType = MinimalIconType.SEND,
                                        enabled = canPost,
                                        onClick = {
                                            val tagged = members.filter { it.uid in noticeTaggedUids }
                                            if (composerType == "poll" && onSendAlertExtended != null) {
                                                val optsJson = PollVotesCodec.encodeOptions(listOf(pollOption1, pollOption2, pollOption3))
                                                onSendAlertExtended(activeTeam.id, alertTitle.trim(), alertMessage.trim(), tagged, "poll", optsJson)
                                            } else if (onSendAlertExtended != null) {
                                                onSendAlertExtended(activeTeam.id, alertTitle.trim(), alertMessage.trim(), tagged, "notice", "[]")
                                            } else {
                                                onSendTeamAlert(activeTeam.id, alertTitle.trim(), alertMessage.trim(), tagged)
                                            }
                                            alertTitle = ""
                                            alertMessage = ""
                                            pollOption1 = ""
                                            pollOption2 = ""
                                            pollOption3 = ""
                                            noticeTaggedUids.clear()
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("team_send_alert_button")
                                    )
                                }
                            }
                        }

                        items(items = notifications, key = { it.id }, contentType = { "team_notif" }) { notif ->
                            TeamPollNoticeCard(
                                notif = notif,
                                currentUserUid = currentUserUid,
                                onVotePoll = { optionIdx -> onVotePoll(activeTeam.id, notif.id, optionIdx) }
                            )
                        }
                    }

                    TeamSectionTab.ACTIVITY -> {
                        teamActivityFeedItems(activities = activities)
                    }

                    TeamSectionTab.ANALYTICS -> {
                        item(key = "analytics_gauge", contentType = "analytics_gauge") {
                            TeamCompletionGaugeCard(summary = analyticsSummary)
                        }
                        item(key = "analytics_member_bars", contentType = "analytics_members") {
                            MemberPerformanceBarsCard(memberStats = analyticsSummary.memberStats)
                        }
                        item(key = "analytics_weekly_velocity", contentType = "analytics_velocity") {
                            WeeklyVelocityChartCard(
                                weeklyVelocity = analyticsSummary.weeklyVelocity,
                                onDaySelected = { day ->
                                    selectedDayFilter = day
                                    taskViewMode = TeamTaskViewMode.SCHEDULE
                                    activeTab = TeamSectionTab.SCHEDULE
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            isBusy = isBusy,
            onDismiss = { showCreateTeamDialog = false },
            onCreate = { name, desc, code -> onCreateTeam(name, desc, code) { showCreateTeamDialog = false } }
        )
    }

    if (showJoinTeamDialog) {
        JoinByInviteCodeDialog(
            isBusy = isBusy,
            preview = invitePreview,
            onLookup = onLookupInviteCode,
            onDismiss = { showJoinTeamDialog = false },
            onJoin = { code -> onJoinTeamByCode(code) { showJoinTeamDialog = false } }
        )
    }

    if (showInviteEmailDialog && activeTeam != null) {
        InviteByEmailDialog(
            teamName = activeTeam.name,
            pendingInvites = emailInvites.filter { it.status == "PENDING" },
            isBusy = isBusy,
            onDismiss = { showInviteEmailDialog = false },
            onInvite = { email, role, roleTag ->
                onInviteByEmail(activeTeam, email, role, roleTag) { showInviteEmailDialog = false }
            },
            onRevokeInvite = onRevokeEmailInvite
        )
    }

    if (editingMember != null && activeTeam != null) {
        val target = editingMember!!
        val canRemove = isCurrentUserAdminOrOwner && target.role != "OWNER" && target.uid != currentUserUid
        EditMemberRoleStatusDialog(
            member = target,
            isCurrentUserOwner = isCurrentUserOwner,
            isCurrentUserAdminOrOwner = isCurrentUserAdminOrOwner,
            canRemoveTarget = canRemove,
            onDismiss = { editingMember = null },
            onRemoveMember = if (canRemove) {
                {
                    editingMember = null
                    pendingDeleteTitle = "Remove Team Member?"
                    pendingDeleteMessage = "Remove ${target.displayName} from \"${activeTeam.name}\"? They will lose live workspace access immediately."
                    pendingDeleteAction = { onRemoveMember(activeTeam.id, target) }
                }
            } else null,
            onSave = { roleTag, customStatus, newRole ->
                onUpdateMemberProfile(activeTeam.id, target, roleTag, customStatus, newRole)
                editingMember = null
            }
        )
    }

    if (showScheduleDialog && activeTeam != null) {
        ScheduleTaskDialog(
            members = members,
            initialDay = selectedDayFilter,
            existingTask = editingTask,
            onDismiss = { showScheduleDialog = false; editingTask = null },
            onSave = { title, desc, day, dateLabel, start, end, assigned, priority, status, deadlineMillis, driveUrl ->
                if (onSaveTaskExtended != null) {
                    onSaveTaskExtended(activeTeam.id, title, desc, day, dateLabel, start, end, assigned, priority, status, deadlineMillis, driveUrl, editingTask)
                } else {
                    onSaveScheduleTask(activeTeam.id, title, desc, day, dateLabel, start, end, assigned, priority, status, editingTask)
                }
                showScheduleDialog = false
                editingTask = null
            }
        )
    }

    if (showNoteDialog && activeTeam != null) {
        SharedNotionPageDialog(
            existingDoc = editingNote,
            initialParentId = newNoteParentId,
            initialTitle = newNoteInitialTitle,
            allDocs = docs,
            members = members,
            playbackState = playbackState,
            onDismiss = {
                showNoteDialog = false
                editingNote = null
                newNoteParentId = null
                newNoteInitialTitle = ""
            },
            onSave = { title, content, url, blocksJson, tagsCsv, parentId, icon, isPinned ->
                if (onSavePageExtended != null) {
                    onSavePageExtended(activeTeam.id, title, content, url, blocksJson, tagsCsv, parentId, icon, isPinned, editingNote)
                } else {
                    onSaveSharedNote(activeTeam.id, title, content, url, blocksJson, tagsCsv, editingNote)
                }
                showNoteDialog = false
                editingNote = null
                newNoteParentId = null
                newNoteInitialTitle = ""
            },
            onOpenNestedDoc = { target ->
                editingNote = target
                newNoteParentId = target.parentId
                newNoteInitialTitle = ""
            },
            onCreateNestedSubpage = { subTitle, pId ->
                editingNote = null
                newNoteParentId = pId
                newNoteInitialTitle = subTitle
            },
            onPolishPageWithAi = onPolishPageWithAi,
            onLogClientPaymentToLedger = onLogClientPaymentToLedger,
            onScheduleInlineReminder = { rem -> onScheduleInlineReminder(activeTeam.id, rem) },
            onSyncScheduleTask = { sched -> onSyncScheduleBlock(activeTeam.id, sched, members) },
            onLogFocusTimerSeconds = onLogFocusTimerSeconds,
            onStartVoiceRecord = onStartInlineVoiceRecord,
            onStopVoiceRecord = onStopInlineVoiceRecord,
            onToggleVoicePlayback = onToggleVoicePlayback,
            onSeekVoicePlayback = onSeekVoicePlayback
        )
    }

    pendingDeleteAction?.let { action ->
        LiquidDeleteConfirmationDialog(
            title = pendingDeleteTitle,
            message = pendingDeleteMessage,
            onConfirm = {
                action()
                pendingDeleteAction = null
            },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}
