package fr.rlebranchu.ytblight.domain.auth

/** Résultat d'une tentative de connexion. */
sealed interface SignInResult {
    /** L'utilisateur est connecté. */
    data object SignedIn : SignInResult

    /** La plateforme doit afficher sa fenêtre de consentement ; son résultat revient via [ConsentResponse]. */
    data class ConsentRequired(
        val request: ConsentRequest,
    ) : SignInResult

    data class Failed(
        val error: SignInError,
        /** Précision technique (code, message), le cas échéant. */
        val detail: String? = null,
    ) : SignInResult
}

enum class SignInError {
    CANCELLED,

    /** Google refuse l'application : package ou empreinte SHA-1 du client OAuth incorrects. */
    CONFIGURATION,
    FAILED,
    UNAVAILABLE,
}

/**
 * Demande d'affichage de la fenêtre de consentement de la plateforme. [handle] est opaque pour le
 * code partagé : seule l'interface de la plateforme sait l'utiliser (sur Android, un `IntentSender`).
 */
class ConsentRequest(
    val handle: Any,
)

/**
 * Réponse de la fenêtre de consentement. [data] est opaque pour le code partagé : seule la plateforme
 * sait la lire (sur Android, l'`Intent` de résultat).
 */
class ConsentResponse(
    val isGranted: Boolean,
    val data: Any?,
)
