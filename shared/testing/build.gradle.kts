plugins {
    id("ytblight.kmp-library")
}

// Données factices partagées par les tests (unitaires et d'interface). Aucun module de production
// ne dépend de celui-ci : il n'est jamais embarqué dans l'application livrée.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.domain)
            api(projects.shared.umbrella)
        }
    }
}
