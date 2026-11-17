package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.ItemEntity
import com.example.data.media.VideoThumbnailGenerator
import com.example.data.util.UrlNormalizer
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.LiquidThemePalette
import com.example.ui.theme.SoftCreamInk
import com.example.ui.theme.SoftCreamWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class ResourceCardDisplayMeta(
    val folderNames: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val healthStatusLabel: String? = null
)

/**
 * Animated Liquid Glass Ambient Background Engine:
 * - Static dot-matrix tile shader cached once in drawWithCache (1 GPU rect draw, zero loops/frame)
 * - 3 slow-drifting translucent 3D orbs/torus rings isolated into sibling GPU graphicsLayer nodes
 *   so background drift never triggers layout or redraw of app content.
 */
@Composable
fun LiquidAmbientBackground(
    modifier: Modifier = Modifier,
    scrollOffsetPx: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LiquidTheme.palette
    Box(modifier = modifier.fillMaxSize()) {
        StaticAmbientBaseLayer(palette = palette)
        DriftingAmbientOrbLayers(palette = palette, scrollOffsetPx = scrollOffsetPx)
        content()
    }
}

@Composable
private fun StaticAmbientBaseLayer(palette: LiquidThemePalette) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val bgBrush = Brush.verticalGradient(
                    colors = listOf(palette.canvasTop, palette.canvasBottom)
                )
                val tilePx = 20.dp.toPx().roundToInt().coerceAtLeast(12)
                val dotRadiusPx = 0.9.dp.toPx().coerceAtLeast(1f)
                val dotBitmap = ImageBitmap(tilePx, tilePx)
                val tileCanvas = ComposeCanvas(dotBitmap)
                val dotPaint = Paint().apply {
                    color = palette.dotMatrixColor
                    isAntiAlias = true
                }
                tileCanvas.drawCircle(
                    center = Offset(tilePx * 0.5f, tilePx * 0.5f),
                    radius = dotRadiusPx,
                    paint = dotPaint
                )
                val dotBrush = ShaderBrush(
                    ImageShader(
                        image = dotBitmap,
                        tileModeX = TileMode.Repeated,
                        tileModeY = TileMode.Repeated
                    )
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawRect(brush = dotBrush)
                }
            }
    )
}

@Composable
private fun DriftingAmbientOrbLayers(
    palette: LiquidThemePalette,
    scrollOffsetPx: Float
) {
    val infinite = rememberInfiniteTransition(label = "liquidBg")
    val drift1 by infinite.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbDrift1"
    )
    val drift2 by infinite.animateFloat(
        initialValue = 0.07f,
        targetValue = -0.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(12500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbDrift2"
    )
    val drift3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbDrift3"
    )

    // Orb 1: Ember Crimson (top-right) - GPU translation only, zero Brush re-allocation
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val parallaxY = (scrollOffsetPx * 0.08f).coerceIn(-140f, 140f)
                translationX = size.width * drift1
                translationY = size.height * (drift2 * 0.5f) - parallaxY * 0.6f
            }
            .drawWithCache {
                val center = Offset(size.width * 0.78f, size.height * 0.14f)
                val radius = (size.width * 0.72f).coerceAtLeast(1f)
                val brush = Brush.radialGradient(
                    colors = listOf(palette.orbCrimson, Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind {
                    drawCircle(brush = brush, center = center, radius = radius)
                }
            }
    )

    // Orb 2: Burnt Sienna / Warm Amber Beam (center-left)
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val parallaxY = (scrollOffsetPx * 0.08f).coerceIn(-140f, 140f)
                translationX = size.width * drift2
                translationY = size.height * (drift1 * 0.6f) + parallaxY * 0.45f
            }
            .drawWithCache {
                val center = Offset(size.width * 0.18f, size.height * 0.48f)
                val radius = (size.width * 0.68f).coerceAtLeast(1f)
                val brush = Brush.radialGradient(
                    colors = listOf(palette.orbSienna, Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind {
                    drawCircle(brush = brush, center = center, radius = radius)
                }
            }
    )

    // Orb 3: Smoked Bronze 3D Torus Ring + Prismatic Sheen (lower-right)
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val parallaxY = (scrollOffsetPx * 0.08f).coerceIn(-140f, 140f)
                translationX = size.width * ((drift3 - 0.5f) * 0.14f)
                translationY = size.height * (-drift1 * 0.4f) - parallaxY * 0.35f
            }
            .drawWithCache {
                val torusCenter = Offset(size.width * 0.66f, size.height * 0.80f)
                val torusRadius = (size.width * 0.36f).coerceAtLeast(1f)
                val glowRadius = torusRadius * 1.45f
                val ringRadius = torusRadius * 0.72f
                val ringColor = if (palette.isDark) Color(0x16FFC8BE) else Color(0x45FFFFFF)
                val ringStroke = Stroke(width = 24.dp.toPx())
                val glowBrush = Brush.radialGradient(
                    colors = listOf(palette.orbBronze, Color.Transparent),
                    center = torusCenter,
                    radius = glowRadius
                )
                val sheenBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        palette.prismaticSheen,
                        palette.orbCrimson.copy(alpha = if (palette.isDark) 0.10f else 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, size.height * 0.18f),
                    end = Offset(size.width, size.height * 0.62f)
                )
                onDrawBehind {
                    drawCircle(brush = glowBrush, center = torusCenter, radius = glowRadius)
                    drawCircle(color = ringColor, radius = ringRadius, center = torusCenter, style = ringStroke)
                    drawRect(brush = sheenBrush)
                }
            }
    )
}

