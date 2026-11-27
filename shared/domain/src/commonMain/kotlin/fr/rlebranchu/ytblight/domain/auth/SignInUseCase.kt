package fr.rlebranchu.ytblight.domain.auth

/** Démarre la connexion Google. */
class SignInUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): SignInResult = authRepository.signIn()
}
