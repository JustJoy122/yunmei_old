# 云莓智能 Android —— 参考 App UI 视觉规格（UI Visual Spec）

> 依据 `<reference-shots>/` 下 11 张参考截图 + 三套参考源码实测，给出可直接指导
> `docs/ui-port-plan.md`（t2）与 t4 落地的**数值化视觉 tokens 与组件规格**。
> 目标：让 YunMeiAndroid 的 Miuix / Material 双皮肤观感达到与 LSPosed / KernelSU / InstallerX Revived 一致。
> 本文的每一个数值都来自以下实测来源，标注于括号内：
> - `miuix 0.9.0 sources.jar`（`top.yukonga.miuix.kmp.*`，本机 gradle 缓存已解包）＝库默认值
> - `KernelSU-main/manager/.../ui/theme|component|screen` ＝参考 App 结构选型
> - 截图本体（OnePlus 15T，1216×2640 px，约 2.75× 密度，dp = px ÷ ~2.75）

---

## 0. 结论摘要（TL;DR）

1. **视觉骨架 = Miuix 库默认 tokens**（圆角 16dp 卡片 / 胶囊 Switch / 28dp 滑块 / 64dp 底栏）+ **KernelSU 结构选型**
   （大标题 TopAppBar、绿色状态卡、分组设置卡片、悬浮胶囊底栏 + 圆形选中 pill + 毛玻璃）。
2. **颜色 = 双来源**：Miuix/Material 皮肤底色与语义色由 **Monet 动态取色（material-kolor 5.0.0）** 生成；
   **特定静态色**（绿色状态卡、工作模式 pill、预设种子色板）才手写。
3. **截图中的绿色系 ≠ 写死**：截图三 App 均使用动态取色（TonalSpot / SPEC_2025，种子来自壁纸），
   因此主色是**橄榄绿**；换种子会整体变色。`ui-visual-spec` 会同时给出 Miuix 默认蓝与 KernelSU 的静态绿。
4. **关键差异（40 项数值 token 见下表）**，t4 落地时按 `§7 包结构` + `§8 组件规格` 逐项对齐。
5. 圆角过渡使用 Miuix 的 **G2-连续平滑圆角**（`SmoothRoundedCornerShape`，`smoothRounding=true`），
   低端机可回退标准 `RoundedCornerShape`（性能开关，不改视觉语义）。

---

## 1. 参考截图盘点（11 张 → 3 参考 App → 5 类页面）

| # | 文件（截尾） | 参考 App | 页面 | 目的 |
|---|---|---|---|---|
| 1 | ..._16-28-58 | KernelSU | 主页 | 状态卡 + 设备信息卡 + 底栏选中态 |
| 2 | ..._16-29-05 | KernelSU | 设置 | 分组卡片 + Switch/下拉/Arrow + 单个选项卡 |
| 3 | ..._16-29-13 | KernelSU | 设置(续) | 关闭态 Switch（X）、禁用态、卸载/日志/关于 |
| 4 | ..._16-29-24 | KernelSU | 主题设置 | 强调色色盘 + 明暗分段按钮 + 滑块 |
| 5 | ..._16-29-37 | LSPosed | 概览 | 激活状态卡 + 信息卡 + 日志卡 + 底栏 |
| 6 | ..._16-29-41 | LSPosed | 设置 | 分组标题（网络/语言/主题）+ 分组卡片 |
| 7 | ..._16-29-50 | LSPosed | 设置(续) | 主题子组 + 框架子组 + 关闭态 Switch |
| 8 | ..._16-29-53 | LSPosed | 主题设置 | 主题/强调色/调色板/色彩规范 + 滑块 |
| 9 | ..._16-30-15 | InstallerX | 主页 | 绿色渐变状态卡 + 双统计卡 + 设备信息 |
| 10 | ..._16-30-30 | InstallerX | 主题设置 | 主题风格单选（Google Material 3 / Miuix）+ 悬浮底栏 |
| 11 | ..._16-30-34 | InstallerX | 设置 | 分组 + 隐藏图标开关 + 备份/还原 + 悬浮底栏 |

**统一视觉语言（三 App 一致）**：暖白底（>92% #F4F4EC 系）、大留白、大标题、圆角分组卡片、
胶囊式控制件、绿色系动态主色、底部悬浮胶囊栏 + 圆形选中 pill + 角标计数。

