# 云莓智能 Android —— 统一主题框架调研与适配方案

> 调研对象：LSPosed-master（官方旧版/已闭源）、Vector-master（开源 fork，UI 以代码为准）、
> KernelSU-main、InstallerX-Revived-main。目标工程：`YunMeiAndroid`（AGP 8.13.2 / compileSdk 36 /
> Kotlin 2.3.20 / Compose BOM 2026.03.00 → material3 1.4.0）。
>
> 本文是 `docs/ui-patterns.md` 的唯一权威来源，供后续实现任务（Material 分支 Monet 动态取色、
> MiuiX/MIUI 分支毛玻璃底栏）直接落地。文中所有路径均为工作区相对路径，可直接跳转。

---

## 0. 结论摘要（TL;DR）

1. **Monet / Material You 取色、MIUI/HyperOS 风格、毛玻璃/半透明底栏，确实同属一套“统一主题框架”**：
   参考项目的共同做法是「**一个取色引擎（HCT/CIE LCh） + 一套可切换的“风格皮肤” + 一组可复用的
   玻璃/模糊底层原语**」，而不是三个独立功能。

2. **推荐取色引擎：`com.materialkolor:material-kolor`，版本锁定 `4.1.1`**。KernelSU 与
   InstallerX Revived 两个 Compose 参考项目都用它（`rememberDynamicColorScheme` /
   `dynamicColorScheme` + `PaletteStyle` + `ColorSpec.SpecVersion`）。**`material-kolor 4.1.1` 的
   Android AAR 实测 `minCompileSdk=1`，与 AGP 8.13.2 / compileSdk 36 兼容**；参考项目所用的
   `5.0.x` 实测 `minCompileSdk=37`，本工程**不可用**（见 §4 证据）。零依赖替代方案是 Vector 的
   `SeedScheme`（自研 CIE LCh，约 297 行，无外部依赖）。

3. **官方 `top.yukonga.miuix.kmp:miuix-ui`（0.9.x）暂不接入**：其 Android AAR 实测
   `minCompileSdk=37`（见 §4 证据），与本工程 AGP 8.13.2（最高 compileSdk 36）**硬冲突**。
   MiuiX/MIUI 分支继续走「**Material3 基础设施 + MIUI/HyperOS token 近似 + 本地可移植的玻璃原语**」路线。

4. **毛玻璃/半透明底栏不依赖 Miuix 库也能做**：InstallerX 的 `ColorBlendToken`（模糊混合色表）与
   `ui/library/liquid/`、`ui/library/effect/`（Lens/Vibrancy/InnerShadow/BgEffect）均为
   Apache-2.0、Compose-only 代码，可选择性移植；`InnerShadow` 与 `BgEffect` 是**自包含**的（仅依赖
   Compose `Modifier.Node` / `RuntimeShader`），可直接落地。真正依赖 `miuix-blur` 的只有
   `Lens`/`Vibrancy`/`CombinedBackdrop`（它们 import `top.yukonga.miuix.kmp.blur.*` 的
   `Backdrop`/`BackdropEffectScope`）。

5. **material3 1.4.0（本工程 BOM 2026.03.00 已映射）自带 `MaterialExpressiveTheme` 与
   `MotionScheme.expressive()`**（已从本地 AAR classes.jar 验证），Material 分支可直接对标
   InstallerX/Vector 的“Expressive + 动态取色”，无需升 material3 到 1.5.0-alpha。

---

## 1. 四个参考项目的主题框架横评

| 项目 | UI 栈 | 取色引擎（Monet/Material You） | MIUI/HyperOS 风格 | 毛玻璃/底栏 | 关键结论 |
| --- | --- | --- | --- | --- | --- |
| **LSPosed-master** | 经典 View + Material Components（XML 主题 overlay） | `DynamicColors.isDynamicColorAvailable()` + `@android:color/system_accent1_*`（Android 12+ 墙纸取色），预设色用 `ThemeOverlay_Material*` 系列 | 无（官方旧版只做 Material 预设色） | 无 | 只看“Monet 种子 + 预设色板 + 跟随系统强调色”这个**交互模式**，实现不可直接照搬（View 体系） |
| **Vector-master**（fork） | Compose + `MaterialExpressiveTheme` + `MotionScheme.expressive()` | 内置 `dynamicDark/LightColorScheme`（墙纸）+ **自研 `SeedScheme`**（CIE LCh，零依赖生成任意种子的色板，含 AMOLED） | 无（Material 单风格） | 自定义 `FloatingPanelNav` 等 | **`SeedScheme` 是零依赖 Monet 参考实现**，还带 `ColorWheel`（HCT 色相/彩度取色盘） |
| **KernelSU-main** | Compose + Material3 + Miuix **双风格** | `com.materialkolor:material-kolor`（`rememberDynamicColorScheme`、`PaletteStyle`、`ColorSpec.SpecVersion`、AMOLED） | 官方 `top.yukonga.miuix.kmp` 0.9.3（`MiuixTheme` + `ThemeController`） | `LocalEnableFloatingBottomBar` / `LocalEnableFloatingBottomBarBlur` + `ColorBlendToken` | **最接近本工程架构的“统一主题框架”范本**：`ColorMode`（System/Light/Dark/Monet*/AMOLED）+ `keyColor` + `paletteStyle` + `colorSpec` |
| **InstallerX-Revived-main** | Compose + Material3 **Expressive** + Miuix KMP **双风格** | `com.materialkolor:material-kolor` 5.0.1 + `MonetCompat` + `androidx.palette`（图标取色） | 官方 `top.yukonga.miuix.kmp` 0.9.4-SNAPSHOT（`MiuixTheme` + `ThemeController`，含 `useMiuixMonet`） | `ColorBlendToken` + `ui/library/liquid/`（Lens/Vibrancy/InnerShadow，移植自 Kyant0/AndroidLiquidGlass）+ `ui/library/effect/`（MIUI OS2/OS3 动态渐变背景） | **玻璃/模糊效果最全、可移植性最强的参考**；`ThemeState` 是最完整的主题状态模型 |

