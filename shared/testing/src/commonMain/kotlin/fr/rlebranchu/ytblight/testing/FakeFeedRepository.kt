package fr.rlebranchu.ytblight.testing

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
            value =
                listOf(
                    video(
                        id = "fake-1",
                        title = "Un long format sur Kotlin Multiplatform",
                        channel = "Chaîne Kotlin",
                        duration = 24.minutes + 10.seconds,
                        now = now,
                        daysAgo = 5,
                    ),
                    video(
                        id = "fake-2",
                        title = "Un court extrait",
                        channel = "Chaîne Kotlin",
                        duration = 45.seconds,
                        now = now,
                        daysAgo = 4,
                    ),
                    video(
                        id = "fake-3",
                        title = "Visite guidée d'un atelier",
                        channel = "Atelier",
                        duration = 12.minutes + 3.seconds,
                        now = now,
                        daysAgo = 3,
                    ),
                    video(
                        id = "fake-4",
                        title = "Conférence d'une heure",
                        channel = "Conférences",
                        duration = 62.minutes,
                        now = now,
                        daysAgo = 2,
                    ),
                    video(
                        id = "fake-5",
                        title = "Un autre Short",
                        channel = "Atelier",
                        duration = 30.seconds,
                        now = now,
                        daysAgo = 1,
                    ),
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
