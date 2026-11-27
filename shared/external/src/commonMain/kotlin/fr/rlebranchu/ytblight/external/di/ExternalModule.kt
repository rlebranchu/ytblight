package fr.rlebranchu.ytblight.external.di

import fr.rlebranchu.ytblight.external.KtorYouTubeApi
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.configureYouTube
import io.ktor.client.HttpClient
import org.koin.dsl.module

/** Koin : l'API YouTube. La connexion Google ([PlatformAuth]) est fournie par le module de la plateforme. */
val externalModule =
    module {
        single<YouTubeApi> {
            KtorYouTubeApi(
                client =
                    HttpClient {
                        configureYouTube(
                            tokens = get<PlatformAuth>(),
                        )
                    },
            )
        }
    }
