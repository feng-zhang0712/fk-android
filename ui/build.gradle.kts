plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.fk.ui"
  compileSdk = providers.gradleProperty("FK_COMPILE_SDK").get().toInt()

  defaultConfig {
    minSdk = providers.gradleProperty("FK_MIN_SDK").get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

dependencies {
  api(project(":core"))
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.material3)

  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.coil.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)

  debugImplementation(libs.androidx.compose.ui.tooling)

  testImplementation(libs.junit)
}
