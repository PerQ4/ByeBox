import java.nio.file.StandardCopyOption
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
// Дата сборки должна быть детерминированной — иначе одинаковый код даёт разные APK
// и ломает воспроизводимость/кэш. Приоритет источников:
//   1) SOURCE_DATE_EPOCH (env) — стандарт воспроизводимых сборок;
//   2) дата последнего коммита git (YYMMDD);
//   3) текущая дата — только как аварийный fallback (например, без git).
val vBuildDate: String = run {
    val dateFormat = SimpleDateFormat("yyMMdd", Locale.US)
    val envEpoch = System.getenv("SOURCE_DATE_EPOCH")?.trim()?.toLongOrNull()
    if (envEpoch != null && envEpoch >= 0L) {
        dateFormat.format(Date(envEpoch * 1000L))
    } else {
        // git вызываем через providers.exec (не ProcessBuilder): запуск внешнего
        // процесса напрямую на этапе конфигурации ломает configuration cache.
        val gitDate = try {
            providers.exec {
                commandLine("git", "log", "-1", "--format=%cd", "--date=format:%y%m%d")
                workingDir(rootProject.projectDir)
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
                .takeIf { it.isNotEmpty() && it.matches(Regex("\\d{6}")) }
        } catch (_: Exception) {
            null
        }
        gitDate ?: dateFormat.format(Date())
    }
}

android {
    namespace = "com.perqa.byebox"
    compileSdk = 37
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
        // multiDexEnabled не задаём: при minSdk 24 native multidex включён по
        // умолчанию, флаг был избыточен.
        // Единственный поддерживаемый ABI — arm64-v8a. ABI-сплиты и universal APK
        // намеренно отключены: при `abiFilters` они давали два БАЙТ-ИДЕНТИЧНЫХ
        // APK (~79 МБ каждый) и только раздували вывод сборки.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    val localProps = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }

    // Встроенный пароль — НЕ секрет: он лежит в репозитории и нужен лишь чтобы сборка
    // работала без local.properties. Его следует ротировать: сгенерируйте собственный
    // keystore (keytool -genkeypair) и задайте оба пароля в local.properties
    // (файл в .gitignore). Fallback не удаляем, иначе «чистая» сборка сломается.
    val defaultStorePassword = "ByeBoxSign2024!"
    val hasCustomStorePassword = localProps.containsKey("BYEBOX_KEYSTORE_PASSWORD")
    val hasCustomKeyPassword = localProps.containsKey("BYEBOX_KEY_PASSWORD")

    val ksPath = localProps.getProperty("BYEBOX_KEYSTORE_PATH", "keystore/byebox.jks")
    val ksPass = localProps.getProperty("BYEBOX_KEYSTORE_PASSWORD", defaultStorePassword)
    val ksAlias = localProps.getProperty("BYEBOX_KEY_ALIAS", "byebox")
    val kPass = localProps.getProperty("BYEBOX_KEY_PASSWORD", defaultStorePassword)

    if (!hasCustomStorePassword || !hasCustomKeyPassword) {
        logger.warn(
            "!!! ВНИМАНИЕ: используется встроенный (демонстрационный) пароль keystore по умолчанию. " +
                "Он не является секретом и подлежит ротации. Задайте BYEBOX_KEYSTORE_PASSWORD и " +
                "BYEBOX_KEY_PASSWORD в local.properties или укажите собственный keystore."
        )
    }

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
            isMinifyEnabled = true
            // Удаляем неиспользуемые ресурсы. Ресурсы, на которые есть ссылки из кода/манифеста,
            // сохраняются автоматически; динамически (getIdentifier/по имени) здесь ничего не
            // ищется — если это появится, добавьте res/raw/keep.xml. См. app/proguard-rules.pro.
            isShrinkResources = true
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

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Markdown (описания подписок панелей)
  implementation(libs.commonmark)
  implementation(libs.commonmark.ext.gfm.strikethrough)
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



// Имя APK задаётся НЕ здесь намеренно: задача, которая переименовывает
// (перемещает) готовый артефакт внутри build-каталога во время сборки,
// периодически оставляла частичный/битый APK (`INSTALL_PARSE_FAILED`, ~295 КБ
// вместо полного). Поэтому сборка выдаёт стандартные имена AGP
// (`app-debug.apk` / `app-release.apk`), а итоговое имя при публикации задаётся
// вне Gradle — см. docs/release_process.md.
