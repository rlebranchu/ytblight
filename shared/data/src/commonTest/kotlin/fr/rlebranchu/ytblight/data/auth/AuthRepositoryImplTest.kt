package fr.rlebranchu.ytblight.data.auth

import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRepositoryImplTest {
    private class FakePlatformAuth(
        var token: String? = null,
        var tokenError: Exception? = null,
        var signOutError: Exception? = null,
    ) : PlatformAuth {
        var signOutCalls = 0

        override suspend fun currentToken(): String? = tokenError?.let { throw it } ?: token

        override suspend fun refreshToken(): String? = token

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
    fun becomesSignedInAfterANativeSignIn() {
        val repository =
            AuthRepositoryImpl(
                platformAuth = FakePlatformAuth(),
            )

        repository.onSignedIn()

        assertEquals(AuthState.SIGNED_IN, repository.state.value)
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
            repository.onSignedIn()

            repository.signOut()

            assertEquals(AuthState.SIGNED_OUT, repository.state.value)
        }
}
