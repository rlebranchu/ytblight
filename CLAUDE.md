# ytblight — règles pour Claude

Client YouTube perso (Android, iOS, Web). Logique métier en Kotlin Multiplatform (`shared/`), interface native par plateforme. Voir `README.md` pour l'avancement et l'architecture.

## Langue
- Interface de l'app, commentaires, KDoc, commits et PR : **français**.
- Identifiants de code : anglais.

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
- Modules : `domain` ← `data` → `external`, `local` ; `presentation` dépend de `domain` ; `umbrella` assemble tout (Koin).
- Pas de logique d'affichage dans SwiftUI/React/Compose : formatage dans `shared/presentation`.

## Tests et vérification
- `./gradlew build` (ktlint, lint, tests Android hôte et JS), `./gradlew iosSimulatorArm64Test` (macOS).
- Tests d'interface sur données factices (`SharedDependencies.fake`), jamais sur le vrai Google.
- Ne pas lancer `ktlintFormat` sur `build-logic` sans relire le diff.

## Git
- Branches `claude/<sujet>`, PR vers `main`, CI verte avant fusion.
