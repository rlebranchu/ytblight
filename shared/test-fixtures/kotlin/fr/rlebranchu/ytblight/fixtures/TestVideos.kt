package fr.rlebranchu.ytblight.fixtures

import fr.rlebranchu.ytblight.domain.model.Video
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Vidéo de test : seuls les champs utiles au test sont à préciser. */
fun testVideo(
    id: String,
    title: String = "Titre $id",
    channelId: String = "UC1",
    channelTitle: String = "Chaîne",
    duration: Duration = 10.minutes,
    publishedAt: Instant = Instant.fromEpochMilliseconds(0),
) = Video(
    id = id,
    title = title,
    channelId = channelId,
    channelTitle = channelTitle,
    thumbnailUrl = null,
    publishedAt = publishedAt,
    duration = duration,
)
