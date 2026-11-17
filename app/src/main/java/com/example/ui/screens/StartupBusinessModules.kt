package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamWorkspaceEntity
import com.example.data.repository.BusinessCrmDeal
import com.example.data.repository.BusinessOkrItem
import com.example.data.repository.BusinessSopDoc
import com.example.data.repository.FinanceLedgerSummary
import com.example.data.repository.OsRepository
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

@Composable
fun LiveFinancialSnapshotCard(
    ledgerSummary: FinanceLedgerSummary,
    crmDeals: List<BusinessCrmDeal>,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val pipelineCents = remember(crmDeals) { crmDeals.sumOf { it.dealValueCents } }

    FeaturedCrimsonBronzeCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("startup_financial_snapshot_card"),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Live Money Ledger & Pipeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                Surface(shape = RoundedCornerShape(9999.dp), color = palette.frostedPillBg) {
                    Text(
                        text = "${ledgerSummary.savingsRatePercent}% Margin",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAccent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Net Ledger", OsRepository.formatCurrencyCents(ledgerSummary.netBalanceCents), Modifier.weight(1f))
                MetricTile("Income", OsRepository.formatCurrencyCents(ledgerSummary.totalIncomeCents), Modifier.weight(1f))
                MetricTile("CRM Pipeline", OsRepository.formatCurrencyCents(pipelineCents), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LiquidTheme.palette
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = palette.cardSurfaceTop,
        border = BorderStroke(1.dp, palette.specularRimOutline)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk, maxLines = 1)
        }
    }
}

