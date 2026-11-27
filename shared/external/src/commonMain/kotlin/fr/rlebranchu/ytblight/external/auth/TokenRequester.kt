package fr.rlebranchu.ytblight.external.auth

/**
 * Demande un jeton d'accès à Google. Implémenté par la plateforme (sur le web : Google Identity
 * Services). Lève [TokenRequestException] si Google n'en accorde pas.
 */
interface TokenRequester {
    /**
     * @param interactive `true` pour afficher la fenêtre de connexion et de consentement de Google ;
     * `false` pour un renouvellement silencieux, sans aucune fenêtre.
     */
    suspend fun request(
        interactive: Boolean,
    ): TokenGrant

    /** Retire l'autorisation donnée à l'application. */
    suspend fun revoke(
        accessToken: String,
    )
}

/** Jeton accordé et sa durée de validité en secondes. */
data class TokenGrant(
    val accessToken: String,
    val expiresInSeconds: Long,
)

class TokenRequestException(
    val reason: PlatformSignInFailure,
    val detail: String? = null,
) : Exception(detail)
