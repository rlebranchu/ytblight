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
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        token = "jeton",
                    ),
            ).apply {
                checkSession()
                assertEquals(AuthState.SIGNED_IN, state.value)
            }
        }

    @Test
    fun isSignedOutWhenThereIsNoToken() =
        runTest {
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        token = null,
                    ),
            ).apply {
                checkSession()
                assertEquals(AuthState.SIGNED_OUT, state.value)
            }
        }

    @Test
    fun fallsBackToSignedOutWhenThePlatformFails() =
        runTest {
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        tokenError = IllegalStateException("Play Services absent"),
                    ),
            ).apply {
                checkSession()
                assertEquals(AuthState.SIGNED_OUT, state.value)
            }
        }

    @Test
    fun becomesSignedInWhenThePlatformAuthorizes() =
        runTest {
            AuthRepositoryImpl(
                platformAuth = FakePlatformAuth(),
            ).apply {
                assertEquals(SignInResult.SignedIn, signIn())
                assertEquals(AuthState.SIGNED_IN, state.value)
            }
        }

    @Test
    fun passesTheConsentRequestToTheInterfaceWithoutSigningIn() =
        runTest {
            val handle = Any()
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        signInOutcome =
                            PlatformSignInOutcome.ConsentRequired(
                                request = handle,
                            ),
                    ),
            ).apply {
                assertEquals(handle, (signIn() as SignInResult.ConsentRequired).request.handle)
                assertEquals(AuthState.CHECKING, state.value)
            }
        }

    @Test
    fun finishesTheSignInAfterTheConsent() =
        runTest {
            val platform = FakePlatformAuth()
            val data = Any()
            AuthRepositoryImpl(
                platformAuth = platform,
            ).apply {
                assertEquals(
                    SignInResult.SignedIn,
                    completeConsent(
                        response =
                            ConsentResponse(
                                isGranted = true,
                                data = data,
                            ),
                    ),
                )
                assertEquals(listOf<Any?>(data), platform.consentResponses)
                assertEquals(AuthState.SIGNED_IN, state.value)
            }
        }

    @Test
    fun translatesPlatformFailures() =
        runTest {
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        signInOutcome =
                            PlatformSignInOutcome.Failed(
                                reason = PlatformSignInFailure.CONFIGURATION,
                                detail = "10",
                            ),
                    ),
            ).apply {
                assertEquals(
                    SignInResult.Failed(
                        error = SignInError.CONFIGURATION,
                        detail = "10",
                    ),
                    signIn(),
                )
                assertEquals(AuthState.CHECKING, state.value)
            }
        }

    @Test
    fun signsOutThroughThePlatform() =
        runTest {
            val platform =
                FakePlatformAuth(
                    token = "jeton",
                )
            AuthRepositoryImpl(
                platformAuth = platform,
            ).apply {
                checkSession()
                signOut()

                assertEquals(1, platform.signOutCalls)
                assertEquals(AuthState.SIGNED_OUT, state.value)
            }
        }

    @Test
    fun signsOutLocallyEvenWhenThePlatformFails() =
        runTest {
            AuthRepositoryImpl(
                platformAuth =
                    FakePlatformAuth(
                        token = "jeton",
                        signOutError = IllegalStateException("hors ligne"),
                    ),
            ).apply {
                signIn()
                signOut()

                assertEquals(AuthState.SIGNED_OUT, state.value)
            }
        }
}
