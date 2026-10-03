# 云莓智能 Android —— 官方 Miuix 统一 UI 移植方案（UI Port Plan）

> 本文是任务 t2 的交付物：梳理 LSPosed / Vector / KernelSU / InstallerX Revived 的统一主题框架、
> 导航脚手架、组件与资源结构，形成**按优先级排序的整体移植清单与目标包结构**，供 t3（构建链升级）
> 与 t4（UI 整体移植）落地。
>
> 前置结论：本团队已决定走「官方 Miuix」路线（Gradle 9.7.1 / AGP 9.3.2 / Kotlin 2.4.10 /
> compileSdk 37），因此本文默认**不再维持 compileSdk 36 的「Material3 基础设施 + MIUI token 近似」路线**。
> 与 `docs/ui-patterns.md`（compileSdk 36 时代的调研）的分工：`ui-patterns.md` 保留 Material 侧全部
> 特性与历史取舍，本文聚焦「接入官方 Miuix 后」的移植结构。
>
> 视觉 tokens / 配色 / 圆角 / 间距等具体规格由 t7 产出 `docs/ui-visual-spec.md`，本文只负责
> 「结构 + 组件 + 优先级 + 包结构」。

---

## 0. 结论摘要（TL;DR）

1. **官方 Miuix 稳定坐标锁定 `0.9.3`**（KernelSU 同款，可解析）：
   `top.yukonga.miuix.kmp:miuix-{ui,icons,preference,blur,navigation3-ui}-android:0.9.3`。
   InstallerX 用 `0.9.4-103b737b-SNAPSHOT`（私有源 SNAPSHOT），不作为默认版本；如需 `miuix-nav` /
   `miuix-shader` 再单独评估。

2. **统一主题框架第一蓝本 = KernelSU**：`KernelSUTheme` → `MiuixKernelSUTheme`（`ThemeController` +
   `MiuixTheme`）与 `MaterialKernelSUTheme`（material-kolor + `MaterialExpressiveTheme`）双皮肤，
   由 `UiMode { Miuix, Material }` 分派。这是与本工程「Material3 / MIUIX 双风格」最贴合的范本。

3. **导航脚手架第一蓝本 = KernelSU 的 androidx.navigation3 方案**：`NavDisplay` + `entryProvider` +
   自定义 `Navigator`（回退栈）+ `HorizontalPager`（顶层 Tab）+ `Miuix NavigationBar` /
   `Material3 ShortNavigationBar` 底栏。相比 InstallerX 的 `miuix-nav`（`NavDisplay` + 预测性返回转场），
   KernelSU 方案依赖更少、版本与 `0.9.3` 完全对齐，作为默认；InstallerX 的 `miuix-nav` 预测性返回列为 P2。

4. **「SuperNavBar」术语澄清**：`top.yukonga.miuix.kmp` 0.9.x 里的底部导航组件名是
   `NavigationBar` / `NavigationBarItem`（`top.yukonga.miuix.kmp.basic`），即历史上 `SuperNavBar` 的
   KMP 重命名；Material 侧对应 `ShortNavigationBar` / `ShortNavigationBarItem`。落地时以
   `NavigationBar`（Miuix）/ `ShortNavigationBar`（Material）为准。

5. **与既有功能零冲突**：登录 / 门锁（BLE+扫码）/ 二维码 / 打卡 / 设置均为普通页面，接 Miuix 只替换
   主题入口、底栏与设置页容器，网络层 / 数据层 / BLE / 二维码代码不动。

---

## 1. 四个参考项目的 UI 架构横评

| 项目 | 主题框架 | 取色引擎 | 导航脚手架 | 底栏/导航组件 | 玻璃/液态玻璃 | 价值 |
| --- | --- | --- | --- | --- | --- | --- |
| **LSPosed-master** | 经典 View + Material XML overlay | `DynamicColors` + `system_accent1_*`（Android 12+） | 无 Compose 导航 | 无 | 无 | 仅参考「Monet 种子 + 预设色」交互模式，不照搬（View 体系） |
| **Vector-master** | Compose `MaterialExpressiveTheme` | 自研 `SeedScheme`（CIE LCh，零依赖） | `NavigationSuiteScaffold` + `PanelBar` | `ShortNavigationBarItem` / `WideNavigationRailItem` | 无 | `SeedScheme` 零依赖 Monet + `ColorWheel` 色盘（P2） |
| **KernelSU-main** | `KernelSUTheme`（Miuix / Material 双皮肤） | material-kolor 5.0.0 | androidx.navigation3（NavDisplay + 自定义 Navigator + Pager） | Miuix `NavigationBar` / Material `ShortNavigationBar` + `FloatingBottomBar` | `ColorBlendToken` + `liquid/` + miuix-blur | **第一蓝本**：miuix 0.9.3 + 双皮肤 + 双底栏 |
| **InstallerX-Revived-main** | `InstallerTheme`（`useMiuix` 分派） | material-kolor 5.0.1 + `MonetCompat` + `androidx.palette` | miuix-nav `NavDisplay` + 预测性返回 | Miuix `NavigationBar` + `FloatingBottomBar`（liquid glass 三档） | `ColorBlendToken` + `ui/library/liquid/` + `ui/library/effect/` | **增强参考**：ThemeState 最完整、玻璃效果最全 |

