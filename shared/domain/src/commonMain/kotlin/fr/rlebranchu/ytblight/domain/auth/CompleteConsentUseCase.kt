package fr.rlebranchu.ytblight.domain.auth

/** Termine la connexion une fois la fenêtre de consentement de la plateforme refermée. */
class CompleteConsentUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        response: ConsentResponse,
    ): SignInResult =
        authRepository.completeConsent(
            response = response,
        )
}
