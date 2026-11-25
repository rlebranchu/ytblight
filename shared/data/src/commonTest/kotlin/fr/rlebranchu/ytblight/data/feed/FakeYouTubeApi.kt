package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.dto.ChannelContentDetailsDto
import fr.rlebranchu.ytblight.external.dto.ChannelDto
import fr.rlebranchu.ytblight.external.dto.ChannelListResponseDto
import fr.rlebranchu.ytblight.external.dto.PlaylistItemContentDetailsDto
import fr.rlebranchu.ytblight.external.dto.PlaylistItemDto
import fr.rlebranchu.ytblight.external.dto.PlaylistItemListResponseDto
import fr.rlebranchu.ytblight.external.dto.RelatedPlaylistsDto
import fr.rlebranchu.ytblight.external.dto.ResourceIdDto
import fr.rlebranchu.ytblight.external.dto.SubscriptionDto
import fr.rlebranchu.ytblight.external.dto.SubscriptionListResponseDto
import fr.rlebranchu.ytblight.external.dto.SubscriptionSnippetDto
import fr.rlebranchu.ytblight.external.dto.ThumbnailDto
import fr.rlebranchu.ytblight.external.dto.ThumbnailsDto
import fr.rlebranchu.ytblight.external.dto.VideoContentDetailsDto
import fr.rlebranchu.ytblight.external.dto.VideoDto
import fr.rlebranchu.ytblight.external.dto.VideoListResponseDto
import fr.rlebranchu.ytblight.external.dto.VideoSnippetDto

/** Fausse API en mémoire : une page d'abonnements = une liste de chaînes. */
class FakeYouTubeApi(
    private val subscriptionPages: List<List<String>> = emptyList(),
    private val uploadsByChannel: Map<String, String> = emptyMap(),
    private val videoIdsByPlaylist: Map<String, List<String>> = emptyMap(),
    private val videos: Map<String, VideoDto> = emptyMap(),
    private val onPlaylistItems: suspend (String) -> Unit = {},
) : YouTubeApi {
    var subscriptionCalls = 0
        private set
    var channelCalls = 0
        private set
    var playlistItemCalls = 0
        private set
    var videoCalls = 0
        private set
    val requestedVideoBatches = mutableListOf<List<String>>()

    override suspend fun subscriptions(
        pageToken: String?,
    ): SubscriptionListResponseDto {
        subscriptionCalls++
        val index = pageToken?.removePrefix("page-")?.toInt() ?: 0
        val items = subscriptionPages.getOrElse(index) { emptyList() }
        return SubscriptionListResponseDto(
            items =
                items.map {
                    SubscriptionDto(
                        snippet =
                            SubscriptionSnippetDto(
                                resourceId =
                                    ResourceIdDto(
                                        channelId = it,
                                    ),
                            ),
                    )
                },
            nextPageToken = if (index + 1 < subscriptionPages.size) "page-${index + 1}" else null,
        )
    }

    override suspend fun channels(
        channelIds: List<String>,
    ): ChannelListResponseDto {
        channelCalls++
        return ChannelListResponseDto(
            items =
                channelIds.map { id ->
                    ChannelDto(
                        id = id,
                        contentDetails =
                            ChannelContentDetailsDto(
                                relatedPlaylists =
                                    RelatedPlaylistsDto(
                                        uploads = uploadsByChannel[id],
                                    ),
                            ),
                    )
                },
        )
    }

    override suspend fun playlistItems(
        playlistId: String,
        maxResults: Int,
    ): PlaylistItemListResponseDto {
        playlistItemCalls++
        onPlaylistItems(playlistId)
        val ids = videoIdsByPlaylist.getValue(playlistId).take(maxResults)
        return PlaylistItemListResponseDto(
            items =
                ids.map {
                    PlaylistItemDto(
                        contentDetails =
                            PlaylistItemContentDetailsDto(
                                videoId = it,
                            ),
                    )
                },
        )
    }

    override suspend fun videos(
        videoIds: List<String>,
    ): VideoListResponseDto {
        videoCalls++
        requestedVideoBatches += videoIds
        return VideoListResponseDto(
            items = videoIds.mapNotNull { videos[it] },
        )
    }

    companion object {
        fun videoDto(
            id: String,
            channelId: String = "UC1",
            duration: String = "PT10M",
            publishedAt: String = "2026-11-20T18:00:00Z",
        ) = VideoDto(
            id = id,
            snippet =
                VideoSnippetDto(
                    title = "Titre $id",
                    channelId = channelId,
                    channelTitle = "Chaîne $channelId",
                    publishedAt = publishedAt,
                    thumbnails =
                        ThumbnailsDto(
                            medium =
                                ThumbnailDto(
                                    url = "https://img/$id.jpg",
                                ),
                        ),
                ),
            contentDetails =
                VideoContentDetailsDto(
                    duration = duration,
                ),
        )
    }
}
