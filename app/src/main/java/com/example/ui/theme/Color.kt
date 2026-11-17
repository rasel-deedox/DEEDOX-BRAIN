package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val code: String, val label: String) {
    LIGHT("LIGHT", "Light"),
    DIM("DIM", "Dim"),
    NIGHT("NIGHT", "Dark");

    companion object {
        fun fromCode(code: String?): AppThemeMode = when {
            code.equals("LIGHT", ignoreCase = true) -> LIGHT
            code.equals("DIM", ignoreCase = true) -> DIM
            code.equals("DARK", ignoreCase = true) || code.equals("NIGHT", ignoreCase = true) -> NIGHT
            else -> NIGHT
        }
    }
}

enum class LiquidAccentPreset(
    val code: String,
    val title: String,
    val subtitle: String,
    val primaryAccent: Color,
    val gradientTop: Color,
    val gradientBottom: Color,
    val darkSoftInk: Color,
    val lightSoftInk: Color,
    val darkBronzeTint: Color,
    val lightBronzeTint: Color
) {
    CRIMSON_BRONZE(
        code = "CRIMSON_BRONZE",
        title = "Liquid Crimson-Bronze",
        subtitle = "Signature Apple Liquid Glass & smoked crimson",
        primaryAccent = Color(0xFFE53935),
        gradientTop = Color(0xFFFF6B4A),
        gradientBottom = Color(0xFFD93829),
        darkSoftInk = Color(0xFFFF8A75),
        lightSoftInk = Color(0xFFC62828),
        darkBronzeTint = Color(0x8C603A36),
        lightBronzeTint = Color(0xEBF5ECE4)
    ),
    WARM_TERRACOTTA(
        code = "WARM_TERRACOTTA",
        title = "Sunset Terracotta",
        subtitle = "Smoked ember terracotta & warm pearl refraction",
        primaryAccent = Color(0xFFD84315),
        gradientTop = Color(0xFFFF7D54),
        gradientBottom = Color(0xFFBF360C),
        darkSoftInk = Color(0xFFFF9E7D),
        lightSoftInk = Color(0xFFB7360E),
        darkBronzeTint = Color(0x8C5E352B),
        lightBronzeTint = Color(0xEBF7ECE5)
    ),
    CHAMPAGNE_BRONZE(
        code = "CHAMPAGNE_BRONZE",
        title = "Champagne Bronze",
        subtitle = "Minimal warm brushed bronze & ivory glass",
        primaryAccent = Color(0xFFB88655),
        gradientTop = Color(0xFFD4A373),
        gradientBottom = Color(0xFF8D5B34),
        darkSoftInk = Color(0xFFE5BE96),
        lightSoftInk = Color(0xFF7D4F2A),
        darkBronzeTint = Color(0x8C524234),
        lightBronzeTint = Color(0xEBF5EFE6)
    ),
    BURNT_SIENNA(
        code = "BURNT_SIENNA",
        title = "Obsidian Sienna",
        subtitle = "Deep architectural sienna & matte cream contrast",
        primaryAccent = Color(0xFFC64D2D),
        gradientTop = Color(0xFFE06A4B),
        gradientBottom = Color(0xFF99341B),
        darkSoftInk = Color(0xFFF29A82),
        lightSoftInk = Color(0xFF9E3219),
        darkBronzeTint = Color(0x8C5A332C),
        lightBronzeTint = Color(0xEBF4EAE4)
    );

    companion object {
        fun fromCode(code: String?): LiquidAccentPreset =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: CRIMSON_BRONZE
    }
}

// Unified Minimal Luxury Palette: Warm Cream & Obsidian Crimson-Bronze Liquid Glass
val ObsidianCanvas = Color(0xFF0A0A0C)
val ObsidianCanvasDeep = Color(0xFF060608)
val DimBronzeCanvas = Color(0xFF141214)
val DimBronzeCanvasDeep = Color(0xFF0E0C0F)
val PearlCreamTop = Color(0xFFEFECE6)
val PearlCreamBottom = Color(0xFFF7F5F0)

val SoftCreamWhite = Color(0xFFEDEAE4)
val SoftCreamInk = Color(0xFF0D0D0F)
val DeepCharcoalInk = Color(0xFF181715)
val ObsidianActionBg = Color(0xFF1C1B1A)

val CrimsonAccent = Color(0xFFE53935)
val TerracottaTop = Color(0xFFFF6B4A)
val TerracottaBottom = Color(0xFFD93829)

// Featured Warm Crimson-Bronze Gradient Tokens (Dark, Dim & Light)
val DarkBronzeCardTop = Color(0x8C603A36)
val DarkBronzeCardMid = Color(0xB32C1E1E)
val DarkBronzeCardBottom = Color(0xDC141214)
val DarkBronzeBorder = Color(0x24FFC8BE)

