plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yunmei.client"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.yunmei.client"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Monet / Material You 动态取色引擎（HCT），KernelSU / InstallerX 同款封装。
    implementation(libs.material.kolor)
    // 毛玻璃背景模糊原语（hazeSource / hazeEffect），用于 MiuiX 毛玻璃底栏。
    implementation(libs.haze)

    // 官方 Miuix（t3 最小接入集；t4 再按需加 preference/navigation3-ui）。
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur)
    // 官方 Miuix 设置项组件（SwitchPreference / OverlayDropdownPreference / ArrowPreference，KernelSU 同款）。
    implementation(libs.miuix.preference)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // 网络层：登录/学校/门锁/打卡/开锁密码接口。
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)

    // 加密存储（账号/门锁敏感数据）与二维码、BLE 开门能力。
    implementation(libs.androidx.security.crypto)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    // FastBle 2.3.4 原本托管在已停服的 JCenter；改用本地 AAR，让构建不依赖外部镜像。
    implementation(files("libs/FastBleLib-2.3.4.aar"))

    debugImplementation(libs.androidx.compose.ui.tooling)
}
