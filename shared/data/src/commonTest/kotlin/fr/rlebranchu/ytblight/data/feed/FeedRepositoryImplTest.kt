package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.data.feed.FakeYouTubeApi.Companion.videoDto
import fr.rlebranchu.ytblight.domain.error.DomainError
import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.model.Video
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.YouTubeHttpException
import fr.rlebranchu.ytblight.external.YouTubeNetworkException
import fr.rlebranchu.ytblight.external.dto.PlaylistItemListResponseDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FeedRepositoryImplTest {
    private fun twoChannelsApi() =
        FakeYouTubeApi(
            subscriptionPages = listOf(listOf("UC1"), listOf("UC2")),
            uploadsByChannel = mapOf("UC1" to "UU1", "UC2" to "UU2"),
            videoIdsByPlaylist = mapOf("UU1" to listOf("V1", "V2"), "UU2" to listOf("V3")),
            videos =
                mapOf(
                    "V1" to
                        videoDto(
                            id = "V1",
                            channelId = "UC1",
                            duration = "PT4M13S",
                        ),
                    "V2" to
                        videoDto(
                            id = "V2",
                            channelId = "UC1",
                            duration = "PT1H2M3S",
                        ),
                    "V3" to
                        videoDto(
                            id = "V3",
                            channelId = "UC2",
                            duration = "PT45S",
                        ),
                ),
        )

    private fun repositoryOf(
        api: YouTubeApi,
        videosPerChannel: Int = FeedRepositoryImpl.DEFAULT_VIDEOS_PER_CHANNEL,
        maxParallelRequests: Int = FeedRepositoryImpl.DEFAULT_MAX_PARALLEL_REQUESTS,
    ) = FeedRepositoryImpl(
        api = api,
        videosPerChannel = videosPerChannel,
        maxParallelRequests = maxParallelRequests,
    )

    private fun DomainResult<List<Video>>.videos(): List<Video> = (this as DomainResult.Success).value

    @Test
    fun buildsTheFeedFromAllSubscriptionPages() =
        runTest {
            val api = twoChannelsApi()

            val videos =
                repositoryOf(api = api)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(setOf("V1", "V2", "V3"), videos.map { it.id }.toSet())
            assertEquals(2, api.subscriptionCalls)
            assertEquals(1, api.channelCalls)
            assertEquals(2, api.playlistItemCalls)
            assertEquals(1, api.videoCalls)
        }

    @Test
    fun mapsDurationsDatesAndThumbnails() =
        runTest {
            val videos =
                repositoryOf(api = twoChannelsApi())
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()
                    .associateBy { it.id }

            assertEquals(4.minutes + 13.seconds, videos.getValue("V1").duration)
            assertEquals(45.seconds, videos.getValue("V3").duration)
            assertEquals("Chaîne UC1", videos.getValue("V1").channelTitle)
            assertEquals("https://img/V1.jpg", videos.getValue("V1").thumbnailUrl)
            assertEquals(
                2026,
                videos
                    .getValue("V1")
                    .publishedAt
                    .toString()
                    .take(4)
                    .toInt(),
            )
        }

    @Test
    fun servesTheCacheUntilARefreshIsForced() =
        runTest {
            val api = twoChannelsApi()
            val repository = repositoryOf(api = api)

            repository.loadFeed(
                forceRefresh = false,
            )
            repository.loadFeed(
                forceRefresh = false,
            )
            assertEquals(1, api.videoCalls)

            repository.loadFeed(
                forceRefresh = true,
            )
            assertEquals(2, api.videoCalls)
            assertEquals(4, api.playlistItemCalls)
        }

    @Test
    fun forgetsTheCachedFeedOnClear() =
        runTest {
            val api = twoChannelsApi()
            val repository = repositoryOf(api = api)
            repository.loadFeed(
                forceRefresh = false,
            )

            repository.clear()
            repository.loadFeed(
                forceRefresh = false,
            )

            assertEquals(2, api.videoCalls)
        }

    @Test
    fun asksForTheConfiguredNumberOfVideosPerChannel() =
        runTest {
            val api = twoChannelsApi()

            val videos =
                repositoryOf(api = api, videosPerChannel = 1)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(setOf("V1", "V3"), videos.map { it.id }.toSet())
        }

    @Test
    fun batchesVideoDetailsBy50() =
        runTest {
            val ids = (1..120).map { "V$it" }
            val api =
                FakeYouTubeApi(
                    subscriptionPages = listOf(listOf("UC1")),
                    uploadsByChannel = mapOf("UC1" to "UU1"),
                    videoIdsByPlaylist = mapOf("UU1" to ids),
                    videos =
                        ids.associateWith {
                            videoDto(
                                id = it,
                            )
                        },
                )

            val videos =
                repositoryOf(api = api, videosPerChannel = 120)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(120, videos.size)
            assertEquals(listOf(50, 50, 20), api.requestedVideoBatches.map { it.size })
        }

    @Test
    fun dropsDeletedVideosAndUnreadableOnes() =
        runTest {
            val api =
                FakeYouTubeApi(
                    subscriptionPages = listOf(listOf("UC1")),
                    uploadsByChannel = mapOf("UC1" to "UU1"),
                    videoIdsByPlaylist = mapOf("UU1" to listOf("V1", "DELETED", "BAD-DURATION", "BAD-DATE")),
                    videos =
                        mapOf(
                            "V1" to
                                videoDto(
                                    id = "V1",
                                ),
                            "BAD-DURATION" to
                                videoDto(
                                    id = "BAD-DURATION",
                                    duration = "nimporte quoi",
                                ),
                            "BAD-DATE" to
                                videoDto(
                                    id = "BAD-DATE",
                                    publishedAt = "hier",
                                ),
                        ),
                )

            val videos =
                repositoryOf(api = api)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(listOf("V1"), videos.map { it.id })
        }

    @Test
    fun ignoresChannelsWithoutAnUploadsPlaylist() =
        runTest {
            val api =
                FakeYouTubeApi(
                    subscriptionPages = listOf(listOf("UC1", "UC-NO-UPLOADS")),
                    uploadsByChannel = mapOf("UC1" to "UU1"),
                    videoIdsByPlaylist = mapOf("UU1" to listOf("V1")),
                    videos =
                        mapOf(
                            "V1" to
                                videoDto(
                                    id = "V1",
                                ),
                        ),
                )

            val videos =
                repositoryOf(api = api)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(listOf("V1"), videos.map { it.id })
            assertEquals(1, api.playlistItemCalls)
        }

    @Test
    fun skipsAPlaylistThatIsNotFound() =
        runTest {
            val api =
                object : YouTubeApi by twoChannelsApi() {
                    override suspend fun playlistItems(
                        playlistId: String,
                        maxResults: Int,
                    ): PlaylistItemListResponseDto =
                        if (playlistId == "UU1") {
                            throw YouTubeHttpException(
                                statusCode = 404,
                                reason = "playlistNotFound",
                                message = null,
                            )
                        } else {
                            twoChannelsApi().playlistItems(
                                playlistId = playlistId,
                                maxResults = maxResults,
                            )
                        }
                }

            val videos =
                repositoryOf(api = api)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(listOf("V3"), videos.map { it.id })
        }

    @Test
    fun mapsQuotaErrors() =
        runTest {
            val result =
                failingWith(
                    YouTubeHttpException(
                        statusCode = 403,
                        reason = "quotaExceeded",
                        message = "Quota",
                    ),
                )

            assertEquals(
                DomainResult.Failure(
                    error = DomainError.QuotaExceeded,
                ),
                result,
            )
        }

    @Test
    fun mapsUnauthorizedToNotSignedIn() =
        runTest {
            val result =
                failingWith(
                    YouTubeHttpException(
                        statusCode = 401,
                        reason = null,
                        message = null,
                    ),
                )

            assertEquals(
                DomainResult.Failure(
                    error = DomainError.NotSignedIn,
                ),
                result,
            )
        }

    @Test
    fun mapsNetworkFailures() =
        runTest {
            val result =
                failingWith(
                    YouTubeNetworkException(
                        cause = RuntimeException("hors ligne"),
                    ),
                )

            assertEquals(
                DomainResult.Failure(
                    error = DomainError.Network,
                ),
                result,
            )
        }

    @Test
    fun keepsOtherForbiddenErrorsAsUnknown() =
        runTest {
            val result =
                failingWith(
                    YouTubeHttpException(
                        statusCode = 403,
                        reason = "forbidden",
                        message = "Interdit",
                    ),
                )

            val error = assertIs<DomainResult.Failure>(result).error
            assertIs<DomainError.Unknown>(error)
        }

    @Test
    fun doesNotCacheFailuresAndRecoversOnTheNextCall() =
        runTest {
            var failing = true
            val healthy = twoChannelsApi()
            val api =
                object : YouTubeApi by healthy {
                    override suspend fun subscriptions(
                        pageToken: String?,
                    ) = if (failing) {
                        throw YouTubeNetworkException(
                            cause = RuntimeException("x"),
                        )
                    } else {
                        healthy.subscriptions(
                            pageToken = pageToken,
                        )
                    }
                }
            val repository = repositoryOf(api = api)

            assertIs<DomainResult.Failure>(
                repository.loadFeed(
                    forceRefresh = false,
                ),
            )
            failing = false

            assertEquals(
                3,
                repository
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()
                    .size,
            )
        }

    @Test
    fun doesNotExceedTheParallelismLimit() =
        runTest {
            var running = 0
            var peak = 0
            val channels = (1..20).map { "UC$it" }
            val api =
                FakeYouTubeApi(
                    subscriptionPages = listOf(channels),
                    uploadsByChannel = channels.associateWith { "UU$it" },
                    videoIdsByPlaylist = channels.associate { "UU$it" to listOf("V$it") },
                    videos =
                        channels.associate {
                            "V$it" to
                                videoDto(
                                    id = "V$it",
                                )
                        },
                    onPlaylistItems = {
                        running++
                        peak = maxOf(peak, running)
                        delay(10)
                        running--
                    },
                )

            val videos =
                repositoryOf(api = api, maxParallelRequests = 3)
                    .loadFeed(
                        forceRefresh = false,
                    ).videos()

            assertEquals(20, videos.size)
            assertEquals(3, peak)
        }

    private suspend fun failingWith(
        error: Exception,
    ): DomainResult<List<Video>> {
        val api =
            object : YouTubeApi by FakeYouTubeApi() {
                override suspend fun subscriptions(
                    pageToken: String?,
                ) = throw error
            }
        return repositoryOf(api = api).loadFeed(
            forceRefresh = false,
        )
    }
}
