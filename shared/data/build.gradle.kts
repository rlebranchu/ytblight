plugins {
    id("ytblight.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.domain)
            api(projects.shared.external)
            implementation(projects.shared.local)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
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
