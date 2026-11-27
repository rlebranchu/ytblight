package fr.rlebranchu.ytblight.domain.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Session de l'utilisateur. La page de connexion Google est propre à chaque plateforme :
 * le code partagé ne fait que suivre son résultat.
 */
interface AuthRepository {
    val state: StateFlow<AuthState>

    /** Vérifie, sans rien afficher, si une session valide existe déjà. */
    suspend fun checkSession()

    /** Démarre la connexion. Peut demander l'affichage de la fenêtre de consentement de la plateforme. */
    suspend fun signIn(): SignInResult

    /** Termine la connexion après la fenêtre de consentement. */
    suspend fun completeConsent(
        response: ConsentResponse,
    ): SignInResult

    suspend fun signOut()
}
