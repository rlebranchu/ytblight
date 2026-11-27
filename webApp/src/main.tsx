import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { HashRouter, Route, Routes } from 'react-router'

import './index.css'
import { createGoogleIdentityBridge } from '@/google/googleIdentity'
import { initYtblight } from '@/kotlin'
import { FeedRoute } from '@/pages/feed/FeedRoute'
import { PlayerRoute } from '@/pages/player/PlayerRoute'
import { SignInRoute } from '@/pages/signin/SignInRoute'
import { SplashRoute } from '@/pages/splash/SplashRoute'

// Démarre le code Kotlin partagé (Koin) avec la connexion Google d'ici.
initYtblight(createGoogleIdentityBridge({ clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID }))

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HashRouter>
      <Routes>
        <Route path="/" element={<SplashRoute />} />
        <Route path="/signin" element={<SignInRoute />} />
        <Route path="/feed" element={<FeedRoute />} />
        <Route path="/player/:videoId" element={<PlayerRoute />} />
      </Routes>
    </HashRouter>
  </StrictMode>,
)