---

## 2. 设计原则（t4 落地遵守）

1. **层级靠颜色明度与留白，而非描边**：卡片与背景同色系，靠 `surface` → `surfaceContainer` 的微妙差
   + **大圆角** 区分；不使用粗描边。
2. **大标题 + 内容区**：页面首屏在 TopAppBar 展示大标题（32sp），上滑收起为居中 20sp 小标题（KernelSU 行为）。
3. **控制件全部胶囊化**：Switch 胶囊、Slider 圆头、底栏 pill、标签 pill，统一 G2 平滑圆角，营造“液态”感。
4. **一节一卡、卡间 12dp**：设置页每逻辑组一张 `surfaceContainer` 卡片，组间 12dp 留白（见 §6）。
5. **选中态用「填充 + 色调」双重表达**：底栏选中项 = 圆形 pill（primary@15%）+ 图标/文字变 primary；
   非选中项 = `onSurfaceContainer`@40% 透明度 + 常规字重。
6. **玻璃优先级**：`启用模糊` 时底栏/卡片用 `miuix-blur`（`blur 4dp` + `vibrancy` + `lens 24dp` + 内阴影），
   关闭时回退半透色。默认 Material 皮肤对应 `MaterialExpressiveTheme` + `MotionScheme.expressive()`。

---

## 3. 颜色 Tokens

### 3.1 双皮肤体系（与 ui-port-plan §3 对齐）

- **Miuix 皮肤**：`MiuixTheme(controller=ThemeController(...))`，颜色取自 `MiuixTheme.colorScheme`（静态皮肤）。
- **Material 皮肤**：`MaterialExpressiveTheme(colorScheme=rememberDynamicColorScheme(...), motionScheme=expressive())`，
  `material-kolor 5.0.0` 生成全 30+ 角色 + `animateAsState()` 平滑过渡（KernelSU `ThemeExt.kt`）。

### 3.2 Miuix 默认皮肤色（实测 `miuix 0.9.0 Colors.kt`，**未动态取色时**）

| Role(颜色语义) | Light | Dark |
|---|---|---|
| background 页面底 | `#FFFFFF` | `#242424` |
| surface 顶级面/底栏 | `#F7F7F7` | `#000000` |
| surfaceContainer 卡片 | `#FFFFFF` | `#242424` |
| surfaceContainerHigh | `#E8E8E8` | `#242424` |
| surfaceContainerHighest | `#E8E8E8` | `#2D2D2D` |
| primary 主色(Switch/Button/Slider) | `#3482FF` | `#277AF7` |
| onPrimary | `#FFFFFF` | `#FFFFFF` |
| primaryContainer | `#5D9BFF` | `#338FE4` |
| onPrimaryContainer | `#FFFFFF` | `#FFFFFF` |
| secondary 副色(未选中轨道) | `#E6E6E6` | `#505050` |
| secondaryVariant | `#F0F0F0` | `#434343` |
| onSecondary | `#FFFFFF` | `#FFFFFF` |
| onSecondaryVariant | `#303030` | `#D9D9D9` |
| secondaryContainer | `#F0F0F0` | `#434343` |
| onSecondaryContainer | `#A9A9A9` | `#7C7C7C` |
| tertiaryContainer | `#EAF2FF` | `#2B3B54` |
| onTertiaryContainer | `#3482FF` | `#4788FF` |
| onBackgroundVariant | `#8C93B0` | `#787E96` |
| onSurfaceSecondary | `#CC000000`(80%) | `#CCFFFFFF` |
| onSurfaceVariantSummary 摘要文字 | `#99000000`(60%) | `#80FFFFFF` |
| onSurfaceVariantActions | `#66000000`(40%) | `#66FFFFFF` |
| disabledOnSurface | `#B2B2B2` | `#666666` |
| outline | `#D9D9D9` | `#404040` |
| dividerLine | `#E0E0E0` | `#393939` |
| windowDimming(对话框遮罩) | `#000000`@30% | `#000000`@60% |
| sliderBackground | `#0F000000`(6%) | `#26FFFFFF` |
| sliderKeyPoint | `#4DA3B3CD` | `#4D7A8AA6` |
| sliderKeyPointForeground | `#6EB5FF` | `#5DAAFF` |

### 3.3 截图中的实际色（动态取色 → 橄榄绿系，用于校准观感）

