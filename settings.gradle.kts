pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ripple"

include(":app")
include(":wear")
include(":core:domain")
include(":core:designsystem")
include(":core:storage")
include(":core:preferences")
include(":core:health")
include(":core:wear-sync")
include(":feature:today")
include(":feature:history")
include(":feature:stats")
include(":feature:settings")
include(":feature:onboarding")
include(":system:widgets")
include(":system:quicksettings")
include(":system:notifications")
include(":system:appactions")
include(":system:export")
