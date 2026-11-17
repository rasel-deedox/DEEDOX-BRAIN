package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CloudDeviceSession
import com.example.data.cloud.CloudDeviceTransfer
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.LibraryReview
import com.example.data.local.SavedAccountRecord
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.SoftCreamInk
import com.example.ui.theme.SoftCreamWhite
import com.example.ui.theme.TelemetryOptimalDot
import java.util.Locale

/**
 * Sleek Liquid Glass Theme Pill Switcher ([ Light | Dim | Dark ]) synced across devices.
 */
@Composable
fun LiquidThemePillSwitcher(
    selectedMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val modes = AppThemeMode.entries
    val selectedIdx = modes.indexOf(selectedMode).coerceAtLeast(0)
    val pillShape = remember { RoundedCornerShape(9999.dp) }

    BoxWithConstraints(
        modifier = modifier
            .height(38.dp)
            .clip(pillShape)
            .background(palette.frostedPillBg)
            .border(1.dp, palette.specularRimOutline, pillShape)
            .padding(3.dp)
            .testTag("theme_pill_switcher")
    ) {
        val slotWidth = maxWidth / modes.size
        val animatedSlot by animateFloatAsState(
            targetValue = selectedIdx.toFloat(),
            animationSpec = spring(stiffness = 440f, dampingRatio = 0.78f, visibilityThreshold = 0.002f),
            label = "themePillSlot"
        )

        Box(
            modifier = Modifier
                .width(slotWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = animatedSlot * slotWidth.toPx()
                }
                .clip(pillShape)
                .background(palette.dockTileActiveBg)
        )

        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            modes.forEach { mode ->
                val selected = mode == selectedMode
                val fg by animateColorAsState(
                    targetValue = if (selected) palette.dockTileActiveContent else palette.secondaryInk,
                    label = "themeFg_${mode.code}"
                )
                val icon = when (mode) {
                    AppThemeMode.LIGHT -> MinimalIconType.SUN
                    AppThemeMode.DIM -> MinimalIconType.DIM_SUN
                    AppThemeMode.NIGHT -> MinimalIconType.MOON
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("theme_option_${mode.code.lowercase()}")
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectMode(mode) }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
                ) {
                    MinimalIcon(
                        type = icon,
                        active = selected,
                        tint = fg,
                        size = 12.dp
                    )
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Virtualized LazyListScope items for the Reviews & Ratings summary and review cards.
 * Keeps each review card lazily composed with stable keys & contentType during scrolling.
 */
fun LazyListScope.liquidReviewSectionItems(
    reviews: List<LibraryReview>,
    onWriteReviewClick: () -> Unit,
    onToggleHelpful: (String) -> Unit
) {
    item(key = "reviews_summary_header", contentType = "reviews_summary") {
        LiquidReviewSummaryCard(
            reviews = reviews,
            onWriteReviewClick = onWriteReviewClick
        )
    }

    items(
        items = reviews,
        key = { "review_${it.id}" },
        contentType = { "review_card" }
    ) { review ->
        LiquidReviewItemCard(
            review = review,
            onToggleHelpful = onToggleHelpful
        )
    }
}

/**
 * Apple Liquid Glass Review & Rating Summary Card + Interactive Write Review & Helpful Cards.
 */
@Composable
fun LiquidReviewSection(
    reviews: List<LibraryReview>,
    onWriteReviewClick: () -> Unit,
    onToggleHelpful: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LiquidReviewSummaryCard(
            reviews = reviews,
            onWriteReviewClick = onWriteReviewClick
        )
        reviews.forEach { review ->
            LiquidReviewItemCard(
                review = review,
                onToggleHelpful = onToggleHelpful
            )
        }
    }
}

@Composable
private fun LiquidReviewSummaryCard(
    reviews: List<LibraryReview>,
    onWriteReviewClick: () -> Unit
) {
    val palette = LiquidTheme.palette
    val summaryStats = remember(reviews) {
        val count = reviews.size
        val avg = if (count == 0) 0.0 else reviews.sumOf { it.rating }.toDouble() / count
        val starFractions = (5 downTo 1).map { stars ->
            val matching = reviews.count { it.rating == stars }
            stars to (if (count == 0) 0f else matching.toFloat() / count.toFloat())
        }
        Triple(count, avg, starFractions)
    }
    val count = summaryStats.first
    val avg = summaryStats.second
    val starFractions = summaryStats.third

    TactileGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reviews & Ratings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = if (count == 0) "No reviews yet — be the first to rate" else "$count synced review(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
                CrimsonActionPill(
                    text = "Write Review",
                    onClick = onWriteReviewClick,
                    modifier = Modifier.testTag("write_review_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (count == 0) "—" else String.format(Locale.US, "%.1f", avg),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { idx ->
                            val filled = count > 0 && idx < avg.toInt()
                            MinimalIcon(
                                type = MinimalIconType.STAR,
                                active = filled,
                                tint = if (filled) palette.accentCrimson else palette.mutedIcon,
                                size = 14.dp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    starFractions.forEach { (stars, frac) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$stars★",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk,
                                modifier = Modifier.width(24.dp)
                            )
                            LinearProgressIndicator(
                                progress = { frac },
                                color = palette.accentCrimson,
                                trackColor = palette.barTrack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidReviewItemCard(
    review: LibraryReview,
    onToggleHelpful: (String) -> Unit
) {
    val palette = LiquidTheme.palette
    val dateLabel = remember(review.createdAt) { formatRelativeDate(review.createdAt) }
    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.accentSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.authorName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSoftInk
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = review.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            if (review.verified) {
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.accentSoftBg
                                ) {
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentSoftInk,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.secondaryPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MinimalIcon(
                            type = MinimalIconType.STAR,
                            active = true,
                            tint = palette.accentCrimson,
                            size = 13.dp
                        )
                        Text(
                            text = "${review.rating}.0",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                }
            }

            Text(
                text = review.body,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.primaryInk
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { onToggleHelpful(review.id) },
                    shape = RoundedCornerShape(9999.dp),
                    color = if (review.isHelpfulByMe) palette.accentSoftBg else palette.secondaryPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.testTag("review_helpful_${review.id}")
                ) {
                    Text(
                        text = if (review.isHelpfulByMe) "Helpful (${review.helpfulCount}) ✓"
                        else "Helpful (${review.helpfulCount})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (review.isHelpfulByMe) palette.accentSoftInk else palette.secondaryInk,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewModal(
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Int, body: String) -> Unit
) {
    val palette = LiquidTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by remember { mutableIntStateOf(5) }
    var body by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Write a Review",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            Text(
                text = "Rate your vault workflow or saved resource. Syncs automatically when signed in.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryInk
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { value ->
                    val active = value <= rating
                    Surface(
                        onClick = { rating = value },
                        shape = RoundedCornerShape(9999.dp),
                        color = if (active) SoftCreamWhite else palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MinimalIcon(
                                type = MinimalIconType.STAR,
                                active = active,
                                tint = if (active) palette.accentCrimson else palette.mutedIcon,
                                size = 16.dp
                            )
                            Text(
                                text = "$value",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (active) SoftCreamInk else palette.secondaryInk
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Your review") },
                placeholder = { Text("Share notes, highlights, or feedback...") },
                minLines = 3,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.cardSurfaceTop,
                    unfocusedContainerColor = palette.cardSurfaceTop,
                    focusedBorderColor = palette.accentCrimson,
                    unfocusedBorderColor = palette.inputBorder,
                    focusedTextColor = palette.primaryInk,
                    unfocusedTextColor = palette.primaryInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("write_review_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel", color = palette.primaryInk)
                }
                TactilePrimaryPillButton(
                    text = "Publish Review",
                    enabled = body.isNotBlank(),
                    onClick = { onSubmitReview(rating, body.trim()) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("submit_review_button")
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * Multi-Device Sync, Google Account Switcher & Instant Send-to-PC/Phone Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncAndSendDeviceModal(
    isSignedIn: Boolean,
    isOnline: Boolean,
    userDisplayName: String,
    userEmail: String?,
    currentDeviceId: String,
    currentDeviceName: String,
    syncBadgeState: SyncBadgeState,
    pendingSyncCount: Int,
    savedAccounts: List<SavedAccountRecord>,
    connectedDevices: List<CloudDeviceSession>,
    transfers: List<CloudDeviceTransfer>,
    initialTitle: String = "",
    initialPayload: String = "",
    onDismiss: () -> Unit,
    onGoogleSignInOrSwitch: () -> Unit,
    onSignOutToGuest: () -> Unit,
    onSyncNow: () -> Unit,
    onSendToDevice: (title: String, payloadText: String, payloadType: String, targetDeviceId: String?) -> Unit,
    onAcceptTransfer: (CloudDeviceTransfer) -> Unit,
    onDismissTransfer: (String) -> Unit
) {
    val palette = LiquidTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sendTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var sendPayload by remember(initialPayload) { mutableStateOf(initialPayload) }
    var sendType by remember {
        mutableStateOf(if (initialPayload.startsWith("http", ignoreCase = true)) "link" else "note")
    }
    var selectedTargetDeviceId by remember { mutableStateOf<String?>(null) }

    val pendingTransfers = remember(transfers) { transfers.filter { it.status == "pending" } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sync & Send to PC / Phone",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "$currentDeviceName • ${if (isOnline) "Realtime Firestore Ready" else "Offline Outbox Active"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
                SyncStatusPill(badgeState = syncBadgeState, onClick = onSyncNow)
            }

            // Account Switcher Card
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Switcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = if (isSignedIn) "Signed in as $userDisplayName (${userEmail ?: "Google"})"
                        else "Guest Mode (Local-Only) • Outbox events: $pendingSyncCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )

                    if (savedAccounts.isNotEmpty()) {
                        savedAccounts.take(3).forEach { acc ->
                            val isActive = isSignedIn && acc.email.equals(userEmail, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isActive) palette.accentSoftBg else palette.recessedTrayBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = palette.primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = acc.email.ifBlank { acc.uid.take(12) },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (isActive) "Active" else "Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) palette.accentSoftInk else palette.secondaryInk
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactilePrimaryPillButton(
                            text = if (isSignedIn) "Switch Account" else "Sign in with Google",
                            onClick = onGoogleSignInOrSwitch,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("device_modal_switch_account")
                        )
                        if (isSignedIn) {
                            OutlinedButton(
                                onClick = onSyncNow,
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, palette.inputBorder),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Sync ($pendingSyncCount)", color = palette.primaryInk)
                            }
                            TextButton(onClick = onSignOutToGuest) {
                                Text("Guest", color = palette.accentCrimson)
                            }
                        }
                    }
                }
            }

            // Connected Devices Session List
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Synced Devices (${connectedDevices.size.coerceAtLeast(if (isSignedIn) 1 else 0)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    if (!isSignedIn) {
                        Text(
                            text = "Sign in with the same Google account on your PC browser or other phone to see live online devices and beam links instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    } else if (connectedDevices.isEmpty()) {
                        Text(
                            text = "$currentDeviceName is registering live presence...",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    } else {
                        connectedDevices.forEach { dev ->
                            val isSelf = dev.id == currentDeviceId
                            val selected = selectedTargetDeviceId == dev.id
                            Surface(
                                onClick = {
                                    selectedTargetDeviceId = if (selected) null else dev.id
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) palette.accentSoftBg else palette.recessedTrayBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (dev.status == "online") TelemetryOptimalDot else palette.mutedIcon)
                                        )
                                        Column {
                                            Text(
                                                text = if (isSelf) "${dev.deviceName} (This Device)" else dev.deviceName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = palette.primaryInk
                                            )
                                            Text(
                                                text = "${dev.platform.uppercase()} • ${dev.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.secondaryInk
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (selected) "Target ✓" else "Select",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) palette.accentSoftInk else palette.secondaryInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Send Link or Note to Synced PC / Phone
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Send to Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("link" to "Link", "note" to "Note").forEach { (code, label) ->
                            val active = sendType == code
                            Surface(
                                onClick = { sendType = code },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (active) palette.primaryButtonBg else palette.recessedTrayBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sendTitle,
                        onValueChange = { sendTitle = it },
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardSurfaceTop,
                            unfocusedContainerColor = palette.cardSurfaceTop,
                            focusedBorderColor = palette.accentCrimson,
                            unfocusedBorderColor = palette.inputBorder,
                            focusedTextColor = palette.primaryInk,
                            unfocusedTextColor = palette.primaryInk
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sendPayload,
                        onValueChange = { sendPayload = it },
                        label = { Text(if (sendType == "link") "Paste URL to send" else "Note text to send") },
                        minLines = 2,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardSurfaceTop,
                            unfocusedContainerColor = palette.cardSurfaceTop,
                            focusedBorderColor = palette.accentCrimson,
                            unfocusedBorderColor = palette.inputBorder,
                            focusedTextColor = palette.primaryInk,
                            unfocusedTextColor = palette.primaryInk
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_send_payload_input")
                    )

                    CrimsonActionPill(
                        text = if (selectedTargetDeviceId != null) "Send to Selected Device" else "Broadcast to All My Devices",
                        enabled = sendPayload.isNotBlank() && isSignedIn && isOnline,
                        onClick = {
                            onSendToDevice(
                                sendTitle.ifBlank { if (sendType == "link") "Shared Link" else "Shared Note" },
                                sendPayload.trim(),
                                sendType,
                                selectedTargetDeviceId
                            )
                            sendTitle = ""
                            sendPayload = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_send_button")
                    )
                }
            }

            // Incoming Pending Transfers from PC / Other Devices
            if (pendingTransfers.isNotEmpty()) {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Incoming Transfers (${pendingTransfers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        pendingTransfers.forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.recessedTrayBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${t.title} • from ${t.fromDeviceName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.primaryInk
                                    )
                                    Text(
                                        text = t.payloadText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onAcceptTransfer(t) },
                                            shape = RoundedCornerShape(9999.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.primaryButtonBg,
                                                contentColor = palette.primaryButtonContent
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Save to Library", style = MaterialTheme.typography.labelMedium)
                                        }
                                        TextButton(onClick = { onDismissTransfer(t.id) }) {
                                            Text("Dismiss", color = palette.secondaryInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