> 三参考 App 均在 Miuix/Material 下**开启 Monet**，故主色随壁纸为**橄榄绿**。这是“观感对标”的关键。

| 元素 | 观察色（估） | 来源/说明 |
|---|---|---|
| 页面底 | `#F4F4EC` 暖米白 | 动态 `surface`/`background`（暖瓦片色） |
| 主色（Switch 开/滑块/选中 pill） | `#5B7A3F` 橄榄绿 | 动态 `primary` |
| 主色容器（状态卡底、选中 pill 底、强调色选中） | `#DFFAE4`/`#E8EEDB` | `secondaryContainer` 或 `primaryContainer` |
| 关闭开关轨道 | `#D6D6D0` 灰 | `secondary`（未选中） |
| 关闭开关 thumb | `#F5F5F0` | `onSecondary` |
| Switch 开态 thumb（白色对勾） | `#FFFFFF` | `onPrimary` |
| 卡面 | `#FFFFFF` | `surfaceContainer` |
| 主文字 | `#1C1C1C` | `onSurface`/`onBackground` |
| 次文字/摘要 | `#8A8A82` | `onBackgroundVariant`/`onSurfaceVariantSummary` |
| 角标（17/14） | 深橄榄底白字 | `primary`/`primaryContainer`（`BadgeTone.Accent`） |
| 工作模式 pill（LKM/API 102） | `#5B7A3F` 底 `#FFFFFF` 字 | `primary` 实心 pill |

### 3.4 KernelSU 静态绿（状态卡，实测 `HomeMiuix.kt` StatusCard）

```
非动态(静态)：Light 状态卡底 = #DFFAE4 ，Dark 状态卡底 = #1A3825
动态（Monet）：状态卡底 = colorScheme.secondaryContainer
状态卡对勾：动态 primary@80% ，静态 = #36D167
```

### 3.5 预设种子色板（可移植 `InstallerX .../PresetColors.kt`，material-kolor 用）

```
default #4A672D  pink #B94073  red #BA1A1A  orange #944A00  amber #8C5300
yellow #795900  lime #5E6400  green #006D39  cyan #006A64  teal #006874
light_blue #00639B  blue #335BBC  indigo #5355A9  purple #6750A4
deep_purple #7E42A4  blue_grey #575D7E  brown #7D524A  grey #5F6162
```

### 3.6 YuniMei 既有 Miuix 静态皮肤（保持不变，或并入 §3.2）
主色 `#1677FF`（HyperOS 蓝）、浅底 `#F5F5F5`、面 `#FFFFFF`、`surfaceVariant #F2F3F5`、描边 `#E0E0E0`、错误 `#E5484D`；
暗色主色 `#6AA8FF`、底 `#111111`、面 `#1A1A1A`、`surfaceVariant #2A2A2A`、描边 `#3A3A3A`。

---

## 4. 排版 Tokens

### 4.1 Miuix 默认 TextStyles（实测 `miuix 0.9.0 TextStyles.kt`）

| Token | 字号/字重 | 用途 |
|---|---|---|
| title1 | 32sp | **大标题**（TopAppBar largeTitle，字重 Normal 由 TopAppBar 控制） |
| title2 | 24sp | 次级大标题 |
| title3 | 20sp | 收起后的小标题（TopAppBar small title，字重 Medium） |
| title4 | 18sp | 分区标题 |
| headline1 | 17sp | 设置项标题 / 信息卡字段名（KernelSU InfoText 用，Medium） |
| headline2 | 16sp | 次级标题 |
| main | 17sp | 正文主文案 |
| paragraph | 17sp（行高 1.2em） | 段落 |
| body1 | 16sp | 正文 |
| body2 | 14sp | 摘要/说明（KernelSU InfoText 内容、状态卡版本） |
| subtitle | 14sp **Bold** | `SmallTitle` 分组小标题（onBackgroundVariant） |
| button | 17sp | 按钮 |
| footnote1 | 13sp | 脚注/角标值 |
| footnote2 | 11sp | 极小注释 |

### 4.2 Material Typography（实测 `KernelSU Type.kt`；默认 Material3 Typography 其余按需）
`bodyLarge = 16sp / lineHeight 24sp / letterSpacing 0.5sp`（自定义项），其余走 Material3 默认。

