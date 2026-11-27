package fr.rlebranchu.ytblight.presentation.feed

import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Instant

/** Une vidéo prête à afficher : les libellés sont déjà formatés, les interfaces ne calculent rien. */
data class FeedItemUi(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String?,
    /** Par exemple `24:10`. */
    val durationLabel: String,
    /** Par exemple `il y a 3 jours`. */
    val publishedLabel: String,
)

internal fun Video.toItemUi(
    now: Instant,
) = FeedItemUi(
    id = id,
    title = title,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl,
    durationLabel = duration.toClockLabel(),
    publishedLabel =
        relativeTimeLabel(
            publishedAt = publishedAt,
            now = now,
        ),
)
