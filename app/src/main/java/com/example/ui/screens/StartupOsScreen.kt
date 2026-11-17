package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.local.StartupPhaseItemEntity
import com.example.data.local.StartupProjectEntity
import com.example.data.local.StartupSprintTaskEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamWorkspaceEntity
import com.example.data.repository.BusinessCrmDeal
import com.example.data.repository.BusinessOkrItem
import com.example.data.repository.BusinessSopDoc
import com.example.data.repository.OsRepository
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

private enum class StartupOsSection(val label: String) {
    ROADMAP("Roadmap"),
    KANBAN("Sprint"),
    CRM("CRM"),
    OKRS("OKRs"),
    SOPS("SOPs"),
    AI_SPEC("AI Spec")
}

private val STARTUP_STAGES = listOf("IDEATION", "MVP", "BETA", "LIVE", "SCALING")
private val PROJECT_TYPES = listOf("STARTUP", "AGENCY", "PRODUCT")

@Composable
fun StartupOsScreen(
    projects: List<StartupProjectEntity>,
    phaseItems: List<StartupPhaseItemEntity>,
    sprintTasks: List<StartupSprintTaskEntity>,
    isGeneratingBlueprint: Boolean,
    aiError: String?,
    onCreateProject: (name: String, tagline: String, persona: String, techStack: String, stage: String, onCreated: (String) -> Unit) -> Unit,
    onUpdateStage: (startupId: String, stage: String) -> Unit,
    onTogglePhaseItem: (StartupPhaseItemEntity) -> Unit,
    onAddCustomPhaseItem: (startupId: String, phaseNumber: Int, title: String, notes: String, url: String?) -> Unit,
    onDeletePhaseItem: (StartupPhaseItemEntity) -> Unit,
    onSaveSprintTask: (startupId: String, id: String?, title: String, desc: String, col: String, priority: String, tag: String) -> Unit,
    onMoveSprintTaskColumn: (StartupSprintTaskEntity, String) -> Unit,
    onDeleteSprintTask: (StartupSprintTaskEntity) -> Unit,
    onDeleteProject: (String) -> Unit,
    onGenerateAiBlueprint: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    financeTransactions: List<FinanceTransactionEntity> = emptyList(),
    financeSectors: List<FinanceSectorEntity> = emptyList(),
    teams: List<TeamWorkspaceEntity> = emptyList(),
    activeTeamMembers: List<TeamMemberEntity> = emptyList(),
    onSelectTeamForShare: (String) -> Unit = {},
    onUpdateProjectType: (startupId: String, projectType: String) -> Unit = { _, _ -> },
    onSaveCrmDeals: (startupId: String, deals: List<BusinessCrmDeal>) -> Unit = { _, _ -> },
    onSaveOkrs: (startupId: String, okrs: List<BusinessOkrItem>) -> Unit = { _, _ -> },
    onSaveSopDocs: (startupId: String, docs: List<BusinessSopDoc>) -> Unit = { _, _ -> },
    onShareWorkflowToTeam: (
        startupId: String,
        teamId: String,
        taggedMembers: List<TeamMemberEntity>,
        includeTasks: Boolean,
        includeSop: Boolean,
        headline: String,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, onDone -> onDone() }
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LiquidTheme.palette
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var activeSection by remember { mutableStateOf(StartupOsSection.ROADMAP) }
    var selectedKanbanColumn by remember { mutableStateOf("BACKLOG") }

    var showNewStartupDialog by remember { mutableStateOf(false) }
    var addStepTargetPhase by remember { mutableStateOf<Int?>(null) }
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var showAddCrmDialog by remember { mutableStateOf(false) }
    var showAddOkrDialog by remember { mutableStateOf(false) }
    var showAddSopDialog by remember { mutableStateOf(false) }
    var showShareTeamDialog by remember { mutableStateOf(false) }

    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(projects) {
        if (selectedProjectId == null || projects.none { it.id == selectedProjectId }) {
            selectedProjectId = projects.firstOrNull()?.id
        }
    }

    val activeProject = remember(projects, selectedProjectId) {
        projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()
    }
    val activePhases = remember(phaseItems, activeProject?.id) {
        val pid = activeProject?.id ?: return@remember emptyList()
        phaseItems.filter { it.startupId == pid }
    }
    val activeTasks = remember(sprintTasks, activeProject?.id) {
        val pid = activeProject?.id ?: return@remember emptyList()
        sprintTasks.filter { it.startupId == pid }
    }
    val activeCrmDeals = remember(activeProject?.crmDealsJson) {
        BusinessCrmDeal.parseList(activeProject?.crmDealsJson.orEmpty())
    }
    val activeOkrs = remember(activeProject?.okrsJson) {
        BusinessOkrItem.parseList(activeProject?.okrsJson.orEmpty())
    }
    val activeSops = remember(activeProject?.sopDocsJson) {
        BusinessSopDoc.parseList(activeProject?.sopDocsJson.orEmpty())
    }
    val ledgerSummary = remember(financeTransactions, financeSectors) {
        OsRepository.computeLedgerSummary(financeTransactions, financeSectors)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("startup_os_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "startup_header", contentType = "header") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FloatingGlassIconButton(iconType = MinimalIconType.BACK, contentDescription = "Back", onClick = onBack, modifier = Modifier.testTag("startup_os_back_button"))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Startup & Business OS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                        Text("CRM, OKRs, SOPs, Roadmap, Money Ledger & Team Sync", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                    }
                    TactilePrimaryPillButton(text = "+ Venture", iconType = MinimalIconType.PLUS, onClick = { showNewStartupDialog = true }, modifier = Modifier.testTag("startup_new_venture_button"))
                }
            }

            if (projects.isNotEmpty()) {
                item(key = "startup_switcher", contentType = "switcher") {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        projects.forEach { proj ->
                            val selected = proj.id == activeProject?.id
                            Surface(
                                onClick = { selectedProjectId = proj.id },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.testTag("startup_chip_${proj.id}")
                            ) {
                                Text("${proj.name} • ${proj.stage}", style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) palette.primaryButtonContent else palette.primaryInk, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                            }
                        }
                    }
                }
            }

            if (activeProject == null) {
                item(key = "startup_empty_state", contentType = "empty_state") {
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Build Your Next Venture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            Text("Launch a structured 6-phase founder playbook with Client CRM, OKRs, SOP templates, live Money Ledger snapshot, and 1-click Team OS sync.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryInk)
                            TactilePrimaryPillButton(text = "+ Launch First Venture", iconType = MinimalIconType.SPARKLE, onClick = { showNewStartupDialog = true }, modifier = Modifier.testTag("startup_empty_create_button"))
                        }
                    }
                }
            } else {
                val completedSteps = activePhases.count { it.isCompleted }
                val totalSteps = activePhases.size.coerceAtLeast(1)
                val progress = (completedSteps.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

                item(key = "startup_overview_${activeProject.id}", contentType = "overview") {
                    FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(activeProject.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                    if (activeProject.tagline.isNotBlank()) {
                                        Text(activeProject.tagline, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        pendingDeleteTitle = "Delete Venture?"
                                        pendingDeleteMessage = "Delete \"${activeProject.name}\" and all its roadmap steps, CRM deals & sprint tasks?"
                                        pendingDeleteAction = { onDeleteProject(activeProject.id) }
                                    },
                                    modifier = Modifier.testTag("startup_delete_venture_button")
                                ) {
                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 18.dp, contentDescription = "Delete startup venture")
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PROJECT_TYPES.forEach { pType ->
                                    val active = activeProject.projectType == pType
                                    Surface(
                                        onClick = { onUpdateProjectType(activeProject.id, pType) },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (active) palette.accentSoftBg else palette.recessedTrayBg,
                                        border = BorderStroke(1.dp, if (active) CrimsonAccent else palette.specularRimOutline)
                                    ) {
                                        Text(pType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (active) CrimsonAccent else palette.secondaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                STARTUP_STAGES.forEach { st ->
                                    val active = activeProject.stage == st
                                    Surface(
                                        onClick = { onUpdateStage(activeProject.id, st) },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (active) CrimsonAccent else palette.frostedPillBg,
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier.testTag("startup_stage_${st.lowercase()}")
                                    ) {
                                        Text(st, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (active) Color.White else palette.primaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Execution Roadmap", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                    Text("$completedSteps / ${activePhases.size} complete", style = MaterialTheme.typography.labelMedium, color = CrimsonAccent, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(9999.dp)), color = CrimsonAccent, trackColor = palette.recessedTrayBg)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TactilePrimaryPillButton(
                                    text = if (isGeneratingBlueprint) "Generating..." else "AI Spec",
                                    iconType = MinimalIconType.SPARKLE,
                                    enabled = !isGeneratingBlueprint,
                                    onClick = {
                                        onGenerateAiBlueprint(activeProject.id)
                                        activeSection = StartupOsSection.AI_SPEC
                                    },
                                    modifier = Modifier.weight(1f).testTag("startup_generate_ai_spec_button")
                                )
                                OutlinedButton(
                                    onClick = {
                                        val initId = activeProject.sharedTeamId ?: teams.firstOrNull()?.id
                                        if (!initId.isNullOrBlank()) onSelectTeamForShare(initId)
                                        showShareTeamDialog = true
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    border = BorderStroke(1.dp, CrimsonAccent),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("startup_share_team_button")
                                ) {
                                    MinimalIcon(type = MinimalIconType.SHARE, tint = CrimsonAccent, size = 15.dp)
                                    Text(" Share Team", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                                }
                            }
                        }
                    }
                }

                item(key = "startup_financial_snapshot", contentType = "finance_snapshot") {
                    LiveFinancialSnapshotCard(ledgerSummary = ledgerSummary, crmDeals = activeCrmDeals)
                }

                item(key = "startup_section_tabs", contentType = "segment_switcher") {
                    RecessedFramingBezel(modifier = Modifier.fillMaxWidth(), cornerRadius = 9999.dp, innerPadding = PaddingValues(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StartupOsSection.entries.forEach { sec ->
                                val selected = activeSection == sec
                                Surface(
                                    onClick = { activeSection = sec },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (selected) palette.primaryButtonBg else Color.Transparent,
                                    border = if (selected) BorderStroke(1.dp, palette.specularRimOutline) else null,
                                    modifier = Modifier.testTag("startup_tab_${sec.name.lowercase()}")
                                ) {
                                    Text(sec.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, color = if (selected) palette.primaryButtonContent else palette.secondaryInk, maxLines = 1, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                }

                when (activeSection) {
                    StartupOsSection.ROADMAP -> {
                        val grouped = activePhases.groupBy { it.phaseNumber }
                        for (phaseNum in 1..6) {
                            val phaseTitle = OsRepository.STARTUP_PHASE_TITLES[phaseNum] ?: "Phase $phaseNum"
                            val itemsInPhase = grouped[phaseNum].orEmpty().sortedBy { it.sortOrder }
                            item(key = "phase_$phaseNum", contentType = "phase_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(phaseTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk, modifier = Modifier.weight(1f))
                                            Surface(onClick = { addStepTargetPhase = phaseNum }, shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg, border = BorderStroke(1.dp, palette.specularRimOutline), modifier = Modifier.testTag("phase_add_step_$phaseNum")) {
                                                Text("+ Step", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.accentSoftInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                            }
                                        }
                                        if (itemsInPhase.isEmpty()) {
                                            Text("No milestone steps yet.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                        } else {
                                            itemsInPhase.forEach { step ->
                                                Surface(shape = RoundedCornerShape(16.dp), color = palette.cardSurfaceTop.copy(alpha = 0.72f), border = BorderStroke(1.dp, palette.specularRimOutline), modifier = Modifier.fillMaxWidth()) {
                                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Checkbox(checked = step.isCompleted, onCheckedChange = { onTogglePhaseItem(step) }, colors = CheckboxDefaults.colors(checkedColor = CrimsonAccent, uncheckedColor = palette.secondaryInk, checkmarkColor = Color.White), modifier = Modifier.testTag("phase_step_check_${step.id}"))
                                                        Column(modifier = Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text(step.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryInk, textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                                                            if (step.notes.isNotBlank()) Text(step.notes, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                                            if (!step.resourceUrl.isNullOrBlank()) {
                                                                Surface(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(step.resourceUrl))) } }, shape = RoundedCornerShape(9999.dp), color = palette.frostedPillBg, border = BorderStroke(1.dp, palette.specularRimOutline)) {
                                                                    Text("Open Playbook Link", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                                                }
                                                            }
                                                        }
                                                        IconButton(onClick = {
                                                            pendingDeleteTitle = "Delete Milestone?"
                                                            pendingDeleteMessage = "Remove \"${step.title}\" from $phaseTitle?"
                                                            pendingDeleteAction = { onDeletePhaseItem(step) }
                                                        }, modifier = Modifier.size(32.dp)) {
                                                            MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete milestone step")
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

                    StartupOsSection.KANBAN -> {
                        item(key = "kanban_controls", contentType = "kanban_controls") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OsRepository.SPRINT_COLUMNS.forEach { col ->
                                        val count = activeTasks.count { it.columnStatus == col }
                                        val selected = selectedKanbanColumn == col
                                        Surface(onClick = { selectedKanbanColumn = col }, shape = RoundedCornerShape(9999.dp), color = if (selected) palette.primaryButtonBg else palette.frostedPillBg, border = BorderStroke(1.dp, palette.specularRimOutline), modifier = Modifier.testTag("kanban_col_${col.lowercase()}")) {
                                            Text("$col ($count)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) palette.primaryButtonContent else palette.primaryInk, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                        }
                                    }
                                }
                                TactilePrimaryPillButton(text = "+ Add Task to $selectedKanbanColumn", iconType = MinimalIconType.PLUS, onClick = { showNewTaskDialog = true }, modifier = Modifier.fillMaxWidth().testTag("kanban_add_task_button"))
                            }
                        }
                        val columnTasks = activeTasks.filter { it.columnStatus == selectedKanbanColumn }
                        if (columnTasks.isEmpty()) {
                            item(key = "kanban_empty", contentType = "empty") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(22.dp)) { Text("No sprint cards in $selectedKanbanColumn", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk) }
                                }
                            }
                        } else {
                            items(columnTasks.size, key = { columnTasks[it].id }) { idx ->
                                val task = columnTasks[idx]
                                val colIdx = OsRepository.SPRINT_COLUMNS.indexOf(task.columnStatus)
                                TactileGlassCard(modifier = Modifier.fillMaxWidth().testTag("kanban_task_card_${task.id}"), cornerRadius = 22.dp) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(shape = RoundedCornerShape(9999.dp), color = if (task.priority == "HIGH") CrimsonAccent else palette.accentSoftBg) {
                                                    Text(task.priority, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (task.priority == "HIGH") Color.White else palette.accentSoftInk, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                }
                                                Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                                                    Text(task.tag, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                }
                                            }
                                            IconButton(onClick = {
                                                pendingDeleteTitle = "Delete Sprint Card?"
                                                pendingDeleteMessage = "Remove \"${task.title}\" from ${task.columnStatus}?"
                                                pendingDeleteAction = { onDeleteSprintTask(task) }
                                            }, modifier = Modifier.size(30.dp)) {
                                                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete sprint card")
                                            }
                                        }
                                        Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                        if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (colIdx > 0) {
                                                val prevCol = OsRepository.SPRINT_COLUMNS[colIdx - 1]
                                                OutlinedButton(onClick = { onMoveSprintTaskColumn(task, prevCol) }, shape = RoundedCornerShape(9999.dp), modifier = Modifier.weight(1f)) {
                                                    Text("<- $prevCol", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                                                }
                                            }
                                            if (colIdx in 0 until OsRepository.SPRINT_COLUMNS.lastIndex) {
                                                val nextCol = OsRepository.SPRINT_COLUMNS[colIdx + 1]
                                                TactilePrimaryPillButton(text = "Move -> $nextCol", onClick = { onMoveSprintTaskColumn(task, nextCol) }, modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StartupOsSection.CRM -> {
                        businessCrmSection(
                            deals = activeCrmDeals,
                            onAddDealClick = { showAddCrmDialog = true },
                            onAdvanceDealStage = { deal ->
                                val idx = BusinessCrmDeal.ALL_STAGES.indexOf(deal.stage)
                                val next = BusinessCrmDeal.ALL_STAGES.getOrNull(idx + 1) ?: return@businessCrmSection
                                val updated = activeCrmDeals.map { if (it.id == deal.id) it.copy(stage = next, updatedAtMillis = System.currentTimeMillis()) else it }
                                onSaveCrmDeals(activeProject.id, updated)
                            },
                            onDeleteDeal = { deal ->
                                pendingDeleteTitle = "Delete CRM Deal?"
                                pendingDeleteMessage = "Remove \"${deal.clientName}\" from pipeline?"
                                pendingDeleteAction = { onSaveCrmDeals(activeProject.id, activeCrmDeals.filterNot { it.id == deal.id }) }
                            }
                        )
                    }

                    StartupOsSection.OKRS -> {
                        businessOkrsSection(
                            okrs = activeOkrs,
                            onAddOkrClick = { showAddOkrDialog = true },
                            onIncrementOkr = { okr ->
                                val step = (okr.targetValue / 10.0).coerceAtLeast(1.0)
                                val nextVal = (okr.currentValue + step).coerceAtMost(okr.targetValue)
                                val updated = activeOkrs.map { if (it.id == okr.id) it.copy(currentValue = nextVal) else it }
                                onSaveOkrs(activeProject.id, updated)
                            },
                            onDeleteOkr = { okr ->
                                pendingDeleteTitle = "Delete OKR?"
                                pendingDeleteMessage = "Remove \"${okr.objective}\"?"
                                pendingDeleteAction = { onSaveOkrs(activeProject.id, activeOkrs.filterNot { it.id == okr.id }) }
                            }
                        )
                    }

                    StartupOsSection.SOPS -> {
                        businessSopsSection(
                            sops = activeSops,
                            onAddSopClick = { showAddSopDialog = true },
                            onDeleteSop = { sop ->
                                pendingDeleteTitle = "Delete SOP Template?"
                                pendingDeleteMessage = "Remove \"${sop.title}\"?"
                                pendingDeleteAction = { onSaveSopDocs(activeProject.id, activeSops.filterNot { it.id == sop.id }) }
                            }
                        )
                    }

                    StartupOsSection.AI_SPEC -> {
                        item(key = "ai_spec_card", contentType = "ai_spec") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("AI Business Plan & Tech Spec", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                        if (isGeneratingBlueprint) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = CrimsonAccent)
                                    }
                                    if (!aiError.isNullOrBlank()) Text(aiError, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent)
                                    if (activeProject.aiPlanMarkdown.isBlank()) {
                                        Text("Tap \"AI Spec\" above to synthesize an architecture blueprint, unit economics pricing model, and MVP GTM strategy tailored to ${activeProject.name}.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryInk)
                                    } else {
                                        Text(activeProject.aiPlanMarkdown, style = MaterialTheme.typography.bodyMedium, color = palette.primaryInk, modifier = Modifier.testTag("startup_ai_plan_markdown"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewStartupDialog) {
        CreateStartupDialog(
            onDismiss = { showNewStartupDialog = false },
            onSave = { name, tagline, persona, stack, stage ->
                onCreateProject(name, tagline, persona, stack, stage) { newId -> selectedProjectId = newId }
                showNewStartupDialog = false
            }
        )
    }

    addStepTargetPhase?.let { phaseNum ->
        AddPhaseStepDialog(
            phaseTitle = OsRepository.STARTUP_PHASE_TITLES[phaseNum] ?: "Phase $phaseNum",
            onDismiss = { addStepTargetPhase = null },
            onSave = { title, notes, url ->
                activeProject?.let { proj -> onAddCustomPhaseItem(proj.id, phaseNum, title, notes, url) }
                addStepTargetPhase = null
            }
        )
    }

    if (showNewTaskDialog && activeProject != null) {
        AddSprintTaskDialog(
            defaultColumn = selectedKanbanColumn,
            onDismiss = { showNewTaskDialog = false },
            onSave = { title, desc, priority, tag ->
                onSaveSprintTask(activeProject.id, null, title, desc, selectedKanbanColumn, priority, tag)
                showNewTaskDialog = false
            }
        )
    }

    if (showAddCrmDialog && activeProject != null) {
        AddCrmDealDialog(
            onDismiss = { showAddCrmDialog = false },
            onSave = { deal ->
                onSaveCrmDeals(activeProject.id, activeCrmDeals + deal)
                showAddCrmDialog = false
            }
        )
    }

    if (showAddOkrDialog && activeProject != null) {
        AddBusinessOkrDialog(
            onDismiss = { showAddOkrDialog = false },
            onSave = { okr ->
                onSaveOkrs(activeProject.id, activeOkrs + okr)
                showAddOkrDialog = false
            }
        )
    }

    if (showAddSopDialog && activeProject != null) {
        AddBusinessSopDialog(
            projectName = activeProject.name,
            onDismiss = { showAddSopDialog = false },
            onSave = { sop ->
                onSaveSopDocs(activeProject.id, activeSops + sop)
                showAddSopDialog = false
            }
        )
    }

    if (showShareTeamDialog && activeProject != null) {
        ShareWorkflowToTeamDialog(
            projectName = activeProject.name,
            teams = teams,
            activeTeamMembers = activeTeamMembers,
            initialSelectedTeamId = activeProject.sharedTeamId,
            onSelectTeamForMembers = onSelectTeamForShare,
            onDismiss = { showShareTeamDialog = false },
            onConfirmShare = { teamId, tagged, incTasks, incSop, headline ->
                onShareWorkflowToTeam(activeProject.id, teamId, tagged, incTasks, incSop, headline) {
                    showShareTeamDialog = false
                }
            }
        )
    }

    pendingDeleteAction?.let { action ->
        LiquidDeleteConfirmationDialog(
            title = pendingDeleteTitle,
            message = pendingDeleteMessage,
            onConfirm = { action(); pendingDeleteAction = null },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}

@Composable
private fun CreateStartupDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, tagline: String, persona: String, techStack: String, stage: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf("") }
    var stack by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("IDEATION") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.cardSurfaceTop,
        unfocusedContainerColor = palette.cardSurfaceTop,
        focusedBorderColor = CrimsonAccent,
        unfocusedBorderColor = palette.inputBorder,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Launch New Venture", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Startup / Business Name") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("startup_name_input"))
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("1-line Value Proposition") }, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = persona, onValueChange = { persona = it }, label = { Text("Target Customer / ICP") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = stack, onValueChange = { stack = it }, label = { Text("Tech Stack (e.g. Kotlin, Firebase)") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STARTUP_STAGES.forEach { st ->
                        val active = stage == st
                        Surface(onClick = { stage = st }, shape = RoundedCornerShape(9999.dp), color = if (active) CrimsonAccent else palette.recessedTrayBg) {
                            Text(st, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(text = "Create Venture", enabled = name.isNotBlank(), onClick = { onSave(name.trim(), tagline.trim(), persona.trim(), stack.trim(), stage) }, modifier = Modifier.testTag("startup_confirm_create_button"))
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}

@Composable
private fun AddPhaseStepDialog(
    phaseTitle: String,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, url: String?) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val fieldColors = teamFieldColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add Step • $phaseTitle", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Milestone Title") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Execution Details") }, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Reference URL (optional)") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = { TactilePrimaryPillButton(text = "Add Step", enabled = title.isNotBlank(), onClick = { onSave(title.trim(), notes.trim(), url.trim().takeIf { it.isNotBlank() }) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}

@Composable
private fun AddSprintTaskDialog(
    defaultColumn: String,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, priority: String, tag: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var tag by remember { mutableStateOf("Core") }
    val fieldColors = teamFieldColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add Card • $defaultColumn", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Deliverable Title") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Acceptance Criteria / Notes") }, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = tag, onValueChange = { tag = it }, label = { Text("Tag (e.g. Core, API, Growth)") }, singleLine = true, colors = fieldColors, shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { lvl ->
                        val active = priority == lvl
                        Surface(onClick = { priority = lvl }, shape = RoundedCornerShape(9999.dp), color = if (active) CrimsonAccent else palette.recessedTrayBg, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(lvl, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TactilePrimaryPillButton(text = "Save Card", enabled = title.isNotBlank(), onClick = { onSave(title.trim(), description.trim(), priority, tag.trim()) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}
