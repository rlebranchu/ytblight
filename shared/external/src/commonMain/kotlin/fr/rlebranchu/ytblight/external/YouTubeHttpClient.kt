package fr.rlebranchu.ytblight.external

import fr.rlebranchu.ytblight.external.auth.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val YouTubeJson = Json { ignoreUnknownKeys = true }

const val YOUTUBE_API_BASE_URL = "https://www.googleapis.com/youtube/v3/"

/** Configuration partagée : JSON, URL de base et jeton Bearer rafraîchi automatiquement sur un 401. */
fun HttpClientConfig<*>.configureYouTube(
    tokens: AccessTokenProvider,
) {
    install(ContentNegotiation) { json(YouTubeJson) }
    install(Auth) {
        bearer {
            loadTokens { tokens.currentToken()?.let { BearerTokens(it, null) } }
            refreshTokens { tokens.refreshToken()?.let { BearerTokens(it, null) } }
        }
    }
    defaultRequest { url(YOUTUBE_API_BASE_URL) }
}

/** Client de production : le moteur (OkHttp, Darwin ou Js) est choisi par la plateforme. */
fun createYouTubeHttpClient(
    tokens: AccessTokenProvider,
): HttpClient = HttpClient { configureYouTube(tokens) }

/** L'API prête à l'emploi : les autres modules n'ont pas besoin de connaître Ktor. */
fun createYouTubeApi(
    tokens: AccessTokenProvider,
): YouTubeApi = KtorYouTubeApi(createYouTubeHttpClient(tokens))
