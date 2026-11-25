package fr.rlebranchu.ytblight.domain.model

import kotlin.time.Duration
import kotlin.time.Instant

/** Une vidéo d'une chaîne à laquelle l'utilisateur est abonné. */
data class Video(
    val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String?,
    val publishedAt: Instant,
    val duration: Duration,
)
