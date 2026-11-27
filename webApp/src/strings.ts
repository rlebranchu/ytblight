// Tous les textes de l'interface (français uniquement) : l'équivalent de strings.xml côté Android.
export const strings = {
  appName: 'ytblight',
  signIn: {
    message: 'Connecte-toi avec ton compte Google pour voir le fil de tes abonnements.',
    button: 'Se connecter avec Google',
    inProgress: 'Connexion en cours…',
    errorCancelled: 'Connexion annulée.',
    errorConfiguration:
      "Configuration Google incorrecte : vérifie l'origine JavaScript autorisée du client OAuth Web.",
    errorFailed: 'La connexion a échoué.',
    errorUnavailable: 'Connexion impossible.',
    errorUnavailableDetail: (detail: string) => `Connexion impossible (${detail}).`,
  },
  feed: {
    title: 'Abonnements',
    signOut: 'Se déconnecter',
    refresh: 'Actualiser',
    empty: 'Aucune vidéo récente.',
    reconnect: 'Se reconnecter',
    retry: 'Réessayer',
    errorNotSignedIn: 'Ta session a expiré.',
    errorQuotaExceeded: 'Le quota quotidien de YouTube est atteint. Réessaie demain.',
    errorNetwork: 'Pas de connexion. Vérifie ton réseau.',
    errorUnknown: 'Une erreur est survenue.',
    videoSubtitle: (params: { channel: string; published: string }) =>
      `${params.channel} · ${params.published}`,
  },
  player: {
    back: '← Retour',
    iframeTitle: 'Lecteur YouTube',
  },
  loading: 'Chargement…',
} as const
