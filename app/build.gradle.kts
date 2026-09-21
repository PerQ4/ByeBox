import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Version is defined once in version.properties (see docs/version_naming_policy.md).
val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").trim().toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR", "0").trim().toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH", "0").trim().toInt()
val vStage = versionProps.getProperty("VERSION_STAGE", "").trim()
val vStageNumber = versionProps.getProperty("VERSION_STAGE_NUMBER", "0").trim().toInt()
val vCode = versionProps.getProperty("VERSION_CODE", "1").trim().toInt()
val vName = buildString {
    append(vMajor).append('.').append(vMinor).append('.').append(vPatch)
    if (vStage.isNotEmpty()) append('-').append(vStage).append('.').append(vStageNumber)
}
val vBuildDate = SimpleDateFormat("yyMMdd", Locale.US).format(Date())

android {
    namespace = "com.perqa.byebox"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.perqa.byebox"
        minSdk = 24
        targetSdk = 36
        versionCode = vCode
        versionName = vName
        buildConfigField("String", "VERSION_STAGE", "\"$vStage\"")
        buildConfigField("int", "VERSION_STAGE_NUMBER", "$vStageNumber")
        buildConfigField("String", "BUILD_DATE", "\"$vBuildDate\"")
        buildConfigField("int", "BUILD_NUMBER", "$vCode")
        multiDexEnabled = true
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = true
        }
    }

    val localProps = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }

    val ksPath = localProps.getProperty("BYEBOX_KEYSTORE_PATH", "keystore/byebox.jks")
    val ksPass = localProps.getProperty("BYEBOX_KEYSTORE_PASSWORD", "ByeBoxSign2024!")
    val ksAlias = localProps.getProperty("BYEBOX_KEY_ALIAS", "byebox")
    val kPass = localProps.getProperty("BYEBOX_KEY_PASSWORD", "ByeBoxSign2024!")

    signingConfigs {
        create("byebox") {
            storeFile = rootProject.file(ksPath)
            storePassword = ksPass
            keyAlias = ksAlias
            keyPassword = kPass
        }
        getByName("debug") {
            storeFile = rootProject.file(ksPath)
            storePassword = ksPass
            keyAlias = ksAlias
            keyPassword = kPass
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("byebox")
        }
        debug {
            signingConfig = signingConfigs.getByName("byebox")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      viewBinding = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
      jniLibs {
        useLegacyPackaging = false
      }
    }
    testOptions {
      unitTests.isIncludeAndroidResources = true
    }
}



kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.datastore.preferences)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.mockito)
  testImplementation("org.json:json:20231013")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Material Icons Core
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")

  // Xray shared library (gomobile-generated)
  implementation(files("libs/libv2ray.aar"))

  // v2rayNG dependencies
  implementation(libs.mmkv.static)
  implementation(libs.gson)
  implementation(libs.okhttp)
  implementation(libs.preference.ktx)
  implementation(libs.work.runtime.ktx)
  implementation(libs.work.multiprocess)
  implementation(libs.multidex)
  implementation(libs.zxing.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
   implementation("dev.chrisbanes.haze:haze:0.7.3")
   implementation(libs.kotlinx.serialization.json)

   // TG WS Proxy (local Telegram MTProto/WS proxy, GPLv3 — kept as a separate module)
   implementation(project(":tgwsproxy"))

   // AndroidX graphics-path ships 16KB-aligned RELRO since 1.1.0; force beyond the transitive 1.0.1
   implementation("androidx.graphics:graphics-path:1.1.0")
}

tasks.register("renameApks") {
    val apkDir = layout.buildDirectory.dir("outputs/apk")
    doLast {
        val dir = apkDir.get().asFile
        if (dir.exists()) {
            dir.walk().forEach { f ->
                if (f.isFile && f.name.endsWith(".apk") && f.name.startsWith("app-")) {
                    val newName = f.name.replace("app-", "byebox-")
                    val newFile = File(f.parentFile, newName)
                    if (f.renameTo(newFile)) {
                        println("Renamed: ${f.name} -> $newName")
                    }
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("assemble") }.all {
    finalizedBy("renameApks")
}

