package com.yunmei.client.ui.theme

import com.yunmei.client.data.preferences.ThemeMode

/**
 * 统一主题框架的状态模型（蓝本：InstallerX `ThemeState` / KernelSU `AppSettings`）。
 *
 * - 风格：[themeMode]（沿用 [ThemeMode]，由 DataStore 持久化）。
 * - 深浅：[darkMode]（SYSTEM / LIGHT / DARK）。
 * - 取色：[dynamicColor]（Miuix 皮肤的「莫奈取色」开关：开 → MiuixTheme 动态取色，
 *   关 → MiuixTheme 原色。Material 皮肤始终走模板 Monet 动态取色引擎，不受此开关影响）、
 *   [seedColor]（Material 皮肤的种子色；0 表示「默认 / 跟随壁纸动态取色」，与模板
 *   KernelSU `keyColor == 0` 同语义）、[paletteStyle]（调色板风格）、
 *   [colorSpec]（Material 色板规范版本）。
 *
 * [seedColor] 统一使用 `0xFFRRGGBB` 形态的 Long（即 `Color(longValue)` 构造入参），
 * 默认 0（壁纸动态取色），预设值来自 [PresetSeedColors]。
 */
enum class ThemeDarkMode { SYSTEM, LIGHT, DARK }

enum class ThemePaletteStyle {
    TonalSpot, Neutral, Vibrant, Expressive, Rainbow, FruitSalad,
    Monochrome, Fidelity, Content,
}

enum class ThemeColorSpec { SPEC_2021, SPEC_2025 }

/** 种子色 0 = 默认（跟随壁纸动态取色，KernelSU keyColor==0 同语义）。 */
const val DEFAULT_SEED_COLOR: Long = 0L

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.MATERIAL3,
    val darkMode: ThemeDarkMode = ThemeDarkMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val seedColor: Long = DEFAULT_SEED_COLOR,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
) {
    val isMaterial: Boolean get() = themeMode == ThemeMode.MATERIAL3
}
