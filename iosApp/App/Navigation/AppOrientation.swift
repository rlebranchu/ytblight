import UIKit

/// Portrait partout, sauf sur la page du lecteur qui autorise aussi le paysage.
final class AppDelegate: NSObject, UIApplicationDelegate {
    nonisolated(unsafe) static var allowsLandscape = false

    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        AppDelegate.allowsLandscape ? .allButUpsideDown : .portrait
    }

    /// Autorise (ou non) le paysage et recadre l'écran si besoin.
    @MainActor
    static func setLandscapeAllowed(_ allowed: Bool) {
        allowsLandscape = allowed
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
        scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        if !allowed {
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: .portrait))
        }
    }
}
