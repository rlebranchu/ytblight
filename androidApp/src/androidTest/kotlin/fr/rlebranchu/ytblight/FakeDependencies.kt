package fr.rlebranchu.ytblight

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Module Koin de test : remplace les repositories réels par des données factices, pour qu'aucun
 * appel ne parte vers Google et qu'aucun quota ne soit consommé.
 * @param auth session factice : `signedIn = false` démarre sur l'écran de connexion.
 */
fun fakeModule(
    auth: FakeAuthRepository,
): Module =
    module {
        single<FeedRepository> { FakeFeedRepository() }
        single<AuthRepository> { auth }
    }
