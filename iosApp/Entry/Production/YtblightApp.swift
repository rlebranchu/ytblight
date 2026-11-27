import Shared
import SwiftUI

@main
struct YtblightApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Le code Kotlin partagé démarre avec la connexion Google (AppAuth) et le Trousseau d'ici.
        Ytblight.shared.start(
            tokenRequester: AppAuthTokenRequester(),
            tokenStorage: KeychainTokenStorage()
        )
    }

    var body: some Scene {
        WindowGroup {
            AppRoot()
                .preferredColorScheme(.dark)
        }
    }
}
