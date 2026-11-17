package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ai.AiLibraryService
import com.example.data.cloud.CloudOsSync
import com.example.data.cloud.CloudSyncRepository
import com.example.data.local.AppDatabase
import com.example.data.local.FtsSearchEngine
import com.example.data.local.ItemEntity
import com.example.data.local.UserPreferencesStore
import com.example.data.media.VoiceAudioEngine
import com.example.data.network.MetadataEnricher
import com.example.data.repository.HabitGamificationEngine
import com.example.data.repository.LibraryRepository
import com.example.data.repository.OsRepository
import com.example.data.repository.WorkspaceRepository
import com.example.data.storage.LocalFileReferenceManager
import com.example.firebase.GoogleAuthHelper
import com.example.firebase.configureAppCheck
import com.example.ui.components.AppLogo
import com.example.ui.components.EditResourceDialog
import com.example.ui.components.FloatingIconlyBottomDock
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MainNavTab
import com.example.ui.components.SmartReminderSheet
import com.example.ui.components.SyncAndSendDeviceModal
import com.example.ui.components.TactileAlabasterBackground
import com.example.ui.components.WriteReviewModal
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AiOrganizationPreviewScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubSection
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.LauncherMetrics
import com.example.ui.screens.LibraryHubScreen
import com.example.ui.screens.MoneyLedgerScreen
import com.example.ui.screens.MonkModeScreen
import com.example.ui.screens.OnboardingAuthScreen
import com.example.ui.screens.ProfileManagementModal
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SecretAdminPasswordModal
import com.example.ui.screens.StartupOsScreen
import com.example.ui.screens.TeamCollabScreen
import com.example.ui.screens.UniversalCaptureSheet
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PersonalAiLibraryTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

private const val ROUTE_HOME = "home"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_FOLDERS = "folders"
private const val ROUTE_WORKSPACE = "workspace"
private const val ROUTE_MONK = "monk"
private const val ROUTE_STARTUP_OS = "startup_os"
private const val ROUTE_MONEY_LEDGER = "money_ledger"
private const val ROUTE_TEAM_COLLAB = "team_collab"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_DETAIL = "detail/{itemId}"
private const val ROUTE_AI_ASSISTANT = "ai_assistant"
private const val ROUTE_AI_ORG_PREVIEW = "ai_org_preview/{planId}"
private const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"

class MainActivity : ComponentActivity() {

    private var activeViewModel: LibraryViewModel? = null
    private var pendingSharePayload: PendingSharePayload? = null

    private data class PendingSharePayload(
        val text: String,
        val title: String,
        val uris: List<Uri>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // App Check MUST be configured before Firebase AI or Firestore calls
        configureAppCheck(this, intent)

        pendingSharePayload = parseShareIntent(intent)

        val appCtx = applicationContext
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(appCtx)
                val fts = FtsSearchEngine(db)
                val enricher = MetadataEnricher(appCtx)
                val fileMgr = LocalFileReferenceManager(appCtx)
                val cloudSync = CloudSyncRepository(appCtx)
                val aiService = AiLibraryService()
                val prefs = UserPreferencesStore(appCtx)
                val repo = LibraryRepository(
                    context = appCtx,
                    database = db,
                    ftsSearchEngine = fts,
                    metadataEnricher = enricher,
                    fileManager = fileMgr,
                    cloudSync = cloudSync,
                    aiService = aiService,
                    preferencesStore = prefs
                )
                val cloudOsSync = CloudOsSync(appCtx)
                val workspaceRepo = WorkspaceRepository(appCtx, db, cloudOsSync = cloudOsSync)
                val osRepo = OsRepository(appCtx, db, cloudOsSync, aiService)
                val voiceEngine = VoiceAudioEngine(
                    context = appCtx,
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                )
                return LibraryViewModel(repo, workspaceRepo, voiceEngine, osRepo, cloudOsSync) as T
            }
        }

        setContent {
            val vm: LibraryViewModel = viewModel(factory = factory)
            activeViewModel = vm

            val currentTheme by vm.themeMode.collectAsStateWithLifecycle()
            LaunchedEffect(currentTheme) {
                val style = if (currentTheme == AppThemeMode.LIGHT) {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                } else {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            LaunchedEffect(pendingSharePayload) {
                pendingSharePayload?.let { payload ->
                    vm.openCaptureSheet(
                        initialText = payload.text,
                        initialTitle = payload.title,
                        uris = payload.uris
                    )
                    pendingSharePayload = null
                }
            }

            PersonalAiLibraryApp(viewModel = vm)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val payload = parseShareIntent(intent) ?: return
        val vm = activeViewModel
        if (vm != null) {
            vm.openCaptureSheet(
                initialText = payload.text,
                initialTitle = payload.title,
                uris = payload.uris
            )
        } else {
            pendingSharePayload = payload
        }
    }

    private fun parseShareIntent(intent: Intent?): PendingSharePayload? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val uris = mutableListOf<Uri>()

        if (action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                uris.add(it)
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                uris.addAll(it)
            }
        }

        if (uris.isEmpty()) {
            intent.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { uris.add(it) }
                }
            }
        }

        if (sharedText.isBlank() && sharedSubject.isBlank() && uris.isEmpty()) return null
        return PendingSharePayload(text = sharedText, title = sharedSubject, uris = uris)
    }
}

