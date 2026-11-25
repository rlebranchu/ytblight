/*
 * Convention de l'application Android (Jetpack Compose).
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    compileSdk {
        version =
            release(intVersionOf("android-compileSdk")) {
                minorApiLevel = intVersionOf("android-compileSdkMinor")
            }
    }

    defaultConfig {
        minSdk = intVersionOf("android-minSdk")
        targetSdk = intVersionOf("android-targetSdk")
    }

    buildFeatures {
        compose = true
    }
}
