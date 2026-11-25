package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.data.auth.AuthRepositoryImpl
import fr.rlebranchu.ytblight.data.feed.FeedRepositoryImpl
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.CheckSessionUseCase
import fr.rlebranchu.ytblight.domain.auth.CompleteSignInUseCase
import fr.rlebranchu.ytblight.domain.auth.SignOutUseCase
import fr.rlebranchu.ytblight.domain.feed.FeedPolicy
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.createYouTubeApi
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.presentation.splash.SplashViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Point d'entrée unique du code partagé pour les trois interfaces.
 *
 * Koin reste interne : on n'expose que des méthodes simples, utilisables telles quelles
 * depuis Swift et TypeScript. Un conteneur isolé (et non global) évite tout état partagé
 * entre les tests.
 */
class SharedDependencies private constructor(
    internal val koinApplication: KoinApplication,
) {
    private val koin get() = koinApplication.koin

    fun splashViewModel(): SplashViewModel = koin.get()

    fun signInViewModel(): SignInViewModel = koin.get()

    fun feedViewModel(): FeedViewModel = koin.get()

    fun playerViewModel(
        videoId: String,
    ): PlayerViewModel =
        koin.get {
            parametersOf(videoId)
        }

    companion object {
        /** Vraie API YouTube ; la connexion Google est fournie par la plateforme. */
        fun create(
            platformAuth: PlatformAuth,
        ): SharedDependencies =
            build(
                module {
                    single<YouTubeApi> {
                        createYouTubeApi(
                            tokens = platformAuth,
                        )
                    }
                    single<FeedRepository> {
                        FeedRepositoryImpl(
                            api = get(),
                        )
                    }
                    single<AuthRepository> {
                        AuthRepositoryImpl(
                            platformAuth = platformAuth,
                        )
                    }
                },
            )

        /**
         * Données factices : tests d'interface et développement sans connexion Google.
         * @param signedIn `false` pour démarrer sur l'écran de connexion.
         */
        fun fake(
            signedIn: Boolean = true,
        ): SharedDependencies =
            build(
                module {
                    single<FeedRepository> { FakeFeedRepository() }
                    single<AuthRepository> {
                        FakeAuthRepository(
                            signedIn = signedIn,
                        )
                    }
                },
            )

        private fun build(
            dataModule: Module,
        ): SharedDependencies {
            val domainModule =
                module {
                    factory { FeedPolicy() }
                    factory {
                        GetFeedUseCase(
                            repository = get(),
                            policy = get(),
                        )
                    }
                    factory {
                        CheckSessionUseCase(
                            authRepository = get(),
                        )
                    }
                    factory {
                        CompleteSignInUseCase(
                            authRepository = get(),
                        )
                    }
                    factory {
                        SignOutUseCase(
                            authRepository = get(),
                            feedRepository = get(),
                        )
                    }
                }
            val presentationModule =
                module {
                    viewModel {
                        SplashViewModel(
                            checkSession = get(),
                        )
                    }
                    viewModel {
                        SignInViewModel(
                            completeSignIn = get(),
                        )
                    }
                    viewModel {
                        FeedViewModel(
                            getFeed = get(),
                            signOutUseCase = get(),
                        )
                    }
                    viewModel { parameters ->
                        PlayerViewModel(
                            videoId = parameters.get(),
                        )
                    }
                }
            return SharedDependencies(
                koinApplication =
                    koinApplication {
                        modules(dataModule, domainModule, presentationModule)
                    },
            )
        }
    }
}
