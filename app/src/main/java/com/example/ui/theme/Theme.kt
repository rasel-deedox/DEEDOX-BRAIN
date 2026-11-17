package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val LocalLiquidPalette = compositionLocalOf { DarkObsidianLiquidPalette }

object LiquidTheme {
    val palette: LiquidThemePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalLiquidPalette.current
}

@Composable
fun PersonalAiLibraryTheme(
    themeMode: AppThemeMode = AppThemeMode.NIGHT,
    accentPreset: LiquidAccentPreset = LiquidAccentPreset.CRIMSON_BRONZE,
    content: @Composable () -> Unit
) {
    val targetPalette = buildLiquidPalette(themeMode, accentPreset)

    val animSpec = remember { tween<Color>(durationMillis = 180) }
    val canvasTop by animateColorAsState(targetPalette.canvasTop, animSpec, label = "canvasTop")
    val canvasBottom by animateColorAsState(targetPalette.canvasBottom, animSpec, label = "canvasBottom")
    val cardTop by animateColorAsState(targetPalette.cardSurfaceTop, animSpec, label = "cardTop")
    val cardBottom by animateColorAsState(targetPalette.cardSurfaceBottom, animSpec, label = "cardBottom")
    val primaryInk by animateColorAsState(targetPalette.primaryInk, animSpec, label = "primaryInk")
    val secondaryInk by animateColorAsState(targetPalette.secondaryInk, animSpec, label = "secondaryInk")
    val dockBg by animateColorAsState(targetPalette.dockBg, animSpec, label = "dockBg")
    val accentColor by animateColorAsState(targetPalette.accentCrimson, animSpec, label = "accentColor")
    val accentTop by animateColorAsState(targetPalette.accentTerracottaTop, animSpec, label = "accentTop")
    val accentBottom by animateColorAsState(targetPalette.accentTerracottaBottom, animSpec, label = "accentBottom")

    val animatedPalette = remember(
        targetPalette,
        canvasTop,
        canvasBottom,
        cardTop,
        cardBottom,
        primaryInk,
        secondaryInk,
        dockBg,
        accentColor,
        accentTop,
        accentBottom
    ) {
        if (canvasTop == targetPalette.canvasTop &&
            canvasBottom == targetPalette.canvasBottom &&
            cardTop == targetPalette.cardSurfaceTop &&
            primaryInk == targetPalette.primaryInk &&
            accentColor == targetPalette.accentCrimson
        ) {
            targetPalette
        } else {
            targetPalette.copy(
                canvasTop = canvasTop,
                canvasBottom = canvasBottom,
                cardSurfaceTop = cardTop,
                cardSurfaceBottom = cardBottom,
                primaryInk = primaryInk,
                secondaryInk = secondaryInk,
                dockBg = dockBg,
                accentCrimson = accentColor,
                accentTerracottaTop = accentTop,
                accentTerracottaBottom = accentBottom
            )
        }
    }

    val m3Scheme = remember(targetPalette) {
        if (targetPalette.isDark) {
            darkColorScheme(
                primary = SoftCreamWhite,
                onPrimary = SoftCreamInk,
                primaryContainer = targetPalette.accentSoftBg,
                onPrimaryContainer = targetPalette.accentSoftInk,
                secondary = targetPalette.accentCrimson,
                onSecondary = Color.White,
                secondaryContainer = targetPalette.secondaryPillBg,
                onSecondaryContainer = targetPalette.primaryInk,
                tertiary = targetPalette.accentTerracottaTop,
                onTertiary = Color.White,
                background = targetPalette.canvasTop,
                onBackground = targetPalette.primaryInk,
                surface = targetPalette.cardSurfaceTop,
                onSurface = targetPalette.primaryInk,
                surfaceVariant = targetPalette.secondaryPillBg,
                onSurfaceVariant = targetPalette.secondaryInk,
                outline = targetPalette.inputBorder,
                error = CrimsonAccent,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = DeepCharcoalInk,
                onPrimary = PearlCreamBottom,
                primaryContainer = targetPalette.accentSoftBg,
                onPrimaryContainer = targetPalette.accentSoftInk,
                secondary = targetPalette.accentCrimson,
                onSecondary = Color.White,
                secondaryContainer = targetPalette.secondaryPillBg,
                onSecondaryContainer = DeepCharcoalInk,
                tertiary = targetPalette.accentTerracottaBottom,
                onTertiary = Color.White,
                background = targetPalette.canvasTop,
                onBackground = DeepCharcoalInk,
                surface = targetPalette.cardSurfaceTop,
                onSurface = DeepCharcoalInk,
                surfaceVariant = targetPalette.secondaryPillBg,
                onSurfaceVariant = targetPalette.secondaryInk,
                outline = targetPalette.inputBorder,
                error = CrimsonAccent,
                onError = Color.White
            )
        }
    }

    CompositionLocalProvider(LocalLiquidPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = AppTypography,
            content = content
        )
    }
}
