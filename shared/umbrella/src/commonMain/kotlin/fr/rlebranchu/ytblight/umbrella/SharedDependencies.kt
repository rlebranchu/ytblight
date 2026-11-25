package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.feed.FeedPolicy
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import org.koin.core.Koin
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
    fun feedViewModel(): FeedViewModel = koin.get()

    companion object {
        /** Données factices : tests d'interface et développement sans connexion Google. */
        fun fake(): SharedDependencies = create(module { single<FeedRepository> { FakeFeedRepository() } })

        private fun create(dataModule: org.koin.core.module.Module): SharedDependencies {
            val domainModule =
                module {
                    factory { FeedPolicy() }
                    factory { GetFeedUseCase(get(), get()) }
                    factory { FeedViewModel(get()) }
                }
            return SharedDependencies(koinApplication { modules(dataModule, domainModule) }.koin)
        }
    }
}
