package com.example.monk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.local.DetoxRuntimeStore
import com.example.data.local.UserPreferencesStore
import com.example.ui.components.EmergencyExitDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileAlabasterBackground
import com.example.ui.components.TactileGlassCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.PersonalAiLibraryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusShieldActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val ruleId = intent.getStringExtra(EXTRA_RULE_ID).orEmpty()
        val ruleTitle = intent.getStringExtra(EXTRA_RULE_TITLE).orEmpty().ifBlank { "Monk Mode Active" }
        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val remainingMs = intent.getLongExtra(EXTRA_REMAINING_MS, -1L)

        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(blockedPkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            blockedPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }.ifBlank { "Restricted App" }
        }

        setContent {
            val scope = rememberCoroutineScope()
            var showEmergencyDialog by remember { mutableStateOf(false) }

            // Block casual back-button escape from the focus shield
            BackHandler(enabled = true) {
                goToHomeLauncher()
            }

            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT) {
                val palette = LiquidTheme.palette
                TactileAlabasterBackground {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TactileGlassCard(
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .fillMaxWidth()
                                    .testTag("focus_shield_card"),
                                cornerRadius = 32.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.accentSoftBg,
                                        border = BorderStroke(1.5.dp, CrimsonAccent.copy(alpha = 0.65f)),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            MinimalIcon(
                                                type = MinimalIconType.SHIELD,
                                                active = true,
                                                tint = CrimsonAccent,
                                                size = 32.dp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "MONK MODE FOCUS SHIELD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonAccent
                                    )
                                    Text(
                                        text = "$appLabel is Locked",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Protected by \"$ruleTitle\". Stay aligned with your deep work and saved library goals.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryInk,
                                        textAlign = TextAlign.Center
                                    )

                                    if (remainingMs > 0L) {
                                        val mins = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = palette.recessedTrayBg,
                                            border = BorderStroke(1.dp, palette.specularRimOutline)
                                        ) {
                                            Text(
                                                text = "Remaining lock: ~$mins min",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.accentSoftInk,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Button(
                                        onClick = { goToHomeLauncher() },
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryButtonBg,
                                            contentColor = palette.primaryButtonContent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("shield_return_focus_button")
                                    ) {
                                        Text("Return to Focus", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showEmergencyDialog = true },
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("shield_emergency_exit_button")
                                    ) {
                                        Text(
                                            text = "Emergency Exit (3-Stage Challenge)",
                                            color = CrimsonAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (showEmergencyDialog) {
                            EmergencyExitDialog(
                                targetRuleTitle = ruleTitle,
                                onDismiss = { showEmergencyDialog = false },
                                onUnlockConfirmed = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val dao = AppDatabase.getInstance(applicationContext).workspaceDao()
                                            if (ruleId == MonkBlockerEngine.DETOX_RULE_ID) {
                                                DetoxRuntimeStore.clear(applicationContext)
                                                UserPreferencesStore(applicationContext).clearDetoxSessionState()
                                            } else if (ruleId.isNotBlank()) {
                                                dao.getBlockerRuleById(ruleId)?.let { rule ->
                                                    dao.upsertBlockerRule(
                                                        rule.copy(isEnabled = false, updatedAt = System.currentTimeMillis())
                                                    )
                                                }
                                            }
                                            dao.getActiveFocusSession()?.let { session ->
                                                val now = System.currentTimeMillis()
                                                val actualMins = ((now - session.startedAt) / 60_000L).toInt().coerceAtLeast(0)
                                                dao.upsertFocusSession(
                                                    session.copy(
                                                        endedAt = now,
                                                        actualMinutes = actualMins,
                                                        completed = false,
                                                        emergencyUnlocked = true
                                                    )
                                                )
                                            }
                                        }
                                        showEmergencyDialog = false
                                        finish()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun goToHomeLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(homeIntent)
        } catch (_: Exception) {
        }
        finish()
    }

    companion object {
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_RULE_TITLE = "extra_rule_title"
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCK_MODE = "extra_block_mode"
        const val EXTRA_REMAINING_MS = "extra_remaining_ms"
    }
}
