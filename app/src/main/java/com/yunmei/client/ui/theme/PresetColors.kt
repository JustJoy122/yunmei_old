package com.yunmei.client.ui.theme

/**
 * Monet 预设种子色板（移植自 InstallerX `PresetColors.kt`，17 色；KernelSU `keyColorOptions` 同语义）。
 *
 * 颜色统一为 `0xFFRRGGBB` 形态的 Long，直接作为 `Color(longValue)` 构造入参传给
 * material-kolor 取色引擎；「默认 / 跟随壁纸动态取色」由 [DEFAULT_SEED_COLOR]（0）表示，
 * 不再占用列表条目。
 */
data class SeedColor(val key: String, val label: String, val value: Long)

val PresetSeedColors: List<SeedColor> = listOf(
    SeedColor("pink", "粉", 0xFFB94073),
    SeedColor("red", "红", 0xFFBA1A1A),
    SeedColor("orange", "橙", 0xFF944A00),
    SeedColor("amber", "琥珀", 0xFF8C5300),
    SeedColor("yellow", "黄", 0xFF795900),
    SeedColor("lime", "青柠", 0xFF5E6400),
    SeedColor("green", "绿", 0xFF006D39),
    SeedColor("cyan", "青", 0xFF006A64),
    SeedColor("teal", "蓝绿", 0xFF006874),
    SeedColor("light_blue", "浅蓝", 0xFF00639B),
    SeedColor("blue", "蓝", 0xFF335BBC),
    SeedColor("indigo", "靛蓝", 0xFF5355A9),
    SeedColor("purple", "紫", 0xFF6750A4),
    SeedColor("deep_purple", "深紫", 0xFF7E42A4),
    SeedColor("blue_grey", "蓝灰", 0xFF575D7E),
    SeedColor("brown", "棕", 0xFF7D524A),
    SeedColor("grey", "灰", 0xFF5F6162),
)
