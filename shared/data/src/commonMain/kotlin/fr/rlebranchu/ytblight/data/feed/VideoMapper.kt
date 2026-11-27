package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.domain.model.Video
import fr.rlebranchu.ytblight.external.dto.VideoDto
import kotlin.time.Duration
import kotlin.time.Instant

/** Convertit un DTO en vidéo métier, ou `null` si la date ou la durée est illisible. */
internal fun VideoDto.toDomainOrNull(): Video? =
    runCatching {
        Instant.parse(
            input = snippet.publishedAt,
        )
    }.getOrNull()
        ?.let { publishedAt ->
            Duration
                .parseIsoStringOrNull(
                    value = contentDetails.duration,
                )?.let { duration ->
                    Video(
                        id = id,
                        title = snippet.title,
                        channelId = snippet.channelId,
                        channelTitle = snippet.channelTitle,
                        thumbnailUrl =
                            snippet.thumbnails
                                .run {
                                    medium ?: high ?: default
                                }?.url,
                        publishedAt = publishedAt,
                        duration = duration,
                    )
                }
        }
