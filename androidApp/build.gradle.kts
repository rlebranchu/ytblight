plugins {
    id("ytblight.android-application")
}

android {
    namespace = "fr.rlebranchu.ytblight"

    defaultConfig {
        applicationId = "fr.rlebranchu.ytblight"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.shared.umbrella)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
