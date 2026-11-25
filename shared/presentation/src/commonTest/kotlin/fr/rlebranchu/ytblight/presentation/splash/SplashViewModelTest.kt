package fr.rlebranchu.ytblight.presentation.splash

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private class FakeAuthRepository(
        private val sessionIsValid: Boolean,
    ) : AuthRepository {
        private val mutableState = MutableStateFlow(AuthState.CHECKING)
        override val state: StateFlow<AuthState> = mutableState

        override suspend fun checkSession() {
            mutableState.value = if (sessionIsValid) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
        }

        override fun onSignedIn() = Unit

        override suspend fun signOut() = Unit
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun goesToTheFeedWhenASessionAlreadyExists() =
        runTest {
            val viewModel =
                SplashViewModel(
                    authRepository = FakeAuthRepository(sessionIsValid = true),
                )

            assertEquals(SplashDestination.FEED, viewModel.state.value.destination)
        }

    @Test
    fun goesToSignInWhenThereIsNoSession() =
        runTest {
            val viewModel =
                SplashViewModel(
                    authRepository = FakeAuthRepository(sessionIsValid = false),
                )

            assertEquals(SplashDestination.SIGN_IN, viewModel.state.value.destination)
        }
}
