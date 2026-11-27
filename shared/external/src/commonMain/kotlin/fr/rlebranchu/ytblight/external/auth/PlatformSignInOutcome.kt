package fr.rlebranchu.ytblight.external.auth

/** Résultat d'une connexion côté plateforme. */
sealed interface PlatformSignInOutcome {
    data object Authorized : PlatformSignInOutcome

    /** La plateforme demande l'affichage de sa fenêtre de consentement ; [request] lui est propre (opaque). */
    data class ConsentRequired(
        val request: Any,
    ) : PlatformSignInOutcome

    data class Failed(
        val reason: PlatformSignInFailure,
        val detail: String? = null,
    ) : PlatformSignInOutcome
}

enum class PlatformSignInFailure {
    CANCELLED,
    CONFIGURATION,
    FAILED,
    UNAVAILABLE,
}
