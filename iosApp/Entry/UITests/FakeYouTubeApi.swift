import Foundation
import Shared

/// API YouTube factice : un abonnement, quatre vidéos dont un Short (45 s) que le fil doit masquer.
/// Les dates sont relatives à l'instant présent pour que les libellés restent stables.
final class FakeYouTubeApi: YouTubeApi, @unchecked Sendable {
    private struct Entry {
        let id: String
        let title: String
        let duration: String
        let daysAgo: Int
    }

    private let entries = [
        Entry(id: "V1", title: "Un long format sur Kotlin", duration: "PT24M10S", daysAgo: 5),
        Entry(id: "V2", title: "Un court extrait", duration: "PT45S", daysAgo: 4),
        Entry(id: "V3", title: "Visite guidée d'un atelier", duration: "PT12M3S", daysAgo: 3),
        Entry(id: "V4", title: "Conférence d'une heure", duration: "PT1H2M", daysAgo: 2),
    ]

    func __subscriptions(pageToken: String?) async throws -> SubscriptionListResponseDto {
        SubscriptionListResponseDto(
            items: [SubscriptionDto(snippet: SubscriptionSnippetDto(resourceId: ResourceIdDto(channelId: "UC1")))],
            nextPageToken: nil
        )
    }

    func __channels(channelIds: [String]) async throws -> ChannelListResponseDto {
        ChannelListResponseDto(
            items: [
                ChannelDto(
                    id: "UC1",
                    contentDetails: ChannelContentDetailsDto(relatedPlaylists: RelatedPlaylistsDto(uploads: "UU1"))
                )
            ]
        )
    }

    func __playlistItems(playlistId: String, maxResults: Int32) async throws -> PlaylistItemListResponseDto {
        PlaylistItemListResponseDto(
            items: entries.map { PlaylistItemDto(contentDetails: PlaylistItemContentDetailsDto(videoId: $0.id)) },
            nextPageToken: nil
        )
    }

    func __videos(videoIds: [String]) async throws -> VideoListResponseDto {
        let formatter = ISO8601DateFormatter()
        return VideoListResponseDto(
            items: entries.map { entry in
                let published = Date().addingTimeInterval(-Double(entry.daysAgo) * 86_400 - 60)
                return VideoDto(
                    id: entry.id,
                    snippet: VideoSnippetDto(
                        title: entry.title,
                        channelId: "UC1",
                        channelTitle: "Chaîne Kotlin",
                        publishedAt: formatter.string(from: published),
                        thumbnails: ThumbnailsDto(default: nil, medium: nil, high: nil)
                    ),
                    contentDetails: VideoContentDetailsDto(duration: entry.duration)
                )
            }
        )
    }
}
