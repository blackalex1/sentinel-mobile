plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.xprox.sentinel"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.xprox.sentinel"
        manifestPlaceholders["appName"] = "Sentinel"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "0.0.1"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    flavorDimensions.add("version")
    productFlavors {
        create("foss") {
            dimension = "version"
        }
        create("store") {
            dimension = "version"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.systemProperty("jna.library.path", "${project.projectDir}/src/test/resources/win32-x86-64;${project.projectDir}/src/test/resources")
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
      jniLibs {
        useLegacyPackaging = false
      }
    }
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
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.mockito:mockito-core:5.23.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Jetpack Security for EncryptedSharedPreferences
  implementation("androidx.security:security-crypto:1.1.0")

  // JNA for Native Go Sentinel-Core C-FFI (16 KB page-aligned in 5.16.0+)
  implementation("net.java.dev.jna:jna:5.16.0@aar")
  testImplementation("net.java.dev.jna:jna:5.16.0")

  // AndroidX Graphics Path (16 KB page-aligned in 1.1.0)
  implementation("androidx.graphics:graphics-path:1.1.0")

  // Kotlinx Serialization JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
