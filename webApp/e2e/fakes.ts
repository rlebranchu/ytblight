import type { Page } from '@playwright/test'

const MINUTE = 60_000

function iso(params: { minutesAgo: number }): string {
  return new Date(Date.now() - params.minutesAgo * MINUTE).toISOString()
}

/** Quatre vidéos : trois longues et un Short (45 s) que le fil doit masquer. */
const VIDEOS = [
  { id: 'V1', title: 'Un long format sur Kotlin', duration: 'PT24M10S', minutesAgo: 60 * 24 * 5 },
  { id: 'V2', title: 'Un court extrait', duration: 'PT45S', minutesAgo: 60 * 24 * 4 },
  { id: 'V3', title: "Visite guidée d'un atelier", duration: 'PT12M3S', minutesAgo: 60 * 24 * 3 },
  { id: 'V4', title: "Conférence d'une heure", duration: 'PT1H2M', minutesAgo: 60 * 24 * 2 },
]

const PNG_1PX = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
  'base64',
)

/** Simule l'API YouTube : aucun appel ne part vers Google. */
export async function fakeYouTube(params: { page: Page; unauthorizedFirst?: boolean }) {
  const { page } = params
  const calls = { subscriptions: 0 }
  let rejected = false

  await page.route('https://i.ytimg.com/**', (route) =>
    route.fulfill({ contentType: 'image/png', body: PNG_1PX }),
  )
  await page.route('https://www.googleapis.com/youtube/v3/**', (route) => {
    const url = new URL(route.request().url())
    const json = (body: unknown) =>
      route.fulfill({
        contentType: 'application/json',
        headers: { 'access-control-allow-origin': '*' },
        body: JSON.stringify(body),
      })
    if (route.request().method() === 'OPTIONS') {
      return route.fulfill({
        status: 204,
        headers: {
          'access-control-allow-origin': '*',
          'access-control-allow-headers': '*',
          'access-control-allow-methods': 'GET',
        },
      })
    }
    if (url.pathname.endsWith('/subscriptions')) {
      calls.subscriptions += 1
      if (params.unauthorizedFirst && !rejected) {
        rejected = true
        return route.fulfill({
          status: 401,
          headers: {
            'access-control-allow-origin': '*',
            'access-control-expose-headers': 'WWW-Authenticate',
            'www-authenticate': 'Bearer realm="https://accounts.google.com/"',
          },
          contentType: 'application/json',
          body: '{"error":{"code":401}}',
        })
      }
      return json({ items: [{ snippet: { resourceId: { channelId: 'UC1' } } }] })
    }
    if (url.pathname.endsWith('/channels')) {
      return json({
        items: [{ id: 'UC1', contentDetails: { relatedPlaylists: { uploads: 'UU1' } } }],
      })
    }
    if (url.pathname.endsWith('/playlistItems')) {
      return json({ items: VIDEOS.map((v) => ({ contentDetails: { videoId: v.id } })) })
    }
    if (url.pathname.endsWith('/videos')) {
      return json({
        items: VIDEOS.map((v) => ({
          id: v.id,
          snippet: {
            title: v.title,
            channelId: 'UC1',
            channelTitle: 'Chaîne Kotlin',
            publishedAt: iso({ minutesAgo: v.minutesAgo }),
            thumbnails: { medium: { url: `https://i.ytimg.com/vi/${v.id}/mqdefault.jpg` } },
          },
          contentDetails: { duration: v.duration },
        })),
      })
    }
    return route.fulfill({ status: 404 })
  })
  return calls
}

type SilentBehavior = 'granted' | 'refused'

/**
 * Remplace Google Identity Services par un faux : aucune vraie fenêtre, aucun vrai compte.
 * `silent` : réponse au renouvellement silencieux (`granted` si Google a déjà une session, sinon `refused`).
 */
export async function fakeGoogle(params: { page: Page; silent?: SilentBehavior }) {
  await params.page.addInitScript((silent) => {
    const w = window as unknown as Record<string, unknown>
    const log = { prompts: [] as string[], revoked: [] as string[] }
    w.__gis = log
    let counter = 0
    w.google = {
      accounts: {
        oauth2: {
          initTokenClient: (config: {
            callback: (r: unknown) => void
            error_callback: (e: { type: string }) => void
          }) => ({
            requestAccessToken: (overrides: { prompt: string }) => {
              log.prompts.push(overrides.prompt)
              if (overrides.prompt === 'none' && silent === 'refused') {
                config.callback({ error: 'interaction_required' })
                return
              }
              counter += 1
              config.callback({ access_token: `token-${counter}`, expires_in: 3600 })
            },
          }),
          // eslint-disable-next-line max-params -- signature imposée par Google Identity Services
          revoke: (token: string, done: () => void) => {
            log.revoked.push(token)
            done()
          },
        },
      },
    }
  }, params.silent ?? 'refused')
}

export async function googleLog(params: { page: Page }) {
  return params.page.evaluate(
    () => (window as unknown as { __gis: { prompts: string[]; revoked: string[] } }).__gis,
  )
}

/** Prépare un jeton déjà enregistré par une visite précédente (une seule fois par onglet). */
export async function seedStoredToken(params: {
  page: Page
  token: string
  expiresInMillis: number
}) {
  await params.page.addInitScript((seed) => {
    if (sessionStorage.getItem('seeded')) return
    sessionStorage.setItem('seeded', '1')
    localStorage.setItem('ytblight.token', seed.token)
    localStorage.setItem('ytblight.token.expiresAt', String(Date.now() + seed.expiresInMillis))
  }, params)
}
