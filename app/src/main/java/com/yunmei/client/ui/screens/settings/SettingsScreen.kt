package com.yunmei.client.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Palette as RoundedPalette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yunmei.client.core.di.AppContainer
import com.yunmei.client.data.preferences.AppSettings
import com.yunmei.client.data.preferences.ThemeMode
import com.yunmei.client.ui.component.material.ExpressiveScaffold
import com.yunmei.client.ui.component.material.SegmentedColumn
import com.yunmei.client.ui.component.material.SegmentedDropdownItem
import com.yunmei.client.ui.component.material.SegmentedListItem
import com.yunmei.client.ui.component.material.SegmentedSwitchItem
import com.yunmei.client.ui.component.material.expressiveTopAppBarColors
import com.yunmei.client.ui.theme.ThemeState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/**
 * 主设置页：结构与 KernelSU `SettingsScreen.kt`（SettingPager / SettingPagerMaterial /
 * SettingPagerMiuix）一致，全部设置行使用模板组件（Material: SegmentedColumn +
 * SegmentedDropdownItem/SegmentedSwitchItem/SegmentedListItem；Miuix: Card +
 * OverlayDropdownPreference/SwitchPreference/ArrowPreference）。
 *
 * 「界面风格」与「主题设置」同卡同排布：界面风格 = 四格网格线框图标（Dashboard）+ 当前值 +
 * 上下双箭头下拉（选中项带对勾）；主题设置 = 带圆孔调色盘线框图标（Palette）+ 右单箭头。
 * 其余分组（开门 / 获取密码 / 打卡 / 关于 / 恢复默认设置）复用同一套组件，保持模板观感。
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    themeState: ThemeState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(container.appPreferences)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val actions = SettingsScreenActions(
        onSetUiModeIndex = { index ->
            onThemeModeChange(if (index == 0) ThemeMode.MIUIX else ThemeMode.MATERIAL3)
        },
        onOpenTheme = onNavigateToTheme,
        onOpenAbout = onNavigateToAbout,
    )

    when (themeState.themeMode) {
        ThemeMode.MIUIX -> SettingPagerMiuix(themeState, settings, viewModel, actions)
        ThemeMode.MATERIAL3 -> SettingPagerMaterial(themeState, settings, viewModel, actions)
    }
}

private class SettingsScreenActions(
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenTheme: () -> Unit,
    val onOpenAbout: () -> Unit,
)

private val uiModeItems = listOf("Miuix", "Material")
private const val uiModeSummary = "选择应用的界面风格"
private const val themeSummary = "自定义更多主题选项"

/* ============================ Material 皮肤（移植 SettingsMaterial.kt） ============================ */

@Composable
private fun SettingPagerMaterial(
    themeState: ThemeState,
    settings: AppSettings,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showResetDialog by remember { mutableStateOf(false) }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置") },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = buildList {
                    add {
                        SegmentedDropdownItem(
                            icon = Icons.Rounded.Dashboard,
                            title = "界面风格",
                            summary = uiModeSummary,
                            items = uiModeItems,
                            selectedIndex = if (themeState.themeMode == ThemeMode.MATERIAL3) 1 else 0,
                            onItemSelected = actions.onSetUiModeIndex
                        )
                    }
                    add {
                        SegmentedListItem(
                            onClick = actions.onOpenTheme,
                            headlineContent = { Text("主题设置") },
                            supportingContent = {
                                Text(
                                    themeSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = { Icon(Icons.Filled.Palette, "主题设置") },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null
                                )
                            }
                        )
                    }
                }
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Bolt,
                            title = "快速连接",
                            checked = settings.quickConnect,
                            onCheckedChange = viewModel::setQuickConnect
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.LockOpen,
                            title = "启动时自动开门",
                            checked = settings.autoConnect,
                            onCheckedChange = viewModel::setAutoConnect
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.ExitToApp,
                            title = "开门后自动退出",
                            checked = settings.autoExit,
                            onCheckedChange = viewModel::setAutoExit
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.VpnKey,
                            title = "开门时同时获取密码",
                            checked = settings.autoCode,
                            onCheckedChange = viewModel::setAutoCode
                        )
                    },
                )
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.AccountCircle,
                            title = "无对应账号时使用首个账号",
                            checked = settings.alwaysCode,
                            onCheckedChange = viewModel::setAlwaysCode
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.VisibilityOff,
                            title = "隐藏获取密码按钮",
                            checked = settings.hideCode,
                            onCheckedChange = viewModel::setHideCode
                        )
                    },
                )
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.EventBusy,
                            title = "隐藏打卡按钮",
                            checked = settings.hideSign,
                            onCheckedChange = viewModel::setHideSign
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.ScreenLockPortrait,
                            title = "强制竖屏",
                            checked = settings.forceOrientation,
                            onCheckedChange = viewModel::setForceOrientation
                        )
                    },
                )
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = actions.onOpenAbout,
                            headlineContent = { Text("关于") },
                            supportingContent = { Text("云莓智能 · 版本 0.1.0") },
                            leadingContent = { Icon(Icons.Rounded.Info, "关于") },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null
                                )
                            }
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = { showResetDialog = true },
                            headlineContent = { Text("恢复默认设置") },
                            leadingContent = { Icon(Icons.Rounded.SettingsBackupRestore, "恢复默认设置") },
                        )
                    },
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认设置") },
            text = { Text("确定要恢复默认设置吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetSettings(viewModel)
                        showResetDialog = false
                    },
                ) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            },
        )
    }
}

