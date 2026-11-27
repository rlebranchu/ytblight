plugins {
    id("ytblight.android-application")
    id("ytblight.serialization")
}

android {
    namespace = "fr.rlebranchu.ytblight"

    sourceSets {
        // Données factices partagées avec les tests des modules `shared` (jamais dans le code livré).
        getByName("androidTest").kotlin.directories.add(rootProject.file("shared/test-fixtures/kotlin").path)
    }

    defaultConfig {
        applicationId = "fr.rlebranchu.ytblight"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(projects.shared.presentation)
    implementation(projects.shared.domain)
    implementation(projects.shared.data)
    implementation(projects.shared.external)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.youtube.player)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
