package com.yunmei.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yunmei.client.ui.AppRoot
import com.yunmei.client.ui.ThemeViewModel
import com.yunmei.client.ui.theme.ThemePreferences
import com.yunmei.client.ui.theme.YunMeiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as YunMeiApp
        setContent {
            val themeViewModel: ThemeViewModel = viewModel {
                ThemeViewModel(
                    appPreferences = app.container.appPreferences,
                    themePreferences = ThemePreferences(applicationContext),
                )
            }
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()

            YunMeiTheme(themeState = themeState) {
                AppRoot(
                    container = app.container,
                    themeState = themeState,
                    onThemeModeChange = themeViewModel::setThemeMode,
                    onDarkModeChange = themeViewModel::setDarkMode,
                    onDynamicColorChange = themeViewModel::setDynamicColor,
                    onSeedColorChange = themeViewModel::setSeedColor,
                    onPaletteStyleChange = themeViewModel::setPaletteStyle,
                )
            }
        }
    }
}
