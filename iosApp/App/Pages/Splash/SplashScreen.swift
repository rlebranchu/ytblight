import SwiftUI

struct SplashScreen: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel(Text("loading"))
    }
}
