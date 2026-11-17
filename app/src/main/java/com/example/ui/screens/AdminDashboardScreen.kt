package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AdminVaultConfig
import com.example.data.local.CustomCredentialEntry
import com.example.ui.components.AccentActionPill
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.LiquidTheme
import java.util.UUID

@Composable
fun SecretAdminPasswordModal(
    hasExistingPasscode: Boolean,
    authError: String?,
    onDismiss: () -> Unit,
    onSubmitPasscode: (String) -> Unit
) {
    val palette = LiquidTheme.palette
    var passcode by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        TactileGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("secret_admin_password_modal"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalIcon(
                        type = MinimalIconType.LOCK,
                        active = true,
                        tint = palette.accentCrimson,
                        size = 22.dp
                    )
                    Column {
                        Text(
                            text = if (hasExistingPasscode) "Admin Vault Access" else "Create Admin Passcode",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = if (hasExistingPasscode) {
                                "Enter your encrypted admin passcode to manage live API credentials."
                            } else {
                                "First-time setup: create a passcode (4+ chars) to protect dynamic API keys."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                }

                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text(if (hasExistingPasscode) "Admin Passcode" else "New Admin Passcode") },
                    singleLine = true,
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { reveal = !reveal }) {
                            Text(
                                text = if (reveal) "Hide" else "Show",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentSoftInk
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = palette.accentCrimson,
                        unfocusedBorderColor = palette.specularRimOutline,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (!authError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.accentSoftBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accentSoftInk,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = palette.primaryInk)
                    }
                    TactilePrimaryPillButton(
                        text = if (hasExistingPasscode) "Unlock" else "Create & Enter",
                        enabled = passcode.trim().length >= 4,
                        onClick = { onSubmitPasscode(passcode) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_password_submit")
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    config: AdminVaultConfig,
    isSignedIn: Boolean,
    isOnline: Boolean,
    userEmail: String?,
    isTesting: Boolean,
    testStatus: String?,
    isCloudSyncing: Boolean = false,
    passcodeStatus: String? = null,
    freeModelOptions: List<Pair<String, String>> = AdminVaultConfig.FREE_OPENROUTER_PRESETS,
    isLoadingFreeModels: Boolean = false,
    onLoadFreeModels: () -> Unit = {},
    onBackAndLock: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onRefreshCloudConfig: () -> Unit = {},
    onSaveConfig: (AdminVaultConfig) -> Unit,
    onTestConnection: (provider: String, openRouterKey: String, openRouterModel: String) -> Unit,
    onChangePasscode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackAndLock() }

    val palette = LiquidTheme.palette
    var aiProvider by remember(config.aiProvider) { mutableStateOf(config.aiProvider) }
    var providerManuallySelected by remember(config.providerManuallySelected) {
        mutableStateOf(config.providerManuallySelected)
    }
    var openRouterKey by remember(config.openRouterApiKey) { mutableStateOf(config.openRouterApiKey) }
    var openRouterModel by remember(config.openRouterModel) {
        mutableStateOf(AdminVaultConfig.enforceFreeOpenRouterModel(config.openRouterModel))
    }
    var revealOpenRouterKey by remember { mutableStateOf(false) }

    var supabaseUrl by remember(config.supabaseUrl) { mutableStateOf(config.supabaseUrl) }
    var supabaseAnonKey by remember(config.supabaseAnonKey) { mutableStateOf(config.supabaseAnonKey) }
    var revealSupabaseKey by remember { mutableStateOf(false) }

    var customCredentials by remember(config.customCredentials) { mutableStateOf(config.customCredentials) }
    var newKeyName by remember { mutableStateOf("") }
    var newKeyValue by remember { mutableStateOf("") }
    var newKeyNote by remember { mutableStateOf("") }
    var revealedIds by remember { mutableStateOf(emptySet<String>()) }

    var nextPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var passcodeValidationError by remember { mutableStateOf<String?>(null) }

    val buildUpdatedConfig = {
        val cleanModel = AdminVaultConfig.enforceFreeOpenRouterModel(openRouterModel)
        config.copy(
            aiProvider = aiProvider,
            openRouterApiKey = openRouterKey.trim(),
            openRouterModel = cleanModel,
            supabaseUrl = supabaseUrl.trim(),
            supabaseAnonKey = supabaseAnonKey.trim(),
            customCredentials = customCredentials,
            providerManuallySelected = providerManuallySelected
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("admin_dashboard_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "admin_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.BACK,
                            contentDescription = "Back and lock admin vault",
                            onClick = onBackAndLock,
                            modifier = Modifier.testTag("admin_back_button")
                        )
                        Column {
                            Text(
                                text = "Admin API Vault",
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Dynamic runtime endpoints & cloud sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                    Surface(
                        onClick = onBackAndLock,
                        shape = RoundedCornerShape(9999.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("admin_lock_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MinimalIcon(
                                type = MinimalIconType.LOCK,
                                active = true,
                                tint = palette.accentSoftInk,
                                size = 14.dp
                            )
                            Text(
                                text = "Lock",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentSoftInk
                            )
                        }
                    }
                }
            }

            // Firestore Vault Backend Card
            item(key = "admin_firestore_backend", contentType = "admin_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinimalIcon(
                                type = MinimalIconType.CLOUD_SYNC,
                                active = isSignedIn,
                                tint = palette.accentSoftInk,
                                size = 20.dp
                            )
                            Text(
                                text = "Firestore Admin Backend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                        }
                        Text(
                            text = if (isSignedIn) {
                                "Connected as ${userEmail ?: "Google Account"} • Real-time broadcast to all mobile apps (app_config/ai_runtime) + private admin_vault" +
                                    (config.lastSyncedAt?.let { " • Updated ${formatRelativeDate(it)}" } ?: "")
                            } else {
                                "Stored locally. Sign in with Google to broadcast API key & free model updates live to all mobile app installations."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                        if (isSignedIn) {
                            OutlinedButton(
                                onClick = onRefreshCloudConfig,
                                enabled = !isCloudSyncing && isOnline,
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, palette.inputBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_sync_now_button")
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = if (isOnline) "Pull Latest Cloud Vault Now" else "Offline — Local Vault Active",
                                        color = palette.primaryInk
                                    )
                                }
                            }
                        } else {
                            TactilePrimaryPillButton(
                                text = "Sign in with Google to Sync",
                                onClick = onGoogleSignInClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Live AI Engine & OpenRouter Configuration
            item(key = "admin_ai_config", contentType = "admin_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "AI Engine & OpenRouter Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )

                        val engineStatusText = when {
                            aiProvider == AdminVaultConfig.PROVIDER_OPENROUTER && openRouterKey.isNotBlank() ->
                                "Active: OpenRouter Free • Key Ready (${AdminVaultConfig.enforceFreeOpenRouterModel(openRouterModel)})"
                            aiProvider == AdminVaultConfig.PROVIDER_OPENROUTER ->
                                "OpenRouter Selected — Paste your sk-or-v1-... key below or set OPENROUTER_API_KEY in AI Studio Secrets"
                            else ->
                                "Active: Firebase AI Logic (Gemini)"
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_ai_status_badge")
                        ) {
                            Text(
                                text = engineStatusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }

                        RecessedFramingBezel(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 9999.dp,
                            innerPadding = PaddingValues(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val firebaseSelected = aiProvider == AdminVaultConfig.PROVIDER_FIREBASE_AI
                                Surface(
                                    onClick = {
                                        aiProvider = AdminVaultConfig.PROVIDER_FIREBASE_AI
                                        providerManuallySelected = true
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (firebaseSelected) palette.primaryButtonBg else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_provider_firebase")
                                ) {
                                    Text(
                                        text = "Firebase AI Logic",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (firebaseSelected) palette.primaryButtonContent else palette.secondaryInk,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    )
                                }
                                Surface(
                                    onClick = {
                                        aiProvider = AdminVaultConfig.PROVIDER_OPENROUTER
                                        providerManuallySelected = true
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (!firebaseSelected) palette.primaryButtonBg else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_provider_openrouter")
                                ) {
                                    Text(
                                        text = "OpenRouter API",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (!firebaseSelected) palette.primaryButtonContent else palette.secondaryInk,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = openRouterKey,
                            onValueChange = { nextKey ->
                                val wasBlank = openRouterKey.isBlank()
                                openRouterKey = nextKey
                                val trimmedNext = nextKey.trim()
                                if (trimmedNext.isNotBlank() && (wasBlank || trimmedNext.startsWith("sk-or-"))) {
                                    aiProvider = AdminVaultConfig.PROVIDER_OPENROUTER
                                    providerManuallySelected = true
                                }
                            },
                            label = { Text("OpenRouter API Key (sk-or-...)") },
                            singleLine = true,
                            visualTransformation = if (revealOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { revealOpenRouterKey = !revealOpenRouterKey }) {
                                    Text(
                                        text = if (revealOpenRouterKey) "Hide" else "Reveal",
                                        color = palette.accentSoftInk
                                    )
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_openrouter_key_input")
                        )

                        OutlinedTextField(
                            value = openRouterModel,
                            onValueChange = { openRouterModel = it },
                            label = { Text("OpenRouter Free Model ID (100% Free Only)") },
                            placeholder = { Text(AdminVaultConfig.DEFAULT_OPENROUTER_MODEL) },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_openrouter_model_input")
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(freeModelOptions, key = { it.first }) { (modelId, chipLabel) ->
                                val active = openRouterModel == modelId
                                Surface(
                                    onClick = { openRouterModel = modelId },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (active) palette.accentSoftBg else palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = chipLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) palette.accentSoftInk else palette.secondaryInk,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "100% free mode: only openrouter/free or :free models run.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onLoadFreeModels,
                                enabled = !isLoadingFreeModels && isOnline,
                                modifier = Modifier.testTag("admin_load_free_models_button")
                            ) {
                                Text(
                                    text = if (isLoadingFreeModels) "Loading..." else "Refresh Free List",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentSoftInk
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onTestConnection(aiProvider, openRouterKey.trim(), openRouterModel.trim())
                                },
                                enabled = !isTesting && isOnline,
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, palette.inputBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_test_connection_button")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = if (isOnline) "Test Live Connection" else "Offline",
                                        color = palette.primaryInk
                                    )
                                }
                            }
                        }

                        if (!testStatus.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.accentSoftBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryInk,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Supabase Credentials Card
            item(key = "admin_supabase_card", contentType = "admin_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Supabase Backend Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        OutlinedTextField(
                            value = supabaseUrl,
                            onValueChange = { supabaseUrl = it },
                            label = { Text("Supabase Project URL (https://xxx.supabase.co)") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_supabase_url_input")
                        )
                        OutlinedTextField(
                            value = supabaseAnonKey,
                            onValueChange = { supabaseAnonKey = it },
                            label = { Text("Supabase Anon / Publishable Key") },
                            singleLine = true,
                            visualTransformation = if (revealSupabaseKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { revealSupabaseKey = !revealSupabaseKey }) {
                                    Text(
                                        text = if (revealSupabaseKey) "Hide" else "Reveal",
                                        color = palette.accentSoftInk
                                    )
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_supabase_key_input")
                        )
                    }
                }
            }

            // Dynamic Custom Service Credentials
            item(key = "admin_custom_keys_form", contentType = "admin_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Custom Service Keys (${customCredentials.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        OutlinedTextField(
                            value = newKeyName,
                            onValueChange = { newKeyName = it },
                            label = { Text("Service / Key Name (e.g. TAVILY_API_KEY)") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_custom_key_name_input")
                        )
                        OutlinedTextField(
                            value = newKeyValue,
                            onValueChange = { newKeyValue = it },
                            label = { Text("Secret Value / Endpoint") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_custom_key_value_input")
                        )
                        OutlinedTextField(
                            value = newKeyNote,
                            onValueChange = { newKeyNote = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                if (newKeyName.isNotBlank() && newKeyValue.isNotBlank()) {
                                    val entry = CustomCredentialEntry(
                                        id = UUID.randomUUID().toString(),
                                        keyName = newKeyName.trim(),
                                        keyValue = newKeyValue.trim(),
                                        note = newKeyNote.trim()
                                    )
                                    val nextList = customCredentials + entry
                                    customCredentials = nextList
                                    newKeyName = ""
                                    newKeyValue = ""
                                    newKeyNote = ""
                                    onSaveConfig(buildUpdatedConfig().copy(customCredentials = nextList))
                                }
                            },
                            enabled = newKeyName.isNotBlank() && newKeyValue.isNotBlank(),
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.inputBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_add_credential_button")
                        ) {
                            Text("Add Custom Credential", color = palette.primaryInk)
                        }
                    }
                }
            }

            if (customCredentials.isNotEmpty()) {
                items(
                    items = customCredentials,
                    key = { it.id },
                    contentType = { "custom_credential_card" }
                ) { entry ->
                    val isShown = entry.id in revealedIds
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.keyName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = if (isShown) entry.keyValue else "••••••••••••" + entry.keyValue.takeLast(4),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (entry.note.isNotBlank()) {
                                    Text(
                                        text = entry.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentSoftInk
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        revealedIds = if (isShown) revealedIds - entry.id else revealedIds + entry.id
                                    }
                                ) {
                                    Text(if (isShown) "Hide" else "Show", color = palette.primaryInk)
                                }
                                TextButton(
                                    onClick = {
                                        val nextList = customCredentials.filterNot { it.id == entry.id }
                                        customCredentials = nextList
                                        onSaveConfig(buildUpdatedConfig().copy(customCredentials = nextList))
                                    }
                                ) {
                                    Text("Delete", color = palette.accentCrimson)
                                }
                            }
                        }
                    }
                }
            }

            // Admin Passcode Rotation
            item(key = "admin_rotate_passcode", contentType = "admin_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Update Admin Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Stored strictly as a salted SHA-256 cryptographic digest locally and in your cloud vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                        OutlinedTextField(
                            value = nextPasscode,
                            onValueChange = {
                                nextPasscode = it
                                passcodeValidationError = null
                            },
                            label = { Text("New Admin Password (4+ chars)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_new_passcode_input")
                        )
                        OutlinedTextField(
                            value = confirmPasscode,
                            onValueChange = {
                                confirmPasscode = it
                                passcodeValidationError = null
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp),
                            colors = adminFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_confirm_passcode_input")
                        )
                        val displayPasscodeMsg = passcodeValidationError ?: passcodeStatus
                        if (!displayPasscodeMsg.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.accentSoftBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_passcode_status_banner")
                            ) {
                                Text(
                                    text = displayPasscodeMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryInk,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val trimmedNew = nextPasscode.trim()
                                val trimmedConfirm = confirmPasscode.trim()
                                when {
                                    trimmedNew.length < 4 -> {
                                        passcodeValidationError = "Password must be at least 4 characters."
                                    }
                                    trimmedConfirm.isNotBlank() && trimmedConfirm != trimmedNew -> {
                                        passcodeValidationError = "Passwords do not match."
                                    }
                                    trimmedConfirm.isBlank() -> {
                                        passcodeValidationError = "Please confirm your new password."
                                    }
                                    else -> {
                                        passcodeValidationError = null
                                        onChangePasscode(trimmedNew)
                                        nextPasscode = ""
                                        confirmPasscode = ""
                                    }
                                }
                            },
                            enabled = nextPasscode.trim().length >= 4 && confirmPasscode.trim().isNotBlank(),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryButtonBg,
                                contentColor = palette.primaryButtonContent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_rotate_passcode_button")
                        ) {
                            Text("Update & Encrypt Admin Password")
                        }
                    }
                }
            }

            // Save & Sync CTA
            item(key = "admin_save_cta", contentType = "cta") {
                AccentActionPill(
                    text = if (isSignedIn) "Save & Sync to Firestore" else "Save Configuration Locally",
                    onClick = { onSaveConfig(buildUpdatedConfig()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_save_config_button")
                )
            }
        }
    }
}

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LiquidTheme.palette.recessedTrayBg,
    unfocusedContainerColor = LiquidTheme.palette.recessedTrayBg,
    focusedBorderColor = LiquidTheme.palette.accentCrimson,
    unfocusedBorderColor = LiquidTheme.palette.specularRimOutline,
    focusedTextColor = LiquidTheme.palette.primaryInk,
    unfocusedTextColor = LiquidTheme.palette.primaryInk
)
