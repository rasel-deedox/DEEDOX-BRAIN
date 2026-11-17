package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.ItemEntity
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.TerracottaBottom
import com.example.ui.theme.TerracottaTop
import com.example.ui.viewmodel.LibrarySnapshot

@Composable
fun VaultHeroCardStack(
    snapshot: LibrarySnapshot,
    hasPin: Boolean,
    isVaultLocked: Boolean,
    onPinVaultClick: () -> Unit,
    onVaultDetailsClick: () -> Unit,
    onToggleLockClick: () -> Unit,
    onOpenWorkspaceClick: () -> Unit = {},
    onOpenMonkModeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var stackExpanded by remember { mutableStateOf(false) }
    val spread by animateFloatAsState(
        targetValue = if (stackExpanded) 1f else 0f,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.72f),
        label = "vaultStackSpread"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Manage Your\nCollections",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            color = palette.primaryInk
        )

        // Two minimal Liquid Glass squircle quick-access buttons (Notes & Habits + Monk Mode)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenWorkspaceClick,
                shape = RoundedCornerShape(18.dp),
                color = palette.frostedPillBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("vault_open_workspace_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        MinimalIcon(
                            type = MinimalIconType.NOTEBOOK,
                            active = true,
                            tint = CrimsonAccent,
                            size = 16.dp,
                            contentDescription = "Open Notes and Habits"
                        )
                    }
                    Text(
                        text = "Notes & Habits",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryInk
                    )
                }
            }

            Surface(
                onClick = onOpenMonkModeClick,
                shape = RoundedCornerShape(18.dp),
                color = palette.frostedPillBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("vault_open_monk_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        MinimalIcon(
                            type = MinimalIconType.TIMER,
                            active = true,
                            tint = CrimsonAccent,
                            size = 16.dp,
                            contentDescription = "Open Monk Mode"
                        )
                    }
                    Text(
                        text = "Monk Mode",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryInk
                    )
                }
            }
        }

        // 3D Tilted Frosted Glass Card Stack
        val backShape = remember { RoundedCornerShape(26.dp) }
        val frontShape = remember { RoundedCornerShape(28.dp) }
        val backBrush = remember(palette.bronzeCardTop, palette.bronzeCardBottom) {
            Brush.linearGradient(listOf(palette.bronzeCardTop, palette.bronzeCardBottom))
        }
        val frontBrush = remember(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom) {
            Brush.linearGradient(
                colors = listOf(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { stackExpanded = !stackExpanded }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Back left card
            Box(
                modifier = Modifier
                    .width(236.dp)
                    .height(168.dp)
                    .graphicsLayer {
                        rotationZ = -8f - (spread * 5f)
                        translationX = (-38f - spread * 24f)
                        translationY = 12f
                    }
                    .clip(backShape)
                    .background(palette.recessedTrayBg)
                    .border(1.dp, palette.specularRimOutline, backShape)
            )

            // Back right card
            Box(
                modifier = Modifier
                    .width(236.dp)
                    .height(168.dp)
                    .graphicsLayer {
                        rotationZ = 8f + (spread * 5f)
                        translationX = (38f + spread * 24f)
                        translationY = 12f
                    }
                    .clip(backShape)
                    .background(backBrush)
                    .border(1.dp, palette.bronzeCardBorder, backShape)
            )

            // Front centerpiece card with pearl/bronze specular sheen
            Box(
                modifier = Modifier
                    .width(268.dp)
                    .height(182.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = frontShape,
                        ambientColor = if (palette.isDark) Color(0x88000000) else Color(0x202D261E)
                    )
                    .clip(frontShape)
                    .background(frontBrush)
                    .border(1.2.dp, palette.specularBorderTop, frontShape)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.frostedPillBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = if (hasPin) (if (isVaultLocked) "Locked" else "PIN Armed") else "Open",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasPin) palette.accentSoftInk else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${snapshot.folders.size} Folders • ${snapshot.collections.size} Collections",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "${snapshot.items.size} resources & ${snapshot.tags.size} tags indexed offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•••• ${snapshot.items.size.toString().padStart(4, '0')}",
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 2.sp,
                            color = palette.secondaryInk
                        )
                        Text(
                            text = "VAULT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = palette.primaryInk
                        )
                    }
                }
            }
        }

        // 3 Circular Quick-Action Buttons: Pin Vault | Vault Details | Lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultCircularAction(
                label = if (hasPin) "Change PIN" else "Pin Vault",
                iconType = MinimalIconType.GRID,
                highlighted = hasPin,
                onClick = onPinVaultClick,
                modifier = Modifier.testTag("vault_pin_button")
            )
            VaultCircularAction(
                label = "Vault Details",
                iconType = MinimalIconType.LAYERS,
                highlighted = false,
                onClick = onVaultDetailsClick,
                modifier = Modifier.testTag("vault_details_button")
            )
            VaultCircularAction(
                label = if (isVaultLocked) "Unlock" else "Lock",
                iconType = MinimalIconType.LOCK,
                highlighted = isVaultLocked,
                onClick = onToggleLockClick,
                modifier = Modifier.testTag("vault_lock_button")
            )
        }
    }
}

