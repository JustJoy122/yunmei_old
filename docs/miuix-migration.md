# YunMeiAndroid 构建链升级 + 官方 Miuix 接入迁移清单

> 目标：把 YunMeiAndroid 从 **AGP 8.13.2 / Gradle 8.13 / Kotlin 2.3.20 / compileSdk 36** 升级到
> **Gradle 9.7.1 / AGP 9.3.2 / Kotlin 2.4.10 / compileSdk 37**，并接入官方 Miuix（`top.yukonga.miuix.kmp`），
> 使 assembleDebug 通过。本文档是 `t3`（构建链升级）与 `t4`（UI 整体移植）的执行依据。
>
> 结论依据全部来自工作区内的两个「已知可行」参考工程（它们已用上目标版本组合）：
> - `KernelSU-main/manager`（**本迁移的首选范本**：AGP 9.3.2 / Kotlin 2.4.10 / Gradle 9.7.1 / compileSdk 37 / miuix 0.9.3）
> - `InstallerX-Revived-main`（AGP 9.3.2 / Kotlin 2.4.10 / Gradle 9.7.1 / compileSdk 37 / miuix 0.9.4-SNAPSHOT，GitHub Packages 私有源）
> 以及 `YunMeiAndroid/docs/ui-patterns.md` §4 的 AAR 元数据实测结论。

---

## 0. TL;DR（结论）

1. **硬门槛**：官方 Miuix 0.9.x 的 Android AAR 元数据为 `minCompileSdk=37`，因此**必须先升级到 compileSdk 37 + AGP 9.x**，否则依赖解析直接报错（见 ui-patterns.md §4）。
2. **官方 Miuix 坐标**（团队已定稿）：`top.yukonga.miuix.kmp`，版本 **0.9.3**，来自 **Maven Central**（KernelSU 同款，无需 GitHub Packages 鉴权）。注意 KMP 实际工件名带 `-android` 后缀（见 §2）。
3. **版本组合完全对齐 KernelSU-main/manager**，这是唯一已在工作区验证过的「稳定版 Miuix 0.9.3 + AGP 9.3.2 + compileSdk 37」组合。
4. **环境风险**：本机 Gradle 缓存里目前**没有** Gradle 9.7.1、AGP 9.3.2、kotlin-gradle-plugin 2.4.10、miuix 0.9.3（只有 miuix 0.9.0），且 `bash` 工具无外网；构建前需先确认能访问 Maven Central / services.gradle.org（见 §5）。

---

## 1. 版本矩阵（现状 → 目标）

| 组件 | 现状（YunMeiAndroid） | 目标 | 依据（参考工程） | 是否硬性 |
| --- | --- | --- | --- | --- |
| Gradle (wrapper) | 8.13 | **9.7.1** | KernelSU / InstallerX 均为 9.7.1 | 硬性 |
| AGP | 8.13.2 | **9.3.2** | 两参考工程均 9.3.2 | 硬性 |
| Kotlin | 2.3.20 | **2.4.10** | 两参考工程均 2.4.10 | 硬性 |
| compileSdk | 36 | **37**（`android-37.0`，本机已装） | 两参考工程均 37 | 硬性 |
| targetSdk | 36 | **37** | 两参考工程均 37 | 硬性 |
| minSdk | 26 | **26（保持）** | InstallerX=26；KernelSU=31 | 否 |
| Java source/target | 17 | **21** | KernelSU=21；InstallerX=jvmToolchain(25) | 建议 |
| Compose BOM | 2026.03.00 | **2026.08.00** | 两参考工程均 2026.08.00 | 建议 |
| material3 | 由 BOM 定 | **1.5.0-alpha26（显式 pin）** | KernelSU 显式 pin alpha26；InstallerX alpha27 | 建议 |
| material-kolor | 4.1.1 | **5.0.0** | KernelSU=5.0.0（4.1.1 为 `minCompileSdk=1`，5.x 为 `minCompileSdk=37`，升级到 37 后必须跟到 5.x） | 硬性 |
| haze | 1.7.0 | 保留 1.7.0，或改用 `miuix-blur` | haze 1.7.0=`minCompileSdk=1` 可用；t4 阶段用 `miuix-blur` 替换毛玻璃底栏 | 否 |
| lifecycle | 2.9.4 | **2.11.0** | 两参考工程均 2.11.0 | 建议 |
| activity-compose | 1.11.0 | **1.13.0** | 两参考工程均 1.13.0 | 建议 |
| kotlinx-coroutines | 1.9.0 | **1.11.0** | 两参考工程均 1.11.0 | 建议 |
| kotlinx-serialization | 1.7.3 | 1.11.0（可保持 1.7.3） | InstallerX=1.11.0 | 否 |
| core-ktx | 1.16.0 | 1.19.0（可保持） | InstallerX=1.19.0 | 否 |
| datastore | 1.1.1 | 1.2.1（可保持） | InstallerX=1.2.1 | 否 |
| okhttp | 4.12.0 | 5.5.0（可保持 4.12.0） | 两参考工程 5.5.0 | 否 |
| navigation | navigation-compose 2.9.5 | 保持 2.9.5；如需 `miuix-navigation3-ui` 再引入 navigation3 | KernelSU 用 `androidx.navigation3` 1.1.6 + navigationevent 1.1.2 | 否 |
| Miuix | （无） | **0.9.3** | KernelSU=0.9.3（稳定，Maven Central） | 硬性 |