> 结论：主题框架以 **KernelSU** 为骨架、**InstallerX** 为增强；导航以 **KernelSU navigation3** 为骨架、
> **InstallerX miuix-nav** 为可选增强；Material 侧 Monet 取色参考 Vector `SeedScheme`（已落地，见
> `ui-patterns.md` §10）与 KernelSU/InstallerX 的 material-kolor 封装。

---

## 2. 官方 Miuix 0.9.x 依赖矩阵与 API 清单

### 2.1 版本矩阵（唯一已知可行组合）

| 项目 | AGP | Kotlin | compileSdk | compose-bom | material3 | material-kolor | miuix |
| --- | --- | --- | --- | --- | --- | --- | --- |
| KernelSU-main | 9.3.2 | 2.4.10 | 37 | 2026.08.00 | 1.5.0-alpha26 | 5.0.0 | **0.9.3** |
| InstallerX-Revived-main | 9.3.2 | 2.4.10 | 37 | — | 1.5.0-alpha27 | 5.0.1 | 0.9.4-103b737b-SNAPSHOT |
| **YunMeiAndroid（已落地 t3）** | **9.3.2** | **2.4.10** | **37** | **2026.08.00** | **1.5.0-alpha26** | **5.0.0** | **0.9.3** |

> **t3 已落地的实际采用值**（写入 `gradle/libs.versions.toml`，`:app:assembleDebug` 已验证通过）：
> `agp=9.3.2`、`kotlin=2.4.10`、`compileSdk=37`（`compileSdkMinor=0`）、`targetSdk=37`、
> `composeBom=2026.08.00`、`material3=1.5.0-alpha26`、`materialKolor=5.0.0`、`miuix=0.9.3`、
> `lifecycle=2.11.0`、`activityCompose=1.13.0`、`kotlinxCoroutines=1.11.0`、`kotlinxSerialization=1.11.0`、
> `navigation3=1.1.6`、`navigationevent=1.1.2`；Java/Kotlin 目标 = 21（AGP9 走 built-in Kotlin，
> 不再 apply `org.jetbrains.kotlin.android`）。
> 其中 `navigation3`/`navigationevent`（`androidx-navigation3-runtime`、`androidx-navigationevent-compose`、
> `androidx-lifecycle-viewmodel-navigation3`）已入版本目录但尚未作为 app 依赖，t4 接线导航时再 `implementation`。

### 2.2 依赖坐标（0.9.3，KernelSU 实测）

```toml
miuix = "0.9.3"
miuix-ui             = { module = "top.yukonga.miuix.kmp:miuix-ui-android",             version.ref = "miuix" }
miuix-icons          = { module = "top.yukonga.miuix.kmp:miuix-icons-android",          version.ref = "miuix" }
miuix-preference     = { module = "top.yukonga.miuix.kmp:miuix-preference-android",     version.ref = "miuix" }
miuix-blur           = { module = "top.yukonga.miuix.kmp:miuix-blur-android",           version.ref = "miuix" }
miuix-navigation3-ui = { module = "top.yukonga.miuix.kmp:miuix-navigation3-ui-android", version.ref = "miuix" }
# 可选（InstallerX 0.9.4 才有 / 或按需）：miuix-core / miuix-shader / miuix-nav
```

### 2.3 API 面清单（0.9.x，实测于两参考仓库 import）

**主题**（`top.yukonga.miuix.kmp.theme`）：`MiuixTheme`、`ThemeController`、`ColorSchemeMode`
（System/Light/Dark/MonetSystem/MonetLight/MonetDark）、`ThemePaletteStyle`、`ThemeColorSpec`
（Spec2021/Spec2025）、`LocalContentColor`、`LocalDismissState`、`MiuixTheme.isDynamicColor`、
`LocalSquircleEnabled`。