@Composable
fun TactileAlabasterBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidAmbientBackground(modifier = modifier, content = content)
}

@Composable
fun RecessedFramingBezel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    innerPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LiquidTheme.palette
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.recessedTrayBg)
            .drawWithCache {
                val radiusPx = cornerRadius.toPx()
                val cr = CornerRadius(radiusPx, radiusPx)
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                )
                onDrawBehind {
                    drawRoundRect(
                        color = palette.recessedDashedBorder,
                        cornerRadius = cr,
                        style = stroke
                    )
                }
            }
            .padding(innerPadding),
        content = content
    )
}

@Composable
fun TactileGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LiquidTheme.palette
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val bgBrush = remember(containerColor, palette.cardSurfaceTop, palette.cardSurfaceBottom) {
        if (containerColor != null) {
            Brush.verticalGradient(listOf(containerColor, containerColor))
        } else {
            Brush.verticalGradient(listOf(palette.cardSurfaceTop, palette.cardSurfaceBottom))
        }
    }

    val interactiveModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.968f else 1f,
            animationSpec = spring(stiffness = 480f, dampingRatio = 0.76f),
            label = "cardScale"
        )
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    val shadowColor = if (palette.isDark) Color(0x66000000) else Color(0x122D261E)
    val baseModifier = modifier
        .then(interactiveModifier)
        .shadow(
            elevation = if (palette.isDark) 8.dp else 6.dp,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .background(bgBrush)
        .border(width = 1.dp, color = palette.specularRimOutline, shape = shape)
        .drawWithCache {
            val inset = cornerRadius.toPx() * 0.4f
            val y = 0.9.dp.toPx()
            val sw = 1.4.dp.toPx()
            val start = Offset(inset, y)
            val end = Offset((size.width - inset).coerceAtLeast(inset), y)
            onDrawBehind {
                drawLine(
                    color = palette.specularBorderTop,
                    start = start,
                    end = end,
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
            }
        }

    Column(modifier = baseModifier, content = content)
}

/**
 * Featured Warm Crimson-Bronze Gradient Card matching the reference Profile/Telemetry card.
 */
@Composable
fun FeaturedCrimsonBronzeCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LiquidTheme.palette
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val bgBrush = remember(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom) {
        Brush.linearGradient(
            colors = listOf(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom)
        )
    }

    val interactiveModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.968f else 1f,
            animationSpec = spring(stiffness = 480f, dampingRatio = 0.76f),
            label = "bronzeCardScale"
        )
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    val shadowColor = if (palette.isDark) Color(0x78000000) else Color(0x162D261E)
    Column(
        modifier = modifier
            .then(interactiveModifier)
            .shadow(
                elevation = if (palette.isDark) 12.dp else 8.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, color = palette.bronzeCardBorder, shape = shape)
            .drawWithCache {
                val inset = cornerRadius.toPx() * 0.45f
                val y = 1.dp.toPx()
                val sw = 1.3.dp.toPx()
                val start = Offset(inset, y)
                val end = Offset((size.width - inset).coerceAtLeast(inset), y)
                onDrawBehind {
                    drawLine(
                        color = palette.specularBorderTop,
                        start = start,
                        end = end,
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )
                }
            },
        content = content
    )
}

