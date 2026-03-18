// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("ci") {
    group = "verification"
    description = "Run the default CI quality gate."
    dependsOn(":app:lintDebug", ":app:testDebugUnitTest", ":app:assembleDebug")
}