val LightBronzeCardTop = Color(0xEBF5ECE4)
val LightBronzeCardMid = Color(0xE8EDE0D4)
val LightBronzeCardBottom = Color(0xF5FBF8F3)
val LightBronzeBorder = Color(0xE6FFFFFF)

// Micro telemetry status indicator dots (only for tiny 6-8dp telemetry indicators)
val TelemetryCrimsonDot = Color(0xFFE53935)
val TelemetryOptimalDot = Color(0xFF10B981)
val TelemetryModerateDot = Color(0xFFF59E0B)

@Immutable
data class LiquidThemePalette(
    val mode: AppThemeMode,
    val accentPreset: LiquidAccentPreset,
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val dotMatrixColor: Color,
    val orbCrimson: Color,
    val orbSienna: Color,
    val orbBronze: Color,
    val prismaticSheen: Color,
    val cardSurfaceTop: Color,
    val cardSurfaceBottom: Color,
    val bronzeCardTop: Color,
    val bronzeCardMid: Color,
    val bronzeCardBottom: Color,
    val bronzeCardBorder: Color,
    val frostedPillBg: Color,
    val dockBg: Color,
    val dockTileInactiveBg: Color,
    val dockTileActiveBg: Color,
    val dockTileActiveContent: Color,
    val recessedTrayBg: Color,
    val recessedDashedBorder: Color,
    val specularBorderTop: Color,
    val specularRimOutline: Color,
    val primaryInk: Color,
    val secondaryInk: Color,
    val mutedIcon: Color,
    val primaryButtonBg: Color,
    val primaryButtonContent: Color,
    val secondaryPillBg: Color,
    val accentCrimson: Color,
    val accentTerracottaTop: Color,
    val accentTerracottaBottom: Color,
    val accentSoftBg: Color,
    val accentSoftInk: Color,
    val inputBorder: Color,
    val barTrack: Color,
    val heatmapLevels: List<Color>,
    val folderPocketFrontTop: Color,
    val folderPocketFrontBottom: Color
)

val DarkObsidianLiquidPalette = LiquidThemePalette(
    mode = AppThemeMode.NIGHT,
    accentPreset = LiquidAccentPreset.CRIMSON_BRONZE,
    isDark = true,
    canvasTop = ObsidianCanvas,
    canvasBottom = ObsidianCanvasDeep,
    dotMatrixColor = Color(0x10FFFFFF),
    orbCrimson = Color(0x2AD93829),
    orbSienna = Color(0x24B84B2A),
    orbBronze = Color(0x385C3630),
    prismaticSheen = Color(0x14FFC8BE),
    cardSurfaceTop = Color(0xB8161416),
    cardSurfaceBottom = Color(0xCC121012),
    bronzeCardTop = DarkBronzeCardTop,
    bronzeCardMid = DarkBronzeCardMid,
    bronzeCardBottom = DarkBronzeCardBottom,
    bronzeCardBorder = DarkBronzeBorder,
    frostedPillBg = Color(0xBF1E1B1C),
    dockBg = Color(0xC7141416),
    dockTileInactiveBg = Color(0x0DFFFFFF),
    dockTileActiveBg = SoftCreamWhite,
    dockTileActiveContent = SoftCreamInk,
    recessedTrayBg = Color(0x8C121012),
    recessedDashedBorder = Color(0x1FFFFFFF),
    specularBorderTop = Color(0x24FFFFFF),
    specularRimOutline = Color(0x16FFFFFF),
    primaryInk = Color(0xFFF2EFE9),
    secondaryInk = Color(0xFF9E9893),
    mutedIcon = Color(0xFF726C68),
    primaryButtonBg = SoftCreamWhite,
    primaryButtonContent = SoftCreamInk,
    secondaryPillBg = Color(0xFF1A1718),
    accentCrimson = CrimsonAccent,
    accentTerracottaTop = TerracottaTop,
    accentTerracottaBottom = TerracottaBottom,
    accentSoftBg = Color(0x28E53935),
    accentSoftInk = Color(0xFFFF8A75),
    inputBorder = Color(0x22FFFFFF),
    barTrack = Color(0xFF231E1E),
    heatmapLevels = listOf(
        Color(0xFF1B1819),
        Color(0xFF4A2522),
        Color(0xFF8C362D),
        Color(0xFFE53935),
        SoftCreamWhite
    ),
    folderPocketFrontTop = Color(0xE02A2222),
    folderPocketFrontBottom = Color(0xF5151314)
)

val DimBronzeLiquidPalette = DarkObsidianLiquidPalette.copy(
    mode = AppThemeMode.DIM,
    canvasTop = DimBronzeCanvas,
    canvasBottom = DimBronzeCanvasDeep,
    dotMatrixColor = Color(0x12FFF5EB),
    orbCrimson = Color(0x30D93829),
    orbSienna = Color(0x2EC65D3B),
    orbBronze = Color(0x47683E36),
    cardSurfaceTop = Color(0xB82A1C1B),
    cardSurfaceBottom = Color(0xCC1B1617),
    frostedPillBg = Color(0xC7281F1F),
    dockBg = Color(0xD11F1A1B),
    recessedTrayBg = Color(0x9420191A),
    secondaryPillBg = Color(0xFF241C1C),
    barTrack = Color(0xFF2D2322)
)

