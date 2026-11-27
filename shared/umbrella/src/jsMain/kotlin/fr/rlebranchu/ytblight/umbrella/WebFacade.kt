package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.data.di.dataModule
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.RefreshingTokenAuth
import fr.rlebranchu.ytblight.external.di.externalModule
import fr.rlebranchu.ytblight.local.token.BrowserTokenStorage
import fr.rlebranchu.ytblight.presentation.di.presentationModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Point d'entrée du code partagé pour le web : démarre Koin avec la connexion Google fournie par
 * le pont TypeScript. À appeler une fois, au démarrage de la page.
 */
@JsExport
fun initYtblight(
    bridge: GoogleIdentityBridge,
) {
    // Rechargement à chaud (Vite) : on repart d'un conteneur neuf.
    if (GlobalContext.getOrNull() != null) stopKoin()
    startKoin {
        modules(
            module {
                single<PlatformAuth> {
                    RefreshingTokenAuth(
                        requester =
                            GoogleTokenRequester(
                                bridge = bridge,
                            ),
                        storage = BrowserTokenStorage(),
                    )
                }
            },
            externalModule,
            dataModule,
            presentationModule,
        )
    }
}

@JsExport
fun createSplashViewModel(): SplashFacade =
    SplashFacade(
        viewModel = GlobalContext.get().get(),
    )

@JsExport
fun createSignInViewModel(): SignInFacade =
    SignInFacade(
        viewModel = GlobalContext.get().get(),
    )

@JsExport
fun createFeedViewModel(): FeedFacade =
    FeedFacade(
        viewModel = GlobalContext.get().get(),
    )

@JsExport
fun createPlayerViewModel(
    videoId: String,
): PlayerFacade =
    PlayerFacade(
        viewModel =
            GlobalContext.get().get {
                parametersOf(videoId)
            },
    )
