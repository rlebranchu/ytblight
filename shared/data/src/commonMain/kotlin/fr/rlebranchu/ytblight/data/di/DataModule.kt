package fr.rlebranchu.ytblight.data.di

import fr.rlebranchu.ytblight.data.auth.AuthRepositoryImpl
import fr.rlebranchu.ytblight.data.feed.FeedRepositoryImpl
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.CheckSessionUseCase
import fr.rlebranchu.ytblight.domain.auth.CompleteConsentUseCase
import fr.rlebranchu.ytblight.domain.auth.SignInUseCase
import fr.rlebranchu.ytblight.domain.auth.SignOutUseCase
import fr.rlebranchu.ytblight.domain.feed.FeedPolicy
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import org.koin.dsl.module

/** Koin : les repositories et les use cases du domaine (qui lui-même ne dépend d'aucun module). */
val dataModule =
    module {
        // Repositories
        single<FeedRepository> {
            FeedRepositoryImpl(
                api = get(),
            )
        }
        single<AuthRepository> {
            AuthRepositoryImpl(
                platformAuth = get<PlatformAuth>(),
            )
        }

        // Use cases
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
            SignInUseCase(
                authRepository = get(),
            )
        }
        factory {
            CompleteConsentUseCase(
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
