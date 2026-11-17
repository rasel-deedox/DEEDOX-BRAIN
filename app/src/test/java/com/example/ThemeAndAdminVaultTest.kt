package com.example

import com.example.data.ai.AiLibraryService
import com.example.data.local.AdminPasscodeCrypto
import com.example.data.local.AdminVaultConfig
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.buildLiquidPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndAdminVaultTest {

    @Test
    fun appThemeMode_fromCode_mapsLightDimNightAndLegacyDark() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromCode("LIGHT"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromCode("light"))
        assertEquals(AppThemeMode.DIM, AppThemeMode.fromCode("DIM"))
        assertEquals(AppThemeMode.NIGHT, AppThemeMode.fromCode("NIGHT"))
        assertEquals(AppThemeMode.NIGHT, AppThemeMode.fromCode("DARK"))
        assertEquals(AppThemeMode.NIGHT, AppThemeMode.fromCode(null))
        assertEquals(AppThemeMode.NIGHT, AppThemeMode.fromCode("unknown"))
    }

    @Test
    fun liquidAccentPreset_fromCode_resolvesAllPresetsAndBuildsPalette() {
        LiquidAccentPreset.entries.forEach { preset ->
            assertEquals(preset, LiquidAccentPreset.fromCode(preset.code))
            val lightPalette = buildLiquidPalette(AppThemeMode.LIGHT, preset)
            val dimPalette = buildLiquidPalette(AppThemeMode.DIM, preset)
            val nightPalette = buildLiquidPalette(AppThemeMode.NIGHT, preset)
            assertFalse(lightPalette.isDark)
            assertTrue(dimPalette.isDark)
            assertTrue(nightPalette.isDark)
            assertEquals(preset.primaryAccent, lightPalette.accentCrimson)
            assertEquals(preset.primaryAccent, dimPalette.accentCrimson)
            assertEquals(preset.primaryAccent, nightPalette.accentCrimson)
        }
        assertEquals(LiquidAccentPreset.CRIMSON_BRONZE, LiquidAccentPreset.fromCode("invalid"))
    }

    @Test
    fun adminPasscodeCrypto_hashesWithUniqueSaltAndVerifiesCorrectly() {
        val salt1 = AdminPasscodeCrypto.generateSaltHex()
        val salt2 = AdminPasscodeCrypto.generateSaltHex()
        assertNotEquals(salt1, salt2)
        assertEquals(32, salt1.length)

        val hash = AdminPasscodeCrypto.hashPasscode("mySecret99", salt1)
        assertTrue(AdminPasscodeCrypto.verify("mySecret99", hash, salt1))
        assertFalse(AdminPasscodeCrypto.verify("wrongPass", hash, salt1))
        assertFalse(AdminPasscodeCrypto.verify("mySecret99", hash, salt2))
        assertFalse(AdminPasscodeCrypto.verify("mySecret99", null, salt1))
        assertFalse(AdminPasscodeCrypto.verify("mySecret99", hash, null))
    }

    @Test
    fun enforceFreeOpenRouterModel_blocksPaidModelsAndKeepsFreeSlugs() {
        assertEquals("openrouter/free", AdminVaultConfig.DEFAULT_OPENROUTER_MODEL)
        assertEquals("openrouter/free", AdminVaultConfig.enforceFreeOpenRouterModel(null))
        assertEquals("openrouter/free", AdminVaultConfig.enforceFreeOpenRouterModel(""))
        assertEquals("openrouter/free", AdminVaultConfig.enforceFreeOpenRouterModel("openai/gpt-4o-mini"))
        assertEquals("openrouter/free", AdminVaultConfig.enforceFreeOpenRouterModel("google/gemini-2.5-flash"))
        assertEquals("openrouter/free", AdminVaultConfig.enforceFreeOpenRouterModel("anthropic/claude-3.5-sonnet"))
        assertEquals(
            "meta-llama/llama-3.3-70b-instruct:free",
            AdminVaultConfig.enforceFreeOpenRouterModel("meta-llama/llama-3.3-70b-instruct:free")
        )
        assertEquals(
            "google/gemma-3-27b-it:free",
            AdminVaultConfig.enforceFreeOpenRouterModel("  google/gemma-3-27b-it:free  ")
        )
    }

    @Test
    fun resolveEffective_activatesOpenRouterWhenKeyPresentUnlessManuallyLockedToFirebase() {
        val syncedWithKey = AdminVaultConfig(
            aiProvider = AdminVaultConfig.PROVIDER_FIREBASE_AI,
            openRouterApiKey = "sk-or-test-key-123",
            lastSyncedAt = 1_700_000_000_000L,
            providerManuallySelected = false
        )
        val eff1 = AdminVaultConfig.resolveEffective(syncedWithKey)
        assertEquals(AdminVaultConfig.PROVIDER_OPENROUTER, eff1.aiProvider)
        assertEquals("sk-or-test-key-123", eff1.openRouterApiKey)

        val explicitlyFirebase = syncedWithKey.copy(providerManuallySelected = true)
        val eff2 = AdminVaultConfig.resolveEffective(explicitlyFirebase)
        assertEquals(AdminVaultConfig.PROVIDER_FIREBASE_AI, eff2.aiProvider)

        val emptySynced = AdminVaultConfig(
            aiProvider = AdminVaultConfig.PROVIDER_FIREBASE_AI,
            openRouterApiKey = "",
            lastSyncedAt = 1_700_000_000_000L
        )
        val eff3 = AdminVaultConfig.resolveEffective(emptySynced)
        assertEquals(AdminVaultConfig.PROVIDER_FIREBASE_AI, eff3.aiProvider)
    }

    @Test
    fun aiLibraryService_cleansThinkBlocksBeforeExtractingTextAndJson() {
        val noisyText = "<think>Thinking with {fake: json} inside thoughts</think>\nREADY"
        assertEquals("READY", AiLibraryService.cleanOpenRouterReply(noisyText))

        val noisyJson = """
            <think>Let me think about {ignored} first</think>
            ```json
            {"summary":"Real summary","suggestedTags":["ai","android"]}
            ```
        """.trimIndent()
        assertEquals(
            """{"summary":"Real summary","suggestedTags":["ai","android"]}""",
            AiLibraryService.extractJsonObject(noisyJson)
        )
    }
}
