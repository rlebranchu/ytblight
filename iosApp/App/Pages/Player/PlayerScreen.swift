import Combine
import SwiftUI
import YouTubePlayerKit

/// Lecteur officiel YouTube (iframe). Seule page où le paysage est autorisé. Pas de lecture
/// suivante : la page se ferme à la fin de la vidéo.
struct PlayerScreen: View {
    let videoId: String
    let onVideoEnded: () -> Void

    @State private var player: YouTubePlayer

    init(videoId: String, onVideoEnded: @escaping () -> Void) {
        self.videoId = videoId
        self.onVideoEnded = onVideoEnded
        _player = State(
            initialValue: YouTubePlayer(
                source: .video(id: videoId),
                parameters: .init(autoPlay: true, showControls: true)
            )
        )
    }

    var body: some View {
        YouTubePlayerView(player)
            .background(Color.black)
            .ignoresSafeArea(edges: .bottom)
            .onAppear { AppDelegate.setLandscapeAllowed(true) }
            .onDisappear { AppDelegate.setLandscapeAllowed(false) }
            .onReceive(player.playbackStatePublisher) { state in
                if state == .ended { onVideoEnded() }
            }
    }
}