/* ============================ Miuix 皮肤（移植 SettingsMiuix.kt） ============================ */

@Composable
private fun SettingPagerMiuix(
    themeState: ThemeState,
    settings: AppSettings,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    OverlayDropdownPreference(
                        title = "界面风格",
                        summary = uiModeSummary,
                        items = uiModeItems,
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Dashboard,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "界面风格",
                                tint = colorScheme.onBackground
                            )
                        },
                        selectedIndex = if (themeState.themeMode == ThemeMode.MATERIAL3) 1 else 0,
                        onSelectedIndexChange = actions.onSetUiModeIndex
                    )
                    ArrowPreference(
                        title = "主题设置",
                        summary = themeSummary,
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.RoundedPalette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "主题设置",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = actions.onOpenTheme
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.Bolt,
                        title = "快速连接",
                        checked = settings.quickConnect,
                        onCheckedChange = viewModel::setQuickConnect
                    )
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.LockOpen,
                        title = "启动时自动开门",
                        checked = settings.autoConnect,
                        onCheckedChange = viewModel::setAutoConnect
                    )
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.ExitToApp,
                        title = "开门后自动退出",
                        checked = settings.autoExit,
                        onCheckedChange = viewModel::setAutoExit
                    )
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.VpnKey,
                        title = "开门时同时获取密码",
                        checked = settings.autoCode,
                        onCheckedChange = viewModel::setAutoCode
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.AccountCircle,
                        title = "无对应账号时使用首个账号",
                        checked = settings.alwaysCode,
                        onCheckedChange = viewModel::setAlwaysCode
                    )
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "隐藏获取密码按钮",
                        checked = settings.hideCode,
                        onCheckedChange = viewModel::setHideCode
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.EventBusy,
                        title = "隐藏打卡按钮",
                        checked = settings.hideSign,
                        onCheckedChange = viewModel::setHideSign
                    )
                    MiuixSwitchPreference(
                        icon = Icons.Rounded.ScreenLockPortrait,
                        title = "强制竖屏",
                        checked = settings.forceOrientation,
                        onCheckedChange = viewModel::setForceOrientation
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    ArrowPreference(
                        title = "关于",
                        summary = "云莓智能 · 版本 0.1.0",
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Info,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "关于",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = actions.onOpenAbout
                    )
                    ArrowPreference(
                        title = "恢复默认设置",
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.SettingsBackupRestore,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "恢复默认设置",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = { showResetDialog = true },
                    )
                }
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    if (showResetDialog) {
        MiuixWindowDialog(
            show = true,
            title = "恢复默认设置",
            summary = "确定要恢复默认设置吗？",
            onDismissRequest = { showResetDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixTextButton(text = "取消", onClick = { showResetDialog = false })
                Spacer(modifier = Modifier.width(8.dp))
                MiuixTextButton(
                    text = "恢复",
                    onClick = {
                        resetSettings(viewModel)
                        showResetDialog = false
                    },
                )
            }
        }
    }
}

/** Miuix 版开关设置行（模板 SwitchPreference：开关内白色对勾，开启背景由 MiuixTheme 控制）。 */
@Composable
private fun MiuixSwitchPreference(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = title,
        startAction = {
            MiuixIcon(
                icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = title,
                tint = colorScheme.onBackground
            )
        },
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

private fun resetSettings(viewModel: SettingsViewModel) {
    viewModel.setQuickConnect(true)
    viewModel.setAutoConnect(false)
    viewModel.setAutoExit(false)
    viewModel.setAutoCode(false)
    viewModel.setAlwaysCode(false)
    viewModel.setHideSign(false)
    viewModel.setHideCode(false)
    viewModel.setForceOrientation(false)
}
