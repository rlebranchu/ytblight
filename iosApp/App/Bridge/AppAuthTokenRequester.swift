import AppAuth
import Shared
import UIKit

/// Connexion Google par AppAuth (code + PKCE, client OAuth « iOS », sans serveur ni secret).
/// Implémente l'interface Kotlin `TokenRequester` : la fenêtre de connexion pour `interactive`,
/// le jeton de rafraîchissement du Trousseau pour le renouvellement silencieux.
final class AppAuthTokenRequester: TokenRequester, @unchecked Sendable {
    private let clientId = "122414469789-pq47lbn92sqimp3eo0aefvctm9719sgp.apps.googleusercontent.com"
    private let redirectURL = URL(
        string: "com.googleusercontent.apps.122414469789-pq47lbn92sqimp3eo0aefvctm9719sgp:/oauth2redirect"
    )!
    private let scope = "https://www.googleapis.com/auth/youtube.readonly"
    private let configuration = OIDServiceConfiguration(
        authorizationEndpoint: URL(string: "https://accounts.google.com/o/oauth2/v2/auth")!,
        tokenEndpoint: URL(string: "https://oauth2.googleapis.com/token")!
    )
    private let refreshTokens = RefreshTokenStore()

    @MainActor private var currentFlow: (any OIDExternalUserAgentSession)?

    func __request(interactive: Bool) async throws -> any TokenResult {
        if interactive {
            return await signIn()
        }
        return await renew()
    }

    func __revoke(accessToken: String) async throws {
        let token = refreshTokens.read() ?? accessToken
        refreshTokens.clear()
        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/revoke")!)
        request.httpMethod = "POST"
        request.httpBody = "token=\(token)".data(using: .utf8)
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        // Hors ligne : la déconnexion locale suffit.
        _ = try? await URLSession.shared.data(for: request)
    }

    // MARK: Connexion (fenêtre Google)

    @MainActor
    private func signIn() async -> any TokenResult {
        guard let presenter = Self.topViewController() else {
            return TokenResultDenied(reason: .unavailable, detail: "no_presenter")
        }
        let request = OIDAuthorizationRequest(
            configuration: configuration,
            clientId: clientId,
            clientSecret: nil,
            scopes: [scope],
            redirectURL: redirectURL,
            responseType: OIDResponseTypeCode,
            additionalParameters: nil
        )
        return await withCheckedContinuation { continuation in
            currentFlow = OIDAuthState.authState(byPresenting: request, presenting: presenter) {
                [weak self] state, error in
                Task { @MainActor in self?.currentFlow = nil }
                continuation.resume(returning: Self.result(state: state, error: error, store: self?.refreshTokens))
            }
        }
    }

    // MARK: Renouvellement silencieux

    private func renew() async -> any TokenResult {
        guard let refreshToken = refreshTokens.read() else {
            return TokenResultDenied(reason: .unavailable, detail: "no_refresh_token")
        }
        let request = OIDTokenRequest(
            configuration: configuration,
            grantType: OIDGrantTypeRefreshToken,
            authorizationCode: nil,
            redirectURL: nil,
            clientID: clientId,
            clientSecret: nil,
            scope: scope,
            refreshToken: refreshToken,
            codeVerifier: nil,
            additionalParameters: nil
        )
        return await withCheckedContinuation { continuation in
            OIDAuthorizationService.perform(request) { response, error in
                guard let accessToken = response?.accessToken else {
                    continuation.resume(returning: TokenResultDenied(reason: .unavailable, detail: error?.localizedDescription))
                    return
                }
                continuation.resume(returning: Self.granted(accessToken: accessToken, expiration: response?.accessTokenExpirationDate))
            }
        }
    }

    // MARK: Conversion des réponses

    private static func result(state: OIDAuthState?, error: Error?, store: RefreshTokenStore?) -> any TokenResult {
        guard let state, let response = state.lastTokenResponse, let accessToken = response.accessToken else {
            return denied(error: error)
        }
        if let refresh = response.refreshToken {
            store?.save(refresh)
        }
        return granted(accessToken: accessToken, expiration: response.accessTokenExpirationDate)
    }

    private static func granted(accessToken: String, expiration: Date?) -> any TokenResult {
        let seconds = Int64(expiration?.timeIntervalSinceNow ?? 3600)
        return TokenResultGranted(grant: TokenGrant(accessToken: accessToken, expiresInSeconds: seconds))
    }

    private static func denied(error: Error?) -> any TokenResult {
        let nsError = error as NSError?
        if nsError?.domain == OIDGeneralErrorDomain,
            nsError?.code == OIDErrorCode.userCanceledAuthorizationFlow.rawValue
        {
            return TokenResultDenied(reason: .cancelled, detail: nil)
        }
        return TokenResultDenied(reason: .unavailable, detail: nsError?.localizedDescription)
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var controller = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
