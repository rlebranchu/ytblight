import { useEffect } from 'react'
import { useNavigate } from 'react-router'

import { useViewModel } from '@/hooks/useViewModel'
import { createFeedViewModel, type FeedFacade, type FeedStateJs } from '@/kotlin'
import { FeedScreen } from './FeedScreen'

export function FeedRoute() {
  const navigate = useNavigate()
  const { facade, state } = useViewModel<FeedStateJs, FeedFacade>({ create: createFeedViewModel })
  const isSignedOut = state?.isSignedOut ?? false

  useEffect(() => {
    if (isSignedOut) navigate('/signin', { replace: true })
  }, [isSignedOut, navigate])

  return (
    <FeedScreen
      videos={state ? Array.from(state.videos) : []}
      isLoading={state?.isLoading ?? true}
      isRefreshing={state?.isRefreshing ?? false}
      error={state?.error ?? null}
      onRefresh={() => facade.current?.refresh()}
      onRetry={() => facade.current?.retry()}
      onSignOut={() => facade.current?.signOut()}
      onReconnect={() => facade.current?.signOut()}
      onOpenVideo={(videoId) => navigate(`/player/${videoId}`)}
    />
  )
}
