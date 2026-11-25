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

    /** À appeler quand la connexion native vient de réussir. */
    fun onSignedIn()

    suspend fun signOut()
}
