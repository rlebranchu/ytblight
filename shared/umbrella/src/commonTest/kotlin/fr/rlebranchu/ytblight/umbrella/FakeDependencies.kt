package fr.rlebranchu.ytblight.umbrella

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