> 说明：
> - 「硬性」= 不满足则无法解析官方 Miuix 0.9.3 或无法 assembleDebug。
> - 「建议」= 与参考工程对齐，规避 Compose/协程/生命周期版本漂移导致的运行时或编译问题；可保守保留现状版本，但风险自担。
> - `material-kolor 4.1.1 → 5.0.0` 是硬性：miuix 0.9.x 内部依赖 `material-color-utilities`，且 compileSdk 37 后 4.1.1 与 5.x 的 `minCompileSdk` 分叉（4.1.1=1 可用但会与 miuix/参考工程的 5.x 产生版本冲突，统一到 5.0.0 与 KernelSU 完全一致最稳）。

---

## 2. 官方 Miuix 接入坐标（团队定稿：0.9.3）

**仓库**：Maven Central（KernelSU 的 `settings.gradle.kts` 仅声明 `google()` + `mavenCentral()` + `jitpack.io`，故 `top.yukonga.miuix.kmp:0.9.3` 位于 Maven Central，无需 GitHub Packages 鉴权）。
**项目主页**：<https://github.com/compose-miuix-ui/miuix>。

KMP 发布物包含「元数据根模块」与「`-android` 目标工件」两类，Android 工程二选一即可，**推荐显式用 `-android` 工件**（KernelSU 即如此，最明确）：

```toml
# gradle/libs.versions.toml 新增
miuix = "0.9.3"

miuix-ui            = { module = "top.yukonga.miuix.kmp:miuix-ui-android",          version.ref = "miuix" }
miuix-icons         = { module = "top.yukonga.miuix.kmp:miuix-icons-android",       version.ref = "miuix" }
miuix-navigation3-ui= { module = "top.yukonga.miuix.kmp:miuix-navigation3-ui-android", version.ref = "miuix" }
miuix-preference    = { module = "top.yukonga.miuix.kmp:miuix-preference-android",  version.ref = "miuix" }
miuix-blur          = { module = "top.yukonga.miuix.kmp:miuix-blur-android",        version.ref = "miuix" }
```

> ⚠️ **坐标纠正**：任务描述里写的 `top.yukonga.miuix.kmp:miuix-ui:0.9.3` 是「元数据根模块」写法，能解析但不够明确；KernelSU 实际用的是 **`miuix-ui-android`**（带 `-android` 后缀的 KMP 目标工件）。落库时用上表的 `-android` 形式。
> 同样，`miuix-nav-android`（InstallerX 用）与 `miuix-navigation3-ui-android`（KernelSU 用）是**两个不同工件**，团队定稿走 KernelSU 的 `miuix-navigation3-ui`。

**最小接入集（t3 目标）**：`miuix-ui` + `miuix-icons` + `miuix-blur`（三者即可让 `MiuixTheme`/`ThemeController`/毛玻璃跑起来）。`miuix-preference` 与 `miuix-navigation3-ui` 在 t4 移植设置页/导航时再加（后者额外要求 `androidx.navigation3` 与 `androidx.navigationevent:navigationevent-compose`）。

**miuix 0.9.0 的 POM 实测依赖（0.9.3 同源，供参考）**：

