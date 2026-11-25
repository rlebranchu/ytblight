package fr.rlebranchu.ytblight.external.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionListResponseDto(
    val items: List<SubscriptionDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SubscriptionDto(
    val snippet: SubscriptionSnippetDto,
)

@Serializable
data class SubscriptionSnippetDto(
    val resourceId: ResourceIdDto,
)

@Serializable
data class ResourceIdDto(
    val channelId: String,
)
