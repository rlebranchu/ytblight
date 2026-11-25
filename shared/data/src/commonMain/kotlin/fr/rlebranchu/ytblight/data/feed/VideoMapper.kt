package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.domain.model.Video
import fr.rlebranchu.ytblight.external.dto.ThumbnailsDto
import fr.rlebranchu.ytblight.external.dto.VideoDto
import kotlin.time.Duration
import kotlin.time.Instant

/** Convertit un DTO en vidéo métier, ou `null` si la date ou la durée est illisible. */
internal fun VideoDto.toDomainOrNull(): Video? {
    val duration = Duration.parseIsoStringOrNull(contentDetails.duration) ?: return null
    val publishedAt = runCatching { Instant.parse(snippet.publishedAt) }.getOrNull() ?: return null
    return Video(
        id = id,
        title = snippet.title,
        channelId = snippet.channelId,
        channelTitle = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.bestUrl(),
        publishedAt = publishedAt,
        duration = duration,
    )
}

private fun ThumbnailsDto.bestUrl(): String? = (medium ?: high ?: default)?.url
