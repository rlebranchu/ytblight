package fr.rlebranchu.ytblight.external.auth

/**
 * Demande un jeton d'accès à Google. Implémenté par la plateforme (web : Google Identity Services,
 * iOS : AppAuth). Ne lève pas d'exception : un refus est rapporté par [TokenResult.Denied].
 */
interface TokenRequester {
    /**
     * @param interactive `true` pour afficher la fenêtre de connexion et de consentement de Google ;
     * `false` pour un renouvellement silencieux, sans aucune fenêtre.
     */
    suspend fun request(
        interactive: Boolean,
    ): TokenResult

    /** Retire l'autorisation donnée à l'application (sans échec si le réseau est coupé). */
    suspend fun revoke(
        accessToken: String,
    )
}

/** Jeton accordé et sa durée de validité en secondes. */
data class TokenGrant(
    val accessToken: String,
    val expiresInSeconds: Long,
)

/** Réponse de Google à une demande de jeton : un échec est un résultat, pas une exception. */
sealed interface TokenResult {
    data class Granted(
        val grant: TokenGrant,
    ) : TokenResult

    data class Denied(
        val reason: PlatformSignInFailure,
        val detail: String? = null,
    ) : TokenResult
}
