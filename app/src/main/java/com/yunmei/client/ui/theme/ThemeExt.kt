package com.yunmei.client.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 是否启用 miuix-blur 毛玻璃（移植 KernelSU `LocalEnableBlur`）。
 *
 * 默认开启；SDK < 33 或设备不支持 RuntimeShader 时由 `isRuntimeShaderSupported()` /
 * `isRenderEffectSupported()` 自动降级，不影响观感语义。
 */
val LocalEnableBlur = staticCompositionLocalOf { true }

/**
 * 当前是否深色。由双皮肤主题入口（`YunMeiMaterialTheme` / `YunMeiMiuixTheme`）注入，
 * 等价于 KernelSU `LocalColorMode` 的「当前深色」判定。
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = LocalDarkTheme.current
