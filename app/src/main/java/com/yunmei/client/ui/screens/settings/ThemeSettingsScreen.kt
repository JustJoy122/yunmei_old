package com.yunmei.client.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yunmei.client.data.preferences.ThemeMode
import com.yunmei.client.ui.component.material.ExpressiveScaffold
import com.yunmei.client.ui.component.material.ExpressiveToggleButton
import com.yunmei.client.ui.component.material.SegmentedColumn
import com.yunmei.client.ui.component.material.SegmentedDropdownItem
import com.yunmei.client.ui.component.material.TopBarBackButton
import com.yunmei.client.ui.component.material.expressiveTopAppBarColors
import com.yunmei.client.ui.theme.DEFAULT_SEED_COLOR
import com.yunmei.client.ui.theme.PresetSeedColors
import com.yunmei.client.ui.theme.ThemeDarkMode
import com.yunmei.client.ui.theme.ThemePaletteStyle
import com.yunmei.client.ui.theme.ThemeState
import com.yunmei.client.ui.theme.rememberYunMeiColorScheme
import com.yunmei.client.ui.theme.toMaterialKolor
import com.yunmei.client.ui.theme.toSpecVersion
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 「主题设置」二级子页面：结构移植 KernelSU `ColorPaletteScreenMaterial.kt` /
 * `ColorPaletteScreenMiuix.kt`。
 *
 * Material：深浅模式 = 模板明暗分段按钮组；种子色 = 模板色盘按钮（默认 = 跟随壁纸动态取色，
 * 选中态带对勾）；调色板风格 / 色彩规范 = 模板下拉（选中项带对勾）。Material 分支完全走
 * 模板 Monet 动态取色。
 *
 * Miuix：深浅模式 = 模板 TabRow；「莫奈取色」= 模板 SwitchPreference（开关内白色对勾，
 * 开启背景由 MiuixTheme 控制）：开 → MiuixTheme 动态取色，关 → MiuixTheme 原色。
 */
@Composable
fun ThemeSettingsScreen(
    themeState: ThemeState,
    onDarkModeChange: (ThemeDarkMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSeedColorChange: (Long) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    when (themeState.themeMode) {
        ThemeMode.MIUIX -> ThemeSettingsPagerMiuix(themeState, onDarkModeChange, onDynamicColorChange, onBack)
        ThemeMode.MATERIAL3 -> ThemeSettingsPagerMaterial(
            themeState,
            onDarkModeChange,
            onSeedColorChange,
            onPaletteStyleChange,
            onBack,
        )
    }
}

/* ============================ Material 皮肤（移植 ColorPaletteScreenMaterial.kt） ============================ */

@Composable
private fun ThemeSettingsPagerMaterial(
    themeState: ThemeState,
    onDarkModeChange: (ThemeDarkMode) -> Unit,
    onSeedColorChange: (Long) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val haptic = LocalHapticFeedback.current
    val darkTheme = when (themeState.darkMode) {
        ThemeDarkMode.LIGHT -> false
        ThemeDarkMode.DARK -> true
        ThemeDarkMode.SYSTEM -> isSystemInDarkTheme()
    }
    val paletteStyle = themeState.paletteStyle.toMaterialKolor()
    val colorSpec = themeState.colorSpec.toSpecVersion()

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    TopBarBackButton(onClick = onBack)
                },
                title = { Text("主题设置") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButtonMaterial(
                        color = Color.Unspecified,
                        isSelected = themeState.seedColor == DEFAULT_SEED_COLOR,
                        isDark = darkTheme,
                        paletteStyle = paletteStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            onSeedColorChange(DEFAULT_SEED_COLOR)
                        }
                    )
                }

                items(PresetSeedColors) { seed ->
                    ColorButtonMaterial(
                        color = Color(seed.value),
                        isSelected = themeState.seedColor == seed.value,
                        isDark = darkTheme,
                        paletteStyle = paletteStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            onSeedColorChange(seed.value)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    ThemeDarkMode.SYSTEM to "跟随系统",
                    ThemeDarkMode.LIGHT to "浅色",
                    ThemeDarkMode.DARK to "深色",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    options.forEachIndexed { index, (mode, label) ->
                        ExpressiveToggleButton(
                            checked = themeState.darkMode == mode,
                            onCheckedChange = {
                                if (it) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onDarkModeChange(mode)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeDarkMode.SYSTEM -> Icons.Filled.Brightness4
                                    ThemeDarkMode.LIGHT -> Icons.Filled.Brightness7
                                    ThemeDarkMode.DARK -> Icons.Filled.Brightness3
                                },
                                contentDescription = label
                            )
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val styles = ThemePaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = "调色板风格",
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(themeState.paletteStyle).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    onPaletteStyleChange(styles[index])
                                }
                            )
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** 移植 KernelSU `ColorButtonMaterial`：色盘按钮，选中态 = 对勾圆环（onPrimary 对勾）。 */
@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    paletteStyle: com.materialkolor.PaletteStyle,
    colorSpec: com.materialkolor.dynamiccolor.ColorSpec.SpecVersion,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = rememberYunMeiColorScheme(
        seedColor = color,
        isDark = isDark,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

/* ============================ Miuix 皮肤（移植 ColorPaletteScreenMiuix.kt） ============================ */

@Composable
private fun ThemeSettingsPagerMiuix(
    themeState: ThemeState,
    onDarkModeChange: (ThemeDarkMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "主题设置",
                navigationIcon = {
                    MiuixIconButton(
                        onClick = onBack
                    ) {
                        MiuixIcon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
                Spacer(modifier = Modifier.height(12.dp))

                val themeItems = listOf("跟随系统", "浅色", "深色")
                TabRow(
                    tabs = themeItems,
                    selectedTabIndex = themeState.darkMode.ordinal,
                    onTabSelected = { index ->
                        onDarkModeChange(ThemeDarkMode.entries[index])
                    },
                )

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = "莫奈取色",
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Wallpaper,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "莫奈取色",
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = themeState.dynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
