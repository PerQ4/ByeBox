plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

tasks.register("checkSingBox") {
    group = "byebox"
    description = "Check that sing-box binaries are bundled in assets"
    notCompatibleWithConfigurationCache("uses project.file() at execution time")
    doLast {
        val assetsDir = file("src/main/assets/sing-box")
        val abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        val missing = abis.filter { !file("$assetsDir/$it/sing-box").exists() }
        if (missing.isNotEmpty()) {
            println("WARNING: sing-box binaries missing for ABIs: $missing")
            println("Run 'scripts/download-singbox.ps1' or download manually from:")
            println("https://github.com/SagerNet/sing-box/releases")
            println("Place in app/src/main/assets/sing-box/<abi>/sing-box")
        } else {
            println("sing-box binaries found for all ABIs.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("checkSingBox")
}

android {
    namespace = "com.perqa.byebox"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.perqa.byebox"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
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
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

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

  // sing-box shared library (gomobile-generated)
  implementation(files("libs/libbox.aar"))
}


