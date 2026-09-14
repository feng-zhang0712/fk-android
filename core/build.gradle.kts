plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  `maven-publish`
}

android {
  namespace = "com.fk.core"
  compileSdk = providers.gradleProperty("FK_COMPILE_SDK").get().toInt()

  defaultConfig {
    minSdk = providers.gradleProperty("FK_MIN_SDK").get().toInt()
    consumerProguardFiles("consumer-rules.pro")
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

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        from(components["release"])
        // JitPack remaps multi-module consumers to com.github.USER.REPO:module:tag.
        // Match that groupId while building on JitPack so POM transitive deps resolve.
        groupId = if (System.getenv("JITPACK") == "true") {
          "com.github.feng-zhang0712.fk-android"
        } else {
          providers.gradleProperty("FK_GROUP_ID").get()
        }
        artifactId = "core"
        version = providers.gradleProperty("FK_VERSION_NAME").get()
        pom {
          name.set("fk-android core")
          description.set("Foundation library for fk-android (network, storage, pluggable, …)")
          url.set("https://github.com/feng-zhang0712/fk-android")
          licenses {
            license {
              name.set("MIT License")
              url.set("https://opensource.org/licenses/MIT")
            }
          }
          scm {
            connection.set("scm:git:git://github.com/feng-zhang0712/fk-android.git")
            developerConnection.set("scm:git:ssh://github.com/feng-zhang0712/fk-android.git")
            url.set("https://github.com/feng-zhang0712/fk-android")
          }
        }
      }
    }
    repositories {
      maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/feng-zhang0712/fk-android")
        credentials {
          username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
          password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
      }
    }
  }
}

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.json)
  api(libs.okhttp)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.ktx)
  api(libs.androidx.biometric)
  api(libs.androidx.work.runtime)
  api(libs.coil)
  api(libs.androidx.lifecycle.process)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.okhttp.logging)

  testImplementation(libs.junit)
}
