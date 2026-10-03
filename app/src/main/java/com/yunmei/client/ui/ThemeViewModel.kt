package com.yunmei.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunmei.client.data.preferences.AppPreferences
import com.yunmei.client.data.preferences.ThemeMode
import com.yunmei.client.ui.theme.ThemeDarkMode
import com.yunmei.client.ui.theme.ThemePaletteStyle
import com.yunmei.client.ui.theme.ThemePreferences
import com.yunmei.client.ui.theme.ThemeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主题状态 VM：合并 `ThemeMode`（DataStore，风格切换）与 [ThemePreferences]
 * （SharedPreferences，深浅 / Monet / 种子色 / 调色板），对外暴露单一 [ThemeState]。
 */
class ThemeViewModel(
    private val appPreferences: AppPreferences,
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = combine(
        appPreferences.themeMode,
        themePreferences.state,
    ) { mode, state -> state.copy(themeMode = mode) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeState(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDarkMode(mode: ThemeDarkMode) = themePreferences.setDarkMode(mode)

    fun setDynamicColor(value: Boolean) = themePreferences.setDynamicColor(value)

    fun setSeedColor(value: Long) = themePreferences.setSeedColor(value)

    fun setPaletteStyle(style: ThemePaletteStyle) = themePreferences.setPaletteStyle(style)
}