@Composable
fun PersonalAiLibraryApp(viewModel: LibraryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentPreset by viewModel.accentPreset.collectAsStateWithLifecycle()
    val adminVaultConfig by viewModel.adminVaultConfig.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val adminAuthError by viewModel.adminAuthError.collectAsStateWithLifecycle()
    val isAdminTesting by viewModel.isAdminTesting.collectAsStateWithLifecycle()
    val adminTestStatus by viewModel.adminTestStatus.collectAsStateWithLifecycle()
    val isAdminCloudSyncing by viewModel.isAdminCloudSyncing.collectAsStateWithLifecycle()
    val adminPasscodeStatus by viewModel.adminPasscodeStatus.collectAsStateWithLifecycle()
    val freeOpenRouterModels by viewModel.freeOpenRouterModels.collectAsStateWithLifecycle()
    val isLoadingFreeModels by viewModel.isLoadingFreeModels.collectAsStateWithLifecycle()
    val vaultPin by viewModel.vaultPin.collectAsStateWithLifecycle()
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val connectedDevices by viewModel.connectedDevices.collectAsStateWithLifecycle()
    val deviceTransfers by viewModel.deviceTransfers.collectAsStateWithLifecycle()
    val currentDeviceId by viewModel.currentDeviceId.collectAsStateWithLifecycle()

    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val profileAvatar by viewModel.profileAvatar.collectAsStateWithLifecycle()
    val onboardingProfile by viewModel.onboardingProfile.collectAsStateWithLifecycle()
    val isWelcomeSyncTransitioning by viewModel.isWelcomeSyncTransitioning.collectAsStateWithLifecycle()
    val captureDraft by viewModel.captureDraft.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiBusy by viewModel.isAiBusy.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val lastUndoablePlanId by viewModel.lastUndoablePlanId.collectAsStateWithLifecycle()
    val itemSuggestion by viewModel.itemSuggestion.collectAsStateWithLifecycle()
    val workspaceSnapshot by viewModel.workspaceSnapshot.collectAsStateWithLifecycle()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsStateWithLifecycle()
    val voicePlaybackState by viewModel.voicePlaybackState.collectAsStateWithLifecycle()
    val launchableApps by viewModel.launchableApps.collectAsStateWithLifecycle()
    val monkPermissions by viewModel.monkPermissions.collectAsStateWithLifecycle()
    val detoxState by viewModel.detoxState.collectAsStateWithLifecycle()
    val startupProjects by viewModel.startupProjects.collectAsStateWithLifecycle()
    val startupPhaseItems by viewModel.startupPhaseItems.collectAsStateWithLifecycle()
    val startupSprintTasks by viewModel.startupSprintTasks.collectAsStateWithLifecycle()
    val isGeneratingStartupPlan by viewModel.isGeneratingStartupPlan.collectAsStateWithLifecycle()
    val aiStartupError by viewModel.aiStartupError.collectAsStateWithLifecycle()
    val financeSectors by viewModel.financeSectors.collectAsStateWithLifecycle()
    val financeTransactions by viewModel.financeTransactions.collectAsStateWithLifecycle()
    val recentFinanceIconKeys by viewModel.recentFinanceIconKeys.collectAsStateWithLifecycle()
    val financeTxIconOverrides by viewModel.financeTxIconOverrides.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val activeTeamId by viewModel.activeTeamId.collectAsStateWithLifecycle()
    val activeTeamMembers by viewModel.activeTeamMembers.collectAsStateWithLifecycle()
    val activeTeamTasks by viewModel.activeTeamTasks.collectAsStateWithLifecycle()
    val activeTeamDocs by viewModel.activeTeamDocs.collectAsStateWithLifecycle()
    val activeTeamNotifications by viewModel.activeTeamNotifications.collectAsStateWithLifecycle()
    val activeTeamActivities by viewModel.activeTeamActivities.collectAsStateWithLifecycle()
    val activeTeamEmailInvites by viewModel.activeTeamEmailInvites.collectAsStateWithLifecycle()
    val incomingEmailInvites by viewModel.incomingEmailInvites.collectAsStateWithLifecycle()
    val latestTeamAlertBanner by viewModel.latestTeamAlertBanner.collectAsStateWithLifecycle()
    val inviteLookupPreview by viewModel.inviteLookupPreview.collectAsStateWithLifecycle()
    val teamBusy by viewModel.teamBusy.collectAsStateWithLifecycle()
    val teamError by viewModel.teamError.collectAsStateWithLifecycle()

    var authError by remember { mutableStateOf<String?>(null) }
    var hubInitialSection by remember { mutableStateOf(HubSection.ALL_PAGES) }
    var pendingFileTargetFolderId by remember { mutableStateOf(AppDatabase.INBOX_FOLDER_ID) }
    var pendingFileTargetCollectionId by remember { mutableStateOf<String?>(null) }

    var reminderSheetRequest by remember { mutableStateOf<Triple<String, String?, String?>?>(null) }

    var showDeviceSyncModal by remember { mutableStateOf(false) }
    var deviceModalPrefillTitle by remember { mutableStateOf("") }
    var deviceModalPrefillPayload by remember { mutableStateOf("") }

    var showProfileModal by remember { mutableStateOf(false) }
    var pendingPickedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    var showWriteReviewModal by remember { mutableStateOf(false) }
    var reviewTargetItemId by remember { mutableStateOf<String?>(null) }

    var showAdminPasswordModal by remember { mutableStateOf(false) }
    var editTargetItem by remember { mutableStateOf<ItemEntity?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<ItemEntity?>(null) }

    val openSendItemToDevice = remember<(ItemEntity) -> Unit>(viewModel) {
        { item ->
            deviceModalPrefillTitle = item.title
            deviceModalPrefillPayload = item.originalUrl ?: item.canonicalUrl ?: item.description.ifBlank { item.title }
            showDeviceSyncModal = true
        }
    }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                authError = null
                viewModel.registerDevicePresence()
            },
            onUnauthenticated = {},
            scope = scope
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val colTarget = pendingFileTargetCollectionId
        pendingFileTargetCollectionId = null
        uri?.let { viewModel.captureLocalFile(it, pendingFileTargetFolderId, colTarget) }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        val colTarget = pendingFileTargetCollectionId
        pendingFileTargetCollectionId = null
        if (uris.isNotEmpty()) {
            viewModel.captureMultipleUris(uris, pendingFileTargetFolderId, colTarget)
        }
    }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingPickedAvatarUri = uri
        }
    }

    val connectFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { viewModel.connectFolderTree(it) }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val speechCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty().trim()
            if (spoken.isNotBlank()) {
                viewModel.openCaptureSheet(initialText = spoken, initialTitle = "Voice Note")
            }
        }
    }

    val launchSpeechCapture = remember<() -> Unit>(viewModel, speechCaptureLauncher) {
        {
            try {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a note, topic, or link to save...")
                }
                speechCaptureLauncher.launch(speechIntent)
            } catch (_: Exception) {
                viewModel.openCaptureSheet(initialTitle = "Voice Note")
            }
        }
    }

    val launchGoogleSignIn = remember<() -> Unit>(context, credentialManager, scope, viewModel) {
        {
            authError = null
            GoogleAuthHelper.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    authError = null
                    viewModel.updateSettings { it.copy(cloudSyncOptIn = true) }
                    viewModel.registerDevicePresence()
                    viewModel.triggerSyncNow()
                },
                onAuthError = { err -> authError = err },
                scope = scope
            )
        }
    }

    val onSetThemeMode = remember<(AppThemeMode) -> Unit>(viewModel) { { viewModel.setThemeMode(it) } }
    val onToggleFavoriteItem = remember<(ItemEntity) -> Unit>(viewModel) { { viewModel.toggleFavorite(it) } }
    val onToggleReviewHelpful = remember<(String) -> Unit>(viewModel) { { viewModel.toggleReviewHelpful(it) } }
    val onOpenEmptyDeviceSyncModal = remember<() -> Unit> {
        {
            deviceModalPrefillTitle = ""
            deviceModalPrefillPayload = ""
            showDeviceSyncModal = true
        }
    }
    val onSignOutToGuest = remember<() -> Unit>(credentialManager, scope, viewModel) {
        {
            viewModel.signOutSupabaseSession()
            GoogleAuthHelper.signOut(credentialManager, scope) {
                viewModel.updateSettings { it.copy(cloudSyncOptIn = false) }
            }
        }
    }
    val onEditItemCallback = remember<(ItemEntity) -> Unit> { { item -> editTargetItem = item } }
    val onDeleteItemCallback = remember<(ItemEntity) -> Unit> { { item -> pendingDeleteItem = item } }

    var showLaunchSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(550L)
        showLaunchSplash = false
    }

    PersonalAiLibraryTheme(themeMode = themeMode, accentPreset = accentPreset) {
        Box(modifier = Modifier.fillMaxSize()) {
        if (!snapshot.settings.onboardingCompleted || isWelcomeSyncTransitioning) {
            OnboardingAuthScreen(
                isSignedIn = isSignedIn,
                isWelcomeSyncing = isWelcomeSyncTransitioning,
                authError = authError,
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                onboardingProfile = onboardingProfile,
                themeMode = themeMode,
                onSelectThemeMode = onSetThemeMode,
                onExploreDemoClick = remember(viewModel) { { viewModel.enterDemoMode() } },
                onContinueAsGuestClick = remember(viewModel) {
                    { viewModel.completeOnboarding(guestMode = true, cloudSyncOptIn = false) }
                },
                onGoogleSignInClick = launchGoogleSignIn,
                onWizardCompleted = remember(viewModel) {
                    { role, useCase, saveHabit, workflow ->
                        viewModel.markOnboardingWizardCompleted(role, useCase, saveHabit, workflow)
                    }
                },
                onCompleteWizardAndSync = remember(viewModel) {
                    { role, useCase, saveHabit, workflow ->
                        viewModel.completeWizardAndGoogleSync(role, useCase, saveHabit, workflow)
                    }
                },
                onClearAuthError = { authError = null }
            )
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

            val activeTab = when (currentRoute) {
                ROUTE_HOME -> MainNavTab.HOME
                ROUTE_SEARCH -> MainNavTab.SEARCH
                ROUTE_FOLDERS -> MainNavTab.FOLDERS
                ROUTE_LIBRARY -> MainNavTab.LIBRARY
                else -> null
            }

            val isOnline = viewModel.isOnline()
            val syncBadge = viewModel.getSyncBadgeState()
            val pendingTransfersCount = remember(deviceTransfers) {
                deviceTransfers.count { it.status == "pending" }
            }

            val onSelectTabCallback = remember<(MainNavTab) -> Unit>(navController) {
                { tab ->
                    if (tab == MainNavTab.LIBRARY) {
                        hubInitialSection = HubSection.ALL_PAGES
                    }
                    navController.navigate(tab.route) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            val onOpenCaptureSheetCallback = remember<() -> Unit>(viewModel) {
                { viewModel.openCaptureSheet() }
            }
            val onOpenItemCallback = remember<(String) -> Unit>(viewModel, navController) {
                { itemId ->
                    viewModel.clearItemSuggestion()
                    navController.navigate("detail/$itemId")
                }
            }
            val onOrganizeInboxCallback = remember<() -> Unit>(viewModel, navController) {
                {
                    viewModel.generateOrganizationPlanForInbox { planId ->
                        navController.navigate("ai_org_preview/$planId")
                    }
                }
            }
            val onOpenAiAssistantCallback = remember<() -> Unit>(navController) {
                { navController.navigate(ROUTE_AI_ASSISTANT) }
            }
            val onOpenSearchCallback = remember<() -> Unit>(navController) {
                {
                    navController.navigate(ROUTE_SEARCH) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            val onOpenSyncSettingsCallback = remember<() -> Unit>(navController) {
                {
                    hubInitialSection = HubSection.SETTINGS
                    navController.navigate(ROUTE_LIBRARY) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            val onOpenInboxCallback = remember<() -> Unit>(navController) {
                {
                    hubInitialSection = HubSection.INBOX
                    navController.navigate(ROUTE_LIBRARY) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            val onUploadMediaInboxCallback = remember<() -> Unit>(pickMediaLauncher) {
                {
                    pendingFileTargetFolderId = AppDatabase.INBOX_FOLDER_ID
                    pendingFileTargetCollectionId = null
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            }
            val onPickLocalFileInboxCallback = remember<() -> Unit>(pickFileLauncher) {
                {
                    pendingFileTargetFolderId = AppDatabase.INBOX_FOLDER_ID
                    pendingFileTargetCollectionId = null
                    pickFileLauncher.launch(arrayOf("*/*"))
                }
            }

            TactileAlabasterBackground {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (activeTab != null) {
                            FloatingIconlyBottomDock(
                                selectedTab = activeTab,
                                isAiGlowing = isAiBusy || searchState.isOnlineAiMode,
                                onSelectTab = onSelectTabCallback,
                                onAddClick = onOpenCaptureSheetCallback
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    snapshot = snapshot,
                                    themeMode = themeMode,
                                    reviews = reviews,
                                    connectedDevices = connectedDevices,
                                    pendingTransfersCount = pendingTransfersCount,
                                    userDisplayName = userDisplayName,
                                    profileAvatar = profileAvatar,
                                    isSignedIn = isSignedIn,
                                    isOnline = isOnline,
                                    syncBadgeState = syncBadge,
                                    isAiBusy = isAiBusy,
                                    aiError = aiError,
                                    lastUndoablePlanId = lastUndoablePlanId,
                                    onOpenProfileModal = remember {
                                        {
                                            pendingPickedAvatarUri = null
                                            showProfileModal = true
                                        }
                                    },
                                    onSelectThemeMode = onSetThemeMode,
                                    onOpenDeviceSyncModal = onOpenEmptyDeviceSyncModal,
                                    onSendItemToDevice = openSendItemToDevice,
                                    onOpenSyncSettings = onOpenSyncSettingsCallback,
                                    onOpenInbox = onOpenInboxCallback,
                                    onOpenCapture = onOpenCaptureSheetCallback,
                                    onSaveQuickInput = remember<(String) -> Unit>(viewModel) {
                                        { rawText ->
                                            viewModel.saveCapture(
                                                rawInput = rawText,
                                                title = "",
                                                folderId = AppDatabase.INBOX_FOLDER_ID,
                                                tagsCsv = "",
                                                note = "",
                                                allowDuplicate = false
                                            )
                                        }
                                    },
                                    onSpeakCapture = launchSpeechCapture,
                                    onUploadMedia = onUploadMediaInboxCallback,
                                    onOpenSearch = onOpenSearchCallback,
                                    onOpenItem = onOpenItemCallback,
                                    onToggleFavorite = onToggleFavoriteItem,
                                    onOrganizeInboxClick = onOrganizeInboxCallback,
                                    onUndoLastPlanClick = remember(viewModel) { { viewModel.undoLastOrganizationPlan() } },
                                    onOpenAiAssistant = onOpenAiAssistantCallback,
                                    onWriteReviewClick = remember {
                                        {
                                            reviewTargetItemId = null
                                            showWriteReviewModal = true
                                        }
                                    },
                                    onToggleReviewHelpful = onToggleReviewHelpful,
                                    onEditItem = onEditItemCallback,
                                    onDeleteItem = onDeleteItemCallback
                                )
                            }

                            composable(ROUTE_SEARCH) {
                                SearchScreen(
                                    snapshot = snapshot,
                                    searchState = searchState,
                                    isOnline = isOnline,
                                    onQueryChange = remember(viewModel) { { viewModel.updateSearchQuery(it) } },
                                    onToggleAiMode = remember(viewModel) { { viewModel.setSearchAiMode(it) } },
                                    onSelectTypeFilter = remember(viewModel) { { viewModel.setSearchContentTypeFilter(it) } },
                                    onSelectFolderFilter = remember(viewModel) { { viewModel.setSearchFolderFilter(it) } },
                                    onToggleFavoritesOnly = remember(viewModel) { { viewModel.toggleSearchFavoritesOnly() } },
                                    onOpenItem = onOpenItemCallback,
                                    onToggleFavorite = onToggleFavoriteItem,
                                    onSendItemToDevice = openSendItemToDevice,
                                    onEditItem = onEditItemCallback,
                                    onDeleteItem = onDeleteItemCallback
                                )
                            }

                            composable(ROUTE_FOLDERS) {
                                FoldersScreen(
                                    snapshot = snapshot,
                                    vaultPin = vaultPin,
                                    isVaultLocked = isVaultLocked,
                                    onSetVaultPin = remember(viewModel) { { viewModel.setVaultPin(it) } },
                                    onSetVaultLocked = remember(viewModel) { { viewModel.setVaultLocked(it) } },
                                    onCreateFolder = remember(viewModel) { { name, parentId -> viewModel.createFolder(name, parentId) } },
                                    onRenameFolder = remember(viewModel) { { id, newName -> viewModel.renameFolder(id, newName) } },
                                    onDeleteFolder = remember(viewModel) { { id, moveTarget -> viewModel.deleteFolder(id, moveTarget) } },
                                    onAddStarterFolders = remember(viewModel) { { viewModel.addStarterFolders() } },
                                    onRenameTag = remember(viewModel) { { id, newName -> viewModel.renameTag(id, newName) } },
                                    onDeleteTag = remember(viewModel) { { id -> viewModel.deleteTag(id) } },
                                    onCreateCollection = remember(viewModel) { { name, desc -> viewModel.createCollection(name, desc) } },
                                    onOpenItem = onOpenItemCallback,
                                    onToggleFavorite = onToggleFavoriteItem,
                                    onSendItemToDevice = openSendItemToDevice,
                                    onEditItem = onEditItemCallback,
                                    onDeleteItem = onDeleteItemCallback,
                                    onQuickAddToFolder = remember(viewModel) {
                                        { folderId ->
                                            viewModel.openCaptureSheet(targetFolderId = folderId)
                                        }
                                    },
                                    onPickMediaForTarget = remember(pickMediaLauncher) {
                                        { folderId, collectionId ->
                                            pendingFileTargetFolderId = folderId
                                            pendingFileTargetCollectionId = collectionId
                                            pickMediaLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        }
                                    },
                                    onPickFileForTarget = remember(pickFileLauncher) {
                                        { folderId, collectionId ->
                                            pendingFileTargetFolderId = folderId
                                            pendingFileTargetCollectionId = collectionId
                                            pickFileLauncher.launch(arrayOf("*/*"))
                                        }
                                    },
                                    onQuickAddToCollection = remember(viewModel) {
                                        { collectionId ->
                                            viewModel.openCaptureSheet(targetCollectionId = collectionId)
                                        }
                                    },
                                    onRemoveItemFromCollection = remember(viewModel) {
                                        { colId, itemId -> viewModel.removeItemFromCollection(colId, itemId) }
                                    },
                                    onDeleteCollection = remember(viewModel) {
                                        { colId -> viewModel.deleteCollection(colId) }
                                    },
                                    onOpenWorkspace = remember(navController) {
                                        {
                                            navController.navigate(ROUTE_WORKSPACE) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onOpenMonkMode = remember(navController) {
                                        {
                                            navController.navigate(ROUTE_MONK) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_WORKSPACE) {
                                WorkspaceScreen(
                                    workspaceSnapshot = workspaceSnapshot,
                                    libraryItems = snapshot.items,
                                    recorderState = voiceRecorderState,
                                    playbackState = voicePlaybackState,
                                    onStartVoiceRecording = remember(viewModel) { { viewModel.startVoiceRecording() } },
                                    onPauseVoiceRecording = remember(viewModel) { { viewModel.pauseVoiceRecording() } },
                                    onResumeVoiceRecording = remember(viewModel) { { viewModel.resumeVoiceRecording() } },
                                    onDiscardVoiceRecording = remember(viewModel) { { viewModel.discardVoiceRecording() } },
                                    onSaveVoiceToNote = remember(viewModel) { { title -> viewModel.stopVoiceRecordingToNote(title) } },
                                    onSaveVoiceToTodo = remember(viewModel) { { title -> viewModel.stopVoiceRecordingToTask(title) } },
                                    onToggleVoicePlayback = remember(viewModel) { { uri -> viewModel.toggleVoicePlayback(uri) } },
                                    onSeekVoicePlayback = remember(viewModel) { { uri, pos -> viewModel.seekVoicePlayback(uri, pos) } },
                                    onSaveNote = remember(viewModel) {
                                        { id, title, blocks, tags, pinned, cloudSync, linkedId, audioUri, durMs ->
                                            viewModel.saveWorkspaceNote(id, title, blocks, tags, pinned, cloudSync, linkedId, audioUri, durMs)
                                        }
                                    },
                                    onToggleNotePin = remember(viewModel) { { note -> viewModel.toggleWorkspaceNotePin(note) } },
                                    onToggleNoteCloudSync = remember(viewModel) { { note -> viewModel.toggleWorkspaceNoteCloudSync(note) } },
                                    onDeleteNote = remember(viewModel) { { id -> viewModel.deleteWorkspaceNote(id) } },
                                    onSaveTask = remember(viewModel) {
                                        { id, title, notes, day, priority, cloudSync ->
                                            viewModel.saveWorkspaceTask(id, title, notes, day, priority, cloudSync)
                                        }
                                    },
                                    onToggleTaskCompleted = remember(viewModel) { { task -> viewModel.toggleTaskCompleted(task) } },
                                    onDeleteTask = remember(viewModel) { { id -> viewModel.deleteWorkspaceTask(id) } },
                                    onOpenReminderSheet = remember {
                                        { title, itemId, noteId ->
                                            reminderSheetRequest = Triple(title, itemId, noteId)
                                        }
                                    },
                                    onToggleReminder = remember(viewModel) { { rem -> viewModel.toggleReminderEnabled(rem) } },
                                    onDeleteReminder = remember(viewModel) { { id -> viewModel.deleteReminder(id) } },
                                    onCreateHabit = remember(viewModel) {
                                        { title, sub, cat, target, cloudSync, remEnabled, remHour, remMin, recType, intervalMin, ringUri, ringTitle ->
                                            viewModel.createHabit(
                                                title = title,
                                                subtitle = sub,
                                                category = cat,
                                                targetPerWeek = target,
                                                cloudSync = cloudSync,
                                                reminderEnabled = remEnabled,
                                                reminderHour = remHour,
                                                reminderMinute = remMin,
                                                recurrenceType = recType,
                                                intervalMinutes = intervalMin,
                                                ringtoneUri = ringUri,
                                                ringtoneTitle = ringTitle
                                            )
                                        }
                                    },
                                    onToggleHabitToday = remember(viewModel) { { id -> viewModel.toggleHabitCompletion(id) } },
                                    onUpdateHabitReminder = remember(viewModel) {
                                        { habit, remEnabled, remHour, remMin, recType, intervalMin, ringUri, ringTitle ->
                                            viewModel.updateHabitReminder(
                                                habit = habit,
                                                reminderEnabled = remEnabled,
                                                reminderHour = remHour,
                                                reminderMinute = remMin,
                                                recurrenceType = recType,
                                                intervalMinutes = intervalMin,
                                                ringtoneUri = ringUri,
                                                ringtoneTitle = ringTitle
                                            )
                                        }
                                    },
                                    onDeleteHabit = remember(viewModel) { { id -> viewModel.deleteHabit(id) } },
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    onPolishPageWithAi = viewModel::polishCanvasPageWithAi,
                                    onLogClientPaymentToLedger = remember(viewModel) {
                                        { client, project, cents -> viewModel.recordCanvasClientPaymentToLedger(client, project, cents) }
                                    },
                                    onScheduleInlineReminder = remember(viewModel) {
                                        { rem, noteId ->
                                            viewModel.createCanvasInlineReminder(
                                                title = rem.title,
                                                triggerAtMillis = rem.triggerAtMillis,
                                                ringtoneTitle = rem.ringtoneTitle,
                                                noteId = noteId,
                                                teamId = null,
                                                taggedMemberName = rem.taggedMemberName.ifBlank { null },
                                                taggedMemberUid = rem.taggedMemberUid.ifBlank { null }
                                            )
                                        }
                                    },
                                    onSyncScheduleTask = remember(viewModel) {
                                        { sched -> viewModel.syncCanvasScheduleBlock(null, sched, emptyList()) }
                                    },
                                    onLogFocusTimerSeconds = remember(viewModel) {
                                        { lbl, secs -> viewModel.logCanvasFocusTimerSession(lbl, secs) }
                                    },
                                    onStartInlineVoiceRecord = remember(viewModel) { { viewModel.startInlineCanvasVoiceRecording() } },
                                    onStopInlineVoiceRecord = remember(viewModel) { { viewModel.stopInlineCanvasVoiceRecording() } }
                                )
                            }

                            composable(ROUTE_MONK) {
                                MonkModeScreen(
                                    workspaceSnapshot = workspaceSnapshot,
                                    permissionsStatus = monkPermissions,
                                    launchableApps = launchableApps,
                                    detoxState = detoxState,
                                    onStartDetox = remember(viewModel) {
                                        { hours, grey, suppress, whitelist ->
                                            viewModel.startDigitalDetox(hours, grey, suppress, whitelist)
                                        }
                                    },
                                    onEmergencyUnlockDetox = remember(viewModel) {
                                        { viewModel.endDigitalDetoxEmergency() }
                                    },
                                    onRefreshMonkState = remember(viewModel, context) { { viewModel.refreshMonkState(context) } },
                                    onStartFocusSession = remember(viewModel) { { label, mins -> viewModel.startFocusSession(label, mins) } },
                                    onCompleteFocusSession = remember(viewModel) { { emergency -> viewModel.completeFocusSession(emergency) } },
                                    onSaveBlockerRule = remember(viewModel) {
                                        { title, mode, pkgs, startMin, endMin, daysCsv, delayMin, activeDur, preMin ->
                                            viewModel.saveBlockerRule(
                                                title = title,
                                                mode = mode,
                                                packages = pkgs,
                                                startMinuteOfDay = startMin,
                                                endMinuteOfDay = endMin,
                                                daysOfWeekCsv = daysCsv,
                                                delayDurationMinutes = delayMin,
                                                activeDurationMinutes = activeDur,
                                                preReminderMinutes = preMin
                                            )
                                        }
                                    },
                                    onToggleBlockerRule = remember(viewModel) { { rule -> viewModel.toggleBlockerRule(rule) } },
                                    onDeleteBlockerRule = remember(viewModel) { { id -> viewModel.deleteBlockerRule(id) } },
                                    onBack = remember(navController) { { navController.popBackStack() } }
                                )
                            }

                            composable(ROUTE_STARTUP_OS) {
                                StartupOsScreen(
                                    projects = startupProjects,
                                    phaseItems = startupPhaseItems,
                                    sprintTasks = startupSprintTasks,
                                    isGeneratingBlueprint = isGeneratingStartupPlan,
                                    aiError = aiStartupError,
                                    onCreateProject = remember(viewModel) {
                                        { name, tagline, persona, stack, stage, onCreated ->
                                            viewModel.createStartupProject(name, tagline, persona, stack, stage, onCreated = onCreated)
                                        }
                                    },
                                    onUpdateStage = remember(viewModel) { { id, st -> viewModel.updateStartupStage(id, st) } },
                                    onTogglePhaseItem = remember(viewModel) { { viewModel.toggleStartupPhaseItem(it) } },
                                    onAddCustomPhaseItem = remember(viewModel) {
                                        { pid, pNum, title, notes, url ->
                                            viewModel.addCustomStartupPhaseItem(pid, pNum, title, notes, url)
                                        }
                                    },
                                    onDeletePhaseItem = remember(viewModel) { { viewModel.deleteStartupPhaseItem(it) } },
                                    onSaveSprintTask = remember(viewModel) {
                                        { pid, tid, title, desc, col, pri, tag ->
                                            viewModel.saveStartupSprintTask(pid, tid, title, desc, col, pri, tag)
                                        }
                                    },
                                    onMoveSprintTaskColumn = remember(viewModel) { { t, col -> viewModel.moveSprintTaskStatus(t, col) } },
                                    onDeleteSprintTask = remember(viewModel) { { viewModel.deleteStartupSprintTask(it) } },
                                    onDeleteProject = remember(viewModel) { { viewModel.deleteStartupProject(it) } },
                                    onGenerateAiBlueprint = remember(viewModel) { { viewModel.generateStartupBlueprint(it) } },
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    financeTransactions = financeTransactions,
                                    financeSectors = financeSectors,
                                    teams = teams,
                                    activeTeamMembers = activeTeamMembers,
                                    onSelectTeamForShare = remember(viewModel) { { viewModel.selectActiveTeam(it) } },
                                    onUpdateProjectType = remember(viewModel) { { pid, pType -> viewModel.updateStartupProjectType(pid, pType) } },
                                    onSaveCrmDeals = remember(viewModel) { { pid, deals -> viewModel.saveBusinessCrmDeals(pid, deals) } },
                                    onSaveOkrs = remember(viewModel) { { pid, okrs -> viewModel.saveBusinessOkrs(pid, okrs) } },
                                    onSaveSopDocs = remember(viewModel) { { pid, docs -> viewModel.saveBusinessSopDocs(pid, docs) } },
                                    onShareWorkflowToTeam = remember(viewModel) {
                                        { pid, tid, tagged, incTasks, incSop, headline, onDone ->
                                            viewModel.shareBusinessWorkflowToTeam(
                                                startupId = pid,
                                                teamId = tid,
                                                taggedMembers = tagged,
                                                includeRoadmapTasks = incTasks,
                                                includeSopPage = incSop,
                                                customHeadline = headline,
                                                onDone = onDone
                                            )
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_MONEY_LEDGER) {
                                MoneyLedgerScreen(
                                    sectors = financeSectors,
                                    transactions = financeTransactions,
                                    recentIconKeys = recentFinanceIconKeys,
                                    txIconOverrides = financeTxIconOverrides,
                                    onRecordRecentIcon = remember(viewModel) { { viewModel.recordRecentFinanceIcon(it) } },
                                    onCreateSector = remember(viewModel) {
                                        { name, iconKey, colorHex, affinity ->
                                            viewModel.createFinanceSector(name, iconKey, colorHex, affinity)
                                        }
                                    },
                                    onDeleteSector = remember(viewModel) { { viewModel.deleteFinanceSector(it) } },
                                    onLogTransaction = remember(viewModel) {
                                        { type, cents, sector, method, notes, millis, iconKey ->
                                            viewModel.logFinanceTransaction(type, cents, sector, method, notes, millis, iconKey)
                                        }
                                    },
                                    onDeleteTransaction = remember(viewModel) { { viewModel.deleteFinanceTransaction(it) } },
                                    onBack = remember(navController) { { navController.popBackStack() } }
                                )
                            }

                            composable(ROUTE_TEAM_COLLAB) {
                                val currentUid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
                                TeamCollabScreen(
                                    isSignedIn = isSignedIn,
                                    currentUserUid = currentUid,
                                    teams = teams,
                                    activeTeamId = activeTeamId,
                                    members = activeTeamMembers,
                                    tasks = activeTeamTasks,
                                    docs = activeTeamDocs,
                                    notifications = activeTeamNotifications,
                                    latestAlertBanner = latestTeamAlertBanner,
                                    invitePreview = inviteLookupPreview,
                                    isBusy = teamBusy,
                                    teamError = teamError,
                                    onGoogleSignInClick = launchGoogleSignIn,
                                    onSelectTeam = remember(viewModel) { { viewModel.selectActiveTeam(it) } },
                                    onCreateTeam = remember(viewModel) {
                                        { name, desc, code, onDone ->
                                            viewModel.createTeam(name, desc, code, onDone = onDone)
                                        }
                                    },
                                    onLookupInviteCode = remember(viewModel) { { viewModel.lookupInviteCode(it) } },
                                    onJoinTeamByCode = remember(viewModel) {
                                        { code, onDone -> viewModel.joinTeamByInviteCode(code, onDone) }
                                    },
                                    onExpandTeamLimit = remember(viewModel) { { viewModel.expandTeamLimit(it) } },
                                    onUpdateMemberProfile = remember(viewModel) {
                                        { tid, member, roleTag, customStatus, newRole ->
                                            viewModel.updateTeamMemberProfile(tid, member, roleTag, customStatus, newRole)
                                        }
                                    },
                                    onSaveScheduleTask = remember(viewModel) {
                                        { tid, title, desc, day, dateLabel, startTime, endTime, assigned, priority, status, existing ->
                                            viewModel.saveSharedTask(
                                                teamId = tid,
                                                title = title,
                                                description = desc,
                                                priority = priority,
                                                dayOfWeek = day,
                                                dateLabel = dateLabel,
                                                startTime = startTime,
                                                endTime = endTime,
                                                assignedMembers = assigned,
                                                status = status,
                                                existingTask = existing
                                            )
                                        }
                                    },
                                    onCycleTaskStatus = remember(viewModel) { { viewModel.cycleSharedTaskStatus(it) } },
                                    onSetTaskStatus = remember(viewModel) { { task, st -> viewModel.setSharedTaskStatus(task, st) } },
                                    onDeleteSharedTask = remember(viewModel) { { viewModel.deleteSharedTask(it) } },
                                    onSaveSharedNote = remember(viewModel) {
                                        { tid, title, content, url, blocksJson, tagsCsv, existing ->
                                            viewModel.saveSharedDoc(
                                                teamId = tid,
                                                title = title,
                                                content = content,
                                                resourceUrl = url,
                                                blocksJson = blocksJson,
                                                tagsCsv = tagsCsv,
                                                existingDoc = existing
                                            )
                                        }
                                    },
                                    onDeleteSharedDoc = remember(viewModel) { { viewModel.deleteSharedDoc(it) } },
                                    onSendTeamAlert = remember(viewModel) {
                                        { tid, title, msg, tagged ->
                                            viewModel.sendTeamAlert(tid, title, msg, tagged)
                                        }
                                    },
                                    onDismissAlertBanner = remember(viewModel) { { viewModel.dismissTeamAlertBanner() } },
                                    onClearTeamError = remember(viewModel) { { viewModel.clearTeamError() } },
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    activities = activeTeamActivities,
                                    emailInvites = activeTeamEmailInvites,
                                    incomingEmailInvites = incomingEmailInvites,
                                    onInviteByEmail = remember(viewModel) {
                                        { team, email, role, roleTag, onDone ->
                                            viewModel.inviteTeamMemberByEmail(team, email, role, roleTag, onDone)
                                        }
                                    },
                                    onAcceptEmailInvite = remember(viewModel) { { viewModel.acceptEmailInvite(it) } },
                                    onRevokeEmailInvite = remember(viewModel) { { viewModel.revokeEmailInvite(it) } },
                                    onRemoveMember = remember(viewModel) { { tid, member -> viewModel.removeTeamMember(tid, member) } },
                                    onTogglePinSharedDoc = remember(viewModel) { { viewModel.togglePinSharedDoc(it) } },
                                    onVotePoll = remember(viewModel) { { tid, nid, optIdx -> viewModel.voteOnTeamPoll(tid, nid, optIdx) } },
                                    onSaveTaskExtended = remember(viewModel) {
                                        { tid, title, desc, day, dateLabel, startTime, endTime, assigned, priority, status, deadlineMillis, driveUrl, existing ->
                                            viewModel.saveSharedTask(
                                                teamId = tid,
                                                title = title,
                                                description = desc,
                                                priority = priority,
                                                dayOfWeek = day,
                                                dateLabel = dateLabel,
                                                startTime = startTime,
                                                endTime = endTime,
                                                assignedMembers = assigned,
                                                status = status,
                                                deadlineMillis = deadlineMillis,
                                                linkedDriveUrl = driveUrl,
                                                existingTask = existing
                                            )
                                        }
                                    },
                                    onSavePageExtended = remember(viewModel) {
                                        { tid, title, content, url, blocksJson, tagsCsv, parentId, icon, isPinned, existing ->
                                            viewModel.saveSharedDoc(
                                                teamId = tid,
                                                title = title,
                                                content = content,
                                                resourceUrl = url,
                                                blocksJson = blocksJson,
                                                tagsCsv = tagsCsv,
                                                parentId = parentId,
                                                icon = icon,
                                                isPinned = isPinned,
                                                existingDoc = existing
                                            )
                                        }
                                    },
                                    onSendAlertExtended = remember(viewModel) {
                                        { tid, title, msg, tagged, noticeType, pollOptionsJson ->
                                            viewModel.sendTeamAlert(
                                                teamId = tid,
                                                title = title,
                                                message = msg,
                                                taggedMembers = tagged,
                                                noticeType = noticeType,
                                                pollOptionsJson = pollOptionsJson
                                            )
                                        }
                                    },
                                    playbackState = voicePlaybackState,
                                    onPolishPageWithAi = viewModel::polishCanvasPageWithAi,
                                    onLogClientPaymentToLedger = remember(viewModel) {
                                        { client, project, cents -> viewModel.recordCanvasClientPaymentToLedger(client, project, cents) }
                                    },
                                    onScheduleInlineReminder = remember(viewModel) {
                                        { tid, rem ->
                                            viewModel.createCanvasInlineReminder(
                                                title = rem.title,
                                                triggerAtMillis = rem.triggerAtMillis,
                                                ringtoneTitle = rem.ringtoneTitle,
                                                noteId = null,
                                                teamId = tid,
                                                taggedMemberName = rem.taggedMemberName.ifBlank { null },
                                                taggedMemberUid = rem.taggedMemberUid.ifBlank { null }
                                            )
                                        }
                                    },
                                    onSyncScheduleBlock = remember(viewModel) {
                                        { tid, sched, teamMembers -> viewModel.syncCanvasScheduleBlock(tid, sched, teamMembers) }
                                    },
                                    onLogFocusTimerSeconds = remember(viewModel) {
                                        { lbl, secs -> viewModel.logCanvasFocusTimerSession(lbl, secs) }
                                    },
                                    onStartInlineVoiceRecord = remember(viewModel) { { viewModel.startInlineCanvasVoiceRecording() } },
                                    onStopInlineVoiceRecord = remember(viewModel) { { viewModel.stopInlineCanvasVoiceRecording() } },
                                    onToggleVoicePlayback = remember(viewModel) { { uri -> viewModel.toggleVoicePlayback(uri) } },
                                    onSeekVoicePlayback = remember(viewModel) { { uri, pos -> viewModel.seekVoicePlayback(uri, pos) } }
                                )
                            }

                            composable(ROUTE_LIBRARY) {
                                val gamificationSummary = remember(
                                    workspaceSnapshot.habits,
                                    workspaceSnapshot.habitCompletions
                                ) {
                                    HabitGamificationEngine.compute(
                                        workspaceSnapshot.habits,
                                        workspaceSnapshot.habitCompletions
                                    )
                                }
                                val ledgerSummary = remember(financeTransactions, financeSectors) {
                                    OsRepository.computeLedgerSummary(financeTransactions, financeSectors)
                                }
                                val isDetoxNow = detoxState.isRunningAt()
                                val launcherMetrics = remember(
                                    isDetoxNow,
                                    workspaceSnapshot.blockerRules,
                                    gamificationSummary,
                                    workspaceSnapshot.notes.size,
                                    startupProjects.size,
                                    ledgerSummary.netBalanceCents,
                                    teams.size,
                                    activeTeamNotifications.size
                                ) {
                                    LauncherMetrics(
                                        monkSummary = if (isDetoxNow) {
                                            "Detox Locked"
                                        } else {
                                            val enabledRules = workspaceSnapshot.blockerRules.count { it.isEnabled }
                                            if (enabledRules > 0) "$enabledRules Shield(s) Armed" else "App Shield • Standby"
                                        },
                                        isDetoxActive = isDetoxNow,
                                        workspaceSummary = "LV ${gamificationSummary.level} • ${workspaceSnapshot.notes.size} Notes",
                                        startupSummary = if (startupProjects.isEmpty()) "Launch Ventures" else "${startupProjects.size} Active Venture(s)",
                                        moneySummary = "${OsRepository.formatCurrencyCents(ledgerSummary.netBalanceCents)} Net",
                                        teamSummary = if (teams.isEmpty()) "Live Sync Workspace" else "${teams.size} Team(s) • ${activeTeamNotifications.size} Alerts"
                                    )
                                }

                                LibraryHubScreen(
                                    snapshot = snapshot,
                                    initialSection = hubInitialSection,
                                    themeMode = themeMode,
                                    accentPreset = accentPreset,
                                    vaultPin = vaultPin,
                                    isSignedIn = isSignedIn,
                                    userDisplayName = userDisplayName,
                                    userEmail = userEmail,
                                    isOnline = isOnline,
                                    syncBadgeState = syncBadge,
                                    isSyncing = isSyncing,
                                    syncMessage = syncMessage,
                                    isAiBusy = isAiBusy,
                                    onSelectThemeMode = onSetThemeMode,
                                    onSelectAccentPreset = remember(viewModel) { { viewModel.setAccentPreset(it) } },
                                    onUpdateProfileName = remember(viewModel) { { viewModel.updateUserProfileName(it) } },
                                    onSecretAdminTrigger = remember(viewModel) {
                                        {
                                            viewModel.clearAdminAuthError()
                                            showAdminPasswordModal = true
                                        }
                                    },
                                    onOpenItem = onOpenItemCallback,
                                    onToggleFavorite = onToggleFavoriteItem,
                                    onSendItemToDevice = openSendItemToDevice,
                                    onOpenDeviceSyncModal = onOpenEmptyDeviceSyncModal,
                                    onOrganizeInboxClick = onOrganizeInboxCallback,
                                    onOpenAiAssistant = onOpenAiAssistantCallback,
                                    onPickLocalFileClick = onPickLocalFileInboxCallback,
                                    onPickPhotoVideoClick = onUploadMediaInboxCallback,
                                    onConnectFolderTreeClick = remember(connectFolderLauncher) { { connectFolderLauncher.launch(null) } },
                                    onRefreshFileHealthClick = remember(viewModel) { { viewModel.refreshFileHealth() } },
                                    onGoogleSignInClick = launchGoogleSignIn,
                                    onSignOutClick = onSignOutToGuest,
                                    onSyncNowClick = remember(viewModel) { { viewModel.triggerSyncNow() } },
                                    onUpdateSettings = remember(viewModel) { { transform -> viewModel.updateSettings(transform) } },
                                    onExportBackupClick = remember(exportBackupLauncher) {
                                        { exportBackupLauncher.launch("personal_ai_library_backup.json") }
                                    },
                                    onImportBackupClick = remember(importBackupLauncher) {
                                        { importBackupLauncher.launch(arrayOf("application/json", "*/*")) }
                                    },
                                    onClearDiagnosticsClick = remember(viewModel) { { viewModel.clearDiagnostics() } },
                                    onEditItem = onEditItemCallback,
                                    onDeleteItem = onDeleteItemCallback,
                                    launcherMetrics = launcherMetrics,
                                    onOpenMonkMode = remember(navController) {
                                        { navController.navigate(ROUTE_MONK) { launchSingleTop = true } }
                                    },
                                    onOpenWorkspace = remember(navController) {
                                        { navController.navigate(ROUTE_WORKSPACE) { launchSingleTop = true } }
                                    },
                                    onOpenStartupOs = remember(navController) {
                                        { navController.navigate(ROUTE_STARTUP_OS) { launchSingleTop = true } }
                                    },
                                    onOpenMoneyLedger = remember(navController) {
                                        { navController.navigate(ROUTE_MONEY_LEDGER) { launchSingleTop = true } }
                                    },
                                    onOpenTeamCollab = remember(navController) {
                                        { navController.navigate(ROUTE_TEAM_COLLAB) { launchSingleTop = true } }
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_DETAIL,
                                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                            ) { entry ->
                                val itemId = entry.arguments?.getString("itemId").orEmpty()
                                val item by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
                                val notes by viewModel.observeNotesForItem(itemId).collectAsStateWithLifecycle(initialValue = emptyList())
                                val itemReviews = remember(reviews, itemId) {
                                    reviews.filter { it.itemId == itemId }
                                }

                                val itemLinkedReminders = remember(workspaceSnapshot.reminders, itemId) {
                                    workspaceSnapshot.reminders.filter { it.targetItemId == itemId }
                                }
                                ItemDetailScreen(
                                    item = item,
                                    notes = notes,
                                    itemReviews = itemReviews,
                                    snapshot = snapshot,
                                    isOnline = isOnline,
                                    isAiBusy = isAiBusy,
                                    aiSuggestion = itemSuggestion,
                                    aiError = aiError,
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    onToggleFavorite = onToggleFavoriteItem,
                                    onSendToDevice = openSendItemToDevice,
                                    onWriteReviewClick = remember {
                                        { targetId: String ->
                                            reviewTargetItemId = targetId
                                            showWriteReviewModal = true
                                        }
                                    },
                                    onToggleReviewHelpful = onToggleReviewHelpful,
                                    onSoftDelete = remember(viewModel) { { viewModel.softDeleteItem(it) } },
                                    onAddToFolder = remember(viewModel) { { id, fId -> viewModel.addItemToFolder(id, fId) } },
                                    onRemoveFromFolder = remember(viewModel) { { id, fId -> viewModel.removeItemFromFolder(id, fId) } },
                                    onAddTag = remember(viewModel) { { id, tag, origin -> viewModel.addTagToItem(id, tag, origin) } },
                                    onRemoveTag = remember(viewModel) { { id, tId -> viewModel.removeTagFromItem(id, tId) } },
                                    onSaveNote = remember(viewModel) { { id, body, existingId -> viewModel.saveNote(id, body, existingId) } },
                                    onDeleteNote = remember(viewModel) { { nId, id -> viewModel.deleteNote(nId, id) } },
                                    onEnrichMetadata = remember(viewModel) { { viewModel.enrichItemNow(it) } },
                                    onSuggestAiTags = remember(viewModel) { { viewModel.suggestTagsForItem(it) } },
                                    onRegrantLocalAccess = onPickLocalFileInboxCallback,
                                    onUpdateItem = remember(viewModel) {
                                        { id, title, url, description ->
                                            viewModel.editItemDetails(id, title, url, description)
                                        }
                                    },
                                    linkedReminders = itemLinkedReminders,
                                    onCreateReminderClick = remember {
                                        { target ->
                                            reminderSheetRequest = Triple("Review: ${target.title}", target.id, null)
                                        }
                                    },
                                    onToggleReminder = remember(viewModel) { { rem -> viewModel.toggleReminderEnabled(rem) } },
                                    onDeleteReminder = remember(viewModel) { { id -> viewModel.deleteReminder(id) } }
                                )
                            }

                            composable(ROUTE_AI_ASSISTANT) {
                                val engineLabel = if (adminVaultConfig.aiProvider == com.example.data.local.AdminVaultConfig.PROVIDER_OPENROUTER) {
                                    "OpenRouter Free (${adminVaultConfig.openRouterModel}) • Grounded in your library"
                                } else {
                                    "Online Gemini • Grounded in your saved items"
                                }
                                val missingKeyHint = if (
                                    adminVaultConfig.aiProvider == com.example.data.local.AdminVaultConfig.PROVIDER_OPENROUTER &&
                                    adminVaultConfig.openRouterApiKey.isBlank()
                                ) {
                                    "OpenRouter API key is not configured yet. Add it in Admin API Vault or AI Studio Secrets."
                                } else null
                                AiAssistantScreen(
                                    messages = chatMessages,
                                    isOnline = isOnline,
                                    aiEnabled = snapshot.settings.aiEnabled,
                                    isAiBusy = isAiBusy,
                                    activeEngineLabel = engineLabel,
                                    missingKeyWarning = missingKeyHint,
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    onSendQuestion = remember(viewModel) { { viewModel.sendAssistantMessage(it) } }
                                )
                            }

                            composable(
                                route = ROUTE_AI_ORG_PREVIEW,
                                arguments = listOf(navArgument("planId") { type = NavType.StringType })
                            ) { entry ->
                                val planId = entry.arguments?.getString("planId").orEmpty()
                                val plan = remember(snapshot.plans, planId) {
                                    snapshot.plans.find { it.id == planId }
                                }
                                val operations by viewModel.observeOperationsForPlan(planId)
                                    .collectAsStateWithLifecycle(initialValue = emptyList())

                                AiOrganizationPreviewScreen(
                                    plan = plan,
                                    operations = operations,
                                    aiError = aiError,
                                    onBack = remember(navController) { { navController.popBackStack() } },
                                    onToggleOperation = remember(viewModel) {
                                        { opId, selected -> viewModel.toggleOperationSelection(opId, selected) }
                                    },
                                    onApprovePlan = remember(viewModel, navController, planId) {
                                        {
                                            viewModel.approveOrganizationPlan(planId) {
                                                navController.popBackStack()
                                            }
                                        }
                                    },
                                    onRejectPlan = remember(viewModel, navController, planId) {
                                        {
                                            viewModel.rejectOrganizationPlan(planId) {
                                                navController.popBackStack()
                                            }
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_ADMIN_DASHBOARD) {
                                if (isAdminUnlocked) {
                                    AdminDashboardScreen(
                                        config = adminVaultConfig,
                                        isSignedIn = isSignedIn,
                                        isOnline = isOnline,
                                        userEmail = userEmail,
                                        isTesting = isAdminTesting,
                                        testStatus = adminTestStatus,
                                        isCloudSyncing = isAdminCloudSyncing,
                                        passcodeStatus = adminPasscodeStatus,
                                        freeModelOptions = freeOpenRouterModels,
                                        isLoadingFreeModels = isLoadingFreeModels,
                                        onLoadFreeModels = remember(viewModel) { { viewModel.loadLiveFreeOpenRouterModels() } },
                                        onBackAndLock = remember(viewModel, navController) {
                                            {
                                                viewModel.lockAdminDashboard()
                                                navController.popBackStack()
                                            }
                                        },
                                        onGoogleSignInClick = launchGoogleSignIn,
                                        onRefreshCloudConfig = remember(viewModel) { { viewModel.refreshAdminConfigFromCloud() } },
                                        onSaveConfig = remember(viewModel) { { viewModel.saveAdminConfig(it) } },
                                        onTestConnection = remember(viewModel) {
                                            { provider, key, model ->
                                                viewModel.testAdminAiConnection(provider, key, model)
                                            }
                                        },
                                        onChangePasscode = remember(viewModel) { { viewModel.changeAdminPasscode(it) } }
                                    )
                                } else {
                                    LaunchedEffect(Unit) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }

                        if (captureDraft.isOpen) {
                            UniversalCaptureSheet(
                                draft = captureDraft,
                                folders = snapshot.folders,
                                onDismiss = remember(viewModel) { { viewModel.closeCaptureSheet() } },
                                onPreviewUrl = remember(viewModel) { { raw -> viewModel.previewUrlForCapture(raw) } },
                                onSaveWebOrNote = remember(viewModel) {
                                    { raw, title, description, folderId, tags, note, preloadedThumb, allowDup ->
                                        viewModel.saveCapture(
                                            rawInput = raw,
                                            title = title,
                                            folderId = folderId,
                                            tagsCsv = tags,
                                            note = note,
                                            description = description,
                                            preloadedThumbnailUrl = preloadedThumb,
                                            allowDuplicate = allowDup
                                        )
                                    }
                                },
                                onOpenExistingDuplicate = remember(viewModel, navController) {
                                    { existingId ->
                                        viewModel.closeCaptureSheet()
                                        navController.navigate("detail/$existingId")
                                    }
                                },
                                onAddDuplicateToFolder = remember(viewModel) {
                                    { existingId, folderId ->
                                        viewModel.addExistingDuplicateToFolder(existingId, folderId)
                                    }
                                },
                                onPickLocalFile = remember(pickFileLauncher, captureDraft.targetCollectionId) {
                                    { folderId ->
                                        pendingFileTargetFolderId = folderId
                                        pendingFileTargetCollectionId = captureDraft.targetCollectionId
                                        pickFileLauncher.launch(arrayOf("*/*"))
                                    }
                                },
                                onPickPhotoVideo = remember(pickMediaLauncher, captureDraft.targetCollectionId) {
                                    { folderId ->
                                        pendingFileTargetFolderId = folderId
                                        pendingFileTargetCollectionId = captureDraft.targetCollectionId
                                        pickMediaLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                },
                                onConnectFolderTree = remember(connectFolderLauncher) { { connectFolderLauncher.launch(null) } },
                                onSaveSharedUris = remember(viewModel, captureDraft.targetCollectionId) {
                                    { uris, folderId -> viewModel.captureMultipleUris(uris, folderId, captureDraft.targetCollectionId) }
                                }
                            )
                        }

                        reminderSheetRequest?.let { (initTitle, targetItemId, targetNoteId) ->
                            SmartReminderSheet(
                                initialTitle = initTitle,
                                initialTargetItemId = targetItemId,
                                initialTargetNoteId = targetNoteId,
                                availableItems = snapshot.items,
                                onDismiss = { reminderSheetRequest = null },
                                onSaveReminder = { mode, title, notes, priority, triggerAt, recType, intervalMin, rUri, rTitle, cloudSync, tItem, tNote ->
                                    viewModel.saveSmartReminder(
                                        mode = mode,
                                        title = title,
                                        notes = notes,
                                        priority = priority,
                                        triggerAtMillis = triggerAt,
                                        recurrenceType = recType,
                                        intervalMinutes = intervalMin,
                                        ringtoneUri = rUri,
                                        ringtoneTitle = rTitle,
                                        cloudSync = cloudSync,
                                        targetItemId = tItem,
                                        targetNoteId = tNote
                                    )
                                }
                            )
                        }

                        if (showDeviceSyncModal) {
                            SyncAndSendDeviceModal(
                                isSignedIn = isSignedIn,
                                isOnline = isOnline,
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                currentDeviceId = currentDeviceId,
                                currentDeviceName = viewModel.currentDeviceName,
                                syncBadgeState = syncBadge,
                                pendingSyncCount = snapshot.pendingSyncCount,
                                savedAccounts = savedAccounts,
                                connectedDevices = connectedDevices,
                                transfers = deviceTransfers,
                                initialTitle = deviceModalPrefillTitle,
                                initialPayload = deviceModalPrefillPayload,
                                onDismiss = remember { { showDeviceSyncModal = false } },
                                onGoogleSignInOrSwitch = launchGoogleSignIn,
                                onSignOutToGuest = onSignOutToGuest,
                                onSyncNow = remember(viewModel) { { viewModel.triggerSyncNow() } },
                                onSendToDevice = remember(viewModel) {
                                    { title, payload, type, targetId ->
                                        viewModel.sendToDevice(
                                            title = title,
                                            payloadText = payload,
                                            payloadType = type,
                                            targetDeviceId = targetId
                                        )
                                    }
                                },
                                onAcceptTransfer = remember(viewModel) { { viewModel.acceptIncomingTransfer(it) } },
                                onDismissTransfer = remember(viewModel) { { viewModel.dismissIncomingTransfer(it) } }
                            )
                        }

                        if (showWriteReviewModal) {
                            WriteReviewModal(
                                onDismiss = remember { { showWriteReviewModal = false } },
                                onSubmitReview = remember(viewModel, reviewTargetItemId) {
                                    { rating, body ->
                                        viewModel.submitReview(
                                            itemId = reviewTargetItemId,
                                            rating = rating,
                                            body = body,
                                            onDone = { showWriteReviewModal = false }
                                        )
                                    }
                                }
                            )
                        }

                        if (showAdminPasswordModal) {
                            SecretAdminPasswordModal(
                                hasExistingPasscode = adminVaultConfig.hasAdminPasscode,
                                authError = adminAuthError,
                                onDismiss = remember(viewModel) {
                                    {
                                        viewModel.clearAdminAuthError()
                                        showAdminPasswordModal = false
                                    }
                                },
                                onSubmitPasscode = remember(viewModel, navController) {
                                    { passcode ->
                                        viewModel.submitAdminPassword(passcode) {
                                            showAdminPasswordModal = false
                                            navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (showProfileModal) {
                            ProfileManagementModal(
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                profileAvatar = profileAvatar,
                                pendingPickedAvatarUri = pendingPickedAvatarUri,
                                isSignedIn = isSignedIn,
                                themeMode = themeMode,
                                accentPreset = accentPreset,
                                syncBadgeState = syncBadge,
                                settings = snapshot.settings,
                                onDismiss = {
                                    pendingPickedAvatarUri = null
                                    showProfileModal = false
                                },
                                onPickAvatarPhotoClick = {
                                    pickAvatarLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearPendingPickedAvatar = { pendingPickedAvatarUri = null },
                                onSaveProfile = { name, email, avatarVal, pickedUri ->
                                    viewModel.updateUserProfileDetails(
                                        newName = name,
                                        newEmail = email,
                                        avatarValue = avatarVal,
                                        selectedImageUri = pickedUri,
                                        onDone = {
                                            pendingPickedAvatarUri = null
                                            showProfileModal = false
                                        }
                                    )
                                },
                                onSelectThemeMode = onSetThemeMode,
                                onSelectAccentPreset = remember(viewModel) { { viewModel.setAccentPreset(it) } },
                                onUpdateSettings = remember(viewModel) { { transform -> viewModel.updateSettings(transform) } },
                                onGoogleSignInClick = launchGoogleSignIn,
                                onSignOutClick = {
                                    showProfileModal = false
                                    onSignOutToGuest()
                                },
                                onSyncNowClick = remember(viewModel) { { viewModel.triggerSyncNow() } },
                                onOpenDeviceSyncModal = onOpenEmptyDeviceSyncModal,
                                onOpenFullSettings = onOpenSyncSettingsCallback
                            )
                        }

                        editTargetItem?.let { target ->
                            EditResourceDialog(
                                item = target,
                                onDismiss = { editTargetItem = null },
                                onSave = { title, url, description ->
                                    viewModel.editItemDetails(
                                        itemId = target.id,
                                        title = title,
                                        url = url,
                                        description = description,
                                        onDone = { editTargetItem = null }
                                    )
                                }
                            )
                        }

                        pendingDeleteItem?.let { targetItem ->
                            LiquidDeleteConfirmationDialog(
                                title = "Delete Resource?",
                                message = "Move \"${targetItem.title}\" to trash?",
                                onConfirm = {
                                    viewModel.softDeleteItem(targetItem.id)
                                    pendingDeleteItem = null
                                },
                                onDismiss = { pendingDeleteItem = null }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showLaunchSplash,
            enter = fadeIn(animationSpec = tween(240)),
            exit = fadeOut(animationSpec = tween(320))
        ) {
            AppLaunchSplashOverlay()
        }
        }
    }
}

@Composable
private fun AppLaunchSplashOverlay() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.90f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "launchSplashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "launchSplashAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1012),
                        Color(0xFF0D0D0F)
                    )
                )
            )
            .testTag("app_launch_splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AppLogo(
                size = 112.dp,
                cornerRadius = 28.dp,
                testTag = "app_launch_splash_logo"
            )
            Text(
                text = "Personal AI Library",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                ),
                color = Color(0xFFF3EFEA)
            )
        }
    }
}
