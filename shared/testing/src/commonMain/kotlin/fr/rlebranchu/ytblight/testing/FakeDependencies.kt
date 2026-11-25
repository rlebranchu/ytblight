package fr.rlebranchu.ytblight.testing

import fr.rlebranchu.ytblight.umbrella.SharedDependencies

/**
 * Application entière sur données factices : aucun appel à Google, aucun quota consommé.
 * @param signedIn `false` pour démarrer sur l'écran de connexion.
 */
fun fakeDependencies(
    signedIn: Boolean = true,
): SharedDependencies =
    SharedDependencies.createWith(
        feedRepository = FakeFeedRepository(),
        authRepository =
            FakeAuthRepository(
                signedIn = signedIn,
            ),
    )
