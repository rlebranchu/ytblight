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

        override suspend fun loadFeed(
            forceRefresh: Boolean,
        ): DomainResult<List<Video>> {
            requests += forceRefresh
            return result
        }

        override suspend fun clear() = Unit
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
                    result =
                        DomainResult.Success(
                            value =
                                listOf(
                                    video(
                                        id = "short-30s",
                                        duration = 30.seconds,
                                    ),
                                    video(
                                        id = "short-3min",
                                        duration = 3.minutes,
                                    ),
                                    video(
                                        id = "long-3min-1s",
                                        duration = 3.minutes + 1.seconds,
                                    ),
                                    video(
                                        id = "long-20min",
                                        duration = 20.minutes,
                                    ),
                                ),
                        ),
                )

            val result =
                GetFeedUseCase(
                    repository = repository,
                )()

            val ids = (result as DomainResult.Success).value.map { it.id }
            assertEquals(setOf("long-3min-1s", "long-20min"), ids.toSet())
        }

    @Test
    fun dropsLiveAndUpcomingVideosWithZeroDuration() =
        runTest {
            val repository =
                FakeFeedRepository(
                    result =
                        DomainResult.Success(
                            value =
                                listOf(
                                    video(
                                        id = "live",
                                        duration = Duration.ZERO,
                                    ),
                                ),
                        ),
                )

            val result =
                GetFeedUseCase(
                    repository = repository,
                )()

            assertEquals(emptyList(), (result as DomainResult.Success).value)
        }

    @Test
    fun sortsFromNewestToOldest() =
        runTest {
            val repository =
                FakeFeedRepository(
                    result =
                        DomainResult.Success(
                            value =
                                listOf(
                                    video(
                                        id = "old",
                                        duration = 10.minutes,
                                        publishedAtMillis = 1_000,
                                    ),
                                    video(
                                        id = "new",
                                        duration = 10.minutes,
                                        publishedAtMillis = 3_000,
                                    ),
                                    video(
                                        id = "middle",
                                        duration = 10.minutes,
                                        publishedAtMillis = 2_000,
                                    ),
                                ),
                        ),
                )

            val result =
                GetFeedUseCase(
                    repository = repository,
                )()

            assertEquals(listOf("new", "middle", "old"), (result as DomainResult.Success).value.map { it.id })
        }

    @Test
    fun usesTheConfiguredShortThreshold() =
        runTest {
            val repository =
                FakeFeedRepository(
                    result =
                        DomainResult.Success(
                            value =
                                listOf(
                                    video(
                                        id = "two-min",
                                        duration = 2.minutes,
                                    ),
                                ),
                        ),
                )

            val result =
                GetFeedUseCase(
                    repository = repository,
                    policy =
                        FeedPolicy(
                            maxShortDuration = 60.seconds,
                        ),
                )()

            assertEquals(listOf("two-min"), (result as DomainResult.Success).value.map { it.id })
        }

    @Test
    fun propagatesFailures() =
        runTest {
            val repository =
                FakeFeedRepository(
                    result =
                        DomainResult.Failure(
                            error = DomainError.QuotaExceeded,
                        ),
                )

            val result =
                GetFeedUseCase(
                    repository = repository,
                )()

            assertEquals(
                DomainResult.Failure(
                    error = DomainError.QuotaExceeded,
                ),
                result,
            )
        }

    @Test
    fun forwardsTheRefreshRequestToTheRepository() =
        runTest {
            val repository =
                FakeFeedRepository(
                    result =
                        DomainResult.Success(
                            value = emptyList(),
                        ),
                )
            val useCase =
                GetFeedUseCase(
                    repository = repository,
                )

            useCase()
            useCase(forceRefresh = true)

            assertEquals(listOf(false, true), repository.requests)
        }
}
