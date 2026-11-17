package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.ui.components.FinanceIconCatalog
import com.example.ui.components.FinanceIconCategory
import com.example.ui.screens.MoneyLedgerScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.PersonalAiLibraryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h892dp-xhdpi")
class FinanceIconPickerRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun financeIconCatalog_has60PlusIconsAcross8Categories_andSearchesKeywords() {
        assertTrue(FinanceIconCatalog.allIcons.size >= 60)
        assertEquals(8, FinanceIconCategory.entries.size)
        FinanceIconCategory.entries.forEach { cat ->
            assertTrue(
                "Expected icons in category ${cat.fullTitle}",
                FinanceIconCatalog.allIcons.any { it.category == cat }
            )
        }

        assertTrue(FinanceIconCatalog.search("coffee").any { it.key == "coffee" })
        assertTrue(FinanceIconCatalog.search("car").any { it.key == "car" || it.key == "car_service" })
        assertTrue(FinanceIconCatalog.search("rent").any { it.key == "rent" })
        assertTrue(FinanceIconCatalog.search("server").any { it.key == "server" || it.key == "cloud_api" })
        assertTrue(FinanceIconCatalog.search("salary").any { it.key == "salary" })

        val businessOnly = FinanceIconCatalog.search("", FinanceIconCategory.BUSINESS)
        assertTrue(businessOnly.all { it.category == FinanceIconCategory.BUSINESS })

        // Legacy sector icon keys resolve cleanly
        listOf("briefcase", "sparkle", "send", "heart", "compass", "star", "check", "wallet").forEach { legacy ->
            val resolved = FinanceIconCatalog.resolve(legacy)
            assertTrue(resolved.key.isNotBlank())
        }

        val recent = FinanceIconCatalog.resolveRecentSpecs(
            recentKeys = listOf("coffee", "server", "coffee", "rent"),
            fallbackSectorKeys = listOf("salary", "fuel", "gym")
        )
        assertEquals(5, recent.size)
        assertEquals("coffee", recent.first().key)
    }

    @Test
    fun moneyLedger_createSectorAndLogEntry_pickCategorizedIconsWithSearchAndFilters() {
        val sectors = listOf(
            FinanceSectorEntity(
                id = "sec_food",
                name = "Food & Living",
                iconKey = "restaurant",
                colorHex = "#EF6C00",
                typeAffinity = "EXPENSE"
            ),
            FinanceSectorEntity(
                id = "sec_biz",
                name = "Business",
                iconKey = "office",
                colorHex = "#2E7D32",
                typeAffinity = "BOTH"
            )
        )
        val existingTx = FinanceTransactionEntity(
            id = "tx_existing",
            txType = "EXPENSE",
            amountCents = 1450L,
            sectorId = "sec_food",
            sectorName = "Food & Living",
            paymentMethod = "CARD",
            notes = "Lunch Bowl"
        )

        var createdSectorName = ""
        var createdSectorIcon = ""
        var loggedAmountCents = 0L
        var loggedNotes = ""
        var loggedIconKey = ""

        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                MoneyLedgerScreen(
                    sectors = sectors,
                    transactions = listOf(existingTx),
                    recentIconKeys = listOf("coffee", "fuel", "rent"),
                    txIconOverrides = mapOf("tx_existing" to "restaurant"),
                    onCreateSector = { name, iconKey, _, _ ->
                        createdSectorName = name
                        createdSectorIcon = iconKey
                    },
                    onDeleteSector = {},
                    onLogTransaction = { _, cents, _, _, notes, _, iconKey ->
                        loggedAmountCents = cents
                        loggedNotes = notes
                        loggedIconKey = iconKey
                    },
                    onDeleteTransaction = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("money_tx_icon_tx_existing").assertIsDisplayed()

        // 1. Create Custom Sector -> open categorized icon picker -> filter Business -> search "server" -> select
        composeTestRule.onNodeWithTag("money_new_sector_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sector_name_input").performTextInput("Cloud Infra")
        composeTestRule.onNodeWithTag("sector_open_icon_picker_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finance_icon_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("icon_picker_recent_strip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("icon_picker_cat_business").performClick()
        composeTestRule.onNodeWithTag("icon_picker_search_input").performTextInput("server")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("icon_picker_item_server").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sector_confirm_create_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Cloud Infra", createdSectorName)
        assertEquals("server", createdSectorIcon)

        // 2. Log Entry -> open categorized icon picker -> search "coffee" -> select -> save
        composeTestRule.onNodeWithTag("money_log_entry_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_amount_input").performTextInput("6.75")
        composeTestRule.onNodeWithTag("tx_notes_input").performTextInput("Oat Latte")
        composeTestRule.onNodeWithTag("tx_open_icon_picker_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("icon_picker_search_input").performTextInput("coffee")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("icon_picker_item_coffee").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tx_confirm_save_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(675L, loggedAmountCents)
        assertEquals("Oat Latte", loggedNotes)
        assertEquals("coffee", loggedIconKey)
    }

    @Test
    fun financeIconPicker_rendersCleanlyInObsidianDarkMode_andSupportsEmptySearchReset() {
        val sectors = listOf(
            FinanceSectorEntity(id = "s1", name = "Living", iconKey = "rent", colorHex = "#E53935")
        )
        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                MoneyLedgerScreen(
                    sectors = sectors,
                    transactions = emptyList(),
                    onCreateSector = { _, _, _, _ -> },
                    onDeleteSector = {},
                    onLogTransaction = { _, _, _, _, _, _, _ -> },
                    onDeleteTransaction = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("money_log_entry_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_open_icon_picker_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("icon_picker_search_input").performTextInput("zzznomatch")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset Filters").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("icon_picker_recent_strip").assertIsDisplayed()
    }
}
