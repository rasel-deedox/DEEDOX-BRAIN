package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // --- Workspace Notes ---
    @Query("SELECT * FROM workspace_notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeWorkspaceNotes(): Flow<List<WorkspaceNoteEntity>>

    @Query("SELECT * FROM workspace_notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllWorkspaceNotes(): List<WorkspaceNoteEntity>

    @Query("SELECT * FROM workspace_notes WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceNoteById(id: String): WorkspaceNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspaceNote(note: WorkspaceNoteEntity)

    @Query("UPDATE workspace_notes SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteWorkspaceNote(id: String, deletedAt: Long = System.currentTimeMillis())

    // --- Workspace Tasks ---
    @Query("SELECT * FROM workspace_tasks WHERE deletedAt IS NULL ORDER BY isCompleted ASC, dueDateEpochDay ASC, updatedAt DESC")
    fun observeWorkspaceTasks(): Flow<List<WorkspaceTaskEntity>>

    @Query("SELECT * FROM workspace_tasks WHERE deletedAt IS NULL ORDER BY dueDateEpochDay ASC, updatedAt DESC")
    suspend fun getAllWorkspaceTasks(): List<WorkspaceTaskEntity>

    @Query("SELECT * FROM workspace_tasks WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceTaskById(id: String): WorkspaceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspaceTask(task: WorkspaceTaskEntity)

    @Query("UPDATE workspace_tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteWorkspaceTask(id: String, deletedAt: Long = System.currentTimeMillis())

    // --- Smart Reminders ---
    @Query("SELECT * FROM smart_reminders WHERE deletedAt IS NULL ORDER BY isEnabled DESC, triggerAtMillis ASC")
    fun observeReminders(): Flow<List<SmartReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE deletedAt IS NULL AND isEnabled = 1 ORDER BY triggerAtMillis ASC")
    suspend fun getEnabledReminders(): List<SmartReminderEntity>

    @Query("SELECT * FROM smart_reminders WHERE deletedAt IS NULL ORDER BY triggerAtMillis ASC")
    suspend fun getAllReminders(): List<SmartReminderEntity>

    @Query("SELECT * FROM smart_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): SmartReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: SmartReminderEntity)

    @Query("UPDATE smart_reminders SET deletedAt = :deletedAt, isEnabled = 0, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteReminder(id: String, deletedAt: Long = System.currentTimeMillis())

    // --- Habits & Completions ---
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("UPDATE habits SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteHabit(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habit_completions ORDER BY epochDay DESC")
    fun observeHabitCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions ORDER BY epochDay DESC")
    suspend fun getAllHabitCompletions(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getHabitCompletion(habitId: String, epochDay: Long): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteHabitCompletion(habitId: String, epochDay: Long)

    // --- Blocker Rules & Focus Sessions ---
    @Query("SELECT * FROM blocker_rules ORDER BY createdAt DESC")
    fun observeBlockerRules(): Flow<List<BlockerRuleEntity>>

    @Query("SELECT * FROM blocker_rules ORDER BY createdAt DESC")
    suspend fun getAllBlockerRules(): List<BlockerRuleEntity>

    @Query("SELECT * FROM blocker_rules WHERE id = :id LIMIT 1")
    suspend fun getBlockerRuleById(id: String): BlockerRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockerRule(rule: BlockerRuleEntity)

    @Query("DELETE FROM blocker_rules WHERE id = :id")
    suspend fun deleteBlockerRule(id: String)

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun getAllFocusSessions(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveFocusSession(): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSession(session: FocusSessionEntity)
}
