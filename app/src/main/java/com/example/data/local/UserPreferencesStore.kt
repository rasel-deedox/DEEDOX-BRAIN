package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

private val Context.liquidDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_library_prefs")

@androidx.compose.runtime.Immutable
data class SavedAccountRecord(
    val uid: String,
    val displayName: String,
    val email: String,
    val lastUsedAt: Long
)

@androidx.compose.runtime.Immutable
data class SupabaseSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String = "",
    val expiresAtMillis: Long = 0L
)

@androidx.compose.runtime.Immutable
data class OnboardingProfile(
    val role: String = "",
    val primaryUseCase: String = "",
    val saveHabit: String = "",
    val workflowPreset: String = "starter_folders",
    val wizardCompleted: Boolean = false
)

data class LibraryReview(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String? = null,
    val authorName: String,
    val rating: Int,
    val body: String,
    val helpfulCount: Int = 0,
    val isHelpfulByMe: Boolean = false,
    val verified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@androidx.compose.runtime.Immutable
data class DetoxSessionState(
    val isActive: Boolean = false,
    val durationHours: Int = 2,
    val startedAtMillis: Long = 0L,
    val endsAtMillis: Long = 0L,
    val greyscaleUi: Boolean = true,
    val suppressNotifications: Boolean = true,
    val whitelistedPackagesCsv: String = ""
) {
    fun isRunningAt(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return isActive && endsAtMillis > nowMillis
    }

    fun whitelistedSet(): Set<String> {
        if (whitelistedPackagesCsv.isBlank()) return emptySet()
        return whitelistedPackagesCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
}

object DetoxRuntimeStore {
    private const val PREFS_NAME = "monk_detox_prefs"
    private const val KEY_ACTIVE = "detox_active"
    private const val KEY_HOURS = "detox_hours"
    private const val KEY_STARTED = "detox_started_at"
    private const val KEY_ENDS = "detox_ends_at"
    private const val KEY_GREY = "detox_grey"
    private const val KEY_SUPPRESS = "detox_suppress"
    private const val KEY_WHITELIST = "detox_whitelist"

    fun write(context: Context, state: DetoxSessionState) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, state.isActive)
            .putInt(KEY_HOURS, state.durationHours)
            .putLong(KEY_STARTED, state.startedAtMillis)
            .putLong(KEY_ENDS, state.endsAtMillis)
            .putBoolean(KEY_GREY, state.greyscaleUi)
            .putBoolean(KEY_SUPPRESS, state.suppressNotifications)
            .putString(KEY_WHITELIST, state.whitelistedPackagesCsv)
            .apply()
    }

    fun read(context: Context, nowMillis: Long = System.currentTimeMillis()): DetoxSessionState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        val ends = prefs.getLong(KEY_ENDS, 0L)
        if (!active || ends <= nowMillis) {
            return DetoxSessionState()
        }
        return DetoxSessionState(
            isActive = true,
            durationHours = prefs.getInt(KEY_HOURS, 2),
            startedAtMillis = prefs.getLong(KEY_STARTED, 0L),
            endsAtMillis = ends,
            greyscaleUi = prefs.getBoolean(KEY_GREY, true),
            suppressNotifications = prefs.getBoolean(KEY_SUPPRESS, true),
            whitelistedPackagesCsv = prefs.getString(KEY_WHITELIST, "").orEmpty()
        )
    }

    fun clear(context: Context) {
        write(context, DetoxSessionState())
    }
}

data class CustomCredentialEntry(
    val id: String = UUID.randomUUID().toString(),
    val keyName: String,
    val keyValue: String,
    val note: String = ""
)

