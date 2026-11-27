import { Skeleton } from '@/components/ui/skeleton'
import { strings } from '@/strings'

export function SplashScreen() {
  return (
    <main
      className="flex min-h-screen items-center justify-center"
      role="status"
      aria-label={strings.loading}
    >
      <Skeleton className="size-10 rounded-full" />
    </main>
  )
}
