package com.yunmei.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixThemePaletteStyle

/**
 * 官方 Miuix 皮肤：`MiuixTheme` + `ThemeController`。
 *
 * 逻辑移植 KernelSU `MiuixKernelSUTheme.kt`，并按模板优先准则简化为「莫奈开关」二元语义：
 * - 莫奈开 → `ColorSchemeMode.MonetSystem/MonetLight/MonetDark`，keyColor = 系统墙纸动态色
 *   primary（KernelSU `resolvedKeyColor` 的 Monet 分支）→ 动态取色；
 * - 莫奈关 → `ColorSchemeMode.System/Light/Dark`，keyColor = null → MiuixTheme 原色。
 *
 * 为了让既有页面里基于 `androidx.compose.material3.MaterialTheme.*` 的控件在 Miuix 皮肤下也拿到
 * Miuix 配色，这里额外提供一个 [MaterialTheme] 桥接层：把 Miuix [Colors] 的关键角色映射到
 * androidx [ColorScheme]。Miuix 原生组件（NavigationBar/Card 等）直接读 `MiuixTheme.colorScheme`，
 * 二者互不影响。
 */
@Composable
fun YunMeiMiuixTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeState.darkMode) {
        ThemeDarkMode.LIGHT -> false
        ThemeDarkMode.DARK -> true
        ThemeDarkMode.SYSTEM -> isSystemInDarkTheme()
    }
    val useMonet = themeState.dynamicColor

    // KernelSU MiuixKernelSUTheme 的 resolvedKeyColor：开莫奈 → 系统墙纸动态色 primary；否则原色。
    val resolvedKeyColor: Color? = if (useMonet) {
        if (darkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        null
    }

    val controller = ThemeController(
        when (themeState.darkMode) {
            ThemeDarkMode.LIGHT -> if (useMonet) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
            ThemeDarkMode.DARK -> if (useMonet) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
            ThemeDarkMode.SYSTEM -> if (useMonet) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        },
        keyColor = resolvedKeyColor,
        colorSpec = MiuixThemeColorSpec.Spec2021,
        paletteStyle = MiuixThemePaletteStyle.TonalSpot,
        isDark = darkTheme,
    )

    MiuixTheme(controller = controller) {
        val view = LocalView.current
        LaunchedEffect(darkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }

        MaterialTheme(
            colorScheme = miuixColorsToMaterial3(darkTheme),
            typography = MiuiXTypography,
            shapes = MiuiXShapes,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
                LocalDarkTheme provides darkTheme,
                content = content,
            )
        }
    }
}

/** 把官方 Miuix [Colors] 的关键角色映射为 androidx Material3 [ColorScheme]（桥接层）。 */
@Composable
fun miuixColorsToMaterial3(isDark: Boolean): ColorScheme {
    val c: Colors = MiuixTheme.colorScheme
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.primary,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        background = c.background,
        onBackground = c.onBackground,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariantSummary,
        outline = c.outline,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
    )
}