**基础组件**（`top.yukonga.miuix.kmp.basic`）：`Scaffold`、`TopAppBar`、`SmallTopAppBar`、`SmallTitle`、
`Card`、`Surface`、`NavigationBar` / `NavigationBarItem` / `NavigationItem`、`NavigationRail` /
`NavigationRailItem`、`Switch`、`Slider`、`Checkbox`、`Text`、`Icon`、`IconButton`、`Button`、
`TextButton`、`TextField` / `InputField`、`HorizontalDivider`、`ListPopupColumn`、
`FloatingActionButton`、`InfiniteProgressIndicator`、`PullToRefresh`、`SnackbarHost`、
`VerticalScrollBar`、`MiuixScrollBehavior`、`Dropdown*` 系列。

**偏好组件**（`top.yukonga.miuix.kmp.preference`）：`SwitchPreference`、`ArrowPreference`、
`CheckboxPreference`、`OverlayDropdownPreference`、`WindowSpinnerPreference`。

**毛玻璃/液态玻璃**（`top.yukonga.miuix.kmp.blur`）：`Backdrop`、`LayerBackdrop`、`rememberLayerBackdrop`、
`BlendColorEntry`、`BlurBlendMode`、`highlight.{BloomStroke,Highlight,LightSource,LightPosition}`、
`sensor.rememberDeviceTilt`、`isRuntimeShaderSupported`。注意：`Lens` / `Vibrancy` / `CombinedBackdrop`
等折射组件是参考项目本地 `ui/library/liquid/` 的自研封装，**不是** miuix 库自带（需按源码移植，Apache-2.0）。

**导航（可选增强，InstallerX）**（`top.yukonga.miuix.kmp.nav.*`）：`NavDisplay`、`NavBackStack`、`NavKey`、
`rememberNavBackStack`、`NavTransition` / `navDirectionalTransition` / `navGraphicsTransition`、
`NavSwipeDirection` / `NavSwipeEdge`、`NavCornerClipMode`、`NavDisplayEffects`。

**图标**（`top.yukonga.miuix.kmp.icon`）：`MiuixIcons` + `basic.*` / `extended.*` 子包（Back/Close/Delete/
Edit/Info/More/Ok/Refresh/Settings/Check/ArrowUpDown …）。

---

## 3. 统一主题框架移植（MiuixTheme + ThemeController）

### 3.1 现状与目标差异

现状（`app/src/main/java/com/yunmei/client/ui/theme/Theme.kt`）：`YunMeiTheme(themeState)` 内部用
`MaterialTheme` 承载两套静态 token（`MiuiXLightColors/DarkColors` 是「MIUI token 近似」），不是官方 Miuix。

目标：Material 皮肤继续 `MaterialTheme`（material-kolor 取色 + Expressive 动效，见 `ui-patterns.md` §9），
MiuiX 皮肤改走**官方 `MiuixTheme`**。

### 3.2 主题状态模型扩展（蓝本：InstallerX `ThemeState` + KernelSU `AppSettings`）

现有 `ThemeState`（`ThemeMode { MATERIAL3, MIUIX }` + `ThemeDarkMode` + `dynamicColor` + `seedColor` +
`paletteStyle` + `colorSpec`）已覆盖三维。**只需补两个字段**：

```kotlin
data class ThemeState(
    val themeMode: ThemeMode,          // MATERIAL3 / MIUIX（沿用）
    val darkMode: ThemeDarkMode,       // SYSTEM / LIGHT / DARK（沿用）
    val dynamicColor: Boolean,         // 沿用
    val seedColor: Long,               // 沿用
    val paletteStyle: ThemePaletteStyle, // 沿用，映射到 ThemePaletteStyle
    val colorSpec: ThemeColorSpec,     // 沿用，映射到 ThemeColorSpec
    // 新增：
    val useMiuixMonet: Boolean = false,  // MiuiX 皮肤是否也走 Monet 种子（对标 InstallerX useMiuixMonet）
    val useBlur: Boolean = true,         // MiuiX 皮肤是否启用 miuix-blur 毛玻璃（SDK<33 自动降级）
)
```

### 3.3 主题入口重构

```kotlin
@Composable
fun YunMeiTheme(themeState: ThemeState, content: @Composable () -> Unit) {
    when (themeState.themeMode) {
        ThemeMode.MIUIX     -> YunMeiMiuixTheme(themeState, content)
        ThemeMode.MATERIAL3 -> YunMeiMaterialTheme(themeState, content)
    }
}
```

