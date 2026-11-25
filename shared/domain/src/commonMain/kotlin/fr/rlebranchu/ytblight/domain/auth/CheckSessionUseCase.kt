package fr.rlebranchu.ytblight.domain.auth

/** Vérifie, sans rien afficher, si une session valide existe déjà au démarrage. */
class CheckSessionUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AuthState {
        authRepository.checkSession()
        return authRepository.state.value
    }
}
