import Shared
import SwiftUI

struct SignInRoute: View {
    let onSignedIn: () -> Void

    @State private var viewModel: SignInViewModel?
    @State private var state = SignInUiState(inProgress: false, error: nil, errorDetail: nil, isSignedIn: false, consentRequest: nil)

    var body: some View {
        SignInScreen(
            inProgress: state.inProgress,
            error: state.error,
            errorDetail: state.errorDetail,
            onSignIn: { viewModel?.onSignInClicked() }
        )
        .task {
            let viewModel = Ytblight.shared.signInViewModel()
            self.viewModel = viewModel
            defer { Ytblight.shared.release(viewModel: viewModel) }
            for await next in viewModel.state {
                state = next
                if next.isSignedIn {
                    onSignedIn()
                    return
                }
            }
        }
    }
}
