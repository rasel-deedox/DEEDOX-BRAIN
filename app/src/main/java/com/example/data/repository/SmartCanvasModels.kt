package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

object CanvasIconKeys {
    const val DOC = "doc"
    const val ROCKET = "rocket"
    const val PIN = "pin"
    const val BRAIN = "brain"
    const val GEAR = "gear"
    const val CHART = "chart"
    const val CHECK = "check"
    const val INVOICE = "invoice"
    const val WALLET = "wallet"
    const val SHIELD = "shield"
    const val MAIL = "mail"
    const val CLOCK = "clock"

    val ALL_KEYS = listOf(
        DOC, ROCKET, PIN, BRAIN, GEAR, CHART, CHECK, INVOICE, WALLET, SHIELD, MAIL, CLOCK
    )

    fun normalize(raw: String?): String {
        val clean = raw?.trim().orEmpty()
        return when (clean) {
            "", "📄", "doc" -> DOC
            "🚀", "rocket" -> ROCKET
            "📌", "pin" -> PIN
            "🧠", "brain" -> BRAIN
            "⚙️", "⚙", "gear" -> GEAR
            "📊", "chart" -> CHART
            "✅", "check" -> CHECK
            "invoice", "wallet", "shield", "mail", "clock" -> clean
            else -> if (clean in ALL_KEYS) clean else DOC
        }
    }
}

data class PageMetaBlockData(
    val iconKey: String = CanvasIconKeys.DOC,
    val coverEnabled: Boolean = false,
    val coverVariant: String = "liquid_obsidian",
    val focusSecondsSpent: Long = 0L,
    val shareLinkUrl: String = ""
) {
    fun toJson(): String = JSONObject().apply {
        put("iconKey", CanvasIconKeys.normalize(iconKey))
        put("coverEnabled", coverEnabled)
        put("coverVariant", coverVariant)
        put("focusSecondsSpent", focusSecondsSpent)
        if (shareLinkUrl.isNotBlank()) put("shareLinkUrl", shareLinkUrl)
    }.toString()

    companion object {
        fun fromJson(json: String): PageMetaBlockData {
            if (json.isBlank()) return PageMetaBlockData()
            return try {
                val obj = JSONObject(json)
                PageMetaBlockData(
                    iconKey = CanvasIconKeys.normalize(obj.optString("iconKey", CanvasIconKeys.DOC)),
                    coverEnabled = obj.optBoolean("coverEnabled", false),
                    coverVariant = obj.optString("coverVariant", "liquid_obsidian"),
                    focusSecondsSpent = obj.optLong("focusSecondsSpent", 0L).coerceAtLeast(0L),
                    shareLinkUrl = obj.optString("shareLinkUrl", "")
                )
            } catch (_: Exception) {
                PageMetaBlockData()
            }
        }
    }
}

data class SubpageBlockData(
    val subpageId: String = UUID.randomUUID().toString(),
    val title: String = "Nested Sub-Page",
    val summary: String = "Tap to open or create sub-page",
    val iconKey: String = CanvasIconKeys.DOC
) {
    fun toJson(): String = JSONObject().apply {
        put("subpageId", subpageId)
        put("title", title)
        put("summary", summary)
        put("iconKey", CanvasIconKeys.normalize(iconKey))
    }.toString()

    companion object {
        fun fromJson(json: String, fallbackTitle: String = ""): SubpageBlockData {
            if (json.isBlank()) {
                return SubpageBlockData(title = fallbackTitle.ifBlank { "Nested Sub-Page" })
            }
            return try {
                val obj = JSONObject(json)
                SubpageBlockData(
                    subpageId = obj.optString("subpageId", UUID.randomUUID().toString()),
                    title = obj.optString("title", fallbackTitle.ifBlank { "Nested Sub-Page" }),
                    summary = obj.optString("summary", "Tap to open or create sub-page"),
                    iconKey = CanvasIconKeys.normalize(obj.optString("iconKey", CanvasIconKeys.DOC))
                )
            } catch (_: Exception) {
                SubpageBlockData(title = fallbackTitle.ifBlank { "Nested Sub-Page" })
            }
        }
    }
}

enum class TableAggregation(val label: String) {
    NONE("Off"),
    SUM("Total Sum"),
    AVG("Average"),
    COUNT("Count");

    fun next(): TableAggregation = when (this) {
        NONE -> SUM
        SUM -> AVG
        AVG -> COUNT
        COUNT -> NONE
    }

