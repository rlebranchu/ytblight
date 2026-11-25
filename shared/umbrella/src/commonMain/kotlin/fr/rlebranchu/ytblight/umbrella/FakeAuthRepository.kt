package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session factice pour les tests d'interface : `signedIn` décide de l'écran de départ. */
class FakeAuthRepository(
    private val signedIn: Boolean,
) : AuthRepository {
    private val mutableState = MutableStateFlow(AuthState.CHECKING)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    override suspend fun checkSession() {
        mutableState.value = if (signedIn) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
    }

    override fun onSignedIn() {
        mutableState.value = AuthState.SIGNED_IN
    }

    override suspend fun signOut() {
        mutableState.value = AuthState.SIGNED_OUT
    }
}
