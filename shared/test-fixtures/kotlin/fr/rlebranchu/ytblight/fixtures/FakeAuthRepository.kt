package fr.rlebranchu.ytblight.fixtures

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Session factice, configurable.
 * @param signedIn résultat de `checkSession()` : `true` démarre connecté, `false` sur la connexion.
 */
class FakeAuthRepository(
    private val signedIn: Boolean = true,
    var signInResult: SignInResult = SignInResult.SignedIn,
    var consentResult: SignInResult = SignInResult.SignedIn,
) : AuthRepository {
    private val mutableState = MutableStateFlow(AuthState.CHECKING)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    var signInCalls = 0
        private set
    var signOutCalls = 0
        private set
    val consentResponses = mutableListOf<ConsentResponse>()

    override suspend fun checkSession() {
        mutableState.value = if (signedIn) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
    }

    override suspend fun signIn(): SignInResult {
        signInCalls++
        return signInResult.also(::updateState)
    }

    override suspend fun completeConsent(
        response: ConsentResponse,
    ): SignInResult {
        consentResponses += response
        return consentResult.also(::updateState)
    }

    override suspend fun signOut() {
        signOutCalls++
        mutableState.value = AuthState.SIGNED_OUT
    }

    private fun updateState(
        result: SignInResult,
    ) {
        if (result == SignInResult.SignedIn) mutableState.value = AuthState.SIGNED_IN
    }
}
