import Shared

/// Connexion Google factice : accorde un jeton tout de suite. `hasSession` indique si Google a déjà
/// une session (renouvellement silencieux accepté) ou non (renouvellement silencieux refusé).
final class FakeTokenRequester: TokenRequester, @unchecked Sendable {
    private let hasSession: Bool

    init(hasSession: Bool) {
        self.hasSession = hasSession
    }

    func __request(interactive: Bool) async throws -> any TokenResult {
        if !interactive && !hasSession {
            return TokenResultDenied(reason: .unavailable, detail: "interaction_required")
        }
        return TokenResultGranted(grant: TokenGrant(accessToken: "faux-jeton", expiresInSeconds: 3600))
    }

    func __revoke(accessToken: String) async throws {}
}

final class InMemoryTokenStorage: TokenStorage, @unchecked Sendable {
    private var token: StoredToken?

    func read() -> StoredToken? { token }

    func save(token: StoredToken) { self.token = token }

    func clear() { token = nil }
}
