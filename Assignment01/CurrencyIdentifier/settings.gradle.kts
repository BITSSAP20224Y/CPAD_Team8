// settings.gradle.kts (Project Root)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google() // <--- REMOVED THE RESTRICTIVE CONTENT FILTER HERE
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CurrencyIdentifier"
include(":app")