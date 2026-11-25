package fr.rlebranchu.ytblight.presentation.session

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.SignOutUseCase
import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
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
class SessionViewModelTest {
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

        override suspend fun signOut() {
            mutableState.value = AuthState.SIGNED_OUT
        }
    }

    private class FakeFeedRepository : FeedRepository {
        var cleared = false

        override suspend fun loadFeed(
            forceRefresh: Boolean,
        ): DomainResult<List<Video>> =
            DomainResult.Success(
                value = emptyList(),
            )

        override suspend fun clear() {
            cleared = true
        }
    }

    private val feed = FakeFeedRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        sessionIsValid: Boolean,
    ): SessionViewModel {
        val auth =
            FakeAuthRepository(
                sessionIsValid = sessionIsValid,
            )
        return SessionViewModel(
            authRepository = auth,
            signOutUseCase =
                SignOutUseCase(
                    authRepository = auth,
                    feedRepository = feed,
                ),
        )
    }

    @Test
    fun isSignedInWhenASessionAlreadyExists() =
        runTest {
            assertEquals(
                AuthState.SIGNED_IN,
                viewModel(
                    sessionIsValid = true,
                ).state.value,
            )
        }

    @Test
    fun isSignedOutWhenThereIsNoSession() =
        runTest {
            assertEquals(
                AuthState.SIGNED_OUT,
                viewModel(
                    sessionIsValid = false,
                ).state.value,
            )
        }

    @Test
    fun becomesSignedInAfterTheNativeSignIn() =
        runTest {
            val viewModel =
                viewModel(
                    sessionIsValid = false,
                )

            viewModel.onSignedIn()

            assertEquals(AuthState.SIGNED_IN, viewModel.state.value)
        }

    @Test
    fun signOutEndsTheSessionAndClearsTheFeed() =
        runTest {
            val viewModel =
                viewModel(
                    sessionIsValid = true,
                )

            viewModel.signOut()

            assertEquals(AuthState.SIGNED_OUT, viewModel.state.value)
            assertEquals(true, feed.cleared)
        }
}
