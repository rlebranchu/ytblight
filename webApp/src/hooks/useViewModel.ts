import { useEffect, useRef, useState, type RefObject } from 'react'

interface Facade<State> {
  subscribe(listener: (state: State) => void): () => void
  clear(): void
}

/**
 * Crée le ViewModel partagé à l'affichage de la page et le libère à son démontage
 * (`create` doit être stable : fonction du module ou `useCallback`). `facade` est une référence :
 * on s'en sert dans les gestionnaires d'événements (`facade.current?.action()`).
 */
export function useViewModel<State, F extends Facade<State>>({
  create,
}: {
  create: () => F
}): { facade: RefObject<F | null>; state: State | null } {
  const facade = useRef<F | null>(null)
  const [state, setState] = useState<State | null>(null)

  useEffect(() => {
    const created = create()
    facade.current = created
    const stopListening = created.subscribe(setState)
    return () => {
      stopListening()
      created.clear()
      facade.current = null
    }
  }, [create])

  return { facade, state }
}
