package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.FolderEntity
import com.example.data.local.ItemEntity
import com.example.data.local.NoteEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun pushAndFetchItem_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice_library@test.com")
        val repository = CloudSyncRepository(firestore)
        val itemId = "item_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val item = ItemEntity(
            id = itemId,
            userId = uid,
            title = "Jetpack Compose Navigation",
            description = "Official guide",
            originalUrl = "https://developer.android.com/guide/navigation",
            canonicalUrl = "https://developer.android.com/guide/navigation",
            domain = "developer.android.com",
            contentType = "article",
            sourceType = "web",
            isFavorite = true,
            status = "organized"
        )

        val pushResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.pushItem(item, listOf("folder-1"), listOf("compose"))
        }
        assertTrue("Expected pushItem success: ${pushResult.exceptionOrNull()}", pushResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getItemById(itemId).getOrThrow()
        }
        assertEquals("Jetpack Compose Navigation", fetched?.title)
        assertEquals(uid, fetched?.userId)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceItem() = runBlocking {
        val aliceUid = signInTestUser("alice2@test.com")
        val aliceRepo = CloudSyncRepository(firestore)
        val itemId = "item_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val item = ItemEntity(
            id = itemId,
            userId = aliceUid,
            title = "Alice Private Repo",
            contentType = "github",
            sourceType = "github",
            status = "unsorted"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.pushItem(item, emptyList(), emptyList()).getOrThrow()
        }

        signInTestUser("bob2@test.com")
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                // Attempt direct cross-user read bypassing repo scoping
                firestore.collection("users").document(aliceUid).collection("items")
                    .document(itemId).get().let { task ->
                        com.google.android.gms.tasks.Tasks.await(task)
                    }
            }
        } catch (e: Exception) {
            val fs = e.unwrapFirestoreException()
            if (fs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Bob must receive PERMISSION_DENIED when reading Alice's items", denied)
    }

    @Test
    fun observeCloudItems_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudSyncRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCloudItems("some_uid").first()
            }
        } catch (e: Exception) {
            // Either fail-fast IllegalStateException or PERMISSION_DENIED
            if (e is IllegalStateException ||
                e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                denied = true
            }
        }
        assertTrue("Unauthenticated observeCloudItems must fail", denied)
    }

    @Test
    fun pushFolderAndNote_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("carol@test.com")
        val repository = CloudSyncRepository(firestore)
        val folderId = "folder_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val noteId = "note_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val folderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.pushFolder(
                FolderEntity(
                    id = folderId,
                    userId = uid,
                    name = "AI Research"
                )
            )
        }
        assertTrue(folderResult.isSuccess)

        val noteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.pushNote(
                NoteEntity(
                    id = noteId,
                    itemId = "item_abc",
                    userId = uid,
                    body = "Important takeaways"
                )
            )
        }
        assertTrue(noteResult.isSuccess)
    }

    @Test
    fun pushAdminConfig_authenticatedOwnerSucceeds_crossUserDenied() = runBlocking {
        val aliceUid = signInTestUser("admin_alice@test.com")
        val repository = CloudSyncRepository(firestore)

        val pushRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.pushAdminConfig(
                aiProvider = "openrouter",
                openRouterApiKey = "sk-or-v1-test",
                openRouterModel = "openrouter/free",
                supabaseUrl = "https://example.supabase.co",
                supabaseAnonKey = "anon-key-sample",
                customCredentialsJson = "[]",
                adminPasscodeHash = "abc123",
                adminPasscodeSalt = "salt123"
            )
        }
        assertTrue("Expected pushAdminConfig success: ${pushRes.exceptionOrNull()}", pushRes.isSuccess)

        signInTestUser("intruder_bob@test.com")
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("admin_vault").document("api_config").get().let { task ->
                        com.google.android.gms.tasks.Tasks.await(task)
                    }
            }
        } catch (e: Exception) {
            val fs = e.unwrapFirestoreException()
            if (fs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Cross-user read on admin_vault must return PERMISSION_DENIED", denied)
    }

    @Test
    fun pushGlobalAiConfig_adminBroadcastsToAllSignedInMobileApps_nonAdminCannotOverwrite() = runBlocking {
        signInTestUser("global_admin_alice@test.com")
        val adminRepo = CloudSyncRepository(firestore)

        val pushGlobal = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.pushGlobalAiConfig(
                aiProvider = "openrouter",
                openRouterApiKey = "sk-or-global-key-1",
                openRouterModel = "meta-llama/llama-3.3-70b-instruct:free",
                supabaseUrl = "https://example.supabase.co",
                supabaseAnonKey = "anon-key"
            )
        }
        assertTrue("Expected pushGlobalAiConfig success: ${pushGlobal.exceptionOrNull()}", pushGlobal.isSuccess)

        // Another user on a different mobile phone signs in -> must receive live global key & free model
        signInTestUser("mobile_user_bob@test.com")
        val bobRepo = CloudSyncRepository(firestore)
        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.fetchGlobalAiConfig().getOrThrow()
        }
        assertEquals("sk-or-global-key-1", fetched?.openRouterApiKey)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", fetched?.openRouterModel)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeGlobalAiConfig().first { it?.openRouterApiKey == "sk-or-global-key-1" }
        }
        assertEquals("openrouter", observed?.aiProvider)

        // Non-admin Bob must NOT be allowed to overwrite /app_config/ai_runtime
        val bobAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.pushGlobalAiConfig(
                aiProvider = "openrouter",
                openRouterApiKey = "sk-or-hijack",
                openRouterModel = "openrouter/free",
                supabaseUrl = "",
                supabaseAnonKey = ""
            )
        }
        assertTrue("Non-admin overwrite of global config must fail", bobAttempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
