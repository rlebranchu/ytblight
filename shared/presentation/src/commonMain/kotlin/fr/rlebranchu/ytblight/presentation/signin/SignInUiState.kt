package fr.rlebranchu.ytblight.presentation.signin

data class SignInUiState(
    val inProgress: Boolean = false,
    val error: SignInError? = null,
    /** Précision technique à afficher avec l'erreur (code, message), le cas échéant. */
    val errorDetail: String? = null,
    /** La connexion a réussi : l'interface peut passer au fil. */
    val isSignedIn: Boolean = false,
)

enum class SignInError {
    CANCELLED,

    /** Google refuse l'application : package ou empreinte SHA-1 du client OAuth incorrects. */
    CONFIGURATION,
    FAILED,
    UNAVAILABLE,
}