@Composable
private fun VaultCircularAction(
    label: String,
    iconType: MinimalIconType,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (highlighted) palette.primaryButtonBg else palette.frostedPillBg,
            border = BorderStroke(1.dp, palette.specularRimOutline),
            shadowElevation = 8.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                MinimalIcon(
                    type = iconType,
                    active = highlighted,
                    tint = if (highlighted) palette.primaryButtonContent else palette.primaryInk,
                    cutoutColor = if (highlighted) palette.primaryButtonBg else palette.frostedPillBg,
                    size = 20.dp
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = palette.secondaryInk
        )
    }
}

/**
 * 3D Frosted Glass Folder Pocket Card matching Tomography & Radiography cards:
 * Bold title, date subtitle, top-right arrow badge, 3 fanned miniature preview cards
 * peeking above a sculpted frosted glass pocket front with "items: X" counter.
 */
@Composable
fun FolderPocketCard(
    folder: FolderEntity,
    previewItems: List<ItemEntity>,
    itemCount: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val subtitleText = remember(folder.isInbox, folder.updatedAt) {
        if (folder.isInbox) "Inbox • Auto" else formatRelativeDate(folder.updatedAt)
    }
    val pocketShape = remember { RoundedCornerShape(18.dp) }
    val pocketBrush = remember(palette.folderPocketFrontTop, palette.folderPocketFrontBottom) {
        Brush.verticalGradient(listOf(palette.folderPocketFrontTop, palette.folderPocketFrontBottom))
    }

    TactileGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("folder_card_${folder.id}"),
        cornerRadius = 26.dp,
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title + Subtitle + Top-Right Arrow Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.recessedTrayBg)
                        .border(1.dp, palette.specularRimOutline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = MinimalIconType.ARROW_UP_RIGHT,
                        tint = palette.primaryInk,
                        size = 14.dp
                    )
                }
            }

            // 3D Fanned Miniature Cards + Frosted Pocket Front
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Left fanned mini card (-8deg)
                MiniPreviewCard(
                    title = previewItems.getOrNull(0)?.title ?: "Archive",
                    accent = CrimsonAccent,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = (-22).dp, y = 6.dp)
                        .graphicsLayer { rotationZ = -8f }
                )
                // Right fanned mini card (+8deg)
                MiniPreviewCard(
                    title = previewItems.getOrNull(2)?.title ?: "Refs",
                    accent = TerracottaTop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 22.dp, y = 6.dp)
                        .graphicsLayer { rotationZ = 8f }
                )
                // Center miniature card (0deg)
                MiniPreviewCard(
                    title = previewItems.getOrNull(1)?.title ?: folder.name,
                    accent = palette.primaryInk,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp)
                )

                // Sculpted Frosted Glass Pocket Front
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(pocketShape)
                        .background(pocketBrush)
                        .border(1.dp, palette.specularBorderTop, pocketShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "items: $itemCount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )

                        if (!folder.isInbox) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(palette.recessedTrayBg)
                                        .clickable(onClick = onRename),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MinimalIcon(
                                        type = MinimalIconType.EDIT,
                                        tint = palette.secondaryInk,
                                        size = 13.dp,
                                        contentDescription = "Rename ${folder.name}"
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(palette.accentSoftBg)
                                        .clickable(onClick = onDelete),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MinimalIcon(
                                        type = MinimalIconType.TRASH,
                                        tint = CrimsonAccent,
                                        size = 13.dp,
                                        contentDescription = "Delete ${folder.name}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPreviewCard(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val cardShape = remember { RoundedCornerShape(12.dp) }
    Box(
        modifier = modifier
            .width(74.dp)
            .height(54.dp)
            .clip(cardShape)
            .background(palette.secondaryPillBg)
            .border(0.9.dp, palette.specularRimOutline, cardShape)
            .padding(7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent)
            )
            Text(
                text = title,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * ZIKO-style 4-Digit Frosted Glass Keypad PIN Drawer & Unlock Surface.
 */
@Composable
fun VaultKeypadSurface(
    headline: String,
    subtitle: String,
    enteredPin: String,
    errorText: String?,
    confirmLabel: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = palette.primaryInk
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryInk,
            textAlign = TextAlign.Center
        )

        // 4 PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { idx ->
                val filled = idx < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) CrimsonAccent else palette.recessedTrayBg)
                        .border(1.dp, palette.specularRimOutline, CircleShape)
                )
            }
        }

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = CrimsonAccent
            )
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { digit ->
                        KeypadDigitButton(
                            label = digit,
                            onClick = { if (enteredPin.length < 4) onDigitClick(digit) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("keypad_digit_$digit")
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onClearClick,
                    shape = RoundedCornerShape(20.dp),
                    color = palette.recessedTrayBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.secondaryInk
                        )
                    }
                }
                KeypadDigitButton(
                    label = "0",
                    onClick = { if (enteredPin.length < 4) onDigitClick("0") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_digit_0")
                )
                Surface(
                    onClick = onBackspaceClick,
                    shape = RoundedCornerShape(20.dp),
                    color = palette.recessedTrayBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Backspace",
                            tint = palette.primaryInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSecondaryClick != null && secondaryLabel != null) {
                TextButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(secondaryLabel, color = palette.secondaryInk)
                }
            }
            TactilePrimaryPillButton(
                text = confirmLabel,
                enabled = enteredPin.length == 4,
                onClick = onConfirmClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("keypad_confirm_button")
            )
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = 4.dp,
        modifier = modifier.height(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryInk
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPinDrawerModal(
    existingPin: String?,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onClearPin: () -> Unit
) {
    val palette = LiquidTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pinInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            VaultKeypadSurface(
                headline = if (existingPin.isNullOrBlank()) "Set 4-Digit Vault PIN" else "Update Vault PIN",
                subtitle = "Protect your Visual Vault collections on this device.",
                enteredPin = pinInput,
                errorText = null,
                confirmLabel = "Save PIN",
                onDigitClick = { pinInput += it },
                onBackspaceClick = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
                onClearClick = { pinInput = "" },
                onConfirmClick = {
                    if (pinInput.length == 4) {
                        onSavePin(pinInput)
                        onDismiss()
                    }
                },
                onSecondaryClick = if (!existingPin.isNullOrBlank()) {
                    {
                        onClearPin()
                        onDismiss()
                    }
                } else null,
                secondaryLabel = if (!existingPin.isNullOrBlank()) "Remove PIN" else null
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
