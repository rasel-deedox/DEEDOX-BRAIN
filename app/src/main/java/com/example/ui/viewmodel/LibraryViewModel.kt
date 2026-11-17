package com.example.ui.viewmodel

import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiPagePolishMode
import com.example.data.ai.AiSearchResponse
import com.example.data.ai.AiTagSummarySuggestion
import com.example.data.cloud.CloudDeviceSession
import com.example.data.cloud.CloudDeviceTransfer
import com.example.data.cloud.CloudOsSync
import com.example.data.cloud.CloudTeamInvitePreview
import com.example.data.cloud.SupabaseSignUpOutcome
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.AdminVaultConfig
import com.example.data.local.AiJobEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.BlockerRuleEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CollectionItemCrossRef
import com.example.data.local.ConnectedSourceEntity
import com.example.data.local.DetoxSessionState
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.FileSourceEntity
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.FolderEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.ItemEntity
import com.example.data.local.ItemFolderCrossRef
import com.example.data.local.ItemTagCrossRef
import com.example.data.local.LibraryReview
import com.example.data.local.LocalSearchHit
import com.example.data.local.NoteEntity
import com.example.data.local.OnboardingProfile
import com.example.data.local.OrganizationOperationEntity
import com.example.data.local.OrganizationPlanEntity
import com.example.data.local.SavedAccountRecord
import com.example.data.local.SmartReminderEntity
import com.example.data.local.StartupPhaseItemEntity
import com.example.data.local.StartupProjectEntity
import com.example.data.local.StartupSprintTaskEntity
import com.example.data.local.SupabaseSession
import com.example.data.local.TagEntity
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamEmailInviteEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.local.TeamWorkspaceEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.local.WorkspaceTaskEntity
import com.example.data.media.VoiceAudioEngine
import com.example.data.media.VoicePlaybackState
import com.example.data.media.VoiceRecorderState
import com.example.data.network.EnrichedMetadata
import com.example.data.repository.BusinessCrmDeal
import com.example.data.repository.BusinessOkrItem
import com.example.data.repository.BusinessSopDoc
import com.example.data.repository.CaptureOutcome
import com.example.data.repository.LibraryRepository
import com.example.data.repository.OsRepository
import com.example.data.repository.WorkspaceBlock
import com.example.data.repository.WorkspaceRepository
import com.example.data.util.UrlNormalizer
import com.example.monk.LaunchableAppInfo
import com.example.monk.MonkBlockerEngine
import com.example.monk.MonkPermissionsStatus
import com.example.reminders.ReminderScheduler
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@Immutable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isError: Boolean = false
)

@Immutable
data class CaptureDraft(
    val isOpen: Boolean = false,
    val initialText: String = "",
    val initialTitle: String = "",
    val sharedUris: List<Uri> = emptyList(),
    val targetFolderId: String = AppDatabase.INBOX_FOLDER_ID,
    val targetCollectionId: String? = null,
    val duplicateState: CaptureOutcome.DuplicateDetected? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val previewUrl: String? = null,
    val isPreviewLoading: Boolean = false,
    val previewMetadata: EnrichedMetadata? = null,
    val previewError: String? = null
)

@Immutable
data class WorkspaceSnapshot(
    val notes: List<WorkspaceNoteEntity> = emptyList(),
    val tasks: List<WorkspaceTaskEntity> = emptyList(),
    val reminders: List<SmartReminderEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitCompletions: List<HabitCompletionEntity> = emptyList(),
    val blockerRules: List<BlockerRuleEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList()
)

