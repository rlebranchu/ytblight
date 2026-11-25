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
                    "V1" to videoDto("V1", "UC1", "PT4M13S"),
                    "V2" to videoDto("V2", "UC1", "PT1H2M3S"),
                    "V3" to videoDto("V3", "UC2", "PT45S"),
                ),
        )

    private fun DomainResult<List<Video>>.videos(): List<Video> = (this as DomainResult.Success).value

    @Test
    fun buildsTheFeedFromAllSubscriptionPages() =
        runTest {
            val api = twoChannelsApi()

            val videos = FeedRepositoryImpl(api).loadFeed(forceRefresh = false).videos()

            assertEquals(setOf("V1", "V2", "V3"), videos.map { it.id }.toSet())
            assertEquals(2, api.subscriptionCalls)
            assertEquals(1, api.channelCalls)
            assertEquals(2, api.playlistItemCalls)
            assertEquals(1, api.videoCalls)
        }

    @Test
    fun mapsDurationsDatesAndThumbnails() =
        runTest {
            val videos = FeedRepositoryImpl(twoChannelsApi()).loadFeed(false).videos().associateBy { it.id }

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
            val repository = FeedRepositoryImpl(api)

            repository.loadFeed(forceRefresh = false)
            repository.loadFeed(forceRefresh = false)
            assertEquals(1, api.videoCalls)

            repository.loadFeed(forceRefresh = true)
            assertEquals(2, api.videoCalls)
            assertEquals(4, api.playlistItemCalls)
        }

    @Test
    fun forgetsTheCachedFeedOnClear() =
        runTest {
            val api = twoChannelsApi()
            val repository = FeedRepositoryImpl(api)
            repository.loadFeed(forceRefresh = false)

            repository.clear()
            repository.loadFeed(forceRefresh = false)

            assertEquals(2, api.videoCalls)
        }

    @Test
    fun asksForTheConfiguredNumberOfVideosPerChannel() =
        runTest {
            val api = twoChannelsApi()

            val videos = FeedRepositoryImpl(api, videosPerChannel = 1).loadFeed(false).videos()

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
                    videos = ids.associateWith { videoDto(it) },
                )

            val videos = FeedRepositoryImpl(api, videosPerChannel = 120).loadFeed(false).videos()

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
                            "V1" to videoDto("V1"),
                            "BAD-DURATION" to videoDto("BAD-DURATION", duration = "nimporte quoi"),
                            "BAD-DATE" to videoDto("BAD-DATE", publishedAt = "hier"),
                        ),
                )

            val videos = FeedRepositoryImpl(api).loadFeed(false).videos()

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
                    videos = mapOf("V1" to videoDto("V1")),
                )

            val videos = FeedRepositoryImpl(api).loadFeed(false).videos()

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
                            throw YouTubeHttpException(404, "playlistNotFound", null)
                        } else {
                            twoChannelsApi().playlistItems(playlistId, maxResults)
                        }
                }

            val videos = FeedRepositoryImpl(api).loadFeed(false).videos()

            assertEquals(listOf("V3"), videos.map { it.id })
        }

    @Test
    fun mapsQuotaErrors() =
        runTest {
            val result = failingWith(YouTubeHttpException(403, "quotaExceeded", "Quota"))

            assertEquals(DomainResult.Failure(DomainError.QuotaExceeded), result)
        }

    @Test
    fun mapsUnauthorizedToNotSignedIn() =
        runTest {
            val result = failingWith(YouTubeHttpException(401, null, null))

            assertEquals(DomainResult.Failure(DomainError.NotSignedIn), result)
        }

    @Test
    fun mapsNetworkFailures() =
        runTest {
            val result = failingWith(YouTubeNetworkException(RuntimeException("hors ligne")))

            assertEquals(DomainResult.Failure(DomainError.Network), result)
        }

    @Test
    fun keepsOtherForbiddenErrorsAsUnknown() =
        runTest {
            val result = failingWith(YouTubeHttpException(403, "forbidden", "Interdit"))

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
                            RuntimeException("x"),
                        )
                    } else {
                        healthy.subscriptions(pageToken)
                    }
                }
            val repository = FeedRepositoryImpl(api)

            assertIs<DomainResult.Failure>(repository.loadFeed(false))
            failing = false

            assertEquals(3, repository.loadFeed(false).videos().size)
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
                    videos = channels.associate { "V$it" to videoDto("V$it") },
                    onPlaylistItems = {
                        running++
                        peak = maxOf(peak, running)
                        delay(10)
                        running--
                    },
                )

            val videos = FeedRepositoryImpl(api, maxParallelRequests = 3).loadFeed(false).videos()

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
        return FeedRepositoryImpl(api).loadFeed(false)
    }
}
