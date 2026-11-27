package fr.rlebranchu.ytblight.presentation.signin

import fr.rlebranchu.ytblight.domain.auth.ConsentRequest

data class SignInUiState(
    val inProgress: Boolean = false,
    val error: SignInUiError? = null,
    /** Précision technique à afficher avec l'erreur (code, message), le cas échéant. */
    val errorDetail: String? = null,
    /** La connexion a réussi : l'interface peut passer au fil. */
    val isSignedIn: Boolean = false,
    /** La plateforme doit afficher sa fenêtre de consentement Google (voir `onConsentLaunched`). */
    val consentRequest: ConsentRequest? = null,
)

enum class SignInUiError {
    CANCELLED,

    /** Google refuse l'application : package ou empreinte SHA-1 du client OAuth incorrects. */
    CONFIGURATION,
    FAILED,
    UNAVAILABLE,
}
