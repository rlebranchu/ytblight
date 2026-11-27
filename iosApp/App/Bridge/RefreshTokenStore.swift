import Foundation
import Security

/// Jeton de rafraîchissement de Google (client iOS : accordé sans serveur). Il reste dans le
/// Trousseau et permet de renouveler le jeton d'accès sans rouvrir la fenêtre de connexion.
struct RefreshTokenStore: Sendable {
    private let service = "fr.rlebranchu.ytblight.refresh"
    private let account = "refreshToken"

    private var query: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    func read() -> String? {
        var request = query
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess,
            let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func save(_ token: String) {
        clear()
        var request = query
        request[kSecValueData as String] = Data(token.utf8)
        request[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(request as CFDictionary, nil)
    }

    func clear() {
        SecItemDelete(query as CFDictionary)
    }
}