    companion object {
        fun fromKey(raw: String): TableAggregation = entries.find { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

data class TableBlockData(
    val title: String = "Smart Table",
    val columns: List<String> = listOf("Item", "Qty", "Amount ($)"),
    val rows: List<List<String>> = listOf(
        listOf("Design Sprint", "1", "450.00"),
        listOf("Cloud Hosting", "2", "120.00")
    ),
    val columnAggregations: List<String> = listOf("COUNT", "SUM", "SUM")
) {
    fun normalizedAggregations(): List<TableAggregation> =
        columns.indices.map { idx -> TableAggregation.fromKey(columnAggregations.getOrElse(idx) { "NONE" }) }

    fun computeColumnSummary(colIndex: Int): String {
        val agg = normalizedAggregations().getOrNull(colIndex) ?: TableAggregation.NONE
        if (agg == TableAggregation.NONE) return "—"
        val cells = rows.map { row -> row.getOrElse(colIndex) { "" }.trim() }
        return when (agg) {
            TableAggregation.NONE -> "—"
            TableAggregation.COUNT -> "Cnt: ${cells.count { it.isNotBlank() }}"
            TableAggregation.SUM -> {
                val nums = cells.mapNotNull { parseNumericCell(it) }
                if (nums.isEmpty()) "Sum: 0" else "Sum: ${formatCompactNumber(nums.sum(), hasCurrencySymbol(cells))}"
            }
            TableAggregation.AVG -> {
                val nums = cells.mapNotNull { parseNumericCell(it) }
                if (nums.isEmpty()) "Avg: 0" else "Avg: ${formatCompactNumber(nums.average(), hasCurrencySymbol(cells))}"
            }
        }
    }

    fun addColumn(name: String = "Col ${columns.size + 1}"): TableBlockData = copy(
        columns = columns + name,
        rows = rows.map { it + "" },
        columnAggregations = columnAggregations + TableAggregation.NONE.name
    )

    fun removeColumn(colIndex: Int): TableBlockData {
        if (columns.size <= 1 || colIndex !in columns.indices) return this
        return copy(
            columns = columns.filterIndexed { i, _ -> i != colIndex },
            rows = rows.map { r -> r.filterIndexed { i, _ -> i != colIndex } },
            columnAggregations = columnAggregations.filterIndexed { i, _ -> i != colIndex }
        )
    }

    fun addRow(): TableBlockData = copy(
        rows = rows + listOf(List(columns.size) { "" })
    )

    fun removeRow(rowIndex: Int): TableBlockData {
        if (rows.size <= 1 || rowIndex !in rows.indices) return this
        return copy(rows = rows.filterIndexed { i, _ -> i != rowIndex })
    }

    fun updateCell(rowIndex: Int, colIndex: Int, value: String): TableBlockData {
        if (rowIndex !in rows.indices || colIndex !in columns.indices) return this
        val updatedRows = rows.mapIndexed { rIdx, r ->
            if (rIdx != rowIndex) r
            else List(columns.size) { cIdx -> if (cIdx == colIndex) value else r.getOrElse(cIdx) { "" } }
        }
        return copy(rows = updatedRows)
    }

    fun updateHeader(colIndex: Int, header: String): TableBlockData {
        if (colIndex !in columns.indices) return this
        return copy(columns = columns.mapIndexed { i, c -> if (i == colIndex) header else c })
    }

    fun cycleAggregation(colIndex: Int): TableBlockData {
        if (colIndex !in columns.indices) return this
        val current = normalizedAggregations()
        val updated = current.mapIndexed { i, agg -> if (i == colIndex) agg.next().name else agg.name }
        return copy(columnAggregations = updated)
    }

    fun toJson(): String = JSONObject().apply {
        put("title", title)
        put("columns", JSONArray(columns))
        put("rows", JSONArray().apply {
            rows.forEach { r -> put(JSONArray(r)) }
        })
        put("columnAggregations", JSONArray(columnAggregations))
    }.toString()

    companion object {
        fun parseNumericCell(raw: String): Double? {
            val cleaned = raw.trim()
                .replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace("৳", "")
                .replace(",", "")
                .replace("%", "")
                .trim()
            if (cleaned.isBlank()) return null
            return cleaned.toDoubleOrNull()
        }

        private fun hasCurrencySymbol(cells: List<String>): Boolean =
            cells.any { it.contains("$") || it.contains("€") || it.contains("£") || it.contains("৳") }

        private fun formatCompactNumber(value: Double, currency: Boolean): String {
            val formatted = if (value % 1.0 == 0.0) {
                String.format(Locale.US, "%,.0f", value)
            } else {
                String.format(Locale.US, "%,.2f", value)
            }
            return if (currency) "$$formatted" else formatted
        }

        fun fromJson(json: String): TableBlockData {
            if (json.isBlank()) return TableBlockData()
            return try {
                val obj = JSONObject(json)
                val colsArr = obj.optJSONArray("columns") ?: JSONArray()
                val cols = buildList {
                    for (i in 0 until colsArr.length()) add(colsArr.optString(i, "Col ${i + 1}"))
                }.ifEmpty { listOf("Item", "Qty", "Amount") }

                val rowsArr = obj.optJSONArray("rows") ?: JSONArray()
                val parsedRows = buildList {
                    for (r in 0 until rowsArr.length()) {
                        val rowArr = rowsArr.optJSONArray(r) ?: continue
                        add(List(cols.size) { c -> rowArr.optString(c, "") })
                    }
                }.ifEmpty { listOf(List(cols.size) { "" }) }

                val aggArr = obj.optJSONArray("columnAggregations") ?: JSONArray()
                val aggs = cols.indices.map { i -> aggArr.optString(i, "NONE") }
                TableBlockData(
                    title = obj.optString("title", "Smart Table"),
                    columns = cols,
                    rows = parsedRows,
                    columnAggregations = aggs
                )
            } catch (_: Exception) {
                TableBlockData()
            }
        }
    }
}

data class InvoiceLineItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val qty: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = (qty.coerceAtLeast(0.0) * unitPrice.coerceAtLeast(0.0))
}

data class InvoiceBlockData(
    val mode: String = MODE_INVOICE,
    val docNumber: String = "INV-1042",
    val clientName: String = "",
    val clientEmail: String = "",
    val currencySymbol: String = "$",
    val taxPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val items: List<InvoiceLineItem> = listOf(
        InvoiceLineItem(description = "UI/UX Architecture & Setup", qty = 1.0, unitPrice = 500.0)
    ),
    val notes: String = "Thank you for your business. Payment due within 14 days."
) {
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val safeDiscount: Double get() = discountAmount.coerceIn(0.0, subtotal)
    val taxableBase: Double get() = (subtotal - safeDiscount).coerceAtLeast(0.0)
    val taxAmount: Double get() = taxableBase * (taxPercent.coerceIn(0.0, 100.0) / 100.0)
    val grandTotal: Double get() = (taxableBase + taxAmount).coerceAtLeast(0.0)

    fun formatMoney(amount: Double): String = "$currencySymbol${String.format(Locale.US, "%,.2f", amount)}"

    fun toEmailSubject(): String = "${if (mode == MODE_QUOTATION) "Quotation" else "Invoice"} $docNumber — ${clientName.ifBlank { "Client" }}"

    fun toEmailBody(): String = buildString {
        appendLine("Hello ${clientName.ifBlank { "there" }},")
        appendLine()
        appendLine("Please find your ${if (mode == MODE_QUOTATION) "quotation" else "invoice"} ($docNumber) summary below:")
        appendLine()
        items.forEachIndexed { i, item ->
            appendLine("${i + 1}. ${item.description.ifBlank { "Service" }} — ${item.qty} x ${formatMoney(item.unitPrice)} = ${formatMoney(item.lineTotal)}")
        }
        appendLine()
        appendLine("Subtotal: ${formatMoney(subtotal)}")
        if (safeDiscount > 0.0) appendLine("Discount: -${formatMoney(safeDiscount)}")
        if (taxPercent > 0.0) appendLine("Tax (${String.format(Locale.US, "%.1f", taxPercent)}%): ${formatMoney(taxAmount)}")
        appendLine("Grand Total: ${formatMoney(grandTotal)}")
        if (notes.isNotBlank()) {
            appendLine()
            appendLine(notes)
        }
    }

    fun toJson(): String = JSONObject().apply {
        put("mode", mode)
        put("docNumber", docNumber)
        put("clientName", clientName)
        put("clientEmail", clientEmail)
        put("currencySymbol", currencySymbol)
        put("taxPercent", taxPercent)
        put("discountAmount", discountAmount)
        put("notes", notes)
        put("items", JSONArray().apply {
            items.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("description", item.description)
                    put("qty", item.qty)
                    put("unitPrice", item.unitPrice)
                })
            }
        })
    }.toString()

    companion object {
        const val MODE_INVOICE = "INVOICE"
        const val MODE_QUOTATION = "QUOTATION"

        fun fromJson(json: String): InvoiceBlockData {
            if (json.isBlank()) return InvoiceBlockData()
            return try {
                val obj = JSONObject(json)
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val parsedItems = buildList {
                    for (i in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.optJSONObject(i) ?: continue
                        add(
                            InvoiceLineItem(
                                id = itemObj.optString("id", UUID.randomUUID().toString()),
                                description = itemObj.optString("description", ""),
                                qty = itemObj.optDouble("qty", 1.0),
                                unitPrice = itemObj.optDouble("unitPrice", 0.0)
                            )
                        )
                    }
                }.ifEmpty { listOf(InvoiceLineItem(description = "Service Item", qty = 1.0, unitPrice = 0.0)) }

                InvoiceBlockData(
                    mode = if (obj.optString("mode") == MODE_QUOTATION) MODE_QUOTATION else MODE_INVOICE,
                    docNumber = obj.optString("docNumber", "INV-1042"),
                    clientName = obj.optString("clientName", ""),
                    clientEmail = obj.optString("clientEmail", ""),
                    currencySymbol = obj.optString("currencySymbol", "$").ifBlank { "$" },
                    taxPercent = obj.optDouble("taxPercent", 0.0),
                    discountAmount = obj.optDouble("discountAmount", 0.0),
                    items = parsedItems,
                    notes = obj.optString("notes", "")
                )
            } catch (_: Exception) {
                InvoiceBlockData()
            }
        }
    }
}

