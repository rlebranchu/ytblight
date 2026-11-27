// Types minimaux de Google Identity Services (modèle « jeton », sans serveur).
interface GisTokenResponse {
  access_token?: string
  expires_in?: number | string
  error?: string
}

interface GisTokenClient {
  requestAccessToken: (overrides: { prompt: string }) => void
}

interface GisOauth2 {
  initTokenClient: (config: {
    client_id: string
    scope: string
    callback: (response: GisTokenResponse) => void
    error_callback: (error: { type: string }) => void
  }) => GisTokenClient
  revoke: (accessToken: string, done: () => void) => void
}

interface Window {
  google?: { accounts: { oauth2: GisOauth2 } }
}
