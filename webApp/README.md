# webApp

Application web en React + TypeScript (Vite) avec shadcn/ui, thème sombre uniquement, en français.
Elle utilise le code Kotlin partagé (ViewModels, use cases, API YouTube) par la façade JS de
`shared/umbrella` (`jsMain`).

## Lancer

```bash
npm run kotlin   # produit la librairie Kotlin/JS (à refaire quand le code partagé change)
npm install
npm run dev      # http://localhost:5173
```

La connexion Google utilise l'identifiant client « Application Web » de `.env`
(`VITE_GOOGLE_CLIENT_ID`). Dans Google Cloud, l'origine JavaScript autorisée doit inclure
`http://localhost:5173` (et plus tard `https://rlebranchu.github.io`).

## Vérifier

```bash
npm run lint && npx tsc --noEmit
npm test         # Playwright (Chromium, Firefox, WebKit) sur données factices
```

Les tests simulent le réseau YouTube et Google Identity Services : aucun vrai compte Google.

## Session

Le jeton Google (environ 1 h) est conservé dans le `localStorage` avec sa date d'expiration, réutilisé
au rechargement, puis renouvelé en silence auprès de Google. Sans serveur, Google ne donne pas de
jeton de rafraîchissement : si le renouvellement silencieux est refusé, retour à la connexion.
