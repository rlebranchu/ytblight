@file:Suppress("EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE")

package fr.rlebranchu.ytblight.umbrella

import kotlin.js.Promise

/**
 * Pont vers Google Identity Services, écrit en TypeScript (`webApp`). Ses promesses ne sont jamais
 * rejetées : un échec est décrit par [GoogleTokenResponse.error].
 */
external interface GoogleIdentityBridge {
    /** `interactive = false` : renouvellement silencieux, sans fenêtre. */
    fun requestToken(
        interactive: Boolean,
    ): Promise<GoogleTokenResponse>

    fun revokeToken(
        accessToken: String,
    ): Promise<Any?>
}

external interface GoogleTokenResponse {
    val accessToken: String?
    val expiresInSeconds: Double?

    /** Code d'erreur de Google (`popup_closed`, `access_denied`…) ; absent en cas de succès. */
    val error: String?
}
