import Shared
import SwiftUI

struct PlayerRoute: View {
    let videoId: String
    let onClose: () -> Void

    var body: some View {
        PlayerScreen(videoId: videoId, onVideoEnded: onClose)
            .task {
                let viewModel = Ytblight.shared.playerViewModel(videoId: videoId)
                defer { Ytblight.shared.release(viewModel: viewModel) }
                for await state in viewModel.state where state.isEnded {
                    onClose()
                    return
                }
            }
    }
}
