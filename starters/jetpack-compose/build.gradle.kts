import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.android)
  alias(libs.plugins.compose.compiler)
}

// Same convention as starters/android-views: the Content Delivery URL comes from the environment.
// local.properties -> -P / gradle.properties -> environment -> demo fallback.
val localProperties = Properties().apply {
  val file = rootProject.file("local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

fun contentDeliveryUrl(key: String): String =
  localProperties.getProperty(key)
    ?: providers.gradleProperty(key).orNull
    ?: providers.environmentVariable(key).orNull
    // Tolgee's public demo project (locales en, cs, fr, sv). Replace with your own prefix before shipping.
    ?: "https://cdn.tolg.ee/96eacb8b07382b60c3f94b30405cc49b"

android {
  namespace = "io.tolgee.starter.compose"
  compileSdk = 36

  defaultConfig {
    applicationId = "io.tolgee.starter.compose"
    minSdk = 24
    targetSdk = 36
    versionCode = 1 // Bumping this invalidates the on-device translation cache.
    versionName = "1.0"
  }

  buildFeatures {
    buildConfig = true
    compose = true
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
  implementation(libs.lifecycle.runtime.ktx)
  implementation(libs.activity.compose)
  implementation(platform(libs.compose.bom))
  implementation(libs.ui)
  implementation(libs.ui.graphics)
  implementation(libs.ui.tooling.preview)
  implementation(libs.material3)
  debugImplementation(libs.ui.tooling)

  // In your own app use the published artifact instead (it already includes core):
  //   implementation("io.tolgee.mobile-kotlin-sdk:compose:<version>")   // latest on Maven Central
  implementation(project(":compose"))
}
