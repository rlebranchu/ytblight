package fr.rlebranchu.ytblight.external.di

import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.createYouTubeApi
import org.koin.dsl.module

/** Koin : l'API YouTube. La connexion Google ([PlatformAuth]) est fournie par le module de la plateforme. */
val externalModule =
    module {
        single<YouTubeApi> {
            createYouTubeApi(
                tokens = get<PlatformAuth>(),
            )
        }
    }
