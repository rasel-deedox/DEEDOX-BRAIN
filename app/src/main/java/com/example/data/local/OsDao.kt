package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OsDao {

    // --- Startup & Business OS ---
    @Query("SELECT * FROM startup_projects WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeStartupProjects(): Flow<List<StartupProjectEntity>>

    @Query("SELECT * FROM startup_projects WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getAllStartupProjects(): List<StartupProjectEntity>

    @Query("SELECT * FROM startup_projects WHERE id = :id LIMIT 1")
    suspend fun getStartupProjectById(id: String): StartupProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStartupProject(project: StartupProjectEntity)

    @Query("UPDATE startup_projects SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchStartupProject(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE startup_projects SET stage = :stage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStartupStage(id: String, stage: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE startup_projects SET aiPlanMarkdown = :markdown, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStartupAiPlan(id: String, markdown: String, updatedAt: Long = System.currentTimeMillis())

    @Query(
        "UPDATE startup_projects SET projectType = :projectType, crmDealsJson = :crmDealsJson, " +
            "okrsJson = :okrsJson, sopDocsJson = :sopDocsJson, sharedTeamId = :sharedTeamId, " +
            "updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateStartupBusinessModules(
        id: String,
        projectType: String,
        crmDealsJson: String,
        okrsJson: String,
        sopDocsJson: String,
        sharedTeamId: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE startup_projects SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteStartupProject(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM startup_phase_items ORDER BY phaseNumber ASC, sortOrder ASC")
    fun observeAllStartupPhaseItems(): Flow<List<StartupPhaseItemEntity>>

    @Query("SELECT * FROM startup_phase_items WHERE startupId = :startupId ORDER BY phaseNumber ASC, sortOrder ASC")
    suspend fun getPhaseItemsForStartup(startupId: String): List<StartupPhaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStartupPhaseItems(items: List<StartupPhaseItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStartupPhaseItem(item: StartupPhaseItemEntity)

    @Query("DELETE FROM startup_phase_items WHERE id = :id")
    suspend fun deleteStartupPhaseItem(id: String)

    @Query("DELETE FROM startup_phase_items WHERE startupId = :startupId")
    suspend fun deletePhaseItemsForStartup(startupId: String)

    @Query("SELECT * FROM startup_sprint_tasks ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAllSprintTasks(): Flow<List<StartupSprintTaskEntity>>

    @Query("SELECT * FROM startup_sprint_tasks WHERE startupId = :startupId ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun getSprintTasksForStartup(startupId: String): List<StartupSprintTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSprintTasks(tasks: List<StartupSprintTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSprintTask(task: StartupSprintTaskEntity)

    @Query("DELETE FROM startup_sprint_tasks WHERE id = :id")
    suspend fun deleteSprintTask(id: String)

    @Query("DELETE FROM startup_sprint_tasks WHERE startupId = :startupId")
    suspend fun deleteSprintTasksForStartup(startupId: String)

    // --- Money & Custom Sector Ledger ---
    @Query("SELECT * FROM finance_sectors WHERE deletedAt IS NULL ORDER BY isCustom ASC, createdAt ASC")
    fun observeFinanceSectors(): Flow<List<FinanceSectorEntity>>

    @Query("SELECT * FROM finance_sectors WHERE deletedAt IS NULL")
    suspend fun getAllFinanceSectors(): List<FinanceSectorEntity>

    @Query("SELECT * FROM finance_sectors WHERE id = :id LIMIT 1")
    suspend fun getFinanceSectorById(id: String): FinanceSectorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinanceSectors(sectors: List<FinanceSectorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinanceSector(sector: FinanceSectorEntity)

    @Query("UPDATE finance_sectors SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteFinanceSector(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM finance_transactions WHERE deletedAt IS NULL ORDER BY occurredAtMillis DESC")
    fun observeFinanceTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE deletedAt IS NULL ORDER BY occurredAtMillis DESC")
    suspend fun getAllFinanceTransactions(): List<FinanceTransactionEntity>

    @Query("SELECT * FROM finance_transactions WHERE id = :id LIMIT 1")
    suspend fun getFinanceTransactionById(id: String): FinanceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinanceTransaction(tx: FinanceTransactionEntity)

    @Query("UPDATE finance_transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteFinanceTransaction(id: String, deletedAt: Long = System.currentTimeMillis())

    // --- Real-Time Team Collaboration Local Cache ---
    @Query("SELECT * FROM team_workspaces ORDER BY updatedAt DESC")
    fun observeTeams(): Flow<List<TeamWorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeams(teams: List<TeamWorkspaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeam(team: TeamWorkspaceEntity)

    @Query("DELETE FROM team_workspaces WHERE id = :teamId")
    suspend fun deleteTeam(teamId: String)

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY role ASC, displayName ASC")
    fun observeTeamMembers(teamId: String): Flow<List<TeamMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamMembers(members: List<TeamMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE teamId = :teamId")
    suspend fun clearTeamMembers(teamId: String)

    @Query("DELETE FROM team_members WHERE teamId = :teamId AND uid = :uid")
    suspend fun deleteTeamMember(teamId: String, uid: String)

    @Query("SELECT * FROM team_shared_tasks WHERE teamId = :teamId ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeTeamTasks(teamId: String): Flow<List<TeamSharedTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamTasks(tasks: List<TeamSharedTaskEntity>)

    @Query("DELETE FROM team_shared_tasks WHERE teamId = :teamId")
    suspend fun clearTeamTasks(teamId: String)

    @Query("SELECT * FROM team_shared_docs WHERE teamId = :teamId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeTeamDocs(teamId: String): Flow<List<TeamSharedDocEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamDocs(docs: List<TeamSharedDocEntity>)

    @Query("DELETE FROM team_shared_docs WHERE teamId = :teamId")
    suspend fun clearTeamDocs(teamId: String)

    @Query("SELECT * FROM team_notifications WHERE teamId = :teamId ORDER BY createdAtMillis DESC")
    fun observeTeamNotifications(teamId: String): Flow<List<TeamNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamNotifications(notifications: List<TeamNotificationEntity>)

    @Query("SELECT * FROM team_activities WHERE teamId = :teamId ORDER BY createdAtMillis DESC LIMIT 60")
    fun observeTeamActivities(teamId: String): Flow<List<TeamActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamActivities(activities: List<TeamActivityEntity>)

    @Query("DELETE FROM team_activities WHERE teamId = :teamId")
    suspend fun clearTeamActivities(teamId: String)

    @Query("SELECT * FROM team_email_invites WHERE teamId = :teamId ORDER BY createdAtMillis DESC")
    fun observeTeamEmailInvites(teamId: String): Flow<List<TeamEmailInviteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamEmailInvites(invites: List<TeamEmailInviteEntity>)

    @Query("DELETE FROM team_email_invites WHERE teamId = :teamId")
    suspend fun clearTeamEmailInvites(teamId: String)
}
