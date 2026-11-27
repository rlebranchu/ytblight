# ytblight

Client YouTube personnel et minimaliste : uniquement le fil chronologique de mes abonnements,
sans recommandations, sans Shorts, sans recherche.

- **Android** : Jetpack Compose (Material 3)
- **iOS** : SwiftUI
- **Web** : React + TypeScript + shadcn/ui, publié sur GitHub Pages
- **Code métier partagé** : Kotlin Multiplatform (Clean Architecture)

## Architecture

```
ytblight/
├─ build-logic/          # plugins de convention Gradle
├─ gradle/libs.versions.toml
├─ shared/
│  ├─ domain/            # models métier, UseCases, interfaces Repository (Kotlin pur)
│  ├─ data/              # RepositoryImpl + mappers DTO <-> métier
│  ├─ external/          # client Ktor, API YouTube, DTO
│  ├─ local/             # stockage sécurisé du jeton OAuth
│  ├─ presentation/      # ViewModels + UiState partagés
│  └─ umbrella/          # framework iOS « Shared » et librairie JS (exports) ; tests d'assemblage Koin
├─ androidApp/
├─ iosApp/
└─ webApp/
```

Sens des dépendances : `presentation -> domain <- data -> external, local`.
`domain` ne dépend de rien d'autre que des coroutines et de kotlinx-datetime.
Seuls `umbrella` et `androidApp` connaissent tous les modules.

## Prérequis

- JDK 21 (fourni avec Android Studio)
- Android Studio (dernière version stable) avec le plugin Kotlin Multiplatform
- Xcode (dernière version stable) pour iOS
- Node.js LTS pour le web

## Commandes

```bash
./gradlew build                      # compilation Android + JS, ktlint, tests
./gradlew ktlintFormat               # formatage du code Kotlin
./gradlew :androidApp:installDebug   # installation sur un appareil ou un émulateur
./gradlew iosSimulatorArm64Test      # tests du code partagé sur iOS (macOS uniquement)
```

## Avancement

1. [x] Socle du monorepo (modules, build-logic, catalogue de versions, ktlint, CI)
2. [x] Fil d'abonnements en code partagé
3. [x] Android de bout en bout (connexion Google réelle validée sur émulateur, tests d'interface Compose)
4. [x] Prototype web (connexion Google réelle validée dans les navigateurs, tests Playwright)
5. [ ] iOS de bout en bout
6. [ ] Web complet et déployé sur GitHub Pages
7. [ ] Ouverture aux autres utilisateurs
