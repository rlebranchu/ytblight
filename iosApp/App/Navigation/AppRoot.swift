import Shared
import SwiftUI

/// Pages « racine » : on n'y revient pas en arrière (démarrage, connexion, fil).
private enum RootDestination {
    case splash
    case signIn
    case feed
}

/// Navigation de l'application. Chaque page a son ViewModel partagé (Kotlin), créé à son
/// affichage et libéré à sa disparition (voir les `XxxRoute`).
struct AppRoot: View {
    @State private var destination: RootDestination = .splash
    @State private var path: [Route] = []

    var body: some View {
        switch destination {
        case .splash:
            SplashRoute(
                onNavigate: { next in
                    destination = next == .feed ? .feed : .signIn
                }
            )
        case .signIn:
            SignInRoute(
                onSignedIn: { destination = .feed }
            )
        case .feed:
            NavigationStack(path: $path) {
                FeedRoute(
                    onOpenVideo: { videoId in path.append(.player(videoId: videoId)) },
                    onSignedOut: {
                        path = []
                        destination = .signIn
                    }
                )
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .player(let videoId):
                        PlayerRoute(
                            videoId: videoId,
                            onClose: { path.removeLast() }
                        )
                    }
                }
            }
        }
    }
}