@Immutable
data class SearchUiState(
    val query: String = "",
    val isOnlineAiMode: Boolean = false,
    val contentTypeFilter: String = "ALL",
    val folderFilter: String? = null,
    val favoritesOnly: Boolean = false,
    val localHits: List<LocalSearchHit> = emptyList(),
    val aiResponse: AiSearchResponse? = null,
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class LibrarySnapshot(
    val items: List<ItemEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val itemFolders: List<ItemFolderCrossRef> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val itemTags: List<ItemTagCrossRef> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val collectionItems: List<CollectionItemCrossRef> = emptyList(),
    val fileSources: List<FileSourceEntity> = emptyList(),
    val connectedSources: List<ConnectedSourceEntity> = emptyList(),
    val plans: List<OrganizationPlanEntity> = emptyList(),
    val pendingSyncCount: Int = 0,
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val diagnosticLogs: List<DiagnosticLogEntity> = emptyList(),
    val aiJobs: List<AiJobEntity> = emptyList()
)

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val workspaceRepository: WorkspaceRepository? = null,
    private val voiceAudioEngine: VoiceAudioEngine? = null,
    private val osRepository: OsRepository? = null,
    private val cloudOsSync: CloudOsSync? = null
) : ViewModel() {

    private val auth = Firebase.auth
    val supabaseSession: StateFlow<SupabaseSession?> = repository.supabaseSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private val _isGoogleSignedIn = MutableStateFlow(auth.currentUser != null)
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    val isSignedIn: StateFlow<Boolean> = combine(
        _isGoogleSignedIn,
        supabaseSession
    ) { googleSignedIn, supa ->
        googleSignedIn || supa != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = auth.currentUser != null
    )

    private val _userDisplayName = MutableStateFlow(auth.currentUser?.displayName ?: "Guest User")
    val userDisplayName: StateFlow<String> = combine(
        _userDisplayName,
        supabaseSession,
        repository.customDisplayName
    ) { authName, supa, customName ->
        customName?.takeIf { it.isNotBlank() }
            ?: supa?.displayName?.takeIf { it.isNotBlank() }
            ?: authName
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = auth.currentUser?.displayName ?: "Guest User"
    )

    private val _userEmail = MutableStateFlow(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = combine(
        _userEmail,
        supabaseSession,
        repository.customEmail
    ) { googleEmail, supa, custom ->
        custom?.takeIf { it.isNotBlank() }
            ?: googleEmail?.takeIf { it.isNotBlank() }
            ?: supa?.email?.takeIf { it.isNotBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = auth.currentUser?.email
    )

    private val _googlePhotoUrl = MutableStateFlow(auth.currentUser?.photoUrl?.toString())
    val profileAvatar: StateFlow<String> = combine(
        repository.profileAvatar,
        _googlePhotoUrl
    ) { storedAvatar, googlePhoto ->
        when {
            storedAvatar.startsWith("file:") || storedAvatar.startsWith("content:") || storedAvatar.startsWith("http") -> storedAvatar
            storedAvatar.startsWith("preset:") && storedAvatar != "preset:terracotta" -> storedAvatar
            !googlePhoto.isNullOrBlank() -> googlePhoto
            else -> storedAvatar
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = auth.currentUser?.photoUrl?.toString() ?: "preset:terracotta"
    )

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _supabaseAuthError = MutableStateFlow<String?>(null)
    val supabaseAuthError: StateFlow<String?> = _supabaseAuthError.asStateFlow()

    private val _supabaseAuthNotice = MutableStateFlow<String?>(null)
    val supabaseAuthNotice: StateFlow<String?> = _supabaseAuthNotice.asStateFlow()

    val onboardingProfile: StateFlow<OnboardingProfile> = repository.onboardingProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OnboardingProfile()
    )

    private val _isWelcomeSyncTransitioning = MutableStateFlow(false)
    val isWelcomeSyncTransitioning: StateFlow<Boolean> = _isWelcomeSyncTransitioning.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppThemeMode.NIGHT
    )

    val accentPreset: StateFlow<LiquidAccentPreset> = repository.accentPreset.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LiquidAccentPreset.CRIMSON_BRONZE
    )

    val recentFinanceIconKeys: StateFlow<List<String>> = repository.recentFinanceIconKeys.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val financeTxIconOverrides: StateFlow<Map<String, String>> = repository.financeTxIconOverrides.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyMap()
    )

    val adminVaultConfig: StateFlow<AdminVaultConfig> = repository.adminVaultConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminVaultConfig()
    )

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    private val _isAdminTesting = MutableStateFlow(false)
    val isAdminTesting: StateFlow<Boolean> = _isAdminTesting.asStateFlow()

    private val _adminTestStatus = MutableStateFlow<String?>(null)
    val adminTestStatus: StateFlow<String?> = _adminTestStatus.asStateFlow()

    private val _isAdminCloudSyncing = MutableStateFlow(false)
    val isAdminCloudSyncing: StateFlow<Boolean> = _isAdminCloudSyncing.asStateFlow()

    private val _adminPasscodeStatus = MutableStateFlow<String?>(null)
    val adminPasscodeStatus: StateFlow<String?> = _adminPasscodeStatus.asStateFlow()

    private val _freeOpenRouterModels = MutableStateFlow(AdminVaultConfig.FREE_OPENROUTER_PRESETS)
    val freeOpenRouterModels: StateFlow<List<Pair<String, String>>> = _freeOpenRouterModels.asStateFlow()

    private val _isLoadingFreeModels = MutableStateFlow(false)
    val isLoadingFreeModels: StateFlow<Boolean> = _isLoadingFreeModels.asStateFlow()

    val vaultPin: StateFlow<String?> = repository.vaultPin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    val isVaultLocked: StateFlow<Boolean> = repository.isVaultLocked.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )

    val savedAccounts: StateFlow<List<SavedAccountRecord>> = repository.savedAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val reviews: StateFlow<List<LibraryReview>> = repository.localReviews.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _connectedDevices = MutableStateFlow<List<CloudDeviceSession>>(emptyList())
    val connectedDevices: StateFlow<List<CloudDeviceSession>> = _connectedDevices.asStateFlow()

    private val _deviceTransfers = MutableStateFlow<List<CloudDeviceTransfer>>(emptyList())
    val deviceTransfers: StateFlow<List<CloudDeviceTransfer>> = _deviceTransfers.asStateFlow()

    private val _currentDeviceId = MutableStateFlow("")
    val currentDeviceId: StateFlow<String> = _currentDeviceId.asStateFlow()

    val currentDeviceName: String = buildString {
        val mfg = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        if (model.startsWith(mfg, ignoreCase = true) && model.isNotBlank()) {
            append(model.replaceFirstChar { it.uppercase() })
        } else {
            append("${mfg.replaceFirstChar { it.uppercase() }} $model".trim().ifBlank { "Android Device" })
        }
    }

    private var cloudListenersJob: Job? = null
    private var teamListListenerJob: Job? = null
    private var activeTeamDetailJob: Job? = null
    private var incomingEmailInvitesJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _isGoogleSignedIn.value = user != null
        _userDisplayName.value = user?.displayName ?: "Guest User"
        _userEmail.value = user?.email
        _googlePhotoUrl.value = user?.photoUrl?.toString()
        if (user != null) {
            viewModelScope.launch {
                repository.recordSignedInAccount(
                    uid = user.uid,
                    displayName = user.displayName ?: "Library User",
                    email = user.email.orEmpty()
                )
            }
            restartCloudObserversIfEligible(user.uid)
        } else {
            cloudListenersJob?.cancel()
            teamListListenerJob?.cancel()
            activeTeamDetailJob?.cancel()
            incomingEmailInvitesJob?.cancel()
            _connectedDevices.value = emptyList()
            _deviceTransfers.value = emptyList()
            _incomingEmailInvites.value = emptyList()
        }
    }

    private val _captureDraft = MutableStateFlow(CaptureDraft())
    val captureDraft: StateFlow<CaptureDraft> = _captureDraft.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiBusy = MutableStateFlow(false)
    val isAiBusy: StateFlow<Boolean> = _isAiBusy.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _hasSyncError = MutableStateFlow(false)
    val hasSyncError: StateFlow<Boolean> = _hasSyncError.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _lastUndoablePlanId = MutableStateFlow<String?>(null)
    val lastUndoablePlanId: StateFlow<String?> = _lastUndoablePlanId.asStateFlow()

    private val _itemSuggestion = MutableStateFlow<AiTagSummarySuggestion?>(null)
    val itemSuggestion: StateFlow<AiTagSummarySuggestion?> = _itemSuggestion.asStateFlow()

    private var searchJob: Job? = null
    private var previewJob: Job? = null
    private val previewCache = mutableMapOf<String, EnrichedMetadata>()

    private val baseDataFlow = combine(
        repository.allItems,
        repository.allFolders,
        repository.allItemFolders,
        repository.allTags,
        repository.allItemTags
    ) { items, folders, itemFolders, tags, itemTags ->
        LibrarySnapshot(
            items = items,
            folders = folders,
            itemFolders = itemFolders,
            tags = tags,
            itemTags = itemTags
        )
    }

    private val secondaryDataFlow = combine(
        repository.allNotes,
        repository.allCollections,
        repository.allCollectionItems,
        repository.allFileSources,
        repository.connectedSources
    ) { notes, collections, collectionItems, fileSources, connected ->
        LibrarySnapshot(
            notes = notes,
            collections = collections,
            collectionItems = collectionItems,
            fileSources = fileSources,
            connectedSources = connected
        )
    }

    private val opsDataFlow = combine(
        repository.organizationPlans,
        repository.pendingSyncEvents,
        repository.auditLogs,
        repository.userSettings,
        repository.diagnosticLogs
    ) { plans, syncEvents, audits, settings, diagnostics ->
        LibrarySnapshot(
            plans = plans,
            pendingSyncCount = syncEvents.size,
            auditLogs = audits,
            settings = settings ?: UserSettingsEntity(),
            diagnosticLogs = diagnostics
        )
    }

    val snapshot: StateFlow<LibrarySnapshot> = combine(
        baseDataFlow,
        secondaryDataFlow,
        opsDataFlow,
        repository.aiJobs
    ) { base, sec, ops, jobs ->
        base.copy(
            notes = sec.notes,
            collections = sec.collections,
            collectionItems = sec.collectionItems,
            fileSources = sec.fileSources,
            connectedSources = sec.connectedSources,
            plans = ops.plans,
            pendingSyncCount = ops.pendingSyncCount,
            auditLogs = ops.auditLogs,
            settings = ops.settings,
            diagnosticLogs = ops.diagnosticLogs,
            aiJobs = jobs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LibrarySnapshot()
    )

    val voiceRecorderState: StateFlow<VoiceRecorderState> = voiceAudioEngine?.recorderState
        ?: MutableStateFlow(VoiceRecorderState()).asStateFlow()
    val voicePlaybackState: StateFlow<VoicePlaybackState> = voiceAudioEngine?.playbackState
        ?: MutableStateFlow(VoicePlaybackState()).asStateFlow()

    private val _launchableApps = MutableStateFlow<List<LaunchableAppInfo>>(emptyList())
    val launchableApps: StateFlow<List<LaunchableAppInfo>> = _launchableApps.asStateFlow()

    private val _monkPermissions = MutableStateFlow(
        MonkPermissionsStatus(
            hasNotifications = true,
            hasExactAlarms = true,
            hasUsageStats = false,
            hasOverlay = false,
            hasAccessibility = false,
            hasBatteryExemption = false
        )
    )
    val monkPermissions: StateFlow<MonkPermissionsStatus> = _monkPermissions.asStateFlow()

    private val workspaceBaseFlow = if (workspaceRepository != null) {
        combine(
            workspaceRepository.notes,
            workspaceRepository.tasks,
            workspaceRepository.reminders,
            workspaceRepository.habits,
            workspaceRepository.habitCompletions
        ) { notes, tasks, reminders, habits, completions ->
            WorkspaceSnapshot(
                notes = notes,
                tasks = tasks,
                reminders = reminders,
                habits = habits,
                habitCompletions = completions
            )
        }
    } else {
        MutableStateFlow(WorkspaceSnapshot())
    }

    val workspaceSnapshot: StateFlow<WorkspaceSnapshot> = if (workspaceRepository != null) {
        combine(
            workspaceBaseFlow,
            workspaceRepository.blockerRules,
            workspaceRepository.focusSessions
        ) { base, rules, sessions ->
            base.copy(blockerRules = rules, focusSessions = sessions)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = WorkspaceSnapshot()
        )
    } else {
        MutableStateFlow(WorkspaceSnapshot()).asStateFlow()
    }

    val detoxState: StateFlow<DetoxSessionState> = repository.detoxSessionState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DetoxSessionState()
    )

    // --- OS Modules StateFlows ---
    val startupProjects: StateFlow<List<StartupProjectEntity>> = (osRepository?.startupProjects
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val startupPhaseItems: StateFlow<List<StartupPhaseItemEntity>> = (osRepository?.startupPhaseItems
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val startupSprintTasks: StateFlow<List<StartupSprintTaskEntity>> = (osRepository?.startupSprintTasks
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _isGeneratingStartupPlan = MutableStateFlow(false)
    val isGeneratingStartupPlan: StateFlow<Boolean> = _isGeneratingStartupPlan.asStateFlow()

    private val _aiStartupError = MutableStateFlow<String?>(null)
    val aiStartupError: StateFlow<String?> = _aiStartupError.asStateFlow()

    val financeSectors: StateFlow<List<FinanceSectorEntity>> = (osRepository?.financeSectors
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val financeTransactions: StateFlow<List<FinanceTransactionEntity>> = (osRepository?.financeTransactions
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val teams: StateFlow<List<TeamWorkspaceEntity>> = (osRepository?.teams
        ?: MutableStateFlow(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _activeTeamId = MutableStateFlow<String?>(null)
    val activeTeamId: StateFlow<String?> = _activeTeamId.asStateFlow()

    private val _activeTeamMembers = MutableStateFlow<List<TeamMemberEntity>>(emptyList())
    val activeTeamMembers: StateFlow<List<TeamMemberEntity>> = _activeTeamMembers.asStateFlow()

    private val _activeTeamTasks = MutableStateFlow<List<TeamSharedTaskEntity>>(emptyList())
    val activeTeamTasks: StateFlow<List<TeamSharedTaskEntity>> = _activeTeamTasks.asStateFlow()

    private val _activeTeamDocs = MutableStateFlow<List<TeamSharedDocEntity>>(emptyList())
    val activeTeamDocs: StateFlow<List<TeamSharedDocEntity>> = _activeTeamDocs.asStateFlow()

    private val _activeTeamNotifications = MutableStateFlow<List<TeamNotificationEntity>>(emptyList())
    val activeTeamNotifications: StateFlow<List<TeamNotificationEntity>> = _activeTeamNotifications.asStateFlow()

    private val _activeTeamActivities = MutableStateFlow<List<TeamActivityEntity>>(emptyList())
    val activeTeamActivities: StateFlow<List<TeamActivityEntity>> = _activeTeamActivities.asStateFlow()

    private val _activeTeamEmailInvites = MutableStateFlow<List<TeamEmailInviteEntity>>(emptyList())
    val activeTeamEmailInvites: StateFlow<List<TeamEmailInviteEntity>> = _activeTeamEmailInvites.asStateFlow()

    private val _incomingEmailInvites = MutableStateFlow<List<TeamEmailInviteEntity>>(emptyList())
    val incomingEmailInvites: StateFlow<List<TeamEmailInviteEntity>> = _incomingEmailInvites.asStateFlow()

    private val _latestTeamAlertBanner = MutableStateFlow<TeamNotificationEntity?>(null)
    val latestTeamAlertBanner: StateFlow<TeamNotificationEntity?> = _latestTeamAlertBanner.asStateFlow()

    private val _inviteLookupPreview = MutableStateFlow<CloudTeamInvitePreview?>(null)
    val inviteLookupPreview: StateFlow<CloudTeamInvitePreview?> = _inviteLookupPreview.asStateFlow()

    private val _teamBusy = MutableStateFlow(false)
    val teamBusy: StateFlow<Boolean> = _teamBusy.asStateFlow()

    private val _teamError = MutableStateFlow<String?>(null)
    val teamError: StateFlow<String?> = _teamError.asStateFlow()

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            repository.adminVaultConfig.collect { cfg ->
                repository.syncRuntimeAiFromVault(cfg)
            }
        }
        viewModelScope.launch {
            repository.ensureInitialized()
            osRepository?.ensureDefaultFinanceSectors()
            _currentDeviceId.value = repository.getOrCreateDeviceId()
            auth.currentUser?.let { u ->
                repository.recordSignedInAccount(
                    uid = u.uid,
                    displayName = u.displayName ?: "Library User",
                    email = u.email.orEmpty()
                )
                restartCloudObserversIfEligible(u.uid)
            }
            // Background pass to upgrade saved links missing OG/YouTube/GitHub previews
            launch {
                repository.refreshMissingThumbnails()
            }
            launch {
                workspaceRepository?.syncMonitorServiceState()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        cloudListenersJob?.cancel()
        teamListListenerJob?.cancel()
        activeTeamDetailJob?.cancel()
        incomingEmailInvitesJob?.cancel()
        voiceAudioEngine?.releaseAll()
    }

    private fun restartCloudObserversIfEligible(uid: String) {
        cloudListenersJob?.cancel()
        cloudListenersJob = viewModelScope.launch {
            repository.registerDevicePresence(currentDeviceName, "online")
            launch {
                repository.observeCloudDevices(uid).collect { list ->
                    _connectedDevices.value = list
                }
            }
            launch {
                repository.observeCloudTransfers(uid).collect { list ->
                    _deviceTransfers.value = list.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
            }
            launch {
                repository.observeCloudReviews(uid).collect { cloudList ->
                    repository.mergeCloudReviews(cloudList)
                }
            }
            launch {
                repository.observeRemoteThemeMode(uid).collect { remoteTheme ->
                    if (!remoteTheme.isNullOrBlank()) {
                        repository.applyRemoteThemeMode(remoteTheme)
                    }
                }
            }
            launch {
                repository.observeRemoteAccentPreset(uid).collect { remoteAccent ->
                    if (!remoteAccent.isNullOrBlank()) {
                        repository.applyRemoteAccentPreset(remoteAccent)
                    }
                }
            }
            launch {
                repository.observeGlobalAiConfig().collect { globalConfig ->
                    if (globalConfig != null) {
                        repository.applyGlobalAiConfig(globalConfig)
                    }
                }
            }
            launch {
                repository.observeRemoteAdminConfig(uid).collect { remoteAdmin ->
                    if (remoteAdmin != null) {
                        repository.applyRemoteAdminVaultConfig(remoteAdmin)
                    } else {
                        repository.bootstrapRemoteAdminConfigIfNeeded()
                    }
                }
            }
        }
        startTeamWorkspaceObserver(uid)
    }

    private fun startTeamWorkspaceObserver(uid: String) {
        val sync = cloudOsSync ?: return
        val repo = osRepository ?: return
        incomingEmailInvitesJob?.cancel()
        val email = auth.currentUser?.email?.trim()?.lowercase().orEmpty()
        if (email.isNotBlank()) {
            incomingEmailInvitesJob = viewModelScope.launch {
                sync.observeIncomingEmailInvites(email).collect { invites ->
                    _incomingEmailInvites.value = invites
                }
            }
        }
        teamListListenerJob?.cancel()
        teamListListenerJob = viewModelScope.launch {
            sync.observeMyTeams(uid).collect { cloudTeams ->
                repo.cacheTeamsSnapshot(cloudTeams)
                val currentActive = _activeTeamId.value
                val nextActive = when {
                    currentActive != null && cloudTeams.any { it.id == currentActive } -> currentActive
                    cloudTeams.isNotEmpty() -> cloudTeams.first().id
                    else -> null
                }
                if (nextActive != _activeTeamId.value) {
                    _activeTeamId.value = nextActive
                    if (nextActive != null) {
                        startActiveTeamListeners(nextActive, uid)
                    } else {
                        activeTeamDetailJob?.cancel()
                        _activeTeamMembers.value = emptyList()
                        _activeTeamTasks.value = emptyList()
                        _activeTeamDocs.value = emptyList()
                        _activeTeamNotifications.value = emptyList()
                        _activeTeamActivities.value = emptyList()
                        _activeTeamEmailInvites.value = emptyList()
                    }
                }
            }
        }
    }

    private fun startActiveTeamListeners(teamId: String, uid: String) {
        val sync = cloudOsSync ?: return
        val repo = osRepository ?: return
        activeTeamDetailJob?.cancel()
        activeTeamDetailJob = viewModelScope.launch {
            launch {
                sync.updateMemberPresence(
                    teamId = teamId,
                    displayName = userDisplayName.value,
                    email = userEmail.value.orEmpty(),
                    isOnline = true,
                    photoUrl = _googlePhotoUrl.value.orEmpty()
                )
            }
            launch {
                sync.observeTeamMembers(teamId).collect { list ->
                    _activeTeamMembers.value = list
                    repo.cacheTeamMembersSnapshot(teamId, list)
                }
            }
            launch {
                sync.observeTeamTasks(teamId).collect { list ->
                    _activeTeamTasks.value = list
                    repo.cacheTeamTasksSnapshot(teamId, list)
                }
            }
            launch {
                sync.observeTeamDocs(teamId).collect { list ->
                    _activeTeamDocs.value = list
                    repo.cacheTeamDocsSnapshot(teamId, list)
                }
            }
            launch {
                sync.observeTeamNotifications(teamId).collect { list ->
                    _activeTeamNotifications.value = list
                    repo.cacheTeamNotificationsSnapshot(list)
                    val newest = list.firstOrNull()
                    if (newest != null && repo.maybePostLocalTeamNotification(newest, uid)) {
                        _latestTeamAlertBanner.value = newest
                    }
                }
            }
            launch {
                sync.observeTeamActivities(teamId).collect { list ->
                    _activeTeamActivities.value = list
                    repo.cacheTeamActivitiesSnapshot(teamId, list)
                }
            }
            launch {
                sync.observeTeamEmailInvites(teamId).collect { list ->
                    _activeTeamEmailInvites.value = list
                    repo.cacheTeamEmailInvitesSnapshot(teamId, list)
                }
            }
        }
    }

    fun isOnline(): Boolean = repository.isOnline()

    fun getSyncBadgeState(): SyncBadgeState {
        val snap = snapshot.value
        return repository.computeSyncBadgeState(
            isSignedIn = isSignedIn.value,
            cloudSyncOptIn = snap.settings.cloudSyncOptIn,
            pendingCount = snap.pendingSyncCount,
            isSyncing = _isSyncing.value,
            hasSyncError = _hasSyncError.value
        )
    }

    fun clearSupabaseAuthMessages() {
        _supabaseAuthError.value = null
        _supabaseAuthNotice.value = null
    }

    fun signInWithSupabase(
        email: String,
        password: String,
        addStarters: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _supabaseAuthError.value = null
            _supabaseAuthNotice.value = null
            val res = repository.signInWithSupabase(email, password)
            res.fold(
                onSuccess = { session ->
                    if (addStarters) addStarterFolders()
                    _bannerMessage.value = "Signed in as ${session.displayName}"
                    onDone()
                    triggerSyncNow()
                },
                onFailure = { err ->
                    _supabaseAuthError.value = err.message ?: "Sign in failed."
                }
            )
            _isAuthBusy.value = false
        }
    }

    fun signUpWithSupabase(
        fullName: String,
        email: String,
        password: String,
        addStarters: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _supabaseAuthError.value = null
            _supabaseAuthNotice.value = null
            val res = repository.signUpWithSupabase(fullName, email, password)
            res.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is SupabaseSignUpOutcome.Authenticated -> {
                            if (addStarters) addStarterFolders()
                            _bannerMessage.value = "Welcome, ${outcome.session.displayName}"
                            onDone()
                            triggerSyncNow()
                        }
                        is SupabaseSignUpOutcome.ConfirmationRequired -> {
                            _supabaseAuthNotice.value = outcome.message
                        }
                    }
                },
                onFailure = { err ->
                    _supabaseAuthError.value = err.message ?: "Account creation failed."
                }
            )
            _isAuthBusy.value = false
        }
    }

    fun signOutSupabaseSession() {
        viewModelScope.launch {
            repository.signOutSupabase()
        }
    }

    fun completeOnboarding(guestMode: Boolean, cloudSyncOptIn: Boolean) {
        viewModelScope.launch {
            repository.completeOnboarding(guestMode, cloudSyncOptIn)
            if (!guestMode && cloudSyncOptIn && isSignedIn.value) {
                triggerSyncNow()
            }
        }
    }

    fun enterDemoMode() {
        viewModelScope.launch {
            repository.seedDemoLibraryData()
            repository.completeOnboarding(guestMode = true, cloudSyncOptIn = false)
            _bannerMessage.value = "Explore Demo ready • Local vault pre-populated"
            launch {
                repository.refreshMissingThumbnails()
            }
        }
    }

    fun markOnboardingWizardCompleted(
        role: String,
        useCase: String,
        saveHabit: String,
        workflow: String
    ) {
        viewModelScope.launch {
            repository.saveOnboardingProfile(
                role = role,
                useCase = useCase,
                workflow = workflow,
                saveHabit = saveHabit,
                wizardCompleted = true
            )
        }
    }

    fun completeWizardAndGoogleSync(
        role: String,
        useCase: String,
        saveHabit: String,
        workflow: String
    ) {
        if (_isWelcomeSyncTransitioning.value) return
        viewModelScope.launch {
            _isWelcomeSyncTransitioning.value = true
            try {
                repository.saveOnboardingProfile(
                    role = role,
                    useCase = useCase,
                    workflow = workflow,
                    saveHabit = saveHabit,
                    wizardCompleted = true
                )
                if (workflow != "minimal_inbox") {
                    repository.createStarterFolders(
                        listOf(
                            "AI & Technology",
                            "GitHub Projects",
                            "Tech News",
                            "Video Editing",
                            "Design Inspiration",
                            "Business Ideas",
                            "Learning Resources",
                            "Personal Notes",
                            "Other Resources"
                        )
                    )
                }
                repository.updateSettings {
                    it.copy(
                        cloudSyncOptIn = true,
                        autoMetadataEnrichment = true
                    )
                }
                repository.registerDevicePresence(currentDeviceName, "online")
                repository.syncNow()
                delay(850L)
                repository.completeOnboarding(guestMode = false, cloudSyncOptIn = true)
                _bannerMessage.value = "Welcome! Cloud sync & preferences configured."
            } finally {
                _isWelcomeSyncTransitioning.value = false
            }
        }
    }

    fun addStarterFolders() {
        viewModelScope.launch {
            repository.createStarterFolders(
                listOf(
                    "AI & Technology",
                    "GitHub Projects",
                    "Tech News",
                    "Video Editing",
                    "Design Inspiration",
                    "Business Ideas",
                    "Learning Resources",
                    "Personal Notes",
                    "Other Resources"
                )
            )
            _bannerMessage.value = "Starter folders added to your library."
        }
    }

    // --- Capture & Share Intents ---
    fun openCaptureSheet(
        initialText: String = "",
        initialTitle: String = "",
        uris: List<Uri> = emptyList(),
        targetFolderId: String = AppDatabase.INBOX_FOLDER_ID,
        targetCollectionId: String? = null
    ) {
        previewJob?.cancel()
        _captureDraft.value = CaptureDraft(
            isOpen = true,
            initialText = initialText,
            initialTitle = initialTitle,
            sharedUris = uris,
            targetFolderId = targetFolderId,
            targetCollectionId = targetCollectionId
        )
        if (initialText.isNotBlank()) {
            previewUrlForCapture(initialText)
        }
    }

    fun closeCaptureSheet() {
        previewJob?.cancel()
        _captureDraft.value = CaptureDraft()
    }

    fun previewUrlForCapture(rawInput: String) {
        val extracted = UrlNormalizer.extractFirstWebUrl(rawInput)
        if (extracted == null || !UrlNormalizer.isSafeRemoteUrl(extracted)) {
            previewJob?.cancel()
            _captureDraft.update {
                it.copy(
                    previewUrl = null,
                    isPreviewLoading = false,
                    previewMetadata = null,
                    previewError = null
                )
            }
            return
        }
        val cached = previewCache[extracted]
        if (cached != null) {
            previewJob?.cancel()
            _captureDraft.update {
                it.copy(
                    previewUrl = extracted,
                    isPreviewLoading = false,
                    previewMetadata = cached,
                    previewError = null
                )
            }
            return
        }
        val current = _captureDraft.value
        if (current.previewUrl == extracted && current.isPreviewLoading) return

        previewJob?.cancel()
        _captureDraft.update {
            it.copy(
                previewUrl = extracted,
                isPreviewLoading = true,
                previewMetadata = null,
                previewError = null
            )
        }
        if (!repository.isOnline()) {
            _captureDraft.update {
                it.copy(
                    isPreviewLoading = false,
                    previewError = "Offline — metadata will auto-scrape when online."
                )
            }
            return
        }
        previewJob = viewModelScope.launch {
            val res = repository.previewUrlMetadata(extracted)
            if (_captureDraft.value.previewUrl != extracted) return@launch
            res.fold(
                onSuccess = { meta ->
                    previewCache[extracted] = meta
                    _captureDraft.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewMetadata = meta,
                            previewError = null
                        )
                    }
                },
                onFailure = { err ->
                    _captureDraft.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewError = err.message
                        )
                    }
                }
            )
        }
    }

    fun saveCapture(
        rawInput: String,
        title: String,
        folderId: String,
        tagsCsv: String,
        note: String,
        description: String = "",
        preloadedThumbnailUrl: String? = null,
        allowDuplicate: Boolean = false,
        collectionId: String? = _captureDraft.value.targetCollectionId,
        onSavedItem: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _captureDraft.update { it.copy(isSaving = true, errorMessage = null) }
            val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val extractedUrl = UrlNormalizer.extractFirstWebUrl(rawInput)
            val cachedPreview = extractedUrl?.let { previewCache[it] } ?: _captureDraft.value.previewMetadata
            val effectiveTitle = title.ifBlank {
                cachedPreview?.title?.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) }.orEmpty()
            }
            val effectiveDescription = description.ifBlank {
                cachedPreview?.description.orEmpty()
            }
            val effectiveThumb = preloadedThumbnailUrl ?: cachedPreview?.thumbnailUrl

            when (val outcome = repository.captureWebUrlOrText(
                rawInput = rawInput,
                customTitle = effectiveTitle,
                customDescription = effectiveDescription,
                folderId = folderId,
                tagNames = tags,
                initialNote = note.takeIf { it.isNotBlank() },
                preloadedThumbnailUrl = effectiveThumb,
                allowDuplicateSave = allowDuplicate,
                collectionId = collectionId
            )) {
                is CaptureOutcome.Saved -> {
                    previewJob?.cancel()
                    _captureDraft.value = CaptureDraft()
                    _bannerMessage.value = if (collectionId != null) "Saved & added to collection" else "Saved locally to library"
                    onSavedItem(outcome.item.id)
                    // Background metadata enrichment when online for missing title, description, or real thumbnail
                    val savedItem = outcome.item
                    val needsEnrichment = !savedItem.originalUrl.isNullOrBlank() && (
                        savedItem.thumbnailUrl.isNullOrBlank() ||
                            UrlNormalizer.isFaviconFallbackUrl(savedItem.thumbnailUrl) ||
                            savedItem.description.isBlank() ||
                            UrlNormalizer.looksLikePlaceholderTitle(savedItem.title, savedItem.originalUrl, savedItem.domain)
                        )
                    if (needsEnrichment && repository.isOnline()) {
                        launch {
                            repository.enrichItemMetadata(savedItem.id)
                        }
                    }
                }
                is CaptureOutcome.DuplicateDetected -> {
                    _captureDraft.update {
                        it.copy(isSaving = false, duplicateState = outcome)
                    }
                }
                is CaptureOutcome.Error -> {
                    _captureDraft.update {
                        it.copy(isSaving = false, errorMessage = outcome.message)
                    }
                }
            }
        }
    }

    fun addExistingDuplicateToFolder(existingItemId: String, folderId: String) {
        viewModelScope.launch {
            repository.addItemToFolder(existingItemId, folderId)
            _captureDraft.value.targetCollectionId?.let { colId ->
                repository.addItemToCollection(colId, existingItemId)
            }
            _captureDraft.value = CaptureDraft()
            _bannerMessage.value = "Added existing item to selected folder"
        }
    }

    fun captureLocalFile(
        uri: Uri,
        folderId: String = AppDatabase.INBOX_FOLDER_ID,
        collectionId: String? = null
    ) {
        viewModelScope.launch {
            when (val outcome = repository.captureLocalFileUri(uri, folderId, collectionId = collectionId)) {
                is CaptureOutcome.Saved -> {
                    _captureDraft.value = CaptureDraft()
                    _bannerMessage.value = "File reference saved with preview"
                }
                is CaptureOutcome.Error -> {
                    _bannerMessage.value = outcome.message
                }
                else -> Unit
            }
        }
    }

    fun captureMultipleUris(
        uris: List<Uri>,
        folderId: String = AppDatabase.INBOX_FOLDER_ID,
        collectionId: String? = null
    ) {
        viewModelScope.launch {
            var count = 0
            uris.forEach { uri ->
                if (repository.captureLocalFileUri(uri, folderId, collectionId = collectionId) is CaptureOutcome.Saved) {
                    count++
                }
            }
            _captureDraft.value = CaptureDraft()
            _bannerMessage.value = "Saved $count local file references"
        }
    }

    fun connectFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            val res = repository.connectFolderTree(treeUri)
            res.fold(
                onSuccess = { count ->
                    _bannerMessage.value = "Connected folder ($count file references indexed)"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Failed to connect folder"
                }
            )
        }
    }

    fun refreshFileHealth() {
        viewModelScope.launch {
            val issues = repository.refreshAllFileHealth()
            _bannerMessage.value = if (issues == 0) {
                "All local file references verified Available"
            } else {
                "$issues local reference(s) need attention"
            }
        }
    }

    // --- Search ---
    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(query = query, errorMessage = null) }
        triggerDebouncedSearch()
    }

    fun setSearchAiMode(enabled: Boolean) {
        _searchState.update { it.copy(isOnlineAiMode = enabled, aiResponse = null, errorMessage = null) }
        triggerDebouncedSearch()
    }

    fun setSearchContentTypeFilter(type: String) {
        _searchState.update { it.copy(contentTypeFilter = type) }
    }

    fun setSearchFolderFilter(folderId: String?) {
        _searchState.update { it.copy(folderFilter = folderId) }
    }

    fun toggleSearchFavoritesOnly() {
        _searchState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()
        val current = _searchState.value
        if (current.query.isBlank()) {
            _searchState.update { it.copy(localHits = emptyList(), aiResponse = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(220L)
            _searchState.update { it.copy(isSearching = true, errorMessage = null) }
            val local = repository.searchLocal(current.query)
            _searchState.update { it.copy(localHits = local, isSearching = current.isOnlineAiMode) }
            if (current.isOnlineAiMode) {
                val aiRes = repository.searchOnlineWithAi(current.query)
                aiRes.fold(
                    onSuccess = { resp ->
                        _searchState.update { it.copy(aiResponse = resp, isSearching = false) }
                    },
                    onFailure = { err ->
                        _searchState.update {
                            it.copy(
                                isSearching = false,
                                errorMessage = err.message ?: "Online AI search failed"
                            )
                        }
                    }
                )
            }
        }
    }

    // --- Item, Folder, Tag, Note, Collection CRUD ---
    fun observeItem(itemId: String): Flow<ItemEntity?> = repository.observeItem(itemId)
    fun observeNotesForItem(itemId: String): Flow<List<NoteEntity>> = repository.observeNotesForItem(itemId)
    fun observeOperationsForPlan(planId: String): Flow<List<OrganizationOperationEntity>> =
        repository.observeOperationsForPlan(planId)

    fun toggleFavorite(item: ItemEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.isFavorite)
        }
    }

    fun editItemDetails(
        itemId: String,
        title: String,
        url: String?,
        description: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateItemDetails(itemId, title, url, description)
            res.fold(
                onSuccess = { updated ->
                    _bannerMessage.value = "Updated \"${updated.title}\""
                    onDone()
                    if (!updated.originalUrl.isNullOrBlank() && repository.isOnline() &&
                        (updated.thumbnailUrl.isNullOrBlank() || com.example.data.util.UrlNormalizer.isFaviconFallbackUrl(updated.thumbnailUrl))
                    ) {
                        launch { repository.enrichItemMetadata(updated.id) }
                    }
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not update item"
                }
            )
        }
    }

    fun softDeleteItem(itemId: String) {
        viewModelScope.launch {
            repository.softDeleteItem(itemId)
            _bannerMessage.value = "Item deleted from library"
        }
    }

    fun addItemToFolder(itemId: String, folderId: String) {
        viewModelScope.launch {
            repository.addItemToFolder(itemId, folderId)
            _bannerMessage.value = "Folder updated"
        }
    }

    fun removeItemFromFolder(itemId: String, folderId: String) {
        viewModelScope.launch {
            repository.removeItemFromFolder(itemId, folderId)
        }
    }

    fun createFolder(name: String, parentFolderId: String? = null, colorHex: String = "#E53935") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name, parentFolderId, colorHex)
            _bannerMessage.value = "Folder \"$name\" created"
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameFolder(folderId, newName)
        }
    }

    fun deleteFolder(folderId: String, moveTargetFolderId: String?) {
        viewModelScope.launch {
            repository.deleteFolder(folderId, moveTargetFolderId)
            _bannerMessage.value = "Folder deleted"
        }
    }

    fun addTagToItem(itemId: String, tagName: String, origin: String = "user") {
        viewModelScope.launch {
            repository.addTagToItem(itemId, tagName, origin)
        }
    }

    fun removeTagFromItem(itemId: String, tagId: String) {
        viewModelScope.launch {
            repository.removeTagFromItem(itemId, tagId)
        }
    }

    fun renameTag(tagId: String, newName: String) {
        viewModelScope.launch {
            repository.renameTag(tagId, newName)
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
        }
    }

    fun saveNote(itemId: String, body: String, existingNoteId: String? = null) {
        viewModelScope.launch {
            repository.saveNote(itemId, body, existingNoteId)
            _bannerMessage.value = "Note saved"
        }
    }

    fun deleteNote(noteId: String, itemId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId, itemId)
        }
    }

    fun createCollection(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCollection(name, description)
            _bannerMessage.value = "Collection \"$name\" created"
        }
    }

    fun addItemToCollection(collectionId: String, itemId: String) {
        viewModelScope.launch {
            repository.addItemToCollection(collectionId, itemId)
            _bannerMessage.value = "Added to collection"
        }
    }

    fun removeItemFromCollection(collectionId: String, itemId: String) {
        viewModelScope.launch {
            repository.removeItemFromCollection(collectionId, itemId)
            _bannerMessage.value = "Removed from collection"
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            _bannerMessage.value = "Collection deleted"
        }
    }

    fun enrichItemNow(itemId: String) {
        viewModelScope.launch {
            val res = repository.enrichItemMetadata(itemId, overwriteTitle = true)
            res.fold(
                onSuccess = { _bannerMessage.value = "Metadata refreshed" },
                onFailure = { _bannerMessage.value = it.message ?: "Metadata enrichment failed" }
            )
        }
    }

    // --- AI Assistant & Organization Plan ---
    fun sendAssistantMessage(question: String) {
        if (question.isBlank()) return
        _chatMessages.update { it + ChatMessage(isUser = true, text = question) }
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiError.value = null
            val result = repository.askAiAssistant(question)
            result.fold(
                onSuccess = { answer ->
                    _chatMessages.update { it + ChatMessage(isUser = false, text = answer) }
                },
                onFailure = { err ->
                    val msg = err.message ?: "AI Assistant unavailable."
                    _aiError.value = msg
                    _chatMessages.update { it + ChatMessage(isUser = false, text = msg, isError = true) }
                }
            )
            _isAiBusy.value = false
        }
    }

    fun suggestTagsForItem(itemId: String) {
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiError.value = null
            _itemSuggestion.value = null
            val res = repository.suggestTagsAndSummaryForItem(itemId)
            res.fold(
                onSuccess = { _itemSuggestion.value = it },
                onFailure = { _aiError.value = it.message ?: "AI suggestion failed" }
            )
            _isAiBusy.value = false
        }
    }

    fun clearItemSuggestion() {
        _itemSuggestion.value = null
        _aiError.value = null
    }

    fun generateOrganizationPlanForInbox(onPlanReady: (String) -> Unit) {
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiError.value = null
            val snap = snapshot.value
            val inboxItemIds = snap.itemFolders
                .filter { it.folderId == AppDatabase.INBOX_FOLDER_ID }
                .map { it.itemId }
                .toSet()
            val targets = snap.items.filter { it.id in inboxItemIds || it.status == "unsorted" }
                .ifEmpty { snap.items.take(25) }

            if (targets.isEmpty()) {
                _aiError.value = "Your library is empty. Save links or files first."
                _isAiBusy.value = false
                return@launch
            }

            val result = repository.generateOrganizationPreview(targets)
            result.fold(
                onSuccess = { planId ->
                    _isAiBusy.value = false
                    onPlanReady(planId)
                },
                onFailure = { err ->
                    _aiError.value = err.message ?: "Failed to build AI organization plan"
                    _isAiBusy.value = false
                }
            )
        }
    }

    fun toggleOperationSelection(operationId: String, selected: Boolean) {
        viewModelScope.launch {
            repository.togglePlanOperationSelection(operationId, selected)
        }
    }

    fun approveOrganizationPlan(planId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.applyApprovedOrganizationPlan(planId)
            res.fold(
                onSuccess = { count ->
                    _lastUndoablePlanId.value = planId
                    _bannerMessage.value = "Applied $count AI organization moves"
                    onDone()
                },
                onFailure = { err ->
                    _aiError.value = err.message ?: "Failed to apply organization plan"
                }
            )
        }
    }

    fun rejectOrganizationPlan(planId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.rejectOrganizationPlan(planId)
            _bannerMessage.value = "Organization plan rejected — zero changes applied"
            onDone()
        }
    }

    fun undoLastOrganizationPlan() {
        val planId = _lastUndoablePlanId.value ?: return
        viewModelScope.launch {
            val res = repository.undoOrganizationPlan(planId)
            res.fold(
                onSuccess = { count ->
                    _lastUndoablePlanId.value = null
                    _bannerMessage.value = "Undid organization for $count items"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Undo failed"
                }
            )
        }
    }

    // --- Cloud Sync & Settings & Backup ---
    fun triggerSyncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _hasSyncError.value = false
            _syncMessage.value = null
            val res = repository.syncNow()
            val workspaceSynced = runCatching { workspaceRepository?.syncWorkspaceNow() ?: 0 }.getOrDefault(0)
            val osSynced = runCatching { osRepository?.syncOsDataNow() ?: 0 }.getOrDefault(0)
            val extraCount = workspaceSynced + osSynced
            res.fold(
                onSuccess = { msg ->
                    _syncMessage.value = if (extraCount > 0) "$msg • $extraCount workspace & OS records synced" else msg
                },
                onFailure = { err ->
                    _hasSyncError.value = true
                    _syncMessage.value = err.message ?: "Sync failed"
                }
            )
            _isSyncing.value = false
        }
    }

    fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportBackup(uri)
            res.fold(
                onSuccess = { count -> _bannerMessage.value = "Exported $count library items to JSON" },
                onFailure = { err -> _bannerMessage.value = err.message ?: "Export failed" }
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val res = repository.importBackup(uri)
            res.fold(
                onSuccess = { count -> _bannerMessage.value = "Restored $count items from backup" },
                onFailure = { err -> _bannerMessage.value = err.message ?: "Import failed" }
            )
        }
    }

    fun clearDiagnostics() {
        viewModelScope.launch {
            repository.clearDiagnostics()
            _bannerMessage.value = "Diagnostic logs cleared"
        }
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    // --- Theme, Appearance, Profile, Admin Vault, Multi-Device Sync & Reviews ---
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setAccentPreset(preset: LiquidAccentPreset) {
        viewModelScope.launch {
            repository.setAccentPreset(preset)
        }
    }

    fun updateUserProfileName(newName: String) {
        viewModelScope.launch {
            val res = repository.updateUserProfileName(newName)
            res.fold(
                onSuccess = { savedName ->
                    _userDisplayName.value = savedName
                    _bannerMessage.value = "Profile updated to $savedName"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not update profile"
                }
            )
        }
    }

    fun updateUserProfileDetails(
        newName: String,
        newEmail: String,
        avatarValue: String? = null,
        selectedImageUri: Uri? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateUserProfileDetails(
                newName = newName,
                newEmail = newEmail,
                avatarValue = avatarValue,
                selectedImageUri = selectedImageUri
            )
            res.fold(
                onSuccess = { savedName ->
                    _userDisplayName.value = savedName
                    _bannerMessage.value = "Profile saved for $savedName"
                    onDone()
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not save profile changes"
                }
            )
        }
    }

    fun clearAdminAuthError() {
        _adminAuthError.value = null
    }

    fun submitAdminPassword(passcode: String, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            _adminAuthError.value = null
            val res = repository.verifyOrCreateAdminPasscode(passcode)
            res.fold(
                onSuccess = {
                    _isAdminUnlocked.value = true
                    _adminTestStatus.value = null
                    _adminPasscodeStatus.value = null
                    onUnlocked()
                },
                onFailure = { err ->
                    _adminAuthError.value = err.message ?: "Invalid passcode"
                }
            )
        }
    }

    fun changeAdminPasscode(newPasscode: String) {
        viewModelScope.launch {
            _adminPasscodeStatus.value = null
            val res = repository.changeAdminPasscode(newPasscode)
            res.fold(
                onSuccess = {
                    val syncedNote = if (_isGoogleSignedIn.value && repository.isOnline()) {
                        "Admin passcode hash updated & synced to cloud."
                    } else {
                        "Admin passcode hash updated in local vault."
                    }
                    _adminPasscodeStatus.value = syncedNote
                    _bannerMessage.value = syncedNote
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to update passcode"
                    _adminPasscodeStatus.value = msg
                    _bannerMessage.value = msg
                }
            )
        }
    }

    fun lockAdminDashboard() {
        _isAdminUnlocked.value = false
        _adminTestStatus.value = null
        _adminPasscodeStatus.value = null
    }

    fun refreshAdminConfigFromCloud() {
        viewModelScope.launch {
            _isAdminCloudSyncing.value = true
            val res = repository.pullLatestCloudAdminConfig()
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Live API credentials synchronized from cloud vault"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Cloud vault refresh failed"
                }
            )
            _isAdminCloudSyncing.value = false
        }
    }

    fun saveAdminConfig(config: AdminVaultConfig) {
        viewModelScope.launch {
            _isAdminCloudSyncing.value = true
            val res = repository.saveAdminVaultConfig(config)
            res.fold(
                onSuccess = {
                    _bannerMessage.value = if (_isGoogleSignedIn.value && repository.isOnline()) {
                        "API configuration saved & synced to cloud vault"
                    } else {
                        "API configuration saved locally to device vault"
                    }
                },
                onFailure = { err ->
                    _bannerMessage.value = "Saved locally; cloud sync warning: ${err.message}"
                }
            )
            _isAdminCloudSyncing.value = false
        }
    }

    fun testAdminAiConnection(provider: String, openRouterKey: String, openRouterModel: String) {
        viewModelScope.launch {
            _isAdminTesting.value = true
            _adminTestStatus.value = null
            val res = repository.testAiConnection(provider, openRouterKey, openRouterModel)
            res.fold(
                onSuccess = { msg -> _adminTestStatus.value = "Connected: $msg" },
                onFailure = { err -> _adminTestStatus.value = "Error: ${err.message ?: "Connection failed"}" }
            )
            _isAdminTesting.value = false
        }
    }

    fun loadLiveFreeOpenRouterModels() {
        viewModelScope.launch {
            _isLoadingFreeModels.value = true
            val res = repository.fetchFreeOpenRouterModels()
            res.fold(
                onSuccess = { list ->
                    _freeOpenRouterModels.value = list
                    _bannerMessage.value = "Loaded ${list.size} 100% free OpenRouter models"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not load free model list"
                }
            )
            _isLoadingFreeModels.value = false
        }
    }

    fun setVaultPin(pin: String?) {
        viewModelScope.launch {
            repository.setVaultPin(pin)
            _bannerMessage.value = if (pin.isNullOrBlank()) "Vault PIN cleared" else "Vault PIN saved"
        }
    }

    fun setVaultLocked(locked: Boolean) {
        viewModelScope.launch {
            repository.setVaultLocked(locked)
        }
    }

    fun registerDevicePresence() {
        viewModelScope.launch {
            repository.registerDevicePresence(currentDeviceName, "online")
        }
    }

    fun sendToDevice(
        title: String,
        payloadText: String,
        payloadType: String = "link",
        targetDeviceId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.sendToDevice(
                title = title,
                payloadText = payloadText,
                payloadType = payloadType,
                fromDeviceName = currentDeviceName,
                targetDeviceId = targetDeviceId
            )
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Sent to synced devices!"
                    onSuccess()
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Failed to send across devices"
                }
            )
        }
    }

    fun acceptIncomingTransfer(transfer: CloudDeviceTransfer) {
        viewModelScope.launch {
            val res = repository.acceptTransferToLibrary(transfer)
            res.fold(
                onSuccess = { item ->
                    _bannerMessage.value = "Saved \"${item.title}\" to library"
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not save transfer"
                }
            )
        }
    }

    fun dismissIncomingTransfer(transferId: String) {
        viewModelScope.launch {
            repository.dismissTransfer(transferId)
        }
    }

    fun submitReview(itemId: String?, rating: Int, body: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val author = if (isSignedIn.value) userDisplayName.value else "Guest Vault"
            val res = repository.submitReview(
                itemId = itemId,
                authorName = author,
                rating = rating,
                body = body
            )
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Review published"
                    onDone()
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Could not submit review"
                }
            )
        }
    }

    fun toggleReviewHelpful(reviewId: String) {
        viewModelScope.launch {
            repository.toggleReviewHelpful(reviewId)
        }
    }

    // --- Workspace: Notion Notes, Voice Audio, Tasks, Reminders, Habits, Monk Mode ---
    fun startVoiceRecording() {
        voiceAudioEngine?.startRecording()
    }

    fun pauseVoiceRecording() {
        voiceAudioEngine?.pauseRecording()
    }

    fun resumeVoiceRecording() {
        voiceAudioEngine?.resumeRecording()
    }

    fun discardVoiceRecording() {
        voiceAudioEngine?.discardRecording()
    }

    fun stopVoiceRecordingToNote(title: String, cloudSync: Boolean = true) {
        val result = voiceAudioEngine?.stopAndSaveRecording() ?: return
        val (uri, durationMs) = result
        viewModelScope.launch {
            workspaceRepository?.saveWorkspaceNote(
                title = title.ifBlank { "Voice Note" },
                blocks = listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = title.ifBlank { "Voice Note" }),
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = "Audio reflection recorded inside workspace.")
                ),
                tagsCsv = "voice",
                isPinned = false,
                cloudSync = cloudSync,
                audioUri = uri,
                audioDurationMs = durationMs
            )
            voiceAudioEngine.clearLastRecording()
            _bannerMessage.value = "Voice note saved"
        }
    }

    fun stopVoiceRecordingToTask(title: String, dueDateEpochDay: Long = WorkspaceRepository.todayEpochDay(), cloudSync: Boolean = true) {
        val result = voiceAudioEngine?.stopAndSaveRecording() ?: return
        val (uri, durationMs) = result
        viewModelScope.launch {
            workspaceRepository?.saveWorkspaceTask(
                title = title.ifBlank { "Voice To-Do" },
                dueDateEpochDay = dueDateEpochDay,
                priority = "HIGH",
                cloudSync = cloudSync,
                audioUri = uri,
                audioDurationMs = durationMs
            )
            voiceAudioEngine.clearLastRecording()
            _bannerMessage.value = "Voice task added to Today's To-Do"
        }
    }

    fun toggleVoicePlayback(uriString: String) {
        voiceAudioEngine?.togglePlayback(uriString)
    }

    fun seekVoicePlayback(uriString: String, positionMs: Long) {
        voiceAudioEngine?.seekTo(uriString, positionMs)
    }

    fun saveWorkspaceNote(
        id: String? = null,
        title: String,
        blocks: List<WorkspaceBlock>,
        tagsCsv: String,
        isPinned: Boolean,
        cloudSync: Boolean,
        linkedItemId: String? = null,
        audioUri: String? = null,
        audioDurationMs: Long = 0L
    ) {
        viewModelScope.launch {
            workspaceRepository?.saveWorkspaceNote(
                id = id,
                title = title,
                blocks = blocks,
                tagsCsv = tagsCsv,
                isPinned = isPinned,
                cloudSync = cloudSync,
                linkedItemId = linkedItemId,
                audioUri = audioUri,
                audioDurationMs = audioDurationMs
            )
            _bannerMessage.value = if (cloudSync) "Note saved (Cloud Synced)" else "Note saved (Local Device Only)"
        }
    }

    fun toggleWorkspaceNotePin(note: WorkspaceNoteEntity) {
        viewModelScope.launch { workspaceRepository?.toggleWorkspaceNotePin(note) }
    }

    fun toggleWorkspaceNoteCloudSync(note: WorkspaceNoteEntity) {
        viewModelScope.launch {
            val next = !note.cloudSync
            workspaceRepository?.setWorkspaceNoteCloudSync(note.id, next)
            _bannerMessage.value = if (next) "Cloud Sync enabled for note" else "Switched to Local Device Only"
        }
    }

    fun deleteWorkspaceNote(noteId: String) {
        viewModelScope.launch {
            workspaceRepository?.deleteWorkspaceNote(noteId)
            _bannerMessage.value = "Note deleted"
        }
    }

    fun saveWorkspaceTask(
        id: String? = null,
        title: String,
        notes: String = "",
        dueDateEpochDay: Long = WorkspaceRepository.todayEpochDay(),
        priority: String = "MEDIUM",
        cloudSync: Boolean = true
    ) {
        viewModelScope.launch {
            workspaceRepository?.saveWorkspaceTask(id, title, notes, dueDateEpochDay, priority, cloudSync)
            _bannerMessage.value = "Task saved"
        }
    }

    fun toggleTaskCompleted(task: WorkspaceTaskEntity) {
        viewModelScope.launch { workspaceRepository?.toggleTaskCompleted(task) }
    }

    fun deleteWorkspaceTask(taskId: String) {
        viewModelScope.launch { workspaceRepository?.deleteWorkspaceTask(taskId) }
    }

    fun saveSmartReminder(
        id: String? = null,
        mode: String,
        title: String,
        notes: String,
        priority: String,
        triggerAtMillis: Long,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String,
        cloudSync: Boolean = true,
        targetItemId: String? = null,
        targetNoteId: String? = null
    ) {
        viewModelScope.launch {
            workspaceRepository?.saveSmartReminder(
                id = id,
                mode = mode,
                title = title,
                notes = notes,
                priority = priority,
                triggerAtMillis = triggerAtMillis,
                recurrenceType = recurrenceType,
                intervalMinutes = intervalMinutes,
                ringtoneUri = ringtoneUri,
                ringtoneTitle = ringtoneTitle,
                cloudSync = cloudSync,
                targetItemId = targetItemId,
                targetNoteId = targetNoteId
            )
            _bannerMessage.value = "Reminder scheduled ($ringtoneTitle)"
        }
    }

    fun toggleReminderEnabled(reminder: SmartReminderEntity) {
        viewModelScope.launch { workspaceRepository?.toggleReminderEnabled(reminder) }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            workspaceRepository?.deleteReminder(reminderId)
            _bannerMessage.value = "Reminder removed"
        }
    }

    fun createHabit(
        title: String,
        subtitle: String,
        category: String,
        targetPerWeek: Int,
        cloudSync: Boolean = true,
        reminderEnabled: Boolean = false,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        recurrenceType: String = ReminderScheduler.RECURRENCE_DAILY,
        intervalMinutes: Int = 1440,
        ringtoneUri: String? = null,
        ringtoneTitle: String = "Default System Sound"
    ) {
        viewModelScope.launch {
            workspaceRepository?.createHabit(
                title = title,
                subtitle = subtitle,
                category = category,
                targetPerWeek = targetPerWeek,
                cloudSync = cloudSync,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                recurrenceType = recurrenceType,
                intervalMinutes = intervalMinutes,
                ringtoneUri = ringtoneUri,
                ringtoneTitle = ringtoneTitle
            )
            _bannerMessage.value = if (reminderEnabled) {
                "Habit added with reminder ($ringtoneTitle)"
            } else {
                "Habit added"
            }
        }
    }

    fun updateHabitReminder(
        habit: HabitEntity,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) {
        viewModelScope.launch {
            workspaceRepository?.updateHabitReminder(
                habit = habit,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                recurrenceType = recurrenceType,
                intervalMinutes = intervalMinutes,
                ringtoneUri = ringtoneUri,
                ringtoneTitle = ringtoneTitle
            )
            _bannerMessage.value = if (reminderEnabled) {
                "Habit reminder updated (${String.format("%02d:%02d", reminderHour, reminderMinute)})"
            } else {
                "Habit reminder paused"
            }
        }
    }

    fun toggleHabitCompletion(habitId: String, epochDay: Long = WorkspaceRepository.todayEpochDay()) {
        viewModelScope.launch { workspaceRepository?.toggleHabitCompletion(habitId, epochDay) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch { workspaceRepository?.deleteHabit(habitId) }
    }

    fun refreshMonkState(context: android.content.Context) {
        _monkPermissions.value = MonkBlockerEngine.readPermissionsStatus(context)
        viewModelScope.launch {
            _launchableApps.value = workspaceRepository?.loadLaunchableApps().orEmpty()
        }
    }

    fun saveBlockerRule(
        id: String? = null,
        title: String,
        mode: String,
        packages: Collection<String>,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        daysOfWeekCsv: String,
        delayDurationMinutes: Int,
        activeDurationMinutes: Int?,
        preReminderMinutes: Int
    ) {
        viewModelScope.launch {
            workspaceRepository?.saveBlockerRule(
                id = id,
                title = title,
                mode = mode,
                packages = packages,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
                daysOfWeekCsv = daysOfWeekCsv,
                delayDurationMinutes = delayDurationMinutes,
                activeDurationMinutes = activeDurationMinutes,
                preReminderMinutes = preReminderMinutes,
                isEnabled = true
            )
            _bannerMessage.value = "Monk Mode blocker rule activated"
        }
    }

    fun toggleBlockerRule(rule: BlockerRuleEntity) {
        viewModelScope.launch { workspaceRepository?.toggleBlockerRule(rule) }
    }

    fun deleteBlockerRule(ruleId: String) {
        viewModelScope.launch {
            workspaceRepository?.deleteBlockerRule(ruleId)
            _bannerMessage.value = "Blocker rule deleted"
        }
    }

    fun startFocusSession(label: String, plannedMinutes: Int) {
        viewModelScope.launch {
            workspaceRepository?.startFocusSession(label, plannedMinutes)
            _bannerMessage.value = "Monk Mode session started ($plannedMinutes min)"
        }
    }

    fun completeFocusSession(emergencyUnlocked: Boolean = false) {
        viewModelScope.launch {
            workspaceRepository?.completeActiveFocusSession(emergencyUnlocked)
            _bannerMessage.value = if (emergencyUnlocked) "Emergency exit completed" else "Focus session completed!"
        }
    }

    // --- Digital Detox Lockdown Controls ---
    fun startDigitalDetox(
        durationHours: Int,
        greyscaleUi: Boolean,
        suppressNotifications: Boolean,
        whitelistedPackages: Set<String>
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val clampedHours = durationHours.coerceIn(1, 24)
            val next = DetoxSessionState(
                isActive = true,
                durationHours = clampedHours,
                startedAtMillis = now,
                endsAtMillis = now + clampedHours * 3_600_000L,
                greyscaleUi = greyscaleUi,
                suppressNotifications = suppressNotifications,
                whitelistedPackagesCsv = whitelistedPackages.distinct().sorted().joinToString(",")
            )
            repository.saveDetoxSessionState(next)
            workspaceRepository?.syncMonitorServiceState()
            _bannerMessage.value = "${clampedHours}h Digital Detox Lockdown activated"
        }
    }

    fun endDigitalDetoxEmergency() {
        viewModelScope.launch {
            repository.clearDetoxSessionState()
            workspaceRepository?.syncMonitorServiceState()
            _bannerMessage.value = "Digital Detox unlocked via emergency exit"
        }
    }

    // --- Startup & Business Builder OS ---
    fun createStartupProject(
        name: String,
        tagline: String,
        targetPersona: String,
        techStack: String,
        stage: String = "IDEATION",
        projectType: String = "STARTUP",
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val created = osRepository?.createStartupProject(
                name = name,
                tagline = tagline,
                targetPersona = targetPersona,
                techStack = techStack,
                stage = stage,
                projectType = projectType
            )
            if (created != null) {
                _bannerMessage.value = "\"${created.name}\" created with 6 execution phases & SOP templates"
                onCreated(created.id)
            }
        }
    }

    fun updateStartupStage(startupId: String, stage: String) {
        viewModelScope.launch { osRepository?.updateStartupStage(startupId, stage) }
    }

    fun updateStartupProjectType(startupId: String, projectType: String) {
        viewModelScope.launch { osRepository?.updateStartupProjectType(startupId, projectType) }
    }

    fun saveBusinessCrmDeals(startupId: String, deals: List<BusinessCrmDeal>) {
        viewModelScope.launch {
            osRepository?.saveBusinessCrmDeals(startupId, deals)
            _bannerMessage.value = "CRM pipeline saved"
        }
    }

    fun saveBusinessOkrs(startupId: String, okrs: List<BusinessOkrItem>) {
        viewModelScope.launch {
            osRepository?.saveBusinessOkrs(startupId, okrs)
            _bannerMessage.value = "OKR tracker updated"
        }
    }

    fun saveBusinessSopDocs(startupId: String, docs: List<BusinessSopDoc>) {
        viewModelScope.launch {
            osRepository?.saveBusinessSopDocs(startupId, docs)
            _bannerMessage.value = "Business SOPs saved"
        }
    }

    fun shareBusinessWorkflowToTeam(
        startupId: String,
        teamId: String,
        taggedMembers: List<TeamMemberEntity> = emptyList(),
        includeRoadmapTasks: Boolean = true,
        includeSopPage: Boolean = true,
        customHeadline: String = "",
        onDone: () -> Unit = {}
    ) {
        val repo = osRepository ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            repo.shareBusinessWorkflowToTeam(
                startupId = startupId,
                teamId = teamId,
                actorName = userDisplayName.value.ifBlank { "Founder" },
                taggedMembers = taggedMembers,
                includeRoadmapTasks = includeRoadmapTasks,
                includeSopPage = includeSopPage,
                customHeadline = customHeadline
            ).fold(
                onSuccess = { count ->
                    _bannerMessage.value = "Shared workflow ($count items) live to team!"
                    _activeTeamId.value = teamId
                    auth.currentUser?.uid?.let { uid -> startActiveTeamListeners(teamId, uid) }
                    onDone()
                },
                onFailure = { err ->
                    _bannerMessage.value = err.message ?: "Failed to share workflow with team"
                }
            )
            _teamBusy.value = false
        }
    }

    fun toggleStartupPhaseItem(item: StartupPhaseItemEntity) {
        viewModelScope.launch { osRepository?.toggleStartupPhaseItem(item) }
    }

    fun addCustomStartupPhaseItem(
        startupId: String,
        phaseNumber: Int,
        title: String,
        notes: String = "",
        resourceUrl: String? = null
    ) {
        viewModelScope.launch {
            osRepository?.addCustomStartupPhaseItem(startupId, phaseNumber, title, notes, resourceUrl)
            _bannerMessage.value = "Milestone step added"
        }
    }

    fun deleteStartupPhaseItem(item: StartupPhaseItemEntity) {
        viewModelScope.launch {
            osRepository?.deleteStartupPhaseItem(item)
            _bannerMessage.value = "Milestone step deleted"
        }
    }

    fun saveStartupSprintTask(
        startupId: String,
        id: String? = null,
        title: String,
        description: String,
        columnStatus: String,
        priority: String,
        tag: String
    ) {
        viewModelScope.launch {
            osRepository?.saveSprintTask(startupId, id, title, description, columnStatus, priority, tag)
            _bannerMessage.value = "Sprint task saved"
        }
    }

    fun moveSprintTaskStatus(task: StartupSprintTaskEntity, newColumn: String) {
        viewModelScope.launch { osRepository?.moveSprintTaskColumn(task, newColumn) }
    }

    fun deleteStartupSprintTask(task: StartupSprintTaskEntity) {
        viewModelScope.launch {
            osRepository?.deleteSprintTask(task)
            _bannerMessage.value = "Sprint task deleted"
        }
    }

    fun deleteStartupProject(startupId: String) {
        viewModelScope.launch {
            osRepository?.deleteStartupProject(startupId)
            _bannerMessage.value = "Startup venture deleted"
        }
    }

    fun generateStartupBlueprint(startupId: String) {
        val repo = osRepository ?: return
        viewModelScope.launch {
            _isGeneratingStartupPlan.value = true
            _aiStartupError.value = null
            val res = repo.generateAiStartupBlueprint(startupId)
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "AI Business Plan & Tech Spec generated!"
                },
                onFailure = { err ->
                    _aiStartupError.value = err.message ?: "Failed to generate startup blueprint"
                }
            )
            _isGeneratingStartupPlan.value = false
        }
    }

    // --- Money Management & Custom Sector Ledger ---
    fun recordRecentFinanceIcon(iconKey: String) {
        viewModelScope.launch {
            repository.recordRecentFinanceIcon(iconKey)
        }
    }

    fun createFinanceSector(
        name: String,
        iconKey: String,
        colorHex: String,
        typeAffinity: String
    ) {
        viewModelScope.launch {
            osRepository?.createCustomSector(name, iconKey, colorHex, typeAffinity)
            repository.recordRecentFinanceIcon(iconKey)
            _bannerMessage.value = "Custom sector \"$name\" created"
        }
    }

    fun deleteFinanceSector(sectorId: String) {
        viewModelScope.launch {
            osRepository?.deleteFinanceSector(sectorId)
            _bannerMessage.value = "Sector deleted"
        }
    }

    fun logFinanceTransaction(
        txType: String,
        amountCents: Long,
        sector: FinanceSectorEntity,
        paymentMethod: String,
        notes: String,
        occurredAtMillis: Long,
        iconKey: String? = null
    ) {
        viewModelScope.launch {
            val tx = osRepository?.logFinanceTransaction(txType, amountCents, sector, paymentMethod, notes, occurredAtMillis)
            val chosenIcon = iconKey?.trim()?.takeIf { it.isNotBlank() }
            if (chosenIcon != null) {
                repository.recordRecentFinanceIcon(chosenIcon)
                if (tx != null && chosenIcon != sector.iconKey) {
                    repository.setFinanceTransactionIcon(tx.id, chosenIcon)
                }
            } else {
                repository.recordRecentFinanceIcon(sector.iconKey)
            }
            _bannerMessage.value = "Transaction logged"
        }
    }

    fun deleteFinanceTransaction(txId: String) {
        viewModelScope.launch {
            osRepository?.deleteFinanceTransaction(txId)
            repository.removeFinanceTransactionIcon(txId)
            _bannerMessage.value = "Transaction deleted"
        }
    }

    // --- Real-Time Team & Group Collaboration ---
    fun selectActiveTeam(teamId: String) {
        _activeTeamId.value = teamId
        auth.currentUser?.uid?.let { uid ->
            startActiveTeamListeners(teamId, uid)
        }
    }

    fun clearTeamError() {
        _teamError.value = null
        _inviteLookupPreview.value = null
    }

    fun dismissTeamAlertBanner() {
        _latestTeamAlertBanner.value = null
    }

    fun createTeam(
        name: String,
        description: String,
        customInviteCode: String = "",
        maxMembers: Int = 5,
        onDone: () -> Unit = {}
    ) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            _teamError.value = null
            val teamId = UUID.randomUUID().toString()
            val code = customInviteCode.trim().uppercase().ifBlank {
                "DEEDOX-${(100..999).random()}"
            }
            val res = sync.createTeam(
                teamId = teamId,
                name = name,
                description = description,
                inviteCode = code,
                maxMembers = maxMembers,
                ownerName = userDisplayName.value,
                ownerEmail = userEmail.value.orEmpty(),
                ownerPhotoUrl = _googlePhotoUrl.value.orEmpty()
            )
            res.fold(
                onSuccess = { created ->
                    _activeTeamId.value = created.id
                    auth.currentUser?.uid?.let { uid -> startActiveTeamListeners(created.id, uid) }
                    _bannerMessage.value = "Team \"${created.name}\" created (Code: ${created.inviteCode})"
                    onDone()
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Could not create team"
                }
            )
            _teamBusy.value = false
        }
    }

    fun lookupInviteCode(code: String) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            _teamError.value = null
            _inviteLookupPreview.value = null
            val res = sync.lookupTeamByInviteCode(code)
            res.fold(
                onSuccess = { preview -> _inviteLookupPreview.value = preview },
                onFailure = { err -> _teamError.value = err.message ?: "Invite code not found" }
            )
            _teamBusy.value = false
        }
    }

    fun joinTeamByInviteCode(code: String, onDone: () -> Unit = {}) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            _teamError.value = null
            val res = sync.joinTeamByInviteCode(
                inviteCode = code,
                memberName = userDisplayName.value,
                memberEmail = userEmail.value.orEmpty(),
                memberPhotoUrl = _googlePhotoUrl.value.orEmpty()
            )
            res.fold(
                onSuccess = { joinedTeamId ->
                    _activeTeamId.value = joinedTeamId
                    auth.currentUser?.uid?.let { uid -> startActiveTeamListeners(joinedTeamId, uid) }
                    _inviteLookupPreview.value = null
                    _bannerMessage.value = "Joined team workspace!"
                    onDone()
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to join team"
                }
            )
            _teamBusy.value = false
        }
    }

    fun expandTeamLimit(team: TeamWorkspaceEntity, additionalSlots: Int = 5) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            val nextLimit = (team.maxMembers + additionalSlots).coerceIn(2, 100)
            val res = sync.expandTeamMaxMembers(team.id, team.inviteCode, nextLimit)
            res.fold(
                onSuccess = { _bannerMessage.value = "Team capacity expanded to $nextLimit members" },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to expand team limit" }
            )
        }
    }

    private fun recordTeamActivityAsync(
        teamId: String,
        actionType: String,
        summary: String,
        targetId: String = ""
    ) {
        val sync = cloudOsSync ?: return
        val uid = auth.currentUser?.uid ?: return
        val actor = userDisplayName.value.ifBlank { "Teammate" }
        viewModelScope.launch {
            sync.logTeamActivity(
                TeamActivityEntity(
                    teamId = teamId,
                    actorUid = uid,
                    actorName = actor,
                    actionType = actionType,
                    summary = summary,
                    targetId = targetId,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun inviteTeamMemberByEmail(
        team: TeamWorkspaceEntity,
        email: String,
        role: String = "MEMBER",
        roleTag: String = "Member",
        onDone: () -> Unit = {}
    ) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            _teamError.value = null
            sync.inviteTeamMemberByEmail(
                teamId = team.id,
                teamName = team.name,
                inviteCode = team.inviteCode,
                email = email,
                invitedRole = role,
                roleTag = roleTag,
                invitedByName = userDisplayName.value.ifBlank { "Team Admin" }
            ).fold(
                onSuccess = { invite ->
                    _bannerMessage.value = "Invite sent to ${invite.invitedEmail}"
                    recordTeamActivityAsync(
                        teamId = team.id,
                        actionType = "MEMBER",
                        summary = "Invited ${invite.invitedEmail} (${invite.roleTag})",
                        targetId = invite.id
                    )
                    onDone()
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to invite member by email"
                }
            )
            _teamBusy.value = false
        }
    }

    fun acceptEmailInvite(invite: TeamEmailInviteEntity, onDone: () -> Unit = {}) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            _teamError.value = null
            sync.acceptEmailInvite(
                invite = invite,
                memberName = userDisplayName.value,
                memberEmail = userEmail.value.orEmpty(),
                memberPhotoUrl = _googlePhotoUrl.value.orEmpty()
            ).fold(
                onSuccess = { joinedTeamId ->
                    _activeTeamId.value = joinedTeamId
                    auth.currentUser?.uid?.let { uid -> startActiveTeamListeners(joinedTeamId, uid) }
                    _bannerMessage.value = "Joined \"${invite.teamName}\"!"
                    recordTeamActivityAsync(
                        teamId = joinedTeamId,
                        actionType = "MEMBER",
                        summary = "${userDisplayName.value} joined via Gmail invite",
                        targetId = auth.currentUser?.uid.orEmpty()
                    )
                    onDone()
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to accept invite"
                }
            )
            _teamBusy.value = false
        }
    }

    fun revokeEmailInvite(invite: TeamEmailInviteEntity) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            sync.revokeEmailInvite(invite.id).fold(
                onSuccess = {
                    _bannerMessage.value = "Invite revoked for ${invite.invitedEmail}"
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to revoke invite"
                }
            )
        }
    }

    fun removeTeamMember(teamId: String, member: TeamMemberEntity) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            _teamBusy.value = true
            sync.removeTeamMember(teamId, member.uid).fold(
                onSuccess = {
                    _activeTeamMembers.value = _activeTeamMembers.value.filterNot { it.uid == member.uid }
                    osRepository?.removeCachedTeamMember(teamId, member.uid)
                    _bannerMessage.value = "Removed ${member.displayName} from team"
                    recordTeamActivityAsync(
                        teamId = teamId,
                        actionType = "MEMBER",
                        summary = "Removed ${member.displayName} from team",
                        targetId = member.uid
                    )
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to remove team member"
                }
            )
            _teamBusy.value = false
        }
    }

    fun updateTeamMemberProfile(
        teamId: String,
        member: TeamMemberEntity,
        roleTag: String,
        customStatus: String,
        newRole: String? = null
    ) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            sync.updateTeamMemberProfile(
                teamId = teamId,
                targetUid = member.uid,
                roleTag = roleTag,
                customStatus = customStatus,
                role = newRole
            ).fold(
                onSuccess = {
                    _activeTeamMembers.value = _activeTeamMembers.value.map { current ->
                        if (current.uid == member.uid && current.teamId == teamId) {
                            current.copy(
                                roleTag = roleTag.trim().take(60),
                                customStatus = customStatus.trim().take(80).ifBlank { "Available" },
                                role = newRole ?: current.role
                            )
                        } else current
                    }
                    _bannerMessage.value = "Updated ${member.displayName}'s role & status"
                    recordTeamActivityAsync(
                        teamId = teamId,
                        actionType = "MEMBER",
                        summary = "Updated ${member.displayName} (${newRole ?: member.role}${if (roleTag.isNotBlank()) " · $roleTag" else ""})",
                        targetId = member.uid
                    )
                },
                onFailure = { err ->
                    _teamError.value = err.message ?: "Failed to update member profile"
                }
            )
        }
    }

    fun saveSharedTask(
        teamId: String,
        title: String,
        description: String,
        assignee: TeamMemberEntity? = null,
        priority: String = "MEDIUM",
        dayOfWeek: String = "Mon",
        dateLabel: String = "",
        startTime: String = "10:00 AM",
        endTime: String = "12:00 PM",
        assignedMembers: List<TeamMemberEntity> = listOfNotNull(assignee),
        status: String = "todo",
        deadlineMillis: Long? = null,
        linkedDriveUrl: String? = null,
        sourceBusinessId: String? = null,
        existingTask: TeamSharedTaskEntity? = null
    ) {
        val sync = cloudOsSync ?: return
        val uid = auth.currentUser?.uid ?: return
        val effectiveMembers = assignedMembers.distinctBy { it.uid }
        val uidsCsv = effectiveMembers.joinToString(",") { it.uid }
        val namesCsv = effectiveMembers.joinToString(",") { it.displayName }
        val validStatus = if (status in setOf("backlog", "todo", "in_progress", "review", "done")) status else "todo"
        val resolvedLabel = dateLabel.trim().ifBlank { "$dayOfWeek · $startTime - $endTime" }

        viewModelScope.launch {
            val task = (existingTask?.copy(
                title = title,
                description = description,
                dayOfWeek = dayOfWeek,
                dateLabel = resolvedLabel,
                startTime = startTime,
                endTime = endTime,
                assigneeUid = effectiveMembers.firstOrNull()?.uid.orEmpty(),
                assigneeName = effectiveMembers.joinToString(", ") { it.displayName },
                assignedToUidsCsv = uidsCsv,
                assignedToNamesCsv = namesCsv,
                status = validStatus,
                priority = priority,
                isCompleted = validStatus == "done",
                deadlineMillis = deadlineMillis,
                linkedDriveUrl = linkedDriveUrl?.trim()?.takeIf { it.isNotBlank() },
                sourceBusinessId = sourceBusinessId ?: existingTask.sourceBusinessId,
                updatedAt = System.currentTimeMillis()
            ) ?: TeamSharedTaskEntity(
                teamId = teamId,
                title = title,
                description = description,
                dayOfWeek = dayOfWeek,
                dateLabel = resolvedLabel,
                startTime = startTime,
                endTime = endTime,
                assigneeUid = effectiveMembers.firstOrNull()?.uid.orEmpty(),
                assigneeName = effectiveMembers.joinToString(", ") { it.displayName },
                assignedToUidsCsv = uidsCsv,
                assignedToNamesCsv = namesCsv,
                status = validStatus,
                priority = priority,
                isCompleted = validStatus == "done",
                deadlineMillis = deadlineMillis,
                linkedDriveUrl = linkedDriveUrl?.trim()?.takeIf { it.isNotBlank() },
                sourceBusinessId = sourceBusinessId,
                createdByUid = uid
            ))

            sync.pushSharedTask(task).fold(
                onSuccess = {
                    _bannerMessage.value = if (existingTask != null) {
                        "Task updated"
                    } else {
                        "Created \"$title\" ($dayOfWeek)"
                    }
                    recordTeamActivityAsync(
                        teamId = teamId,
                        actionType = "TASK",
                        summary = if (existingTask != null) "Updated task \"${task.title}\"" else "Added task \"${task.title}\" ($dayOfWeek)",
                        targetId = task.id
                    )
                    val notifyTargets = effectiveMembers.filter { it.uid != uid }
                    if (existingTask == null && notifyTargets.isNotEmpty()) {
                        val mentions = notifyTargets.joinToString(" ") { "@${it.displayName}" }
                        sync.sendTeamNotification(
                            TeamNotificationEntity(
                                teamId = teamId,
                                senderUid = uid,
                                senderName = userDisplayName.value,
                                title = "Task Assigned: ${task.title}",
                                message = "$mentions • $dayOfWeek ($startTime – $endTime)",
                                taggedUidsCsv = notifyTargets.joinToString(",") { it.uid },
                                taggedNamesCsv = notifyTargets.joinToString(",") { it.displayName },
                                createdAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to save task" }
            )
        }
    }

    fun toggleSharedTask(task: TeamSharedTaskEntity) {
        val nextDone = !task.isCompleted && task.status != "done"
        setSharedTaskStatus(task, if (nextDone) "done" else "todo")
    }

    fun cycleSharedTaskStatus(task: TeamSharedTaskEntity) {
        val nextStatus = when (task.status) {
            "backlog" -> "todo"
            "todo" -> "in_progress"
            "in_progress" -> "review"
            "review" -> "done"
            else -> "backlog"
        }
        setSharedTaskStatus(task, nextStatus)
    }

    fun setSharedTaskStatus(task: TeamSharedTaskEntity, nextStatus: String) {
        val sync = cloudOsSync ?: return
        val valid = if (nextStatus in setOf("backlog", "todo", "in_progress", "review", "done")) nextStatus else "todo"
        viewModelScope.launch {
            sync.pushSharedTask(
                task.copy(
                    status = valid,
                    isCompleted = valid == "done",
                    updatedAt = System.currentTimeMillis()
                )
            ).onSuccess {
                recordTeamActivityAsync(
                    teamId = task.teamId,
                    actionType = "TASK",
                    summary = "Moved \"${task.title}\" -> ${com.example.data.repository.TeamAnalyticsCalculator.statusLabel(valid)}",
                    targetId = task.id
                )
            }
        }
    }

    fun deleteSharedTask(task: TeamSharedTaskEntity) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            sync.deleteSharedTask(task.teamId, task.id).fold(
                onSuccess = {
                    _bannerMessage.value = "Task deleted"
                    recordTeamActivityAsync(
                        teamId = task.teamId,
                        actionType = "TASK",
                        summary = "Deleted task \"${task.title}\"",
                        targetId = task.id
                    )
                },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to delete task" }
            )
        }
    }

    fun saveSharedDoc(
        teamId: String,
        title: String,
        content: String,
        resourceUrl: String?,
        blocksJson: String = "[]",
        tagsCsv: String = "",
        parentId: String? = null,
        icon: String = "📄",
        isPinned: Boolean = false,
        existingDoc: TeamSharedDocEntity? = null
    ) {
        val sync = cloudOsSync ?: return
        val uid = auth.currentUser?.uid ?: return
        val editorName = userDisplayName.value.ifBlank { "Teammate" }
        viewModelScope.launch {
            val doc = existingDoc?.copy(
                title = title,
                content = content,
                blocksJson = blocksJson,
                tagsCsv = tagsCsv,
                resourceUrl = resourceUrl?.trim()?.takeIf { it.isNotBlank() },
                parentId = parentId?.trim()?.takeIf { it.isNotBlank() && it != existingDoc.id },
                icon = icon.ifBlank { "📄" },
                isPinned = isPinned,
                lastEditedBy = editorName,
                updatedAt = System.currentTimeMillis()
            ) ?: TeamSharedDocEntity(
                teamId = teamId,
                title = title,
                content = content,
                blocksJson = blocksJson,
                tagsCsv = tagsCsv,
                resourceUrl = resourceUrl?.trim()?.takeIf { it.isNotBlank() },
                parentId = parentId?.trim()?.takeIf { it.isNotBlank() },
                icon = icon.ifBlank { "📄" },
                isPinned = isPinned,
                authorUid = uid,
                authorName = editorName,
                lastEditedBy = editorName
            )
            sync.pushSharedDoc(doc).fold(
                onSuccess = {
                    _bannerMessage.value = if (existingDoc != null) "Page updated" else "Page published to team"
                    recordTeamActivityAsync(
                        teamId = teamId,
                        actionType = "PAGE",
                        summary = if (existingDoc != null) "Edited page \"${doc.title}\"" else "Created page ${doc.icon} \"${doc.title}\"",
                        targetId = doc.id
                    )
                },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to save shared page" }
            )
        }
    }

    fun togglePinSharedDoc(doc: TeamSharedDocEntity) {
        val sync = cloudOsSync ?: return
        val editorName = userDisplayName.value.ifBlank { "Teammate" }
        viewModelScope.launch {
            sync.pushSharedDoc(
                doc.copy(
                    isPinned = !doc.isPinned,
                    lastEditedBy = editorName,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteSharedDoc(doc: TeamSharedDocEntity) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            sync.deleteSharedDoc(doc.teamId, doc.id).fold(
                onSuccess = {
                    _bannerMessage.value = "Shared page deleted"
                    recordTeamActivityAsync(
                        teamId = doc.teamId,
                        actionType = "PAGE",
                        summary = "Deleted page \"${doc.title}\"",
                        targetId = doc.id
                    )
                },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to delete shared page" }
            )
        }
    }

    fun sendTeamAlert(
        teamId: String,
        title: String,
        message: String,
        taggedMembers: List<TeamMemberEntity> = emptyList(),
        noticeType: String = "notice",
        pollOptionsJson: String = "[]"
    ) {
        val sync = cloudOsSync ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val notif = TeamNotificationEntity(
                teamId = teamId,
                senderUid = uid,
                senderName = userDisplayName.value,
                title = title,
                message = message,
                taggedUidsCsv = taggedMembers.joinToString(",") { it.uid },
                taggedNamesCsv = taggedMembers.joinToString(",") { it.displayName },
                noticeType = if (noticeType == "poll") "poll" else "notice",
                pollOptionsJson = pollOptionsJson,
                pollVotesJson = "{}",
                createdAtMillis = System.currentTimeMillis()
            )
            sync.sendTeamNotification(notif).fold(
                onSuccess = {
                    _bannerMessage.value = when {
                        notif.noticeType == "poll" -> "Interactive team poll posted!"
                        taggedMembers.isEmpty() -> "Notice posted to all team devices!"
                        else -> "Alert pushed to ${taggedMembers.joinToString(", ") { "@${it.displayName}" }}!"
                    }
                    recordTeamActivityAsync(
                        teamId = teamId,
                        actionType = "NOTICE",
                        summary = if (notif.noticeType == "poll") "Created poll \"${notif.title}\"" else "Posted notice \"${notif.title}\"",
                        targetId = notif.id
                    )
                },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to send alert" }
            )
        }
    }

    fun voteOnTeamPoll(teamId: String, noticeId: String, optionIndex: Int) {
        val sync = cloudOsSync ?: return
        viewModelScope.launch {
            sync.voteOnTeamPoll(teamId, noticeId, optionIndex).fold(
                onSuccess = { _bannerMessage.value = "Vote recorded!" },
                onFailure = { err -> _teamError.value = err.message ?: "Failed to cast vote" }
            )
        }
    }

    // --- Infinite Smart Notion Canvas Action Bridges ---
    suspend fun polishCanvasPageWithAi(
        title: String,
        blocks: List<com.example.data.repository.WorkspaceBlock>,
        mode: AiPagePolishMode
    ): Result<List<com.example.data.repository.WorkspaceBlock>> {
        val raw = com.example.data.repository.WorkspaceBlock.plainTextSummary(
            com.example.data.repository.WorkspaceBlock.encodeList(blocks)
        )
        return com.example.data.ai.AiLibraryService().polishPageWithAi(title, raw, mode)
    }

    fun recordCanvasClientPaymentToLedger(
        clientName: String,
        projectName: String,
        amountCents: Long
    ) {
        if (amountCents <= 0L) {
            _bannerMessage.value = "Enter an advance amount greater than $0.00 to log."
            return
        }
        viewModelScope.launch {
            val sectors = financeSectors.value
            val sector = sectors.firstOrNull { it.typeAffinity == "INCOME" && it.name.contains("Client", ignoreCase = true) }
                ?: sectors.firstOrNull { it.typeAffinity == "INCOME" || it.typeAffinity == "BOTH" }
                ?: FinanceSectorEntity(
                    id = "sec_client_canvas",
                    name = "Client Payment",
                    iconKey = "briefcase",
                    colorHex = "#E53935",
                    typeAffinity = "INCOME",
                    isCustom = false
                )
            val noteLabel = buildString {
                append("Client: ").append(clientName.ifBlank { "Client" })
                if (projectName.isNotBlank()) append(" • ").append(projectName)
            }
            osRepository?.logFinanceTransaction(
                txType = "INCOME",
                amountCents = amountCents,
                sector = sector,
                paymentMethod = "BANK",
                notes = noteLabel,
                occurredAtMillis = System.currentTimeMillis()
            )
            _bannerMessage.value = "Client payment logged to Money Ledger"
        }
    }

    fun createCanvasInlineReminder(
        title: String,
        triggerAtMillis: Long,
        ringtoneTitle: String,
        noteId: String? = null,
        teamId: String? = null,
        taggedMemberName: String? = null,
        taggedMemberUid: String? = null
    ) {
        val cleanTitle = title.trim().ifBlank { "Page Reminder" }
        saveSmartReminder(
            mode = "PERSONAL",
            title = cleanTitle,
            notes = if (!taggedMemberName.isNullOrBlank()) "Tagged @$taggedMemberName from Smart Canvas" else "Created inside Smart Canvas page",
            priority = "HIGH",
            triggerAtMillis = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 30_000L),
            recurrenceType = com.example.reminders.ReminderScheduler.RECURRENCE_ONCE,
            intervalMinutes = 0,
            ringtoneUri = null,
            ringtoneTitle = ringtoneTitle.ifBlank { "Default System Sound" },
            cloudSync = true,
            targetNoteId = noteId
        )
        if (!teamId.isNullOrBlank()) {
            val taggedMember = activeTeamMembers.value.firstOrNull {
                (!taggedMemberUid.isNullOrBlank() && it.uid == taggedMemberUid) ||
                    (!taggedMemberName.isNullOrBlank() && it.displayName.equals(taggedMemberName, ignoreCase = true))
            }
            sendTeamAlert(
                teamId = teamId,
                title = "Page Reminder: $cleanTitle",
                message = "Reminder scheduled ($ringtoneTitle)${if (!taggedMemberName.isNullOrBlank()) " for @$taggedMemberName" else ""}",
                taggedMembers = listOfNotNull(taggedMember)
            )
        }
    }

    fun syncCanvasScheduleBlock(
        teamId: String?,
        schedule: com.example.data.repository.ScheduleBlockData,
        members: List<TeamMemberEntity>
    ) {
        val cleanTitle = schedule.title.trim().ifBlank { "Scheduled Page Task" }
        if (!teamId.isNullOrBlank()) {
            val teamStatus = when (schedule.status) {
                com.example.data.repository.ScheduleBlockData.STATUS_WORKING -> "in_progress"
                com.example.data.repository.ScheduleBlockData.STATUS_DONE -> "done"
                else -> "todo"
            }
            val matchedMember = members.firstOrNull {
                (schedule.memberUid.isNotBlank() && it.uid == schedule.memberUid) ||
                    (schedule.memberName.isNotBlank() && it.displayName.equals(schedule.memberName, ignoreCase = true))
            }
            val parts = schedule.timeSlot.split("-").map { it.trim() }
            saveSharedTask(
                teamId = teamId,
                title = cleanTitle,
                description = "Synced from Team Notion Page",
                priority = "HIGH",
                dayOfWeek = schedule.dayOfWeek,
                dateLabel = "${schedule.dayOfWeek} • ${schedule.timeSlot}",
                startTime = parts.getOrNull(0).orEmpty().ifBlank { "10:00 AM" },
                endTime = parts.getOrNull(1).orEmpty().ifBlank { "1:00 PM" },
                assignedMembers = listOfNotNull(matchedMember),
                status = teamStatus
            )
        } else {
            saveWorkspaceTask(
                title = "$cleanTitle (${schedule.dayOfWeek} ${schedule.timeSlot})",
                notes = "Synced from Notion Page • Status: ${schedule.status}",
                priority = "HIGH",
                cloudSync = true
            )
        }
    }

    fun logCanvasFocusTimerSession(label: String, durationSeconds: Long) {
        val mins = (durationSeconds / 60L).toInt().coerceAtLeast(1)
        startFocusSession(label = label.ifBlank { "Notion Canvas Focus" }, plannedMinutes = mins)
        completeFocusSession(emergencyUnlocked = false)
        _bannerMessage.value = "Logged ${mins}m focus time to session history"
    }

    fun startInlineCanvasVoiceRecording() {
        voiceAudioEngine?.startRecording()
    }

    fun stopInlineCanvasVoiceRecording(): Pair<String, Long>? {
        return voiceAudioEngine?.stopAndSaveRecording()
    }
}