data class EmailBlockData(
    val toEmail: String = "",
    val subject: String = "",
    val body: String = ""
) {
    fun toJson(): String = JSONObject().apply {
        put("toEmail", toEmail)
        put("subject", subject)
        put("body", body)
    }.toString()

    companion object {
        fun fromJson(json: String): EmailBlockData {
            if (json.isBlank()) return EmailBlockData()
            return try {
                val obj = JSONObject(json)
                EmailBlockData(
                    toEmail = obj.optString("toEmail", ""),
                    subject = obj.optString("subject", ""),
                    body = obj.optString("body", "")
                )
            } catch (_: Exception) {
                EmailBlockData()
            }
        }
    }
}

data class ReminderBlockData(
    val title: String = "",
    val taggedMemberUid: String = "",
    val taggedMemberName: String = "",
    val dateTimeLabel: String = "In 1 hour",
    val triggerAtMillis: Long = System.currentTimeMillis() + 3_600_000L,
    val ringtoneTitle: String = "Default System Sound",
    val isScheduled: Boolean = false
) {
    fun toJson(): String = JSONObject().apply {
        put("title", title)
        put("taggedMemberUid", taggedMemberUid)
        put("taggedMemberName", taggedMemberName)
        put("dateTimeLabel", dateTimeLabel)
        put("triggerAtMillis", triggerAtMillis)
        put("ringtoneTitle", ringtoneTitle)
        put("isScheduled", isScheduled)
    }.toString()

    companion object {
        fun fromJson(json: String): ReminderBlockData {
            if (json.isBlank()) return ReminderBlockData()
            return try {
                val obj = JSONObject(json)
                ReminderBlockData(
                    title = obj.optString("title", ""),
                    taggedMemberUid = obj.optString("taggedMemberUid", ""),
                    taggedMemberName = obj.optString("taggedMemberName", ""),
                    dateTimeLabel = obj.optString("dateTimeLabel", "In 1 hour"),
                    triggerAtMillis = obj.optLong("triggerAtMillis", System.currentTimeMillis() + 3_600_000L),
                    ringtoneTitle = obj.optString("ringtoneTitle", "Default System Sound"),
                    isScheduled = obj.optBoolean("isScheduled", false)
                )
            } catch (_: Exception) {
                ReminderBlockData()
            }
        }
    }
}

