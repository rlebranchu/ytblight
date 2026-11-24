plugins {
    id("ytblight.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.domain)
            implementation(projects.shared.external)
            implementation(projects.shared.local)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