data class AdminVaultConfig(
    val aiProvider: String = PROVIDER_FIREBASE_AI,
    val openRouterApiKey: String = "",
    val openRouterModel: String = DEFAULT_OPENROUTER_MODEL,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val customCredentials: List<CustomCredentialEntry> = emptyList(),
    val adminPasscodeHash: String? = null,
    val adminPasscodeSalt: String? = null,
    val lastSyncedAt: Long? = null,
    val providerManuallySelected: Boolean = false
) {
    val hasAdminPasscode: Boolean
        get() = !adminPasscodeHash.isNullOrBlank() && !adminPasscodeSalt.isNullOrBlank()

    fun getCustomCredential(keyName: String): String? =
        customCredentials.firstOrNull { it.keyName.equals(keyName.trim(), ignoreCase = true) }
            ?.keyValue
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val PROVIDER_FIREBASE_AI = "firebase_ai"
        const val PROVIDER_OPENROUTER = "openrouter"
        const val DEFAULT_OPENROUTER_MODEL = "openrouter/free"
        private const val ENV_PLACEHOLDER = "MY_OPENROUTER_API_KEY"

        val FREE_OPENROUTER_PRESETS: List<Pair<String, String>> = listOf(
            "openrouter/free" to "Auto Free ($0)",
            "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B Free",
            "google/gemma-3-27b-it:free" to "Gemma 3 27B Free",
            "mistralai/mistral-small-3.1-24b-instruct:free" to "Mistral Small 24B Free",
            "nvidia/nemotron-3-super-120b-a12b:free" to "Nemotron 120B Free"
        )

        fun buildConfigOpenRouterApiKey(): String {
            val raw = BuildConfig.OPENROUTER_API_KEY.trim()
            return if (raw.isBlank() || raw == ENV_PLACEHOLDER) "" else raw
        }

        fun enforceFreeOpenRouterModel(rawModel: String?): String {
            val trimmed = rawModel?.trim().orEmpty()
            if (trimmed.isBlank()) return DEFAULT_OPENROUTER_MODEL
            if (trimmed.equals(DEFAULT_OPENROUTER_MODEL, ignoreCase = true)) return DEFAULT_OPENROUTER_MODEL
            if (trimmed.endsWith(":free", ignoreCase = true)) return trimmed
            return DEFAULT_OPENROUTER_MODEL
        }

        fun resolveEffective(config: AdminVaultConfig): AdminVaultConfig {
            val envKey = buildConfigOpenRouterApiKey()
            val effectiveKey = config.openRouterApiKey.trim().ifBlank { envKey }
            val effectiveModel = enforceFreeOpenRouterModel(config.openRouterModel)
            val effectiveProvider = when {
                config.aiProvider == PROVIDER_OPENROUTER -> PROVIDER_OPENROUTER
                effectiveKey.isNotBlank() && !config.providerManuallySelected -> PROVIDER_OPENROUTER
                else -> PROVIDER_FIREBASE_AI
            }
            return config.copy(
                aiProvider = effectiveProvider,
                openRouterApiKey = effectiveKey,
                openRouterModel = effectiveModel
            )
        }
    }
}