data class ScheduleBlockData(
    val title: String = "",
    val memberUid: String = "",
    val memberName: String = "",
    val dayOfWeek: String = "Mon",
    val timeSlot: String = "10:00 AM - 1:00 PM",
    val status: String = STATUS_PENDING,
    val synced: Boolean = false
) {
    fun toJson(): String = JSONObject().apply {
        put("title", title)
        put("memberUid", memberUid)
        put("memberName", memberName)
        put("dayOfWeek", dayOfWeek)
        put("timeSlot", timeSlot)
        put("status", status)
        put("synced", synced)
    }.toString()

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_WORKING = "WORKING"
        const val STATUS_DONE = "DONE"
        val DAYS = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

        fun fromJson(json: String): ScheduleBlockData {
            if (json.isBlank()) return ScheduleBlockData()
            return try {
                val obj = JSONObject(json)
                ScheduleBlockData(
                    title = obj.optString("title", ""),
                    memberUid = obj.optString("memberUid", ""),
                    memberName = obj.optString("memberName", ""),
                    dayOfWeek = obj.optString("dayOfWeek", "Mon").takeIf { it in DAYS } ?: "Mon",
                    timeSlot = obj.optString("timeSlot", "10:00 AM - 1:00 PM"),
                    status = obj.optString("status", STATUS_PENDING),
                    synced = obj.optBoolean("synced", false)
                )
            } catch (_: Exception) {
                ScheduleBlockData()
            }
        }
    }
}

