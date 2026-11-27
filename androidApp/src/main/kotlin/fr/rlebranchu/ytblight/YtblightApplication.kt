package fr.rlebranchu.ytblight

import android.app.Application
import fr.rlebranchu.ytblight.data.di.dataModule
import fr.rlebranchu.ytblight.di.platformModule
import fr.rlebranchu.ytblight.external.di.externalModule
import fr.rlebranchu.ytblight.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YtblightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YtblightApplication)
            modules(
                platformModule,
                externalModule,
                dataModule,
                presentationModule,
            )
        }
    }
}
