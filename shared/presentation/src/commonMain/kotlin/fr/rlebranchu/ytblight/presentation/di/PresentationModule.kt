package fr.rlebranchu.ytblight.presentation.di

import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule =
    module {
        viewModel {
            SplashViewModel(
                checkSession = get(),
            )
        }
        viewModel {
            SignInViewModel(
                signIn = get(),
                completeConsent = get(),
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
