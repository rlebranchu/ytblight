package fr.rlebranchu.ytblight.umbrella

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.data.di.dataModule
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.RefreshingTokenAuth
import fr.rlebranchu.ytblight.external.auth.TokenRequester
import fr.rlebranchu.ytblight.external.di.externalModule
import fr.rlebranchu.ytblight.local.token.TokenStorage
import fr.rlebranchu.ytblight.presentation.di.presentationModule
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.presentation.splash.SplashViewModel
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * Point d'entrée du code partagé pour l'application iOS (SwiftUI). Swift fournit la connexion Google
 * (AppAuth) et le stockage du jeton (Trousseau) ; le reste est le code partagé, via Koin.
 */
object Ytblight {
    /** Démarre l'application. À appeler une fois, au lancement. */
    fun start(
        tokenRequester: TokenRequester,
        tokenStorage: TokenStorage,
    ) {
        startWith(
            overrides = listOf(authModule(tokenRequester = tokenRequester, tokenStorage = tokenStorage)),
        )
    }

    /**
     * Démarre l'application avec sa propre API YouTube : réservé aux tests d'interface (XCUITest),
     * qui fournissent une API factice écrite en Swift. Toute la logique réelle (fil, filtre des
     * Shorts, session, jeton) reste celle de l'application.
     */
    fun startForUiTests(
        youTubeApi: YouTubeApi,
        tokenRequester: TokenRequester,
        tokenStorage: TokenStorage,
    ) {
        startWith(
            overrides =
                listOf(
                    authModule(tokenRequester = tokenRequester, tokenStorage = tokenStorage),
                    module {
                        single<YouTubeApi> { youTubeApi }
                    },
                ),
        )
    }

    fun splashViewModel(): SplashViewModel = KoinPlatform.getKoin().get()

    fun signInViewModel(): SignInViewModel = KoinPlatform.getKoin().get()

    fun feedViewModel(): FeedViewModel = KoinPlatform.getKoin().get()

    fun playerViewModel(
        videoId: String,
    ): PlayerViewModel =
        KoinPlatform.getKoin().get {
            parametersOf(videoId)
        }

    /** À appeler quand la page disparaît : arrête les traitements en cours du ViewModel. */
    fun release(
        viewModel: ViewModel,
    ) {
        viewModel.viewModelScope.cancel()
    }

    private fun authModule(
        tokenRequester: TokenRequester,
        tokenStorage: TokenStorage,
    ): Module =
        module {
            single<PlatformAuth> {
                RefreshingTokenAuth(
                    requester = tokenRequester,
                    storage = tokenStorage,
                )
            }
        }

    private fun startWith(
        overrides: List<Module>,
    ) {
        if (KoinPlatform.getKoinOrNull() != null) stopKoin()
        startKoin {
            modules(
                externalModule,
                dataModule,
                presentationModule,
            )
            modules(overrides)
        }
    }
}