**MiuiX 皮肤**（蓝本：KernelSU `MiuixKernelSUTheme.kt`）：

```kotlin
val controller = ThemeController(
    colorSchemeMode = /* System/Light/Dark 或 MonetSystem/MonetLight/MonetDark */,
    keyColor = resolvedKeyColor,        // 种子色；动态取色时用系统 Monet primary 反推
    isDark = darkTheme,
    paletteStyle = miuixPaletteStyle,   // ThemePaletteStyle.valueOf(paletteStyle.name)
    colorSpec = miuixColorSpec,         // SPEC_2025 仅在 supportsSpec2025 的 style 下生效，否则 SPEC_2021
)
MiuixTheme(controller = controller) {
    LaunchedEffect(darkTheme) { /* 状态栏/导航栏图标深浅 */ }
    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onBackground) {
        content()
    }
}
```

要点（落地时对照）：
- `ColorSchemeMode` 映射：`ThemeDarkMode.SYSTEM/LIGHT/DARK` → `System/Light/Dark`；启用 Monet 时 →
  `MonetSystem/MonetLight/MonetDark`。
- **SPEC_2025 回退**：仅 `TonalSpot/Neutral/Vibrant/Expressive` 支持 Spec2025（KernelSU
  `PaletteStyle.supportsSpec2025`），其余 style 回退 Spec2021。
- **状态栏/导航栏**：`WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
  !darkTheme`；Android 10+ `window.isNavigationBarContrastEnforced = false`（毛玻璃底栏必需）。
- **Material 皮肤**（蓝本：KernelSU `MaterialKernelSUTheme.kt` / InstallerX `InstallerMaterialExpressiveTheme`）：
  用 material-kolor 5.0.0（compileSdk 37 下可用）生成 `ColorScheme` + `MaterialExpressiveTheme(motionScheme =
  MotionScheme.expressive())`。当前工程 material3 1.4.0 的 `MaterialExpressiveTheme` 是 `internal`，**升级
  material3 到 1.5.0-alpha26 后即可公开调用**（t3 同步处理）。

### 3.4 双皮肤切换的持久化

沿用 `AppPreferences`（DataStore）保存 `ThemeMode`；新增 key：`theme_use_miuix_monet`、`theme_use_blur`。
`MainActivity` 的 `ThemeViewModel` 提供对应 setter（参考现有 `setThemeMode` 模式）。

---

## 4. 导航脚手架移植（NavigationBar / ShortNavigationBar）

### 4.1 现状

`AppRoot.kt` 用 `androidx.navigation:navigation-compose`（`NavHost`/`rememberNavController`）+ 自绘
`FrostedBottomBar`（haze）。路由：`home / locks / settings / login`（login 为全屏，不进底栏）。

### 4.2 方案 A（默认）：androidx.navigation3，KernelSU 同款

依赖：
```toml
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }  # 1.1.6
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }  # 2.11.0
androidx-navigationevent-compose = { module = "androidx.navigationevent:navigationevent-compose", version.ref = "navigationevent" }  # 1.1.2
miuix-navigation3-ui = { module = "top.yukonga.miuix.kmp:miuix-navigation3-ui-android", version.ref = "miuix" }
```

结构（对照 KernelSU `ui/MainActivity.kt` / `ui/navigation3/`）：
1. `Routes.kt`：`sealed interface Route : NavKey`，含 `Home / Locks / Settings`（顶层 Tab）与
   `Login`（全屏 push）。
2. `Navigator.kt`：自定义回退栈 `Navigator`（`SnapshotStateList<NavKey>` + push/replace/pop/popUntil/
   setResult/observeResult + `Saver` + `rememberNavigator`），提供 `LocalNavigator`。
3. 顶层用 `HorizontalPager`（`rememberPagerState`）+ `MainPagerState`（`springAnimateToPage` 平滑滑动）
   承载 Home/Locks/Settings 三个 Tab；`NavDisplay` + `entryProvider` + `entryDecorators`
   （`rememberSaveableStateHolderNavEntryDecorator` / `rememberViewModelStoreNavEntryDecorator`）承载
   全屏页（Login）与子页。
4. 底栏按皮肤分派：
   - Miuix：`top.yukonga.miuix.kmp.basic.NavigationBar` + `NavigationBarItem`（`NavigationItem(label, icon)`，
     可选 `badge`，可选悬浮 `FloatingBottomBar`）。
   - Material：material3 `ShortNavigationBar` + `ShortNavigationBarItem`（selected/unselected 双态图标）。

### 4.3 方案 B（可选增强）：miuix-nav（InstallerX 同款）

