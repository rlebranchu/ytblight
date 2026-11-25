package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Fil factice pour les tests d'interface (Compose, XCUITest, Playwright) : aucun appel à Google,
 * aucun quota consommé. Les dates sont relatives à l'instant présent, pour que les libellés
 * (« il y a 5 jours »…) restent stables quel que soit le jour d'exécution.
 *
 * Contient volontairement des Shorts, que la règle du domaine doit filtrer.
 */
class FakeFeedRepository(
    private val clock: Clock = Clock.System,
) : FeedRepository {
    override suspend fun loadFeed(
        forceRefresh: Boolean,
    ): DomainResult<List<Video>> {
        val now = clock.now()
        return DomainResult.Success(
            listOf(
                video(
                    "fake-1",
                    "Un long format sur Kotlin Multiplatform",
                    "Chaîne Kotlin",
                    24.minutes + 10.seconds,
                    now,
                    5,
                ),
                video("fake-2", "Un court extrait", "Chaîne Kotlin", 45.seconds, now, 4),
                video("fake-3", "Visite guidée d'un atelier", "Atelier", 12.minutes + 3.seconds, now, 3),
                video("fake-4", "Conférence d'une heure", "Conférences", 62.minutes, now, 2),
                video("fake-5", "Un autre Short", "Atelier", 30.seconds, now, 1),
            ),
        )
    }

    override suspend fun clear() = Unit

    private fun video(
        id: String,
        title: String,
        channel: String,
        duration: Duration,
        now: kotlin.time.Instant,
        daysAgo: Int,
    ) = Video(
        id = id,
        title = title,
        channelId = "UC-$channel",
        channelTitle = channel,
        thumbnailUrl = null,
        publishedAt = now - daysAgo.days - 1.minutes,
        duration = duration,
    )
}
