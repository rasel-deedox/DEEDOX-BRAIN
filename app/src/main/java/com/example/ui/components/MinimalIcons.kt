package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidTheme
import kotlin.math.cos
import kotlin.math.sin

enum class MinimalIconType {
    HOME,
    SEARCH,
    FOLDER,
    SPARKLE,
    BOOKMARK,
    PLUS,
    BELL,
    LAYERS,
    SETTINGS,
    BACK,
    OPEN_EXTERNAL,
    INBOX,
    SHIELD,
    CLOUD_SYNC,
    PLAY_SQUIRCLE,
    SEND,
    TRASH,
    EDIT,
    CHECK,
    HEART,
    SHARE,
    DEVICE_SYNC,
    STAR,
    GRID,
    SUN,
    DIM_SUN,
    MOON,
    SLIDERS,
    MIC,
    LOCK,
    ARROW_UP_RIGHT,
    NOTEBOOK,
    TIMER
}

private val sunRayAngles = DoubleArray(8) { idx -> Math.toRadians((idx * 45).toDouble()) }

/**
 * Iconly Pro 24x24 viewBox rounded squircle icon renderer.
 * Uses drawWithCache to pre-build Path & Stroke instances once per (size, type, active, tint, cutoutColor)
 * for zero per-frame allocations during 120Hz scrolling.
 */