`top.yukonga.miuix.kmp.nav.core.NavDisplay` + `rememberNavBackStack` + 预测性返回转场
（`NavSwipeDirection`、`navDirectionalTransition`）。观感更接近 HyperOS 原生导航转场，但引入
`miuix-nav`（InstallerX 用 0.9.4-SNAPSHOT，0.9.3 是否含 `miuix-nav` 需落地时以仓库 README 确认）。
**列为 P2**：方案 A 跑通后再评估。

### 4.4 迁移建议（给 t4）

- 保留 `Routes` 常量思路，但把顶层 3 Tab 改为 Pager（对标参考 App 的左右滑动 + 底栏联动），`login` 保持
  `NavDisplay` 全屏 push（无底栏）。
- `FrostedBottomBar`（haze 自绘）在接入官方 miuix-blur 后**弃用或降级**：Miuix 侧直接用
  `NavigationBar`（可透明 + `layerBackdrop` 做毛玻璃，KernelSU `BlurredBar` 思路）；Material 侧用
  `ShortNavigationBar`。
- 预测性返回：manifest 加 `android:enableOnBackInvokedCallback="true"`；优先
  `androidx.navigationevent.compose.NavigationBackHandler`（KernelSU `MainScreenBackHandler`），
  Pager 非首 Tab 时返回先回首页。

---

## 5. 组件与资源移植清单

### 5.1 设置页分组卡片

- 蓝本：KernelSU `ui/screen/settings/SettingsMiuix.kt`（`Card` 内叠多个 preference）。
- Miuix 侧：`top.yukonga.miuix.kmp.basic.Card` + `SwitchPreference` / `ArrowPreference` /
  `CheckboxPreference` / `OverlayDropdownPreference`。`Card` 自动处理分组项圆角/描边，无需手写
  top/middle/bottom/single shape。
- Material 侧：material3 `Surface` + 分段圆角（沿用 `ui-patterns.md` §5.2 `Shape.kt` 的
  `CornerRadius=16dp / ConnectionRadius=5dp`）。
- 现状设置页（`SettingsScreen.kt` 的 `SectionTitle` + `RadioButton` + `SwitchRow`）**全部替换**为上述分组
  卡片；既有设置项（界面风格/深浅/取色/开门/获取密码/打卡）逻辑不变，只换容器与控件。

### 5.2 开关 / 滑块

| 用途 | Miuix | Material |
| --- | --- | --- |
| 列表内开关 | `SwitchPreference`（title/summary/startAction/checked） | material3 `Switch`（现有 `SwitchRow`） |
| 独立开关 | `top.yukonga.miuix.kmp.basic.Switch` | material3 `Switch` |
| 滑块 | `top.yukonga.miuix.kmp.basic.Slider` + `SliderDefaults` | material3 `Slider` |
| 复选 | `CheckboxPreference` / `Checkbox` | material3 `Checkbox` / `RadioButton` |

### 5.3 图标

- Miuix：`top.yukonga.miuix.kmp.icon.MiuixIcons` + `basic.*` / `extended.*`（Back/Close/Delete/Edit/Info/
  More/Ok/Refresh/Settings/Check/ArrowUpDown…），配 `top.yukonga.miuix.kmp.basic.Icon`。
- Material：沿用 `androidx.compose.material:material-icons-extended`（现有 `Icons.Filled.Home/Lock/Settings`）。
- 底栏图标：Home/Locks/Settings 三 Tab 在两种皮肤各配一套（Miuix 用 `MiuixIcons`，Material 用
  `Icons.Filled/Outlined` 双态）。

### 5.4 毛玻璃 / 液态玻璃

| 能力 | 参考实现 | 落地方式 |
| --- | --- | --- |
| 背景模糊/透明底栏 | KernelSU `BlurredBar` + `rememberBlurBackdrop`；miuix-blur `layerBackdrop` / `Backdrop` | 用 `miuix-blur`（`rememberLayerBackdrop` + `Modifier.layerBackdrop`）替换现有 haze 自绘 |
| 玻璃混合色表 | `ColorBlendToken`（IX `ui/library/blend/`，KS `ui/component/miuix/effect/`） | 直接移植（0.9.3 下 `BlendColorEntry`/`BlurBlendMode` 由 `miuix-blur` 提供，无需本地重定义） |
| 内阴影 | `InnerShadow`（`ui/library/liquid/InnerShadow.kt`） | 直接移植（自包含 Modifier.Node） |
| 折射/色散玻璃 | `Lens` / `Vibrancy` / `CombinedBackdrop`（`ui/library/liquid/`） | 按源码移植（依赖 miuix-blur `Backdrop`，0.9.3 已具备） |
| 悬浮液态玻璃底栏 | KS `FloatingBottomBar` / IX `ui/library/FloatingBottomBar.kt` | 移植（依赖 miuix-blur `Highlight/BloomStroke` + `rememberDeviceTilt`） |
| MIUI 动态渐变背景 | IX `ui/library/effect/BgEffect*` | 移植（RuntimeShader，API33+ 兜底） |

