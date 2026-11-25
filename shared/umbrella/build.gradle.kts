plugins {
    id("ytblight.kmp-library")
    id("ytblight.skie")
}

kotlin {
    // iOS : un seul framework, « Shared », qui expose les ViewModels et le domaine.
    // SKIE sera ajouté au jalon 5 (iOS), une fois sa compatibilité avec Kotlin vérifiée sur macOS.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.shared.presentation)
            export(projects.shared.domain)
            export(projects.shared.external)
        }
    }

    // Web : une librairie JS avec ses définitions TypeScript, consommée par webApp (React).
    js {
        browser {
            testTask { enabled = false }
        }
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.presentation)
            api(projects.shared.domain)
            implementation(projects.shared.data)
            api(projects.shared.external)
            implementation(projects.shared.local)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.kermit)
        }
        // Android : le conteneur Koin est exposé à Compose (koinViewModel). Pas sur iOS ni sur le web.
        androidMain.dependencies {
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.shared.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
