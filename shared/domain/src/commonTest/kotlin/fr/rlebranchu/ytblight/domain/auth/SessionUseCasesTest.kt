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

        override suspend fun signIn(): SignInResult {
            mutableState.value = AuthState.SIGNED_IN
            return SignInResult.SignedIn
        }

        override suspend fun completeConsent(
            response: ConsentResponse,
        ): SignInResult = signIn()

        override suspend fun signOut() = Unit
    }

    @Test
    fun checkSessionReturnsTheStateOnceVerified() =
        runTest {
            val valid =
                CheckSessionUseCase(
                    authRepository =
                        FakeAuthRepository(
                            sessionIsValid = true,
                        ),
                )
            val missing =
                CheckSessionUseCase(
                    authRepository =
                        FakeAuthRepository(
                            sessionIsValid = false,
                        ),
                )

            assertEquals(AuthState.SIGNED_IN, valid())
            assertEquals(AuthState.SIGNED_OUT, missing())
        }

    @Test
    fun signInMarksTheSessionAsSignedIn() =
        runTest {
            val repository =
                FakeAuthRepository(
                    sessionIsValid = false,
                )

            val result =
                SignInUseCase(
                    authRepository = repository,
                )()

            assertEquals(SignInResult.SignedIn, result)
            assertEquals(AuthState.SIGNED_IN, repository.state.value)
        }

    @Test
    fun completeConsentFinishesTheSignIn() =
        runTest {
            val repository =
                FakeAuthRepository(
                    sessionIsValid = false,
                )

            val result =
                CompleteConsentUseCase(
                    authRepository = repository,
                )(
                    response =
                        ConsentResponse(
                            isGranted = true,
                            data = null,
                        ),
                )

            assertEquals(SignInResult.SignedIn, result)
        }
}
