import { useEffect } from 'react'
import { useNavigate } from 'react-router'

import { useViewModel } from '@/hooks/useViewModel'
import { createSignInViewModel, type SignInFacade, type SignInStateJs } from '@/kotlin'
import { SignInScreen } from './SignInScreen'

export function SignInRoute() {
  const navigate = useNavigate()
  const { facade, state } = useViewModel<SignInStateJs, SignInFacade>({
    create: createSignInViewModel,
  })
  const isSignedIn = state?.isSignedIn ?? false

  useEffect(() => {
    if (isSignedIn) navigate('/feed', { replace: true })
  }, [isSignedIn, navigate])

  return (
    <SignInScreen
      inProgress={state?.inProgress ?? false}
      error={state?.error ?? null}
      errorDetail={state?.errorDetail ?? null}
      // Appelé directement depuis le clic : les navigateurs bloquent les fenêtres ouvertes plus tard.
      onSignIn={() => facade.current?.signIn()}
    />
  )
}
