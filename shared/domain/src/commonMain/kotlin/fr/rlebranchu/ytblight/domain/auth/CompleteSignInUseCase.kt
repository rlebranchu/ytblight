package fr.rlebranchu.ytblight.domain.auth

/** À appeler quand la connexion native (fenêtre Google de la plateforme) vient de réussir. */
class CompleteSignInUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() {
        authRepository.onSignedIn()
    }
}
