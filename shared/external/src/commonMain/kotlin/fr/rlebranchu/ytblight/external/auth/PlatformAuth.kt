package fr.rlebranchu.ytblight.external.auth

/**
 * Ce que chaque plateforme implémente pour la connexion Google : Play Services sur Android,
 * AppAuth sur iOS, Google Identity Services sur le web. L'écran de connexion lui-même reste
 * dans l'interface native.
 */
interface PlatformAuth : AccessTokenProvider {
    /** Oublie la session côté plateforme. Ne doit pas échouer si le réseau est coupé. */
    suspend fun signOut()
}