@Composable
fun MinimalIcon(
    type: MinimalIconType,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = if (active) LiquidTheme.palette.primaryInk else LiquidTheme.palette.mutedIcon,
    cutoutColor: Color = LiquidTheme.palette.cardSurfaceTop,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    val semanticsMod = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Spacer(
        modifier = semanticsMod
            .size(size)
            .drawWithCache {
                val u = this.size.width / 24f
                val strokeWidth = 2f * u
                val outline = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val thinCutoutStroke = Stroke(width = 1.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

                when (type) {
                    MinimalIconType.HOME -> {
                        val house = Path().apply {
                            moveTo(3.5f * u, 10.6f * u)
                            cubicTo(3.5f * u, 9.4f * u, 4.1f * u, 8.4f * u, 5.1f * u, 7.6f * u)
                            lineTo(10.3f * u, 3.8f * u)
                            cubicTo(11.3f * u, 3.05f * u, 12.7f * u, 3.05f * u, 13.7f * u, 3.8f * u)
                            lineTo(18.9f * u, 7.6f * u)
                            cubicTo(19.9f * u, 8.4f * u, 20.5f * u, 9.4f * u, 20.5f * u, 10.6f * u)
                            lineTo(20.5f * u, 17.5f * u)
                            cubicTo(20.5f * u, 19.7f * u, 19.1f * u, 21f * u, 16.9f * u, 21f * u)
                            lineTo(7.1f * u, 21f * u)
                            cubicTo(4.9f * u, 21f * u, 3.5f * u, 19.7f * u, 3.5f * u, 17.5f * u)
                            close()
                        }
                        val doorTopLeft = Offset(10.2f * u, 14.3f * u)
                        val doorSize = Size(3.6f * u, 5.2f * u)
                        val doorRadius = CornerRadius(1.8f * u, 1.8f * u)
                        val lineStart = Offset(9.5f * u, 16.8f * u)
                        val lineEnd = Offset(14.5f * u, 16.8f * u)
                        onDrawBehind {
                            if (active) {
                                drawPath(house, color = tint)
                                drawRoundRect(
                                    color = cutoutColor,
                                    topLeft = doorTopLeft,
                                    size = doorSize,
                                    cornerRadius = doorRadius
                                )
                            } else {
                                drawPath(house, color = tint, style = outline)
                                drawLine(
                                    color = tint,
                                    start = lineStart,
                                    end = lineEnd,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    MinimalIconType.SEARCH -> {
                        val center = Offset(11f * u, 11f * u)
                        val radius = 6.8f * u
                        val innerR = 2.6f * u
                        val sStart = Offset(16.2f * u, 16.2f * u)
                        val sEnd = Offset(20.2f * u, 20.2f * u)
                        val handleW = if (active) 2.4f * u else strokeWidth
                        onDrawBehind {
                            if (active) {
                                drawCircle(color = tint, radius = radius, center = center)
                                drawCircle(color = cutoutColor, radius = innerR, center = center)
                            } else {
                                drawCircle(color = tint, radius = radius, center = center, style = outline)
                            }
                            drawLine(
                                color = tint,
                                start = sStart,
                                end = sEnd,
                                strokeWidth = handleW,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    MinimalIconType.FOLDER -> {
                        val folderPath = Path().apply {
                            moveTo(3f * u, 8.2f * u)
                            cubicTo(3f * u, 5.8f * u, 4.3f * u, 4.5f * u, 6.6f * u, 4.5f * u)
                            lineTo(8.8f * u, 4.5f * u)
                            cubicTo(9.8f * u, 4.5f * u, 10.6f * u, 5.0f * u, 11.2f * u, 5.8f * u)
                            lineTo(12.2f * u, 7.1f * u)
                            lineTo(17.4f * u, 7.1f * u)
                            cubicTo(19.7f * u, 7.1f * u, 21f * u, 8.4f * u, 21f * u, 10.8f * u)
                            lineTo(21f * u, 16.4f * u)
                            cubicTo(21f * u, 18.8f * u, 19.7f * u, 20f * u, 17.4f * u, 20f * u)
                            lineTo(6.6f * u, 20f * u)
                            cubicTo(4.3f * u, 20f * u, 3f * u, 18.8f * u, 3f * u, 16.4f * u)
                            close()
                        }
                        val sStart = Offset(7.5f * u, 12.2f * u)
                        val sEnd = Offset(16.5f * u, 12.2f * u)
                        onDrawBehind {
                            if (active) {
                                drawPath(folderPath, color = tint)
                                drawLine(
                                    color = cutoutColor,
                                    start = sStart,
                                    end = sEnd,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            } else {
                                drawPath(folderPath, color = tint, style = outline)
                            }
                        }
                    }

                    MinimalIconType.SPARKLE -> {
                        val star = Path().apply {
                            moveTo(12f * u, 2.8f * u)
                            cubicTo(12.6f * u, 8.2f * u, 15.8f * u, 11.4f * u, 21.2f * u, 12f * u)
                            cubicTo(15.8f * u, 12.6f * u, 12.6f * u, 15.8f * u, 12f * u, 21.2f * u)
                            cubicTo(11.4f * u, 15.8f * u, 8.2f * u, 12.6f * u, 2.8f * u, 12f * u)
                            cubicTo(8.2f * u, 11.4f * u, 11.4f * u, 8.2f * u, 12f * u, 2.8f * u)
                            close()
                        }
                        val cCenter = Offset(12f * u, 12f * u)
                        val cRadius = 1.9f * u
                        onDrawBehind {
                            if (active) {
                                drawPath(star, color = tint)
                                drawCircle(color = cutoutColor, radius = cRadius, center = cCenter)
                            } else {
                                drawPath(star, color = tint, style = outline)
                            }
                        }
                    }

                    MinimalIconType.BOOKMARK -> {
                        val bm = Path().apply {
                            moveTo(5.8f * u, 6.2f * u)
                            cubicTo(5.8f * u, 4.1f * u, 7f * u, 3.2f * u, 9f * u, 3.2f * u)
                            lineTo(15f * u, 3.2f * u)
                            cubicTo(17f * u, 3.2f * u, 18.2f * u, 4.1f * u, 18.2f * u, 6.2f * u)
                            lineTo(18.2f * u, 19.2f * u)
                            cubicTo(18.2f * u, 20.4f * u, 17f * u, 20.9f * u, 16f * u, 20.2f * u)
                            lineTo(12f * u, 17.5f * u)
                            lineTo(8f * u, 20.2f * u)
                            cubicTo(7f * u, 20.9f * u, 5.8f * u, 20.4f * u, 5.8f * u, 19.2f * u)
                            close()
                        }
                        val sStart = Offset(9.5f * u, 8.5f * u)
                        val sEnd = Offset(14.5f * u, 8.5f * u)
                        onDrawBehind {
                            if (active) {
                                drawPath(bm, color = tint)
                                drawLine(
                                    color = cutoutColor,
                                    start = sStart,
                                    end = sEnd,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            } else {
                                drawPath(bm, color = tint, style = outline)
                            }
                        }
                    }

                    MinimalIconType.HEART -> {
                        val heart = Path().apply {
                            moveTo(12f * u, 20f * u)
                            cubicTo(6.5f * u, 16.6f * u, 3.3f * u, 13.2f * u, 3.3f * u, 9.1f * u)
                            cubicTo(3.3f * u, 6.1f * u, 5.6f * u, 4f * u, 8.4f * u, 4f * u)
                            cubicTo(10.1f * u, 4f * u, 11.4f * u, 4.9f * u, 12f * u, 6.1f * u)
                            cubicTo(12.6f * u, 4.9f * u, 13.9f * u, 4f * u, 15.6f * u, 4f * u)
                            cubicTo(18.4f * u, 4f * u, 20.7f * u, 6.1f * u, 20.7f * u, 9.1f * u)
                            cubicTo(20.7f * u, 13.2f * u, 17.5f * u, 16.6f * u, 12f * u, 20f * u)
                            close()
                        }
                        onDrawBehind {
                            if (active) drawPath(heart, color = tint)
                            else drawPath(heart, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.PLUS -> {
                        val vStart = Offset(12f * u, 5f * u)
                        val vEnd = Offset(12f * u, 19f * u)
                        val hStart = Offset(5f * u, 12f * u)
                        val hEnd = Offset(19f * u, 12f * u)
                        val pw = 2.2f * u
                        onDrawBehind {
                            drawLine(tint, vStart, vEnd, strokeWidth = pw, cap = StrokeCap.Round)
                            drawLine(tint, hStart, hEnd, strokeWidth = pw, cap = StrokeCap.Round)
                        }
                    }

                    MinimalIconType.BELL -> {
                        val bell = Path().apply {
                            moveTo(12f * u, 3.5f * u)
                            cubicTo(8.2f * u, 3.5f * u, 6f * u, 6.3f * u, 6f * u, 10.2f * u)
                            lineTo(5.6f * u, 14.1f * u)
                            cubicTo(5.4f * u, 15.3f * u, 4.5f * u, 16.3f * u, 4.5f * u, 17f * u)
                            lineTo(19.5f * u, 17f * u)
                            cubicTo(19.5f * u, 16.3f * u, 18.6f * u, 15.3f * u, 18.4f * u, 14.1f * u)
                            lineTo(18f * u, 10.2f * u)
                            cubicTo(18f * u, 6.3f * u, 15.8f * u, 3.5f * u, 12f * u, 3.5f * u)
                            close()
                        }
                        val clapperStart = Offset(10f * u, 20f * u)
                        val clapperEnd = Offset(14f * u, 20f * u)
                        onDrawBehind {
                            if (active) drawPath(bell, color = tint)
                            else drawPath(bell, color = tint, style = outline)
                            drawLine(tint, clapperStart, clapperEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        }
                    }

                    MinimalIconType.LAYERS -> {
                        val top = Path().apply {
                            moveTo(12f * u, 4f * u)
                            lineTo(20f * u, 8.5f * u)
                            lineTo(12f * u, 13f * u)
                            lineTo(4f * u, 8.5f * u)
                            close()
                        }
                        val mid = Path().apply {
                            moveTo(4.5f * u, 12.6f * u)
                            lineTo(12f * u, 16.8f * u)
                            lineTo(19.5f * u, 12.6f * u)
                        }
                        val bot = Path().apply {
                            moveTo(4.5f * u, 16.6f * u)
                            lineTo(12f * u, 20.6f * u)
                            lineTo(19.5f * u, 16.6f * u)
                        }
                        onDrawBehind {
                            if (active) drawPath(top, color = tint)
                            else drawPath(top, color = tint, style = outline)
                            drawPath(mid, color = tint, style = outline)
                            drawPath(bot, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.SETTINGS -> {
                        val rOuter = 7.6f * u
                        val rInnerActive = 3.1f * u
                        val rInnerOutline = 2.8f * u
                        val center = Offset(12f * u, 12f * u)
                        onDrawBehind {
                            if (active) {
                                drawCircle(color = tint, radius = rOuter, center = center)
                                drawCircle(color = cutoutColor, radius = rInnerActive, center = center)
                            } else {
                                drawCircle(color = tint, radius = rOuter, center = center, style = outline)
                                drawCircle(color = tint, radius = rInnerOutline, center = center, style = outline)
                            }
                        }
                    }

                    MinimalIconType.BACK -> {
                        val lStart = Offset(19f * u, 12f * u)
                        val lEnd = Offset(5.5f * u, 12f * u)
                        val head = Path().apply {
                            moveTo(11f * u, 6.5f * u)
                            lineTo(5.2f * u, 12f * u)
                            lineTo(11f * u, 17.5f * u)
                        }
                        onDrawBehind {
                            drawLine(tint, lStart, lEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawPath(head, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.OPEN_EXTERNAL -> {
                        val topLeft = Offset(4f * u, 6f * u)
                        val boxSize = Size(13f * u, 13f * u)
                        val boxR = CornerRadius(4f * u, 4f * u)
                        val dStart = Offset(11.5f * u, 12.5f * u)
                        val dEnd = Offset(19.5f * u, 4.5f * u)
                        val corner = Path().apply {
                            moveTo(14.5f * u, 4.5f * u)
                            lineTo(19.5f * u, 4.5f * u)
                            lineTo(19.5f * u, 9.5f * u)
                        }
                        onDrawBehind {
                            drawRoundRect(tint, topLeft, boxSize, boxR, style = outline)
                            drawLine(tint, dStart, dEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawPath(corner, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.INBOX -> {
                        val topLeft = Offset(3.5f * u, 4.5f * u)
                        val boxSize = Size(17f * u, 15f * u)
                        val boxR = CornerRadius(5f * u, 5f * u)
                        val tray = Path().apply {
                            moveTo(4f * u, 13.5f * u)
                            lineTo(8.5f * u, 13.5f * u)
                            lineTo(10f * u, 15.8f * u)
                            lineTo(14f * u, 15.8f * u)
                            lineTo(15.5f * u, 13.5f * u)
                            lineTo(20f * u, 13.5f * u)
                        }
                        val boxStyle = if (active) Fill else outline
                        val trayColor = if (active) cutoutColor else tint
                        val trayStyle = if (active) thinCutoutStroke else outline
                        onDrawBehind {
                            drawRoundRect(tint, topLeft, boxSize, boxR, style = boxStyle)
                            drawPath(tray, color = trayColor, style = trayStyle)
                        }
                    }

                    MinimalIconType.SHIELD -> {
                        val shield = Path().apply {
                            moveTo(12f * u, 3f * u)
                            lineTo(19.2f * u, 5.8f * u)
                            lineTo(19.2f * u, 11.8f * u)
                            cubicTo(19.2f * u, 16.5f * u, 15.8f * u, 19.8f * u, 12f * u, 21f * u)
                            cubicTo(8.2f * u, 19.8f * u, 4.8f * u, 16.5f * u, 4.8f * u, 11.8f * u)
                            lineTo(4.8f * u, 5.8f * u)
                            close()
                        }
                        val check = Path().apply {
                            moveTo(9f * u, 12f * u)
                            lineTo(11.2f * u, 14.2f * u)
                            lineTo(15.4f * u, 9.8f * u)
                        }
                        onDrawBehind {
                            if (active) {
                                drawPath(shield, color = tint)
                                drawPath(check, color = cutoutColor, style = thinCutoutStroke)
                            } else {
                                drawPath(shield, color = tint, style = outline)
                            }
                        }
                    }

                    MinimalIconType.CLOUD_SYNC -> {
                        val cloud = Path().apply {
                            moveTo(7.5f * u, 18f * u)
                            lineTo(17f * u, 18f * u)
                            cubicTo(19.5f * u, 18f * u, 21f * u, 16.3f * u, 21f * u, 14f * u)
                            cubicTo(21f * u, 12f * u, 19.6f * u, 10.4f * u, 17.6f * u, 10.1f * u)
                            cubicTo(17f * u, 6.8f * u, 14.3f * u, 4.8f * u, 11.3f * u, 5.2f * u)
                            cubicTo(8.5f * u, 5.6f * u, 6.8f * u, 7.8f * u, 6.8f * u, 10.5f * u)
                            cubicTo(4.6f * u, 10.9f * u, 3f * u, 12.6f * u, 3f * u, 14.6f * u)
                            cubicTo(3f * u, 16.6f * u, 4.8f * u, 18f * u, 7.5f * u, 18f * u)
                            close()
                        }
                        onDrawBehind {
                            if (active) drawPath(cloud, color = tint)
                            else drawPath(cloud, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.PLAY_SQUIRCLE -> {
                        val topLeft = Offset(3.5f * u, 4f * u)
                        val boxSize = Size(17f * u, 16f * u)
                        val boxR = CornerRadius(5.5f * u, 5.5f * u)
                        val tri = Path().apply {
                            moveTo(10.2f * u, 9f * u)
                            lineTo(15.6f * u, 12f * u)
                            lineTo(10.2f * u, 15f * u)
                            close()
                        }
                        val style = if (active) Fill else outline
                        val triColor = if (active) cutoutColor else tint
                        onDrawBehind {
                            drawRoundRect(tint, topLeft, boxSize, boxR, style = style)
                            drawPath(tri, color = triColor, style = style)
                        }
                    }

                    MinimalIconType.SEND -> {
                        val plane = Path().apply {
                            moveTo(20.5f * u, 4f * u)
                            lineTo(14.2f * u, 20.5f * u)
                            lineTo(10.8f * u, 13.4f * u)
                            lineTo(3.5f * u, 10f * u)
                            close()
                        }
                        val sStart = Offset(10.8f * u, 13.4f * u)
                        val sEnd = Offset(16.2f * u, 8f * u)
                        val sw = 1.8f * u
                        onDrawBehind {
                            if (active) {
                                drawPath(plane, color = tint)
                                drawLine(cutoutColor, sStart, sEnd, strokeWidth = sw, cap = StrokeCap.Round)
                            } else {
                                drawPath(plane, color = tint, style = outline)
                            }
                        }
                    }

                    MinimalIconType.TRASH -> {
                        val l1Start = Offset(4.5f * u, 7f * u)
                        val l1End = Offset(19.5f * u, 7f * u)
                        val l2Start = Offset(9.5f * u, 4.2f * u)
                        val l2End = Offset(14.5f * u, 4.2f * u)
                        val topLeft = Offset(6.5f * u, 7f * u)
                        val boxSize = Size(11f * u, 13f * u)
                        val boxR = CornerRadius(3f * u, 3f * u)
                        onDrawBehind {
                            drawLine(tint, l1Start, l1End, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawLine(tint, l2Start, l2End, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawRoundRect(tint, topLeft, boxSize, boxR, style = outline)
                        }
                    }

                    MinimalIconType.EDIT -> {
                        val pen = Path().apply {
                            moveTo(15.2f * u, 4.8f * u)
                            lineTo(19.2f * u, 8.8f * u)
                            lineTo(9.2f * u, 18.8f * u)
                            lineTo(4.5f * u, 19.5f * u)
                            lineTo(5.2f * u, 14.8f * u)
                            close()
                        }
                        val style = if (active) Fill else outline
                        onDrawBehind {
                            drawPath(pen, color = tint, style = style)
                        }
                    }

                    MinimalIconType.CHECK -> {
                        val check = Path().apply {
                            moveTo(5.5f * u, 12.5f * u)
                            lineTo(10f * u, 17f * u)
                            lineTo(18.5f * u, 7.5f * u)
                        }
                        onDrawBehind {
                            drawPath(check, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.SHARE -> {
                        val tray = Path().apply {
                            moveTo(5.5f * u, 13.5f * u)
                            lineTo(5.5f * u, 17.5f * u)
                            cubicTo(5.5f * u, 19.2f * u, 6.8f * u, 20.5f * u, 8.5f * u, 20.5f * u)
                            lineTo(15.5f * u, 20.5f * u)
                            cubicTo(17.2f * u, 20.5f * u, 18.5f * u, 19.2f * u, 18.5f * u, 17.5f * u)
                            lineTo(18.5f * u, 13.5f * u)
                        }
                        val sStart = Offset(12f * u, 15.2f * u)
                        val sEnd = Offset(12f * u, 4.5f * u)
                        val arrow = Path().apply {
                            moveTo(8.5f * u, 7.8f * u)
                            lineTo(12f * u, 4.3f * u)
                            lineTo(15.5f * u, 7.8f * u)
                        }
                        onDrawBehind {
                            drawPath(tray, color = tint, style = outline)
                            drawLine(tint, sStart, sEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawPath(arrow, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.DEVICE_SYNC -> {
                        val topArrow = Path().apply {
                            moveTo(6f * u, 8.5f * u)
                            lineTo(18f * u, 8.5f * u)
                            moveTo(15f * u, 5.5f * u)
                            lineTo(18f * u, 8.5f * u)
                            lineTo(15f * u, 11.5f * u)
                        }
                        val bottomArrow = Path().apply {
                            moveTo(18f * u, 15.5f * u)
                            lineTo(6f * u, 15.5f * u)
                            moveTo(9f * u, 12.5f * u)
                            lineTo(6f * u, 15.5f * u)
                            lineTo(9f * u, 18.5f * u)
                        }
                        onDrawBehind {
                            drawPath(topArrow, color = tint, style = outline)
                            drawPath(bottomArrow, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.STAR -> {
                        val star = Path().apply {
                            moveTo(12f * u, 3.6f * u)
                            lineTo(14.6f * u, 8.9f * u)
                            lineTo(20.4f * u, 9.7f * u)
                            lineTo(16.2f * u, 13.8f * u)
                            lineTo(17.2f * u, 19.6f * u)
                            lineTo(12f * u, 16.8f * u)
                            lineTo(6.8f * u, 19.6f * u)
                            lineTo(7.8f * u, 13.8f * u)
                            lineTo(3.6f * u, 9.7f * u)
                            lineTo(9.4f * u, 8.9f * u)
                            close()
                        }
                        val style = if (active) Fill else outline
                        onDrawBehind {
                            drawPath(star, color = tint, style = style)
                        }
                    }

                    MinimalIconType.GRID -> {
                        val style = if (active) Fill else outline
                        val r = CornerRadius(2.6f * u, 2.6f * u)
                        val sz = Size(6.5f * u, 6.5f * u)
                        val tl1 = Offset(4.2f * u, 4.2f * u)
                        val tl2 = Offset(13.3f * u, 4.2f * u)
                        val tl3 = Offset(4.2f * u, 13.3f * u)
                        val tl4 = Offset(13.3f * u, 13.3f * u)
                        onDrawBehind {
                            drawRoundRect(tint, tl1, sz, r, style = style)
                            drawRoundRect(tint, tl2, sz, r, style = style)
                            drawRoundRect(tint, tl3, sz, r, style = style)
                            drawRoundRect(tint, tl4, sz, r, style = style)
                        }
                    }

                    MinimalIconType.SUN -> {
                        val style = if (active) Fill else outline
                        val center = Offset(12f * u, 12f * u)
                        val r = 4.2f * u
                        val rayStarts = Array(8) { idx ->
                            val rad = sunRayAngles[idx]
                            val c = cos(rad).toFloat()
                            val s = sin(rad).toFloat()
                            Offset(12f * u + c * 6.8f * u, 12f * u + s * 6.8f * u)
                        }
                        val rayEnds = Array(8) { idx ->
                            val rad = sunRayAngles[idx]
                            val c = cos(rad).toFloat()
                            val s = sin(rad).toFloat()
                            Offset(12f * u + c * 9f * u, 12f * u + s * 9f * u)
                        }
                        onDrawBehind {
                            drawCircle(tint, radius = r, center = center, style = style)
                            for (i in 0 until 8) {
                                drawLine(tint, rayStarts[i], rayEnds[i], strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            }
                        }
                    }

                    MinimalIconType.DIM_SUN -> {
                        val center = Offset(12f * u, 12f * u)
                        val r = 4.5f * u
                        val arcTL = Offset(7.5f * u, 7.5f * u)
                        val arcSz = Size(9f * u, 9f * u)
                        onDrawBehind {
                            drawCircle(tint, radius = r, center = center, style = outline)
                            drawArc(tint, startAngle = -90f, sweepAngle = 180f, useCenter = true, topLeft = arcTL, size = arcSz)
                        }
                    }

                    MinimalIconType.MOON -> {
                        val crescent = Path().apply {
                            moveTo(15.2f * u, 4.5f * u)
                            cubicTo(10.8f * u, 5.1f * u, 7.5f * u, 9f * u, 7.9f * u, 13.5f * u)
                            cubicTo(8.3f * u, 17.8f * u, 12.1f * u, 20.6f * u, 16.5f * u, 19.8f * u)
                            cubicTo(17.9f * u, 19.5f * u, 19.1f * u, 18.7f * u, 19.9f * u, 17.6f * u)
                            cubicTo(15.9f * u, 18.2f * u, 12.4f * u, 15.2f * u, 12.6f * u, 11.2f * u)
                            cubicTo(12.8f * u, 8.2f * u, 14.3f * u, 5.9f * u, 15.2f * u, 4.5f * u)
                            close()
                        }
                        val style = if (active) Fill else outline
                        onDrawBehind {
                            drawPath(crescent, color = tint, style = style)
                        }
                    }

                    MinimalIconType.SLIDERS -> {
                        val x1 = 7f * u
                        val x2 = 12f * u
                        val x3 = 17f * u
                        val topY = 4.5f * u
                        val botY = 19.5f * u
                        val knobW = strokeWidth * 1.15f
                        val l1s = Offset(x1, topY); val l1e = Offset(x1, botY)
                        val l2s = Offset(x2, topY); val l2e = Offset(x2, botY)
                        val l3s = Offset(x3, topY); val l3e = Offset(x3, botY)
                        val k1s = Offset(5.2f * u, 9f * u); val k1e = Offset(8.8f * u, 9f * u)
                        val k2s = Offset(10.2f * u, 15f * u); val k2e = Offset(13.8f * u, 15f * u)
                        val k3s = Offset(15.2f * u, 10.5f * u); val k3e = Offset(18.8f * u, 10.5f * u)
                        onDrawBehind {
                            drawLine(tint, l1s, l1e, strokeWidth, StrokeCap.Round)
                            drawLine(tint, l2s, l2e, strokeWidth, StrokeCap.Round)
                            drawLine(tint, l3s, l3e, strokeWidth, StrokeCap.Round)
                            drawLine(tint, k1s, k1e, knobW, StrokeCap.Round)
                            drawLine(tint, k2s, k2e, knobW, StrokeCap.Round)
                            drawLine(tint, k3s, k3e, knobW, StrokeCap.Round)
                        }
                    }

                    MinimalIconType.MIC -> {
                        val micTL = Offset(9f * u, 3.5f * u)
                        val micSz = Size(6f * u, 10.5f * u)
                        val micR = CornerRadius(3f * u, 3f * u)
                        val micStyle = if (active) Fill else outline
                        val arc = Path().apply {
                            moveTo(5.8f * u, 11.5f * u)
                            cubicTo(5.8f * u, 15.2f * u, 8.4f * u, 17.5f * u, 12f * u, 17.5f * u)
                            cubicTo(15.6f * u, 17.5f * u, 18.2f * u, 15.2f * u, 18.2f * u, 11.5f * u)
                        }
                        val s1s = Offset(12f * u, 17.5f * u); val s1e = Offset(12f * u, 20.5f * u)
                        val bStart = Offset(9f * u, 20.5f * u); val bEnd = Offset(15f * u, 20.5f * u)
                        onDrawBehind {
                            drawRoundRect(tint, micTL, micSz, micR, style = micStyle)
                            drawPath(arc, color = tint, style = outline)
                            drawLine(tint, s1s, s1e, strokeWidth, StrokeCap.Round)
                            drawLine(tint, bStart, bEnd, strokeWidth, StrokeCap.Round)
                        }
                    }

                    MinimalIconType.LOCK -> {
                        val shackle = Path().apply {
                            moveTo(8f * u, 10.2f * u)
                            lineTo(8f * u, 7.8f * u)
                            cubicTo(8f * u, 5.2f * u, 9.8f * u, 3.8f * u, 12f * u, 3.8f * u)
                            cubicTo(14.2f * u, 3.8f * u, 16f * u, 5.2f * u, 16f * u, 7.8f * u)
                            lineTo(16f * u, 10.2f * u)
                        }
                        val lockTL = Offset(5.2f * u, 10.2f * u)
                        val lockSz = Size(13.6f * u, 10.3f * u)
                        val lockR = CornerRadius(3.5f * u, 3.5f * u)
                        val lockStyle = if (active) Fill else outline
                        val keyholeCenter = Offset(12f * u, 15.3f * u)
                        val keyholeR = 1.6f * u
                        onDrawBehind {
                            drawPath(shackle, color = tint, style = outline)
                            drawRoundRect(tint, lockTL, lockSz, lockR, style = lockStyle)
                            if (active) {
                                drawCircle(color = cutoutColor, radius = keyholeR, center = keyholeCenter)
                            }
                        }
                    }

                    MinimalIconType.ARROW_UP_RIGHT -> {
                        val lStart = Offset(6.5f * u, 17.5f * u)
                        val lEnd = Offset(17.5f * u, 6.5f * u)
                        val head = Path().apply {
                            moveTo(9.5f * u, 6.5f * u)
                            lineTo(17.5f * u, 6.5f * u)
                            lineTo(17.5f * u, 14.5f * u)
                        }
                        onDrawBehind {
                            drawLine(tint, lStart, lEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawPath(head, color = tint, style = outline)
                        }
                    }

                    MinimalIconType.NOTEBOOK -> {
                        val docTL = Offset(4.5f * u, 3.5f * u)
                        val docSz = Size(15f * u, 17f * u)
                        val docR = CornerRadius(4f * u, 4f * u)
                        val l1s = Offset(8.2f * u, 9f * u); val l1e = Offset(15.8f * u, 9f * u)
                        val l2s = Offset(8.2f * u, 13f * u); val l2e = Offset(15.8f * u, 13f * u)
                        val l3s = Offset(8.2f * u, 16.8f * u); val l3e = Offset(13f * u, 16.8f * u)
                        val style = if (active) Fill else outline
                        val lineTint = if (active) cutoutColor else tint
                        onDrawBehind {
                            drawRoundRect(tint, docTL, docSz, docR, style = style)
                            drawLine(lineTint, l1s, l1e, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawLine(lineTint, l2s, l2e, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawLine(lineTint, l3s, l3e, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        }
                    }

                    MinimalIconType.TIMER -> {
                        val center = Offset(12f * u, 13.2f * u)
                        val radius = 7.4f * u
                        val topStart = Offset(9.5f * u, 3.2f * u); val topEnd = Offset(14.5f * u, 3.2f * u)
                        val handEnd = Offset(12f * u, 9.2f * u)
                        val hand2End = Offset(15.2f * u, 13.2f * u)
                        val style = if (active) Fill else outline
                        val handTint = if (active) cutoutColor else tint
                        onDrawBehind {
                            drawLine(tint, topStart, topEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawCircle(color = tint, radius = radius, center = center, style = style)
                            drawLine(handTint, center, handEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawLine(handTint, center, hand2End, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        }
                    }
                }
            }
    )
}
