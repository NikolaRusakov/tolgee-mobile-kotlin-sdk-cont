import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.android)
}

// ---------------------------------------------------------------------------------------------
// Content Delivery URL is an environment concern, not a code concern.
// Resolution order: local.properties -> -P / gradle.properties -> environment -> demo fallback.
// Set TOLGEE_CDN_DEV / TOLGEE_CDN_PROD to the prefix shown in
// Tolgee Platform -> Project -> Developer settings -> Content Delivery (format: "Android SDK").
// ---------------------------------------------------------------------------------------------
val localProperties = Properties().apply {
  val file = rootProject.file("local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

fun contentDeliveryUrl(key: String): String =
  localProperties.getProperty(key)
    ?: providers.gradleProperty(key).orNull
    ?: providers.environmentVariable(key).orNull
    // Tolgee's public demo project (locales en, cs, fr, sv). Lets the starter show
    // over-the-air text on first run; replace with your own prefix before shipping.
    ?: "https://cdn.tolg.ee/96eacb8b07382b60c3f94b30405cc49b"

android {
  namespace = "io.tolgee.starter.views"
  compileSdk = 36

  defaultConfig {
    applicationId = "io.tolgee.starter.views"
    minSdk = 21
    targetSdk = 36
    versionCode = 1 // Bumping this invalidates the on-device translation cache.
    versionName = "1.0"
  }

  buildFeatures {
    buildConfig = true // BuildConfig.VERSION_CODE keys the cache; BuildConfig.TOLGEE_CDN_URL carries the prefix.
    viewBinding = true
  }

  buildTypes {
    debug {
      buildConfigField("String", "TOLGEE_CDN_URL", "\"${contentDeliveryUrl("TOLGEE_CDN_DEV")}\"")
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField("String", "TOLGEE_CDN_URL", "\"${contentDeliveryUrl("TOLGEE_CDN_PROD")}\"")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {
  implementation(libs.android)
  implementation(libs.activity)               // by viewModels()
  implementation(libs.lifecycle.runtime.ktx)  // repeatOnLifecycle, findViewTreeLifecycleOwner
  implementation(libs.lifecycle.viewmodel.ktx) // viewModelScope
  implementation(libs.coroutines.android)

  // In your own app use the published artifact instead:
  //   implementation("io.tolgee.mobile-kotlin-sdk:core:<version>")   // latest on Maven Central
  implementation(project(":core"))
}
