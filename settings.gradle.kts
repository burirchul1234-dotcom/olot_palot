pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Google-hosted mirror of Maven Central first: Central itself rate-limits shared egress (HTTP 429).
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "OlotPalot"

include(":app")
include(":core:common")
include(":core:design")
include(":core:navigation")
include(":core:database")
include(":core:datastore")
include(":domain")
include(":data")
include(":feature:home")
include(":feature:discover")
include(":feature:story")
include(":feature:watch")
include(":feature:library")
include(":feature:profile")
include(":feature:reader")
include(":feature:player")
