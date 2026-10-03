package com.yunmei.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yunmei.client.core.di.AppContainer
import com.yunmei.client.data.preferences.ThemeMode
import com.yunmei.client.ui.components.bottombar.AppBottomBar
import com.yunmei.client.ui.components.bottombar.BottomBarItem
import com.yunmei.client.ui.screens.about.AboutScreen
import com.yunmei.client.ui.screens.actions.ActionsScreen
import com.yunmei.client.ui.screens.home.HomeScreen
import com.yunmei.client.ui.screens.locks.LockListScreen
import com.yunmei.client.ui.screens.login.LoginScreen
import com.yunmei.client.ui.screens.settings.SettingsScreen
import com.yunmei.client.ui.screens.settings.ThemeSettingsScreen
import com.yunmei.client.ui.theme.ThemeDarkMode
import com.yunmei.client.ui.theme.ThemePaletteStyle
import com.yunmei.client.ui.theme.ThemeState

/**
 * 顶层 tab（底栏常驻、无返回箭头）与二级子页面（push/pop、带返回箭头）路由。
 *
 * 以 KernelSU 导航逻辑为基准：底栏是顶层 tab 根页；二级子页面是从某个 tab 进入的更深页面。
 */
object Routes {
    // 顶层 tab
    const val HOME = "home"
    const val LOCKS = "locks"
    const val ACTIONS = "actions"
    const val SETTINGS = "settings"
    // 二级子页面（带返回箭头）
    const val LOGIN = "login"
    const val THEME = "theme"
    const val ABOUT = "about"
}

@Composable
fun AppRoot(
    container: AppContainer,
    themeState: ThemeState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDarkModeChange: (ThemeDarkMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSeedColorChange: (Long) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 底栏只在 4 个顶层 tab 显示；登录/主题设置/关于是二级子页面，隐藏底栏。
    val showBottomBar = currentRoute == Routes.HOME ||
            currentRoute == Routes.LOCKS ||
            currentRoute == Routes.ACTIONS ||
            currentRoute == Routes.SETTINGS

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                    onNavigateToLocks = { navigateTab(Routes.LOCKS) },
                    onNavigateToActions = { navigateTab(Routes.ACTIONS) },
                )
            }
            composable(Routes.LOCKS) {
                LockListScreen(container = container)
            }
            composable(Routes.ACTIONS) {
                ActionsScreen(container = container)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    themeState = themeState,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToTheme = { navController.navigate(Routes.THEME) },
                    onNavigateToAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.THEME) {
                ThemeSettingsScreen(
                    themeState = themeState,
                    onDarkModeChange = onDarkModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onSeedColorChange = onSeedColorChange,
                    onPaletteStyleChange = onPaletteStyleChange,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    themeState = themeState,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showBottomBar) {
            AppBottomBar(
                themeMode = themeState.themeMode,
                items = listOf(
                    BottomBarItem("首页", Icons.Rounded.Home, currentRoute == Routes.HOME) { navigateTab(Routes.HOME) },
                    BottomBarItem("门锁", Icons.Rounded.Lock, currentRoute == Routes.LOCKS) { navigateTab(Routes.LOCKS) },
                    BottomBarItem("开门", Icons.Rounded.Key, currentRoute == Routes.ACTIONS) { navigateTab(Routes.ACTIONS) },
                    BottomBarItem("设置", Icons.Rounded.Settings, currentRoute == Routes.SETTINGS) { navigateTab(Routes.SETTINGS) },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
