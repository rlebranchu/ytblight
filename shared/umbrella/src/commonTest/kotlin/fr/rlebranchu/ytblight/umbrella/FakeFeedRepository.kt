package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Fil factice : deux vidéos longues de dates différentes et un Short que le domaine doit filtrer. */
class FakeFeedRepository : FeedRepository {
    override suspend fun loadFeed(
        forceRefresh: Boolean,
    ): DomainResult<List<Video>> =
        DomainResult.Success(
            value =
                listOf(
                    video(
                        id = "old",
                        duration = 10.minutes,
                        publishedAtSeconds = 1_000,
                    ),
                    video(
                        id = "short",
                        duration = 30.seconds,
                        publishedAtSeconds = 3_000,
                    ),
                    video(
                        id = "new",
                        duration = 10.minutes,
                        publishedAtSeconds = 2_000,
                    ),
                ),
        )

    override suspend fun clear() = Unit

    private fun video(
        id: String,
        duration: Duration,
        publishedAtSeconds: Long,
    ) = Video(
        id = id,
        title = "Titre $id",
        channelId = "UC1",
        channelTitle = "Chaîne",
        thumbnailUrl = null,
        publishedAt = Instant.fromEpochSeconds(publishedAtSeconds),
        duration = duration,
    )
}
