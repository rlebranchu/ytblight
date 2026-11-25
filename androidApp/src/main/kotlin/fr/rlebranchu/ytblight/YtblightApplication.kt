package fr.rlebranchu.ytblight

import android.app.Application
import fr.rlebranchu.ytblight.auth.GmsPlatformAuth
import fr.rlebranchu.ytblight.umbrella.SharedDependencies

class YtblightApplication : Application() {
    val platformAuth: GmsPlatformAuth by lazy { GmsPlatformAuth(this) }
    val dependencies: SharedDependencies by lazy { SharedDependencies.create(platformAuth) }
}
