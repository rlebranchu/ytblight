package fr.rlebranchu.ytblight.data.auth

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val platformAuth: PlatformAuth,
) : AuthRepository {
    private val mutableState = MutableStateFlow(AuthState.CHECKING)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    override suspend fun checkSession() {
        val hasToken =
            try {
                platformAuth.currentToken() != null
            } catch (e: CancellationException) {
                throw e
            } catch (
                // Plateforme indisponible, hors ligne… : on repasse par l'écran de connexion.
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                false
            }
        mutableState.value = if (hasToken) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
    }

    override fun onSignedIn() {
        mutableState.value = AuthState.SIGNED_IN
    }

    override suspend fun signOut() {
        try {
            platformAuth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (
            // Hors ligne : la déconnexion locale doit réussir malgré tout.
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // ignoré volontairement
        } finally {
            mutableState.value = AuthState.SIGNED_OUT
        }
    }
}
