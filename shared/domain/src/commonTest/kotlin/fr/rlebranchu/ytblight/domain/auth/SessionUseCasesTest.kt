package fr.rlebranchu.ytblight.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionUseCasesTest {
    private class FakeAuthRepository(
        private val sessionIsValid: Boolean,
    ) : AuthRepository {
        private val mutableState = MutableStateFlow(AuthState.CHECKING)
        override val state: StateFlow<AuthState> = mutableState

        override suspend fun checkSession() {
            mutableState.value = if (sessionIsValid) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
        }

        override fun onSignedIn() {
            mutableState.value = AuthState.SIGNED_IN
        }

        override suspend fun signOut() = Unit
    }

    @Test
    fun checkSessionReturnsTheStateOnceVerified() =
        runTest {
            val valid = CheckSessionUseCase(authRepository = FakeAuthRepository(sessionIsValid = true))
            val missing = CheckSessionUseCase(authRepository = FakeAuthRepository(sessionIsValid = false))

            assertEquals(AuthState.SIGNED_IN, valid())
            assertEquals(AuthState.SIGNED_OUT, missing())
        }

    @Test
    fun completeSignInMarksTheSessionAsSignedIn() {
        val repository = FakeAuthRepository(sessionIsValid = false)

        CompleteSignInUseCase(authRepository = repository)()

        assertEquals(AuthState.SIGNED_IN, repository.state.value)
    }
}
