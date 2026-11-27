package fr.rlebranchu.ytblight.domain.auth

import fr.rlebranchu.ytblight.fixtures.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionUseCasesTest {
    @Test
    fun checkSessionReturnsTheStateOnceVerified() =
        runTest {
            val valid =
                CheckSessionUseCase(
                    authRepository =
                        FakeAuthRepository(
                            signedIn = true,
                        ),
                )
            val missing =
                CheckSessionUseCase(
                    authRepository =
                        FakeAuthRepository(
                            signedIn = false,
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
                    signedIn = false,
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
                    signedIn = false,
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
