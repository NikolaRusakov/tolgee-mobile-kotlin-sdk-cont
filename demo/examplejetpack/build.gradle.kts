import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

fun loadProperties(path: String): Properties {
  val props = Properties()
  val file = rootProject.file(path)
  if (file.exists()) {
    file.inputStream().use { props.load(it) }
  }
  return props
}

val signingProperties = loadProperties("gradle.properties")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.android)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.tolgee.demo.examplejetpack"
  compileSdk = 36

  defaultConfig {
    applicationId = "io.tolgee.demo.examplejetpack"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  signingConfigs {

    val keystoreFile = rootProject.file("tolgee.jks")
    if (keystoreFile.exists()) {
      create("release") {
        storeFile = keystoreFile
        storePassword = signingProperties.getProperty("ANDROID_STORE_PASSWORD") ?: signingProperties.getProperty("storePassword")
        keyAlias = signingProperties.getProperty("ANDROID_KEY_ALIAS") ?: signingProperties.getProperty("keyAlias")
        keyPassword = signingProperties.getProperty("ANDROID_KEY_PASSWORD") ?: signingProperties.getProperty("keyPassword")
      }
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
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
  debugImplementation(libs.ui.test.manifest)
  implementation(project(":compose"))
}