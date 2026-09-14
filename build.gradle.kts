plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

/** Used by JitPack (`jitpack.yml`) and local scripts — publishes libraries only. */
tasks.register("publishLibrariesToMavenLocal") {
  group = "publishing"
  description = "Publish :core, :ui, and :business to mavenLocal (excludes :sample)"
  dependsOn(
    ":core:publishToMavenLocal",
    ":ui:publishToMavenLocal",
    ":business:publishToMavenLocal",
  )
}