### 4.3 实测字号（截图测量校准）
- 大标题（KernelSU/LSPosed/InstallerX 页首）：≈32sp，字重 Normal（Miuix）/ 可能 Medium。
- 状态卡标题（工作中/已激活）：**22sp SemiBold**（实测 `WorkingText`）。
- 状态卡版本（32601-2 / 2.2.0(7854)）：**15sp Medium**；副标题（设置页 2.2.0(7854)）≈15sp。
- 工作模式 pill（LKM/API 102/GKI）：**16sp Medium**。
- 设置项标题：**17sp**（可能 Medium/Bold）；摘要：**14sp**（`onSurfaceVariantSummary`）。
- 底栏 label：**12sp**（`NavigationBarDefaults.LabelFontSize`）；选中 Bold，未选中 Normal。
- 角标计数：**11sp**（footnote2）。
- 分组标题（网络/语言/主题，LSPosed）：**14sp Bold**（`SmallTitle` 的 `subtitle`）。

---

## 5. 形状 Tokens

| 元素 | 值 | 说明 |
|---|---|---|
| 通用卡片圆角 | **16dp** | Miuix `CardDefaults.CornerRadius` |
| 圆角过渡 | **G2 连续**（`SmoothRoundedCornerShape`） | `smoothRounding=true`；低端机回退 `RoundedCornerShape` |
| 胶囊（Switch/滑块头/底栏 pill/标签 pill） | 全圆角（`SmoothCapsuleShape`） | 高度一半 |
| Switch 形体 | **49 × 28dp** | `size(49.dp, 28.dp)`，thumb 20dp，thumb 偏移 25dp(开)/4dp(关) |
| 滑块 | **高 28dp**（`SliderDefaults.MinHeight`）；keyPoint 半径 3.855dp | 圆头 `miuixShape(28dp)`，thumb 半径 ≈ 高/2×0.72 |
| 底栏（悬浮胶囊） | pill = `CircleShape`；面板高 64dp / pill 高 56dp；内边距 4dp | KernelSU `FloatingBottomBar` |
| 底栏（普通） | 每项高 **64dp**，图标 26dp，顶部内边距 8dp | Miuix `NavigationBarDefaults`（InstallerX 用 56dp，见下） |
| 底栏（InstallerX 悬浮） | 圆角 50dp（`FloatingToolbarDefaults.CornerRadius`），图标 24/28dp，间距 12dp | 胶囊化矩形 |
| 图标尺寸（设置项/信息行程） | 24dp 标准，`size(110.dp)` 仅状态卡大对勾 | KernelSU HomeMiuix |
| 分段列表连接半径（InstallerX） | `CornerRadius 16dp` / `ConnectionRadius 5dp` | MIUI 分组卡片第一/中间/末尾项 |

---

## 6. 布局与间距 Tokens（实测 KernelSU SettingsMiuix/HomeMiuix/TopAppBar）

| 项 | 值 | 来源 |
|---|---|---|
| 列表水平外边距 | **12dp** | `LazyColumn.padding(horizontal=12.dp)` |
| 卡片水平外边距 | 填充整行（fillMaxWidth），无额外水平边距 | |
| 卡间垂直间距 | **12dp**（第一张 top=12dp，末张 vertical=12dp） | `Card.padding(top=12.dp)` / `padding(vertical=12.dp)` |
| 卡片内边距（设置分组） | 由 preference 行自管理（约 16dp），卡片本身 insideMargin 默认 0dp | `CardDefaults.InsideMargin=0dp` |
| InfoCard 内边距 | **16dp** | `Column.padding(16.dp)` |
| 信息行 | 字段名 headline1(17sp,Medium)，值 body2(14sp,`onSurfaceVariantSummary`)，字段名下 2dp，值下 24dp | `InfoText` |
| TopAppBar 标题左内边距 | **26dp** | `TopAppBarDefaults.TitlePadding` |
| TopAppBar 导航图标/动作图标左右边距 | **16dp** | `NavigationIconPadding`/`ActionIconPadding` |
| TopAppBar 收起高 | **52dp**；大标题下间距 4dp；小标题下 8dp | `CollapsedHeight` 等 |
| 返回按钮 | 圆形浅底（`surfaceContainer`），约 40dp，内含 24dp 返回箭头 | 截图主题设置/主题页 |
| 分组小标题（网络/语言/主题） | `SmallTitle` 内边距 `PaddingValues(28dp, 8dp)`，即左 28dp 上下 8dp | `SmallTitleDefaults` |
| 状态卡对勾偏移 | 右下偏移 `offset(27.dp, 31.dp)`，图标 110dp | KernelSU StatusCard |
| 状态卡内文本 | 左上 `padding(16.dp, 14.dp)`；工作模式 pill `padding(16.dp, 10.dp)` | KernelSU StatusCard |
| 分段/单选 chip | 等高胶囊，选中为实心 primary+白字，未选中 `surfaceContainer` | 主题模式与主题风格单选 |
| 安全区 | 顶部/底部系统栏 + displayCutout 水平 | `contentWindowInsets` |

