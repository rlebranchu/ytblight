/*
 * Convention commune à tous les modules partagés (dossier shared) :
 * cibles Android, iOS et JS, tests communs, ktlint et Kover.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    android {
        namespace = "fr.rlebranchu.ytblight.${project.name}"
        compileSdk {
            version =
                release(intVersionOf("android-compileSdk")) {
                    minorApiLevel = intVersionOf("android-compileSdkMinor")
                }
        }
        minSdk = intVersionOf("android-minSdk")
        // Les tests commonTest tournent aussi sur la JVM (tests « host » Android).
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
