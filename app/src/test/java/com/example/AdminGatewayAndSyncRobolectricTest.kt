package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdminPasscodeCrypto
import com.example.data.local.AdminVaultConfig
import com.example.data.local.CustomCredentialEntry
import com.example.data.local.UserPreferencesStore
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.SecretAdminPasswordModal
import com.example.ui.screens.SettingsVersionFooterCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PersonalAiLibraryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminGatewayAndSyncRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenAdminSequence_requiresExactFourStageProgressionAndRapidExitTaps() {
        var unlockedCount = 0
        var fakeNow = 10_000L

        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SettingsVersionFooterCard(
                        onSecretAdminTrigger = { unlockedCount++ },
                        clockMillis = { fakeNow }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Stage 0: Less than 5 taps on version row does not reveal metrics or unlock
        repeat(4) {
            fakeNow += 150L
            composeRule.onNodeWithTag("settings_app_version_row").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("settings_subversion_metrics_panel").assertCountEquals(0)
        assertEquals(0, unlockedCount)

        // 5th tap reveals hidden sub-version metrics list
        fakeNow += 150L
        composeRule.onNodeWithTag("settings_app_version_row").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_subversion_metrics_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_subversion_metric_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_subversion_metric_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_subversion_metric_3").performScrollTo().assertIsDisplayed()
        assertEquals(0, unlockedCount)

        // Stage 1: Tapping 3rd sub-version item 2x then tapping 1st item resets counter
        repeat(2) {
            fakeNow += 150L
            composeRule.onNodeWithTag("settings_subversion_metric_2").performScrollTo().performClick()
        }
        fakeNow += 150L
        composeRule.onNodeWithTag("settings_subversion_metric_0").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("settings_hidden_error_banner").assertCountEquals(0)

        // Now tap 3rd sub-version metric 3 consecutive times -> triggers simulated error banner
        repeat(3) {
            fakeNow += 150L
            composeRule.onNodeWithTag("settings_subversion_metric_2").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_hidden_error_banner").performScrollTo().assertIsDisplayed()
        assertEquals(0, unlockedCount)

        // Stage 2: Tapping error banner 3 consecutive times -> transitions to secondary prompt with "Exit"
        repeat(3) {
            fakeNow += 150L
            composeRule.onNodeWithTag("settings_hidden_error_banner").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_hidden_secondary_error_prompt").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_hidden_error_exit_button").performScrollTo().assertIsDisplayed()
        assertEquals(0, unlockedCount)

        // Stage 3: Slow Exit taps (> 1800ms apart) should NOT unlock
        fakeNow += 2500L
        composeRule.onNodeWithTag("settings_hidden_error_exit_button").performScrollTo().performClick()
        fakeNow += 2500L
        composeRule.onNodeWithTag("settings_hidden_error_exit_button").performScrollTo().performClick()
        fakeNow += 2500L
        composeRule.onNodeWithTag("settings_hidden_error_exit_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(0, unlockedCount)

        // Advance clock past the 1800ms window, then 3 rapid Exit taps (< 1800ms apart) instantly unlocks
        fakeNow += 2500L
        repeat(3) {
            fakeNow += 200L
            composeRule.onNodeWithTag("settings_hidden_error_exit_button").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        assertEquals(1, unlockedCount)
    }

    @Test
    fun adminDashboard_validatesPasscodeRotationAndSavesCloudConfig() {
        var savedConfig: AdminVaultConfig? = null
        var rotatedPasscode: String? = null
        var refreshClicked = false

        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT) {
                AdminDashboardScreen(
                    config = AdminVaultConfig(
                        aiProvider = AdminVaultConfig.PROVIDER_FIREBASE_AI,
                        openRouterApiKey = "",
                        openRouterModel = AdminVaultConfig.DEFAULT_OPENROUTER_MODEL
                    ),
                    isSignedIn = true,
                    isOnline = true,
                    userEmail = "admin@example.com",
                    isTesting = false,
                    testStatus = null,
                    isCloudSyncing = false,
                    passcodeStatus = null,
                    onBackAndLock = {},
                    onGoogleSignInClick = {},
                    onRefreshCloudConfig = { refreshClicked = true },
                    onSaveConfig = { savedConfig = it },
                    onTestConnection = { _, _, _ -> },
                    onChangePasscode = { rotatedPasscode = it }
                )
            }
        }
        composeRule.waitForIdle()

        // Trigger cloud pull button
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_sync_now_button"))
        composeRule.onNodeWithTag("admin_sync_now_button").performClick()
        assertTrue(refreshClicked)

        // Switch provider & update OpenRouter key + free model
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_provider_openrouter"))
        composeRule.onNodeWithTag("admin_provider_openrouter").performClick()
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_openrouter_key_input"))
        composeRule.onNodeWithTag("admin_openrouter_key_input").performTextReplacement("sk-or-live-sync-key")
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_openrouter_model_input"))
        composeRule.onNodeWithTag("admin_openrouter_model_input").performTextReplacement("meta-llama/llama-3.3-70b-instruct:free")

        // Passcode mismatch must show validation error and NOT invoke onChangePasscode
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_new_passcode_input"))
        composeRule.onNodeWithTag("admin_new_passcode_input").performTextReplacement("vault2026")
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_confirm_passcode_input"))
        composeRule.onNodeWithTag("admin_confirm_passcode_input").performTextReplacement("mismatch")
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_rotate_passcode_button"))
        composeRule.onNodeWithTag("admin_rotate_passcode_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Passwords do not match.").assertIsDisplayed()
        assertEquals(null, rotatedPasscode)

        // Matching passcodes succeed
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_confirm_passcode_input"))
        composeRule.onNodeWithTag("admin_confirm_passcode_input").performTextReplacement("vault2026")
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_rotate_passcode_button"))
        composeRule.onNodeWithTag("admin_rotate_passcode_button").performClick()
        composeRule.waitForIdle()
        assertEquals("vault2026", rotatedPasscode)

        // Save config CTA sends updated provider + key + model
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_save_config_button"))
        composeRule.onNodeWithTag("admin_save_config_button").performClick()
        composeRule.waitForIdle()
        assertEquals(AdminVaultConfig.PROVIDER_OPENROUTER, savedConfig?.aiProvider)
        assertEquals("sk-or-live-sync-key", savedConfig?.openRouterApiKey)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", savedConfig?.openRouterModel)
    }

    @Test
    fun secretAdminPasswordModal_submitsPasscodeAndDisplaysAuthError() {
        var submittedPasscode = ""
        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                SecretAdminPasswordModal(
                    hasExistingPasscode = true,
                    authError = "Invalid admin passcode.",
                    onDismiss = {},
                    onSubmitPasscode = { submittedPasscode = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invalid admin passcode.").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_password_input").performTextReplacement("root9999")
        composeRule.onNodeWithTag("admin_password_submit").performClick()
        assertEquals("root9999", submittedPasscode)
    }

    @Test
    fun adminVaultConfig_persistsRotatedPasscodeHashAndCustomCredentialsInDataStore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = UserPreferencesStore(context)

        val salt1 = AdminPasscodeCrypto.generateSaltHex()
        val hash1 = AdminPasscodeCrypto.hashPasscode("initial123", salt1)
        val initial = AdminVaultConfig(
            aiProvider = AdminVaultConfig.PROVIDER_OPENROUTER,
            openRouterApiKey = "sk-or-cloud-1",
            openRouterModel = "anthropic/claude-3.5-sonnet",
            customCredentials = listOf(
                CustomCredentialEntry(keyName = "TAVILY_API_KEY", keyValue = "tvly-abc123")
            ),
            adminPasscodeHash = hash1,
            adminPasscodeSalt = salt1
        )
        store.saveAdminVaultConfig(initial)

        val loaded = store.adminVaultConfig.first()
        assertEquals(AdminVaultConfig.PROVIDER_OPENROUTER, loaded.aiProvider)
        assertEquals("sk-or-cloud-1", loaded.openRouterApiKey)
        assertEquals(AdminVaultConfig.DEFAULT_OPENROUTER_MODEL, loaded.openRouterModel)
        assertEquals("tvly-abc123", loaded.getCustomCredential("tavily_api_key"))
        assertTrue(AdminPasscodeCrypto.verify("initial123", loaded.adminPasscodeHash, loaded.adminPasscodeSalt))

        // Rotate password -> old password fails, new password verifies
        val salt2 = AdminPasscodeCrypto.generateSaltHex()
        val hash2 = AdminPasscodeCrypto.hashPasscode("rotated456", salt2)
        store.saveAdminVaultConfig(loaded.copy(adminPasscodeHash = hash2, adminPasscodeSalt = salt2))
        val rotated = store.adminVaultConfig.first()
        assertFalse(AdminPasscodeCrypto.verify("initial123", rotated.adminPasscodeHash, rotated.adminPasscodeSalt))
        assertTrue(AdminPasscodeCrypto.verify("rotated456", rotated.adminPasscodeHash, rotated.adminPasscodeSalt))
    }

    @Test
    fun adminDashboard_autoSelectsOpenRouterWhenKeyEnteredWithoutManualToggle() {
        var savedConfig: AdminVaultConfig? = null
        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                AdminDashboardScreen(
                    config = AdminVaultConfig(
                        aiProvider = AdminVaultConfig.PROVIDER_FIREBASE_AI,
                        openRouterApiKey = "",
                        openRouterModel = AdminVaultConfig.DEFAULT_OPENROUTER_MODEL
                    ),
                    isSignedIn = true,
                    isOnline = true,
                    userEmail = "admin@example.com",
                    isTesting = false,
                    testStatus = null,
                    onBackAndLock = {},
                    onGoogleSignInClick = {},
                    onSaveConfig = { savedConfig = it },
                    onTestConnection = { _, _, _ -> },
                    onChangePasscode = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_openrouter_key_input"))
        composeRule.onNodeWithTag("admin_openrouter_key_input").performTextReplacement("sk-or-v1-sample-only-test")
        composeRule.onNodeWithTag("admin_dashboard_list").performScrollToNode(hasTestTag("admin_save_config_button"))
        composeRule.onNodeWithTag("admin_save_config_button").performClick()
        composeRule.waitForIdle()

        assertEquals(AdminVaultConfig.PROVIDER_OPENROUTER, savedConfig?.aiProvider)
        assertEquals("sk-or-v1-sample-only-test", savedConfig?.openRouterApiKey)
        assertTrue(savedConfig?.providerManuallySelected == true)
    }
}