```
miuix-core  → org.jetbrains.compose.foundation:foundation:1.10.3 + kotlin-stdlib:2.3.20
miuix-ui    → miuix-core + miuix-shapes
            + org.jetbrains.compose.foundation:foundation:1.10.3
            + org.jetbrains.androidx.navigationevent:navigationevent-compose:1.0.1
            + org.jetbrains.compose.material3:material3-window-size-class:1.9.0
            + com.materialkolor:material-color-utilities:4.1.1
```

> 要点：miuix 走 **Compose Multiplatform（`org.jetbrains.compose.*`）** 坐标，Android 上由 Gradle module metadata 映射到 `androidx.compose.*`；它**传递引入 `navigationevent-compose`**，这也是 KernelSU 显式 pin `navigationevent = "1.1.2"` 的原因。它自带 `material-color-utilities`，App 侧的 `material-kolor` 统一到 5.0.0 即可共存（KernelSU 即此组合）。

---

## 3. AGP 9 破坏性变更清单（对本工程的影响）

本工程是**单模块、无 flavor、无 aidl、无 NDK、无 protobuf、无 convention plugin** 的纯 Compose App，受 AGP 9 影响面小。逐条核对：

| # | 变更 | AGP 9 行为 | 本工程影响 | 处理 |
| --- | --- | --- | --- | --- |
| 1 | **compileSdk DSL** | 移除旧的 `compileSdkVersion`；`compileSdk` 属性仍在，新增 `compileSdk { version = release(x) { minorApiLevel = y } }` 与 `compileSdkMinor` | 现用 `compileSdk = 36`，改 37 即可 | 改为 `compileSdk = 37` + `compileSdkMinor = 0`（InstallerX 同款），或 KernelSU 的 `release(37){ minorApiLevel=0 }` 块 |
| 2 | **built-in Kotlin** | AGP 9 内置 Kotlin 编译，`org.jetbrains.kotlin.android` 插件变为可选（两参考工程都**不再 apply** 该插件，仍可用顶层 `kotlin { compilerOptions {} }`） | 现 apply `kotlin.android` | 两个可行路径：**(a) 最小改动**——保留 `org.jetbrains.kotlin.android` 并升到 2.4.10；**(b) 对齐参考**——去掉 `kotlin.android`，靠 AGP built-in Kotlin（KernelSU 即如此，需保留 `compose.compiler`/`serialization` 插件）。推荐先走 (a) 验证编译，再视需要切 (b) |
| 3 | **compileSdk 37 未声明支持** | AGP 9.3.2 对 `android-37.0` 会告警 | 需要干净构建 | `gradle.properties` 加 `android.suppressUnsupportedCompileSdk=37.0`（KernelSU 同款） |
| 4 | **configuration cache** | Gradle 9 默认推进配置缓存，AGP 9 全兼容 | 本工程构建脚本无 `afterEvaluate`/`Runtime.exec`/Project 泄漏，天然兼容 | 保持脚本简洁即可；`gradle.properties` 可加 `org.gradle.configuration-cache=true`（InstallerX 有） |
| 5 | **`packagingOptions` → `packaging`** | 旧 `packagingOptions` 块移除，用 `packaging { }` | 本工程未用 packagingOptions，无影响 | 无动作；t4 如需排除 so/jni 用 `packaging { jniLibs { ... } }` |
| 6 | **namespace 必需** | 必须显式 `namespace` | 已设 `com.yunmei.client` | 无动作 |
| 7 | **非传递 R / App R 类** | 默认非传递 R | 已设 `android.nonTransitiveRClass=true` | 可保留，也可加 `android.enableAppCompileTimeRClass=true`（KernelSU）优化 |
| 8 | **JDK 要求** | AGP 9.x 需 JDK 17+ 运行；KernelSU 用 Java 21 source/target | 现 Java 17 | 升到 Java 21（§4） |
| 9 | **buildToolsVersion** | AGP 9.3.2 默认 build-tools 可能为 37.0.0 | 本机仅装了 35.0.0 / 36.0.0 | **不建议显式写 `buildToolsVersion="37.0.0"`**（KernelSU 写了但那是它的 NDK/签名需要）；YunMei 走 AGP 默认，若默认要求 37.0.0 则需联网下载（见 §5） |
| 10 | **Kotlin `compilerOptions`** | Kotlin 2.x 用 `kotlin { compilerOptions { jvmTarget } }` 替代 `kotlinOptions` | 现已是 `compilerOptions`（good） | 仅把 `JVM_17` → `JVM_21` |

