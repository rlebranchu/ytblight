package fr.rlebranchu.ytblight.external

import fr.rlebranchu.ytblight.external.dto.ApiErrorEnvelopeDto
import fr.rlebranchu.ytblight.external.dto.ChannelListResponseDto
import fr.rlebranchu.ytblight.external.dto.PlaylistItemListResponseDto
import fr.rlebranchu.ytblight.external.dto.SubscriptionListResponseDto
import fr.rlebranchu.ytblight.external.dto.VideoListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.io.IOException

class KtorYouTubeApi(
    private val client: HttpClient,
) : YouTubeApi {
    override suspend fun subscriptions(
        pageToken: String?,
    ): SubscriptionListResponseDto =
        get("subscriptions") {
            parameter("part", "snippet")
            parameter("mine", "true")
            parameter("maxResults", YouTubeApi.MAX_BATCH_SIZE)
            parameter("pageToken", pageToken)
        }

    override suspend fun channels(
        channelIds: List<String>,
    ): ChannelListResponseDto =
        get("channels") {
            parameter("part", "contentDetails")
            parameter("id", channelIds.joinToString(","))
            parameter("maxResults", YouTubeApi.MAX_BATCH_SIZE)
        }

    override suspend fun playlistItems(
        playlistId: String,
        maxResults: Int,
    ): PlaylistItemListResponseDto =
        get("playlistItems") {
            parameter("part", "contentDetails")
            parameter("playlistId", playlistId)
            parameter("maxResults", maxResults)
        }

    override suspend fun videos(
        videoIds: List<String>,
    ): VideoListResponseDto =
        get("videos") {
            parameter("part", "snippet,contentDetails")
            parameter("id", videoIds.joinToString(","))
            parameter("maxResults", YouTubeApi.MAX_BATCH_SIZE)
        }

    private suspend inline fun <reified T> get(
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ): T {
        val response =
            try {
                client.get(path, block)
            } catch (e: IOException) {
                throw YouTubeNetworkException(e)
            }
        if (!response.status.isSuccess()) throw response.toException()
        return response.body()
    }

    private suspend fun HttpResponse.toException(): YouTubeHttpException {
        val error =
            runCatching { YouTubeJson.decodeFromString<ApiErrorEnvelopeDto>(bodyAsText()).error }.getOrNull()
        return YouTubeHttpException(
            statusCode = status.value,
            reason = error?.errors?.firstOrNull()?.reason,
            message = error?.message,
        )
    }
}
