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

// Koin dépend des copies JetBrains (Compose Multiplatform) de lifecycle et savedstate, alors que
// nous utilisons celles d'androidx : sur le web, les deux versions se retrouvaient dans le bundle
// (classes en double). On impose partout la version androidx, qui existe maintenant sur toutes
// les cibles (Android, iOS, JS).
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        val lifecycle = versionOf("androidx-lifecycle")
        val savedState = versionOf("androidx-savedstate")
        listOf("lifecycle-common", "lifecycle-viewmodel", "lifecycle-viewmodel-savedstate").forEach { artifact ->
            substitute(module("org.jetbrains.androidx.lifecycle:$artifact"))
                .using(module("androidx.lifecycle:$artifact:$lifecycle"))
        }
        substitute(module("org.jetbrains.androidx.savedstate:savedstate"))
            .using(module("androidx.savedstate:savedstate:$savedState"))
    }
}