### 1.1 统一主题框架的三层结构（综合 KernelSU + InstallerX）

```
统一主题框架
├─ ① 取色引擎（Monet / Material You）
│    └─ material-kolor（HCT）：seedColor → ColorScheme
│        ├─ 来源：墙纸动态色 / 用户预设种子色 / 图标取色（palette）
│        ├─ PaletteStyle：TonalSpot / Vibrant / Expressive / Neutral / Rainbow / FruitSalad / Monochrome / Fidelity / Content
│        └─ ColorSpec：SPEC_2021（Material 3）/ SPEC_2025（Expressive）
├─ ② 风格皮肤（Style Skin，可切换）
│    ├─ Material 皮肤：MaterialExpressiveTheme + MotionScheme.expressive()
│    └─ MIUI/HyperOS 皮肤：MiuixTheme（官方库）或 “Material3 基础设施 + MIUI token 近似”（本工程）
└─ ③ 玻璃/模糊原语（Glass / Blur Primitives）
     ├─ 颜色混合 token：ColorBlendToken（Info/Colored/Pured/Overlay × 深浅 × 厚度）
     ├─ 模糊/折射：Backdrop + Lens（圆角折射 + 色散）+ Vibrancy（饱和度/亮度/对比）
     ├─ 内阴影：InnerShadow（自包含 Modifier.Node）
     └─ 动态渐变背景：BgEffect（OS2/OS3 RuntimeShader，可动画）
```

---

## 2. 统一主题状态模型（建议直接采用 InstallerX 的 `ThemeState` 形态）

InstallerX 的 `ThemeState` 是三个 Compose 项目里最完整的主题状态，建议本工程以它为蓝本演进：

```kotlin
// 参考: InstallerX-Revived-main/app/src/main/java/com/rosan/installer/domain/settings/model/preferences/ThemeState.kt
data class ThemeState(
    val isLoaded: Boolean = false,
    val useMiuix: Boolean = false,          // 对应本工程 ThemeMode.MATERIAL3 / MIUIX
    val themeMode: ThemeMode = ThemeMode.SYSTEM,  // LIGHT / DARK / SYSTEM
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025, // SPEC_2021 / SPEC_2025
    val useDynamicColor: Boolean = true,    // 是否用墙纸动态取色（Android 12+）
    val useMiuixMonet: Boolean = false,     // MIUI 皮肤是否也走 Monet 种子
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
)
```

本工程当前 `ThemeMode { MATERIAL3, MIUIX }` + `darkTheme: Boolean = isSystemInDarkTheme()`（见
`app/src/main/java/com/yunmei/client/ui/theme/Theme.kt`）是上述模型的**最小子集**。升级路径：

- 把 `ThemeMode` 扩为「风格 × 深浅 × 取色」三维：
  - 风格：`MATERIAL3` / `MIUIX`（沿用现状）
  - 深浅：`SYSTEM` / `LIGHT` / `DARK`（对标 `ThemeMode` LIGHT/DARK/SYSTEM）
  - 取色：`dynamicColor: Boolean`、`seedColor: Int`、`paletteStyle`、`colorSpec`（新增）
- 持久化沿用 `AppPreferences` 的 DataStore（新增 key：`theme_dark_mode`、`theme_dynamic_color`、
  `theme_seed_color`、`theme_palette_style`、`theme_color_spec`）。

---

## 3. 统一适配方案（分阶段）

### 阶段 A（立即，不迁移构建链）—— 统一取色 + Material 皮肤 Monet

1. **引入取色引擎 `com.materialkolor:material-kolor:4.1.1`**（`minCompileSdk=1`，兼容 compileSdk 36；
   `5.0.x` 需 compileSdk 37，勿用）：
   - Material 分支：用 `rememberDynamicColorScheme(seedColor, isDark, style, specVersion)` 生成
     `ColorScheme`，替代当前写死的 `MaterialLightColors` / `MaterialDarkColors`。
   - 种子来源优先级：`useDynamicColor && SDK>=31` → 墙纸动态色（`dynamicLight/DarkColorScheme` 的
     primary 作为 seed，KernelSU `ThemeExt.kt` 的做法）；否则用用户预设种子色；再否则默认种子。
   - 预设色板直接搬 InstallerX `PresetColors.kt`（17 色，含本工程当前主色 `0xFF00639B` light_blue）。

2. **Material 皮肤升级到 `MaterialExpressiveTheme` + `MotionScheme.expressive()`**：
   material3 1.4.0 已含该 API（已从本地 AAR 验证），对标 Vector/InstallerX 的动效观感。
   平滑过渡复用 KernelSU/InstallerX 的 `ColorScheme.animateAsState()`（逐角色 `animateColorAsState`）。

3. **主题入口重构为 `YunMeiTheme(themeState, content)`**（参考 InstallerX `InstallerTheme`）：
   通过 `CompositionLocalProvider` 暴露 `LocalIsDark` / `LocalSeedColor` / `LocalColorScheme` 等，
   并处理状态栏图标深浅（`WindowCompat.getInsetsController(...).isAppearanceLightStatusBars`）与
   Android 10+ 导航栏对比度（`window.isNavigationBarContrastEnforced = false`）。

### 阶段 B（立即，不迁移构建链）—— MiuiX 皮肤 + 毛玻璃底栏

