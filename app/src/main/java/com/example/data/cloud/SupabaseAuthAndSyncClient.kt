package com.example.data.cloud

import com.example.data.local.AdminVaultConfig
import com.example.data.local.ItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.SupabaseSession
import com.example.data.local.UserPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface SupabaseSignUpOutcome {
    data class Authenticated(val session: SupabaseSession) : SupabaseSignUpOutcome
    data class ConfirmationRequired(val email: String, val message: String) : SupabaseSignUpOutcome
}

class SupabaseAuthAndSyncClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun normalizeBaseUrl(rawUrl: String): String? {
        val cleaned = rawUrl.trim().trimEnd('/')
        if (!cleaned.startsWith("https://") && !cleaned.startsWith("http://")) return null
        return cleaned
    }

    suspend fun signInWithPassword(
        supabaseUrl: String,
        anonKey: String,
        email: String,
        password: String
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Supabase URL is not configured. Add it in Settings → Secret Admin Vault or use Google Sign-In.")
            )
        val key = anonKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase Anon Key is not configured. Add it in Settings → Secret Admin Vault.")
            )
        }
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/auth/v1/token?grant_type=password")
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException(extractErrorMessage(body, "Sign in failed (HTTP ${response.code})."))
                    )
                }
                val session = parseAuthSession(body, fallbackEmail = cleanEmail, fallbackName = null)
                    ?: return@withContext Result.failure(IllegalStateException("Invalid auth response from Supabase."))
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Unable to reach Supabase: ${e.localizedMessage ?: "network error"}"))
        }
    }

    suspend fun signUp(
        supabaseUrl: String,
        anonKey: String,
        fullName: String,
        email: String,
        password: String
    ): Result<SupabaseSignUpOutcome> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Supabase URL is not configured. Add it in Settings → Secret Admin Vault or use Google Sign-In.")
            )
        val key = anonKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase Anon Key is not configured. Add it in Settings → Secret Admin Vault.")
            )
        }
        val cleanName = fullName.trim().ifBlank { email.trim().substringBefore("@") }
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            put(
                "data",
                JSONObject().apply {
                    put("full_name", cleanName)
                    put("name", cleanName)
                }
            )
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/auth/v1/signup")
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException(extractErrorMessage(body, "Account creation failed (HTTP ${response.code})."))
                    )
                }
                val session = parseAuthSession(body, fallbackEmail = cleanEmail, fallbackName = cleanName)
                if (session != null) {
                    Result.success(SupabaseSignUpOutcome.Authenticated(session))
                } else {
                    Result.success(
                        SupabaseSignUpOutcome.ConfirmationRequired(
                            email = cleanEmail,
                            message = "Verification email sent to $cleanEmail. Confirm your email, then Sign In."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Unable to reach Supabase: ${e.localizedMessage ?: "network error"}"))
        }
    }

    suspend fun signOut(
        supabaseUrl: String,
        anonKey: String,
        accessToken: String
    ) = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl) ?: return@withContext
        val key = anonKey.trim()
        if (key.isBlank() || accessToken.isBlank()) return@withContext
        val request = Request.Builder()
            .url("$baseUrl/auth/v1/logout")
            .header("apikey", key)
            .header("Authorization", "Bearer $accessToken")
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().close()
        } catch (_: Exception) {
        }
    }

    suspend fun upsertItem(
        supabaseUrl: String,
        anonKey: String,
        session: SupabaseSession,
        item: ItemEntity,
        folderIds: List<String>,
        tagNames: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl) ?: return@withContext Result.failure(IllegalStateException("Missing Supabase URL"))
        val key = anonKey.trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing Supabase key"))

        val row = JSONObject().apply {
            put("id", item.id)
            put("user_id", session.userId)
            put("title", item.title)
            put("description", item.description)
            put("original_url", item.originalUrl)
            put("canonical_url", item.canonicalUrl)
            put("domain", item.domain)
            put("content_type", item.contentType)
            put("source_type", item.sourceType)
            put("thumbnail_url", item.thumbnailUrl)
            put("is_favorite", item.isFavorite)
            put("status", item.status)
            put("folder_ids", JSONArray(folderIds))
            put("tag_names", JSONArray(tagNames))
            put("is_device_local", item.isDeviceLocal)
            put("local_version", item.localVersion)
            put("updated_at", item.updatedAt)
            put("deleted_at", item.deletedAt)
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/items?on_conflict=id")
            .header("apikey", key)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(row.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException(extractErrorMessage(body, "Supabase item sync HTTP ${response.code}")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteItem(
        supabaseUrl: String,
        anonKey: String,
        session: SupabaseSession,
        itemId: String,
        deletedAtMillis: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl) ?: return@withContext Result.failure(IllegalStateException("Missing Supabase URL"))
        val key = anonKey.trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing Supabase key"))

        val patch = JSONObject().apply {
            put("deleted_at", deletedAtMillis)
            put("updated_at", deletedAtMillis)
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/items?id=eq.$itemId&user_id=eq.${session.userId}")
            .header("apikey", key)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .patch(patch.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException(extractErrorMessage(body, "Supabase delete HTTP ${response.code}")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertNote(
        supabaseUrl: String,
        anonKey: String,
        session: SupabaseSession,
        note: NoteEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl) ?: return@withContext Result.failure(IllegalStateException("Missing Supabase URL"))
        val key = anonKey.trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing Supabase key"))

        val row = JSONObject().apply {
            put("id", note.id)
            put("user_id", session.userId)
            put("item_id", note.itemId)
            put("body", note.body)
            put("is_pinned", note.isPinned)
            put("local_version", note.localVersion)
            put("updated_at", note.updatedAt)
            put("deleted_at", note.deletedAt)
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/notes?on_conflict=id")
            .header("apikey", key)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(row.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException(extractErrorMessage(body, "Supabase note sync HTTP ${response.code}")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertAdminVaultConfig(
        supabaseUrl: String,
        anonKey: String,
        session: SupabaseSession,
        config: AdminVaultConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl)
            ?: return@withContext Result.failure(IllegalStateException("Missing Supabase URL"))
        val key = anonKey.trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing Supabase key"))

        val row = JSONObject().apply {
            put("user_id", session.userId)
            put("ai_provider", config.aiProvider)
            put("openrouter_api_key", config.openRouterApiKey)
            put("openrouter_model", config.openRouterModel)
            put("supabase_url", config.supabaseUrl)
            put("supabase_anon_key", config.supabaseAnonKey)
            put("custom_credentials_json", UserPreferencesStore.encodeCustomCredentials(config.customCredentials))
            put("admin_passcode_hash", config.adminPasscodeHash ?: "")
            put("admin_passcode_salt", config.adminPasscodeSalt ?: "")
            put("updated_at", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/admin_vault?on_conflict=user_id")
            .header("apikey", key)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(row.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException(extractErrorMessage(body, "Supabase admin_vault sync HTTP ${response.code}")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAdminVaultConfig(
        supabaseUrl: String,
        anonKey: String,
        session: SupabaseSession
    ): Result<AdminVaultConfig?> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(supabaseUrl)
            ?: return@withContext Result.failure(IllegalStateException("Missing Supabase URL"))
        val key = anonKey.trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing Supabase key"))

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/admin_vault?user_id=eq.${session.userId}&limit=1")
            .header("apikey", key)
            .header("Authorization", "Bearer ${session.accessToken}")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException(extractErrorMessage(body, "Supabase admin_vault fetch HTTP ${response.code}"))
                    )
                }
                val arr = JSONArray(body.ifBlank { "[]" })
                if (arr.length() == 0) return@withContext Result.success(null)
                val obj = arr.getJSONObject(0)
                val parsed = AdminVaultConfig(
                    aiProvider = obj.optString("ai_provider", AdminVaultConfig.PROVIDER_FIREBASE_AI)
                        .takeIf { it in setOf(AdminVaultConfig.PROVIDER_FIREBASE_AI, AdminVaultConfig.PROVIDER_OPENROUTER) }
                        ?: AdminVaultConfig.PROVIDER_FIREBASE_AI,
                    openRouterApiKey = obj.optString("openrouter_api_key"),
                    openRouterModel = obj.optString("openrouter_model", AdminVaultConfig.DEFAULT_OPENROUTER_MODEL)
                        .ifBlank { AdminVaultConfig.DEFAULT_OPENROUTER_MODEL },
                    supabaseUrl = obj.optString("supabase_url").ifBlank { supabaseUrl },
                    supabaseAnonKey = obj.optString("supabase_anon_key").ifBlank { anonKey },
                    customCredentials = UserPreferencesStore.parseCustomCredentials(obj.optString("custom_credentials_json")),
                    adminPasscodeHash = obj.optString("admin_passcode_hash").takeIf { it.isNotBlank() },
                    adminPasscodeSalt = obj.optString("admin_passcode_salt").takeIf { it.isNotBlank() },
                    lastSyncedAt = obj.optLong("updated_at", System.currentTimeMillis())
                )
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAuthSession(
        rawBody: String,
        fallbackEmail: String,
        fallbackName: String?
    ): SupabaseSession? {
        return try {
            val root = JSONObject(rawBody)
            val accessToken = root.optString("access_token")
            if (accessToken.isBlank()) return null
            val refreshToken = root.optString("refresh_token")
            val expiresIn = root.optLong("expires_in", 3600L)
            val userObj = root.optJSONObject("user")
            val userId = userObj?.optString("id").orEmpty().ifBlank { return null }
            val email = userObj?.optString("email").orEmpty().ifBlank { fallbackEmail }
            val meta = userObj?.optJSONObject("user_metadata")
            val displayName = meta?.optString("full_name")
                ?.takeIf { it.isNotBlank() }
                ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                ?: fallbackName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifBlank { "Member" }

            SupabaseSession(
                userId = userId,
                email = email,
                displayName = displayName,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractErrorMessage(rawBody: String, fallback: String): String {
        if (rawBody.isBlank()) return fallback
        return try {
            val obj = JSONObject(rawBody)
            obj.optString("error_description")
                .takeIf { it.isNotBlank() }
                ?: obj.optString("msg").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("error").takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
