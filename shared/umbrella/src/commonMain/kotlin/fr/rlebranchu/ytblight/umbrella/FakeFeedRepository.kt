package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Fil factice et déterministe pour les tests d'interface (Compose, XCUITest, Playwright) :
 * aucun appel à Google, aucun quota consommé. Contient volontairement des Shorts, qui doivent
 * être filtrés par la règle du domaine.
 */
class FakeFeedRepository : FeedRepository {
    override suspend fun loadFeed(forceRefresh: Boolean): DomainResult<List<Video>> = DomainResult.Success(VIDEOS)

    private companion object {
        val VIDEOS =
            listOf(
                video("fake-1", "Un long format sur Kotlin Multiplatform", "Chaîne Kotlin", 24.minutes + 10.seconds, 5),
                video("fake-2", "Un court extrait", "Chaîne Kotlin", 45.seconds, 4),
                video("fake-3", "Visite guidée d'un atelier", "Atelier", 12.minutes + 3.seconds, 3),
                video("fake-4", "Conférence d'une heure", "Conférences", 62.minutes, 2),
                video("fake-5", "Un autre Short", "Atelier", 30.seconds, 1),
            )

        fun video(
            id: String,
            title: String,
            channel: String,
            duration: kotlin.time.Duration,
            daysAgo: Int,
        ) = Video(
            id = id,
            title = title,
            channelId = "UC-$channel",
            channelTitle = channel,
            thumbnailUrl = null,
            publishedAt = Instant.fromEpochSeconds(BASE_EPOCH_SECONDS - daysAgo * SECONDS_PER_DAY),
            duration = duration,
        )

        const val BASE_EPOCH_SECONDS = 1_795_000_000L
        const val SECONDS_PER_DAY = 86_400L
    }
}