MiuiX 分支保持「Material3 基础设施 + MIUI token 近似」（现状），叠加以下本地组件：

1. **新增毛玻璃底栏组件 `FrostedBottomBar`**：
   - 当前 App 无 `NavigationBar`（导航靠首页按钮，见 `AppRoot.kt`），底栏是**新组件**。
   - 半透明 + 模糊：优先用 `dev.chrisbanes.haze:1.7.0`（Compose 背景模糊标准库，`hazeSource`/
     `hazeEffect`，实测 `minCompileSdk=1`，兼容 compileSdk 36；`1.7.3` 需 compileSdk 37，勿用）；若不想加依赖，退化为 `graphicsLayer` +
     `RenderEffect.createBlurEffect` 的简化背景模糊。
   - 模糊混合色表用 `ColorBlendToken`（移植，见 §5.4）：浅色用 `Info_*_Light` 系列叠加
     `PlusLighter`/`PlusDarker`，暗色用 `Info_*_Dark`。
   - 底栏形态对标 KernelSU 的 `LocalEnableFloatingBottomBar`（悬浮胶囊）+ InstallerX 的
     `useAppleFloatingBar`（可选）。

2. **移植自包含玻璃原语**（Apache-2.0，仅依赖 Compose，可直接落地）：
   - `InnerShadow`（`ui/library/liquid/InnerShadow.kt`，自包含 `Modifier.Node`，卡片/底栏内阴影）。
   - `BgEffect`（`ui/library/effect/*.kt` + `OS2BgFrag`/`OS3BgFrag`，MIUI OS2/OS3 动态渐变背景，
     RuntimeShader，仅 API 33+ 生效并需 `isRuntimeShaderSupported()` 兜底）。

3. **暂缓移植（依赖 miuix-blur 的 `Backdrop` 原语）**：
   - `Lens` / `Vibrancy` / `CombinedBackdrop` 依赖 `top.yukonga.miuix.kmp.blur.*`，若不接 Miuix 库，
     需额外自研/移植 `Backdrop` 采样原语，成本高；建议列为“进阶玻璃效果”选项，不在阶段 B 强求。

### 阶段 C（可选，未来迁移）—— 换用官方 Miuix KMP 全家桶

当需要 100% MIUI/HyperOS 组件（`SuperNavBar`/`SuperSwitch`/`SuperDialog`/`MiuixPreference` 等）
且愿意接受构建链升级时，整体迁到：

- AGP 9.x + Gradle 9 + Kotlin 2.4 + compileSdk 37（参考 KernelSU：AGP 9.3.2 / Kotlin 2.4.10 /
  compileSdk 37 / miuix 0.9.3；InstallerX：AGP 9.3.2 / Kotlin 2.4.10 / miuix 0.9.4-SNAPSHOT）。
- 届时把阶段 B 的本地玻璃组件替换为 `miuix-blur` / `miuix-shader` 的官方实现，`ThemeController`
  直接对齐 InstallerX `InstallerMiuixTheme`。

---

## 4. compose-miuix-ui/miuix 与 AGP 8.13.2 / compileSdk 36 的兼容性评估（含证据）

### 4.1 结论

**不兼容，需整条升级到 compileSdk 37 + AGP 9.x。** 本工程保持 AGP 8.13.2 / compileSdk 36 时
**不能**直接依赖官方 `top.yukonga.miuix.kmp:miuix-*` 0.9.x。

### 4.2 证据（本地 Gradle 缓存 AAR 元数据实测）

`top.yukonga.miuix.kmp:miuix-ui-android:0.9.0` 的 `META-INF/com/android/build/gradle/aar-metadata.properties`：

```
aarFormatVersion=1.0
aarMetadataVersion=1.0
minCompileSdk=37
minCompileSdkExtension=0
minAndroidGradlePluginVersion=1.0.0
```

`miuix-core-android:0.9.0` 与 `miuix-shapes-android:0.9.0` 同样 `minCompileSdk=37`。
AGP 会在依赖解析时校验 AAR 的 `minCompileSdk`，超过当前 `compileSdk=36` 直接报错。

对照证据 —— 取色引擎 `com.materialkolor:material-kolor` 的 AAR 元数据（实测）：

```
material-kolor:4.1.1  → minCompileSdk=1   # 与本工程兼容
material-kolor:5.0.x  → minCompileSdk=37  # 需 compileSdk 37，本工程不可用
```

玻璃模糊库 `dev.chrisbanes.haze` 的 AAR 元数据（实测）：

```
haze:1.7.0 → minCompileSdk=1   # 与本工程兼容
haze:1.7.3 → minCompileSdk=37  # 需 compileSdk 37，本工程不可用
```

> 本机 Gradle 缓存中可确认 `com.materialkolor:material-color-utilities-android:4.1.1`（material-kolor
> 4.1.1 的底层引擎）为 `minCompileSdk=1`；`material-kolor` 各版本与 `haze` 各版本的 `minCompileSdk`
> 为 AAR 元数据实测值。

### 4.3 两个 Compose 参考项目的版本组合（作为“迁移到 Miuix”的唯一已知可行组合）

| 项目 | AGP | Kotlin | compileSdk | material3 | material-kolor | miuix |
| --- | --- | --- | --- | --- | --- | --- |
| KernelSU-main | 9.3.2 | 2.4.10 | 37 | 1.5.0-alpha26 | 5.0.0 | 0.9.3 |
| InstallerX-Revived-main | 9.3.2 | 2.4.10 | 37（buildTools 37） | 1.5.0-alpha27 | 5.0.1 | 0.9.4-103b737b-SNAPSHOT |

