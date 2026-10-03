# 云莓智能（旧版）— 归档

> **已归档，不再维护。** 本仓库为历史工程快照，仅作参考与追溯之用。

## 项目状态

- 状态：**已归档（Archived）**，不再接受功能更新、问题修复或依赖升级。
- 版本：`versionCode = 1`，`versionName = "0.1.0"`（`applicationId = com.yunmei.client`）。
- 代码停留在 2026-09-18 的快照；此后所有开发均在新项目 `YunmeiVibe` 进行。

## 项目用途与背景

本项目是「云莓智能」Android 客户端的**旧版实现**，用于在校园场景下：

- 登录云莓服务端账号，拉取学校 / 门锁 / 打卡对象等数据；
- 通过 BLE（低功耗蓝牙）与门锁通信完成开门，并支持读取 / 缓存开门密码；
- 完成定位打卡并上传打卡记录；
- 提供门锁二维码分享与扫码添加门锁。

它是由更早的 `yunmei_unintelligent`（Java 版）向 Kotlin + Compose + Miuix 迁移过程中的中间形态，用于验证「Material 3 Expressive / Miuix 双主题」架构的可行性。

## 原技术栈

- 语言 / 构建：Kotlin（JVM 21）、Gradle 9.7.1、AGP 9.x，`compileSdk 37` / `minSdk 26` / `targetSdk 37`
- UI：Jetpack Compose、Material 3（含 Expressive API）、官方 Miuix（`miuix-ui` / `miuix-icons` / `miuix-blur` / `miuix-preference`）
- 取色与视觉：material-kolor（Monet 动态取色）、haze（毛玻璃模糊原语）
- 数据与网络：kotlinx.serialization、DataStore Preferences、Retrofit + OkHttp、Coil
- 安全与硬件：androidx.security.crypto（EncryptedSharedPreferences）、zxing（二维码）、BLE 门锁通信
- 本地依赖：`app/libs/FastBleLib-2.3.4.aar`（FastBle 2.3.4 原托管于已停服的 JCenter，故改为本地 AAR，避免依赖外部镜像）
- 导航：androidx.navigation.compose

## 与当前项目 YunmeiVibe 的关系

**本项目已被 `YunmeiVibe` 完全取代，两者代码不共享、不互相引用。**

| 维度 | 本仓库（旧版） | YunmeiVibe（现役） |
| --- | --- | --- |
| applicationId | `com.yunmei.client` | `com.yunmei.vibe` |
| 页面架构 | 3 个主页面（打卡 / 首页 / 门锁） | 底部多 Tab + miuix-nav 类型安全路由 |
| 打卡 / 开门 | 前台流程 | 前台 + 快捷方式跳板 + 前台服务 + 通知进度 |
| 数据存储 | EncryptedSharedPreferences | SecureStore + DataStore + 备份/还原（PBKDF2 + AES-GCM） |
| 状态 | 已归档 | 持续开发中 |

本仓库中的网络协议、BLE 开帧格式、打卡定位方式等实现，已被 `YunmeiVibe` 重新实现并扩展；如需了解现役设计，请以 `YunmeiVibe` 为准。

## 归档内容（`docs/` 说明）

仓库保留下列 7 份设计 / 规格文档，是本工程尚存的独特产出：

| 文件 | 主题 |
| --- | --- |
| `docs/ui-patterns.md`（约 37.5 KB） | UI 模式大全：双主题下各类页面 / 组件的结构范例与可照抄写法 |
| `docs/ui-port-plan.md`（约 28.8 KB） | UI 移植计划：把参考项目（KernelSU / InstallerX 等）的界面按阶段移植到本工程的方案 |
| `docs/ui-visual-spec.md`（约 23 KB） | 视觉规格：依据参考截图给出的间距、圆角、字号、色彩层级等可执行规格 |
| `docs/miuix-migration.md`（约 17.6 KB） | Miuix 迁移记录：依赖接入、版本选择、兼容性坑与 Gradle wrapper 升级过程 |
| `docs/lock-api-findings.md`（约 13.4 KB） | 门锁接口实测结论：请求 / 响应字段、鉴权头、错误码与踩坑记录 |
| `docs/ui-polish-spec.md`（约 1.8 KB） | 界面打磨清单：细节修正项 |
| `docs/appearance-settings-spec.md`（约 1 KB） | 外观设置规格：主题 / 取色 / 深浅模式相关设置项定义 |

原始交接材料（后端逻辑总结、UI 总结、项目移交文档、KernelSU 风格 UI Kit API 参考）不属于本仓库，保存在开发者本地归档中。

## 本地构建（仅供参考，已不再验证）

本项目需要 Android SDK 与 JDK 21。仓库内**不包含** `local.properties`，如需构建请自行创建并写入：

    sdk.dir=<你的 Android SDK 路径>

然后执行 `./gradlew :app:assembleDebug`。请注意：`miuix-*` 等依赖来自 GitHub Packages，可能需要配置 `gpr.user` / `gpr.key`（或 `GITHUB_ACTOR` / `GITHUB_TOKEN` 环境变量）。由于项目已归档，此构建路径**未做保证**。

## 许可说明

本仓库**未附任何开源许可证文件（无 LICENSE）**。

因此：**仅供归档参考，不提供任何担保**。请勿将其用于生产环境；如需复用其中的代码或文档，请自行确认权利归属与合规性。文档中出现的第三方项目名称（KernelSU、InstallerX、Miuix 等）归各自权利人所有。