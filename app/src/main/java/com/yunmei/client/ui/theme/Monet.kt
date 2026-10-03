package com.yunmei.client.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * Monet / Material You 取色引擎封装（`com.materialkolor:material-kolor`，HCT）。
 *
 * 逐行移植 KernelSU `ThemeExt.kt`（`rememberKernelSUColorScheme` / `animateAsState`）：
 * - 用 [rememberDynamicColorScheme] 从种子色生成整套 Material3 色板（含 SPEC_2025 与
 *   30+ 角色），种子来源 `seedColor == Color.Unspecified` 时取系统墙纸动态色的 primary；
 * - 用 [ColorScheme.animateAsState] 逐角色 spring 过渡，避免切主题闪变。
 */

fun ThemePaletteStyle.toMaterialKolor(): MaterialKolorPaletteStyle = when (this) {
    ThemePaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
    ThemePaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
    ThemePaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
    ThemePaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
    ThemePaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
    ThemePaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
    ThemePaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
    ThemePaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
    ThemePaletteStyle.Content -> MaterialKolorPaletteStyle.Content
}

fun ThemeColorSpec.toSpecVersion(): ColorSpec.SpecVersion = when (this) {
    ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    ThemeColorSpec.SPEC_2025 -> ColorSpec.SpecVersion.SPEC_2025
}

fun ColorSpec.SpecVersion.effectiveFor(style: MaterialKolorPaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !styleSupportsSpec2025(style)) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

private fun styleSupportsSpec2025(style: MaterialKolorPaletteStyle): Boolean =
    style == MaterialKolorPaletteStyle.TonalSpot ||
        style == MaterialKolorPaletteStyle.Neutral ||
        style == MaterialKolorPaletteStyle.Vibrant ||
        style == MaterialKolorPaletteStyle.Expressive

@Composable
fun rememberYunMeiColorScheme(
    seedColor: Color,
    isDark: Boolean,
    paletteStyle: MaterialKolorPaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): ColorScheme {
    val context = LocalContext.current
    val seed = if (seedColor == Color.Unspecified) {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        seedColor
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    )
}

/** 逐角色 spring 过渡整个 ColorScheme（含 Material 3 1.4.0 新增的 fixed container 角色）。 */
@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation",
    ).value

    return ColorScheme(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),
        primaryFixed = animateColor(primaryFixed),
        primaryFixedDim = animateColor(primaryFixedDim),
        onPrimaryFixed = animateColor(onPrimaryFixed),
        onPrimaryFixedVariant = animateColor(onPrimaryFixedVariant),
        secondaryFixed = animateColor(secondaryFixed),
        secondaryFixedDim = animateColor(secondaryFixedDim),
        onSecondaryFixed = animateColor(onSecondaryFixed),
        onSecondaryFixedVariant = animateColor(onSecondaryFixedVariant),
        tertiaryFixed = animateColor(tertiaryFixed),
        tertiaryFixedDim = animateColor(tertiaryFixedDim),
        onTertiaryFixed = animateColor(onTertiaryFixed),
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant),
    )
}