> 说明：InstallerX 的 miuix 是 GitHub Packages 私有源 + mavenLocal 的 SNAPSHOT，官方公开版本应查
> `compose-miuix-ui/miuix` 主仓库 README 的稳定版本号；KernelSU 使用的 `0.9.3` 是可解析的稳定号。
> （本机无外网，无法实时核对最新 release；落地前以仓库 README 为准。）

### 4.4 取舍

| 方案 | 优点 | 代价 | 建议 |
| --- | --- | --- | --- |
| A. 保持 AGP 8.13.2/compileSdk 36 + material-kolor 4.1.1 + haze 1.7.0 + 本地玻璃原语 | 零构建链风险，立即可做 | MIUI 组件需手写/移植，非 100% 官方观感 | **首选（阶段 A+B）** |
| B. 升级 AGP 9.x/Gradle 9/Kotlin 2.4/compileSdk 37 + 官方 miuix | 观感与 KernelSU/InstallerX 完全一致，组件齐全 | 大规模构建链迁移，风险集中在 Gradle 9 配置缓存/插件兼容 | 作为未来可选（阶段 C） |

---

## 5. Tokens 清单

### 5.1 颜色 token（Monet 生成 + 皮肤静态色）

**Monet 引擎产物（material-kolor）** 覆盖 Material3 全色板角色（含 1.4.0 新增的
`primaryFixed*`/`secondaryFixed*`/`tertiaryFixed*`/`surfaceBright`/`surfaceDim`/
`surfaceContainer*`）。不要手写这 30+ 角色，交由 `dynamicColorScheme()` 生成；`animateAsState()`
逐角色做平滑过渡（参考 `InstallerX-Revived-main/.../ui/theme/material/ThemeExt.kt` 与
`KernelSU-main/.../ui/theme/ThemeExt.kt` 的完整角色列表）。

**预设种子色板**（用户可选，移植 `InstallerX-Revived-main/.../ui/theme/material/PresetColors.kt`）：

```
default 0xFF4A672D   pink 0xFFB94073   red 0xFFBA1A1A   orange 0xFF944A00
amber 0xFF8C5300     yellow 0xFF795900  lime 0xFF5E6400  green 0xFF006D39
cyan 0xFF006A64      teal 0xFF006874   light_blue 0xFF00639B  blue 0xFF335BBC
indigo 0xFF5355A9    purple 0xFF6750A4 deep_purple 0xFF7E42A4 blue_grey 0xFF575D7E
brown 0xFF7D524A     grey 0xFF5F6162
```

**MiuiX 静态皮肤色**（本工程已有，见 `app/src/main/java/com/yunmei/client/ui/theme/Color.kt`）：
主色 `#1677FF`（HyperOS 蓝）、浅底 `#F5F5F5`、面 `#FFFFFF`、`surfaceVariant #F2F3F5`、
描边 `#E0E0E0`、错误 `#E5484D`；暗色主色 `#6AA8FF`、底 `#111111`、面 `#1A1A1A`、
`surfaceVariant #2A2A2A`、描边 `#3A3A3A`。可选加入 InstallerX 的 `miuixSheetColorLight #F7F7F7` /
`miuixSheetColorDark #242424` / `miuixSheetCardColorDark #434343`。

### 5.2 形状 token

- Material 皮肤（现状 `AppShapes`）：small 8dp / medium 12dp / large 16dp。
- MiuiX 皮肤（现状 `MiuiXShapes`）：small 6dp / medium 12dp / large 16dp。
- 分段列表形状（移植 InstallerX `Shape.kt`）：`CornerRadius=16dp`、`ConnectionRadius=5dp`，
  生成 `topShape/middleShape/bottomShape/singleShape`（用于 MIUI 风格分组卡片列表）。

### 5.3 排版 token

沿用现状 `AppTypography`（Material）/ `MiuiXTypography`（更紧凑、字重偏 Medium）。Expressive 动效下
可补充 `MotionScheme.expressive()`（material3 1.4.0 已提供，无需自造 token）。

### 5.4 玻璃/模糊混合 token（`ColorBlendToken`）

移植自 `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/library/blend/ColorBlendToken.kt`
与 `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/component/miuix/effect/ColorBlendToken.kt`
（两处内容一致，均 Apache-2.0）。核心是「**颜色 + `BlurBlendMode`（PlusLighter/PlusDarker/Overlay/
ColorDodge/ColorBurn/SrcOver/Luminosity/SoftLight/HardLight）的组合列表**」，按用途分四族 × 深浅 × 厚度：

- **Info 族**（信息/底栏半透明遮罩）：`Info_Extra_Thin` / `Info_Thin` / `Info_Regular` / `Info_Thick`（Light/Dark）。
- **Colored 族**（彩色玻璃）：`Colored_Extra_Thin` … `Colored_Extra_Thick`（Light/Dark）。
- **Pured 族**（纯净/镜面玻璃）：`Pured_Extra_Thin` … `Pured_Extra_Thick`（Light/Dark）。
- **Overlay 族**（压暗/提亮遮罩）：`Overlay_Extra_Thin` … `Overlay_Extra_Thick`（Light/Dark）。

移植注意：`BlendColorEntry` / `BlurBlendMode` 来自 `miuix-blur`。若阶段 B 不接 Miuix 库，需
**本地重定义这两个类型**（枚举 + 数据类，逻辑等价）后再使用色值表；色值表本身无任何外部依赖。

---

## 6. 组件清单（Compose）

> 来源缩写：IX=InstallerX-Revived-main、KS=KernelSU-main、V=Vector-master。
> “落地方式”＝直接移植 / 需本地重定义依赖类型 / 用第三方库替代。

