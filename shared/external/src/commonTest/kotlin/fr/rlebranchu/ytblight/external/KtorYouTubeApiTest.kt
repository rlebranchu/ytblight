package fr.rlebranchu.ytblight.external

import fr.rlebranchu.ytblight.external.auth.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class KtorYouTubeApiTest {
    private class FakeTokens(
        private var current: String? = "jeton-1",
        private val refreshed: String? = "jeton-2",
    ) : AccessTokenProvider {
        var refreshCount = 0

        override suspend fun currentToken(): String? = current

        override suspend fun refreshToken(): String? {
            refreshCount++
            current = refreshed
            return refreshed
        }
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun apiWith(
        tokens: AccessTokenProvider = FakeTokens(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorYouTubeApi {
        val engine =
            MockEngine { request ->
                requests += request
                handler(request)
            }
        return KtorYouTubeApi(
            client = HttpClient(engine) { configureYouTube(tokens = tokens) },
        )
    }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        vararg extraHeaders: Pair<String, String>,
    ): HttpResponseData {
        val headers =
            headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                *extraHeaders.map { (name, value) -> name to listOf(value) }.toTypedArray(),
            )
        return respond(content = body, status = status, headers = headers)
    }

    @Test
    fun readsSubscriptionsAndSendsTheBearerToken() =
        runTest {
            val api =
                apiWith {
                    json(
                        body =
                            """
                            {"items":[{"snippet":{"resourceId":{"kind":"youtube#channel","channelId":"UC1"}}}],
                             "nextPageToken":"PAGE2","etag":"ignoré"}
                            """.trimIndent(),
                    )
                }

            val page =
                api.subscriptions(
                    pageToken = null,
                )

            assertEquals(listOf("UC1"), page.items.map { it.snippet.resourceId.channelId })
            assertEquals("PAGE2", page.nextPageToken)
            val request = requests.single()
            assertEquals("/youtube/v3/subscriptions", request.url.encodedPath)
            assertEquals("true", request.url.parameters["mine"])
            assertEquals("50", request.url.parameters["maxResults"])
            assertNull(request.url.parameters["pageToken"])
            assertEquals("Bearer jeton-1", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun sendsThePageTokenWhenThereIsOne() =
        runTest {
            val api =
                apiWith {
                    json(
                        body = """{"items":[]}""",
                    )
                }

            api.subscriptions(
                pageToken = "PAGE2",
            )

            assertEquals("PAGE2", requests.single().url.parameters["pageToken"])
        }

    @Test
    fun readsChannelUploadsPlaylists() =
        runTest {
            val api =
                apiWith {
                    json(
                        body =
                            """{"items":[{"id":"UC1","contentDetails":{"relatedPlaylists":{"uploads":"UU1","likes":""}}}]}""",
                    )
                }

            val response =
                api.channels(
                    channelIds = listOf("UC1", "UC2"),
                )

            assertEquals(
                "UU1",
                response.items
                    .single()
                    .contentDetails.relatedPlaylists.uploads,
            )
            assertEquals("UC1,UC2", requests.single().url.parameters["id"])
        }

    @Test
    fun readsPlaylistItems() =
        runTest {
            val api =
                apiWith {
                    json(
                        body =
                            """{"items":[{"contentDetails":{"videoId":"V1"}},{"contentDetails":{"videoId":"V2"}}]}""",
                    )
                }

            val response =
                api.playlistItems(
                    playlistId = "UU1",
                    maxResults = 5,
                )

            assertEquals(listOf("V1", "V2"), response.items.map { it.contentDetails.videoId })
            assertEquals("UU1", requests.single().url.parameters["playlistId"])
            assertEquals("5", requests.single().url.parameters["maxResults"])
        }

    @Test
    fun readsVideoDetails() =
        runTest {
            val api =
                apiWith {
                    json(
                        body =
                            """
                            {"items":[{"id":"V1",
                              "snippet":{"title":"Titre","channelId":"UC1","channelTitle":"Chaîne",
                                         "publishedAt":"2026-11-20T18:00:07Z",
                                         "thumbnails":{"medium":{"url":"https://i.ytimg.com/vi/V1/mqdefault.jpg","width":320}}},
                              "contentDetails":{"duration":"PT4M13S","dimension":"2d"}}]}
                            """.trimIndent(),
                    )
                }

            val video =
                api
                    .videos(
                        videoIds = listOf("V1"),
                    ).items
                    .single()

            assertEquals("Titre", video.snippet.title)
            assertEquals("PT4M13S", video.contentDetails.duration)
            assertEquals(
                "https://i.ytimg.com/vi/V1/mqdefault.jpg",
                video.snippet.thumbnails.medium
                    ?.url,
            )
            assertEquals("snippet,contentDetails", requests.single().url.parameters["part"])
        }

    @Test
    fun reportsQuotaErrorsWithTheirReason() =
        runTest {
            val api =
                apiWith {
                    json(
                        body =
                            """{"error":{"code":403,"message":"Quota dépassé","errors":[{"reason":"quotaExceeded"}]}}""",
                        status = HttpStatusCode.Forbidden,
                    )
                }

            val error =
                assertFailsWith<YouTubeHttpException> {
                    api.subscriptions(
                        pageToken = null,
                    )
                }

            assertEquals(403, error.statusCode)
            assertEquals("quotaExceeded", error.reason)
        }

    @Test
    fun reportsErrorsWithAnUnreadableBody() =
        runTest {
            val api = apiWith { respond(content = "<html>Bad gateway</html>", status = HttpStatusCode.BadGateway) }

            val error =
                assertFailsWith<YouTubeHttpException> {
                    api.subscriptions(
                        pageToken = null,
                    )
                }

            assertEquals(502, error.statusCode)
            assertNull(error.reason)
        }

    @Test
    fun refreshesTheTokenOnceAfterA401AndRetries() =
        runTest {
            val tokens = FakeTokens()
            val api =
                apiWith(
                    tokens = tokens,
                ) { request ->
                    if (request.headers[HttpHeaders.Authorization] == "Bearer jeton-2") {
                        json(
                            body = """{"items":[]}""",
                        )
                    } else {
                        json(
                            body = """{"error":{"code":401}}""",
                            status = HttpStatusCode.Unauthorized,
                            HttpHeaders.WWWAuthenticate to """Bearer realm="https://accounts.google.com/"""",
                        )
                    }
                }

            api.subscriptions(
                pageToken = null,
            )

            assertEquals(1, tokens.refreshCount)
            assertEquals(
                listOf("Bearer jeton-1", "Bearer jeton-2"),
                requests.map { it.headers[HttpHeaders.Authorization] },
            )
        }

    @Test
    fun wrapsNetworkFailures() =
        runTest {
            val api = apiWith { throw IOException("hors ligne") }

            assertFailsWith<YouTubeNetworkException> {
                api.subscriptions(
                    pageToken = null,
                )
            }
        }
}
