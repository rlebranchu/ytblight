package fr.rlebranchu.ytblight.domain.feed

import fr.rlebranchu.ytblight.domain.error.DomainError
import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.model.Video
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class GetFeedUseCaseTest {
    private class FakeFeedRepository(
        private val result: DomainResult<List<Video>>,
    ) : FeedRepository {
        val requests = mutableListOf<Boolean>()

        override suspend fun loadFeed(forceRefresh: Boolean): DomainResult<List<Video>> {
            requests += forceRefresh
            return result
        }
    }

    private fun video(
        id: String,
        duration: Duration,
        publishedAtMillis: Long = 0,
    ) = Video(
        id = id,
        title = "Titre $id",
        channelId = "UC1",
        channelTitle = "Chaîne",
        thumbnailUrl = null,
        publishedAt = Instant.fromEpochMilliseconds(publishedAtMillis),
        duration = duration,
    )

    @Test
    fun removesShortsAndKeepsLongerVideos() =
        runTest {
            val repository =
                FakeFeedRepository(
                    DomainResult.Success(
                        listOf(
                            video("short-30s", 30.seconds),
                            video("short-3min", 3.minutes),
                            video("long-3min-1s", 3.minutes + 1.seconds),
                            video("long-20min", 20.minutes),
                        ),
                    ),
                )

            val result = GetFeedUseCase(repository)()

            val ids = (result as DomainResult.Success).value.map { it.id }
            assertEquals(setOf("long-3min-1s", "long-20min"), ids.toSet())
        }

    @Test
    fun dropsLiveAndUpcomingVideosWithZeroDuration() =
        runTest {
            val repository = FakeFeedRepository(DomainResult.Success(listOf(video("live", Duration.ZERO))))

            val result = GetFeedUseCase(repository)()

            assertEquals(emptyList(), (result as DomainResult.Success).value)
        }

    @Test
    fun sortsFromNewestToOldest() =
        runTest {
            val repository =
                FakeFeedRepository(
                    DomainResult.Success(
                        listOf(
                            video("old", 10.minutes, publishedAtMillis = 1_000),
                            video("new", 10.minutes, publishedAtMillis = 3_000),
                            video("middle", 10.minutes, publishedAtMillis = 2_000),
                        ),
                    ),
                )

            val result = GetFeedUseCase(repository)()

            assertEquals(listOf("new", "middle", "old"), (result as DomainResult.Success).value.map { it.id })
        }

    @Test
    fun usesTheConfiguredShortThreshold() =
        runTest {
            val repository = FakeFeedRepository(DomainResult.Success(listOf(video("two-min", 2.minutes))))

            val result = GetFeedUseCase(repository, FeedPolicy(maxShortDuration = 60.seconds))()

            assertEquals(listOf("two-min"), (result as DomainResult.Success).value.map { it.id })
        }

    @Test
    fun propagatesFailures() =
        runTest {
            val repository = FakeFeedRepository(DomainResult.Failure(DomainError.QuotaExceeded))

            val result = GetFeedUseCase(repository)()

            assertEquals(DomainResult.Failure(DomainError.QuotaExceeded), result)
        }

    @Test
    fun forwardsTheRefreshRequestToTheRepository() =
        runTest {
            val repository = FakeFeedRepository(DomainResult.Success(emptyList()))
            val useCase = GetFeedUseCase(repository)

            useCase()
            useCase(forceRefresh = true)

            assertEquals(listOf(false, true), repository.requests)
        }
}
