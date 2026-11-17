package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.local.StartupPhaseItemEntity
import com.example.data.local.StartupProjectEntity
import com.example.data.local.StartupSprintTaskEntity
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.repository.HabitGamificationSummary
import com.example.data.repository.PollVotesCodec
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OsCloudSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun pushGamificationStats_authenticatedOwnerSucceeds_crossUserDenied() = runBlocking {
        val aliceUid = signInTestUser("gamify_alice@test.com")
        val sync = CloudOsSync(firestore)

        val summary = HabitGamificationSummary(
            totalXp = 325,
            level = 2,
            xpIntoLevel = 75,
            xpPerLevel = 250,
            levelProgress = 0.3f,
            rankTitle = "Focus Apprentice",
            bestStreak = 6,
            activeStreaksCount = 2,
            totalCompletions = 9,
            streakBonusXp = 100
        )

        val pushRes = withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushGamificationStats(summary) }
        assertTrue("Expected pushGamificationStats success: ${pushRes.exceptionOrNull()}", pushRes.isSuccess)

        signInTestUser("gamify_bob@test.com")
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("users").document(aliceUid)
                        .collection("gamification").document("stats").get()
                )
            }
        } catch (e: Exception) {
            if (e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Bob must receive PERMISSION_DENIED reading Alice gamification stats", denied)
    }

    @Test
    fun startupAndFinanceSync_ownerPushAndFetchSucceed_crossUserDenied() = runBlocking {
        val aliceUid = signInTestUser("os_alice@test.com")
        val sync = CloudOsSync(firestore)

        val projectId = "startup_${UUID.randomUUID().toString().take(8)}"
        val project = StartupProjectEntity(
            id = projectId,
            userId = aliceUid,
            name = "NeuralVault AI",
            tagline = "Personal knowledge OS",
            stage = "MVP",
            projectType = "AGENCY",
            targetPersona = "Solo founders",
            techStack = "Kotlin, Compose, Firestore",
            crmDealsJson = """[{"id":"d1","clientName":"Acme","dealValueCents":150000,"stage":"ACTIVE_CLIENT"}]""",
            okrsJson = """[{"id":"o1","objective":"MRR","keyResult":"$10k","currentValue":4500.0,"targetValue":10000.0,"unitLabel":"USD"}]""",
            sopDocsJson = """[{"id":"s1","title":"Client Kickoff","templateType":"SOP","content":"Kickoff checklist"}]""",
            sharedTeamId = "team_shared_1"
        )
        val phaseItem = StartupPhaseItemEntity(
            startupId = projectId,
            phaseNumber = 1,
            phaseTitle = "1. Ideation & Validation",
            title = "Validate ICP",
            isCompleted = true
        )
        val sprintTask = StartupSprintTaskEntity(
            startupId = projectId,
            title = "Ship Onboarding",
            columnStatus = "IN_DEV",
            priority = "HIGH"
        )

        val startupPush = withTimeout(DEFAULT_TIMEOUT_MS) {
            sync.pushStartupProject(project, listOf(phaseItem), listOf(sprintTask))
        }
        assertTrue("Expected startup push success: ${startupPush.exceptionOrNull()}", startupPush.isSuccess)

        val fetchedStartups = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchStartupProjects().getOrThrow() }
        val fetchedStartup = fetchedStartups.firstOrNull { it.id == projectId }
        assertNotNull(fetchedStartup)
        assertEquals("NeuralVault AI", fetchedStartup?.name)
        assertEquals("AGENCY", fetchedStartup?.projectType)
        assertEquals("team_shared_1", fetchedStartup?.sharedTeamId)
        assertTrue(fetchedStartup?.crmDealsJson?.contains("Acme") == true)
        assertTrue(fetchedStartup?.okrsJson?.contains("MRR") == true)
        assertTrue(fetchedStartup?.sopDocsJson?.contains("Client Kickoff") == true)

        val sector = FinanceSectorEntity(
            id = "sec_${UUID.randomUUID().toString().take(8)}",
            userId = aliceUid,
            name = "Cloud Infra",
            colorHex = "#E53935",
            typeAffinity = "EXPENSE",
            isCustom = true
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushFinanceSector(sector) }.isSuccess)

        val tx = FinanceTransactionEntity(
            id = "tx_${UUID.randomUUID().toString().take(8)}",
            userId = aliceUid,
            txType = "EXPENSE",
            amountCents = 4299L,
            sectorId = sector.id,
            sectorName = sector.name,
            paymentMethod = "CARD",
            notes = "Firebase Blaze"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { sync.pushFinanceTransaction(tx) }.isSuccess)

        val fetchedTxs = withTimeout(DEFAULT_TIMEOUT_MS) { sync.fetchFinanceTransactions().getOrThrow() }
        assertTrue(fetchedTxs.any { it.id == tx.id && it.amountCents == 4299L })

        signInTestUser("os_bob@test.com")
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("users").document(aliceUid)
                        .collection("finance_transactions").document(tx.id).get()
                )
            }
        } catch (e: Exception) {
            if (e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Bob must receive PERMISSION_DENIED reading Alice ledger", denied)
    }

    @Test
    fun teamCollabWorkflow_createInviteJoinExpandDocsTasksAndAlerts_succeeds() = runBlocking {
        val aliceUid = signInTestUser("team_alice@test.com")
        val aliceSync = CloudOsSync(firestore)
        val teamId = "team_${UUID.randomUUID().toString().take(8)}"
        val inviteCode = "AI${(1000..9999).random()}"

        val createdTeam = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSync.createTeam(
                teamId = teamId,
                name = "Founders Lab",
                description = "Shipping v4",
                inviteCode = inviteCode,
                maxMembers = 5,
                ownerName = "Alice",
                ownerEmail = "team_alice@test.com"
            ).getOrThrow()
        }
        assertEquals(5, createdTeam.maxMembers)
        assertEquals(aliceUid, createdTeam.ownerId)

        // Bob looks up invite code and joins the team
        val bobUid = signInTestUser("team_bob@test.com")
        val bobSync = CloudOsSync(firestore)

        val preview = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobSync.lookupTeamByInviteCode(inviteCode).getOrThrow()
        }
        assertEquals(teamId, preview.teamId)
        assertEquals("Founders Lab", preview.teamName)

        val joinedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobSync.joinTeamByInviteCode(inviteCode, "Bob", "team_bob@test.com").getOrThrow()
        }
        assertEquals(teamId, joinedId)

        // Bob updates his roleTag & customStatus and posts a shared note, Sat-Fri schedule task, and tagged notice
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobSync.updateTeamMemberProfile(
                    teamId = teamId,
                    targetUid = bobUid,
                    roleTag = "Lead Developer",
                    customStatus = "Working on API"
                )
            }.isSuccess
        )

        val doc = TeamSharedDocEntity(
            id = "doc_${UUID.randomUUID().toString().take(8)}",
            teamId = teamId,
            title = "Q4 Distribution Plan",
            content = "Focus on Play Store organic + creator demos.",
            blocksJson = """[{"id":"b1","type":"CHECKLIST","text":"Ship v5 release","checked":true}]""",
            tagsCsv = "launch,growth",
            authorUid = bobUid,
            authorName = "Bob",
            lastEditedBy = "Bob"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobSync.pushSharedDoc(doc) }.isSuccess)

        val sharedTask = TeamSharedTaskEntity(
            id = "task_${UUID.randomUUID().toString().take(8)}",
            teamId = teamId,
            title = "Finalize Security Rules",
            description = "Verify emulator suite",
            dayOfWeek = "Wed",
            dateLabel = "Wed · 02:00 PM - 04:00 PM",
            startTime = "02:00 PM",
            endTime = "04:00 PM",
            assigneeUid = aliceUid,
            assigneeName = "Alice, Bob",
            assignedToUidsCsv = "$aliceUid,$bobUid",
            assignedToNamesCsv = "Alice,Bob",
            status = "in_progress",
            priority = "HIGH",
            createdByUid = bobUid
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobSync.pushSharedTask(sharedTask) }.isSuccess)

        // Bob sends real-time team alert tagging Alice
        val alert = TeamNotificationEntity(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            teamId = teamId,
            senderUid = bobUid,
            senderName = "Bob",
            title = "Build Ready",
            message = "@Alice please check the Wednesday sprint schedule!",
            taggedUidsCsv = aliceUid,
            taggedNamesCsv = "Alice"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobSync.sendTeamNotification(alert) }.isSuccess)

        // Alice expands capacity & receives real-time team updates
        signInTestUser("team_alice@test.com")
        val expandRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSync.expandTeamMaxMembers(teamId, inviteCode, 10)
        }
        assertTrue("Expected owner expand success: ${expandRes.exceptionOrNull()}", expandRes.isSuccess)

        val observedMembers = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamMembers(teamId).first { list ->
                list.any { it.uid == bobUid && it.roleTag == "Lead Developer" }
            }
        }
        assertEquals("Working on API", observedMembers.first { it.uid == bobUid }.customStatus)

        val observedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamTasks(teamId).first { list -> list.any { it.id == sharedTask.id } }
        }
        val observedTask = observedTasks.first { it.id == sharedTask.id }
        assertEquals("Wed", observedTask.dayOfWeek)
        assertEquals("in_progress", observedTask.status)
        assertTrue(observedTask.assignedToUidsCsv.contains(aliceUid))
        assertTrue(observedTask.assignedToUidsCsv.contains(bobUid))

        val observedNotifs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamNotifications(teamId).first { list -> list.any { it.id == alert.id } }
        }
        val observedAlert = observedNotifs.firstOrNull { it.title == "Build Ready" }
        assertNotNull(observedAlert)
        assertEquals(aliceUid, observedAlert?.taggedUidsCsv)

        val observedDocs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamDocs(teamId).first { list -> list.any { it.id == doc.id } }
        }
        assertEquals("Q4 Distribution Plan", observedDocs.first().title)
        assertEquals("launch,growth", observedDocs.first().tagsCsv)
    }

    @Test
    fun enterpriseTeamCollab_emailInvitesRoleAdminPollsActivityNestedPagesAndMemberRemoval() = runBlocking {
        val aliceUid = signInTestUser("ent_alice@test.com")
        val aliceSync = CloudOsSync(firestore)
        val teamId = "team_${UUID.randomUUID().toString().take(8)}"
        val inviteCode = "ENT${(1000..9999).random()}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSync.createTeam(
                teamId = teamId,
                name = "Enterprise Squad",
                description = "Notion + Business OS",
                inviteCode = inviteCode,
                maxMembers = 8,
                ownerName = "Alice",
                ownerEmail = "ent_alice@test.com"
            ).getOrThrow()
        }

        // Alice invites Bob directly by email with ADMIN roleTag
        val invite = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSync.inviteTeamMemberByEmail(
                teamId = teamId,
                teamName = "Enterprise Squad",
                inviteCode = inviteCode,
                email = "ent_bob@test.com",
                invitedRole = "MEMBER",
                roleTag = "Product Engineer",
                invitedByName = "Alice"
            ).getOrThrow()
        }

        // Bob accepts email invite
        val bobUid = signInTestUser("ent_bob@test.com")
        val bobSync = CloudOsSync(firestore)
        val incomingForBob = withTimeout(FLOW_TIMEOUT_MS) {
            bobSync.observeIncomingEmailInvites("ent_bob@test.com").first { it.any { inv -> inv.id == invite.id } }
        }
        assertEquals(1, incomingForBob.count { it.id == invite.id })
        val joinedTeamId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobSync.acceptEmailInvite(invite, "Bob", "ent_bob@test.com").getOrThrow()
        }
        assertEquals(teamId, joinedTeamId)

        // Alice promotes Bob to ADMIN
        signInTestUser("ent_alice@test.com")
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceSync.updateTeamMemberProfile(
                    teamId = teamId,
                    targetUid = bobUid,
                    roleTag = "Staff Engineer",
                    customStatus = "Reviewing PRs",
                    role = "ADMIN"
                )
            }.isSuccess
        )

        // Alice posts a nested Notion page, 5-stage Kanban task, interactive poll, and activity log
        val parentPage = TeamSharedDocEntity(
            id = "page_root_${UUID.randomUUID().toString().take(6)}",
            teamId = teamId,
            title = "Engineering Handbook",
            content = "Core practices",
            icon = "📘",
            isPinned = true,
            authorUid = aliceUid,
            authorName = "Alice",
            lastEditedBy = "Alice"
        )
        val childPage = TeamSharedDocEntity(
            id = "page_sub_${UUID.randomUUID().toString().take(6)}",
            teamId = teamId,
            title = "Deployment Runbook",
            content = "Step-by-step release",
            parentId = parentPage.id,
            icon = "🚀",
            isPinned = false,
            authorUid = aliceUid,
            authorName = "Alice",
            lastEditedBy = "Alice"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.pushSharedDoc(parentPage) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.pushSharedDoc(childPage) }.isSuccess)

        val reviewTask = TeamSharedTaskEntity(
            id = "task_rev_${UUID.randomUUID().toString().take(6)}",
            teamId = teamId,
            title = "Client Proposal Review",
            dayOfWeek = "Thu",
            status = "review",
            priority = "HIGH",
            assignedToUidsCsv = bobUid,
            assignedToNamesCsv = "Bob",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            linkedDriveUrl = "https://drive.google.com/file/d/proposal",
            sourceBusinessId = "biz_123",
            createdByUid = aliceUid
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.pushSharedTask(reviewTask) }.isSuccess)

        val poll = TeamNotificationEntity(
            id = "poll_${UUID.randomUUID().toString().take(6)}",
            teamId = teamId,
            senderUid = aliceUid,
            senderName = "Alice",
            title = "Sprint Demo Time?",
            message = "Pick best slot",
            noticeType = "poll",
            pollOptionsJson = PollVotesCodec.encodeOptions(listOf("10 AM", "4 PM")),
            pollVotesJson = "{}"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.sendTeamNotification(poll) }.isSuccess)

        // Bob votes on poll and logs activity
        signInTestUser("ent_bob@test.com")
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobSync.voteOnTeamPoll(teamId, poll.id, 1) }.isSuccess)
        val act = TeamActivityEntity(
            id = "act_${UUID.randomUUID().toString().take(6)}",
            teamId = teamId,
            actorUid = bobUid,
            actorName = "Bob",
            actionType = "TASK",
            summary = "Moved Client Proposal Review to Review",
            targetId = reviewTask.id
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobSync.logTeamActivity(act) }.isSuccess)

        // Verify live listeners pick up poll vote, nested page, 5-stage task, and activity
        signInTestUser("ent_alice@test.com")
        val notifs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamNotifications(teamId).first { list ->
                list.any { it.id == poll.id && PollVotesCodec.parseVotes(it.pollVotesJson)[bobUid] == 1 }
            }
        }
        assertEquals(1, PollVotesCodec.parseVotes(notifs.first { it.id == poll.id }.pollVotesJson)[bobUid])

        val acts = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamActivities(teamId).first { list -> list.any { it.id == act.id } }
        }
        assertEquals("TASK", acts.first { it.id == act.id }.actionType)

        val docs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamDocs(teamId).first { list -> list.any { it.id == childPage.id } }
        }
        assertEquals(parentPage.id, docs.first { it.id == childPage.id }.parentId)

        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamTasks(teamId).first { list -> list.any { it.id == reviewTask.id } }
        }
        val syncedTask = tasks.first { it.id == reviewTask.id }
        assertEquals("review", syncedTask.status)
        assertEquals("biz_123", syncedTask.sourceBusinessId)
        assertEquals("https://drive.google.com/file/d/proposal", syncedTask.linkedDriveUrl)

        // Owner Alice removes Bob from the team
        val removeRes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceSync.removeTeamMember(teamId, bobUid) }
        assertTrue("Expected owner removeTeamMember success: ${removeRes.exceptionOrNull()}", removeRes.isSuccess)
        val remainingMembers = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSync.observeTeamMembers(teamId).first { list -> list.none { it.uid == bobUid } }
        }
        assertTrue(remainingMembers.none { it.uid == bobUid })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