> 落地策略：先接 `miuix-blur` 的 `layerBackdrop` 做透明底栏（P0）；`ColorBlendToken` + `InnerShadow`（P1）；
> `Lens/Vibrancy` 折射与 `FloatingBottomBar` 液态玻璃（P2）；`BgEffect` 动态背景（P2 可选）。

---

## 6. 按优先级排序的整体移植清单

### P0（立即，接入骨架，构建通过即达标）

1. t3 前置：升级构建链到 Gradle 9.7.1 / AGP 9.3.2 / Kotlin 2.4.10 / compileSdk 37（见 `docs/miuix-migration.md`）。
2. 引入 miuix 依赖（`miuix-ui/icons/preference/blur/navigation3-ui:0.9.3`）+ material-kolor 5.0.0 +
   material3 1.5.0-alpha26 + androidx.navigation3 1.1.6。
3. 重构主题入口：`YunMeiTheme` 分派 `YunMeiMiuixTheme`（`ThemeController` + `MiuixTheme`）与
   `YunMeiMaterialTheme`（material-kolor + `MaterialExpressiveTheme`）。
4. `ThemeState` 补 `useMiuixMonet` / `useBlur` 字段 + `ThemeViewModel`/`AppPreferences` 持久化。
5. 状态栏图标深浅 + `isNavigationBarContrastEnforced=false` 处理（两皮肤共用）。
6. 底栏双皮肤：Miuix `NavigationBar` / Material `ShortNavigationBar`；替换 `FrostedBottomBar`。

### P1（紧随，对标观感）

7. 设置页改为分组卡片：Miuix `Card` + preference 组件；Material `Surface` + 分段圆角。
8. 开关/滑块/复选统一映射到 Miuix `Switch/Slider/Checkbox` 与 preference 族。
9. 图标统一：Miuix `MiuixIcons` + Material icons 双套。
10. 导航升级为 navigation3（Pager 顶层 Tab + NavDisplay 全屏页），login 保持全屏无底栏。
11. 玻璃原语：`ColorBlendToken` + `InnerShadow` 落地；`layerBackdrop` 毛玻璃底栏。
12. 平滑过渡：换主题/深浅/种子色时全角色 `animateAsState`（Material）/ Miuix 侧由 `ThemeController` 处理。

### P2（可选增强）

13. 液态玻璃悬浮底栏 `FloatingBottomBar`（`BloomStroke` + `rememberDeviceTilt`）。
14. 折射玻璃 `Lens` / `Vibrancy` / `CombinedBackdrop`。
15. 预测性返回（`androidx.navigationevent` `NavigationBackHandler`；InstallerX 式 `miuix-nav` 转场）。
16. `PaletteStyle` / `ColorSpec` 设置项（设置页暴露）；AMOLED 纯黑。
17. Vector `SeedScheme` 色盘 `ColorWheel`（主题设置页增强）。

### D（deferred，本阶段不做）

18. 图标取色（`androidx.palette` + `appiconloader`）——门锁列表无 app 图标场景。
19. 大屏自适应 `NavigationRail` / `NavigationSuiteScaffold`——手机单栏场景暂缓。

---

## 7. 目标包结构

