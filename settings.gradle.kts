pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ytblight"

include(":androidApp")
include(":shared:domain")
include(":shared:data")
include(":shared:external")
include(":shared:local")
include(":shared:presentation")
include(":shared:umbrella")
include(":shared:testing")
