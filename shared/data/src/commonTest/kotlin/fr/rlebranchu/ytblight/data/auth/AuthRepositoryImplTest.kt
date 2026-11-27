package fr.rlebranchu.ytblight.data.auth

import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInError
import fr.rlebranchu.ytblight.domain.auth.SignInResult
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.PlatformSignInFailure
import fr.rlebranchu.ytblight.external.auth.PlatformSignInOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRepositoryImplTest {
    private class FakePlatformAuth(
        var token: String? = null,
        var tokenError: Exception? = null,
        var signOutError: Exception? = null,
        var signInOutcome: PlatformSignInOutcome = PlatformSignInOutcome.Authorized,
    ) : PlatformAuth {
        var signOutCalls = 0
        var consentResponses = mutableListOf<Any?>()

        override suspend fun currentToken(): String? = tokenError?.let { throw it } ?: token

        override suspend fun refreshToken(): String? = token

        override suspend fun signIn(): PlatformSignInOutcome = signInOutcome

        override suspend fun completeConsent(
            response: Any?,
            isGranted: Boolean,
        ): PlatformSignInOutcome {
            consentResponses += response
            return signInOutcome
        }

        override suspend fun signOut() {
            signOutCalls++
            signOutError?.let { throw it }
        }
    }

    @Test
    fun startsByCheckingTheSession() {
        val repository =
            AuthRepositoryImpl(
                platformAuth = FakePlatformAuth(),
            )
        assertEquals(AuthState.CHECKING, repository.state.value)
    }

    @Test
    fun isSignedInWhenThePlatformHasAToken() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            token = "jeton",
                        ),
                )

            repository.checkSession()

            assertEquals(AuthState.SIGNED_IN, repository.state.value)
        }

    @Test
    fun isSignedOutWhenThereIsNoToken() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            token = null,
                        ),
                )

            repository.checkSession()

            assertEquals(AuthState.SIGNED_OUT, repository.state.value)
        }

    @Test
    fun fallsBackToSignedOutWhenThePlatformFails() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            tokenError = IllegalStateException("Play Services absent"),
                        ),
                )

            repository.checkSession()

            assertEquals(AuthState.SIGNED_OUT, repository.state.value)
        }

    @Test
    fun becomesSignedInWhenThePlatformAuthorizes() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth = FakePlatformAuth(),
                )

            val result = repository.signIn()

            assertEquals(SignInResult.SignedIn, result)
            assertEquals(AuthState.SIGNED_IN, repository.state.value)
        }

    @Test
    fun passesTheConsentRequestToTheInterfaceWithoutSigningIn() =
        runTest {
            val handle = Any()
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            signInOutcome =
                                PlatformSignInOutcome.ConsentRequired(
                                    request = handle,
                                ),
                        ),
                )

            val result = repository.signIn()

            assertEquals(handle, (result as SignInResult.ConsentRequired).request.handle)
            assertEquals(AuthState.CHECKING, repository.state.value)
        }

    @Test
    fun finishesTheSignInAfterTheConsent() =
        runTest {
            val platform = FakePlatformAuth()
            val repository =
                AuthRepositoryImpl(
                    platformAuth = platform,
                )
            val data = Any()

            val result =
                repository.completeConsent(
                    response =
                        ConsentResponse(
                            isGranted = true,
                            data = data,
                        ),
                )

            assertEquals(SignInResult.SignedIn, result)
            assertEquals(listOf<Any?>(data), platform.consentResponses)
            assertEquals(AuthState.SIGNED_IN, repository.state.value)
        }

    @Test
    fun translatesPlatformFailures() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            signInOutcome =
                                PlatformSignInOutcome.Failed(
                                    reason = PlatformSignInFailure.CONFIGURATION,
                                    detail = "10",
                                ),
                        ),
                )

            val result = repository.signIn()

            assertEquals(
                SignInResult.Failed(
                    error = SignInError.CONFIGURATION,
                    detail = "10",
                ),
                result,
            )
            assertEquals(AuthState.CHECKING, repository.state.value)
        }

    @Test
    fun signsOutThroughThePlatform() =
        runTest {
            val platform =
                FakePlatformAuth(
                    token = "jeton",
                )
            val repository =
                AuthRepositoryImpl(
                    platformAuth = platform,
                )
            repository.checkSession()

            repository.signOut()

            assertEquals(1, platform.signOutCalls)
            assertEquals(AuthState.SIGNED_OUT, repository.state.value)
        }

    @Test
    fun signsOutLocallyEvenWhenThePlatformFails() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    platformAuth =
                        FakePlatformAuth(
                            token = "jeton",
                            signOutError = IllegalStateException("hors ligne"),
                        ),
                )
            repository.signIn()

            repository.signOut()

            assertEquals(AuthState.SIGNED_OUT, repository.state.value)
        }
}