```
app/src/main/java/com/yunmei/client/
├── MainActivity.kt                 # enableEdgeToEdge(SystemBarStyle.auto) + ThemeViewModel + 分派
├── ui/
│   ├── AppRoot.kt                  # 顶层 NavDisplay + Pager + 底栏（按皮肤分派）
│   ├── YunMeiTheme.kt              # 主题入口：Miuix / Material 分派（原 Theme.kt 重构）
│   ├── theme/
│   │   ├── ThemeState.kt           # 扩展后的主题状态（+useMiuixMonet/+useBlur）
│   │   ├── MiuixTheme.kt           # ThemeController + MiuixTheme + 状态栏处理（新增）
│   │   ├── MaterialTheme.kt        # material-kolor + MaterialExpressiveTheme（重构自 Monet.kt）
│   │   ├── Monet.kt                # material-kolor 封装：dynamicColorScheme + animateAsState
│   │   ├── PresetColors.kt         # 预设种子色板（保留）
│   │   ├── Color.kt / Shape.kt / Type.kt  # 保留（Material 皮肤 token；MiuiX token 由官方提供后精简）
│   │   └── ThemePreferences.kt     # 持久化（新增 useMiuixMonet/useBlur key）
│   ├── navigation3/                # 新增（蓝本 KernelSU ui/navigation3/）
│   │   ├── Routes.kt               # sealed interface Route : NavKey（Home/Locks/Settings/Login）
│   │   ├── Navigator.kt            # 自定义回退栈 + LocalNavigator + rememberNavigator
│   │   └── MainPagerState.kt       # Pager spring 滑动（可选，从 BottomBar.kt 拆出）
│   ├── components/
│   │   ├── bottombar/
│   │   │   ├── BottomBar.kt        # 分派 Miuix / Material 底栏
│   │   │   ├── BottomBarMiuix.kt   # Miuix NavigationBar（可选悬浮 FloatingBottomBar）
│   │   │   └── BottomBarMaterial.kt# Material ShortNavigationBar
│   │   ├── blend/ColorBlendToken.kt# 玻璃混合色表（用 miuix-blur 的 BlendColorEntry/BlurBlendMode）
│   │   ├── glass/InnerShadow.kt    # 内阴影
│   │   ├── glass/Lens.kt / Vibrancy.kt / CombinedBackdrop.kt  # P2 折射玻璃
│   │   └── effect/BgEffect*.kt     # P2 MIUI 动态背景
│   └── screens/
│       ├── home/HomeScreen.kt       # 适配双皮肤（保留既有功能）
│       ├── locks/LockListScreen.kt  # 门锁列表（保留）
│       ├── login/LoginScreen.kt     # 登录（保留，全屏）
│       └── settings/
│           ├── SettingsScreen.kt    # 分派 SettingsMiuix / SettingsMaterial
│           ├── SettingsMiuix.kt     # Card + preference 分组卡片
│           ├── SettingsMaterial.kt  # Surface + 分段圆角
│           └── SettingsViewModel.kt # 保留既有设置逻辑
├── data/                           # 不动（登录/门锁/二维码/打卡数据层）
└── core/di/AppContainer.kt         # 不动
```

> 注意：现 `ui/components/FrostedBottomBar.kt`、`ui/components/glass/InnerShadow.kt` 已存在，接入 miuix-blur
> 后 `FrostedBottomBar` 由官方 `NavigationBar`+`layerBackdrop` 取代（可删除或保留作 Material 侧 fallback）；
> `glass/InnerShadow.kt` 保留。

---

## 8. 与既有功能适配要点（登录 / 门锁 / 二维码 / 打卡）

1. **零改动区**：`data/`（AccountStore/LockStore/SecureStore/UnlockManager/LocationProvider/YunMeiApi/
   QrCode）、`core/di/AppContainer.kt`、BLE `FastBleLib` 本地 AAR 均不动。
2. **登录**：`LoginScreen` 保持全屏（不挂底栏），仅主题随皮肤切换；`TextField`/`Button` 映射到 Miuix
   `TextField`/`Button`（或保持 Material3 组件，两者可混用但不建议——登录页建议统一皮肤控件）。
3. **门锁列表**：`LockListScreen` 是顶层 Tab 之一，进 Pager；扫码/手动导入/获取开门密码逻辑不动，入口按钮
   样式随皮肤。
4. **二维码**：`QrCode`/zxing 逻辑不动；扫码页面保持全屏 push。
5. **设置项**：既有开关（快速连接/自动开门/自动退出/同时获取密码/隐藏获取密码/隐藏打卡/强制竖屏）全部
   迁移到 Miuix `SwitchPreference` 分组卡片，回调签名不变（`(Boolean) -> Unit`）。
6. **主题切换即时生效**：`ThemeViewModel.themeState` 已是 StateFlow，`MainActivity` 已 collect；只需确保
   Miuix/Material 两分支都用同一 `ThemeState` 数据源，切换时 `MiuixTheme` 的 `controller` 用 `remember` +
   正确 key 重建（参考 KernelSU `MiuixKernelSUTheme` 的 `ThemeController(...)` 直接构造、InstallerX 用
   `remember(colorSchemeMode, keyColor, ...)`）。

---

## 9. 风险与开放项

