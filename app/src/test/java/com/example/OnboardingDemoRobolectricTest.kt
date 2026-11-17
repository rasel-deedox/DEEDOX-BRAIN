package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.OnboardingProfile
import com.example.data.local.UserPreferencesStore
import com.example.data.local.UserSettingsEntity
import com.example.ui.components.HomeHeaderBar
import com.example.ui.screens.OnboardingAuthScreen
import com.example.ui.screens.ProfileManagementModal
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.PersonalAiLibraryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingDemoRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorialOnboardingWizard_walksFourStepsWithPaginationDots_toGoogleOnlyLogin() {
        var googleClicked = false
        var guestClicked = false
        var demoClicked = false
        var wizardMarkedCompleted = false
        var capturedRole = ""
        var capturedUseCase = ""
        var capturedHabit = ""
        var capturedWorkflow = ""

        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                OnboardingAuthScreen(
                    isSignedIn = false,
                    isWelcomeSyncing = false,
                    authError = null,
                    userDisplayName = "Creator",
                    userEmail = null,
                    onboardingProfile = OnboardingProfile(wizardCompleted = false),
                    themeMode = AppThemeMode.NIGHT,
                    onSelectThemeMode = {},
                    onExploreDemoClick = { demoClicked = true },
                    onContinueAsGuestClick = { guestClicked = true },
                    onGoogleSignInClick = { googleClicked = true },
                    onWizardCompleted = { role, useCase, habit, workflow ->
                        wizardMarkedCompleted = true
                        capturedRole = role
                        capturedUseCase = useCase
                        capturedHabit = habit
                        capturedWorkflow = workflow
                    },
                    onCompleteWizardAndSync = { _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Step 1: Official Brand Logo + Minimal Editorial Illustration + 4 Options + Pagination Dots + NEXT
        composeRule.onNodeWithTag("brand_neural_bookmark_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("editorial_illustration_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_pagination_dots").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("NEXT").assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_option_designer").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Step 2: 4 Options rendered
        composeRule.onNodeWithTag("wizard_option_bookmarks_repos").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Step 3: 4 Options including calm archive option
        composeRule.onNodeWithTag("wizard_option_quick_archive").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Step 4: Final step button says GET STARTED
        composeRule.onNodeWithText("GET STARTED").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_option_deep_sync").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(wizardMarkedCompleted)
        assertEquals("designer", capturedRole)
        assertEquals("bookmarks_repos", capturedUseCase)
        assertEquals("quick_archive", capturedHabit)
        assertEquals("deep_sync", capturedWorkflow)

        // Google-Only Liquid Glass Login screen — zero email/password forms
        composeRule.onNodeWithTag("google_login_page").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_login_app_logo").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Password", substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
                .isEmpty()
        )
        composeRule.onNodeWithTag("onboarding_google_signin_button").performScrollTo().performClick()
        assertTrue(googleClicked)

        composeRule.onNodeWithTag("onboarding_guest_button").performScrollTo().performClick()
        assertTrue(guestClicked)

        composeRule.onNodeWithTag("onboarding_explore_demo_button").performScrollTo().performClick()
        assertTrue(demoClicked)
    }

    @Test
    fun onboardingWizard_skipsQuestionsWhenWizardAlreadyCompleted() {
        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT) {
                OnboardingAuthScreen(
                    isSignedIn = false,
                    isWelcomeSyncing = false,
                    authError = null,
                    userDisplayName = "Creator",
                    userEmail = null,
                    onboardingProfile = OnboardingProfile(
                        role = "developer",
                        primaryUseCase = "ai_knowledge",
                        saveHabit = "daily_curator",
                        workflowPreset = "starter_folders",
                        wizardCompleted = true
                    ),
                    themeMode = AppThemeMode.LIGHT,
                    onSelectThemeMode = {},
                    onExploreDemoClick = {},
                    onGoogleSignInClick = {},
                    onCompleteWizardAndSync = { _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_login_page").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_google_signin_button").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Password", substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }

    @Test
    fun topLeftProfileAvatar_clicksToOpenProfileModal() {
        var avatarClicked = false

        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                HomeHeaderBar(
                    userDisplayName = "Amina",
                    profileAvatar = "preset:crimson",
                    isSignedIn = true,
                    isOnline = true,
                    themeMode = AppThemeMode.NIGHT,
                    syncBadgeState = SyncBadgeState.SYNCED,
                    notificationCount = 0,
                    showFiltersExpanded = true,
                    onProfileAvatarClick = { avatarClicked = true },
                    onToggleFiltersExpanded = {},
                    onBellClick = {},
                    onOpenDeviceSyncModal = {},
                    onSelectThemeMode = {},
                    onOpenSyncSettings = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodesWithTag("brand_neural_bookmark_logo")
                .fetchSemanticsNodes()
                .isEmpty()
        )
        composeRule.onNodeWithTag("home_header_app_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("home_profile_avatar_button").performClick()
        assertTrue(avatarClicked)
    }

    @Test
    fun profileManagementModal_savesUpdatedDisplayNameAndEmail() {
        var savedName = ""
        var savedEmail = ""

        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT) {
                ProfileManagementModal(
                    userDisplayName = "Amina",
                    userEmail = "amina@example.com",
                    profileAvatar = "preset:terracotta",
                    pendingPickedAvatarUri = null,
                    isSignedIn = true,
                    themeMode = AppThemeMode.LIGHT,
                    accentPreset = LiquidAccentPreset.CRIMSON_BRONZE,
                    syncBadgeState = SyncBadgeState.SYNCED,
                    settings = UserSettingsEntity(),
                    onDismiss = {},
                    onPickAvatarPhotoClick = {},
                    onClearPendingPickedAvatar = {},
                    onSaveProfile = { name, email, _, _ ->
                        savedName = name
                        savedEmail = email
                    },
                    onSelectThemeMode = {},
                    onSelectAccentPreset = {},
                    onUpdateSettings = {},
                    onGoogleSignInClick = {},
                    onSignOutClick = {},
                    onSyncNowClick = {},
                    onOpenDeviceSyncModal = {},
                    onOpenFullSettings = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_modal_app_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_modal_name_input").performScrollTo().performTextReplacement("Amina Rahman")
        composeRule.onNodeWithTag("profile_modal_email_input").performScrollTo().performTextReplacement("amina.dev@example.com")
        composeRule.onNodeWithTag("profile_modal_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Amina Rahman", savedName)
        assertEquals("amina.dev@example.com", savedEmail)
    }

    @Test
    fun onboardingWelcomeSyncTransition_displaysFrostedLoadingCard() {
        composeRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT) {
                OnboardingAuthScreen(
                    isSignedIn = true,
                    isWelcomeSyncing = true,
                    authError = null,
                    userDisplayName = "Amina",
                    userEmail = "amina@example.com",
                    themeMode = AppThemeMode.LIGHT,
                    onSelectThemeMode = {},
                    onExploreDemoClick = {},
                    onGoogleSignInClick = {},
                    onCompleteWizardAndSync = { _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("welcome_sync_loading_card").assertIsDisplayed()
    }

    @Test
    fun userPreferencesStore_savesAndLoadsOnboardingProfileAndCustomProfileFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = UserPreferencesStore(context)
        store.saveOnboardingProfile(
            OnboardingProfile(
                role = "designer",
                primaryUseCase = "bookmarks_repos",
                saveHabit = "deep_research",
                workflowPreset = "deep_sync",
                wizardCompleted = true
            )
        )
        store.setCustomEmail("designer@example.com")
        store.setProfileAvatar("preset:crimson")

        val saved = store.onboardingProfile.first()
        assertEquals("designer", saved.role)
        assertEquals("bookmarks_repos", saved.primaryUseCase)
        assertEquals("deep_research", saved.saveHabit)
        assertEquals("deep_sync", saved.workflowPreset)
        assertTrue(saved.wizardCompleted)
        assertEquals("designer@example.com", store.customEmail.first())
        assertEquals("preset:crimson", store.profileAvatar.first())
    }
}
