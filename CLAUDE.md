# ytblight — règles pour Claude

Client YouTube perso (Android, iOS, Web). Logique métier en Kotlin Multiplatform (`shared/`), interface native par plateforme. Voir `README.md` pour l'avancement et l'architecture.

## Langue
- Interface de l'app, commentaires, KDoc, commits et PR : **français**.
- Identifiants de code : anglais.
- **Android : aucun texte en dur dans les composables.** Tous les textes affichés sont dans `androidApp/src/main/res/values/strings.xml` et lus avec `stringResource(R.string...)`.

## Contraintes produit (ne pas contourner)
- Mode sombre uniquement, français uniquement, navigation en portrait (paysage seulement sur l'écran du lecteur).
- Aucun suivi, aucune statistique, aucun rapport d'erreur. Rien de stocké en local hors ce qu'exige la connexion.
- Pas de backend, pas de clé d'API : jeton OAuth uniquement.
- Pas de délai minimal de rafraîchissement ni de limite en jours sur le fil (décision explicite).

## Style de code
- **Signatures** : dès le premier paramètre (fonction) ou attribut (classe), un par ligne, virgule finale, y compris pour un seul :

  ```kotlin
  fun updateUserProfile(
      userId: String,
      isActive: Boolean = true,
  ): Boolean {
      return true
  }

  class UserProfile(
      val userId: String,
  )
  ```

  Appliqué par ktlint via `.editorconfig` (`./gradlew ktlintFormat`). Mêmes principes en Swift et TypeScript.
- **Appels** de fonctions et constructeurs **du projet** : arguments nommés, un par ligne, virgule finale, y compris pour un seul argument :

  ```kotlin
  updateUserProfile(
      userId = userId,
      isActive = false,
  )
  ```

  Non appliqué à la bibliothèque standard ni aux bibliothèques tierces (`assertEquals`, `listOf`, `Modifier.padding`…) ni à l'appel d'un paramètre de type fonction (`onClick()`). Non vérifié par ktlint : à respecter à la main.
- Indentation de 4 espaces, lignes ≤ 120 caractères.

## Architecture
- **Un ViewModel par page** (jamais de ViewModel global). Pages : `splash`, `signin`, `feed`, `player`. Un package par page dans chaque module : `shared/presentation/.../<page>/<Page>ViewModel.kt` (+ `<Page>UiState`) et `androidApp/.../ui/<page>/<Page>Route.kt`. Les ViewModels restent dans `shared` pour servir aussi SwiftUI et React.
- **Fichier `<Page>Route.kt`** : toujours deux fonctions `@Composable`. La première, publique (`<Page>Route`), obtient le ViewModel via Koin (`val viewModel: XViewModel = koinViewModel()`), collecte l'état, gère la navigation, puis appelle la seconde. La seconde, privée (`<Page>Screen`), ne contient que l'interface : elle ne reçoit ni ViewModel ni Koin, seulement un état et des lambdas.
- **Un ViewModel ne parle jamais à un repository** : il passe toujours par un use case du `domain` (`GetFeedUseCase`, `CheckSessionUseCase`, `CompleteSignInUseCase`, `SignOutUseCase`…), même quand il se limite à déléguer.
- **Aucun faux code dans le code de production** : les fausses données (`FakeFeedRepository`, `FakeAuthRepository`, `fakeDependencies()`) vivent uniquement dans les sources de test (`commonTest`, `androidTest`) du module qui les utilise. Les tests les fournissent dans un module Koin de test (`fakeModule`).
- **Connexion Google** : le flux est piloté par `SignInViewModel` → `SignInUseCase` / `CompleteConsentUseCase` → `AuthRepository` → `PlatformAuth` (implémenté par la plateforme : `GmsPlatformAuth` sur Android, qui traduit aussi les exceptions Play Services en échecs). L'interface n'a qu'un rôle : afficher la fenêtre de consentement quand `SignInUiState.consentRequest` est renseigné et renvoyer son résultat brut (`ConsentResponse`). Jamais d'appel direct à `GmsPlatformAuth` depuis l'interface.
- **Koin** (comme dans la documentation) : un module top-level par couche, `val xxxModule = module { ... }` dans le package `di` de la couche : `externalModule` (API YouTube), `dataModule` (repositories + use cases), `presentationModule` (`viewModel {}`), et un `platformModule` par plateforme (Android : `GmsPlatformAuth`, lié à `PlatformAuth`). Koin est démarré par `startKoin` dans l'`Application`. Arguments nommés (`repository = get()`). Les tests d'interface surchargent les repositories avec `loadKoinModules(fakeModule)`.
- Navigation : après connexion ou déconnexion, retirer la page quittée de la pile (`popUpTo ... inclusive`).
- Modules : `domain` ← `data` → `external`, `local` ; `presentation` dépend de `domain` ; `umbrella` ne sert qu'à exporter le framework iOS et la librairie JS.
- Pas de logique d'affichage dans SwiftUI/React/Compose : formatage dans `shared/presentation`.

## Tests et vérification
- `./gradlew build` (ktlint, lint, tests Android hôte et JS), `./gradlew iosSimulatorArm64Test` (macOS).
- Tests d'interface sur données factices (`fakeModule`), jamais sur le vrai Google.
- Ne pas lancer `ktlintFormat` sur `build-logic` sans relire le diff.

## Git
- Branches `claude/<sujet>`, PR vers `main`, CI verte avant fusion.
