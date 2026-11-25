package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.data.auth.AuthRepositoryImpl
import fr.rlebranchu.ytblight.data.feed.FeedRepositoryImpl
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.SignOutUseCase
import fr.rlebranchu.ytblight.domain.feed.FeedPolicy
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.createYouTubeApi
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.session.SessionViewModel
import org.koin.core.Koin
import org.koin.core.module.Module
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
    private val koin: Koin,
) {
    fun sessionViewModel(): SessionViewModel = koin.get()

    fun feedViewModel(): FeedViewModel = koin.get()

    companion object {
        /** Vraie API YouTube ; la connexion Google est fournie par la plateforme. */
        fun create(
            platformAuth: PlatformAuth,
        ): SharedDependencies =
            build(
                module {
                    single<YouTubeApi> { createYouTubeApi(platformAuth) }
                    single<FeedRepository> { FeedRepositoryImpl(get()) }
                    single<AuthRepository> { AuthRepositoryImpl(platformAuth) }
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
                    single<AuthRepository> { FakeAuthRepository(signedIn) }
                },
            )

        private fun build(
            dataModule: Module,
        ): SharedDependencies {
            val domainModule =
                module {
                    factory { FeedPolicy() }
                    factory { GetFeedUseCase(get(), get()) }
                    factory { SignOutUseCase(get(), get()) }
                    factory { FeedViewModel(get()) }
                    factory { SessionViewModel(get(), get()) }
                }
            return SharedDependencies(koinApplication { modules(dataModule, domainModule) }.koin)
        }
    }
}
