package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LiquidTheme

/**
 * Unified Personal AI Library <AppLogo /> component.
 * Renders the official full-bleed neural-brain & bookmark emblem (`ic_official_brand_logo`)
 * inside a clipped rounded squircle with subtle specular Liquid Glass rim and crimson ambient glow.
 */
@Composable
fun AppLogo(
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier,
    showGlassContainer: Boolean = true,
    testTag: String = "app_logo",
    contentDescriptionText: String = "Personal AI Library logo"
) {
    val palette = LiquidTheme.palette
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val backingColor = remember { Color(0xFF0D0D0F) }
    val glowBrush = remember(palette.accentCrimson, palette.isDark) {
        Brush.radialGradient(
            colors = listOf(
                palette.accentCrimson.copy(alpha = if (palette.isDark) 0.28f else 0.16f),
                backingColor
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDescriptionText }
            .testTag(testTag)
            .then(
                if (showGlassContainer) {
                    Modifier
                        .shadow(
                            elevation = if (palette.isDark) 14.dp else 8.dp,
                            shape = shape,
                            ambientColor = palette.accentCrimson.copy(alpha = 0.36f),
                            spotColor = palette.accentCrimson.copy(alpha = 0.38f)
                        )
                        .clip(shape)
                        .background(glowBrush)
                } else {
                    Modifier.clip(shape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_official_brand_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (showGlassContainer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.9.dp, palette.specularRimOutline, shape)
            )
        }
    }
}

/**
 * Backward-compatible alias delegating directly to [AppLogo].
 */
@Composable
fun BrandNeuralBookmarkLogo(
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier,
    showGlassContainer: Boolean = true,
    contentDescriptionText: String = "Personal AI Library logo"
) {
    AppLogo(
        size = size,
        cornerRadius = cornerRadius,
        modifier = modifier,
        showGlassContainer = showGlassContainer,
        testTag = "brand_neural_bookmark_logo",
        contentDescriptionText = contentDescriptionText
    )
}
