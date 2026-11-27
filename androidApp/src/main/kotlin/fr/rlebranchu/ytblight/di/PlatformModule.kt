package fr.rlebranchu.ytblight.di

import fr.rlebranchu.ytblight.auth.GmsPlatformAuth
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Koin : ce qui est propre à Android, ici la connexion Google via Play Services. */
val platformModule =
    module {
        single<PlatformAuth> {
            GmsPlatformAuth(
                context = androidContext(),
            )
        }
    }
