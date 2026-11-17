package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiLibraryService
import com.example.data.ai.AiPagePolishMode
import com.example.data.export.SmartCanvasPdfAndShare
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.CanvasIconKeys
import com.example.data.repository.ClientTrackerBlockData
import com.example.data.repository.InvoiceBlockData
import com.example.data.repository.InvoiceLineItem
import com.example.data.repository.SmartCanvasStarterTemplates
import com.example.data.repository.TableAggregation
import com.example.data.repository.TableBlockData
import com.example.data.repository.WorkspaceBlock
import com.example.ui.screens.NotionEditorSheet
import com.example.ui.screens.SharedNotionPageDialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.PersonalAiLibraryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h892dp-xhdpi")
class SmartCanvasRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun smartTable_supportsDynamicColumnsRowsAndAggregations() {
        var table = TableBlockData(
            title = "Q4 Budget",
            columns = listOf("Item", "Amount"),
            rows = listOf(
                listOf("Design", "$1,250.50"),
                listOf("Cloud", "$249.50")
            ),
            columnAggregations = listOf("COUNT", "SUM")
        )
        assertEquals("Cnt: 2", table.computeColumnSummary(0))
        assertEquals("Sum: $1,500", table.computeColumnSummary(1))

        table = table.addRow().updateCell(2, 0, "QA").updateCell(2, 1, "$300.00")
        assertEquals("Cnt: 3", table.computeColumnSummary(0))
        assertEquals("Sum: $1,800", table.computeColumnSummary(1))

        // Cycle SUM -> AVG on column 1
        table = table.cycleAggregation(1)
        assertEquals(TableAggregation.AVG, table.normalizedAggregations()[1])
        assertEquals("Avg: $600", table.computeColumnSummary(1))

