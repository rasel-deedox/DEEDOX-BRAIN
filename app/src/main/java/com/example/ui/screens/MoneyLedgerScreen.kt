package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.repository.LedgerTimeFilter
import com.example.data.repository.OsRepository
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.FinanceIconCatalog
import com.example.ui.components.FinanceIconPickerDialog
import com.example.ui.components.FinanceIconSquircleBadge
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.LiquidTimePickerDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.formatTime12h
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val SECTOR_COLOR_SWATCHES = listOf(
    "#E53935",
    "#2E7D32",
    "#1565C0",
    "#EF6C00",
    "#6A1B9A",
    "#00897B"
)

private val PAYMENT_METHODS = listOf(
    "CASH" to "Cash",
    "BANK" to "Bank",
    "MOBILE_BANKING" to "Mobile Banking",
    "CARD" to "Card"
)

@Composable
fun MoneyLedgerScreen(
    sectors: List<FinanceSectorEntity>,
    transactions: List<FinanceTransactionEntity>,
    recentIconKeys: List<String> = emptyList(),
    txIconOverrides: Map<String, String> = emptyMap(),
    onRecordRecentIcon: (String) -> Unit = {},
    onCreateSector: (name: String, iconKey: String, colorHex: String, typeAffinity: String) -> Unit,
    onDeleteSector: (String) -> Unit,
    onLogTransaction: (
        txType: String,
        amountCents: Long,
        sector: FinanceSectorEntity,
        paymentMethod: String,
        notes: String,
        occurredAtMillis: Long,
        iconKey: String
    ) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val palette = LiquidTheme.palette
    var timeFilter by remember { mutableStateOf(LedgerTimeFilter.THIS_MONTH) }
    var selectedSectorId by remember { mutableStateOf<String?>(null) }
    var customStartMillis by remember { mutableStateOf<Long?>(null) }
    var customEndMillis by remember { mutableStateOf<Long?>(null) }

    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var showCreateSectorDialog by remember { mutableStateOf(false) }
    var showLogEntryDialog by remember { mutableStateOf(false) }

    var localRecentIcons by remember(recentIconKeys) { mutableStateOf(recentIconKeys) }
    val recordIcon: (String) -> Unit = { key ->
        val normalized = FinanceIconCatalog.normalizeKey(key)
        localRecentIcons = (listOf(normalized) + localRecentIcons.filterNot { it == normalized }).take(5)
        onRecordRecentIcon(normalized)
    }

    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val sectorById = remember(sectors) { sectors.associateBy { it.id } }
    val fallbackSectorKeys = remember(sectors) { sectors.map { it.iconKey } }

    val filteredTransactions = remember(
        transactions,
        timeFilter,
        selectedSectorId,
        customStartMillis,
        customEndMillis
    ) {
        OsRepository.filterTransactions(
            transactions = transactions,
            filter = timeFilter,
            selectedSectorId = selectedSectorId,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis
        )
    }

    val summary = remember(filteredTransactions, sectors) {
        OsRepository.computeLedgerSummary(filteredTransactions, sectors)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .testTag("money_ledger_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "money_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingGlassIconButton(
                        iconType = MinimalIconType.BACK,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.testTag("money_back_button")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Money & Ledger",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Real cashflow, custom sectors & exact timestamps",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                    OutlinedButton(
                        onClick = { showCreateSectorDialog = true },
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("money_new_sector_button")
                    ) {
                        Text("+ Sector", color = palette.primaryInk, style = MaterialTheme.typography.labelMedium)
                    }
                    TactilePrimaryPillButton(
                        text = "+ Entry",
                        iconType = MinimalIconType.PLUS,
                        enabled = sectors.isNotEmpty(),
                        onClick = { showLogEntryDialog = true },
                        modifier = Modifier.testTag("money_log_entry_button")
                    )
                }
            }

            // Balance Hero Card
            item(key = "money_balance_hero", contentType = "balance_hero") {
                FeaturedCrimsonBronzeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("money_balance_hero_card"),
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Net Balance (${timeFilter.label})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryInk
                                )
                                Text(
                                    text = OsRepository.formatCurrencyCents(summary.netBalanceCents),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk,
                                    modifier = Modifier.testTag("money_net_balance_text")
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = "Savings ${summary.savingsRatePercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.savingsRatePercent >= 0) Color(0xFF10B981) else CrimsonAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.cardSurfaceTop.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                    Text(
                                        text = OsRepository.formatCurrencyCents(summary.totalIncomeCents, includeSign = true),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.cardSurfaceTop.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                    Text(
                                        text = OsRepository.formatCurrencyCents(-summary.totalExpenseCents),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonAccent
                                    )
                                }
                            }
                        }

                        if (summary.expenseBreakdown.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Expense Allocation by Sector",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                summary.expenseBreakdown.take(5).forEach { bar ->
                                    val animatedFraction by animateFloatAsState(
                                        targetValue = bar.fraction,
                                        label = "sector_bar_${bar.sectorId}"
                                    )
                                    val barColor = remember(bar.colorHex) { parseSafeHexColor(bar.colorHex, CrimsonAccent) }
                                    val barIconKey = sectorById[bar.sectorId]?.iconKey
                                    val barIconSpec = remember(barIconKey) { FinanceIconCatalog.resolve(barIconKey) }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = barIconSpec.vector,
                                                    contentDescription = null,
                                                    tint = barColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(bar.sectorName, style = MaterialTheme.typography.labelMedium, color = palette.primaryInk)
                                            }
                                            Text(
                                                text = OsRepository.formatCurrencyCents(bar.totalCents),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primaryInk
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(9999.dp))
                                                .background(palette.recessedTrayBg)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.03f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(9999.dp))
                                                    .background(barColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Time Filters & Sector Filters
            item(key = "money_filters", contentType = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LedgerTimeFilter.entries.forEach { f ->
                            val selected = timeFilter == f
                            Surface(
                                onClick = {
                                    timeFilter = f
                                    if (f == LedgerTimeFilter.CUSTOM) {
                                        showCustomRangeDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.testTag("money_time_filter_${f.name.lowercase()}")
                            ) {
                                Text(
                                    text = f.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { selectedSectorId = null },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selectedSectorId == null) CrimsonAccent else palette.recessedTrayBg,
                            modifier = Modifier.testTag("money_sector_all")
                        ) {
                            Text(
                                text = "All Sectors",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedSectorId == null) Color.White else palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                        sectors.forEach { sector ->
                            val selected = selectedSectorId == sector.id
                            val dotColor = remember(sector.colorHex) { parseSafeHexColor(sector.colorHex, CrimsonAccent) }
                            val iconSpec = remember(sector.iconKey) { FinanceIconCatalog.resolve(sector.iconKey) }
                            Surface(
                                onClick = { selectedSectorId = if (selected) null else sector.id },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.testTag("money_sector_chip_${sector.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = if (sector.isCustom) 4.dp else 12.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selected) dotColor.copy(alpha = 0.25f)
                                                else dotColor.copy(alpha = 0.14f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconSpec.vector,
                                            contentDescription = null,
                                            tint = if (selected) palette.primaryButtonContent else dotColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = sector.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) palette.primaryButtonContent else palette.primaryInk
                                    )
                                    if (sector.isCustom) {
                                        IconButton(
                                            onClick = {
                                                pendingDeleteTitle = "Delete Custom Sector?"
                                                pendingDeleteMessage = "Remove sector \"${sector.name}\"? Existing ledger records keep their historical names."
                                                pendingDeleteAction = {
                                                    if (selectedSectorId == sector.id) selectedSectorId = null
                                                    onDeleteSector(sector.id)
                                                }
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            MinimalIcon(
                                                type = MinimalIconType.TRASH,
                                                tint = CrimsonAccent,
                                                size = 12.dp,
                                                contentDescription = "Delete custom sector"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item(key = "money_empty", contentType = "empty") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No ledger entries in ${timeFilter.label}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Tap \"+ Entry\" to log income, expenses, or transfers with categorized minimal icons and exact timestamps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }
            } else {
                items(items = filteredTransactions, key = { it.id }, contentType = { "tx_card" }) { tx ->
                    val signedCents = when (tx.txType) {
                        "INCOME" -> tx.amountCents
                        "EXPENSE" -> -tx.amountCents
                        else -> tx.amountCents
                    }
                    val amountText = when (tx.txType) {
                        "TRANSFER" -> OsRepository.formatCurrencyCents(tx.amountCents)
                        else -> OsRepository.formatCurrencyCents(signedCents, includeSign = true)
                    }
                    val amountColor = when (tx.txType) {
                        "INCOME" -> Color(0xFF10B981)
                        "EXPENSE" -> CrimsonAccent
                        else -> palette.primaryInk
                    }
                    val dateTimeLabel = remember(tx.occurredAtMillis) {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(tx.occurredAtMillis))
                    }
                    val methodLabel = PAYMENT_METHODS.find { it.first == tx.paymentMethod }?.second ?: tx.paymentMethod
                    val txSector = sectorById[tx.sectorId]
                    val resolvedIconKey = txIconOverrides[tx.id] ?: txSector?.iconKey
                    val sectorColor = remember(txSector?.colorHex) {
                        parseSafeHexColor(txSector?.colorHex.orEmpty(), CrimsonAccent)
                    }

                    TactileGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("money_tx_card_${tx.id}"),
                        cornerRadius = 22.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FinanceIconSquircleBadge(
                                iconKey = resolvedIconKey,
                                accentColor = sectorColor,
                                selected = false,
                                badgeSize = 44.dp,
                                iconSize = 20.dp,
                                cornerRadius = 14.dp,
                                modifier = Modifier.testTag("money_tx_icon_${tx.id}")
                            )

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                                        Text(
                                            text = tx.sectorName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accentSoftInk,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                                        Text(
                                            text = methodLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tx.notes.ifBlank { tx.txType.lowercase().replaceFirstChar { it.uppercase() } },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = dateTimeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryInk
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = amountText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                                IconButton(
                                    onClick = {
                                        pendingDeleteTitle = "Delete Transaction?"
                                        pendingDeleteMessage = "Remove $amountText (${tx.sectorName}) from ledger?"
                                        pendingDeleteAction = { onDeleteTransaction(tx.id) }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete transaction")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSectorDialog) {
        CreateSectorDialog(
            recentIconKeys = localRecentIcons,
            fallbackSectorKeys = fallbackSectorKeys,
            onRecordRecentIcon = recordIcon,
            onDismiss = { showCreateSectorDialog = false },
            onCreate = { name, iconKey, colorHex, affinity ->
                recordIcon(iconKey)
                onCreateSector(name, iconKey, colorHex, affinity)
                showCreateSectorDialog = false
            }
        )
    }

    if (showLogEntryDialog && sectors.isNotEmpty()) {
        LogTransactionDialog(
            sectors = sectors,
            recentIconKeys = localRecentIcons,
            fallbackSectorKeys = fallbackSectorKeys,
            onRecordRecentIcon = recordIcon,
            onDismiss = { showLogEntryDialog = false },
            onSave = { txType, cents, sector, method, notes, occurredMillis, iconKey ->
                recordIcon(iconKey)
                onLogTransaction(txType, cents, sector, method, notes, occurredMillis, iconKey)
                showLogEntryDialog = false
            }
        )
    }

    if (showCustomRangeDialog) {
        CustomDateRangeDialog(
            onDismiss = { showCustomRangeDialog = false },
            onApply = { startMs, endMs ->
                customStartMillis = startMs
                customEndMillis = endMs
                showCustomRangeDialog = false
            }
        )
    }

    pendingDeleteAction?.let { action ->
        LiquidDeleteConfirmationDialog(
            title = pendingDeleteTitle,
            message = pendingDeleteMessage,
            onConfirm = {
                action()
                pendingDeleteAction = null
            },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}

@Composable
private fun CreateSectorDialog(
    recentIconKeys: List<String>,
    fallbackSectorKeys: List<String>,
    onRecordRecentIcon: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, iconKey: String, colorHex: String, typeAffinity: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(SECTOR_COLOR_SWATCHES.first()) }
    var iconKey by remember { mutableStateOf("wallet") }
    var typeAffinity by remember { mutableStateOf("EXPENSE") }
    var showIconPicker by remember { mutableStateOf(false) }

    val selectedColor = remember(colorHex) { parseSafeHexColor(colorHex, CrimsonAccent) }
    val selectedIconSpec = remember(iconKey) { FinanceIconCatalog.resolve(iconKey) }
    val quickRecentSpecs = remember(recentIconKeys, fallbackSectorKeys) {
        FinanceIconCatalog.resolveRecentSpecs(recentIconKeys, fallbackSectorKeys, limit = 5)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 480.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(28.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.dp, palette.specularBorderTop),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Create Custom Sector", style = MaterialTheme.typography.titleLarge, color = palette.primaryInk, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Sector Name (e.g. Hardware, Coffee)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.cardSurfaceTop,
                        unfocusedContainerColor = palette.cardSurfaceTop,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.inputBorder,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sector_name_input")
                )

                Text("Type Affinity", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("EXPENSE", "INCOME", "BOTH").forEach { aff ->
                        val active = typeAffinity == aff
                        Surface(
                            onClick = { typeAffinity = aff },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(aff, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk)
                            }
                        }
                    }
                }

                Text("Color Badge", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SECTOR_COLOR_SWATCHES.forEach { hex ->
                        val c = parseSafeHexColor(hex, CrimsonAccent)
                        val selected = colorHex == hex
                        Surface(
                            onClick = { colorHex = hex },
                            shape = CircleShape,
                            color = c,
                            border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) palette.primaryInk else Color.White),
                            modifier = Modifier.size(30.dp)
                        ) {}
                    }
                }

                Text("Sector Icon", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                IconSelectorPreviewCard(
                    selectedIconKey = iconKey,
                    selectedLabel = selectedIconSpec.label,
                    selectedCategoryTitle = selectedIconSpec.category.fullTitle,
                    accentColor = selectedColor,
                    quickRecentSpecs = quickRecentSpecs,
                    openPickerTestTag = "sector_open_icon_picker_button",
                    onQuickSelect = { key ->
                        iconKey = key
                        onRecordRecentIcon(key)
                    },
                    onOpenPicker = { showIconPicker = true }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
                    Spacer(modifier = Modifier.size(8.dp))
                    TactilePrimaryPillButton(
                        text = "Create Sector",
                        enabled = name.isNotBlank(),
                        onClick = { onCreate(name.trim(), iconKey, colorHex, typeAffinity) },
                        modifier = Modifier.testTag("sector_confirm_create_button")
                    )
                }
            }
        }
    }

    if (showIconPicker) {
        FinanceIconPickerDialog(
            selectedIconKey = iconKey,
            recentIconKeys = recentIconKeys,
            fallbackSectorKeys = fallbackSectorKeys,
            accentColor = selectedColor,
            onSelectIcon = { chosen ->
                iconKey = chosen
                onRecordRecentIcon(chosen)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
}

@Composable
private fun LogTransactionDialog(
    sectors: List<FinanceSectorEntity>,
    recentIconKeys: List<String>,
    fallbackSectorKeys: List<String>,
    onRecordRecentIcon: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        txType: String,
        amountCents: Long,
        sector: FinanceSectorEntity,
        paymentMethod: String,
        notes: String,
        occurredAtMillis: Long,
        iconKey: String
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    var txType by remember { mutableStateOf("EXPENSE") }
    var amountStr by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf(sectors.first()) }
    var selectedIconKey by remember { mutableStateOf(FinanceIconCatalog.normalizeKey(sectors.first().iconKey)) }
    var iconManuallyPicked by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("BANK") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(selectedSector.id) {
        if (!iconManuallyPicked) {
            selectedIconKey = FinanceIconCatalog.normalizeKey(selectedSector.iconKey)
        }
    }

    val initialCal = remember { Calendar.getInstance() }
    var dayOffset by remember { mutableIntStateOf(0) }
    var chosenHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var chosenMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val parsedCents = remember(amountStr) { parseDollarsToExactCents(amountStr) }
    val selectedSectorColor = remember(selectedSector.colorHex) { parseSafeHexColor(selectedSector.colorHex, CrimsonAccent) }
    val selectedIconSpec = remember(selectedIconKey) { FinanceIconCatalog.resolve(selectedIconKey) }
    val quickRecentSpecs = remember(recentIconKeys, fallbackSectorKeys) {
        FinanceIconCatalog.resolveRecentSpecs(recentIconKeys, fallbackSectorKeys, limit = 5)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.cardSurfaceTop,
        unfocusedContainerColor = palette.cardSurfaceTop,
        focusedBorderColor = CrimsonAccent,
        unfocusedBorderColor = palette.inputBorder,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.dp, palette.specularBorderTop),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Log Ledger Entry", style = MaterialTheme.typography.titleLarge, color = palette.primaryInk, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("EXPENSE", "INCOME", "TRANSFER").forEach { t ->
                        val active = txType == t
                        Surface(
                            onClick = { txType = t },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tx_type_${t.lowercase()}")
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(t, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                Text("Sector", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sectors.forEach { sec ->
                        val active = sec.id == selectedSector.id
                        val secSpec = remember(sec.iconKey) { FinanceIconCatalog.resolve(sec.iconKey) }
                        Surface(
                            onClick = { selectedSector = sec },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) palette.primaryButtonBg else palette.recessedTrayBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = secSpec.vector,
                                    contentDescription = null,
                                    tint = if (active) palette.primaryButtonContent else palette.secondaryInk,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = sec.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) palette.primaryButtonContent else palette.primaryInk
                                )
                            }
                        }
                    }
                }

                Text("Entry Icon", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                IconSelectorPreviewCard(
                    selectedIconKey = selectedIconKey,
                    selectedLabel = selectedIconSpec.label,
                    selectedCategoryTitle = selectedIconSpec.category.fullTitle,
                    accentColor = selectedSectorColor,
                    quickRecentSpecs = quickRecentSpecs,
                    openPickerTestTag = "tx_open_icon_picker_button",
                    onQuickSelect = { key ->
                        selectedIconKey = key
                        iconManuallyPicked = true
                        onRecordRecentIcon(key)
                    },
                    onOpenPicker = { showIconPicker = true }
                )

                Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PAYMENT_METHODS.forEach { (code, label) ->
                        val active = paymentMethod == code
                        Surface(
                            onClick = { paymentMethod = code },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0 to "Today", -1 to "Yesterday", -2 to "-2 Days").forEach { (offset, label) ->
                        val active = dayOffset == offset
                        Surface(
                            onClick = { dayOffset = offset },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) palette.primaryButtonBg else palette.recessedTrayBg
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(9999.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("tx_pick_time_button")
                    ) {
                        Text(
                            text = formatTime12h(chosenHour, chosenMinute),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSoftInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Counterparty") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_notes_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
                    Spacer(modifier = Modifier.size(8.dp))
                    TactilePrimaryPillButton(
                        text = "Save Entry",
                        enabled = parsedCents > 0L,
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, dayOffset)
                                set(Calendar.HOUR_OF_DAY, chosenHour)
                                set(Calendar.MINUTE, chosenMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onSave(txType, parsedCents, selectedSector, paymentMethod, notes.trim(), cal.timeInMillis, selectedIconKey)
                        },
                        modifier = Modifier.testTag("tx_confirm_save_button")
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        LiquidTimePickerDialog(
            initialHour = chosenHour,
            initialMinute = chosenMinute,
            title = "Exact Transaction Time",
            onConfirm = { h, m ->
                chosenHour = h
                chosenMinute = m
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showIconPicker) {
        FinanceIconPickerDialog(
            selectedIconKey = selectedIconKey,
            recentIconKeys = recentIconKeys,
            fallbackSectorKeys = fallbackSectorKeys,
            accentColor = selectedSectorColor,
            onSelectIcon = { chosen ->
                selectedIconKey = chosen
                iconManuallyPicked = true
                onRecordRecentIcon(chosen)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
}

@Composable
private fun IconSelectorPreviewCard(
    selectedIconKey: String,
    selectedLabel: String,
    selectedCategoryTitle: String,
    accentColor: Color,
    quickRecentSpecs: List<com.example.ui.components.FinanceIconSpec>,
    openPickerTestTag: String,
    onQuickSelect: (String) -> Unit,
    onOpenPicker: () -> Unit
) {
    val palette = LiquidTheme.palette
    val normalizedSelected = FinanceIconCatalog.normalizeKey(selectedIconKey)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.recessedTrayBg)
            .border(1.dp, palette.specularRimOutline, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenPicker,
                color = Color.Transparent,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                FinanceIconSquircleBadge(
                    iconKey = selectedIconKey,
                    accentColor = accentColor,
                    selected = true,
                    badgeSize = 46.dp,
                    iconSize = 22.dp,
                    cornerRadius = 14.dp
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedCategoryTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onOpenPicker,
                shape = RoundedCornerShape(9999.dp),
                color = palette.frostedPillBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(openPickerTestTag)
            ) {
                Text(
                    text = "Browse Icons",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (quickRecentSpecs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent:",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryInk
                )
                quickRecentSpecs.forEach { spec ->
                    val isSelected = normalizedSelected == spec.key
                    Surface(
                        onClick = { onQuickSelect(spec.key) },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        FinanceIconSquircleBadge(
                            iconKey = spec.key,
                            accentColor = accentColor,
                            selected = isSelected,
                            badgeSize = 38.dp,
                            iconSize = 18.dp,
                            cornerRadius = 14.dp,
                            contentDescription = spec.label
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onApply: (startMillis: Long, endMillis: Long) -> Unit
) {
    val palette = LiquidTheme.palette
    var daysBack by remember { mutableIntStateOf(14) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Custom Date Window", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select lookback range up to now:", color = palette.secondaryInk, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(3 to "Last 3d", 14 to "Last 14d", 30 to "Last 30d", 90 to "Last 90d").forEach { (days, label) ->
                        val active = daysBack == days
                        Surface(
                            onClick = { daysBack = days },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Apply Range",
                onClick = {
                    val now = System.currentTimeMillis()
                    val start = now - daysBack * 86_400_000L
                    onApply(start, now)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

internal fun parseDollarsToExactCents(raw: String): Long {
    val cleaned = raw.trim().replace(",", "").replace("$", "")
    if (cleaned.isBlank()) return 0L
    return try {
        val bd = BigDecimal(cleaned)
        if (bd <= BigDecimal.ZERO) 0L
        else bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (_: Exception) {
        0L
    }
}

private fun parseSafeHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
