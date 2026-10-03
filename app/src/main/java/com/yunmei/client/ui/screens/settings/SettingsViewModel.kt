package com.yunmei.client.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunmei.client.data.preferences.AppPreferences
import com.yunmei.client.data.preferences.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val appPreferences: AppPreferences) : ViewModel() {

    val settings: StateFlow<AppSettings> = appPreferences.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    fun setQuickConnect(value: Boolean) = launch { appPreferences.setQuickConnect(value) }
    fun setAutoConnect(value: Boolean) = launch { appPreferences.setAutoConnect(value) }
    fun setAutoExit(value: Boolean) = launch { appPreferences.setAutoExit(value) }
    fun setAutoCode(value: Boolean) = launch { appPreferences.setAutoCode(value) }
    fun setAlwaysCode(value: Boolean) = launch { appPreferences.setAlwaysCode(value) }
    fun setHideSign(value: Boolean) = launch { appPreferences.setHideSign(value) }
    fun setHideCode(value: Boolean) = launch { appPreferences.setHideCode(value) }
    fun setForceOrientation(value: Boolean) = launch { appPreferences.setForceOrientation(value) }
    fun setAttemptUpload(value: Boolean) = launch { appPreferences.setAttemptUpload(value) }
    fun setRecordObject(value: String) = launch { appPreferences.setRecordObject(value) }
    fun setSignLocationMode(value: String) = launch { appPreferences.setSignLocationMode(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
