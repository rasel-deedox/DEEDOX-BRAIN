package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

/**
 * Tactile rounded squircle badge (14.dp corners) with warm ivory / crimson-bronze glass tinting
 * and optional active selection glow.
 */
@Composable
fun FinanceIconSquircleBadge(
    iconKey: String?,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    selected: Boolean = false,
    badgeSize: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 14.dp,
    contentDescription: String? = null
) {
    val palette = LiquidTheme.palette
    val spec = remember(iconKey) { FinanceIconCatalog.resolve(iconKey) }
    val resolvedAccent = accentColor ?: palette.accentCrimson

    val bgTop by animateColorAsState(
        targetValue = when {
            selected -> resolvedAccent.copy(alpha = if (palette.isDark) 0.28f else 0.16f)
            accentColor != null -> accentColor.copy(alpha = if (palette.isDark) 0.18f else 0.11f)
            else -> palette.bronzeCardTop
        },
        label = "squircleBgTop"
    )
    val bgBottom by animateColorAsState(
        targetValue = when {
            selected -> palette.accentSoftBg
            else -> palette.cardSurfaceTop
        },
        label = "squircleBgBottom"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) resolvedAccent else palette.specularRimOutline,
        label = "squircleBorder"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            selected -> if (palette.isDark) palette.accentSoftInk else resolvedAccent
            accentColor != null -> accentColor
            else -> palette.primaryInk
        },
        label = "squircleTint"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val glowMod = if (selected) {
        modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = resolvedAccent.copy(alpha = 0.35f),
            spotColor = resolvedAccent.copy(alpha = 0.45f)
        )
    } else {
        modifier
    }

    Box(
        modifier = glowMod
            .size(badgeSize)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = spec.vector,
            contentDescription = contentDescription ?: spec.label,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun FinanceIconPickerDialog(
    selectedIconKey: String,
    recentIconKeys: List<String>,
    fallbackSectorKeys: List<String> = emptyList(),
    accentColor: Color? = null,
    onSelectIcon: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LiquidTheme.palette
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FinanceIconCategory?>(null) }

    val normalizedSelected = remember(selectedIconKey) {
        FinanceIconCatalog.normalizeKey(selectedIconKey)
    }
    val recentSpecs = remember(recentIconKeys, fallbackSectorKeys) {
        FinanceIconCatalog.resolveRecentSpecs(
            recentKeys = recentIconKeys,
            fallbackSectorKeys = fallbackSectorKeys,
            limit = 5
        )
    }
    val filteredIcons = remember(searchQuery, selectedCategory) {
        FinanceIconCatalog.search(query = searchQuery, category = selectedCategory)
    }
    val groupedSections = remember(filteredIcons, searchQuery, selectedCategory) {
        if (selectedCategory != null || searchQuery.isNotBlank()) {
            emptyList()
        } else {
            FinanceIconCategory.entries.mapNotNull { cat ->
                val list = filteredIcons.filter { it.category == cat }
                if (list.isEmpty()) null else cat to list
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp)
                .testTag("finance_icon_picker_dialog"),
            shape = RoundedCornerShape(30.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.dp, palette.specularBorderTop),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Select Minimal Icon",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "${FinanceIconCatalog.allIcons.size} icons across 8 categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("icon_picker_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close icon picker",
                            tint = palette.secondaryInk
                        )
                    }
                }

                // Instant Keyword Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search coffee, car, rent, server, salary...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search icons",
                            tint = palette.secondaryInk
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("icon_picker_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = palette.secondaryInk
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.cardSurfaceTop,
                        unfocusedContainerColor = palette.cardSurfaceTop,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.inputBorder,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("icon_picker_search_input")
                )

                // Horizontal Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allSelected = selectedCategory == null
                    Surface(
                        onClick = { selectedCategory = null },
                        shape = RoundedCornerShape(9999.dp),
                        color = if (allSelected) palette.primaryButtonBg else palette.frostedPillBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("icon_picker_cat_all")
                    ) {
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (allSelected) palette.primaryButtonContent else palette.primaryInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    FinanceIconCategory.entries.forEach { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = if (active) null else cat },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.frostedPillBg,
                            border = BorderStroke(
                                1.dp,
                                if (active) CrimsonAccent else palette.specularRimOutline
                            ),
                            modifier = Modifier.testTag("icon_picker_cat_${cat.id}")
                        ) {
                            Text(
                                text = cat.pillLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Scrollable body: Recently Used strip + Categorized or Filtered grid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 240.dp, max = 440.dp)
                        .testTag("icon_picker_grid_list"),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recentSpecs.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null) {
                        item(key = "recent_strip", contentType = "recent") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.recessedTrayBg)
                                    .border(1.dp, palette.specularRimOutline, RoundedCornerShape(20.dp))
                                    .padding(12.dp)
                                    .testTag("icon_picker_recent_strip"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Recently Used",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentSpecs.forEach { spec ->
                                        val isSelected = normalizedSelected == spec.key
                                        IconTileButton(
                                            spec = spec,
                                            selected = isSelected,
                                            accentColor = accentColor,
                                            onClick = { onSelectIcon(spec.key) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("icon_picker_recent_${spec.key}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredIcons.isEmpty()) {
                        item(key = "empty_search", contentType = "empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No matching icons for \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "Try keywords like coffee, taxi, rent, server, gym, or salary.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(onClick = {
                                    searchQuery = ""
                                    selectedCategory = null
                                }) {
                                    Text("Reset Filters", color = CrimsonAccent)
                                }
                            }
                        }
                    } else if (groupedSections.isNotEmpty()) {
                        groupedSections.forEach { (category, specs) ->
                            item(key = "header_${category.id}", contentType = "cat_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.fullTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk
                                    )
                                    Text(
                                        text = "${specs.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryInk
                                    )
                                }
                            }
                            val chunked = specs.chunked(4)
                            items(
                                items = chunked,
                                key = { row -> "row_${category.id}_${row.first().key}" },
                                contentType = { "icon_row" }
                            ) { rowSpecs ->
                                IconGridRow(
                                    rowSpecs = rowSpecs,
                                    selectedKey = normalizedSelected,
                                    accentColor = accentColor,
                                    onSelectIcon = onSelectIcon
                                )
                            }
                        }
                    } else {
                        val chunked = filteredIcons.chunked(4)
                        items(
                            items = chunked,
                            key = { row -> "filtered_row_${row.first().key}" },
                            contentType = { "icon_row" }
                        ) { rowSpecs ->
                            IconGridRow(
                                rowSpecs = rowSpecs,
                                selectedKey = normalizedSelected,
                                accentColor = accentColor,
                                onSelectIcon = onSelectIcon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGridRow(
    rowSpecs: List<FinanceIconSpec>,
    selectedKey: String,
    accentColor: Color?,
    onSelectIcon: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rowSpecs.forEach { spec ->
            IconTileButton(
                spec = spec,
                selected = selectedKey == spec.key,
                accentColor = accentColor,
                onClick = { onSelectIcon(spec.key) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("icon_picker_item_${spec.key}")
            )
        }
        repeat(4 - rowSpecs.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun IconTileButton(
    spec: FinanceIconSpec,
    selected: Boolean,
    accentColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val resolvedAccent = accentColor ?: palette.accentCrimson
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) palette.accentSoftBg else palette.cardSurfaceTop.copy(alpha = 0.72f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) resolvedAccent else palette.specularRimOutline
        ),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FinanceIconSquircleBadge(
                iconKey = spec.key,
                accentColor = accentColor,
                selected = selected,
                badgeSize = 42.dp,
                iconSize = 20.dp,
                cornerRadius = 14.dp,
                contentDescription = spec.label
            )
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.primaryInk else palette.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