---

## 7. 目标包结构（对齐 ui-port-plan §7，本次补充视觉命名）

```
app/src/main/java/com/yunmei/client/ui/
├── theme/
│   ├── YunMeiTheme.kt          # 入口：Miuix/Material 双皮肤 + 状态栏
│   ├── ThemeState.kt           # 主题状态（风格/深浅/Monet/取色/色彩规范/模糊/悬浮底栏）
│   ├── Monet.kt                # material-kolor 5.0.0：rememberDynamicColorScheme + animateAsState
│   ├── PresetColors.kt         # 预设种子色板（§3.5）
│   ├── Color.kt / Shape.kt / Type.kt   # 静态皮肤 token（§3.6 / §5 / §4）
│   └── Tokens.kt               # 新增：§3-§6 全部数值 token 集中定义（推荐）
├── navigation3/                # androidx.navigation3（KernelSU 同款）或 miuix-nav
├── components/
│   ├── bottombar/              # BottomBar(Miuix/Material) / FloatingBottomBar / 角标
│   ├── blend/                  # ColorBlendToken + BlendColorEntry/BlurBlendMode
│   ├── glass/                  # InnerShadow / Lens / Vibrancy / CombinedBackdrop
│   ├── effect/                 # BgEffect（MIUI 动态渐变背景，可选）
│   └── miuix/                  # InteractiveHighlight / DampedDragAnimation
└── screens/                    # home / locks / login / settings / theme / qrcode ...
```

---

## 8. 组件视觉规格（逐项，t4 落地逐条对齐）

### 8.1 TopAppBar（大标题 → 小标题）
- **展开态**：大标题居中偏左，`title1(32sp)`，Normal；上方状态栏；顶部无额外底色差（透明于页面底）。
- **收起态**：标题居中 20sp（`title3`）Medium；带返回箭头（左，圆形 `surfaceContainer` 底）或动作（右）。
- **背景**：`MiuixTheme.colorScheme.surface`；启用模糊时透明（`BlurredBar`）。
- **折叠动画**：`MiuixScrollBehavior`（`spring(stiffness=2500f)` 收起，`folmeSpring` 显隐标题）。
- 主题设置/主题页的**返回按钮**：约 40dp 圆形浅底（`surfaceContainer`/`onSurfaceVariant` 图标）。

### 8.2 状态卡（主页 hero，KernelSU/LSPosed/InstallerX 通用）
- **形态**：16dp 圆角，浅绿或 `secondaryContainer` 底，左侧大对勾图标（110dp，偏移右下），
  中央偏左上标题 + 版本，右侧工作模式 pill。
- **标题**：22sp SemiBold，`onBackground`。
- **版本**：15sp Medium，`onBackgroundVariant`。
- **右侧 pill**（LKM/GKI/API 102）：左 16dp 上 10dp，16sp Medium；实心 `primary` 底 `onPrimary` 字，圆角胶囊。
- **InstallerX 变体**：状态卡为**绿色渐变**（`#DFFAE4`→ 更亮绿），无对勾，改中央/左侧标题 + 两列统计。
- **动效**：可点击时 `PressFeedbackType.Tilt`/`Sink`（按压下沉/倾斜反馈），`showIndication`。

### 8.3 信息卡（设备信息 / KernelSU InfoCard）
- 16dp 圆角 `surfaceContainer` 卡 + 内边距 16dp；逐行 `InfoText`：
  - 字段名 17sp Medium `onSurface`；值 14sp `onSurfaceVariantSummary`；字段下 2dp，值下 24dp（末行 0）。

