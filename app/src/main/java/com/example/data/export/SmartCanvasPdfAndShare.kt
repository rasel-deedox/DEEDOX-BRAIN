package com.example.data.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.repository.ClientTrackerBlockData
import com.example.data.repository.EmailBlockData
import com.example.data.repository.EmbedBlockData
import com.example.data.repository.InvoiceBlockData
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.SecretBlockData
import com.example.data.repository.SubpageBlockData
import com.example.data.repository.TableBlockData
import com.example.data.repository.TimerBlockData
import com.example.data.repository.VoiceBlockData
import com.example.data.repository.WorkspaceBlock
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

object SmartCanvasPdfAndShare {

    private const val PAGE_WIDTH = 595   // A4 points @72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 84

    fun generatePagePdfFile(
        context: Context,
        title: String,
        subtitle: String,
        blocks: List<WorkspaceBlock>
    ): File {
        val safeTitle = title.ifBlank { "Untitled Page" }
        val sanitizedSlug = safeTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "page" }
        val outDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val outFile = File(outDir, "${sanitizedSlug}_${System.currentTimeMillis()}.pdf")

        val nativeRendered = runCatching {
            val pdf = PdfDocument()
            try {
                var pageNumber = 1
                var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                var canvas = page.canvas
                var y = drawPageBackdropAndHeader(canvas, safeTitle, subtitle)

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
                    pdf.finishPage(page)
                    pageNumber += 1
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    drawWarmCreamBackground(canvas)
                    y = MARGIN + 10f
                }
            }