data class TimerBlockData(
    val label: String = "Deep Work Block",
    val mode: String = MODE_POMODORO,
    val targetMinutes: Int = 25,
    val totalLoggedSeconds: Long = 0L
) {
    fun toJson(): String = JSONObject().apply {
        put("label", label)
        put("mode", mode)
        put("targetMinutes", targetMinutes)
        put("totalLoggedSeconds", totalLoggedSeconds)
    }.toString()

    companion object {
        const val MODE_POMODORO = "POMODORO"
        const val MODE_COUNTDOWN = "COUNTDOWN"
        const val MODE_STOPWATCH = "STOPWATCH"

        fun fromJson(json: String): TimerBlockData {
            if (json.isBlank()) return TimerBlockData()
            return try {
                val obj = JSONObject(json)
                TimerBlockData(
                    label = obj.optString("label", "Deep Work Block"),
                    mode = obj.optString("mode", MODE_POMODORO),
                    targetMinutes = obj.optInt("targetMinutes", 25).coerceIn(1, 240),
                    totalLoggedSeconds = obj.optLong("totalLoggedSeconds", 0L).coerceAtLeast(0L)
                )
            } catch (_: Exception) {
                TimerBlockData()
            }
        }
    }
}

data class VoiceBlockData(
    val caption: String = "Voice Comment",
    val audioUri: String = "",
    val durationMs: Long = 0L,
    val authorName: String = ""
) {
    fun toJson(): String = JSONObject().apply {
        put("caption", caption)
        put("audioUri", audioUri)
        put("durationMs", durationMs)
        put("authorName", authorName)
    }.toString()

    companion object {
        fun fromJson(json: String): VoiceBlockData {
            if (json.isBlank()) return VoiceBlockData()
            return try {
                val obj = JSONObject(json)
                VoiceBlockData(
                    caption = obj.optString("caption", "Voice Comment"),
                    audioUri = obj.optString("audioUri", ""),
                    durationMs = obj.optLong("durationMs", 0L).coerceAtLeast(0L),
                    authorName = obj.optString("authorName", "")
                )
            } catch (_: Exception) {
                VoiceBlockData()
            }
        }
    }
}

data class SecretBlockData(
    val label: String = "API Key / Password Vault",
    val usernameOrHint: String = "",
    val secretValue: String = "",
    val isLocked: Boolean = true
) {
    fun toJson(): String = JSONObject().apply {
        put("label", label)
        put("usernameOrHint", usernameOrHint)
        put("secretValue", secretValue)
        put("isLocked", isLocked)
    }.toString()

    companion object {
        fun fromJson(json: String): SecretBlockData {
            if (json.isBlank()) return SecretBlockData()
            return try {
                val obj = JSONObject(json)
                SecretBlockData(
                    label = obj.optString("label", "API Key / Password Vault"),
                    usernameOrHint = obj.optString("usernameOrHint", ""),
                    secretValue = obj.optString("secretValue", ""),
                    isLocked = obj.optBoolean("isLocked", true)
                )
            } catch (_: Exception) {
                SecretBlockData()
            }
        }
    }
}