| 组件 | 参考实现 | 落地方式 | 用于 |
| --- | --- | --- | --- |
| 主题入口 `YunMeiTheme` | IX `InstallerTheme.kt` | 重构（CompositionLocal + 状态栏处理） | 全局 |
| 取色生成 `dynamicColorScheme` + `animateAsState` | IX/K S `ui/theme/material/ThemeExt.kt` | 用 material-kolor 4.1.1 | 全局 |
| 预设色板 `PresetColors` | IX `ui/theme/material/PresetColors.kt` | 直接移植 | 主题设置页 |
| 色盘/取色器 `ColorWheel` | V `manager-ui/.../ui/ColorWheel.kt` + `SeedScheme.kt` | 移植（可选） | 主题设置页 |
| 零依赖 Monet `SeedScheme` | V `manager-ui/.../ui/theme/SeedScheme.kt` | 移植（作为 material-kolor 备选） | 取色引擎 |
| 毛玻璃底栏 `FrostedBottomBar` | KS `LocalEnableFloatingBottomBar` 思路 + IX 玻璃原语 | 新写（haze 1.7.0 或 RenderEffect）+ ColorBlendToken | MiuiX 底栏 |
| 模糊混合色表 `ColorBlendToken` | IX/K S `.../blend/ColorBlendToken.kt` | 移植（重定义 BlendColorEntry/BlurBlendMode） | 底栏/卡片玻璃 |
| 内阴影 `InnerShadow` | IX `ui/library/liquid/InnerShadow.kt` | 直接移植（自包含） | 卡片/底栏 |
| 液滴玻璃 `Lens` / `Vibrancy` / `CombinedBackdrop` | IX `ui/library/liquid/*.kt` | 暂缓（依赖 miuix-blur `Backdrop`） | 进阶玻璃（可选） |
| MIUI 动态渐变背景 `BgEffect` | IX `ui/library/effect/*.kt` | 移植（自包含 RuntimeShader，API33+ 兜底） | MiuiX 背景 |
| 主题状态模型 `ThemeState` | IX `domain/.../theme/ThemeState.kt` | 重构 `ThemeMode` | 偏好/VM |
| 主题设置页 | IX `ui/page/miuix/settings/preferred/theme/*.kt` | 参考结构 | 设置页新增 |

### 6.1 建议新增的包结构（对齐参考项目）

```
app/src/main/java/com/yunmei/client/ui/
├── theme/
│   ├── YunMeiTheme.kt          # 入口：CompositionLocal + 状态栏
│   ├── ThemeState.kt           # 主题状态模型（风格/深浅/取色/种子/样式）
│   ├── Monet.kt                # material-kolor 4.1.1 封装：dynamicColorScheme + animateAsState
│   ├── PresetColors.kt         # 预设种子色板
│   ├── Color.kt / Shape.kt / Type.kt   # 保留，静态皮肤 token
└── library/
    ├── blend/ColorBlendToken.kt   # 玻璃混合色表
    ├── glass/InnerShadow.kt        # 内阴影（自包含）
    ├── glass/Lens.kt / Vibrancy.kt # 进阶玻璃（阶段 C）
    └── effect/BgEffect*.kt         # MIUI 动态渐变背景（阶段 B 可选）
```

---

## 7. 关键实现要点（落地时对照）

1. **状态栏/导航栏**（IX `InstallerTheme.kt`）：`WindowCompat.getInsetsController(window, view)
   .isAppearanceLightStatusBars = !darkTheme`；Android 10+ `window.isNavigationBarContrastEnforced = false`
   让底栏玻璃不被系统强制对比度破坏。
2. **墙纸动态取色**（KS `ThemeExt.kt`）：`seed = if (seedColor == Color.Unspecified)
   (dynamicDarkColorScheme(ctx) 或 dynamicLightColorScheme(ctx)).primary else seedColor`——即用系统 Monet
   的 primary 反推 seed，再交给 material-kolor 生成整套（而非直接用内置 `dynamic*ColorScheme`，因为后者无法
   调 paletteStyle/spec/对比度）。
3. **SPEC_2025 只在支持的 style 生效**（IX `ThemeExt.kt` / KS `Theme.kt`）：`SPEC_2025` 需
   `style.supportsSpec2025`（TonalSpot/Neutral/Vibrant/Expressive），否则回退 `SPEC_2021`。
4. **平滑过渡**：整个 `ColorScheme` 逐角色 `animateColorAsState(animationSpec = spring())`，避免换主题闪变。
5. **玻璃效果 API 兜底**：`isRuntimeShaderSupported()` 为 false 或 `SDK < 33` 时退化为纯色/半透明
   （IX `Lens.kt` / `BgEffectBackground.kt` 的做法），保证 minSdk 26 不崩。

---

## 8. 风险与开放项

1. **版本必须锁定到 `minCompileSdk=1` 的旧版**：`material-kolor 4.1.1`、`haze 1.7.0` 兼容 compileSdk 36；
   `material-kolor 5.0.x`、`haze 1.7.3` 实测 `minCompileSdk=37`，本工程不可用（参考项目 KernelSU/InstallerX
   之所以在 compileSdk 37，正因用了这些新版本）。落地时需在有网机器 `./gradlew :app:dependencies` 确认并
   **显式固定** `material-kolor` 到 `4.1.1`、`haze` 到 `1.7.0`，防止依赖解析漂移到 5.0.x / 1.7.3。
   本机无外网（firecrawl/curl 均失败），上述 `minCompileSdk` 为 AAR 元数据实测值。
2. **`Lens`/`Vibrancy` 依赖 `miuix-blur` 的 `Backdrop` 原语**：不接 Miuix 库时需要自研背景采样
   （或改用 `haze`），成本明显高于 `InnerShadow`/`BgEffect`，建议只做「半透明 + 模糊 + 混合色」而非
   完整折射玻璃。
