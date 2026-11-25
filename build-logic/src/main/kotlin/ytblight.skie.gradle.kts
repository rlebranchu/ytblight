/*
 * SKIE (Touchlab) : expose Flow, suspend et les sealed classes de façon idiomatique en Swift.
 */
plugins {
    id("co.touchlab.skie")
}

skie {
    // Aucun suivi : on désactive l'envoi de statistiques d'usage.
    analytics {
        disableUpload.set(true)
        enabled.set(false)
    }
}
