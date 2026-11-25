package fr.rlebranchu.ytblight.domain.error

/** Les erreurs métier que les interfaces savent expliquer à l'utilisateur. */
sealed interface DomainError {
    /** Aucun jeton valide : il faut se (re)connecter. */
    data object NotSignedIn : DomainError

    /** Le quota quotidien de l'API YouTube est épuisé. */
    data object QuotaExceeded : DomainError

    /** Pas de réseau, ou réseau injoignable. */
    data object Network : DomainError

    data class Unknown(
        val message: String?,
    ) : DomainError
}
