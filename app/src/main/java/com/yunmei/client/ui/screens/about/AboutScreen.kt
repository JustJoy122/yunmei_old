package com.yunmei.client.ui.screens.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.yunmei.client.BuildConfig
import com.yunmei.client.R
import com.yunmei.client.data.preferences.ThemeMode
import com.yunmei.client.ui.theme.ThemeState

private const val GITHUB_URL = "https://github.com/zxy19/yunmei_unintelligent"

@Composable
fun AboutScreen(
    themeState: ThemeState,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val state = AboutUiState(
        title = "关于",
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        links = listOf(LinkInfo("在 GitHub 查看源码", GITHUB_URL)),
    )
    val actions = AboutScreenActions(
        onBack = onBack,
        onOpenLink = uriHandler::openUri,
    )

    when (themeState.themeMode) {
        ThemeMode.MIUIX -> AboutScreenMiuix(state, actions)
        ThemeMode.MATERIAL3 -> AboutScreenMaterial(state, actions)
    }
}
