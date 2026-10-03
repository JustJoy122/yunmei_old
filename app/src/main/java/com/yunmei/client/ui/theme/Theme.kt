package com.yunmei.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yunmei.client.data.preferences.ThemeMode

/**
 * 统一主题入口（双皮肤分派）。
 *
 * - [ThemeMode.MIUIX]：官方 Miuix 皮肤 → [YunMeiMiuixTheme]（`MiuixTheme` + `ThemeController`，
 *   莫奈开关决定「MiuixTheme 原色 / 动态取色」，逻辑移植 KernelSU `MiuixKernelSUTheme.kt`）。
 * - [ThemeMode.MATERIAL3]：Material 皮肤 → [YunMeiMaterialTheme]（material-kolor 取色 +
 *   `MaterialExpressiveTheme` + `MotionScheme.expressive()`）。Material 分支完全走模板
 *   Monet 动态取色：`seedColor == 0` 时跟随壁纸，否则用预设种子（KernelSU keyColor 语义）。
 */
@Composable
fun YunMeiTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit,
) {
    when (themeState.themeMode) {
        ThemeMode.MIUIX -> YunMeiMiuixTheme(themeState, content)
        ThemeMode.MATERIAL3 -> YunMeiMaterialTheme(themeState, content)
    }
}

@Composable
fun YunMeiMaterialTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeState.darkMode) {
        ThemeDarkMode.LIGHT -> false
        ThemeDarkMode.DARK -> true
        ThemeDarkMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = rememberYunMeiColorScheme(
        seedColor = if (themeState.seedColor == DEFAULT_SEED_COLOR) {
            Color.Unspecified
        } else {
            Color(themeState.seedColor)
        },
        isDark = darkTheme,
        paletteStyle = themeState.paletteStyle.toMaterialKolor(),
        colorSpec = themeState.colorSpec.toSpecVersion(),
    ).animateAsState()

    val view = LocalView.current
    LaunchedEffect(darkTheme) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = {
            CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
                content()
            }
        },
    )
}
