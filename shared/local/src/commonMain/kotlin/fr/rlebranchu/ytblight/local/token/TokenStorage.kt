package fr.rlebranchu.ytblight.local.token

/**
 * Jeton d'accès OAuth conservé entre deux visites (web). Seule donnée stockée par l'application :
 * la connexion en a besoin. [expiresAtEpochMillis] est la date d'expiration en millisecondes UTC.
 */
data class StoredToken(
    val accessToken: String,
    val expiresAtEpochMillis: Long,
)

/** Stockage persistant du jeton, propre à chaque plateforme (sur le web : le stockage du navigateur). */
interface TokenStorage {
    fun read(): StoredToken?

    fun save(
        token: StoredToken,
    )

    fun clear()
}
