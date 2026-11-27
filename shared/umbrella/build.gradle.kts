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
            export(projects.shared.local)
        }
    }

    // Web : une librairie JS avec ses définitions TypeScript, consommée par webApp (React).
    js {
        browser {
            testTask { enabled = false }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.presentation)
            api(projects.shared.domain)
            api(projects.shared.data)
            api(projects.shared.external)
            implementation(projects.shared.local)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
        commonTest {
            kotlin.srcDir(rootProject.file("shared/test-fixtures/kotlin"))
        }
        jsMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
