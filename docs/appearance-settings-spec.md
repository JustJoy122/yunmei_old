# 外观设置（Appearance Settings）复刻规格 — 执行版

按用户指示：直接移植 `KernelSU-main` 的代码，不要自研。

## 要移植的文件
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/screen/settings/SettingsMiuix.kt`（设置页结构：主题设置、其它设置、关于）
- `KernelSU-main/manager/app/src/main/java/me/weishu/kernelsu/ui/theme/{Theme,MaterialTheme,MiuixTheme,ThemeExt}.kt`（双主题：MaterialExpressive + 官方 MiuixTheme/ThemeController）

## 目标
- Material 与 Miuix 两套“设置 + 主题设置”页与 `reference-shots/` 的 md-setting/md-theme-* 与 miuix-setting/miuix-theme-* 一致。
- 修底栏黑色覆盖、图标样式。
- 修导航 IA：门锁、设置是二级子页面（带左上返回箭头）；底栏只用顶层项（如 首页 + 功能项），不要 “伪并行” 4 项。

## 说明
- 视觉规格以模板图为最终验收目标；实现细节以 KernelSU 源码为准。
- 后端（登录/门锁/二维码/打卡）保留。
