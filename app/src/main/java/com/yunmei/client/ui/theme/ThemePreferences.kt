package com.yunmei.client.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题框架自身的持久化（深浅 / Monet 动态取色 / 种子色 / 调色板 / 色板规范）。
 *
 * 与 [com.yunmei.client.data.preferences.AppPreferences]（DataStore，负责 `ThemeMode`
 * 风格切换与业务设置）互补：这里用轻量 [SharedPreferences] 存放纯 UI 外观状态，
 * 让主题框架留在 `ui/theme` 包内自洽，不侵入 data 层。
 */
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("yunmei_theme", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        ThemeState(
            darkMode = enumOrDefault(KEY_DARK_MODE, ThemeDarkMode.SYSTEM),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            seedColor = prefs.getLong(KEY_SEED_COLOR, DEFAULT_SEED_COLOR),
            paletteStyle = enumOrDefault(KEY_PALETTE_STYLE, ThemePaletteStyle.TonalSpot),
            colorSpec = enumOrDefault(KEY_COLOR_SPEC, ThemeColorSpec.SPEC_2025),
        )
    )

    val state: StateFlow<ThemeState> = _state.asStateFlow()

    fun setDarkMode(value: ThemeDarkMode) = update { it.copy(darkMode = value) }

    fun setDynamicColor(value: Boolean) = update { it.copy(dynamicColor = value) }

    fun setSeedColor(value: Long) = update { it.copy(seedColor = value) }

    fun setPaletteStyle(value: ThemePaletteStyle) = update { it.copy(paletteStyle = value) }

    fun setColorSpec(value: ThemeColorSpec) = update { it.copy(colorSpec = value) }

    private fun update(transform: (ThemeState) -> ThemeState) {
        val next = transform(_state.value)
        _state.value = next
        persist(next)
    }

    private fun persist(state: ThemeState) {
        prefs.edit()
            .putString(KEY_DARK_MODE, state.darkMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, state.dynamicColor)
            .putLong(KEY_SEED_COLOR, state.seedColor)
            .putString(KEY_PALETTE_STYLE, state.paletteStyle.name)
            .putString(KEY_COLOR_SPEC, state.colorSpec.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }
            ?: default

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_SEED_COLOR = "seed_color"
        const val KEY_PALETTE_STYLE = "palette_style"
        const val KEY_COLOR_SPEC = "color_spec"
    }
}
