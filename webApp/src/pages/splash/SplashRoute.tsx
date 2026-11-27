import { useEffect } from 'react'
import { useNavigate } from 'react-router'

import { useViewModel } from '@/hooks/useViewModel'
import { createSplashViewModel, type SplashFacade, type SplashStateJs } from '@/kotlin'
import { SplashScreen } from './SplashScreen'

export function SplashRoute() {
  const navigate = useNavigate()
  const { state } = useViewModel<SplashStateJs, SplashFacade>({ create: createSplashViewModel })
  const destination = state?.destination

  useEffect(() => {
    if (destination) {
      navigate(destination === 'FEED' ? '/feed' : '/signin', { replace: true })
    }
  }, [destination, navigate])

  return <SplashScreen />
}
