package fr.rlebranchu.ytblight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.rlebranchu.ytblight.ui.YtblightApp
import fr.rlebranchu.ytblight.ui.YtblightTheme
import fr.rlebranchu.ytblight.ui.signin.rememberGmsSignInStarter

class MainActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        // Thème sombre uniquement : icônes claires dans les barres système.
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.TRANSPARENT,
                ),
        )
        val app = application as YtblightApplication
        setContent {
            YtblightTheme {
                YtblightApp(
                    dependencies = app.dependencies,
                    signInStarter = { viewModel ->
                        rememberGmsSignInStarter(
                            auth = app.platformAuth,
                            viewModel = viewModel,
                        )
                    },
                )
            }
        }
    }
}
