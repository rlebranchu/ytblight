package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
import fr.rlebranchu.ytblight.external.YouTubeApi
import fr.rlebranchu.ytblight.external.YouTubeHttpException
import fr.rlebranchu.ytblight.external.dto.VideoDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Construit le fil à partir de l'API YouTube.
 *
 * Coût en quota pour N abonnements : environ 3 × N/50 + N + 1 unités, dominé par un appel
 * `playlistItems` par chaîne. Le résultat est gardé en mémoire pour la session : seul un
 * rafraîchissement explicite ré-interroge l'API.
 */
class FeedRepositoryImpl(
    private val api: YouTubeApi,
    private val videosPerChannel: Int = DEFAULT_VIDEOS_PER_CHANNEL,
    private val maxParallelRequests: Int = DEFAULT_MAX_PARALLEL_REQUESTS,
) : FeedRepository {
    private val mutex = Mutex()
    private var cache: List<Video>? = null

    override suspend fun loadFeed(
        forceRefresh: Boolean,
    ): DomainResult<List<Video>> =
        mutex.withLock {
            val cached = cache
            if (!forceRefresh && cached != null) {
                return DomainResult.Success(
                    value = cached,
                )
            }
            try {
                val videos = fetchFeed()
                cache = videos
                DomainResult.Success(
                    value = videos,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DomainResult.Failure(
                    error = e.toDomainError(),
                )
            }
        }

    override suspend fun clear() {
        mutex.withLock { cache = null }
    }

    private suspend fun fetchFeed(): List<Video> {
        val channelIds = fetchSubscribedChannelIds()
        val playlistIds =
            fetchUploadsPlaylistIds(
                channelIds = channelIds,
            )
        val videoIds =
            fetchRecentVideoIds(
                playlistIds = playlistIds,
            )
        return fetchVideos(
            videoIds = videoIds,
        )
    }

    private suspend fun fetchSubscribedChannelIds(): List<String> {
        val channelIds = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val page =
                api.subscriptions(
                    pageToken = pageToken,
                )
            channelIds += page.items.map { it.snippet.resourceId.channelId }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return channelIds.distinct()
    }

    private suspend fun fetchUploadsPlaylistIds(
        channelIds: List<String>,
    ): List<String> =
        channelIds
            .chunked(YouTubeApi.MAX_BATCH_SIZE)
            .flatMap { batch ->
                api
                    .channels(
                        channelIds = batch,
                    ).items
            }.mapNotNull { it.contentDetails.relatedPlaylists.uploads }

    private suspend fun fetchRecentVideoIds(
        playlistIds: List<String>,
    ): List<String> =
        coroutineScope {
            val permits = Semaphore(maxParallelRequests)
            playlistIds
                .map { playlistId ->
                    async {
                        permits.withPermit {
                            recentVideoIds(
                                playlistId = playlistId,
                            )
                        }
                    }
                }.awaitAll()
                .flatten()
                .distinct()
        }

    /** Une playlist introuvable (chaîne sans vidéo) ne doit pas faire échouer tout le fil. */
    private suspend fun recentVideoIds(
        playlistId: String,
    ): List<String> =
        try {
            api
                .playlistItems(
                    playlistId = playlistId,
                    maxResults = videosPerChannel,
                ).items
                .map { it.contentDetails.videoId }
        } catch (e: YouTubeHttpException) {
            if (e.statusCode == HTTP_NOT_FOUND) emptyList() else throw e
        }

    private suspend fun fetchVideos(
        videoIds: List<String>,
    ): List<Video> =
        videoIds
            .chunked(YouTubeApi.MAX_BATCH_SIZE)
            .flatMap { batch ->
                api
                    .videos(
                        videoIds = batch,
                    ).items
            }.mapNotNull(VideoDto::toDomainOrNull)

    companion object {
        const val DEFAULT_VIDEOS_PER_CHANNEL = 5
        const val DEFAULT_MAX_PARALLEL_REQUESTS = 8
        private const val HTTP_NOT_FOUND = 404
    }
}
