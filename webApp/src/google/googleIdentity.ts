import type { GoogleIdentityBridge, GoogleTokenResponse } from '@/kotlin'

const GIS_SCRIPT_URL = 'https://accounts.google.com/gsi/client'
const YOUTUBE_READONLY_SCOPE = 'https://www.googleapis.com/auth/youtube.readonly'

/** Charge le script de Google ; renvoie `null` (sans rejeter) s'il est inaccessible. */
function loadGisScript(): Promise<GisOauth2 | null> {
  return new Promise((resolve) => {
    if (window.google) {
      resolve(window.google.accounts.oauth2)
      return
    }
    const script = document.createElement('script')
    script.src = GIS_SCRIPT_URL
    script.async = true
    script.onload = () => resolve(window.google?.accounts.oauth2 ?? null)
    script.onerror = () => resolve(null)
    document.head.appendChild(script)
  })
}

/**
 * Pont entre le code Kotlin partagé et Google Identity Services. Ses promesses ne sont jamais
 * rejetées : un échec est décrit par le champ `error`.
 */
export function createGoogleIdentityBridge(params: { clientId: string }): GoogleIdentityBridge {
  async function requestToken(interactive: boolean): Promise<GoogleTokenResponse> {
    const oauth2 = await loadGisScript()
    if (!oauth2) return { error: 'gis_unavailable' }
    return new Promise((resolve) => {
      const client = oauth2.initTokenClient({
        client_id: params.clientId,
        scope: YOUTUBE_READONLY_SCOPE,
        callback: (response) => {
          if (response.error || !response.access_token) {
            resolve({ error: response.error ?? 'unknown' })
          } else {
            resolve({
              accessToken: response.access_token,
              expiresInSeconds: Number(response.expires_in ?? 3600),
            })
          }
        },
        error_callback: (error) => resolve({ error: error.type }),
      })
      // Silencieux : aucune fenêtre ; Google répond par une erreur s'il faut l'utilisateur.
      client.requestAccessToken({ prompt: interactive ? '' : 'none' })
    })
  }

  async function revokeToken(accessToken: string): Promise<unknown> {
    const oauth2 = await loadGisScript()
    if (!oauth2) return undefined
    return new Promise<void>((resolve) => oauth2.revoke(accessToken, resolve))
  }

  return { requestToken, revokeToken }
}
