package fr.rlebranchu.ytblight.external

import fr.rlebranchu.ytblight.external.dto.ChannelListResponseDto
import fr.rlebranchu.ytblight.external.dto.PlaylistItemListResponseDto
import fr.rlebranchu.ytblight.external.dto.SubscriptionListResponseDto
import fr.rlebranchu.ytblight.external.dto.VideoListResponseDto

/** Les appels à l'API YouTube Data v3 dont l'application a besoin. */
interface YouTubeApi {
    /** Abonnements de l'utilisateur connecté, par pages de 50 (1 unité de quota). */
    suspend fun subscriptions(pageToken: String?): SubscriptionListResponseDto

    /** Playlists « uploads » de 50 chaînes au plus (1 unité de quota). */
    suspend fun channels(channelIds: List<String>): ChannelListResponseDto

    /** Dernières vidéos d'une playlist (1 unité de quota). */
    suspend fun playlistItems(
        playlistId: String,
        maxResults: Int,
    ): PlaylistItemListResponseDto

    /** Détails et durée de 50 vidéos au plus (1 unité de quota). */
    suspend fun videos(videoIds: List<String>): VideoListResponseDto

    companion object {
        /** Taille maximale d'une page ou d'un lot pour l'API. */
        const val MAX_BATCH_SIZE = 50
    }
}
