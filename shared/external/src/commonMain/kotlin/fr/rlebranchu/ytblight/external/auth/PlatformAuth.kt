package fr.rlebranchu.ytblight.external.auth

/**
 * Ce que chaque plateforme implémente pour la connexion Google : Play Services sur Android,
 * AppAuth sur iOS, Google Identity Services sur le web. L'écran de connexion lui-même reste
 * dans l'interface native.
 */
interface PlatformAuth : AccessTokenProvider {
    /** Démarre la connexion. Ne lève pas d'exception : les échecs sont rapportés dans le résultat. */
    suspend fun signIn(): PlatformSignInOutcome

    /** Termine la connexion après la fenêtre de consentement. [response] est propre à la plateforme. */
    suspend fun completeConsent(
        response: Any?,
        isGranted: Boolean,
    ): PlatformSignInOutcome

    /** Oublie la session côté plateforme. Ne doit pas échouer si le réseau est coupé. */
    suspend fun signOut()
}
