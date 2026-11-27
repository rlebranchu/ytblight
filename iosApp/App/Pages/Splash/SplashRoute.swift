import Shared
import SwiftUI

struct SplashRoute: View {
    let onNavigate: (SplashDestination) -> Void

    @State private var destination: SplashDestination?

    var body: some View {
        SplashScreen()
            .task {
                let viewModel = Ytblight.shared.splashViewModel()
                defer { Ytblight.shared.release(viewModel: viewModel) }
                for await state in viewModel.state {
                    if let next = state.destination {
                        onNavigate(next)
                        return
                    }
                }
            }
    }
}
