package fr.rlebranchu.ytblight.presentation.signin

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInViewModelTest {
    private class FakeAuthRepository : AuthRepository {
        override val state: StateFlow<AuthState> = MutableStateFlow(AuthState.SIGNED_OUT)
        var signedInCalls = 0

        override suspend fun checkSession() = Unit

        override fun onSignedIn() {
            signedInCalls++
        }

        override suspend fun signOut() = Unit
    }

    private val auth = FakeAuthRepository()
    private val viewModel =
        SignInViewModel(
            authRepository = auth,
        )

    @Test
    fun showsProgressWhileSigningIn() {
        viewModel.onStarted()

        assertEquals(SignInUiState(inProgress = true), viewModel.state.value)
    }

    @Test
    fun notifiesTheSessionAndAsksToLeaveThePageOnSuccess() {
        viewModel.onStarted()
        viewModel.onSucceeded()

        assertEquals(1, auth.signedInCalls)
        assertEquals(SignInUiState(isSignedIn = true), viewModel.state.value)
    }

    @Test
    fun keepsTheErrorUntilTheNextAttempt() {
        viewModel.onStarted()
        viewModel.onFailed(error = SignInError.CONFIGURATION, detail = "10")

        assertEquals(
            SignInUiState(error = SignInError.CONFIGURATION, errorDetail = "10"),
            viewModel.state.value,
        )

        viewModel.onStarted()

        assertEquals(SignInUiState(inProgress = true), viewModel.state.value)
        assertEquals(0, auth.signedInCalls)
    }
}
