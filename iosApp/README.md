# iosApp

Application iOS en SwiftUI (iOS 17 minimum), thème sombre uniquement, en français, portrait (le
paysage n'est autorisé que sur le lecteur). Elle utilise le framework Kotlin `Shared`
(`shared/umbrella`, avec SKIE) : ViewModels, use cases et API YouTube partagés.

## Ouvrir et lancer

```bash
open iosApp/ytblight.xcodeproj   # le framework Kotlin est produit par une phase de build (Gradle)
```

Le projet est décrit dans `project.yml` ([XcodeGen](https://github.com/yonaskolb/XcodeGen)) ; après
l'avoir modifié : `brew install xcodegen && cd iosApp && xcodegen generate` (le `.xcodeproj` est versionné).
Pour installer sur un appareil, choisis ton équipe dans Signing & Capabilities.

## Structure

- `App/Pages/<page>/` : `<Page>Route.swift` (crée le ViewModel partagé, observe son état) et
  `<Page>Screen.swift` (interface pure : état et fonctions seulement), comme sur Android et le web.
- `App/Bridge/` : connexion Google par **AppAuth** (client « iOS », code + PKCE, sans serveur) et
  Trousseau pour le jeton ; le jeton de rafraîchissement de Google permet un renouvellement
  silencieux, sans rouvrir la fenêtre de connexion.
- `App/Resources/Localizable.xcstrings` : tous les textes (français), l'équivalent de `strings.xml`.
- `Entry/Production` : l'application livrée (cible `ytblight`).
- `Entry/UITests` + `UITests` : hôte `ytblightFakes` et tests XCUITest, sur API YouTube et connexion
  factices écrites en Swift. Jamais livrés.

## Tester

```bash
xcodebuild -project ytblight.xcodeproj -scheme ytblightUITests \\
  -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO test
```
