package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInResult
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

    override suspend fun signIn(): SignInResult {
        mutableState.value = AuthState.SIGNED_IN
        return SignInResult.SignedIn
    }

    override suspend fun completeConsent(
        response: ConsentResponse,
    ): SignInResult = signIn()

    override suspend fun signOut() {
        mutableState.value = AuthState.SIGNED_OUT
    }
}
