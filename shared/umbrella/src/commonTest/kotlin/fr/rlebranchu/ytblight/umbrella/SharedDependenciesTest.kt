package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.presentation.feed.FeedUiState
import fr.rlebranchu.ytblight.presentation.splash.SplashDestination
import fr.rlebranchu.ytblight.testing.fakeDependencies
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
                fakeDependencies()
                    .feedViewModel()
                    .state.value

            assertEquals(listOf("fake-4", "fake-3", "fake-1"), state.videos.map { it.id })
            assertNull(state.error)
            assertEquals(false, state.isLoading)
        }

    @Test
    fun eachViewModelIsANewInstance() {
        val dependencies = fakeDependencies()

        assertEquals(false, dependencies.feedViewModel() === dependencies.feedViewModel())
    }

    @Test
    fun fakeSessionStartsOnTheFeedOrOnSignIn() =
        runTest {
            val signedIn =
                fakeDependencies(
                    signedIn = true,
                ).splashViewModel()
            val signedOut =
                fakeDependencies(
                    signedIn = false,
                ).splashViewModel()

            assertEquals(SplashDestination.FEED, signedIn.state.value.destination)
            assertEquals(SplashDestination.SIGN_IN, signedOut.state.value.destination)
        }

    @Test
    fun fakeSignOutFromTheFeedAsksToReturnToSignIn() =
        runTest {
            val feed =
                fakeDependencies(
                    signedIn = true,
                ).feedViewModel()

            feed.signOut()

            assertEquals(true, feed.state.value.isSignedOut)
        }

    @Test
    fun playerViewModelReceivesTheVideoId() {
        val player =
            fakeDependencies()
                .playerViewModel(
                    videoId = "abc",
                )

        assertEquals("abc", player.state.value.videoId)
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

            val splash =
                SharedDependencies
                    .create(
                        platformAuth = platform,
                    ).splashViewModel()

            assertEquals(SplashDestination.FEED, splash.state.value.destination)
        }
}
