package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.HabitEntity
import com.example.data.local.SmartReminderEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.local.WorkspaceTaskEntity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class WorkspaceCloudSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun pushAndFetchWorkspaceEntities_cloudSyncEnabled_succeeds() = runBlocking {
        val uid = signInTestUser("workspace_alice@test.com")
        val sync = CloudWorkspaceSync(firestore)

        val noteId = "wn_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val note = WorkspaceNoteEntity(
            id = noteId,
            userId = uid,
            title = "Distributed Systems Study Plan",
            blocksJson = """[{"id":"b1","type":"H1","text":"Consensus"},{"id":"b2","type":"CHECKLIST","text":"Read Raft","checked":true}]""",
            tagsCsv = "systems,study",
            isPinned = true,
            cloudSync = true,
            audioDurationMs = 12_500L
        )
        val notePush = withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushWorkspaceNote(note) }
        assertTrue("Expected pushWorkspaceNote success: ${notePush.exceptionOrNull()}", notePush.isSuccess)

        val fetchedNotes = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchWorkspaceNotes().getOrThrow() }
        val loadedNote = fetchedNotes.find { it.id == noteId }
        assertEquals("Distributed Systems Study Plan", loadedNote?.title)
        assertTrue(loadedNote?.isPinned == true)

        val taskId = "wt_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val task = WorkspaceTaskEntity(
            id = taskId,
            userId = uid,
            title = "Complete Monk Mode Session",
            notes = "No social media until 6 PM",
            dueDateEpochDay = 20200L,
            priority = "HIGH",
            isCompleted = false,
            cloudSync = true
        )
        val taskPush = withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushWorkspaceTask(task) }
        assertTrue("Expected pushWorkspaceTask success: ${taskPush.exceptionOrNull()}", taskPush.isSuccess)
        val fetchedTasks = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchWorkspaceTasks().getOrThrow() }
        assertTrue(fetchedTasks.any { it.id == taskId && it.priority == "HIGH" })

        val libReminderId = "rem_lib_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val libReminder = SmartReminderEntity(
            id = libReminderId,
            userId = uid,
            mode = "LIBRARY_CONTENT",
            title = "Revisit Lecture Video",
            priority = "HIGH",
            targetItemId = "item_123",
            triggerAtMillis = System.currentTimeMillis() + 3600_000L,
            recurrenceType = "DAILY",
            intervalMinutes = 0,
            ringtoneTitle = "Chime",
            cloudSync = true
        )
        val personalReminderId = "rem_per_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val personalReminder = SmartReminderEntity(
            id = personalReminderId,
            userId = uid,
            mode = "PERSONAL",
            title = "Evening Stretch",
            priority = "MEDIUM",
            triggerAtMillis = System.currentTimeMillis() + 7200_000L,
            recurrenceType = "ONCE",
            intervalMinutes = 0,
            ringtoneTitle = "Default Alert",
            cloudSync = true
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushReminder(libReminder) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushReminder(personalReminder) }.isSuccess)
        val fetchedReminders = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchReminders().getOrThrow() }
        assertEquals("LIBRARY_CONTENT", fetchedReminders.find { it.id == libReminderId }?.mode)
        assertEquals("PERSONAL", fetchedReminders.find { it.id == personalReminderId }?.mode)

        val habitId = "hab_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val habit = HabitEntity(
            id = habitId,
            userId = uid,
            title = "Deep Reading",
            subtitle = "30 min nightly",
            category = "Study",
            targetPerWeek = 6,
            cloudSync = true
        )
        val habitPush = withTimeout(DEFAULT_TIMEOUT_MS) {
            sync.pushHabit(habit, listOf(20198L, 20199L, 20200L))
        }
        assertTrue("Expected pushHabit success: ${habitPush.exceptionOrNull()}", habitPush.isSuccess)
        val fetchedHabits = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchHabits().getOrThrow() }
        val loadedHabit = fetchedHabits.find { it.id == habitId }
        assertEquals("Deep Reading", loadedHabit?.title)
        assertEquals(listOf(20200L, 20199L, 20198L), loadedHabit?.completedEpochDays)
    }

    @Test
    fun cloudSyncFalse_skipsFirestoreWriteCompletely() = runBlocking {
        val uid = signInTestUser("local_only_alice@test.com")
        val sync = CloudWorkspaceSync(firestore)
        val localNoteId = "local_wn_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val localNote = WorkspaceNoteEntity(
            id = localNoteId,
            userId = uid,
            title = "Private On-Device Journal",
            blocksJson = """[{"id":"1","type":"TEXT","text":"Stay local only"}]""",
            cloudSync = false
        )
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushWorkspaceNote(localNote) }
        assertTrue(result.isSuccess)

        val snap = withTimeout(DEFAULT_TIMEOUT_MS) {
            Tasks.await(
                firestore.collection("users").document(uid)
                    .collection("workspace_notes").document(localNoteId).get()
            )
        }
        assertFalse("Local-only note must NOT be pushed to Firestore", snap.exists())
    }

    @Test
    fun crossUserAndUnauthenticatedWorkspaceReads_failWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("ws_alice_sec@test.com")
        val aliceSync = CloudWorkspaceSync(firestore)
        val noteId = "sec_wn_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSync.pushWorkspaceNote(
                WorkspaceNoteEntity(
                    id = noteId,
                    userId = aliceUid,
                    title = "Alice Confidential",
                    cloudSync = true
                )
            ).getOrThrow()
        }

        // Cross-user Bob
        signInTestUser("ws_bob_sec@test.com")
        var bobDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                Tasks.await(
                    firestore.collection("users").document(aliceUid)
                        .collection("workspace_notes").document(noteId).get()
                )
            }
        } catch (e: Exception) {
            if (e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                bobDenied = true
            }
        }
        assertTrue("Bob must receive PERMISSION_DENIED reading Alice's workspace_notes", bobDenied)

        // Unauthenticated
        auth.signOut()
        val unauthNotes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.fetchWorkspaceNotes() }
        assertTrue("Unauthenticated fetchWorkspaceNotes must fail", unauthNotes.isFailure)

        var unauthDirectDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                Tasks.await(
                    firestore.collection("users").document(aliceUid)
                        .collection("habits").get()
                )
            }
        } catch (e: Exception) {
            if (e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                unauthDirectDenied = true
            }
        }
        assertTrue("Unauthenticated direct read on habits must return PERMISSION_DENIED", unauthDirectDenied)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
