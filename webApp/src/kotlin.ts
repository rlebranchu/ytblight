// Façade du code Kotlin partagé (shared:umbrella, jsMain). Seul fichier qui l'importe.
export {
  createFeedViewModel,
  createPlayerViewModel,
  createSignInViewModel,
  createSplashViewModel,
  initYtblight,
} from 'ytblight-shared'
export type {
  FeedFacade,
  FeedItemJs,
  FeedStateJs,
  PlayerFacade,
  PlayerStateJs,
  SignInFacade,
  SignInStateJs,
  SplashFacade,
  SplashStateJs,
} from 'ytblight-shared'

// `GoogleIdentityBridge` est une interface externe (implémentée ici, en TypeScript) : elle
// n'apparaît pas dans les définitions générées, on la décrit donc à la main.
export interface GoogleTokenResponse {
  readonly accessToken?: string
  readonly expiresInSeconds?: number
  readonly error?: string
}

export interface GoogleIdentityBridge {
  requestToken(interactive: boolean): Promise<GoogleTokenResponse>
  revokeToken(accessToken: string): Promise<unknown>
}