---

## 4. 分步迁移动作（文件级）

### 4.1 `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.1-bin.zip
```

> 注意：本工程**没有 `gradlew` / `gradlew.bat` 脚本，也没有 `gradle-wrapper.jar`**（只有 properties）。需要用本机已有的 Gradle 8.13 生成 wrapper：
> ```bash
> export JAVA_HOME="/c/Program Files/Android/Android Studio/jdk-21.0.12.1+1"
> <GRADLE_USER_HOME>/wrapper/dists/gradle-8.13-bin/<hash>/gradle-8.13/bin/gradle wrapper --gradle-version 9.7.1
> ```
> 生成后 `./gradlew` 首次运行会联网下载 Gradle 9.7.1 发行版（见 §5 离线风险）。

### 4.2 `gradle.properties`

在现有内容基础上增加/修改：

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
# AGP 9 + compileSdk 37 新增：
android.suppressUnsupportedCompileSdk=37.0
# 可选（Gradle 9 配置缓存，InstallerX 同款）：
org.gradle.configuration-cache=true
```

### 4.3 `settings.gradle.kts`

仓库保持 `google()` + `mavenCentral()` 即可（Miuix 0.9.3 在 Maven Central，无需加 jitpack/GitHub Packages）。**无需改动**，除非 t4 引入 navigation3（也不需要额外仓库）。

### 4.4 `gradle/libs.versions.toml`

`[versions]` 关键项：

```toml
agp = "9.3.2"
kotlin = "2.4.10"
composeBom = "2026.08.00"
material3 = "1.5.0-alpha26"          # 显式 pin，供 miuix/Expressive API 对齐
materialKolor = "5.0.0"
lifecycle = "2.11.0"
activityCompose = "1.13.0"
kotlinxCoroutines = "1.11.0"
miuix = "0.9.3"
# 其余（retrofit/okhttp/coil/securityCrypto/zxing/coil/haze）可保持现状
```

`[libraries]` 新增：

```toml
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }

miuix-ui             = { module = "top.yukonga.miuix.kmp:miuix-ui-android",             version.ref = "miuix" }
miuix-icons          = { module = "top.yukonga.miuix.kmp:miuix-icons-android",          version.ref = "miuix" }
miuix-blur           = { module = "top.yukonga.miuix.kmp:miuix-blur-android",           version.ref = "miuix" }
miuix-preference     = { module = "top.yukonga.miuix.kmp:miuix-preference-android",     version.ref = "miuix" }
miuix-navigation3-ui = { module = "top.yukonga.miuix.kmp:miuix-navigation3-ui-android", version.ref = "miuix" }
```

> `[plugins]` 不用改：`android-application` / `kotlin-android` / `kotlin-compose` / `kotlin-serialization` 的 id 不变，仅版本随 `agp`/`kotlin` 变化。

