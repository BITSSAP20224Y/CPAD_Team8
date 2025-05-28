// build.gradle.kts (Project: CurrencyIdentifier)

plugins {
    // Top-level build file where you can add configuration options common to all sub-projects/modules.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Add the Google Services plugin if you plan to use Firebase or Google Play Services for ML Kit (optional, but good for future expansion)
    // id("com.google.gms.google-services") version "4.4.1" apply false // Check for latest version
}