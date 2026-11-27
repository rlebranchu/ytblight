import { useEffect, useRef } from 'react'

import { Button } from '@/components/ui/button'
import { strings } from '@/strings'

interface PlayerScreenProps {
  videoId: string
  onVideoEnded: () => void
  onClose: () => void
}

const YOUTUBE_ORIGIN = 'https://www.youtube.com'
const STATE_ENDED = 0

export function PlayerScreen({ videoId, onVideoEnded, onClose }: PlayerScreenProps) {
  const frame = useRef<HTMLIFrameElement>(null)

  // Le lecteur embarqué signale son état par postMessage (enablejsapi) : 0 = vidéo terminée.
  useEffect(() => {
    function onMessage(event: MessageEvent) {
      if (event.origin !== YOUTUBE_ORIGIN || typeof event.data !== 'string') return
      try {
        const message = JSON.parse(event.data)
        if (message.event === 'onStateChange' && message.info === STATE_ENDED) onVideoEnded()
      } catch {
        // message d'un autre format : ignoré
      }
    }
    window.addEventListener('message', onMessage)
    return () => window.removeEventListener('message', onMessage)
  }, [onVideoEnded])

  function startListening() {
    frame.current?.contentWindow?.postMessage(
      JSON.stringify({ event: 'listening', id: 1 }),
      YOUTUBE_ORIGIN,
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-black">
      <Button variant="ghost" className="self-start text-white" onClick={onClose}>
        {strings.player.back}
      </Button>
      <iframe
        ref={frame}
        title={strings.player.iframeTitle}
        className="aspect-video w-full max-w-5xl self-center"
        src={`${YOUTUBE_ORIGIN}/embed/${encodeURIComponent(videoId)}?autoplay=1&rel=0&enablejsapi=1&origin=${encodeURIComponent(window.location.origin)}`}
        allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
        allowFullScreen
        referrerPolicy="strict-origin-when-cross-origin"
        onLoad={startListening}
      />
    </div>
  )
}
