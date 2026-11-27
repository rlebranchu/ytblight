import { RefreshCw } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import type { FeedItemJs } from '@/kotlin'
import { strings } from '@/strings'

interface FeedScreenProps {
  videos: FeedItemJs[]
  isLoading: boolean
  isRefreshing: boolean
  error: string | null
  onRefresh: () => void
  onRetry: () => void
  onSignOut: () => void
  onReconnect: () => void
  onOpenVideo: (videoId: string) => void
}

function errorMessage(error: string): string {
  switch (error) {
    case 'NOT_SIGNED_IN':
      return strings.feed.errorNotSignedIn
    case 'QUOTA_EXCEEDED':
      return strings.feed.errorQuotaExceeded
    case 'NETWORK':
      return strings.feed.errorNetwork
    default:
      return strings.feed.errorUnknown
  }
}

interface ErrorActionProps {
  error: string
  onRetry: () => void
  onReconnect: () => void
}

function ErrorAction({ error, onRetry, onReconnect }: ErrorActionProps) {
  if (error === 'QUOTA_EXCEEDED') return null
  if (error === 'NOT_SIGNED_IN') {
    return <Button onClick={onReconnect}>{strings.feed.reconnect}</Button>
  }
  return <Button onClick={onRetry}>{strings.feed.retry}</Button>
}

export function FeedScreen(props: FeedScreenProps) {
  const { videos, isLoading, isRefreshing, error } = props
  return (
    <div className="mx-auto flex min-h-screen max-w-2xl flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b bg-background/95 px-4 py-3 backdrop-blur">
        <h1 className="text-lg font-semibold">{strings.feed.title}</h1>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            disabled={isRefreshing}
            onClick={props.onRefresh}
            aria-label={strings.feed.refresh}
          >
            <RefreshCw className={isRefreshing ? 'animate-spin' : undefined} />
          </Button>
          <Button variant="outline" size="sm" onClick={props.onSignOut}>
            {strings.feed.signOut}
          </Button>
        </div>
      </header>

      {error && (
        <div role="alert" className="flex flex-col items-start gap-3 bg-destructive/15 p-4">
          <p>{errorMessage(error)}</p>
          <ErrorAction error={error} onRetry={props.onRetry} onReconnect={props.onReconnect} />
        </div>
      )}

      {isLoading && (
        <div className="flex flex-col gap-6 p-4" role="status" aria-label={strings.loading}>
          {[0, 1, 2].map((index) => (
            <Skeleton key={index} className="aspect-video w-full" />
          ))}
        </div>
      )}

      {!isLoading && !error && videos.length === 0 && (
        <p className="p-8 text-center text-muted-foreground">{strings.feed.empty}</p>
      )}

      <ul className="flex flex-col gap-6 p-4">
        {videos.map((video) => (
          <li key={video.id}>
            <button
              type="button"
              className="flex w-full flex-col gap-2 text-left"
              onClick={() => props.onOpenVideo(video.id)}
            >
              <div className="relative aspect-video overflow-hidden rounded-xl bg-muted">
                {video.thumbnailUrl && (
                  <img src={video.thumbnailUrl} alt="" className="size-full object-cover" />
                )}
                <span className="absolute right-2 bottom-2 rounded bg-black/80 px-1.5 py-0.5 text-xs text-white">
                  {video.durationLabel}
                </span>
              </div>
              <span className="line-clamp-2 font-medium">{video.title}</span>
              <span className="text-sm text-muted-foreground">
                {strings.feed.videoSubtitle({
                  channel: video.channelTitle,
                  published: video.publishedLabel,
                })}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
