import { useCallback } from 'react'
import { useNavigate, useParams } from 'react-router'

import { useViewModel } from '@/hooks/useViewModel'
import { createPlayerViewModel, type PlayerFacade, type PlayerStateJs } from '@/kotlin'
import { PlayerScreen } from './PlayerScreen'
import { useEffect } from 'react'

export function PlayerRoute() {
  const navigate = useNavigate()
  const { videoId = '' } = useParams()
  const create = useCallback(() => createPlayerViewModel(videoId), [videoId])
  const { facade, state } = useViewModel<PlayerStateJs, PlayerFacade>({ create })
  const isEnded = state?.isEnded ?? false

  // Pas de lecture suivante : la page se ferme à la fin de la vidéo.
  useEffect(() => {
    if (isEnded) navigate(-1)
  }, [isEnded, navigate])

  return (
    <PlayerScreen
      videoId={videoId}
      onVideoEnded={() => facade.current?.onVideoEnded()}
      onClose={() => navigate(-1)}
    />
  )
}
