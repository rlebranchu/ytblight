import { expect, test } from '@playwright/test'

import { fakeGoogle, fakeYouTube, googleLog, seedStoredToken } from './fakes'

const TOKEN_KEY = 'ytblight.token'

test('affiche la connexion, puis le fil sans Shorts du plus récent au plus ancien', async ({
  page,
}) => {
  await fakeGoogle({ page })
  await fakeYouTube({ page })
  await page.goto('/')

  await expect(page.getByText('Connecte-toi avec ton compte Google')).toBeVisible()
  await page.getByRole('button', { name: 'Se connecter avec Google' }).click()

  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()
  await expect(page.getByText('24:10')).toBeVisible()
  await expect(page.getByText('Chaîne Kotlin · il y a 5 jours').first()).toBeVisible()
  await expect(page.getByText('Un court extrait')).toHaveCount(0)

  const titles = await page.locator('li button span.line-clamp-2').allTextContents()
  expect(titles).toEqual([
    "Conférence d'une heure",
    "Visite guidée d'un atelier",
    'Un long format sur Kotlin',
  ])
  // Au démarrage, un renouvellement silencieux est tenté (refusé : aucune session) ; puis la fenêtre.
  expect((await googleLog({ page })).prompts).toEqual(['none', ''])
})

test('garde la session au rechargement : le jeton enregistré est réutilisé', async ({ page }) => {
  await fakeGoogle({ page })
  await fakeYouTube({ page })
  await page.goto('/')
  await page.getByRole('button', { name: 'Se connecter avec Google' }).click()
  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()

  await page.reload()

  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()
  // Rien n'a été redemandé à Google : le jeton enregistré était encore valide.
  expect((await googleLog({ page })).prompts).toEqual([])
})

test('renouvelle le jeton expiré en silence, sans fenêtre', async ({ page }) => {
  await fakeGoogle({ page, silent: 'granted' })
  await fakeYouTube({ page })
  await seedStoredToken({ page, token: 'ancien', expiresInMillis: -1000 })

  await page.goto('/')

  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()
  expect((await googleLog({ page })).prompts).toEqual(['none'])
  expect(await page.evaluate((key) => localStorage.getItem(key), TOKEN_KEY)).toBe('token-1')
})

test('revient à la connexion quand Google refuse le renouvellement silencieux', async ({
  page,
}) => {
  await fakeGoogle({ page, silent: 'refused' })
  await fakeYouTube({ page })
  await seedStoredToken({ page, token: 'ancien', expiresInMillis: -1000 })

  await page.goto('/')

  await expect(page.getByRole('button', { name: 'Se connecter avec Google' })).toBeVisible()
  expect(await page.evaluate((key) => localStorage.getItem(key), TOKEN_KEY)).toBeNull()
})

test("renouvelle le jeton et réessaie quand l'API répond 401", async ({ page }) => {
  await fakeGoogle({ page, silent: 'granted' })
  await fakeYouTube({ page, unauthorizedFirst: true })
  // Jeton encore valide pour l'application, mais refusé par YouTube (401).
  await seedStoredToken({ page, token: 'refusé', expiresInMillis: 30 * 60_000 })

  await page.goto('/')

  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()
  expect((await googleLog({ page })).prompts).toEqual(['none'])
  expect(await page.evaluate((key) => localStorage.getItem(key), TOKEN_KEY)).toBe('token-1')
})

test('se déconnecte : révoque le jeton, l’oublie et ne revient pas au fil avec Retour', async ({
  page,
}) => {
  await fakeGoogle({ page })
  await fakeYouTube({ page })
  await page.goto('/')
  await page.getByRole('button', { name: 'Se connecter avec Google' }).click()
  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()

  await page.getByRole('button', { name: 'Se déconnecter' }).click()

  await expect(page.getByRole('button', { name: 'Se connecter avec Google' })).toBeVisible()
  expect(await page.evaluate((key) => localStorage.getItem(key), TOKEN_KEY)).toBeNull()
  expect((await googleLog({ page })).revoked).toEqual(['token-1'])

  await page.goBack()
  await expect(page.getByText('Un long format sur Kotlin')).toHaveCount(0)
})

test('ouvre le lecteur YouTube intégré puis revient au fil', async ({ page }) => {
  await fakeGoogle({ page })
  await fakeYouTube({ page })
  await page.route('https://www.youtube.com/embed/**', (route) =>
    route.fulfill({ contentType: 'text/html', body: '<html><body>lecteur</body></html>' }),
  )
  await page.goto('/')
  await page.getByRole('button', { name: 'Se connecter avec Google' }).click()
  await page.getByText('Un long format sur Kotlin').click()

  const player = page.locator('iframe')
  await expect(player).toHaveAttribute('src', /youtube\.com\/embed\/V1\?autoplay=1/)
  await expect(player).toHaveAttribute('referrerpolicy', 'strict-origin-when-cross-origin')

  await page.getByRole('button', { name: '← Retour' }).click()
  await expect(page.getByText('Un long format sur Kotlin')).toBeVisible()
})
