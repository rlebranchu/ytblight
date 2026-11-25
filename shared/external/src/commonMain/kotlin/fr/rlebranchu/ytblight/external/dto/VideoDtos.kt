package fr.rlebranchu.ytblight.external.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoListResponseDto(
    val items: List<VideoDto> = emptyList(),
)

@Serializable
data class VideoDto(
    val id: String,
    val snippet: VideoSnippetDto,
    val contentDetails: VideoContentDetailsDto,
)

@Serializable
data class VideoSnippetDto(
    val title: String,
    val channelId: String,
    val channelTitle: String,
    /** Date ISO 8601, par exemple `2026-11-20T18:00:07Z`. */
    val publishedAt: String,
    val thumbnails: ThumbnailsDto = ThumbnailsDto(),
)

@Serializable
data class ThumbnailsDto(
    val default: ThumbnailDto? = null,
    val medium: ThumbnailDto? = null,
    val high: ThumbnailDto? = null,
)

@Serializable
data class ThumbnailDto(
    val url: String,
)

@Serializable
data class VideoContentDetailsDto(
    /** Durée ISO 8601, par exemple `PT4M13S` (`P0D` pour un direct). */
    val duration: String,
)
