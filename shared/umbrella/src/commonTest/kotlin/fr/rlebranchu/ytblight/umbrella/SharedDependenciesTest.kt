package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.presentation.feed.FeedUiState
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SharedDependenciesTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fakeFeedIsWiredEndToEndAndHidesShortsInNewestFirstOrder() =
        runTest {
            val state: FeedUiState =
                SharedDependencies
                    .fake()
                    .feedViewModel()
                    .state.value

            assertEquals(listOf("fake-4", "fake-3", "fake-1"), state.videos.map { it.id })
            assertNull(state.error)
            assertEquals(false, state.isLoading)
        }

    @Test
    fun eachViewModelIsANewInstance() {
        val dependencies = SharedDependencies.fake()

        assertEquals(false, dependencies.feedViewModel() === dependencies.feedViewModel())
    }

    @Test
    fun fakeSessionStartsSignedInOrSignedOut() =
        runTest {
            assertEquals(
                AuthState.SIGNED_IN,
                SharedDependencies
                    .fake(
                        signedIn = true,
                    ).sessionViewModel()
                    .state.value,
            )
            assertEquals(
                AuthState.SIGNED_OUT,
                SharedDependencies
                    .fake(
                        signedIn = false,
                    ).sessionViewModel()
                    .state.value,
            )
        }

    @Test
    fun fakeSignOutReturnsToTheSignInState() =
        runTest {
            val session =
                SharedDependencies
                    .fake(
                        signedIn = true,
                    ).sessionViewModel()

            session.signOut()

            assertEquals(AuthState.SIGNED_OUT, session.state.value)
        }

    @Test
    fun realWiringReadsTheSessionFromThePlatform() =
        runTest {
            val platform =
                object : PlatformAuth {
                    override suspend fun currentToken(): String? = "jeton"

                    override suspend fun refreshToken(): String? = "jeton"

                    override suspend fun signOut() = Unit
                }

            val session =
                SharedDependencies
                    .create(
                        platformAuth = platform,
                    ).sessionViewModel()

            assertEquals(AuthState.SIGNED_IN, session.state.value)
        }
}