        table = table.addColumn("Notes").removeColumn(2).removeRow(2)
        assertEquals(2, table.columns.size)
        assertEquals(2, table.rows.size)
    }

    @Test
    fun invoiceAndClientTracker_computeTaxDiscountAndRemainingDueCorrectly() {
        val invoice = InvoiceBlockData(
            mode = InvoiceBlockData.MODE_QUOTATION,
            docNumber = "QT-7788",
            clientName = "Acme Corp",
            taxPercent = 10.0,
            discountAmount = 100.0,
            items = listOf(
                InvoiceLineItem(description = "Brand System", qty = 2.0, unitPrice = 300.0),
                InvoiceLineItem(description = "Android App", qty = 1.0, unitPrice = 500.0)
            )
        )
        // Subtotal 1100 - discount 100 = 1000 taxable -> +10% = 1100
        assertEquals(1100.0, invoice.subtotal, 0.01)
        assertEquals(1000.0, invoice.taxableBase, 0.01)
        assertEquals(100.0, invoice.taxAmount, 0.01)
        assertEquals(1100.0, invoice.grandTotal, 0.01)
        assertTrue(invoice.toEmailSubject().contains("Quotation QT-7788"))
        assertTrue(invoice.toEmailBody().contains("Grand Total: $1,100.00"))

        val tracker = ClientTrackerBlockData(
            clientName = "Acme Corp",
            projectName = "Mobile Release",
            totalBudgetCents = ClientTrackerBlockData.parseMoneyToCents("$2,500.00"),
            advancePaidCents = ClientTrackerBlockData.parseMoneyToCents("$750.00")
        )
        assertEquals(175_000L, tracker.remainingDueCents)
        assertEquals("$1,750.00", tracker.formatDollars(tracker.remainingDueCents))
    }

    @Test
    fun iconsAndTemplates_containZeroEmojisAndNormalizeLegacyEmojis() {
        assertEquals(CanvasIconKeys.DOC, CanvasIconKeys.normalize("📄"))
        assertEquals(CanvasIconKeys.ROCKET, CanvasIconKeys.normalize("🚀"))
        assertEquals(CanvasIconKeys.GEAR, CanvasIconKeys.normalize("⚙️"))
        assertEquals(CanvasIconKeys.CHART, CanvasIconKeys.normalize("📊"))

        assertEquals(5, SmartCanvasStarterTemplates.TEMPLATES.size)
        SmartCanvasStarterTemplates.TEMPLATES.forEach { tpl ->
            assertFalse("Template label had emoji: ${tpl.label}", containsEmojiOrPictograph(tpl.label))
            assertTrue(tpl.iconKey in CanvasIconKeys.ALL_KEYS)
            assertTrue(tpl.buildBlocks().isNotEmpty())
        }
        WorkspaceBlock.SLASH_COMMANDS.forEach { cmd ->
            assertFalse("Slash command label had emoji: ${cmd.label}", containsEmojiOrPictograph(cmd.label))
        }
    }

    @Test
    fun aiPolishFallbackAndParser_generateStructuredBlocksForAllModes() {
        val raw = "Launch landing page\nFix login bug\nPrepare client pricing for Friday"
        AiPagePolishMode.entries.forEach { mode ->
            val fallback = AiLibraryService.buildFallbackPolishedBlocks("Sprint Notes", raw, mode)
            assertTrue("Fallback empty for $mode", fallback.isNotEmpty())
        }

        val json = """
            {
              "summary": "Deliver Q4 launch milestones across engineering and QA.",
              "milestones": [
                {"title":"Verify production sign-in","day":"Mon","timeSlot":"10:00 AM - 12:00 PM"},
                {"title":"Ship Release Candidate","day":"Thu","timeSlot":"2:00 PM - 5:00 PM"}
              ]
            }
        """.trimIndent()
        val parsed = AiLibraryService.parsePolishedPageJson(json, "Q4 Launch", AiPagePolishMode.ROADMAP)
        assertEquals(4, parsed.size)
        assertEquals(WorkspaceBlock.TYPE_H2, parsed[0].type)
        assertEquals(WorkspaceBlock.TYPE_CALLOUT, parsed[1].type)
        assertEquals(WorkspaceBlock.TYPE_SCHEDULE, parsed[2].type)
        assertEquals(WorkspaceBlock.TYPE_SCHEDULE, parsed[3].type)
    }

    @Test
    fun pdfExport_writesValidPdfFilesForPageAndInvoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val blocks = SmartCanvasStarterTemplates.TEMPLATES.first { it.id == "client_invoice" }.buildBlocks()
        val pagePdf = SmartCanvasPdfAndShare.generatePagePdfFile(
            context = context,
            title = "Proposal PDF Test",
            subtitle = "Generated during test",
            blocks = blocks
        )
        assertTrue(pagePdf.exists())
        assertTrue(pagePdf.length() > 100L)
        assertEquals("%PDF-", pagePdf.inputStream().bufferedReader().use { it.readLine()?.take(5) })

        val invoicePdf = SmartCanvasPdfAndShare.generateStandaloneInvoicePdfFile(
            context = context,
            invoice = InvoiceBlockData(clientName = "Test Co", docNumber = "INV-999")
        )
        assertTrue(invoicePdf.exists())
        assertTrue(invoicePdf.length() > 100L)
        assertEquals("%PDF-", invoicePdf.inputStream().bufferedReader().use { it.readLine()?.take(5) })
    }

    @Test
    fun notionEditorSheet_lightAndDark_templatesSlashSecretAndAiPolishWork() {
        var savedBlocks: List<WorkspaceBlock> = emptyList()
        var loggedLedgerCents = 0L

        composeTestRule.setContent {
            PersonalAiLibraryTheme(
                themeMode = AppThemeMode.LIGHT,
                accentPreset = LiquidAccentPreset.CRIMSON_BRONZE
            ) {
                NotionEditorSheet(
                    existingNote = null,
                    availableItems = emptyList(),
                    playbackState = VoicePlaybackState(),
                    onTogglePlayback = {},
                    onSeekPlayback = { _, _ -> },
                    onAttachReminder = { _, _ -> },
                    onSave = { _, _, blocks, _, _, _, _, _, _ -> savedBlocks = blocks },
                    onDismiss = {},
                    onPolishPageWithAi = { title, blocks, mode ->
                        Result.success(
                            AiLibraryService.buildFallbackPolishedBlocks(title, blocks.joinToString("\n") { it.text }, mode)
                        )
                    },
                    onLogClientPaymentToLedger = { _, _, cents -> loggedLedgerCents = cents }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Apply Client Invoice & Proposal starter template
        composeTestRule.onNodeWithTag("starter_templates_strip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("starter_template_client_invoice")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify Client Tracker & Invoice cards exist
        composeTestRule.onNodeWithTag("notion_canvas_list")
            .performScrollToNode(hasTestTag("rich_block_client_1"))
        composeTestRule.onNodeWithTag("rich_block_client_1").assertIsDisplayed()

        // Test Slash Command Drawer -> insert Secret Vault
        composeTestRule.onNodeWithTag("block_tool_slash_drawer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("smart_slash_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("slash_cmd_secret")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Scroll to newly appended Secret Vault (index 4) and toggle lock/reveal
        composeTestRule.onNodeWithTag("notion_canvas_list")
            .performScrollToNode(hasTestTag("secret_toggle_lock_4"))
        composeTestRule.onNodeWithText("Frosted Locked Vault").assertIsDisplayed()
        composeTestRule.onNodeWithTag("secret_toggle_lock_4").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Unlocked Secret").assertIsDisplayed()

        // Open AI Polish Modal & generate structured checklist
        composeTestRule.onNodeWithTag("notion_ai_polish_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_polish_mode_checklist").performClick()
        composeTestRule.onNodeWithTag("ai_polish_generate_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_polish_append_button").performClick()
        composeTestRule.waitForIdle()

        // Save page and verify rich blocks persisted
        composeTestRule.onNodeWithTag("notion_save_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(savedBlocks.any { it.type == WorkspaceBlock.TYPE_CLIENT })
        assertTrue(savedBlocks.any { it.type == WorkspaceBlock.TYPE_INVOICE })
        assertTrue(savedBlocks.any { it.type == WorkspaceBlock.TYPE_SECRET })
        assertTrue(savedBlocks.any { it.type == WorkspaceBlock.TYPE_CHECKLIST })
        assertEquals(0L, loggedLedgerCents)
    }

    @Test
    fun teamSharedNotionDialog_darkMode_supportsTemplatesAndSlashCommands() {
        var savedBlocksJson = ""
        val sampleMember = TeamMemberEntity(
            teamId = "t1",
            uid = "u1",
            displayName = "Aria",
            email = "aria@example.com",
            role = "OWNER"
        )

        composeTestRule.setContent {
            PersonalAiLibraryTheme(
                themeMode = AppThemeMode.NIGHT,
                accentPreset = LiquidAccentPreset.CRIMSON_BRONZE
            ) {
                SharedNotionPageDialog(
                    existingDoc = null,
                    initialParentId = null,
                    allDocs = emptyList<TeamSharedDocEntity>(),
                    members = listOf(sampleMember),
                    onDismiss = {},
                    onSave = { _, _, _, blocksJson, _, _, _, _ -> savedBlocksJson = blocksJson }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("team_notion_canvas_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("starter_template_weekly_sprint")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Open team slash palette and insert /table
        composeTestRule.onNodeWithTag("team_slash_palette_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_slash_command_palette").assertIsDisplayed()
        composeTestRule.onNodeWithTag("slash_cmd_table")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("team_confirm_doc_button").performClick()
        composeTestRule.waitForIdle()

        val decoded = WorkspaceBlock.parseList(savedBlocksJson)
        assertTrue(decoded.any { it.type == WorkspaceBlock.TYPE_SCHEDULE })
        assertTrue(decoded.any { it.type == WorkspaceBlock.TYPE_TABLE })
    }

    private fun containsEmojiOrPictograph(text: String): Boolean {
        return text.codePoints().anyMatch { cp ->
            cp in 0x1F300..0x1FAFF || cp in 0x2600..0x27BF
        }
    }
}
