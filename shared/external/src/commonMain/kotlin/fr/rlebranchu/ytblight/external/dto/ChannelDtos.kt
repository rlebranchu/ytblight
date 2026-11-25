package fr.rlebranchu.ytblight.external.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChannelListResponseDto(
    val items: List<ChannelDto> = emptyList(),
)

@Serializable
data class ChannelDto(
    val id: String,
    val contentDetails: ChannelContentDetailsDto,
)

@Serializable
data class ChannelContentDetailsDto(
    val relatedPlaylists: RelatedPlaylistsDto,
)

@Serializable
data class RelatedPlaylistsDto(
    val uploads: String? = null,
)
