import Shared
import SwiftUI

/// Application hôte des tests d'interface : même interface et même code Kotlin que l'application
/// livrée, mais l'API YouTube et la connexion Google sont factices (aucun appel à Google).
/// Arguments de lancement : `-signedIn YES|NO` (session déjà ouverte ou non).
@main
struct YtblightFakeApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        let signedIn = UserDefaults.standard.bool(forKey: "signedIn")
        Ytblight.shared.startForUiTests(
            youTubeApi: FakeYouTubeApi(),
            tokenRequester: FakeTokenRequester(hasSession: signedIn),
            tokenStorage: InMemoryTokenStorage()
        )
    }

    var body: some Scene {
        WindowGroup {
            AppRoot()
                .preferredColorScheme(.dark)
        }
    }
}
