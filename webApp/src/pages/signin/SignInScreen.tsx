import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { strings } from '@/strings'

interface SignInScreenProps {
  inProgress: boolean
  error: string | null
  errorDetail: string | null
  onSignIn: () => void
}

function errorMessage(params: { error: string; detail: string | null }): string {
  switch (params.error) {
    case 'CANCELLED':
      return strings.signIn.errorCancelled
    case 'CONFIGURATION':
      return strings.signIn.errorConfiguration
    case 'FAILED':
      return strings.signIn.errorFailed
    default:
      return params.detail
        ? strings.signIn.errorUnavailableDetail(params.detail)
        : strings.signIn.errorUnavailable
  }
}

export function SignInScreen({ inProgress, error, errorDetail, onSignIn }: SignInScreenProps) {
  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <Card className="w-full max-w-sm text-center">
        <CardHeader>
          <CardTitle className="text-2xl">{strings.appName}</CardTitle>
          <CardDescription>{strings.signIn.message}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Button size="lg" disabled={inProgress} onClick={onSignIn}>
            {inProgress ? strings.signIn.inProgress : strings.signIn.button}
          </Button>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {errorMessage({ error, detail: errorDetail })}
            </p>
          )}
        </CardContent>
      </Card>
    </main>
  )
}
