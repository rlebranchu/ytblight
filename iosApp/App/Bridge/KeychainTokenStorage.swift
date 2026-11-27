import Foundation
import Security
import Shared

/// Conserve le jeton d'accès (et son expiration) dans le Trousseau : seule donnée stockée, la
/// connexion en a besoin. Implémente l'interface Kotlin `TokenStorage`.
final class KeychainTokenStorage: TokenStorage, @unchecked Sendable {
    private let service = "fr.rlebranchu.ytblight.token"
    private let accessTokenAccount = "accessToken"
    private let expirationAccount = "expiresAt"

    func read() -> StoredToken? {
        guard
            let token = readString(account: accessTokenAccount),
            let expiration = readString(account: expirationAccount),
            let expiresAt = Int64(expiration)
        else { return nil }
        return StoredToken(accessToken: token, expiresAtEpochMillis: expiresAt)
    }

    func save(token: StoredToken) {
        write(value: token.accessToken, account: accessTokenAccount)
        write(value: String(token.expiresAtEpochMillis), account: expirationAccount)
    }

    func clear() {
        delete(account: accessTokenAccount)
        delete(account: expirationAccount)
    }

    private func query(account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    private func readString(account: String) -> String? {
        var request = query(account: account)
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess,
            let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func write(value: String, account: String) {
        delete(account: account)
        var request = query(account: account)
        request[kSecValueData as String] = Data(value.utf8)
        request[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(request as CFDictionary, nil)
    }

    private func delete(account: String) {
        SecItemDelete(query(account: account) as CFDictionary)
    }
}
