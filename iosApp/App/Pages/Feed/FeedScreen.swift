import Shared
import SwiftUI

struct FeedScreen: View {
    let videos: [FeedItemUi]
    let isLoading: Bool
    let error: FeedError?
    let onRefresh: () -> Void
    let onRetry: () -> Void
    let onSignOut: () -> Void
    let onReconnect: () -> Void
    let onOpenVideo: (String) -> Void

    var body: some View {
        List {
            if let error {
                errorBanner(error)
            }
            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
            } else if videos.isEmpty && error == nil {
                Text("feed.empty")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
            }
            ForEach(videos, id: \.id) { video in
                VideoRow(video: video)
                    .contentShape(Rectangle())
                    .onTapGesture { onOpenVideo(video.id) }
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets())
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isButton)
            }
        }
        .listStyle(.plain)
        .refreshable { onRefresh() }
        .navigationTitle(Text("feed.title"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("feed.signout", action: onSignOut)
                    .accessibilityIdentifier("feed.signout")
            }
        }
    }

    private func errorBanner(_ error: FeedError) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(message(for: error))
            switch error {
            case .quotaExceeded:
                EmptyView()
            case .notSignedIn:
                Button("feed.reconnect", action: onReconnect)
            case .network, .unknown:
                Button("feed.retry", action: onRetry)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color.red.opacity(0.15))
        .listRowInsets(EdgeInsets())
        .accessibilityIdentifier("feed.error")
    }

    private func message(for error: FeedError) -> LocalizedStringKey {
        switch error {
        case .notSignedIn: "feed.error.notSignedIn"
        case .quotaExceeded: "feed.error.quotaExceeded"
        case .network: "feed.error.network"
        case .unknown: "feed.error.unknown"
        }
    }
}

private struct VideoRow: View {
    let video: FeedItemUi

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .bottomTrailing) {
                AsyncImage(url: video.thumbnailUrl.flatMap(URL.init(string:))) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color(.secondarySystemBackground)
                }
                .aspectRatio(16 / 9, contentMode: .fit)
                .clipped()
                Text(video.durationLabel)
                    .font(.caption2)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(.black.opacity(0.8), in: RoundedRectangle(cornerRadius: 4))
                    .foregroundStyle(.white)
                    .padding(8)
            }
            Text(video.title)
                .font(.headline)
                .lineLimit(2)
                .padding(.horizontal)
            Text("feed.video.subtitle \(video.channelTitle) \(video.publishedLabel)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
                .padding(.bottom, 12)
        }
    }
}