3. **阶段 C 迁移面**：AGP 9.x 移除/变更了若干旧 API（configuration cache、parallel、`package` 块、
   `dependenciesInfo` 等），InstallerX 已在 AGP 9.3.2 上踩平；迁移时以其 `build.gradle.kts` 为模板。
4. **MiuiX 皮肤目前仍是“近似”**：纯 token 无法复刻官方 `SuperNavBar` 的完整交互（按压动效、状态层、
   弹簧动画），接受“观感对标”而非“像素级一致”。

---

## 9. Material 侧完整视觉与交互特性清单（MD 完整清单 + 优先级）

> 覆盖 LSPosed / Vector / KernelSU / InstallerX Revived 四个参考项目在 **Material 侧**的全部视觉与交互特性。
> 可落地性判定基准：本工程 **compileSdk 36 / material3 1.4.0 / material-kolor 4.1.1 / haze 1.7.0**。
> MiuiX 侧（官方 Miuix 组件、miuix-blur 玻璃、miuix-nav 转场）统一标注 `deferred`。
> 优先级：**P0** 立即做 · **P1** 紧随 · **P2** 可选 · **D** deferred。

### 9.1 色彩标准 / 色彩风格

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| ColorScheme 全角色（含 `primaryFixed*`/`secondaryFixed*`/`tertiaryFixed*`、`surfaceBright/Dim/Container*`） | IX/KS `ThemeExt.kt` 的 `animateAsState()` 逐角色列表 | ✅ material3 1.4.0 已含全部角色 | P0 | 不手写，交由 `dynamicColorScheme()` 生成 + 逐角色动画 |
| `PaletteStyle`（TonalSpot/Vibrant/Expressive/Neutral/Rainbow/FruitSalad/Monochrome/Fidelity/Content） | material-kolor | ✅ 4.1.1 提供 | P1 | 暴露为设置项，默认 TonalSpot |
| `ColorSpec` 2021 / 2025（Expressive） | IX `ThemeColorSpec` / KS `ColorSpec.SpecVersion` | ✅ 4.1.1 提供；2025 仅 TonalSpot/Neutral/Vibrant/Expressive 支持，否则回退 2021 | P1 | 与 paletteStyle 联动 |
| 预设种子色板 | IX `PresetColors`（17 色）/ Vector `SeedScheme.PRESETS`（8 色） | ✅ 直接移植（纯常量） | P0 | 优先搬 IX `PresetColors`，含本工程主色 `0xFF00639B` |
| AMOLED 纯黑 | Vector `AmoledScheme.toAmoled()` / KS `amoledBackground()` | ✅ 自包含 `copy()`，只压黑背景角色、保留强调色 | P1 | 暗色模式下可选开关 |
| 高对比 / 对比度 | material-kolor `contrastLevel` 参数（IX `ThemeExt` 默认 0.0） | ✅ 4.1.1 提供 | P2 | 设置项可选，注意 accessibility |

### 9.2 动态取色来源

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| 墙纸动态色（Android 12+） | LSPosed `DynamicColors.isDynamicColorAvailable()`；KS `dynamicLight/DarkColorScheme(...).primary` 反推 seed | ✅ material3 内置 + material-kolor | P0 | `useDynamicColor && SDK>=31` → 取系统 Monet primary 作 seed |
| 用户种子色（预设 + 色盘） | Vector `ColorWheel`（HCT 色相/彩度盘）+ `SeedScheme`；IX `PresetColors` | ✅ 自研/移植，零外部依赖 | P0 | 预设色板必做；`ColorWheel` 为 P2 增强 |
| 图标取色 | IX `GetAppIconColorUseCase` + `appiconloader` + `androidx.palette` + `MonetCompat` | ⚠️ 需引 `me.zhanghai.android.appiconloader` + `androidx.palette`；MonetCompat 版本未验证 compileSdk | P2 | 本工程门锁列表无 app 图标场景，可暂缓 |

### 9.3 Expressive motion / 主题切换动画

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| `MotionScheme.expressive()` | Vector/IX `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())` | ✅ material3 1.4.0 已含（本地 AAR 验证） | P0 | Material 皮肤直接上 `MaterialExpressiveTheme` |
| 主题切换平滑过渡（全角色 `animateColorAsState(spring)`） | IX/KS `ThemeExt.kt` `ColorScheme.animateAsState()` | ✅ 直接移植 | P0 | 换主题/深浅/种子时不闪变 |
| 动画头部 Ambience（Circuit/Matrix/Maze/Snow 渲染） | Vector `manager-ui/.../ambience/*.kt` | ⚠️ 自研 Canvas，可移植但工作量大 | P2 | 锦上添花，暂缓 |

### 9.4 预测性返回手势（Predictive Back）

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| `android:enableOnBackInvokedCallback="true"` | IX `AndroidManifest.xml`、Vector manifest | ✅ 一行 manifest | P1 | 直接加 |
| AOSP 预测性返回（`OnBackInvokedCallback`/`PredictiveBackHandler`/`NavigationBackHandler`） | IX `InstallerNavEntry` 用 `androidx.navigationevent.compose.NavigationBackHandler` + `rememberNavigationEventState` | ⚠️ 需评估：本工程用 `androidx.navigation:navigation-compose` 2.9.5，非 navigation3/navigationevent；activity-compose 1.11.0 已含 `PredictiveBackHandler` | P1 | 优先用 `androidx.activity.compose.PredictiveBackHandler`，避免引 navigationevent |
| AOSP/Scale/Classic/MIUIX 预测性返回转场动画 | IX `ui/animation/predictiveback/*.kt` + `top.yukonga.miuix.kmp.nav` 的 `NavDisplay`/`NavTransition`/`NavGesture` | ⛔ deferred（依赖 miuix-nav，需 compileSdk 37） | D | MiuiX 侧统一 deferred |