val LightPearlLiquidPalette = LiquidThemePalette(
    mode = AppThemeMode.LIGHT,
    accentPreset = LiquidAccentPreset.CRIMSON_BRONZE,
    isDark = false,
    canvasTop = PearlCreamTop,
    canvasBottom = PearlCreamBottom,
    dotMatrixColor = Color(0x0A181715),
    orbCrimson = Color(0x18E53935),
    orbSienna = Color(0x5CD7C3AA),
    orbBronze = Color(0x6BE7DDD0),
    prismaticSheen = Color(0x52FFFFFF),
    cardSurfaceTop = Color(0xAEFFFFFF),
    cardSurfaceBottom = Color(0xD4F8F4EC),
    bronzeCardTop = LightBronzeCardTop,
    bronzeCardMid = LightBronzeCardMid,
    bronzeCardBottom = LightBronzeCardBottom,
    bronzeCardBorder = LightBronzeBorder,
    frostedPillBg = Color(0xD4FFFFFF),
    dockBg = Color(0xCCFFFFFF),
    dockTileInactiveBg = Color(0x0D181715),
    dockTileActiveBg = DeepCharcoalInk,
    dockTileActiveContent = PearlCreamBottom,
    recessedTrayBg = Color(0x8FE6E1D8),
    recessedDashedBorder = Color(0xFFC9C2B6),
    specularBorderTop = Color(0xF2FFFFFF),
    specularRimOutline = Color(0x18181715),
    primaryInk = DeepCharcoalInk,
    secondaryInk = Color(0xFF78736A),
    mutedIcon = Color(0xFF8F897F),
    primaryButtonBg = ObsidianActionBg,
    primaryButtonContent = PearlCreamBottom,
    secondaryPillBg = Color(0xFFF6F3ED),
    accentCrimson = CrimsonAccent,
    accentTerracottaTop = TerracottaTop,
    accentTerracottaBottom = TerracottaBottom,
    accentSoftBg = Color(0x1EE53935),
    accentSoftInk = Color(0xFFC62828),
    inputBorder = Color(0xFFDCD6CC),
    barTrack = Color(0xFFE5E0D7),
    heatmapLevels = listOf(
        Color(0xFFE7E2D9),
        Color(0xFFCBBFB2),
        Color(0xFF8E857B),
        Color(0xFFD94A38),
        DeepCharcoalInk
    ),
    folderPocketFrontTop = Color(0xEBFFFFFF),
    folderPocketFrontBottom = Color(0xF5EFEAE1)
)

private val PaletteCache: Map<Pair<AppThemeMode, LiquidAccentPreset>, LiquidThemePalette> = buildMap {
    AppThemeMode.entries.forEach { mode ->
        LiquidAccentPreset.entries.forEach { accent ->
            put(mode to accent, computeLiquidPalette(mode, accent))
        }
    }
}

fun buildLiquidPalette(
    mode: AppThemeMode,
    accent: LiquidAccentPreset
): LiquidThemePalette = PaletteCache[mode to accent] ?: computeLiquidPalette(mode, accent)

private fun computeLiquidPalette(
    mode: AppThemeMode,
    accent: LiquidAccentPreset
): LiquidThemePalette {
    val base = when (mode) {
        AppThemeMode.LIGHT -> LightPearlLiquidPalette
        AppThemeMode.DIM -> DimBronzeLiquidPalette
        AppThemeMode.NIGHT -> DarkObsidianLiquidPalette
    }
    val softBg = if (base.isDark) accent.primaryAccent.copy(alpha = 0.16f) else accent.primaryAccent.copy(alpha = 0.12f)
    val softInk = if (base.isDark) accent.darkSoftInk else accent.lightSoftInk
    return base.copy(
        accentPreset = accent,
        accentCrimson = accent.primaryAccent,
        accentTerracottaTop = accent.gradientTop,
        accentTerracottaBottom = accent.gradientBottom,
        accentSoftBg = softBg,
        accentSoftInk = softInk,
        bronzeCardTop = if (base.isDark) accent.darkBronzeTint else accent.lightBronzeTint,
        orbCrimson = accent.primaryAccent.copy(alpha = if (base.isDark) 0.18f else 0.10f),
        heatmapLevels = if (base.isDark) {
            listOf(
                Color(0xFF1B1819),
                accent.gradientBottom.copy(alpha = 0.35f),
                accent.gradientBottom.copy(alpha = 0.68f),
                accent.primaryAccent,
                SoftCreamWhite
            )
        } else {
            listOf(
                Color(0xFFE7E2D9),
                Color(0xFFCBBFB2),
                Color(0xFF8E857B),
                accent.primaryAccent,
                DeepCharcoalInk
            )
        }
    )
}
