package fr.rlebranchu.ytblight.local.token

import kotlinx.browser.localStorage

/** Garde le jeton dans le `localStorage` du navigateur : il survit au rechargement de la page. */
class BrowserTokenStorage : TokenStorage {
    override fun read(): StoredToken? {
        val token = localStorage.getItem(KEY_TOKEN) ?: return null
        val expiresAt = localStorage.getItem(KEY_EXPIRES_AT)?.toLongOrNull() ?: return null
        return StoredToken(
            accessToken = token,
            expiresAtEpochMillis = expiresAt,
        )
    }

    override fun save(
        token: StoredToken,
    ) {
        localStorage.setItem(KEY_TOKEN, token.accessToken)
        localStorage.setItem(KEY_EXPIRES_AT, token.expiresAtEpochMillis.toString())
    }

    override fun clear() {
        localStorage.removeItem(KEY_TOKEN)
        localStorage.removeItem(KEY_EXPIRES_AT)
    }

    private companion object {
        const val KEY_TOKEN = "ytblight.token"
        const val KEY_EXPIRES_AT = "ytblight.token.expiresAt"
    }
}