### 9.5 edge-to-edge / 状态栏 / 导航栏

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| `enableEdgeToEdge()` 透明系统栏 | KS `MainActivity` `enableEdgeToEdge(SystemBarStyle.auto(transparent,transparent){dark})`；IX `InstallerActivity` | ✅ 本工程已调 `enableEdgeToEdge()`，升级为动态深浅样式 | P0 | 传 `SystemBarStyle.auto`，跟随 `darkTheme` |
| 状态栏图标深浅 `isAppearanceLightStatusBars` | IX `InstallerTheme` SideEffect | ✅ 直接移植 | P0 | `WindowCompat.getInsetsController(window,view)` |
| 导航栏对比度关闭 `isNavigationBarContrastEnforced=false` | IX `NavigationBarContrastHandler`（Android 10+）、KS | ✅ 直接移植 | P1 | 毛玻璃底栏/悬浮底栏必需 |
| 内容 insets（`systemBars.union(displayCutout)`） | KS `BottomBarMaterial` | ✅ 直接 | P1 | 底栏/Scaffold 内边距处理 |

### 9.6 导航形态

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| 底部导航 `ShortNavigationBar`/`ShortNavigationBarItem` + `Badge`/`BadgedBox` | KS `BottomBarMaterial` | ✅ material3 1.4.0 提供 | P1 | 本工程当前无底栏（导航靠按钮），新建即可用此组件 |
| 自适应导航 `NavigationSuiteScaffold` + `NavigationSuiteType`（bar/rail 随窗口） | Vector `VectorApp` + `PanelBar`（`ShortNavigationBarItem`/`WideNavigationRailItem`） | ⚠️ 需引 `androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0` | P2 | 平板/折叠屏才值得，暂缓 |
| 面板可编辑（长按重排/显隐/拖拽） | Vector `PanelBar`/`FloatingNavSettings`/`NavPanels` | ⚠️ 自研，交互复杂 | P2 | 观感提升非必需 |
| 悬浮胶囊底栏（liquid glass，含 BloomStroke/设备倾斜高光） | KS `FloatingBottomBar` + `component/liquid/*` + `top.yukonga.miuix.kmp.blur` | ⛔ deferred（依赖 miuix-blur 的 `Backdrop`/`Highlight`/`LightSource` + 倾斜传感器） | D | Material 侧用 haze 1.7.0 做简化毛玻璃底栏（§3-B） |
| 侧栏 `WideNavigationRailItem` | Vector `PanelBar` | ✅ material3-adaptive 提供 | P2 | 大屏可选 |

### 9.7 组件风格（卡片 / 列表 / 对话框 / 开关 / 滑块）

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| MIUI 风格分段列表圆角（top/middle/bottom/single） | IX `Shape.kt`（`CornerRadius=16dp`/`ConnectionRadius=5dp`） | ✅ 纯 Shape token，直接移植 | P1 | 设置页分组列表用 |
| 卡片 / 列表容器 | 各项目 `Surface` + `surfaceContainer*` 角色 | ✅ material3 1.4.0 | P1 | 保持 Material3 默认 + 角色色 |
| 对话框 `AlertDialog` / 开关 `Switch` / 滑块 `Slider` | 各项目 Material3 默认 | ✅ 本工程已有 | P1 | Material 侧维持默认；MiuiX 侧 deferred |
| 徽标 `Badge`/`BadgedBox` | KS `BottomBarMaterial` | ✅ material3 提供 | P2 | 底栏角标 |
| 无障碍语义 + 触觉反馈（`semantics`/`HapticFeedbackType`） | Vector `PanelBar`（大量语义/自定义动作/触觉） | ✅ 可借鉴 | P2 | 底栏/卡片落地时参考 |

### 9.8 其它外观特性

| 特性 | 参考实现 | 可落地性 | 优先级 | 落地建议 |
| --- | --- | --- | --- | --- |
| 内阴影 `InnerShadow` | IX `ui/library/liquid/InnerShadow.kt`（自包含 `Modifier.Node` + `RenderEffect`） | ✅ 直接移植 | P1 | 卡片/底栏立体感 |
| 页面缩放 `pageScale`（无障碍） | KS `MainActivity` `Density(... * pageScale)` | ⚠️ 可做 | P2 | 设置项可选 |
| MIUI 动态渐变背景 `BgEffect`（OS2/OS3 RuntimeShader） | IX `ui/library/effect/*.kt` | ⚠️ 自包含 RuntimeShader（API33+ 兜底） | D | MiuiX 侧 deferred；Material 侧 P2 可选 |
| 玻璃折射 `Lens`/`Vibrancy`/`CombinedBackdrop` | IX `ui/library/liquid/*.kt`（依赖 miuix-blur `Backdrop`） | ⛔ deferred | D | MiuiX 侧 deferred |

---

## 附录 A：参考文件索引（工作区相对路径）

- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/theme/InstallerTheme.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/theme/material/ThemeExt.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/theme/material/PresetColors.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/theme/{Color,Shape,Type,MiuixSheetColors}.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/library/blend/ColorBlendToken.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/library/liquid/{Lens,Vibrancy,InnerShadow,CombinedBackdrop}.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/library/effect/{BgEffectBackground,BgEffectConfig,BgEffectModifier,BgEffectPainter,OS2BgFrag,OS3BgFrag,DeviceType}.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/domain/settings/model/preferences/theme/{ThemeMode,ThemeColorSpec,ThemeState}.kt`
- `InstallerX-Revived-main/gradle/libs.versions.toml`（miuix/material-kolor/MonetCompat 版本组合）
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/theme/{Theme,ThemeExt,Type}.kt`
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/component/miuix/effect/{ColorBlendToken,DeviceType}.kt`
- `KernelSU-main/manager/gradle/libs.versions.toml`（miuix 0.9.3 / material-kolor 5.0.0 / AGP 9.3.2）
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/theme/{SeedScheme,ThemeMode}.kt`
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/ColorWheel.kt`
- `Vector-master/manager/src/main/kotlin/org/matrix/vector/manager/ui/theme/Theme.kt`
- `LSPosed-master/app/src/main/java/org/lsposed/manager/util/ThemeUtil.java`
- `LSPosed-master/app/src/main/res/values-v31/colors.xml`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/navigation/InstallerNavContainer.kt`（预测性返回 + NavDisplay）
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/animation/predictiveback/{InstallerNavTransition,ScaleNavTransition,NoPredictiveBackTransition}.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/domain/settings/model/preferences/{PredictiveBackAnimation,PredictiveBackExitDirection}.kt`
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/domain/engine/usecase/GetAppIconColorUseCase.kt`（图标取色）
- `InstallerX-Revived-main/app/src/main/java/com/rosan/installer/ui/library/FloatingBottomBar.kt`
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/component/FloatingBottomBar.kt`（liquid glass 底栏）
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/component/bottombar/BottomBarMaterial.kt`（Material 底栏）
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/component/liquid/*.kt`
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/KernelSUApplication.kt`（setEnableOnBackInvokedCallback via HiddenApiBypass）
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/theme/AmoledScheme.kt`
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/appearance/AppearanceSettings.kt`
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/navigation/{FloatingPanelNav,PanelBar,NavPanels,TopLevelDestination}.kt`
- `Vector-master/manager-ui/src/main/kotlin/org/matrix/vector/ui/ambience/*.kt`（动画头部）
- `Vector-master/manager/src/main/kotlin/org/matrix/vector/manager/ui/VectorApp.kt`（NavigationSuiteScaffold）

## 附录 B：本工程现状相关文件

- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/theme/{Theme,Color,Shape,Type}.kt`
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/ThemeViewModel.kt`
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/data/preferences/AppPreferences.kt`（`ThemeMode`）
- `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/AppRoot.kt`（当前无底栏，导航靠按钮）
- `YunMeiAndroid/app/build.gradle.kts`、`YunMeiAndroid/gradle/libs.versions.toml`

---

## 10. 落地实施记录（t4：整体移植/适配）

> 本节为实施任务 t4 的实际落地结果，用于对齐「调研方案」与「已提交代码」的差异。所有结论均以
> `:app:assembleDebug`（compileSdk 36 / AGP 8.13.2）实测为准。

1. **取色引擎最终采用零依赖 `SeedScheme`（CIE LCh，移植自 Vector），未接入 material-kolor**。
   实测 `material-kolor 5.0.1` 传递依赖 `material-color-utilities:5.0.1` 与
   `org.jetbrains.compose.{material3,ui,foundation,runtime}:1.12.0*`，AGP 直接报
   「requires compileSdk 37 / AGP 9.1.0」，与本工程硬冲突；`4.1.1` 亦传递依赖
   `org.jetbrains.compose.material3:material3:1.10.0-alpha05`（比本工程 androidx material3 1.4.0 更新），
   风险不可控。`SeedScheme` 零外部依赖，`assembleDebug` 已通过。

2. **毛玻璃底栏采用 `dev.chrisbanes.haze:haze:1.6.10`**（实测 `minCompileSdk=1`，Kotlin 2.1.0，兼容）。
   1.7.3 实测 `minCompileSdk=37` 不可用。底栏为悬浮胶囊形态，`hazeSource`/`hazeEffect` 做真实背景模糊，
   叠加 `ColorBlendToken` Info 玻璃混合色与半透明 `surface`。

3. **material3 1.4.0 的 `MaterialExpressiveTheme` / `MotionScheme` 为 `internal`**（Kotlin 可见性），
   不可在本工程公开调用；Material 分支继续用 `MaterialTheme`（Monet 色板 + `animateAsState()` 平滑过渡），
   Expressive 动效留待阶段 C 升级 material3。

4. **已落地的代码结构**：
   - `ui/theme/ThemeState.kt`：风格 × 深浅 × 取色三维状态（`themeMode`/`darkMode`/`dynamicColor`/`seedColor`）。
   - `ui/theme/SeedScheme.kt`：零依赖 CIE LCh Monet 引擎。
   - `ui/theme/Monet.kt`：墙纸 seed 反推 + `SeedScheme` 生成 + 全角色 `animateAsState`。
   - `ui/theme/ThemePreferences.kt`：SharedPreferences 持久化（深浅/动态色/种子色）。
   - `ui/theme/PresetColors.kt`：18 色预设种子色板（含品牌蓝 `#00639B`）。
   - `ui/components/FrostedBottomBar.kt`：haze 毛玻璃底栏。
   - `ui/components/blend/ColorBlendToken.kt`、`ui/components/glass/InnerShadow.kt`：玻璃原语（本地重定义类型）。
   - 设置页新增「深浅模式 / Monet 动态取色 / 种子色」；`AppRoot` 在 MiuiX 分支叠加毛玻璃底栏。

5. **暂缓项**（阶段 C）：`PaletteStyle`/`ColorSpec`（依赖 material-kolor）、`MaterialExpressiveTheme` 动效、
   官方 Miuix 组件 / `miuix-blur` 折射玻璃（`Lens`/`Vibrancy`/`CombinedBackdrop`）。
