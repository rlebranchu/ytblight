package fr.rlebranchu.ytblight.external.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItemListResponseDto(
    val items: List<PlaylistItemDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaylistItemDto(
    val contentDetails: PlaylistItemContentDetailsDto,
)

@Serializable
data class PlaylistItemContentDetailsDto(
    val videoId: String,
)
