package fr.rlebranchu.ytblight.presentation.splash

data class SplashUiState(
    /** `null` tant que la session est en cours de vérification. */
    val destination: SplashDestination? = null,
)

enum class SplashDestination {
    SIGN_IN,
    FEED,
}
