package com.example.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ai.AiLibraryService
import com.example.data.cloud.CloudOsSync
import com.example.data.local.AppDatabase
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.local.StartupPhaseItemEntity
import com.example.data.local.StartupProjectEntity
import com.example.data.local.StartupSprintTaskEntity
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamEmailInviteEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.local.TeamWorkspaceEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import kotlin.math.abs

enum class LedgerTimeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

data class SectorBreakdownItem(
    val sectorId: String,
    val sectorName: String,
    val colorHex: String,
    val totalCents: Long,
    val fraction: Float,
    val txType: String
)

data class FinanceLedgerSummary(
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val totalTransferCents: Long = 0L,
    val netBalanceCents: Long = 0L,
    val savingsRatePercent: Int = 0,
    val expenseBreakdown: List<SectorBreakdownItem> = emptyList(),
    val incomeBreakdown: List<SectorBreakdownItem> = emptyList()
)

class OsRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val cloudOsSync: CloudOsSync,
    private val aiLibraryService: AiLibraryService
) {
    private val dao = database.osDao()
    private val seenNotifPrefs by lazy {
        context.applicationContext.getSharedPreferences("team_seen_notifs", Context.MODE_PRIVATE)
    }

    // --- Observable Flows ---
    val startupProjects: Flow<List<StartupProjectEntity>> = dao.observeStartupProjects()
    val startupPhaseItems: Flow<List<StartupPhaseItemEntity>> = dao.observeAllStartupPhaseItems()
    val startupSprintTasks: Flow<List<StartupSprintTaskEntity>> = dao.observeAllSprintTasks()

    val financeSectors: Flow<List<FinanceSectorEntity>> = dao.observeFinanceSectors()
    val financeTransactions: Flow<List<FinanceTransactionEntity>> = dao.observeFinanceTransactions()

    val teams: Flow<List<TeamWorkspaceEntity>> = dao.observeTeams()
    fun observeTeamMembers(teamId: String): Flow<List<TeamMemberEntity>> = dao.observeTeamMembers(teamId)
    fun observeTeamTasks(teamId: String): Flow<List<TeamSharedTaskEntity>> = dao.observeTeamTasks(teamId)
    fun observeTeamDocs(teamId: String): Flow<List<TeamSharedDocEntity>> = dao.observeTeamDocs(teamId)
    fun observeTeamNotifications(teamId: String): Flow<List<TeamNotificationEntity>> = dao.observeTeamNotifications(teamId)
    fun observeTeamActivities(teamId: String): Flow<List<TeamActivityEntity>> = dao.observeTeamActivities(teamId)
    fun observeTeamEmailInvites(teamId: String): Flow<List<TeamEmailInviteEntity>> = dao.observeTeamEmailInvites(teamId)

    private fun currentAuthUid(): String? = try {
        Firebase.auth.currentUser?.uid
    } catch (_: Exception) {
        null
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // =========================================================================
    // 1. Startup & Business Builder OS
    // =========================================================================

    suspend fun createStartupProject(
        name: String,
        tagline: String,
        targetPersona: String,
        techStack: String,
        stage: String = "IDEATION",
        projectType: String = "STARTUP",
        cloudSync: Boolean = true
    ): StartupProjectEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val projectId = UUID.randomUUID().toString()
        val validStage = if (stage in setOf("IDEATION", "MVP", "BETA", "LIVE", "SCALING")) stage else "IDEATION"
        val validType = if (projectType in setOf("STARTUP", "AGENCY", "PRODUCT")) projectType else "STARTUP"
        val defaultSops = BusinessSopDoc.encodeList(
            listOf(
                BusinessSopDoc.TYPE_CANVAS,
                BusinessSopDoc.TYPE_PITCH_DECK,
                BusinessSopDoc.TYPE_PRICING,
                BusinessSopDoc.TYPE_SOP
            ).map { tpl ->
                val (tplTitle, tplContent) = BusinessSopDoc.defaultTemplateContent(tpl, name.trim().ifBlank { "Venture" })
                BusinessSopDoc(
                    title = tplTitle,
                    templateType = tpl,
                    content = tplContent,
                    updatedAtMillis = now
                )
            }
        )
        val project = StartupProjectEntity(
            id = projectId,
            userId = currentAuthUid(),
            name = name.trim().ifBlank { "Untitled Venture" },
            tagline = tagline.trim(),
            stage = validStage,
            projectType = validType,
            targetPersona = targetPersona.trim(),
            techStack = techStack.trim(),
            sopDocsJson = defaultSops,
            cloudSync = cloudSync,
            createdAt = now,
            updatedAt = now
        )
        val seededPhases = defaultPhasesForStartup(projectId, now)
        val starterTasks = defaultSprintTasksForStartup(projectId, now)

        dao.upsertStartupProject(project)
        dao.upsertStartupPhaseItems(seededPhases)
        dao.upsertSprintTasks(starterTasks)

        if (cloudSync && currentAuthUid() != null && isOnline()) {
            cloudOsSync.pushStartupProject(project, seededPhases, starterTasks)
        }
        project
    }

    suspend fun updateStartupStage(startupId: String, stage: String) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val validStage = if (stage in setOf("IDEATION", "MVP", "BETA", "LIVE", "SCALING")) stage else existing.stage
        val now = System.currentTimeMillis()
        dao.updateStartupStage(startupId, validStage, now)
        syncStartupIfConnected(existing.copy(stage = validStage, updatedAt = now))
    }

    suspend fun updateStartupProjectType(startupId: String, projectType: String) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val validType = if (projectType in setOf("STARTUP", "AGENCY", "PRODUCT")) projectType else existing.projectType
        val now = System.currentTimeMillis()
        dao.updateStartupBusinessModules(
            id = startupId,
            projectType = validType,
            crmDealsJson = existing.crmDealsJson,
            okrsJson = existing.okrsJson,
            sopDocsJson = existing.sopDocsJson,
            sharedTeamId = existing.sharedTeamId,
            updatedAt = now
        )
        syncStartupIfConnected(existing.copy(projectType = validType, updatedAt = now))
    }

    suspend fun saveBusinessCrmDeals(startupId: String, deals: List<BusinessCrmDeal>) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val json = BusinessCrmDeal.encodeList(deals)
        val now = System.currentTimeMillis()
        dao.updateStartupBusinessModules(
            id = startupId,
            projectType = existing.projectType,
            crmDealsJson = json,
            okrsJson = existing.okrsJson,
            sopDocsJson = existing.sopDocsJson,
            sharedTeamId = existing.sharedTeamId,
            updatedAt = now
        )
        syncStartupIfConnected(existing.copy(crmDealsJson = json, updatedAt = now))
    }

    suspend fun saveBusinessOkrs(startupId: String, okrs: List<BusinessOkrItem>) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val json = BusinessOkrItem.encodeList(okrs)
        val now = System.currentTimeMillis()
        dao.updateStartupBusinessModules(
            id = startupId,
            projectType = existing.projectType,
            crmDealsJson = existing.crmDealsJson,
            okrsJson = json,
            sopDocsJson = existing.sopDocsJson,
            sharedTeamId = existing.sharedTeamId,
            updatedAt = now
        )
        syncStartupIfConnected(existing.copy(okrsJson = json, updatedAt = now))
    }

    suspend fun saveBusinessSopDocs(startupId: String, docs: List<BusinessSopDoc>) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val json = BusinessSopDoc.encodeList(docs)
        val now = System.currentTimeMillis()
        dao.updateStartupBusinessModules(
            id = startupId,
            projectType = existing.projectType,
            crmDealsJson = existing.crmDealsJson,
            okrsJson = existing.okrsJson,
            sopDocsJson = json,
            sharedTeamId = existing.sharedTeamId,
            updatedAt = now
        )
        syncStartupIfConnected(existing.copy(sopDocsJson = json, updatedAt = now))
    }

    suspend fun shareBusinessWorkflowToTeam(
        startupId: String,
        teamId: String,
        actorName: String,
        taggedMembers: List<TeamMemberEntity> = emptyList(),
        includeRoadmapTasks: Boolean = true,
        includeSopPage: Boolean = true,
        customHeadline: String = ""
    ): Result<Int> = withContext(Dispatchers.IO) {
        val project = dao.getStartupProjectById(startupId)
            ?: return@withContext Result.failure(IllegalStateException("Business project not found."))
        val uid = currentAuthUid()
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In required to share with a team."))
        val now = System.currentTimeMillis()
        val phases = dao.getPhaseItemsForStartup(startupId)
        val sops = BusinessSopDoc.parseList(project.sopDocsJson)
        val deals = BusinessCrmDeal.parseList(project.crmDealsJson)
        val okrs = BusinessOkrItem.parseList(project.okrsJson)
        val uidsCsv = taggedMembers.joinToString(",") { it.uid }
        val namesCsv = taggedMembers.joinToString(",") { it.displayName }
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        var pushedCount = 0

        if (includeRoadmapTasks) {
            val openMilestones = phases.filter { !it.isCompleted }.take(6)
            openMilestones.forEachIndexed { idx, milestone ->
                val day = days[idx % days.size]
                val task = TeamSharedTaskEntity(
                    id = "biz_${project.id.take(12)}_${milestone.id.take(12)}",
                    teamId = teamId,
                    title = "[${project.name.take(24)}] ${milestone.title}",
                    description = "${milestone.phaseTitle}\n${milestone.notes}".trim(),
                    dayOfWeek = day,
                    dateLabel = "$day · Business Sprint",
                    startTime = "10:00 AM",
                    endTime = "12:00 PM",
                    assigneeUid = taggedMembers.firstOrNull()?.uid.orEmpty(),
                    assigneeName = namesCsv,
                    assignedToUidsCsv = uidsCsv,
                    assignedToNamesCsv = namesCsv,
                    status = "todo",
                    priority = if (milestone.phaseNumber <= 2) "HIGH" else "MEDIUM",
                    isCompleted = false,
                    createdByUid = uid,
                    sourceBusinessId = project.id,
                    linkedDriveUrl = milestone.resourceUrl,
                    createdAt = now,
                    updatedAt = now
                )
                if (cloudOsSync.pushSharedTask(task).isSuccess) {
                    pushedCount++
                }
            }
        }

        if (includeSopPage) {
            val summaryLines = buildString {
                appendLine("# ${project.name} (${project.projectType} • ${project.stage})")
                if (project.tagline.isNotBlank()) appendLine(project.tagline)
                if (deals.isNotEmpty()) {
                    appendLine()
                    appendLine("## Client & Deal Pipeline (${deals.size})")
                    deals.take(8).forEach { d ->
                        appendLine("- ${d.clientName} [${d.stage}] — ${formatCurrencyCents(d.dealValueCents)}")
                    }
                }
                if (okrs.isNotEmpty()) {
                    appendLine()
                    appendLine("## Active OKRs (${okrs.size})")
                    okrs.take(8).forEach { o ->
                        appendLine("- ${o.objective} (${o.currentValue.toInt()}/${o.targetValue.toInt()} ${o.unitLabel})")
                    }
                }
                if (sops.isNotEmpty()) {
                    appendLine()
                    appendLine("## SOPs & Playbooks")
                    sops.take(4).forEach { s ->
                        appendLine("### ${s.title} (${s.templateType})")
                        appendLine(s.content.take(500))
                    }
                }
            }.trim()

            val blocks = listOf(
                WorkspaceBlock(type = WorkspaceBlock.TYPE_H1, text = "${project.name} Workflow Brief"),
                WorkspaceBlock(type = WorkspaceBlock.TYPE_CALLOUT, text = "Synced from Startup & Business OS (${project.stage}) • ${phases.count { it.isCompleted }}/${phases.size} milestones complete"),
                WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = summaryLines)
            )
            val sharedDoc = TeamSharedDocEntity(
                id = "biz_brief_${project.id.take(24)}",
                teamId = teamId,
                title = "${project.name} — Business Workflow",
                content = summaryLines,
                blocksJson = WorkspaceBlock.encodeList(blocks),
                tagsCsv = "business-os,${project.projectType.lowercase()}",
                icon = "🚀",
                isPinned = true,
                authorUid = uid,
                authorName = actorName,
                lastEditedBy = actorName,
                createdAt = now,
                updatedAt = now
            )
            if (cloudOsSync.pushSharedDoc(sharedDoc).isSuccess) {
                pushedCount++
            }
        }

        val headline = customHeadline.trim().ifBlank { "Business Workflow Synced: ${project.name}" }
        val mentions = if (taggedMembers.isNotEmpty()) {
            taggedMembers.joinToString(" ") { "@${it.displayName}" } + " · "
        } else ""
        cloudOsSync.sendTeamNotification(
            TeamNotificationEntity(
                teamId = teamId,
                senderUid = uid,
                senderName = actorName,
                title = headline,
                message = "${mentions}Shared roadmap milestones, OKRs & playbook from ${project.name}.",
                taggedUidsCsv = uidsCsv,
                taggedNamesCsv = namesCsv,
                noticeType = "notice",
                createdAtMillis = now
            )
        )
        cloudOsSync.logTeamActivity(
            TeamActivityEntity(
                teamId = teamId,
                actorUid = uid,
                actorName = actorName,
                actionType = "BUSINESS_SYNC",
                summary = "Synced \"${project.name}\" ($pushedCount workflow items) to team",
                targetId = project.id,
                createdAtMillis = now
            )
        )

        dao.updateStartupBusinessModules(
            id = project.id,
            projectType = project.projectType,
            crmDealsJson = project.crmDealsJson,
            okrsJson = project.okrsJson,
            sopDocsJson = project.sopDocsJson,
            sharedTeamId = teamId,
            updatedAt = now
        )
        syncStartupIfConnected(project.copy(sharedTeamId = teamId, updatedAt = now))
        Result.success(pushedCount)
    }

    suspend fun toggleStartupPhaseItem(item: StartupPhaseItemEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = item.copy(
            isCompleted = !item.isCompleted,
            updatedAt = now
        )
        dao.upsertStartupPhaseItem(updated)
        val project = dao.getStartupProjectById(item.startupId) ?: return@withContext
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
    }

    suspend fun addCustomStartupPhaseItem(
        startupId: String,
        phaseNumber: Int,
        title: String,
        notes: String = "",
        resourceUrl: String? = null
    ): StartupPhaseItemEntity? = withContext(Dispatchers.IO) {
        val project = dao.getStartupProjectById(startupId) ?: return@withContext null
        val clampedPhase = phaseNumber.coerceIn(1, 6)
        val existing = dao.getPhaseItemsForStartup(startupId)
        val maxSort = existing.filter { it.phaseNumber == clampedPhase }.maxOfOrNull { it.sortOrder } ?: -1
        val now = System.currentTimeMillis()
        val item = StartupPhaseItemEntity(
            startupId = startupId,
            phaseNumber = clampedPhase,
            phaseTitle = STARTUP_PHASE_TITLES[clampedPhase] ?: "Phase $clampedPhase",
            title = title.trim().ifBlank { "Custom Milestone" },
            notes = notes.trim(),
            resourceUrl = resourceUrl?.trim()?.takeIf { it.isNotBlank() },
            isCompleted = false,
            sortOrder = maxSort + 1,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertStartupPhaseItem(item)
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
        item
    }

    suspend fun deleteStartupPhaseItem(item: StartupPhaseItemEntity) = withContext(Dispatchers.IO) {
        dao.deleteStartupPhaseItem(item.id)
        val project = dao.getStartupProjectById(item.startupId) ?: return@withContext
        val now = System.currentTimeMillis()
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
    }

    suspend fun saveSprintTask(
        startupId: String,
        id: String? = null,
        title: String,
        description: String,
        columnStatus: String,
        priority: String,
        tag: String
    ): StartupSprintTaskEntity? = withContext(Dispatchers.IO) {
        val project = dao.getStartupProjectById(startupId) ?: return@withContext null
        val validCol = if (columnStatus in SPRINT_COLUMNS) columnStatus else "BACKLOG"
        val validPriority = if (priority in setOf("HIGH", "MEDIUM", "LOW")) priority else "MEDIUM"
        val now = System.currentTimeMillis()
        val existingTasks = dao.getSprintTasksForStartup(startupId)
        val existing = id?.let { targetId -> existingTasks.find { it.id == targetId } }
        val nextSort = existing?.sortOrder ?: ((existingTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1)

        val task = (existing ?: StartupSprintTaskEntity(startupId = startupId, title = title)).copy(
            title = title.trim().ifBlank { "Sprint Deliverable" },
            description = description.trim(),
            columnStatus = validCol,
            priority = validPriority,
            tag = tag.trim().ifBlank { "Core" },
            sortOrder = nextSort,
            updatedAt = now
        )
        dao.upsertSprintTask(task)
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
        task
    }

    suspend fun moveSprintTaskColumn(task: StartupSprintTaskEntity, newColumn: String) = withContext(Dispatchers.IO) {
        if (newColumn !in SPRINT_COLUMNS) return@withContext
        val now = System.currentTimeMillis()
        val updated = task.copy(columnStatus = newColumn, updatedAt = now)
        dao.upsertSprintTask(updated)
        val project = dao.getStartupProjectById(task.startupId) ?: return@withContext
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
    }

    suspend fun deleteSprintTask(task: StartupSprintTaskEntity) = withContext(Dispatchers.IO) {
        dao.deleteSprintTask(task.id)
        val project = dao.getStartupProjectById(task.startupId) ?: return@withContext
        val now = System.currentTimeMillis()
        dao.touchStartupProject(project.id, now)
        syncStartupIfConnected(project.copy(updatedAt = now))
    }

    suspend fun deleteStartupProject(startupId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getStartupProjectById(startupId) ?: return@withContext
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteStartupProject(startupId, deletedAt)
        if (existing.cloudSync && currentAuthUid() != null && isOnline()) {
            val phases = dao.getPhaseItemsForStartup(startupId)
            val tasks = dao.getSprintTasksForStartup(startupId)
            cloudOsSync.pushStartupProject(existing.copy(deletedAt = deletedAt), phases, tasks)
        }
    }

    suspend fun generateAiStartupBlueprint(startupId: String): Result<String> = withContext(Dispatchers.IO) {
        val project = dao.getStartupProjectById(startupId)
            ?: return@withContext Result.failure(IllegalStateException("Select a venture first."))
        val result = aiLibraryService.generateStartupBlueprint(
            name = project.name,
            tagline = project.tagline,
            targetPersona = project.targetPersona,
            techStack = project.techStack,
            stage = project.stage
        )
        result.onSuccess { markdown ->
            val now = System.currentTimeMillis()
            dao.updateStartupAiPlan(project.id, markdown, now)
            syncStartupIfConnected(project.copy(aiPlanMarkdown = markdown, updatedAt = now))
        }
        result
    }

    private suspend fun syncStartupIfConnected(project: StartupProjectEntity) {
        if (project.cloudSync && currentAuthUid() != null && isOnline()) {
            val phases = dao.getPhaseItemsForStartup(project.id)
            val tasks = dao.getSprintTasksForStartup(project.id)
            cloudOsSync.pushStartupProject(project, phases, tasks)
        }
    }

    // =========================================================================
    // 2. Money Management & Custom Sector Ledger
    // =========================================================================

    suspend fun ensureDefaultFinanceSectors() = withContext(Dispatchers.IO) {
        val existing = dao.getAllFinanceSectors()
        if (existing.isNotEmpty()) return@withContext
        val uid = currentAuthUid()
        val now = System.currentTimeMillis()
        val defaults = DEFAULT_FINANCE_SECTORS.mapIndexed { idx, spec ->
            FinanceSectorEntity(
                id = spec.id,
                userId = uid,
                name = spec.name,
                iconKey = spec.iconKey,
                colorHex = spec.colorHex,
                typeAffinity = spec.typeAffinity,
                isCustom = false,
                createdAt = now + idx,
                updatedAt = now + idx
            )
        }
        dao.upsertFinanceSectors(defaults)
    }

    suspend fun createCustomSector(
        name: String,
        iconKey: String,
        colorHex: String,
        typeAffinity: String
    ): FinanceSectorEntity = withContext(Dispatchers.IO) {
        val validAffinity = if (typeAffinity in setOf("INCOME", "EXPENSE", "BOTH")) typeAffinity else "BOTH"
        val now = System.currentTimeMillis()
        val sector = FinanceSectorEntity(
            userId = currentAuthUid(),
            name = name.trim().take(80).ifBlank { "Custom Sector" },
            iconKey = iconKey.trim().take(48).ifBlank { "wallet" },
            colorHex = colorHex.trim().take(16).ifBlank { "#E53935" },
            typeAffinity = validAffinity,
            isCustom = true,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertFinanceSector(sector)
        if (currentAuthUid() != null && isOnline()) {
            cloudOsSync.pushFinanceSector(sector)
        }
        sector
    }

    suspend fun deleteFinanceSector(sectorId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getFinanceSectorById(sectorId) ?: return@withContext
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteFinanceSector(sectorId, deletedAt)
        if (currentAuthUid() != null && isOnline()) {
            cloudOsSync.pushFinanceSector(existing.copy(deletedAt = deletedAt))
        }
    }

    suspend fun logFinanceTransaction(
        txType: String,
        amountCents: Long,
        sector: FinanceSectorEntity,
        paymentMethod: String,
        notes: String,
        occurredAtMillis: Long
    ): FinanceTransactionEntity = withContext(Dispatchers.IO) {
        val validType = if (txType in setOf("INCOME", "EXPENSE", "TRANSFER")) txType else "EXPENSE"
        val validMethod = if (paymentMethod in setOf("CASH", "BANK", "MOBILE_BANKING", "CARD")) paymentMethod else "BANK"
        val now = System.currentTimeMillis()
        val tx = FinanceTransactionEntity(
            userId = currentAuthUid(),
            txType = validType,
            amountCents = amountCents.coerceAtLeast(0L),
            sectorId = sector.id,
            sectorName = sector.name,
            paymentMethod = validMethod,
            notes = notes.trim().take(2000),
            occurredAtMillis = occurredAtMillis.coerceAtLeast(0L),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertFinanceTransaction(tx)
        if (currentAuthUid() != null && isOnline()) {
            cloudOsSync.pushFinanceTransaction(tx)
        }
        tx
    }

    suspend fun deleteFinanceTransaction(txId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getFinanceTransactionById(txId) ?: return@withContext
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteFinanceTransaction(txId, deletedAt)
        if (currentAuthUid() != null && isOnline()) {
            cloudOsSync.pushFinanceTransaction(existing.copy(deletedAt = deletedAt))
        }
    }

    // =========================================================================
    // 3. Real-Time Team Collaboration Cache & Local Alerts
    // =========================================================================

    suspend fun cacheTeamsSnapshot(teams: List<TeamWorkspaceEntity>) = withContext(Dispatchers.IO) {
        dao.upsertTeams(teams)
    }

    suspend fun cacheTeamMembersSnapshot(teamId: String, members: List<TeamMemberEntity>) = withContext(Dispatchers.IO) {
        dao.clearTeamMembers(teamId)
        dao.upsertTeamMembers(members)
    }

    suspend fun removeCachedTeamMember(teamId: String, uid: String) = withContext(Dispatchers.IO) {
        dao.deleteTeamMember(teamId, uid)
    }

    suspend fun cacheTeamTasksSnapshot(teamId: String, tasks: List<TeamSharedTaskEntity>) = withContext(Dispatchers.IO) {
        dao.clearTeamTasks(teamId)
        dao.upsertTeamTasks(tasks)
    }

    suspend fun cacheTeamDocsSnapshot(teamId: String, docs: List<TeamSharedDocEntity>) = withContext(Dispatchers.IO) {
        dao.clearTeamDocs(teamId)
        dao.upsertTeamDocs(docs)
    }

    suspend fun cacheTeamNotificationsSnapshot(
        notifications: List<TeamNotificationEntity>
    ) = withContext(Dispatchers.IO) {
        dao.upsertTeamNotifications(notifications)
    }

    suspend fun cacheTeamActivitiesSnapshot(
        teamId: String,
        activities: List<TeamActivityEntity>
    ) = withContext(Dispatchers.IO) {
        dao.clearTeamActivities(teamId)
        dao.upsertTeamActivities(activities)
    }

    suspend fun cacheTeamEmailInvitesSnapshot(
        teamId: String,
        invites: List<TeamEmailInviteEntity>
    ) = withContext(Dispatchers.IO) {
        dao.clearTeamEmailInvites(teamId)
        dao.upsertTeamEmailInvites(invites)
    }

    fun maybePostLocalTeamNotification(
        notif: TeamNotificationEntity,
        currentUid: String?
    ): Boolean {
        if (currentUid.isNullOrBlank() || notif.senderUid == currentUid) return false
        if (seenNotifPrefs.getBoolean(notif.id, false)) return false

        val taggedUids = notif.taggedUidsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (taggedUids.isNotEmpty() && currentUid !in taggedUids) {
            seenNotifPrefs.edit().putBoolean(notif.id, true).apply()
            return false
        }

        // Only fire alert banners/notifications for alerts sent recently (within 6 hours)
        val ageMs = abs(System.currentTimeMillis() - notif.createdAtMillis)
        seenNotifPrefs.edit().putBoolean(notif.id, true).apply()
        if (ageMs > 6 * 3_600_000L) return false

        postSystemTeamNotification(notif, isDirectTag = currentUid in taggedUids)
        return true
    }

    private fun postSystemTeamNotification(notif: TeamNotificationEntity, isDirectTag: Boolean = false) {
        val appCtx = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appCtx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val nm = appCtx.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TEAM_ALERTS,
                "Team Collaboration Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications sent by your team members"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appCtx,
            abs(notif.id.hashCode()),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefix = if (isDirectTag) "@You · " else ""
        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID_TEAM_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$prefix${notif.senderName}: ${notif.title}")
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(abs(notif.id.hashCode()), notification)
    }

    // =========================================================================
    // 4. Unified Cloud Sync (Startups + Finance)
    // =========================================================================

    suspend fun syncOsDataNow(): Int = withContext(Dispatchers.IO) {
        if (currentAuthUid() == null || !isOnline()) return@withContext 0
        var count = 0

        dao.getAllStartupProjects().forEach { project ->
            if (!project.cloudSync) return@forEach
            val phases = dao.getPhaseItemsForStartup(project.id)
            val tasks = dao.getSprintTasksForStartup(project.id)
            if (cloudOsSync.pushStartupProject(project, phases, tasks).isSuccess) count++
        }
        dao.getAllFinanceSectors().forEach { sector ->
            if (cloudOsSync.pushFinanceSector(sector).isSuccess) count++
        }
        dao.getAllFinanceTransactions().forEach { tx ->
            if (cloudOsSync.pushFinanceTransaction(tx).isSuccess) count++
        }

        // Pull missing cloud records
        cloudOsSync.fetchStartupProjects().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getStartupProjectById(remote.id) == null) {
                val createdMs = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                val updatedMs = remote.updatedAt?.toDate()?.time ?: createdMs
                dao.upsertStartupProject(
                    StartupProjectEntity(
                        id = remote.id,
                        userId = remote.userId,
                        name = remote.name,
                        tagline = remote.tagline,
                        stage = remote.stage,
                        projectType = remote.projectType,
                        targetPersona = remote.targetPersona,
                        techStack = remote.techStack,
                        aiPlanMarkdown = remote.aiPlanMarkdown,
                        crmDealsJson = remote.crmDealsJson,
                        okrsJson = remote.okrsJson,
                        sopDocsJson = remote.sopDocsJson,
                        sharedTeamId = remote.sharedTeamId,
                        cloudSync = true,
                        createdAt = createdMs,
                        updatedAt = updatedMs
                    )
                )
                val phases = CloudOsSync.decodePhasesJson(remote.id, remote.phasesJson)
                    .map { p -> p.copy(id = p.id.ifBlank { UUID.randomUUID().toString() }) }
                if (phases.isNotEmpty()) dao.upsertStartupPhaseItems(phases)
                val tasks = CloudOsSync.decodeSprintTasksJson(remote.id, remote.sprintTasksJson)
                    .map { t -> t.copy(id = t.id.ifBlank { UUID.randomUUID().toString() }) }
                if (tasks.isNotEmpty()) dao.upsertSprintTasks(tasks)
            }
        }
        cloudOsSync.fetchFinanceSectors().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getFinanceSectorById(remote.id) == null) {
                dao.upsertFinanceSector(
                    FinanceSectorEntity(
                        id = remote.id,
                        userId = remote.userId,
                        name = remote.name,
                        iconKey = remote.iconKey,
                        colorHex = remote.colorHex,
                        typeAffinity = remote.typeAffinity,
                        isCustom = remote.isCustom
                    )
                )
            }
        }
        cloudOsSync.fetchFinanceTransactions().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getFinanceTransactionById(remote.id) == null) {
                dao.upsertFinanceTransaction(
                    FinanceTransactionEntity(
                        id = remote.id,
                        userId = remote.userId,
                        txType = remote.txType,
                        amountCents = remote.amountCents,
                        sectorId = remote.sectorId,
                        sectorName = remote.sectorName,
                        paymentMethod = remote.paymentMethod,
                        notes = remote.notes,
                        occurredAtMillis = remote.occurredAtMillis
                    )
                )
            }
        }

        count
    }

    companion object {
        const val CHANNEL_ID_TEAM_ALERTS = "team_collab_alerts"

        val SPRINT_COLUMNS = listOf("BACKLOG", "IN_DEV", "QA", "LIVE")

        val STARTUP_PHASE_TITLES = mapOf(
            1 to "1. Ideation & Validation",
            2 to "2. MVP Architecture & Tech Stack",
            3 to "3. Legal, Branding & Pricing",
            4 to "4. Build & Sprint Execution",
            5 to "5. Go-To-Market & Launch",
            6 to "6. Growth & Metrics"
        )

        private data class DefaultSectorSpec(
            val id: String,
            val name: String,
            val iconKey: String,
            val colorHex: String,
            val typeAffinity: String
        )

        private val DEFAULT_FINANCE_SECTORS = listOf(
            DefaultSectorSpec("sector_business_rev", "Business Revenue", "briefcase", "#2E7D32", "INCOME"),
            DefaultSectorSpec("sector_software_apis", "Software & APIs", "sparkle", "#C62828", "EXPENSE"),
            DefaultSectorSpec("sector_marketing", "Marketing", "send", "#D84315", "EXPENSE"),
            DefaultSectorSpec("sector_food_living", "Food & Living", "heart", "#EF6C00", "EXPENSE"),
            DefaultSectorSpec("sector_transport", "Transport", "compass", "#1565C0", "EXPENSE"),
            DefaultSectorSpec("sector_investments", "Investments", "star", "#6A1B9A", "BOTH"),
            DefaultSectorSpec("sector_client_payment", "Client Payment", "check", "#00897B", "INCOME")
        )

        fun defaultPhasesForStartup(startupId: String, now: Long = System.currentTimeMillis()): List<StartupPhaseItemEntity> {
            return listOf(
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 1,
                    phaseTitle = STARTUP_PHASE_TITLES[1]!!,
                    title = "Interview 10 target customers about core pain points",
                    notes = "Validate urgency and current workaround spend before writing code.",
                    resourceUrl = "https://www.ycombinator.com/library",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 1,
                    phaseTitle = STARTUP_PHASE_TITLES[1]!!,
                    title = "Define 1-line value proposition & ICP",
                    notes = "Articulate who, painful job-to-be-done, and 10x outcome.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 2,
                    phaseTitle = STARTUP_PHASE_TITLES[2]!!,
                    title = "Finalize database schema & core API contract",
                    notes = "Lock core entities, auth model, and zero-trust permissions.",
                    resourceUrl = "https://firebase.google.com/docs/firestore",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 2,
                    phaseTitle = STARTUP_PHASE_TITLES[2]!!,
                    title = "Cut non-essential features for 14-day MVP",
                    notes = "Ship only the #1 workflow that proves core value.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 3,
                    phaseTitle = STARTUP_PHASE_TITLES[3]!!,
                    title = "Set tier pricing & unit economics",
                    notes = "Anchor pricing to customer ROI and healthy gross margins.",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 3,
                    phaseTitle = STARTUP_PHASE_TITLES[3]!!,
                    title = "Register domain, privacy policy & brand kit",
                    notes = "Prepare landing page copy, logo lockup, and legal terms.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 4,
                    phaseTitle = STARTUP_PHASE_TITLES[4]!!,
                    title = "Build end-to-end onboarding & core happy path",
                    notes = "Track blockers on the Kanban Sprint Board below.",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 4,
                    phaseTitle = STARTUP_PHASE_TITLES[4]!!,
                    title = "Internal dogfooding & QA smoke pass",
                    notes = "Verify payments, offline edge cases, and error states.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 5,
                    phaseTitle = STARTUP_PHASE_TITLES[5]!!,
                    title = "Launch waitlist & private beta with 15 design partners",
                    notes = "Collect qualitative session recordings and friction logs.",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 5,
                    phaseTitle = STARTUP_PHASE_TITLES[5]!!,
                    title = "Publish launch demo video & distribution posts",
                    notes = "Ship across Product Hunt, X, LinkedIn, and niche communities.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 6,
                    phaseTitle = STARTUP_PHASE_TITLES[6]!!,
                    title = "Instrument activation, Day-7 retention & MRR telemetry",
                    notes = "Identify highest-leverage funnel drop-off to fix weekly.",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupPhaseItemEntity(
                    startupId = startupId,
                    phaseNumber = 6,
                    phaseTitle = STARTUP_PHASE_TITLES[6]!!,
                    title = "Automate referral loop & customer success cadence",
                    notes = "Double down on top acquisition channel.",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        private fun defaultSprintTasksForStartup(
            startupId: String,
            now: Long = System.currentTimeMillis()
        ): List<StartupSprintTaskEntity> {
            return listOf(
                StartupSprintTaskEntity(
                    startupId = startupId,
                    title = "Customer Discovery Script",
                    description = "Draft 6 non-leading validation questions.",
                    columnStatus = "BACKLOG",
                    priority = "HIGH",
                    tag = "Research",
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                StartupSprintTaskEntity(
                    startupId = startupId,
                    title = "Interactive Landing Wireframe",
                    description = "Above-the-fold hook + waitlist CTA.",
                    columnStatus = "IN_DEV",
                    priority = "MEDIUM",
                    tag = "Design",
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        fun filterTransactions(
            transactions: List<FinanceTransactionEntity>,
            filter: LedgerTimeFilter,
            selectedSectorId: String?,
            customStartMillis: Long?,
            customEndMillis: Long?,
            nowMillis: Long = System.currentTimeMillis()
        ): List<FinanceTransactionEntity> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            val weekCal = (cal.clone() as Calendar).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (timeInMillis > nowMillis) add(Calendar.WEEK_OF_YEAR, -1)
            }
            val startOfWeek = weekCal.timeInMillis

            val monthCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startOfMonth = monthCal.timeInMillis

            return transactions.filter { tx ->
                if (selectedSectorId != null && tx.sectorId != selectedSectorId) return@filter false
                when (filter) {
                    LedgerTimeFilter.TODAY -> tx.occurredAtMillis >= startOfToday
                    LedgerTimeFilter.THIS_WEEK -> tx.occurredAtMillis >= startOfWeek
                    LedgerTimeFilter.THIS_MONTH -> tx.occurredAtMillis >= startOfMonth
                    LedgerTimeFilter.ALL_TIME -> true
                    LedgerTimeFilter.CUSTOM -> {
                        val afterStart = customStartMillis == null || tx.occurredAtMillis >= customStartMillis
                        val beforeEnd = customEndMillis == null || tx.occurredAtMillis <= customEndMillis
                        afterStart && beforeEnd
                    }
                }
            }
        }

        fun computeLedgerSummary(
            transactions: List<FinanceTransactionEntity>,
            sectors: List<FinanceSectorEntity>
        ): FinanceLedgerSummary {
            val sectorById = sectors.associateBy { it.id }
            var income = 0L
            var expense = 0L
            var transfer = 0L
            val expenseBySector = linkedMapOf<String, Pair<String, Long>>()
            val incomeBySector = linkedMapOf<String, Pair<String, Long>>()

            transactions.forEach { tx ->
                when (tx.txType) {
                    "INCOME" -> {
                        income += tx.amountCents
                        val label = sectorById[tx.sectorId]?.name ?: tx.sectorName
                        val prev = incomeBySector[tx.sectorId]?.second ?: 0L
                        incomeBySector[tx.sectorId] = label to (prev + tx.amountCents)
                    }
                    "EXPENSE" -> {
                        expense += tx.amountCents
                        val label = sectorById[tx.sectorId]?.name ?: tx.sectorName
                        val prev = expenseBySector[tx.sectorId]?.second ?: 0L
                        expenseBySector[tx.sectorId] = label to (prev + tx.amountCents)
                    }
                    "TRANSFER" -> transfer += tx.amountCents
                }
            }

            val net = income - expense
            val savingsPct = if (income > 0L) {
                (((income - expense).toDouble() / income.toDouble()) * 100.0).toInt().coerceIn(-999, 100)
            } else 0

            val expItems = expenseBySector.entries
                .sortedByDescending { it.value.second }
                .map { (id, pair) ->
                    SectorBreakdownItem(
                        sectorId = id,
                        sectorName = pair.first,
                        colorHex = sectorById[id]?.colorHex ?: "#E53935",
                        totalCents = pair.second,
                        fraction = if (expense > 0L) (pair.second.toFloat() / expense.toFloat()).coerceIn(0f, 1f) else 0f,
                        txType = "EXPENSE"
                    )
                }

            val incItems = incomeBySector.entries
                .sortedByDescending { it.value.second }
                .map { (id, pair) ->
                    SectorBreakdownItem(
                        sectorId = id,
                        sectorName = pair.first,
                        colorHex = sectorById[id]?.colorHex ?: "#2E7D32",
                        totalCents = pair.second,
                        fraction = if (income > 0L) (pair.second.toFloat() / income.toFloat()).coerceIn(0f, 1f) else 0f,
                        txType = "INCOME"
                    )
                }

            return FinanceLedgerSummary(
                totalIncomeCents = income,
                totalExpenseCents = expense,
                totalTransferCents = transfer,
                netBalanceCents = net,
                savingsRatePercent = savingsPct,
                expenseBreakdown = expItems,
                incomeBreakdown = incItems
            )
        }

        fun formatCurrencyCents(cents: Long, includeSign: Boolean = false): String {
            val absCents = abs(cents)
            val whole = absCents / 100L
            val frac = absCents % 100L
            val formattedWhole = String.format("%,d", whole)
            val prefix = when {
                cents < 0 -> "-$"
                includeSign && cents > 0 -> "+$"
                else -> "$"
            }
            return "$prefix$formattedWhole.${String.format("%02d", frac)}"
        }
    }
}