object AdminPasscodeCrypto {
    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPasscode(passcode: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$saltHex:$passcode".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(passcode: String, expectedHash: String?, saltHex: String?): Boolean {
        if (expectedHash.isNullOrBlank() || saltHex.isNullOrBlank()) return false
        val actual = hashPasscode(passcode, saltHex)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}

class UserPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val SAVED_ACCOUNTS_JSON = stringPreferencesKey("saved_accounts_json")
        val REVIEWS_JSON = stringPreferencesKey("reviews_json")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val VAULT_LOCKED = stringPreferencesKey("vault_locked")
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
        val PROFILE_AVATAR = stringPreferencesKey("profile_avatar")
        val ADMIN_CONFIG_JSON = stringPreferencesKey("admin_config_json")
        val SUPABASE_SESSION_JSON = stringPreferencesKey("supabase_session_json")
        val ONBOARDING_PROFILE_JSON = stringPreferencesKey("onboarding_profile_json")
        val DETOX_SESSION_JSON = stringPreferencesKey("detox_session_json")
        val RECENT_FINANCE_ICONS_JSON = stringPreferencesKey("recent_finance_icons_json")
        val FINANCE_TX_ICONS_JSON = stringPreferencesKey("finance_tx_icons_json")
    }

    val themeMode: Flow<AppThemeMode> = context.liquidDataStore.data.map { prefs ->
        AppThemeMode.fromCode(prefs[Keys.THEME_MODE])
    }

    val accentPreset: Flow<LiquidAccentPreset> = context.liquidDataStore.data.map { prefs ->
        LiquidAccentPreset.fromCode(prefs[Keys.ACCENT_PRESET])
    }

    val customDisplayName: Flow<String?> = context.liquidDataStore.data.map { prefs ->
        prefs[Keys.PROFILE_DISPLAY_NAME]?.takeIf { it.isNotBlank() }
    }

    val customEmail: Flow<String?> = context.liquidDataStore.data.map { prefs ->
        prefs[Keys.PROFILE_EMAIL]?.takeIf { it.isNotBlank() }
    }

    val profileAvatar: Flow<String> = context.liquidDataStore.data.map { prefs ->
        prefs[Keys.PROFILE_AVATAR]?.takeIf { it.isNotBlank() } ?: "preset:terracotta"
    }

    val vaultPin: Flow<String?> = context.liquidDataStore.data.map { prefs ->
        prefs[Keys.VAULT_PIN]?.takeIf { it.length == 4 }
    }

    val isVaultLocked: Flow<Boolean> = context.liquidDataStore.data.map { prefs ->
        val hasPin = prefs[Keys.VAULT_PIN]?.length == 4
        hasPin && prefs[Keys.VAULT_LOCKED] == "true"
    }

    val savedAccounts: Flow<List<SavedAccountRecord>> = context.liquidDataStore.data.map { prefs ->
        parseAccounts(prefs[Keys.SAVED_ACCOUNTS_JSON])
    }

    val localReviews: Flow<List<LibraryReview>> = context.liquidDataStore.data.map { prefs ->
        parseReviews(prefs[Keys.REVIEWS_JSON]).sortedByDescending { it.createdAt }
    }

    val adminVaultConfig: Flow<AdminVaultConfig> = context.liquidDataStore.data.map { prefs ->
        AdminVaultConfig.resolveEffective(parseAdminConfig(prefs[Keys.ADMIN_CONFIG_JSON]))
    }

    val supabaseSession: Flow<SupabaseSession?> = context.liquidDataStore.data.map { prefs ->
        parseSupabaseSession(prefs[Keys.SUPABASE_SESSION_JSON])
    }

    val onboardingProfile: Flow<OnboardingProfile> = context.liquidDataStore.data.map { prefs ->
        parseOnboardingProfile(prefs[Keys.ONBOARDING_PROFILE_JSON])
    }

    val detoxSessionState: Flow<DetoxSessionState> = context.liquidDataStore.data.map { prefs ->
        parseDetoxState(prefs[Keys.DETOX_SESSION_JSON])
    }

    val recentFinanceIconKeys: Flow<List<String>> = context.liquidDataStore.data.map { prefs ->
        parseStringList(prefs[Keys.RECENT_FINANCE_ICONS_JSON]).take(5)
    }

    val financeTxIconOverrides: Flow<Map<String, String>> = context.liquidDataStore.data.map { prefs ->
        parseStringMap(prefs[Keys.FINANCE_TX_ICONS_JSON])
    }

    suspend fun recordRecentFinanceIcon(iconKey: String) {
        val clean = iconKey.trim().lowercase().take(48)
        if (clean.isBlank()) return
        context.liquidDataStore.edit { prefs ->
            val current = parseStringList(prefs[Keys.RECENT_FINANCE_ICONS_JSON])
            val updated = (listOf(clean) + current.filterNot { it == clean }).take(5)
            prefs[Keys.RECENT_FINANCE_ICONS_JSON] = JSONArray(updated).toString()
        }
    }

    suspend fun setFinanceTransactionIcon(txId: String, iconKey: String) {
        val cleanId = txId.trim()
        val cleanIcon = iconKey.trim().lowercase().take(48)
        if (cleanId.isBlank() || cleanIcon.isBlank()) return
        context.liquidDataStore.edit { prefs ->
            val current = parseStringMap(prefs[Keys.FINANCE_TX_ICONS_JSON]).toMutableMap()
            current[cleanId] = cleanIcon
            if (current.size > 500) {
                val trimKeys = current.keys.take(current.size - 500)
                trimKeys.forEach { current.remove(it) }
            }
            val obj = JSONObject()
            current.forEach { (k, v) -> obj.put(k, v) }
            prefs[Keys.FINANCE_TX_ICONS_JSON] = obj.toString()
        }
    }

    suspend fun removeFinanceTransactionIcon(txId: String) {
        val cleanId = txId.trim()
        if (cleanId.isBlank()) return
        context.liquidDataStore.edit { prefs ->
            val current = parseStringMap(prefs[Keys.FINANCE_TX_ICONS_JSON]).toMutableMap()
            if (current.remove(cleanId) != null) {
                val obj = JSONObject()
                current.forEach { (k, v) -> obj.put(k, v) }
                prefs[Keys.FINANCE_TX_ICONS_JSON] = obj.toString()
            }
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.liquidDataStore.data.first()[Keys.DEVICE_ID]
        if (!current.isNullOrBlank()) return current
        val created = "android_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = created
        }
        return created
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.code
        }
    }

    suspend fun setAccentPreset(preset: LiquidAccentPreset) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.ACCENT_PRESET] = preset.code
        }
    }

    suspend fun setCustomDisplayName(name: String) {
        context.liquidDataStore.edit { prefs ->
            val trimmed = name.trim().take(120)
            if (trimmed.isBlank()) {
                prefs.remove(Keys.PROFILE_DISPLAY_NAME)
            } else {
                prefs[Keys.PROFILE_DISPLAY_NAME] = trimmed
            }
        }
    }

    suspend fun setCustomEmail(email: String) {
        context.liquidDataStore.edit { prefs ->
            val trimmed = email.trim().take(160)
            if (trimmed.isBlank()) {
                prefs.remove(Keys.PROFILE_EMAIL)
            } else {
                prefs[Keys.PROFILE_EMAIL] = trimmed
            }
        }
    }

    suspend fun setProfileAvatar(avatarValue: String) {
        context.liquidDataStore.edit { prefs ->
            val trimmed = avatarValue.trim()
            if (trimmed.isBlank()) {
                prefs[Keys.PROFILE_AVATAR] = "preset:terracotta"
            } else {
                prefs[Keys.PROFILE_AVATAR] = trimmed
            }
        }
    }

    suspend fun setVaultPin(pin: String?) {
        context.liquidDataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.VAULT_PIN)
                prefs[Keys.VAULT_LOCKED] = "false"
            } else {
                prefs[Keys.VAULT_PIN] = pin.take(4)
                prefs[Keys.VAULT_LOCKED] = "false"
            }
        }
    }

    suspend fun setVaultLocked(locked: Boolean) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.VAULT_LOCKED] = if (locked) "true" else "false"
        }
    }

    suspend fun saveAdminVaultConfig(config: AdminVaultConfig) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.ADMIN_CONFIG_JSON] = encodeAdminConfig(config)
        }
    }

    suspend fun getCurrentAdminVaultConfig(): AdminVaultConfig {
        val raw = context.liquidDataStore.data.first()[Keys.ADMIN_CONFIG_JSON]
        return AdminVaultConfig.resolveEffective(parseAdminConfig(raw))
    }

    suspend fun saveSupabaseSession(session: SupabaseSession) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.SUPABASE_SESSION_JSON] = encodeSupabaseSession(session)
        }
    }

    suspend fun clearSupabaseSession() {
        context.liquidDataStore.edit { prefs ->
            prefs.remove(Keys.SUPABASE_SESSION_JSON)
        }
    }

    suspend fun getCurrentSupabaseSession(): SupabaseSession? {
        val raw = context.liquidDataStore.data.first()[Keys.SUPABASE_SESSION_JSON]
        return parseSupabaseSession(raw)
    }

    suspend fun saveOnboardingProfile(profile: OnboardingProfile) {
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_PROFILE_JSON] = encodeOnboardingProfile(profile)
        }
    }

    suspend fun saveDetoxSessionState(state: DetoxSessionState) {
        DetoxRuntimeStore.write(context, state)
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.DETOX_SESSION_JSON] = encodeDetoxState(state)
        }
    }

    suspend fun clearDetoxSessionState() {
        val cleared = DetoxSessionState()
        DetoxRuntimeStore.clear(context)
        context.liquidDataStore.edit { prefs ->
            prefs[Keys.DETOX_SESSION_JSON] = encodeDetoxState(cleared)
        }
    }

    suspend fun recordAccount(uid: String, displayName: String, email: String) {
        context.liquidDataStore.edit { prefs ->
            val list = parseAccounts(prefs[Keys.SAVED_ACCOUNTS_JSON]).toMutableList()
            list.removeAll { it.uid == uid }
            list.add(
                0,
                SavedAccountRecord(
                    uid = uid,
                    displayName = displayName.ifBlank { "Library User" },
                    email = email,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
            prefs[Keys.SAVED_ACCOUNTS_JSON] = encodeAccounts(list.take(6))
        }
    }

    suspend fun saveOrUpdateReview(review: LibraryReview) {
        context.liquidDataStore.edit { prefs ->
            val list = parseReviews(prefs[Keys.REVIEWS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.id == review.id }
            if (idx >= 0) {
                list[idx] = review
            } else {
                list.add(0, review)
            }
            prefs[Keys.REVIEWS_JSON] = encodeReviews(list.take(100))
        }
    }

    suspend fun toggleReviewHelpful(reviewId: String): LibraryReview? {
        var updatedReview: LibraryReview? = null
        context.liquidDataStore.edit { prefs ->
            val list = parseReviews(prefs[Keys.REVIEWS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.id == reviewId }
            if (idx >= 0) {
                val cur = list[idx]
                val nowHelpful = !cur.isHelpfulByMe
                val nextCount = if (nowHelpful) cur.helpfulCount + 1 else (cur.helpfulCount - 1).coerceAtLeast(0)
                updatedReview = cur.copy(isHelpfulByMe = nowHelpful, helpfulCount = nextCount)
                list[idx] = updatedReview!!
                prefs[Keys.REVIEWS_JSON] = encodeReviews(list)
            }
        }
        return updatedReview
    }

    suspend fun mergeCloudReviews(cloudReviews: List<LibraryReview>) {
        if (cloudReviews.isEmpty()) return
        context.liquidDataStore.edit { prefs ->
            val local = parseReviews(prefs[Keys.REVIEWS_JSON]).associateBy { it.id }.toMutableMap()
            cloudReviews.forEach { cloud ->
                val existing = local[cloud.id]
                local[cloud.id] = if (existing != null) {
                    cloud.copy(
                        isHelpfulByMe = existing.isHelpfulByMe,
                        helpfulCount = maxOf(cloud.helpfulCount, existing.helpfulCount)
                    )
                } else {
                    cloud
                }
            }
            prefs[Keys.REVIEWS_JSON] = encodeReviews(local.values.sortedByDescending { it.createdAt }.take(100))
        }
    }

    private fun parseAccounts(json: String?): List<SavedAccountRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SavedAccountRecord(
                    uid = o.optString("uid"),
                    displayName = o.optString("displayName"),
                    email = o.optString("email"),
                    lastUsedAt = o.optLong("lastUsedAt", 0L)
                )
            }.filter { it.uid.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeAccounts(list: List<SavedAccountRecord>): String {
        val arr = JSONArray()
        list.forEach { acc ->
            arr.put(
                JSONObject().apply {
                    put("uid", acc.uid)
                    put("displayName", acc.displayName)
                    put("email", acc.email)
                    put("lastUsedAt", acc.lastUsedAt)
                }
            )
        }
        return arr.toString()
    }

    private fun parseReviews(json: String?): List<LibraryReview> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                LibraryReview(
                    id = o.optString("id"),
                    itemId = o.optString("itemId").takeIf { it.isNotBlank() },
                    authorName = o.optString("authorName", "Library Member"),
                    rating = o.optInt("rating", 5).coerceIn(1, 5),
                    body = o.optString("body"),
                    helpfulCount = o.optInt("helpfulCount", 0).coerceAtLeast(0),
                    isHelpfulByMe = o.optBoolean("isHelpfulByMe", false),
                    verified = o.optBoolean("verified", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }.filter { it.id.isNotBlank() && it.body.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeReviews(list: List<LibraryReview>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("itemId", r.itemId ?: "")
                    put("authorName", r.authorName)
                    put("rating", r.rating)
                    put("body", r.body)
                    put("helpfulCount", r.helpfulCount)
                    put("isHelpfulByMe", r.isHelpfulByMe)
                    put("verified", r.verified)
                    put("createdAt", r.createdAt)
                }
            )
        }
        return arr.toString()
    }

    companion object {
        fun parseCustomCredentials(json: String?): List<CustomCredentialEntry> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    CustomCredentialEntry(
                        id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                        keyName = o.optString("keyName"),
                        keyValue = o.optString("keyValue"),
                        note = o.optString("note")
                    )
                }.filter { it.keyName.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeCustomCredentials(list: List<CustomCredentialEntry>): String {
            val arr = JSONArray()
            list.forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("keyName", entry.keyName)
                        put("keyValue", entry.keyValue)
                        put("note", entry.note)
                    }
                )
            }
            return arr.toString()
        }

        private fun parseAdminConfig(json: String?): AdminVaultConfig {
            if (json.isNullOrBlank()) return AdminVaultConfig()
            return try {
                val o = JSONObject(json)
                AdminVaultConfig(
                    aiProvider = o.optString("aiProvider", AdminVaultConfig.PROVIDER_FIREBASE_AI)
                        .takeIf { it in setOf(AdminVaultConfig.PROVIDER_FIREBASE_AI, AdminVaultConfig.PROVIDER_OPENROUTER) }
                        ?: AdminVaultConfig.PROVIDER_FIREBASE_AI,
                    openRouterApiKey = o.optString("openRouterApiKey"),
                    openRouterModel = AdminVaultConfig.enforceFreeOpenRouterModel(
                        o.optString("openRouterModel", AdminVaultConfig.DEFAULT_OPENROUTER_MODEL)
                    ),
                    supabaseUrl = o.optString("supabaseUrl"),
                    supabaseAnonKey = o.optString("supabaseAnonKey"),
                    customCredentials = parseCustomCredentials(o.optString("customCredentialsJson")),
                    adminPasscodeHash = o.optString("adminPasscodeHash").takeIf { it.isNotBlank() },
                    adminPasscodeSalt = o.optString("adminPasscodeSalt").takeIf { it.isNotBlank() },
                    lastSyncedAt = o.optLong("lastSyncedAt", 0L).takeIf { it > 0L },
                    providerManuallySelected = o.optBoolean("providerManuallySelected", false)
                )
            } catch (_: Exception) {
                AdminVaultConfig()
            }
        }

        private fun encodeAdminConfig(c: AdminVaultConfig): String {
            return JSONObject().apply {
                put("aiProvider", c.aiProvider)
                put("openRouterApiKey", c.openRouterApiKey)
                put("openRouterModel", c.openRouterModel)
                put("supabaseUrl", c.supabaseUrl)
                put("supabaseAnonKey", c.supabaseAnonKey)
                put("customCredentialsJson", encodeCustomCredentials(c.customCredentials))
                put("adminPasscodeHash", c.adminPasscodeHash ?: "")
                put("adminPasscodeSalt", c.adminPasscodeSalt ?: "")
                put("lastSyncedAt", c.lastSyncedAt ?: 0L)
                put("providerManuallySelected", c.providerManuallySelected)
            }.toString()
        }

        private fun parseSupabaseSession(json: String?): SupabaseSession? {
            if (json.isNullOrBlank()) return null
            return try {
                val o = JSONObject(json)
                val userId = o.optString("userId")
                val accessToken = o.optString("accessToken")
                if (userId.isBlank() || accessToken.isBlank()) return null
                SupabaseSession(
                    userId = userId,
                    email = o.optString("email"),
                    displayName = o.optString("displayName").ifBlank { "Member" },
                    accessToken = accessToken,
                    refreshToken = o.optString("refreshToken"),
                    expiresAtMillis = o.optLong("expiresAtMillis", 0L)
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun encodeSupabaseSession(s: SupabaseSession): String {
            return JSONObject().apply {
                put("userId", s.userId)
                put("email", s.email)
                put("displayName", s.displayName)
                put("accessToken", s.accessToken)
                put("refreshToken", s.refreshToken)
                put("expiresAtMillis", s.expiresAtMillis)
            }.toString()
        }

        private fun parseOnboardingProfile(json: String?): OnboardingProfile {
            if (json.isNullOrBlank()) return OnboardingProfile()
            return try {
                val o = JSONObject(json)
                OnboardingProfile(
                    role = o.optString("role"),
                    primaryUseCase = o.optString("primaryUseCase"),
                    saveHabit = o.optString("saveHabit"),
                    workflowPreset = o.optString("workflowPreset", "starter_folders").ifBlank { "starter_folders" },
                    wizardCompleted = o.optBoolean("wizardCompleted", false)
                )
            } catch (_: Exception) {
                OnboardingProfile()
            }
        }

        private fun encodeOnboardingProfile(p: OnboardingProfile): String {
            return JSONObject().apply {
                put("role", p.role)
                put("primaryUseCase", p.primaryUseCase)
                put("saveHabit", p.saveHabit)
                put("workflowPreset", p.workflowPreset)
                put("wizardCompleted", p.wizardCompleted)
            }.toString()
        }

        private fun parseDetoxState(json: String?): DetoxSessionState {
            if (json.isNullOrBlank()) return DetoxSessionState()
            return try {
                val o = JSONObject(json)
                DetoxSessionState(
                    isActive = o.optBoolean("isActive", false),
                    durationHours = o.optInt("durationHours", 2),
                    startedAtMillis = o.optLong("startedAtMillis", 0L),
                    endsAtMillis = o.optLong("endsAtMillis", 0L),
                    greyscaleUi = o.optBoolean("greyscaleUi", true),
                    suppressNotifications = o.optBoolean("suppressNotifications", true),
                    whitelistedPackagesCsv = o.optString("whitelistedPackagesCsv")
                )
            } catch (_: Exception) {
                DetoxSessionState()
            }
        }

        private fun encodeDetoxState(s: DetoxSessionState): String {
            return JSONObject().apply {
                put("isActive", s.isActive)
                put("durationHours", s.durationHours)
                put("startedAtMillis", s.startedAtMillis)
                put("endsAtMillis", s.endsAtMillis)
                put("greyscaleUi", s.greyscaleUi)
                put("suppressNotifications", s.suppressNotifications)
                put("whitelistedPackagesCsv", s.whitelistedPackagesCsv)
            }.toString()
        }

        private fun parseStringList(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotBlank()) add(s)
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun parseStringMap(json: String?): Map<String, String> {
            if (json.isNullOrBlank()) return emptyMap()
            return try {
                val obj = JSONObject(json)
                val out = linkedMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k).trim()
                    if (k.isNotBlank() && v.isNotBlank()) {
                        out[k] = v
                    }
                }
                out
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}
