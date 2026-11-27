package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.data.di.dataModule
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.PlatformSignInOutcome
import fr.rlebranchu.ytblight.external.di.externalModule
import fr.rlebranchu.ytblight.presentation.di.presentationModule
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.presentation.splash.SplashDestination
import fr.rlebranchu.ytblight.presentation.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Vérifie que les modules Koin des couches s'assemblent et que les ViewModels se résolvent. */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinModulesTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Données factices à la place des repositories réels. */
    private fun fakeApp(
        signedIn: Boolean = true,
    ): KoinApplication =
        koinApplication {
            modules(
                presentationModule,
                dataModule,
                module {
                    single<FeedRepository> { FakeFeedRepository() }
                    single<AuthRepository> {
                        FakeAuthRepository(
                            signedIn = signedIn,
                        )
                    }
                },
            )
        }

    @Test
    fun feedIsWiredEndToEndAndHidesShortsInNewestFirstOrder() =
        runTest {
            val state =
                fakeApp()
                    .koin
                    .get<FeedViewModel>()
                    .state.value

            assertEquals(listOf("new", "old"), state.videos.map { it.id })
            assertNull(state.error)
            assertEquals(false, state.isLoading)
        }

    @Test
    fun eachViewModelIsANewInstance() {
        val koin = fakeApp().koin

        assertEquals(false, koin.get<FeedViewModel>() === koin.get<FeedViewModel>())
    }

    @Test
    fun sessionStartsOnTheFeedOrOnSignIn() =
        runTest {
            val signedIn =
                fakeApp(
                    signedIn = true,
                ).koin.get<SplashViewModel>()
            val signedOut =
                fakeApp(
                    signedIn = false,
                ).koin.get<SplashViewModel>()

            assertEquals(SplashDestination.FEED, signedIn.state.value.destination)
            assertEquals(SplashDestination.SIGN_IN, signedOut.state.value.destination)
        }

    @Test
    fun signOutFromTheFeedAsksToReturnToSignIn() =
        runTest {
            val feed = fakeApp().koin.get<FeedViewModel>()

            feed.signOut()

            assertEquals(true, feed.state.value.isSignedOut)
        }

    @Test
    fun signInAndPlayerViewModelsResolve() {
        val koin = fakeApp().koin

        koin.get<SignInViewModel>()
        val player =
            koin.get<PlayerViewModel> {
                parametersOf("abc")
            }

        assertEquals("abc", player.state.value.videoId)
    }

    @Test
    fun realWiringReadsTheSessionFromThePlatform() =
        runTest {
            val platform =
                object : PlatformAuth {
                    override suspend fun currentToken(): String? = "jeton"

                    override suspend fun refreshToken(): String? = "jeton"

                    override suspend fun signIn(): PlatformSignInOutcome = PlatformSignInOutcome.Authorized

                    override suspend fun completeConsent(
                        response: Any?,
                        isGranted: Boolean,
                    ): PlatformSignInOutcome = PlatformSignInOutcome.Authorized

                    override suspend fun signOut() = Unit
                }
            val koin =
                koinApplication {
                    modules(
                        presentationModule,
                        dataModule,
                        externalModule,
                        module {
                            single<PlatformAuth> { platform }
                        },
                    )
                }.koin

            assertEquals(
                SplashDestination.FEED,
                koin
                    .get<SplashViewModel>()
                    .state.value.destination,
            )
        }
}