### 8.4 设置分组卡片（KernelSU SettingsMiuix）
- 每逻辑组一张 `surfaceContainer` 卡，16dp 圆角，组间 12dp。
- **行类型**（Miuix `preference` 包）：
  - `SwitchPreference`：左图标(24dp) + 标题 + 摘要；右侧 `Switch`。默认无分隔线，行间靠 12-16dp 垂直留白。
  - `ArrowPreference`：同上 + 右侧 `>` 箭头。
  - `OverlayDropdownPreference`：同上 + 右侧当前值文字。
  - 禁用行：标题/图标用 `disabledOnSecondaryVariant`/`disabledOnSurface`，开关轨道变 `disabledSecondary`。
- **开关**：49×28dp 胶囊；关=灰轨道(`secondary`)+浅 thumb(`onSecondary`)，开=主色轨道(`primary`)+白 thumb(`onPrimary`，
  内含白色对勾/圆点）；按下 thumb 放大 1.127×；拖拽 + 触感(ToggleOn/Off)。
- **滑块**：高 28dp 圆头；前景 `primary`、背景 `sliderBackground(6%)`、thumb `onPrimary`；可带 keyPoint(3.855dp)。
- 关闭态开关在部分实现显示 **×**（截图 WebView 调试关态为灰胶囊+白×）。

### 8.5 主题设置页（截图 #4/#8/#10）
- 顶部返回 + 大标题「主题设置」。
- **强调色色盘**：一排圆形色板，选中项有 `primary` 圆环 + 白色对勾；色板下方带柔和光晕。
- **主题模式分段按钮**：一排胶囊（亮/暗/跟随/黑），选中项 `primary` 实心 + 白图标，未选中 `surfaceContainer`。
- **主题风格单选卡**（InstallerX）：整卡可选，选中项 `primaryContainer`/`secondaryContainer` 浅底 + 右侧单选圈
  `primary`；未选中白底 + 空圈。
- **Switch/Arrow/Overlay 行**与设置页一致；底部「界面缩放」= 标签 + 右侧值 + 滑块。

### 8.6 底栏（bottom navigation）
**A. 普通 NavigationBar（默认，KernelSU Settings 用）**
- 高 64dp；图标 26dp 顶部内边距 8dp；label 12sp 底内边距 8dp；`color=surface`，顶部一条 `dividerLine`。
- 选中：图标+文字 `onSurfaceContainer` 满不透明、label **Bold**；未选中：`onSurfaceContainer`@40%。
- 按压缩放/透明度反馈（`UnselectedAlpha=0.4`，`SelectedPressedAlpha=0.5`）。
- 顶部用 `BlurredBar` 时背景透明。

**B. 悬浮胶囊底栏（`启用悬浮底栏`，KernelSU FloatingBottomBar / InstallerX FloatingNavigationBar）**
- **面板**：胶囊圆角（CircleShape 或 50dp），`surfaceContainer`@40% 半透明 + 毛玻璃
  （`blur 4dp` + `vibrancy` + `lens 24dp`）+ 外阴影（dropShadow radius 10dp，#000 0.1 亮/0.2 暗）+ 内阴影。
- **选中 pill**：`CircleShape`，宽=tabWidth（约 76dp），高 56dp，底色 `primary`@15%（非模糊时）或玻璃高光层；
  内部图标与文字着色为 `primary`，缩放 1.2×（按压）/拖拽动效（`DampedDragAnimation`）。
- 面板高 64dp；水平内边距 4dp；关闭模糊时回退 `containerColor = surfaceContainer` 纯色。
- **InstallerX 变体**：`FloatingNavigationBar`，圆角 50dp，`surfaceContainer` 底，图标 24dp(label 12sp) 或 28dp(仅图标)，
  项间距 12dp，水平外距 36dp，阴影 1dp。
- **角标**：选中/超管/模块等图标右上角小 pill（半径=高/2），深橄榄底白字（`BadgeTone.Accent` = `primary`/`primaryContainer`），
  数字 11sp；模块更新为 `Alert` 色系。

### 8.7 图标
- 主用 **Material Icons Rounded**（`Icons.Rounded.*`：Cottage/Security/Extension/Settings/Fence/Palette/Update...）。
- 部分用 **Miuix Icons**（如 `MiuixIcons.Link`）。
- 尺寸：列表/设置项 24dp；底栏 26dp（普通）/24-28dp（悬浮）；状态卡大对勾 110dp。
- 着色：`tint = onBackground/onSurface`（列表）、`onSurfaceContainer`（底栏）、选中 `primary`。

### 8.8 玻璃 / 液态玻璃（`启用模糊`时的增强）
- 依赖 `miuix-blur`：`Backdrop`/`LayerBackdrop`/`layerBackdrop`/`drawBackdrop`/`rememberLayerBackdrop`。
- 效果组合（KernelSU `FloatingBottomBar`）：`vibrancy()` + `blur(4dp)` + `lens(24dp)` + `innerShadow` + `BloomStroke` 高光。
- 色表：移植 `ColorBlendToken`（Info/Colored/Pured/Overlay 四族 × 深浅 × Extra_Thin..Extra_Thick），
  依赖 `BlendColorEntry`/`BlurBlendMode`（来自 miuix-blur）。
- 主题设置页的**动态渐变背景**（InstallerX）：`BgEffect`（RuntimeShader，API33+ 兜底），MIUI 风渐变。

### 8.9 动效（交互反馈，保持轻盈）
- Switch 开合 `spring(damping 0.7, stiffness 987)`；拖拽触感 ToggleOn/Off。
- Slider 值 `spring(0.96, 322)`，拖拽 `spring(0.9, 1755)`；keyPoint 磁吸 `magnetThreshold 0.02`；触感 Edge/Step。
- 底栏 pill 拖拽 `DampedDragAnimation`（pressScale 78/56 = 1.39）；背景缩放、速度拉伸。
- 主题色过渡 `animateAsState(spring)` / `animateColorAsState(spring)` 全角色平滑。
- 页面/标题滚动 `MiuixScrollBehavior` + `folmeSpring`。

---

## 9. 落地核对清单（给 t4）

- [ ] 建立 `ui/theme/Tokens.kt`，集中 §3-§6 数值 token（颜色/圆角/字号/间距），供双皮肤共用。
- [ ] Miuix 页用 `MiuixTheme(controller=ThemeController(...))`；Material 页用 `MaterialExpressiveTheme` + `motionScheme=expressive()`。
- [ ] 圆角统一走 `miuixShape(16.dp)` / `miuixCapsuleShape()`（G2），保留 `smoothRounding` 开关。
- [ ] 设置页：`LazyColumn` 水平 12dp，每卡 16dp 圆角 + 卡间 12dp，行用 `SwitchPreference/ArrowPreference/OverlayDropdownPreference`。
- [ ] 主页 hero 状态卡：绿/`secondaryContainer` 底 + 大对勾 + 右侧 pill；信息卡 16dp 内边距 + `InfoText`。
- [ ] 底栏：默认 `NavigationBar`（64dp/图标26/label12sp）；可切 `FloatingBottomBar`（胶囊+选中 pill+毛玻璃+角标）。
- [ ] 主题设置页：强调色色盘 + 模式分段 chip + 风格单选卡 + `Switch/Slider/Arrow` 行 + 界面缩放滑块。
- [ ] 开关 49×28 胶囊；滑块 28dp 圆头；图标 24dp（Material Rounded）。
- [ ] 保留既有登录/门锁/二维码/打卡功能；只做视觉与导航层移植，不破坏数据/网络层。
- [ ] `assembleDebug` 通过（构建链版本见 `docs/miuix-migration.md`）。

---

## 10. 参考文件索引（工作区相对路径）

- 截图：`reference-shots/Screenshot_2026-08-31-*.jpg`（11 张）
- Miuix 库默认：`~/.gradle/.../top.yukonga.miuix.kmp/*/0.9.0/*-sources.jar`（Colors/TextStyles/Card/Switch/Slider/NavigationBar/TopAppBar/SmallTitle/SmoothRounding）
- KernelSU 结构选型：`KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/theme/{Theme,ThemeExt,MiuixTheme,MaterialTheme,Type}.kt`、
  `.../ui/component/bottombar/{BottomBar,BottomBarMiuix}.kt`、`.../ui/component/FloatingBottomBar.kt`、
  `.../ui/screen/settings/SettingsMiuix.kt`、`.../ui/screen/home/HomeMiuix.kt`
- 关联文档：`docs/miuix-migration.md`（t1，构建链）、`docs/ui-patterns.md`（调研/适配）、`docs/ui-port-plan.md`（t2，移植方案）
