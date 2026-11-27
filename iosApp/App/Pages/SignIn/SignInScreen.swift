import Shared
import SwiftUI

struct SignInScreen: View {
    let inProgress: Bool
    let error: SignInUiError?
    let errorDetail: String?
    let onSignIn: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Text("app.name")
                .font(.largeTitle.bold())
            Text("signin.message")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            if inProgress {
                ProgressView()
            } else {
                Button("signin.button", action: onSignIn)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .accessibilityIdentifier("signin.button")
            }
            if let error {
                Text(message(for: error))
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .accessibilityIdentifier("signin.error")
            }
            Spacer()
        }
        .padding(32)
    }

    private func message(for error: SignInUiError) -> LocalizedStringKey {
        switch error {
        case .cancelled: "signin.error.cancelled"
        case .configuration: "signin.error.configuration"
        case .failed: "signin.error.failed"
        case .unavailable:
            if let errorDetail {
                "signin.error.unavailable.detail \(errorDetail)"
            } else {
                "signin.error.unavailable"
            }
        }
    }
}
