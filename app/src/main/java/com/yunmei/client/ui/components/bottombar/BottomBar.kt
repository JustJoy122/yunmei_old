package com.yunmei.client.ui.components.bottombar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yunmei.client.data.preferences.ThemeMode
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem

data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * 底栏双皮肤分派（整块重做，不再使用 haze 毛玻璃悬浮胶囊，杜绝「黑色覆盖」）：
 * - Miuix：官方 `top.yukonga.miuix.kmp.basic.NavigationBar`（全宽 + 顶部分隔线 + 官方图标样式）。
 * - Material：Material3 `NavigationBar`（surfaceContainer 底色 + 胶囊选中指示器 + 标准图标/文字）。
 *
 * 底栏只承载顶层项；门锁 / 设置等二级子页面不显示底栏，由各自页面 TopAppBar 返回箭头返回。
 */
@Composable
fun AppBottomBar(
    themeMode: ThemeMode,
    items: List<BottomBarItem>,
    modifier: Modifier = Modifier,
) {
    when (themeMode) {
        ThemeMode.MIUIX -> MiuixNavigationBar(modifier = modifier) {
            items.forEach { item ->
                MiuixNavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = item.icon,
                    label = item.label,
                )
            }
        }

        ThemeMode.MATERIAL3 -> NavigationBar(
            modifier = modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                    label = { Text(text = item.label) },
                    colors = NavigationBarItemDefaults.colors(),
                )
            }
        }
    }
}