1. **material3 升级到 1.5.0-alpha26 是 `MaterialExpressiveTheme` 公开可用的前提**（当前 1.4.0 为 internal）。
   升级可能带来 API 微调，t3/t4 需同步验证 `assembleDebug`。
2. **`miuix-nav`（InstallerX 预测性返回）在 0.9.3 是否可用未确认**：KernelSU 0.9.3 用的是
   `androidx.navigation3` 而非 `miuix-nav`；若需要 `miuix-nav`，以 compose-miuix-ui 仓库 README 的
   稳定版本为准（本机无外网，未能实时核对）。
3. **双皮肤组件混用风险**：Miuix 皮肤若混用 Material3 控件，观感不一致。建议每页明确「皮肤 → 控件」映射，
   登录/设置/首页优先统一。
4. **`FloatingBottomBar`/`Lens` 等液态玻璃依赖 `miuix-blur` 的 `Backdrop` 采样**，性能开销高于普通模糊，
   低端机/无 RuntimeShader 需 `isRuntimeShaderSupported()` 兜底（minSdk 26）。
5. **Miuix `NavigationBar` 的 `NavigationItem(label, icon)` 与 badge 需要字符串资源 + ImageVector**，
   现工程底栏用硬编码中文，迁移时改为 `stringResource` + 图标资源（对齐参考 App）。

---

## 10. 参考文件索引（工作区相对路径）

**KernelSU-main**（第一蓝本，miuix 0.9.3）
- `manager/gradle/libs.versions.toml`（miuix/material3/material-kolor/navigation3 版本矩阵）
- `manager/app/src/main/java/me/weishu/kernelsu/ui/theme/{Theme,MiuixTheme,MaterialTheme,ThemeExt,Colors,Type}.kt`
- `manager/app/src/main/java/me/weishu/kernelsu/ui/MainActivity.kt`（navigation3 NavDisplay + Pager + 底栏分派）
- `manager/app/src/main/java/me/weishu/kernelsu/ui/navigation3/{Routes,Navigator,IntentDispatcher}.kt`
- `manager/app/src/main/java/me/weishu/kernelsu/ui/component/bottombar/{BottomBar,BottomBarMiuix,BottomBarMaterial,NavigationRailMiuix,NavigationRailMaterial}.kt`
- `manager/app/src/main/java/me/weishu/kernelsu/ui/component/miuix/effect/{ColorBlendToken,DeviceType,BgEffect*}.kt`
- `manager/app/src/main/java/me/weishu/kernelsu/ui/component/liquid/{Lens,Vibrancy,InnerShadow,CombinedBackdrop}.kt`
- `manager/app/src/main/java/me/weishu/kernelsu/ui/screen/settings/{SettingsScreen,SettingsMiuix,SettingsMaterial}.kt`

**InstallerX-Revived-main**（增强参考，miuix 0.9.4-SNAPSHOT）
- `gradle/libs.versions.toml`（miuix 全家桶坐标）
- `app/src/main/java/com/rosan/installer/ui/theme/{InstallerTheme,Color,Shape,Type,MiuixSheetColors}.kt`
- `app/src/main/java/com/rosan/installer/ui/theme/material/{ThemeExt,PresetColors}.kt`
- `app/src/main/java/com/rosan/installer/ui/navigation/{InstallerNavContainer,MiuixNavWrapper,Navigator,Routes}.kt`
- `app/src/main/java/com/rosan/installer/ui/library/{blend/ColorBlendToken.kt,liquid/*,effect/*,FloatingBottomBar.kt}`
- `app/src/main/java/com/rosan/installer/domain/settings/model/preferences/theme/{ThemeMode,ThemeColorSpec,ThemeState}.kt`

**Vector-master**（Material 侧 Monet 参考）
- `manager-ui/src/main/kotlin/org/matrix/vector/ui/theme/{SeedScheme,ThemeMode,AmoledScheme}.kt`
- `manager-ui/src/main/kotlin/org/matrix/vector/ui/ColorWheel.kt`
- `manager-ui/src/main/kotlin/org/matrix/vector/ui/navigation/{PanelBar,FloatingPanelNav,NavPanels}.kt`

**本工程现状**
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/{AppRoot.kt,ThemeViewModel.kt}`
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/theme/{Theme,ThemeState,Monet,PresetColors,Color,Shape,Type,ThemePreferences}.kt`
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/components/{FrostedBottomBar.kt,blend/ColorBlendToken.kt,glass/InnerShadow.kt}`
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/screens/{home,login,locks,settings}/*`
- `YunMeiAndroid/gradle/libs.versions.toml`、`YunMeiAndroid/app/build.gradle.kts`
