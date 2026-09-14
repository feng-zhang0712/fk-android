plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  `maven-publish`
}

android {
  namespace = "com.fk.business"
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

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        from(components["release"])
        groupId = if (System.getenv("JITPACK") == "true") {
          "com.github.feng-zhang0712.fk-android"
        } else {
          providers.gradleProperty("FK_GROUP_ID").get()
        }
        artifactId = "business"
        version = providers.gradleProperty("FK_VERSION_NAME").get()
        pom {
          name.set("fk-android business")
          description.set("Business Compose kits for fk-android (comment, filter, cell rows)")
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
  api(project(":ui"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.activity.compose)

  testImplementation(libs.junit)
}
