package fr.rlebranchu.ytblight.external.auth

/**
 * Fournit le jeton OAuth Google. Implémenté par chaque plateforme (AppAuth, Google Identity Services),
 * le code partagé ne fait que l'utiliser.
 */
interface AccessTokenProvider {
    /** Jeton d'accès courant, ou `null` si l'utilisateur n'est pas connecté. */
    suspend fun currentToken(): String?

    /** Obtient un nouveau jeton (le précédent a été refusé), ou `null` s'il faut se reconnecter. */
    suspend fun refreshToken(): String?
}