@Composable
fun FloatingGlassIconButton(
    iconType: MinimalIconType,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = LiquidTheme.palette.primaryInk,
    containerColor: Color = LiquidTheme.palette.frostedPillBg,
    size: Dp = 44.dp,
    cornerRadius: Dp = 16.dp
) {
    val palette = LiquidTheme.palette
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "glassBtnScale"
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = 3.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            MinimalIcon(
                type = iconType,
                active = active,
                tint = tint,
                cutoutColor = containerColor,
                size = 20.dp,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun TactilePrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconType: MinimalIconType? = null
) {
    val palette = LiquidTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.75f),
        label = "primaryBtnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primaryButtonBg,
            contentColor = palette.primaryButtonContent,
            disabledContainerColor = palette.mutedIcon.copy(alpha = 0.32f),
            disabledContentColor = palette.secondaryInk
        ),
        border = BorderStroke(1.dp, palette.specularBorderTop.copy(alpha = 0.35f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (iconType != null) {
            MinimalIcon(
                type = iconType,
                active = true,
                tint = palette.primaryButtonContent,
                cutoutColor = palette.primaryButtonBg,
                size = 18.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun CoralDangerPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconType: MinimalIconType? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = CrimsonAccent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        shadowElevation = 4.dp,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (iconType != null) {
                MinimalIcon(
                    type = iconType,
                    active = true,
                    tint = Color.White,
                    cutoutColor = CrimsonAccent,
                    size = 16.dp
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Primary Luxury Pill CTA using Sunset Terracotta-Crimson gradient or Warm Cream-Obsidian contrast.
 */
@Composable
fun AccentActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconType: MinimalIconType = MinimalIconType.SPARKLE
) {
    val palette = LiquidTheme.palette
    val shape = remember { RoundedCornerShape(9999.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.72f),
        label = "accentBtnScale"
    )

    val gradientColors = remember(enabled, palette.isDark, palette.primaryButtonBg, palette.mutedIcon) {
        when {
            !enabled -> listOf(palette.mutedIcon.copy(alpha = 0.30f), palette.mutedIcon.copy(alpha = 0.30f))
            palette.isDark -> listOf(SoftCreamWhite, Color(0xFFDCD6CC))
            else -> listOf(palette.primaryButtonBg, Color(0xFF2D2A27))
        }
    }
    val contentColor = when {
        !enabled -> palette.secondaryInk
        palette.isDark -> SoftCreamInk
        else -> palette.primaryButtonContent
    }
    val gradientBrush = remember(gradientColors) { Brush.horizontalGradient(gradientColors) }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                ambientColor = if (palette.isDark) CrimsonAccent.copy(alpha = 0.22f) else Color(0x22000000),
                spotColor = if (palette.isDark) CrimsonAccent.copy(alpha = 0.22f) else Color(0x22000000)
            )
            .clip(shape)
            .background(gradientBrush)
            .border(1.dp, palette.specularBorderTop.copy(alpha = 0.55f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MinimalIcon(
                type = iconType,
                active = true,
                tint = contentColor,
                cutoutColor = gradientColors.first(),
                size = 18.dp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CrimsonActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LiquidTheme.palette
    val shape = remember { RoundedCornerShape(9999.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.72f),
        label = "crimsonBtnScale"
    )
    val bgBrush = remember(enabled, palette.accentTerracottaTop, palette.accentTerracottaBottom, palette.recessedTrayBg) {
        Brush.horizontalGradient(
            colors = if (enabled) listOf(palette.accentTerracottaTop, palette.accentTerracottaBottom)
            else listOf(palette.recessedTrayBg, palette.recessedTrayBg)
        )
    }

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 9.dp else 0.dp,
                shape = shape,
                ambientColor = palette.accentCrimson.copy(alpha = 0.35f),
                spotColor = palette.accentCrimson.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(brush = bgBrush)
            .border(1.dp, palette.specularBorderTop.copy(alpha = 0.55f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else palette.secondaryInk,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (enabled) SoftCreamWhite else palette.mutedIcon),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = SoftCreamInk,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun SyncStatusPill(
    badgeState: SyncBadgeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val dotColor = when (badgeState) {
        SyncBadgeState.SYNCED -> Color(0xFF10B981)
        SyncBadgeState.LOCAL_ONLY -> palette.secondaryInk
        SyncBadgeState.SYNCING, SyncBadgeState.SYNC_PENDING -> palette.accentTerracottaTop
        SyncBadgeState.NEEDS_ATTENTION, SyncBadgeState.SIGN_IN_REQUIRED -> palette.accentCrimson
    }
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("sync_status_pill"),
        shape = RoundedCornerShape(9999.dp),
        color = palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = badgeState.label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryInk
            )
        }
    }
}

/**
 * 5 Vertical Liquid Glass Capsule Gauges rendered in curated Liquid Glass accent gradient.
 */
@Composable
fun VerticalLiquidCapsuleGauges(
    values: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val maxIdx = remember(values) { values.indices.maxByOrNull { values[it].second } ?: 0 }
    val peakBrush = remember(palette.accentTerracottaTop, palette.accentTerracottaBottom) {
        Brush.verticalGradient(listOf(palette.accentTerracottaTop, palette.accentTerracottaBottom))
    }
    val subBrush = remember(palette.bronzeCardTop, palette.accentTerracottaBottom) {
        Brush.verticalGradient(
            listOf(palette.bronzeCardTop.copy(alpha = 0.95f), palette.accentTerracottaBottom.copy(alpha = 0.72f))
        )
    }
    val capsuleShape = remember { RoundedCornerShape(9999.dp) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { idx, (label, frac) ->
            val clamped = frac.coerceIn(0.14f, 1f)
            val pct = (clamped * 100).toInt()
            val isPeak = idx == maxIdx
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = capsuleShape,
                    color = if (isPeak) palette.accentCrimson else palette.recessedTrayBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Text(
                        text = if (isPeak) "$pct%" else "+${pct / 6}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPeak) Color.White else palette.secondaryInk,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(capsuleShape)
                        .background(palette.barTrack)
                        .border(1.dp, palette.specularRimOutline, capsuleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(clamped)
                            .clip(capsuleShape)
                            .background(if (isPeak) peakBrush else subBrush)
                    )
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPeak) palette.primaryInk else palette.secondaryInk,
                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OfflineStatusBanner(
    message: String = "Offline Mode • Local SQLite & FTS active. Online AI & sync paused.",
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoftBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = "Offline",
                    tint = palette.accentSoftInk,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = palette.primaryInk
            )
        }
    }
}

/**
 * Tactile Liquid Glass Resource Card with Warm Crimson-Bronze / Pearl Cream header,
 * interactive 3-slide 360° preview scrubber, Share-to-Device button, and Bookmark button.
 */
@Composable
fun ResourceItemCard(
    item: ItemEntity,
    folderNames: List<String>,
    tagNames: List<String>,
    healthStatusLabel: String? = null,
    matchLabel: String? = null,
    matchReason: String? = null,
    onOpenClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareToDeviceClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onOpenExternalClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current
    var previewSlide by remember(item.id) { mutableIntStateOf(0) }
    var dragAccum by remember(item.id) { mutableFloatStateOf(0f) }
    val durationLabel = remember(item.metadataJson) {
        VideoThumbnailGenerator.extractDurationLabel(item.metadataJson)
    }

    val externalUrl = remember(item.originalUrl, item.canonicalUrl) {
        (item.originalUrl ?: item.canonicalUrl)?.takeIf { UrlNormalizer.isSafeRemoteUrl(it) }
    }
    val effectiveThumbUrl = remember(item.thumbnailUrl, externalUrl, item.domain) {
        item.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: externalUrl?.let { UrlNormalizer.resolveSmartThumbnailUrl(it, item.domain) }
    }
    val domainFaviconFallbackUrl = remember(externalUrl, item.domain) {
        val host = item.domain?.takeIf { it.isNotBlank() }
            ?: externalUrl?.let { runCatching { Uri.parse(it).host }.getOrNull() }?.takeIf { it.isNotBlank() }
        host?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }
    }
    var primaryImageFailed by remember(item.id, effectiveThumbUrl) { mutableStateOf(false) }
    var fallbackFaviconFailed by remember(item.id, domainFaviconFallbackUrl) { mutableStateOf(false) }

    val displayThumbUrl = remember(effectiveThumbUrl, domainFaviconFallbackUrl, primaryImageFailed, fallbackFaviconFailed) {
        when {
            !effectiveThumbUrl.isNullOrBlank() && !primaryImageFailed -> effectiveThumbUrl
            !domainFaviconFallbackUrl.isNullOrBlank() && !fallbackFaviconFailed -> domainFaviconFallbackUrl
            else -> null
        }
    }
    val isFaviconPreview = remember(displayThumbUrl) {
        UrlNormalizer.isFaviconFallbackUrl(displayThumbUrl)
    }

    val heroShape = remember { RoundedCornerShape(26.dp) }
    val heroBrush = remember(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom) {
        Brush.linearGradient(
            colors = listOf(palette.bronzeCardTop, palette.bronzeCardMid, palette.bronzeCardBottom)
        )
    }
    val overlayBrush = remember {
        Brush.verticalGradient(listOf(Color(0x140A0A0C), Color(0x660A0A0C)))
    }
    val relativeUpdated = remember(item.updatedAt) { formatRelativeDate(item.updatedAt) }
    val imageRequest = remember(displayThumbUrl, isFaviconPreview, context) {
        displayThumbUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .apply {
                    if (isFaviconPreview) size(128, 128) else size(720, 360)
                }
                .crossfade(false)
                .build()
        }
    }

    val typeMinimalIcon = when (item.contentType) {
        "video" -> MinimalIconType.PLAY_SQUIRCLE
        "github" -> MinimalIconType.LAYERS
        "document" -> MinimalIconType.INBOX
        "image" -> MinimalIconType.SPARKLE
        "note" -> MinimalIconType.EDIT
        "local_file" -> MinimalIconType.FOLDER
        else -> MinimalIconType.BOOKMARK
    }

    val watermarkLabel = when (item.contentType) {
        "github" -> "GITHUB"
        "video" -> "VIDEO"
        "document" -> "DOCUMENT"
        "image" -> "MEDIA"
        "note" -> "NOTE"
        "local_file" -> "DEVICE"
        "article" -> "ARTICLE"
        else -> "ARCHIVE"
    }

    TactileGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resource_card_${item.id}"),
        cornerRadius = 32.dp,
        onClick = onOpenClick
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(heroShape)
                    .background(heroBrush)
                    .border(1.dp, palette.bronzeCardBorder, heroShape)
                    .pointerInput(item.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = { dragAccum = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccum += dragAmount
                                if (dragAccum > 56f) {
                                    previewSlide = (previewSlide + 1) % 3
                                    dragAccum = 0f
                                } else if (dragAccum < -56f) {
                                    previewSlide = (previewSlide + 2) % 3
                                    dragAccum = 0f
                                }
                            }
                        )
                    }
            ) {
                if (imageRequest != null && previewSlide == 0) {
                    if (isFaviconPreview) {
                        // Crisp domain favicon inside frosted squircle over gradient art (no blurry upscaling)
                        Text(
                            text = (item.domain?.substringBefore('.') ?: watermarkLabel).uppercase(),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            color = palette.primaryInk.copy(alpha = 0.06f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-8).dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = palette.frostedPillBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-6).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = item.domain ?: item.title,
                                    contentScale = ContentScale.Fit,
                                    onError = {
                                        if (displayThumbUrl == effectiveThumbUrl && !primaryImageFailed) {
                                            primaryImageFailed = true
                                        } else {
                                            fallbackFaviconFailed = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = item.domain ?: "Web Resource",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            onError = { primaryImageFailed = true },
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(overlayBrush)
                        )
                        if (item.contentType == "video") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xA6101012))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = watermarkLabel,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        color = palette.primaryInk.copy(alpha = 0.06f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-8).dp)
                    )

                    AnimatedContent(
                        targetState = previewSlide,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        label = "cardHeroSlide"
                    ) { slide ->
                        when (slide) {
                            0 -> MinimalIcon(
                                type = typeMinimalIcon,
                                active = true,
                                tint = palette.primaryInk.copy(alpha = 0.88f),
                                cutoutColor = palette.bronzeCardMid,
                                size = 46.dp
                            )
                            1 -> Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = item.canonicalUrl ?: item.domain ?: "Stored Locally on Device",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryInk,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            else -> Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.primaryButtonBg
                            ) {
                                Text(
                                    text = item.description.ifBlank {
                                        "Tap card to inspect metadata, folders, tags & personal notes."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryButtonContent,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Top-left Source Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.frostedPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        MinimalIcon(
                            type = typeMinimalIcon,
                            active = false,
                            tint = palette.primaryInk,
                            size = 13.dp
                        )
                        Text(
                            text = item.sourceType.replace('_', ' ').uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                }

                // Top-right frosted squircle action buttons: Send + Edit + Delete + Bookmark
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (onShareToDeviceClick != null) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.SHARE,
                            contentDescription = "Send to PC or Device",
                            tint = palette.primaryInk,
                            containerColor = palette.frostedPillBg,
                            size = 34.dp,
                            cornerRadius = 11.dp,
                            onClick = onShareToDeviceClick
                        )
                    }
                    if (onEditClick != null) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.EDIT,
                            contentDescription = "Edit item",
                            tint = palette.primaryInk,
                            containerColor = palette.frostedPillBg,
                            size = 34.dp,
                            cornerRadius = 11.dp,
                            onClick = onEditClick,
                            modifier = Modifier.testTag("card_edit_${item.id}")
                        )
                    }
                    if (onDeleteClick != null) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.TRASH,
                            contentDescription = "Delete item",
                            tint = CrimsonAccent,
                            containerColor = palette.frostedPillBg,
                            size = 34.dp,
                            cornerRadius = 11.dp,
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag("card_delete_${item.id}")
                        )
                    }
                    FloatingGlassIconButton(
                        iconType = MinimalIconType.BOOKMARK,
                        contentDescription = if (item.isFavorite) "Remove bookmark" else "Bookmark",
                        active = item.isFavorite,
                        tint = if (item.isFavorite) CrimsonAccent else palette.primaryInk,
                        containerColor = palette.frostedPillBg,
                        size = 34.dp,
                        cornerRadius = 11.dp,
                        onClick = onFavoriteClick
                    )
                }

                // Interactive Curved Scrub Arc + '< >' Pill Handle (cached Path & Stroke)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(bottom = 3.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { previewSlide = (previewSlide + 1) % 3 }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val arcPath = Path().apply {
                                    moveTo(size.width * 0.14f, size.height * 0.12f)
                                    quadraticTo(
                                        size.width * 0.5f,
                                        size.height * 0.78f,
                                        size.width * 0.86f,
                                        size.height * 0.12f
                                    )
                                }
                                val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                                onDrawBehind {
                                    drawPath(
                                        path = arcPath,
                                        color = palette.specularBorderTop,
                                        style = stroke
                                    )
                                }
                            }
                    )
                    Surface(
                        onClick = { previewSlide = (previewSlide + 1) % 3 },
                        shape = RoundedCornerShape(9999.dp),
                        color = palette.primaryButtonBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline)
                    ) {
                        Text(
                            text = "‹  ›",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryButtonContent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.contentType == "video" && !durationLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Color(0xCC0D0D10),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 10.dp)
                            .testTag("video_duration_badge_${item.id}")
                    ) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        val metaText = remember(item.domain, item.isDeviceLocal, relativeUpdated) {
                            buildString {
                                append(item.domain ?: if (item.isDeviceLocal) "Device Storage (Ref)" else "Personal Note")
                                append(" • ")
                                append(relativeUpdated)
                            }
                        }
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val badgeText = matchLabel ?: healthStatusLabel ?: when (item.status) {
                        "unsorted" -> "Inbox"
                        "duplicate_candidate" -> "Duplicate"
                        "failed_metadata" -> "Retry Meta"
                        else -> "Saved"
                    }
                    val isMicroAlert = badgeText in setOf("Duplicate", "Missing", "Permission Needed", "Retry Meta")
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = if (isMicroAlert) CrimsonAccent else palette.accentSoftBg,
                        border = BorderStroke(0.8.dp, palette.specularRimOutline)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMicroAlert) Color.White else palette.accentSoftInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!matchReason.isNullOrBlank()) {
                    Text(
                        text = matchReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentSoftInk
                    )
                } else if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        folderNames.take(2).forEach { folderName ->
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = palette.secondaryPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        tagNames.take(1).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = palette.accentSoftBg
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentSoftInk,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (externalUrl != null) {
                            OutlinedButton(
                                onClick = onOpenClick,
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("card_details_${item.id}")
                            ) {
                                Text("Details", style = MaterialTheme.typography.labelMedium, color = palette.primaryInk)
                            }
                            Button(
                                onClick = {
                                    if (onOpenExternalClick != null) {
                                        onOpenExternalClick()
                                    } else {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryButtonBg,
                                    contentColor = palette.primaryButtonContent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("card_open_external_${item.id}")
                            ) {
                                MinimalIcon(
                                    type = MinimalIconType.OPEN_EXTERNAL,
                                    active = false,
                                    tint = palette.primaryButtonContent,
                                    size = 13.dp
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Open Link", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onOpenClick,
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryButtonBg,
                                    contentColor = palette.primaryButtonContent
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("card_open_${item.id}")
                            ) {
                                Text("Open", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditResourceDialog(
    item: ItemEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String?, description: String) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember(item.id) { mutableStateOf(item.title) }
    var url by remember(item.id) { mutableStateOf(item.originalUrl ?: item.canonicalUrl.orEmpty()) }
    var description by remember(item.id) { mutableStateOf(item.description) }

    Dialog(onDismissRequest = onDismiss) {
        TactileGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_item_dialog"),
            cornerRadius = 30.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalIcon(
                        type = MinimalIconType.EDIT,
                        active = true,
                        tint = palette.accentSoftInk,
                        size = 20.dp
                    )
                    Text(
                        text = "Edit Resource",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.specularRimOutline,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_title_input")
                )

                if (!item.isDeviceLocal && (!item.originalUrl.isNullOrBlank() || !item.canonicalUrl.isNullOrBlank() || url.isNotBlank())) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Link URL (https://...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.recessedTrayBg,
                            unfocusedContainerColor = palette.recessedTrayBg,
                            focusedBorderColor = CrimsonAccent,
                            unfocusedBorderColor = palette.specularRimOutline,
                            focusedTextColor = palette.primaryInk,
                            unfocusedTextColor = palette.primaryInk
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_item_url_input")
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.specularRimOutline,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_desc_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = palette.primaryInk)
                    }
                    TactilePrimaryPillButton(
                        text = "Save Changes",
                        enabled = title.trim().isNotBlank(),
                        onClick = {
                            onSave(
                                title.trim(),
                                url.trim().takeIf { it.isNotBlank() },
                                description.trim()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_save_button")
                    )
                }
            }
        }
    }
}

enum class MainNavTab(val route: String, val label: String, val iconType: MinimalIconType) {
    HOME("home", "Home", MinimalIconType.HOME),
    FOLDERS("folders", "Vault", MinimalIconType.GRID),
    SEARCH("search", "Search", MinimalIconType.SEARCH),
    LIBRARY("library", "AI Hub", MinimalIconType.SPARKLE)
}

/**
 * Clean 5-Slot Squircle Liquid Glass Dock using pure GPU graphicsLayer translation & stretch
 * so active pill morphing runs with zero layout invalidations per frame.
 */
@Composable
fun FloatingIconlyBottomDock(
    selectedTab: MainNavTab?,
    isAiGlowing: Boolean,
    onSelectTab: (MainNavTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val slotIndex = when (selectedTab) {
        MainNavTab.HOME -> 0
        MainNavTab.FOLDERS -> 1
        MainNavTab.SEARCH -> 3
        MainNavTab.LIBRARY -> 4
        null -> -1
    }
    val dockShape = remember { RoundedCornerShape(34.dp) }
    val slotShape = remember { RoundedCornerShape(20.dp) }
    val addBrush = remember(palette.accentTerracottaTop, palette.accentTerracottaBottom) {
        Brush.verticalGradient(listOf(palette.accentTerracottaTop, palette.accentTerracottaBottom))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = dockShape,
                    ambientColor = if (palette.isDark) Color(0x88000000) else Color(0x1C2D261E),
                    spotColor = if (palette.isDark) Color(0x88000000) else Color(0x1C2D261E)
                )
                .clip(dockShape)
                .background(palette.dockBg)
                .border(1.dp, palette.specularRimOutline, dockShape)
                .drawWithCache {
                    val start = Offset(28.dp.toPx(), 1.dp.toPx())
                    val end = Offset((size.width - 28.dp.toPx()).coerceAtLeast(start.x), 1.dp.toPx())
                    val sw = 1.4.dp.toPx()
                    onDrawBehind {
                        drawLine(
                            color = palette.specularBorderTop,
                            start = start,
                            end = end,
                            strokeWidth = sw,
                            cap = StrokeCap.Round
                        )
                    }
                }
                .padding(7.dp)
        ) {
            val slotSpacing = 6.dp
            val slotWidth = (maxWidth - slotSpacing * 4) / 5f
            val animatedSlot by animateFloatAsState(
                targetValue = slotIndex.coerceAtLeast(0).toFloat(),
                animationSpec = spring(stiffness = 420f, dampingRatio = 0.78f, visibilityThreshold = 0.002f),
                label = "squirclePillSlot"
            )

            // Sliding Active Squircle Tile behind selected slot (pure GPU transform, zero relayout)
            if (slotIndex >= 0) {
                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val slotSpacingPx = slotSpacing.toPx()
                            val slotWidthPx = slotWidth.toPx()
                            translationX = animatedSlot * (slotWidthPx + slotSpacingPx)
                            val delta = abs(animatedSlot - slotIndex.toFloat())
                            val stretch = (1f + delta * 0.08f).coerceIn(1f, 1.12f)
                            scaleX = stretch
                            scaleY = 1f - ((stretch - 1f) * 0.14f)
                        }
                        .clip(slotShape)
                        .background(palette.dockTileActiveBg)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(slotSpacing)
            ) {
                DockSquircleSlot(
                    tab = MainNavTab.HOME,
                    selected = selectedTab == MainNavTab.HOME,
                    isAiGlowing = false,
                    onClick = { onSelectTab(MainNavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )
                DockSquircleSlot(
                    tab = MainNavTab.FOLDERS,
                    selected = selectedTab == MainNavTab.FOLDERS,
                    isAiGlowing = false,
                    onClick = { onSelectTab(MainNavTab.FOLDERS) },
                    modifier = Modifier.weight(1f)
                )

                // Center Chunky '+' Quick Capture Squircle Tile
                val addInteraction = remember { MutableInteractionSource() }
                val addPressed by addInteraction.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (addPressed) 0.92f else 1f,
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.68f),
                    label = "addSquircleScale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .minimumInteractiveComponentSize()
                        .graphicsLayer {
                            scaleX = addScale
                            scaleY = addScale
                        }
                        .testTag("nav_add_button")
                        .shadow(
                            elevation = 6.dp,
                            shape = slotShape,
                            ambientColor = palette.accentCrimson.copy(alpha = 0.32f),
                            spotColor = palette.accentCrimson.copy(alpha = 0.32f)
                        )
                        .clip(slotShape)
                        .background(addBrush)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), slotShape)
                        .clickable(
                            interactionSource = addInteraction,
                            indication = androidx.compose.material3.ripple(),
                            onClick = onAddClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalIcon(
                        type = MinimalIconType.PLUS,
                        active = true,
                        tint = Color.White,
                        size = 21.dp,
                        contentDescription = "Capture resource"
                    )
                }

                DockSquircleSlot(
                    tab = MainNavTab.SEARCH,
                    selected = selectedTab == MainNavTab.SEARCH,
                    isAiGlowing = false,
                    onClick = { onSelectTab(MainNavTab.SEARCH) },
                    modifier = Modifier.weight(1f)
                )
                DockSquircleSlot(
                    tab = MainNavTab.LIBRARY,
                    selected = selectedTab == MainNavTab.LIBRARY,
                    isAiGlowing = isAiGlowing,
                    onClick = { onSelectTab(MainNavTab.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DockSquircleSlot(
    tab: MainNavTab,
    selected: Boolean,
    isAiGlowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val iconTint by animateColorAsState(
        targetValue = when {
            selected -> palette.dockTileActiveContent
            isAiGlowing -> palette.accentCrimson
            else -> palette.secondaryInk
        },
        label = "dockTint_${tab.route}"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .testTag("nav_tab_${tab.route}")
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.Transparent else palette.dockTileInactiveBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        MinimalIcon(
            type = tab.iconType,
            active = selected,
            tint = iconTint,
            cutoutColor = palette.dockTileActiveBg,
            size = 22.dp,
            contentDescription = tab.label
        )
    }
}

private val shortDateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
}

fun formatRelativeDate(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val mins = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> (shortDateFormatThreadLocal.get() ?: SimpleDateFormat("MMM d", Locale.getDefault())).format(Date(epochMillis))
    }
}