            blocks.filterNot { it.type == WorkspaceBlock.TYPE_PAGE_META }.forEach { block ->
                when (block.type) {
                    WorkspaceBlock.TYPE_DIVIDER -> {
                        ensureSpace(20f)
                        val linePaint = Paint().apply {
                            color = Color.parseColor("#D9D3C7")
                            strokeWidth = 1.5f
                        }
                        canvas.drawLine(MARGIN, y + 8f, PAGE_WIDTH - MARGIN, y + 8f, linePaint)
                        y += 20f
                    }
                    WorkspaceBlock.TYPE_TABLE -> {
                        val table = TableBlockData.fromJson(block.metadataJson)
                        val rowHeight = 24f
                        val headerHeight = 28f
                        val totalNeeded = 26f + headerHeight + (table.rows.size * rowHeight) + 32f
                        ensureSpace(totalNeeded.coerceAtMost(360f))
                        y = drawSectionLabel(canvas, table.title.ifBlank { "Smart Table" }, y)
                        val cols = table.columns.size.coerceAtLeast(1)
                        val colWidth = CONTENT_WIDTH.toFloat() / cols
                        val borderPaint = Paint().apply {
                            style = Paint.Style.STROKE
                            color = Color.parseColor("#C7C0B3")
                            strokeWidth = 1f
                        }
                        val headerFill = Paint().apply { color = Color.parseColor("#E6E1D6") }
                        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#181715")
                            textSize = 10f
                        }
                        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#181715")
                            textSize = 10.5f
                            typeface = Typeface.DEFAULT_BOLD
                        }

                        // Header row
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerHeight, headerFill)
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerHeight, borderPaint)
                        table.columns.forEachIndexed { cIdx, colName ->
                            val x = MARGIN + cIdx * colWidth
                            canvas.drawLine(x, y, x, y + headerHeight, borderPaint)
                            canvas.drawText(colName.take(16), x + 6f, y + 18f, boldPaint)
                        }
                        y += headerHeight

                        // Data rows
                        table.rows.forEach { row ->
                            ensureSpace(rowHeight + 8f)
                            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
                            for (cIdx in 0 until cols) {
                                val x = MARGIN + cIdx * colWidth
                                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                                canvas.drawText(row.getOrElse(cIdx) { "" }.take(18), x + 6f, y + 16f, textPaint)
                            }
                            y += rowHeight
                        }

                        // Footer calculation row
                        val footerFill = Paint().apply { color = Color.parseColor("#F4ECEA") }
                        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#C62828")
                            textSize = 10f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight, footerFill)
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
                        for (cIdx in 0 until cols) {
                            val x = MARGIN + cIdx * colWidth
                            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                            canvas.drawText(table.computeColumnSummary(cIdx).take(18), x + 6f, y + 16f, footerPaint)
                        }
                        y += rowHeight + 14f
                    }
                    WorkspaceBlock.TYPE_INVOICE -> {
                        val inv = InvoiceBlockData.fromJson(block.metadataJson)
                        ensureSpace(160f)
                        y = drawInvoiceCardToPdf(canvas, inv, y, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_CLIENT -> {
                        val client = ClientTrackerBlockData.fromJson(block.metadataJson)
                        val lines = listOf(
                            "CLIENT TRACKER: ${client.clientName.ifBlank { "Client" }} — ${client.projectName.ifBlank { block.text }}",
                            "Total Budget: ${client.formatDollars(client.totalBudgetCents)}   |   Advance Paid: ${client.formatDollars(client.advancePaidCents)}   |   Remaining Due: ${client.formatDollars(client.remainingDueCents)}"
                        )
                        y = drawBoxedLines(canvas, lines, y, ::ensureSpace, accentLeftBar = true)
                    }
                    WorkspaceBlock.TYPE_SCHEDULE -> {
                        val sched = ScheduleBlockData.fromJson(block.metadataJson)
                        val text = "[${sched.status}] ${sched.title.ifBlank { block.text }} • @${sched.memberName.ifBlank { "Team" }} (${sched.dayOfWeek}, ${sched.timeSlot})"
                        y = drawWrappedParagraph(canvas, text, y, 11.5f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_SECRET -> {
                        val sec = SecretBlockData.fromJson(block.metadataJson)
                        y = drawBoxedLines(
                            canvas,
                            listOf("PROTECTED VAULT: ${sec.label}", "[Secret Masked & Redacted in PDF Export]"),
                            y,
                            ::ensureSpace,
                            accentLeftBar = true
                        )
                    }
                    WorkspaceBlock.TYPE_EMAIL -> {
                        val mail = EmailBlockData.fromJson(block.metadataJson)
                        y = drawBoxedLines(
                            canvas,
                            listOf("EMAIL DRAFT -> ${mail.toEmail.ifBlank { "Recipient" }}", "Subject: ${mail.subject}", mail.body),
                            y,
                            ::ensureSpace
                        )
                    }
                    WorkspaceBlock.TYPE_SUBPAGE -> {
                        val sub = SubpageBlockData.fromJson(block.metadataJson, block.text)
                        y = drawWrappedParagraph(canvas, "[Sub-Page] ${sub.title} — ${sub.summary}", y, 11.5f, bold = true, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_REMINDER -> {
                        val rem = ReminderBlockData.fromJson(block.metadataJson)
                        y = drawWrappedParagraph(canvas, "[Reminder] ${rem.title.ifBlank { block.text }} • ${rem.dateTimeLabel} (${rem.taggedMemberName.ifBlank { "Personal" }})", y, 11f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_TIMER -> {
                        val tm = TimerBlockData.fromJson(block.metadataJson)
                        y = drawWrappedParagraph(canvas, "[Focus Log] ${tm.label} • ${tm.totalLoggedSeconds / 60} min tracked", y, 11f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_VOICE -> {
                        val vc = VoiceBlockData.fromJson(block.metadataJson)
                        y = drawWrappedParagraph(canvas, "[Voice Audio Note] ${vc.caption} (${vc.durationMs / 1000}s)", y, 11f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_EMBED, WorkspaceBlock.TYPE_DRIVE_EMBED -> {
                        val em = EmbedBlockData.fromJson(block.metadataJson)
                        y = drawWrappedParagraph(canvas, "[Link] ${em.title.ifBlank { block.text }} — ${em.url.ifBlank { block.details }}", y, 11f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_CODE -> {
                        y = drawBoxedLines(canvas, listOf(block.text), y, ::ensureSpace, monospace = true)
                    }
                    WorkspaceBlock.TYPE_CALLOUT -> {
                        y = drawBoxedLines(canvas, listOf(block.text), y, ::ensureSpace, accentLeftBar = true)
                    }
                    WorkspaceBlock.TYPE_H1 -> {
                        y = drawWrappedParagraph(canvas, block.text, y, 18f, bold = true, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_H2 -> {
                        y = drawWrappedParagraph(canvas, block.text, y, 14.5f, bold = true, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_H3 -> {
                        y = drawWrappedParagraph(canvas, block.text, y, 12.5f, bold = true, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_CHECKLIST -> {
                        val mark = if (block.checked) "[x]" else "[ ]"
                        y = drawWrappedParagraph(canvas, "$mark ${block.text}", y, 11.5f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_BULLET -> {
                        y = drawWrappedParagraph(canvas, "• ${block.text}", y, 11.5f, bold = false, ::ensureSpace)
                    }
                    WorkspaceBlock.TYPE_TOGGLE -> {
                        y = drawWrappedParagraph(canvas, "> ${block.text}", y, 12f, bold = true, ::ensureSpace)
                        if (block.details.isNotBlank()) {
                            y = drawWrappedParagraph(canvas, "   ${block.details}", y, 11f, bold = false, ::ensureSpace)
                        }
                    }
                    else -> {
                        if (block.text.isNotBlank()) {
                            y = drawWrappedParagraph(canvas, block.text, y, 11.5f, bold = false, ::ensureSpace)
                        }
                    }
                }
            }

                pdf.finishPage(page)
                FileOutputStream(outFile).use { fos -> pdf.writeTo(fos) }
            } finally {
                runCatching { pdf.close() }
            }
        }.isSuccess
        if (!nativeRendered || !outFile.exists() || outFile.length() == 0L) {
            writeMinimalTextPdfFallback(outFile, safeTitle, subtitle, blocks)
        }
        return outFile
    }

    private fun writeMinimalTextPdfFallback(
        outFile: File,
        title: String,
        subtitle: String,
        blocks: List<WorkspaceBlock>
    ) {
        val lines = buildList {
            add(title)
            if (subtitle.isNotBlank()) add(subtitle)
            add("------------------------------------------------------------")
            blocks.filterNot { it.type == WorkspaceBlock.TYPE_PAGE_META }.forEach { block ->
                when (block.type) {
                    WorkspaceBlock.TYPE_INVOICE -> {
                        val inv = InvoiceBlockData.fromJson(block.metadataJson)
                        add("INVOICE ${inv.docNumber} | Client: ${inv.clientName} | Total: ${inv.formatMoney(inv.grandTotal)}")
                    }
                    WorkspaceBlock.TYPE_TABLE -> {
                        val tbl = TableBlockData.fromJson(block.metadataJson)
                        add("TABLE: ${tbl.title}")
                        add(tbl.columns.joinToString(" | "))
                        tbl.rows.forEach { r -> add(r.joinToString(" | ")) }
                        add(tbl.columns.indices.joinToString(" | ") { tbl.computeColumnSummary(it) })
                    }
                    WorkspaceBlock.TYPE_CLIENT -> {
                        val c = ClientTrackerBlockData.fromJson(block.metadataJson)
                        add("CLIENT: ${c.clientName} (${c.projectName}) | Remaining Due: ${c.formatDollars(c.remainingDueCents)}")
                    }
                    WorkspaceBlock.TYPE_SECRET -> {
                        val s = SecretBlockData.fromJson(block.metadataJson)
                        add("PROTECTED VAULT: ${s.label} [REDACTED]")
                    }
                    else -> if (block.text.isNotBlank()) add(block.text)
                }
            }
        }.take(45)

        fun escapePdfText(s: String): String = buildString {
            s.take(96).forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '(' -> append("\\(")
                    ')' -> append("\\)")
                    in '\u0020'..'\u007E' -> append(ch)
                    else -> append(' ')
                }
            }
        }

        val streamContent = buildString {
            appendLine("BT")
            appendLine("/F1 11 Tf")
            var cursorY = 790
            lines.forEach { line ->
                appendLine("1 0 0 1 42 $cursorY Tm (${escapePdfText(line)}) Tj")
                cursorY -= 16
            }
            appendLine("ET")
        }

        val objects = listOf(
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n",
            "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
            "5 0 obj << /Length ${streamContent.toByteArray(Charsets.US_ASCII).size} >>\nstream\n$streamContent\nendstream\nendobj\n"
        )

        val pdfBytes = buildString {
            append("%PDF-1.4\n")
            val offsets = mutableListOf<Int>()
            objects.forEach { obj ->
                offsets += length
                append(obj)
            }
            val xrefOffset = length
            append("xref\n0 ${objects.size + 1}\n")
            append("0000000000 65535 f \n")
            offsets.forEach { off ->
                append(String.format(java.util.Locale.US, "%010d 00000 n \n", off))
            }
            append("trailer << /Size ${objects.size + 1} /Root 1 0 R >>\n")
            append("startxref\n$xrefOffset\n%%EOF\n")
        }
        FileOutputStream(outFile).use { fos -> fos.write(pdfBytes.toByteArray(Charsets.US_ASCII)) }
    }

    fun generateStandaloneInvoicePdfFile(context: Context, invoice: InvoiceBlockData): File {
        val block = WorkspaceBlock(
            type = WorkspaceBlock.TYPE_INVOICE,
            text = invoice.docNumber,
            metadataJson = invoice.toJson()
        )
        val kind = if (invoice.mode == InvoiceBlockData.MODE_QUOTATION) "Quotation" else "Invoice"
        return generatePagePdfFile(
            context = context,
            title = "$kind ${invoice.docNumber}",
            subtitle = "Client: ${invoice.clientName.ifBlank { "Valued Client" }} • Total: ${invoice.formatMoney(invoice.grandTotal)}",
            blocks = listOf(block)
        )
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    fun generateAndCopyShareLink(
        context: Context,
        pageId: String,
        title: String,
        blocks: List<WorkspaceBlock>
    ): String {
        val cleanId = pageId.ifBlank { java.util.UUID.randomUUID().toString() }.take(24)
        val shareUrl = "https://personalaidocs.app/view/$cleanId"
        val summaryText = buildString {
            appendLine(title.ifBlank { "Untitled Page" })
            appendLine("Read-Only View Link: $shareUrl")
            appendLine()
            appendLine(WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(blocks)).take(1200))
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Shared Page Link", shareUrl))

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Shared Page" })
            putExtra(Intent.EXTRA_TEXT, summaryText)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Read-Only Page Link").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
        return shareUrl
    }

    fun launchEmailComposer(
        context: Context,
        toEmail: String,
        subject: String,
        body: String
    ): Boolean {
        val mailUri = Uri.parse("mailto:${Uri.encode(toEmail.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail.trim()))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = runCatching { context.startActivity(intent); true }.getOrDefault(false)
        if (!started) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail.trim()))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            val chooser = Intent.createChooser(fallback, "Send Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return runCatching { context.startActivity(chooser); true }.getOrDefault(false)
        }
        return true
    }

    private fun drawWarmCreamBackground(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#FAF8F4"))
    }

    private fun drawPageBackdropAndHeader(canvas: Canvas, title: String, subtitle: String): Float {
        drawWarmCreamBackground(canvas)
        val accentPaint = Paint().apply { color = Color.parseColor("#E53935") }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, accentPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181715")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B645C")
            textSize = 10.5f
        }
        val dateStamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())
        val layout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, CONTENT_WIDTH).build()
        canvas.save()
        canvas.translate(MARGIN, 36f)
        layout.draw(canvas)
        canvas.restore()

        var y = 36f + layout.height + 6f
        val subLine = if (subtitle.isNotBlank()) "$subtitle  •  Exported $dateStamp" else "Exported $dateStamp"
        canvas.drawText(subLine.take(92), MARGIN, y + 10f, subPaint)
        y += 22f

        val rulePaint = Paint().apply {
            color = Color.parseColor("#DCD6CA")
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        return y + 16f
    }

    private fun drawSectionLabel(canvas: Canvas, label: String, y: Float): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181715")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(label, MARGIN, y + 12f, paint)
        return y + 20f
    }

    private fun drawWrappedParagraph(
        canvas: Canvas,
        text: String,
        startY: Float,
        textSizeSp: Float,
        bold: Boolean,
        ensureSpace: (Float) -> Unit
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181715")
            textSize = textSizeSp
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH).build()
        ensureSpace(layout.height + 10f)
        canvas.save()
        canvas.translate(MARGIN, startY)
        layout.draw(canvas)
        canvas.restore()
        return startY + layout.height + 9f
    }

    private fun drawBoxedLines(
        canvas: Canvas,
        lines: List<String>,
        startY: Float,
        ensureSpace: (Float) -> Unit,
        accentLeftBar: Boolean = false,
        monospace: Boolean = false
    ): Float {
        val combined = lines.filter { it.isNotBlank() }.joinToString("\n").ifBlank { " " }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181715")
            textSize = 10.5f
            typeface = if (monospace) Typeface.MONOSPACE else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(combined, 0, combined.length, paint, CONTENT_WIDTH - 24).build()
        val boxHeight = layout.height + 18f
        ensureSpace(boxHeight + 10f)

        val bgPaint = Paint().apply { color = Color.parseColor("#EFECE6") }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#D5CEC2")
            strokeWidth = 1f
        }
        canvas.drawRoundRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + boxHeight, 8f, 8f, bgPaint)
        canvas.drawRoundRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + boxHeight, 8f, 8f, borderPaint)
        if (accentLeftBar) {
            val barPaint = Paint().apply { color = Color.parseColor("#E53935") }
            canvas.drawRect(MARGIN, startY + 2f, MARGIN + 4f, startY + boxHeight - 2f, barPaint)
        }
        canvas.save()
        canvas.translate(MARGIN + 12f, startY + 9f)
        layout.draw(canvas)
        canvas.restore()
        return startY + boxHeight + 10f
    }

    private fun drawInvoiceCardToPdf(
        canvas: Canvas,
        inv: InvoiceBlockData,
        startY: Float,
        ensureSpace: (Float) -> Unit
    ): Float {
        val lines = buildList {
            val kind = if (inv.mode == InvoiceBlockData.MODE_QUOTATION) "QUOTATION" else "INVOICE"
            add("$kind ${inv.docNumber}  •  Client: ${inv.clientName.ifBlank { "Client" }} (${inv.clientEmail.ifBlank { "No email" }})")
            inv.items.forEachIndexed { i, it ->
                add("${i + 1}. ${it.description.ifBlank { "Service" }}   x${it.qty} @ ${inv.formatMoney(it.unitPrice)} = ${inv.formatMoney(it.lineTotal)}")
            }
            add("Subtotal: ${inv.formatMoney(inv.subtotal)}   |   Discount: -${inv.formatMoney(inv.safeDiscount)}   |   Tax: ${inv.formatMoney(inv.taxAmount)}")
            add("GRAND TOTAL: ${inv.formatMoney(inv.grandTotal)}")
            if (inv.notes.isNotBlank()) add(inv.notes)
        }
        return drawBoxedLines(canvas, lines, startY, ensureSpace, accentLeftBar = true)
    }
}