data class ClientTrackerBlockData(
    val clientName: String = "",
    val projectName: String = "",
    val totalBudgetCents: Long = 0L,
    val advancePaidCents: Long = 0L,
    val syncedToLedger: Boolean = false
) {
    val remainingDueCents: Long get() = (totalBudgetCents - advancePaidCents).coerceAtLeast(0L)

    fun formatDollars(cents: Long): String = "$" + String.format(Locale.US, "%,.2f", cents / 100.0)

    fun toJson(): String = JSONObject().apply {
        put("clientName", clientName)
        put("projectName", projectName)
        put("totalBudgetCents", totalBudgetCents)
        put("advancePaidCents", advancePaidCents)
        put("syncedToLedger", syncedToLedger)
    }.toString()

    companion object {
        fun parseMoneyToCents(raw: String): Long {
            val cleaned = raw.replace("$", "").replace(",", "").trim()
            val num = cleaned.toDoubleOrNull() ?: return 0L
            return (num.coerceAtLeast(0.0) * 100.0).roundToLong()
        }

        fun fromJson(json: String): ClientTrackerBlockData {
            if (json.isBlank()) return ClientTrackerBlockData()
            return try {
                val obj = JSONObject(json)
                ClientTrackerBlockData(
                    clientName = obj.optString("clientName", ""),
                    projectName = obj.optString("projectName", ""),
                    totalBudgetCents = obj.optLong("totalBudgetCents", 0L).coerceAtLeast(0L),
                    advancePaidCents = obj.optLong("advancePaidCents", 0L).coerceAtLeast(0L),
                    syncedToLedger = obj.optBoolean("syncedToLedger", false)
                )
            } catch (_: Exception) {
                ClientTrackerBlockData()
            }
        }
    }
}

data class EmbedBlockData(
    val embedKind: String = KIND_WEBSITE,
    val title: String = "",
    val url: String = "",
    val subtitle: String = ""
) {
    fun toJson(): String = JSONObject().apply {
        put("embedKind", embedKind)
        put("title", title)
        put("url", url)
        put("subtitle", subtitle)
    }.toString()

    companion object {
        const val KIND_WEBSITE = "WEBSITE"
        const val KIND_DRIVE = "DRIVE"

        fun fromJson(json: String, defaultKind: String = KIND_WEBSITE): EmbedBlockData {
            if (json.isBlank()) return EmbedBlockData(embedKind = defaultKind)
            return try {
                val obj = JSONObject(json)
                EmbedBlockData(
                    embedKind = obj.optString("embedKind", defaultKind),
                    title = obj.optString("title", ""),
                    url = obj.optString("url", ""),
                    subtitle = obj.optString("subtitle", "")
                )
            } catch (_: Exception) {
                EmbedBlockData(embedKind = defaultKind)
            }
        }
    }
}

data class SmartCanvasTemplate(
    val id: String,
    val label: String,
    val iconKey: String,
    val defaultTitle: String,
    val tagsCsv: String,
    val buildBlocks: () -> List<WorkspaceBlock>
)

