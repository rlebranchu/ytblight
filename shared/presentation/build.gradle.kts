plugins {
    id("ytblight.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.domain)
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest {
            kotlin.srcDir(rootProject.file("shared/test-fixtures/kotlin"))
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
