package fr.rlebranchu.ytblight.presentation.splash

import fr.rlebranchu.ytblight.domain.auth.CheckSessionUseCase
import fr.rlebranchu.ytblight.fixtures.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                    checkSession =
                        CheckSessionUseCase(
                            authRepository =
                                FakeAuthRepository(
                                    signedIn = true,
                                ),
                        ),
                )

            assertEquals(SplashDestination.FEED, viewModel.state.value.destination)
        }

    @Test
    fun goesToSignInWhenThereIsNoSession() =
        runTest {
            val viewModel =
                SplashViewModel(
                    checkSession =
                        CheckSessionUseCase(
                            authRepository =
                                FakeAuthRepository(
                                    signedIn = false,
                                ),
                        ),
                )

            assertEquals(SplashDestination.SIGN_IN, viewModel.state.value.destination)
        }
}
