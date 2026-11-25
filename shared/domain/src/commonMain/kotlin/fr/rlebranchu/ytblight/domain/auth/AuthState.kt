package fr.rlebranchu.ytblight.domain.auth

enum class AuthState {
    /** Vérification de la session au démarrage. */
    CHECKING,
    SIGNED_OUT,
    SIGNED_IN,
}