object SmartCanvasStarterTemplates {
    val TEMPLATES: List<SmartCanvasTemplate> = listOf(
        SmartCanvasTemplate(
            id = "blank_notepad",
            label = "Blank Notepad",
            iconKey = CanvasIconKeys.DOC,
            defaultTitle = "",
            tagsCsv = "",
            buildBlocks = {
                listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = "")
                )
            }
        ),
        SmartCanvasTemplate(
            id = "client_invoice",
            label = "Client Invoice & Proposal",
            iconKey = CanvasIconKeys.INVOICE,
            defaultTitle = "Client Proposal & Invoice",
            tagsCsv = "client, invoice, proposal",
            buildBlocks = {
                val invoiceData = InvoiceBlockData(
                    mode = InvoiceBlockData.MODE_INVOICE,
                    docNumber = "INV-2026",
                    clientName = "Client Studio",
                    taxPercent = 5.0,
                    items = listOf(
                        InvoiceLineItem(description = "Product Design & Prototype", qty = 1.0, unitPrice = 1200.0),
                        InvoiceLineItem(description = "Mobile Architecture Setup", qty = 1.0, unitPrice = 800.0)
                    )
                )
                val trackerData = ClientTrackerBlockData(
                    clientName = "Client Studio",
                    projectName = "Mobile App Build",
                    totalBudgetCents = 200_000L,
                    advancePaidCents = 60_000L
                )
                listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Project Scope & Budget"),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_CLIENT,
                        text = trackerData.projectName,
                        metadataJson = trackerData.toJson()
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_INVOICE,
                        text = invoiceData.docNumber,
                        metadataJson = invoiceData.toJson()
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_EMAIL,
                        text = "Send Invoice to Client",
                        metadataJson = EmailBlockData(
                            subject = invoiceData.toEmailSubject(),
                            body = invoiceData.toEmailBody()
                        ).toJson()
                    )
                )
            }
        ),
        SmartCanvasTemplate(
            id = "weekly_sprint",
            label = "Weekly Team Sprint",
            iconKey = CanvasIconKeys.ROCKET,
            defaultTitle = "Weekly Team Sprint",
            tagsCsv = "sprint, team, schedule",
            buildBlocks = {
                val sprintTable = TableBlockData(
                    title = "Sprint Capacity & Deliverables",
                    columns = listOf("Owner", "Deliverable", "Est. Hrs"),
                    rows = listOf(
                        listOf("@Lead", "API & Auth Verification", "6"),
                        listOf("@Design", "Liquid Glass Polish", "4"),
                        listOf("@QA", "Release Checklist", "3")
                    ),
                    columnAggregations = listOf("COUNT", "NONE", "SUM")
                )
                val sched = ScheduleBlockData(
                    title = "Ship Sprint Release Candidate",
                    dayOfWeek = "Thu",
                    timeSlot = "2:00 PM - 5:00 PM",
                    status = ScheduleBlockData.STATUS_WORKING
                )
                listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Sprint Objectives"),
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_CALLOUT, text = "Align deliverables, owner slots, and daily review targets."),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_SCHEDULE,
                        text = sched.title,
                        metadataJson = sched.toJson()
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_TABLE,
                        text = sprintTable.title,
                        metadataJson = sprintTable.toJson()
                    ),
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_CHECKLIST, text = "Confirm staging build passes all smoke tests", checked = false)
                )
            }
        ),
        SmartCanvasTemplate(
            id = "product_spec",
            label = "Product / App Spec Sheet",
            iconKey = CanvasIconKeys.BRAIN,
            defaultTitle = "Product & App Spec Sheet",
            tagsCsv = "spec, architecture, dev",
            buildBlocks = {
                val secret = SecretBlockData(
                    label = "Staging API Credentials",
                    usernameOrHint = "Environment: staging-v2",
                    secretValue = "sk_live_replace_with_token",
                    isLocked = true
                )
                val embed = EmbedBlockData(
                    embedKind = EmbedBlockData.KIND_WEBSITE,
                    title = "System Architecture Docs",
                    url = "https://github.com",
                    subtitle = "Repository & technical wiki"
                )
                listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Core Architecture"),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_TOGGLE,
                        text = "Authentication & Data Flow",
                        details = "Single source of truth via Room + Firestore real-time listeners.",
                        isExpanded = true
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_CODE,
                        text = "val stateFlow = repository.observeStream()\n    .stateIn(scope, SharingStarted.WhileSubscribed(5000), initial)"
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_SECRET,
                        text = secret.label,
                        metadataJson = secret.toJson()
                    ),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_EMBED,
                        text = embed.title,
                        details = embed.url,
                        metadataJson = embed.toJson()
                    )
                )
            }
        ),
        SmartCanvasTemplate(
            id = "monthly_budget",
            label = "Monthly Budget & Expense Sheet",
            iconKey = CanvasIconKeys.WALLET,
            defaultTitle = "Monthly Budget & Expense Sheet",
            tagsCsv = "finance, budget, ledger",
            buildBlocks = {
                val budgetTable = TableBlockData(
                    title = "Monthly Budget Breakdown",
                    columns = listOf("Category", "Planned ($)", "Actual ($)"),
                    rows = listOf(
                        listOf("Cloud & Infrastructure", "180.00", "165.00"),
                        listOf("Software Tools", "120.00", "118.50"),
                        listOf("Design Assets", "90.00", "60.00")
                    ),
                    columnAggregations = listOf("COUNT", "SUM", "SUM")
                )
                listOf(
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Cashflow & Expense Summary"),
                    WorkspaceBlock(
                        type = WorkspaceBlock.TYPE_TABLE,
                        text = budgetTable.title,
                        metadataJson = budgetTable.toJson()
                    ),
                    WorkspaceBlock(type = WorkspaceBlock.TYPE_CHECKLIST, text = "Reconcile monthly bank & ledger statements", checked = false)
                )
            }
        )
    )
}
