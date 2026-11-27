import Shared
import SwiftUI

struct FeedRoute: View {
    let onOpenVideo: (String) -> Void
    let onSignedOut: () -> Void

    @State private var viewModel: FeedViewModel?
    @State private var state = FeedUiState(videos: [], isLoading: true, isRefreshing: false, error: nil, isSignedOut: false)

    var body: some View {
        FeedScreen(
            videos: state.videos,
            isLoading: state.isLoading,
            error: state.error,
            onRefresh: { viewModel?.refresh() },
            onRetry: { viewModel?.retry() },
            onSignOut: { viewModel?.signOut() },
            onReconnect: { viewModel?.signOut() },
            onOpenVideo: onOpenVideo
        )
        .task {
            let viewModel = Ytblight.shared.feedViewModel()
            self.viewModel = viewModel
            defer { Ytblight.shared.release(viewModel: viewModel) }
            for await next in viewModel.state {
                state = next
                if next.isSignedOut {
                    onSignedOut()
                    return
                }
            }
        }
    }
}