fun LazyListScope.businessCrmSection(
    deals: List<BusinessCrmDeal>,
    onAddDealClick: () -> Unit,
    onAdvanceDealStage: (BusinessCrmDeal) -> Unit,
    onDeleteDeal: (BusinessCrmDeal) -> Unit
) {
    item(key = "crm_header_action", contentType = "action") {
        TactilePrimaryPillButton(
            text = "+ Add Client / Deal",
            iconType = MinimalIconType.PLUS,
            onClick = onAddDealClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("business_add_crm_button")
        )
    }
    if (deals.isEmpty()) {
        item(key = "crm_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No clients or deals tracked yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                    Text("Track prospects through Lead -> Negotiation -> Active Client -> Delivered.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                }
            }
        }
    } else {
        items(items = deals, key = { it.id }, contentType = { "crm_deal" }) { deal ->
            val palette = LiquidTheme.palette
            val stageIdx = BusinessCrmDeal.ALL_STAGES.indexOf(deal.stage)
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deal.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            if (deal.contactEmail.isNotBlank()) {
                                Text(deal.contactEmail, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                                Text(
                                    text = BusinessCrmDeal.stageLabel(deal.stage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonAccent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteDeal(deal) }, modifier = Modifier.size(32.dp)) {
                                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete CRM deal")
                            }
                        }
                    }
                    Text(
                        text = "Deal Value: ${OsRepository.formatCurrencyCents(deal.dealValueCents)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    if (deal.notes.isNotBlank()) {
                        Text(deal.notes, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                    }
                    if (stageIdx in 0 until BusinessCrmDeal.ALL_STAGES.lastIndex) {
                        val nextStage = BusinessCrmDeal.ALL_STAGES[stageIdx + 1]
                        OutlinedButton(
                            onClick = { onAdvanceDealStage(deal) },
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Advance -> ${BusinessCrmDeal.stageLabel(nextStage)}", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.businessOkrsSection(
    okrs: List<BusinessOkrItem>,
    onAddOkrClick: () -> Unit,
    onIncrementOkr: (BusinessOkrItem) -> Unit,
    onDeleteOkr: (BusinessOkrItem) -> Unit
) {
    item(key = "okr_header_action", contentType = "action") {
        TactilePrimaryPillButton(
            text = "+ Add OKR / KPI Target",
            iconType = MinimalIconType.PLUS,
            onClick = onAddOkrClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("business_add_okr_button")
        )
    }
    if (okrs.isEmpty()) {
        item(key = "okr_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No OKRs or KPIs defined yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                    Text("Set quarterly objectives like MRR, signups, or retention with live progress bars.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                }
            }
        }
    } else {
        items(items = okrs, key = { it.id }, contentType = { "okr_item" }) { okr ->
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(okr.objective, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                            Text(okr.keyResult, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                        }
                        IconButton(onClick = { onDeleteOkr(okr) }, modifier = Modifier.size(32.dp)) {
                            MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete OKR")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${okr.currentValue.toInt()} / ${okr.targetValue.toInt()} ${okr.unitLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent
                        )
                        Surface(
                            onClick = { onIncrementOkr(okr) },
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text("+ Update Progress", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { okr.progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp)),
                        color = CrimsonAccent,
                        trackColor = palette.recessedTrayBg
                    )
                }
            }
        }
    }
}

fun LazyListScope.businessSopsSection(
    sops: List<BusinessSopDoc>,
    onAddSopClick: () -> Unit,
    onDeleteSop: (BusinessSopDoc) -> Unit
) {
    item(key = "sop_header_action", contentType = "action") {
        TactilePrimaryPillButton(
            text = "+ Add SOP / Template",
            iconType = MinimalIconType.PLUS,
            onClick = onAddSopClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("business_add_sop_button")
        )
    }
    if (sops.isEmpty()) {
        item(key = "sop_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No SOP templates yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                }
            }
        }
    } else {
        items(items = sops, key = { it.id }, contentType = { "sop_doc" }) { sop ->
            val context = LocalContext.current
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                                Text(sop.templateType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text(sop.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                        }
                        IconButton(onClick = { onDeleteSop(sop) }, modifier = Modifier.size(32.dp)) {
                            MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Delete SOP")
                        }
                    }
                    if (sop.content.isNotBlank()) {
                        Text(sop.content, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                    }
                    if (!sop.resourceUrl.isNullOrBlank()) {
                        Surface(
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sop.resourceUrl))) } },
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.frostedPillBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text("Open Drive / Template URL", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCrmDealDialog(
    onDismiss: () -> Unit,
    onSave: (BusinessCrmDeal) -> Unit
) {
    val palette = LiquidTheme.palette
    var clientName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var amountDollars by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(BusinessCrmDeal.STAGE_LEAD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add Client / Deal", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client / Account Name") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email (optional)") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = amountDollars, onValueChange = { amountDollars = it }, label = { Text("Deal Value (USD, e.g. 2500)") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Scope / Next Steps") }, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BusinessCrmDeal.ALL_STAGES.forEach { st ->
                        val active = stage == st
                        Surface(
                            onClick = { stage = st },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg
                        ) {
                            Text(BusinessCrmDeal.stageLabel(st), style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Save Deal",
                enabled = clientName.isNotBlank(),
                onClick = {
                    val cents = ((amountDollars.trim().toDoubleOrNull() ?: 0.0) * 100.0).toLong().coerceAtLeast(0L)
                    onSave(BusinessCrmDeal(clientName = clientName.trim(), contactEmail = email.trim(), dealValueCents = cents, stage = stage, notes = notes.trim()))
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}

@Composable
fun AddBusinessOkrDialog(
    onDismiss: () -> Unit,
    onSave: (BusinessOkrItem) -> Unit
) {
    val palette = LiquidTheme.palette
    var objective by remember { mutableStateOf("") }
    var keyResult by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("100") }
    var unitLabel by remember { mutableStateOf("%") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add OKR / KPI", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Objective (e.g. Grow Paying Users)") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = keyResult, onValueChange = { keyResult = it }, label = { Text("Key Result (e.g. Reach 50 active teams)") }, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = targetText, onValueChange = { targetText = it }, label = { Text("Target") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unitLabel, onValueChange = { unitLabel = it }, label = { Text("Unit") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Save OKR",
                enabled = objective.isNotBlank() && keyResult.isNotBlank(),
                onClick = {
                    onSave(BusinessOkrItem(objective = objective.trim(), keyResult = keyResult.trim(), currentValue = 0.0, targetValue = (targetText.toDoubleOrNull() ?: 100.0).coerceAtLeast(1.0), unitLabel = unitLabel.ifBlank { "%" }))
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}

@Composable
fun AddBusinessSopDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onSave: (BusinessSopDoc) -> Unit
) {
    val palette = LiquidTheme.palette
    var templateType by remember { mutableStateOf(BusinessSopDoc.TYPE_SOP) }
    val (initTitle, initBody) = remember(projectName) { BusinessSopDoc.defaultTemplateContent(BusinessSopDoc.TYPE_SOP, projectName) }
    var title by remember { mutableStateOf(initTitle) }
    var content by remember { mutableStateOf(initBody) }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Add SOP / Playbook", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(BusinessSopDoc.TYPE_SOP, BusinessSopDoc.TYPE_CANVAS, BusinessSopDoc.TYPE_PITCH_DECK, BusinessSopDoc.TYPE_PRICING).forEach { tpl ->
                        val active = templateType == tpl
                        Surface(
                            onClick = {
                                templateType = tpl
                                val (t, c) = BusinessSopDoc.defaultTemplateContent(tpl, projectName)
                                title = t
                                content = c
                            },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg
                        ) {
                            Text(tpl, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.secondaryInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Template Title") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("SOP Steps / Framework") }, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Google Drive / Docs Link (optional)") }, singleLine = true, colors = teamFieldColors(), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Save SOP",
                enabled = title.isNotBlank(),
                onClick = { onSave(BusinessSopDoc(title = title.trim(), templateType = templateType, content = content.trim(), resourceUrl = url.trim().takeIf { it.isNotBlank() })) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) } }
    )
}

@Composable
fun ShareWorkflowToTeamDialog(
    projectName: String,
    teams: List<TeamWorkspaceEntity>,
    activeTeamMembers: List<TeamMemberEntity>,
    initialSelectedTeamId: String?,
    onSelectTeamForMembers: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirmShare: (teamId: String, taggedMembers: List<TeamMemberEntity>, includeTasks: Boolean, includeSop: Boolean, headline: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var selectedTeamId by remember(teams, initialSelectedTeamId) {
        mutableStateOf(initialSelectedTeamId?.takeIf { id -> teams.any { it.id == id } } ?: teams.firstOrNull()?.id.orEmpty())
    }
    var includeTasks by remember { mutableStateOf(true) }
    var includeSop by remember { mutableStateOf(true) }
    var headline by remember { mutableStateOf("$projectName Sprint & Playbook Sync") }
    val taggedUids = remember { mutableStateListOf<String>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("share_workflow_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.2.dp, palette.specularBorderTop),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Workflow with Team",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primaryInk,
                    fontWeight = FontWeight.Bold
                )
                if (teams.isEmpty()) {
                    Text(
                        text = "Join or create a team in Team & Group OS first to sync business workflows live.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonAccent
                    )
                } else {
                    Text("Select Target Team", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        teams.forEach { team ->
                            val selected = selectedTeamId == team.id
                            Surface(
                                onClick = {
                                    selectedTeamId = team.id
                                    taggedUids.clear()
                                    onSelectTeamForMembers(team.id)
                                },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) CrimsonAccent else palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.testTag("share_pick_team_${team.id}")
                            ) {
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else palette.primaryInk,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Team Notice Headline") },
                        singleLine = true,
                        colors = teamFieldColors(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeTasks, onCheckedChange = { includeTasks = it }, colors = CheckboxDefaults.colors(checkedColor = CrimsonAccent))
                        Text("Push incomplete roadmap milestones as team tasks", style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeSop, onCheckedChange = { includeSop = it }, colors = CheckboxDefaults.colors(checkedColor = CrimsonAccent))
                        Text("Publish pinned Notion Brief (CRM + OKRs + SOPs)", style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
                    }
                    if (activeTeamMembers.isNotEmpty()) {
                        Text("Tag @Members", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeTeamMembers.forEach { m ->
                                val tagged = m.uid in taggedUids
                                Surface(
                                    onClick = { if (tagged) taggedUids.remove(m.uid) else taggedUids.add(m.uid) },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (tagged) palette.accentSoftBg else palette.cardSurfaceTop,
                                    border = BorderStroke(1.dp, if (tagged) CrimsonAccent else palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = "@${m.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (tagged) CrimsonAccent else palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.secondaryInk)
                    }
                    TactilePrimaryPillButton(
                        text = "Sync Live Now",
                        enabled = teams.isNotEmpty() && selectedTeamId.isNotBlank() && (includeTasks || includeSop),
                        onClick = {
                            val tagged = activeTeamMembers.filter { it.uid in taggedUids }
                            onConfirmShare(selectedTeamId, tagged, includeTasks, includeSop, headline.trim())
                        },
                        modifier = Modifier.testTag("confirm_share_workflow_button")
                    )
                }
            }
        }
    }
}
