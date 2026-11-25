package fr.rlebranchu.ytblight.domain.feed

import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Règles anti-distraction du fil.
 *
 * Les Shorts n'ont pas de marqueur dans l'API : on les repère à leur durée. Ils peuvent aller
 * jusqu'à 3 minutes, d'où le seuil par défaut. Une durée nulle (direct ou première à venir)
 * est donc aussi écartée.
 */
data class FeedPolicy(
    val maxShortDuration: Duration = DEFAULT_MAX_SHORT_DURATION,
) {
    fun isShort(video: Video): Boolean = video.duration <= maxShortDuration

    /** Retire les Shorts et classe du plus récent au plus ancien. */
    fun apply(videos: List<Video>): List<Video> =
        videos
            .filterNot(::isShort)
            .sortedByDescending { it.publishedAt }

    companion object {
        val DEFAULT_MAX_SHORT_DURATION: Duration = 3.minutes
    }
}