### 4.5 `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.yunmei.client"
    compileSdk = 37                       // 或 KernelSU 的 release(37){ minorApiLevel = 0 }
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.yunmei.client"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes { /* 保持现状 */ }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // ... 既有依赖保持 ...
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)   // 现在落到 1.5.0-alpha26

    // 官方 Miuix（t3 最小集；t4 再按需加 preference/navigation3-ui）
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur)
}
```

> 关键点：
> - `compileSdk = 37` + `compileSdkMinor = 0` 是 AGP 9.3.2 合法写法（InstallerX convention plugin 实测同款）；KernelSU 的 `compileSdk { version = release(37) { minorApiLevel = 0 } }` 亦可，二选一。
> - Java/Kotlin 目标从 17 升到 **21**（对齐 KernelSU；本机 `jdk-21.0.12.1+1` 与 JBR 25 均可用）。
> - 如果走「built-in Kotlin」路径：删除 `alias(libs.plugins.kotlin.android)`，保留 `kotlin.compose` / `kotlin.serialization`，顶层 `kotlin { compilerOptions {} }` 仍可用（KernelSU 实测）。

---

## 5. 环境核查与离线风险（t3 执行前必读）

本机现状（已实测）：

| 资源 | 状态 | 影响 |
| --- | --- | --- |
| JDK | `jdk-21.0.12.1+1`、JBR 25.0.2 均存在 | ✅ 满足 AGP 9.3.2 / Kotlin 2.4.10 / Java 21 |
| SDK 平台 | `android-36`、`android-37.0` 已装 | ✅ compileSdk 37 可用 |
| build-tools | 仅 `35.0.0`、`36.0.0`（无 37.0.0） | ⚠️ 若 AGP 9.3.2 默认 build-tools 为 37.0.0 则需联网下载 |
| Gradle 发行版 | 仅 `gradle-8.13-bin` 缓存 | ❌ 需联网下载 Gradle 9.7.1 |
| AGP | 仅 `8.13.2` 缓存 | ❌ 需联网下载 AGP 9.3.2 |
| kotlin-gradle-plugin | 仅 `2.3.20` 缓存（stdlib 有 2.4.10） | ❌ 需联网下载 Kotlin 2.4.10 插件 |
| Miuix | 仅 `0.9.0` 缓存（0.9.3 无） | ❌ 需联网下载 miuix 0.9.3 |
| 网络 | `bash` 工具无外网（只有 apt/pip 镜像） | ❌ **最大风险**：`./gradlew` 首次运行会从 services.gradle.org / dl.google.com / repo.maven.apache.org 下载，离线会失败 |

**结论**：目标工具链依赖目前**完全不在本地缓存**，首次构建必须能访问 Gradle 官方分发站 + Google Maven + Maven Central。若执行环境确实离线，需要：
1. 预先从外网把 Gradle 9.7.1 发行版放入 `~/.gradle/wrapper/dists/`；
2. 用一台能联网的机器预拉取依赖（或提供公司级 Maven 镜像 / `init.gradle` 镜像配置）；
3. 确认 `build-tools` 是否需补 37.0.0（`sdkmanager "build-tools;37.0.0"`）。

这些属于 t3 的落地前提，本文档仅做风险提示。

---

## 6. 验证命令（t3 验收）

```bash
cd <repo>
export JAVA_HOME="/c/Program Files/Android/Android Studio/jdk-21.0.12.1+1"

# 1) 生成 wrapper（首次，本机无 gradlew）
<GRADLE_USER_HOME>/wrapper/dists/gradle-8.13-bin/<hash>/gradle-8.13/bin/gradle wrapper --gradle-version 9.7.1

# 2) 构建
./gradlew :app:assembleDebug
```

通过判据：`BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/app-debug.apk`，且 `dependencies` 中能解析到 `top.yukonga.miuix.kmp:miuix-ui-android:0.9.3`：

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i miuix
```

---

## 7. 给 t3 / t4 的交接注意事项

1. **Miuix 坐标用 `-android` 工件**（`miuix-ui-android`），别写任务描述里的裸 `miuix-ui`。
2. **t3 最小接入集**：`miuix-ui` + `miuix-icons` + `miuix-blur` 即可让官方 `MiuixTheme`/`ThemeController` 生效；`miuix-preference` / `miuix-navigation3-ui` 留给 t4（后者要额外引入 navigation3）。
3. **`material-kolor` 必须升到 5.0.0**（compileSdk 37 后 4.1.1 与参考工程/ miuix 的 5.x 冲突），否则 Monet 主题分支可能与 Miuix 共存时冲突。
4. **`android.suppressUnsupportedCompileSdk=37.0`** 务必加，否则 AGP 9.3.2 对 compileSdk 37 告警。
5. **Java/Kotlin 目标升到 21**，与 KernelSU 一致，避免 d8/R8 与 Compose 新 BOM 的字节码版本不匹配。
6. **既有功能零改动**：登录/门锁/二维码/BLE（`FastBleLib-2.3.4.aar`）/retrofit/okhttp/coil/security-crypto 全部保持；本次只动构建链 + 依赖版本 + 主题接入点。
7. 若 t3 阶段走 built-in Kotlin（去掉 `org.jetbrains.kotlin.android`），务必保留 `org.jetbrains.kotlin.plugin.compose` 与 `org.jetbrains.kotlin.plugin.serialization`（二者是 Compose/序列化的编译期插件，built-in Kotlin 不替代它们）。
8. 本文档与 `docs/ui-patterns.md` §4 的兼容性评估完全一致，落地时两者交叉核对。
