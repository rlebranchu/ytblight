package fr.rlebranchu.ytblight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.rlebranchu.ytblight.ui.YtblightApp
import fr.rlebranchu.ytblight.ui.YtblightTheme
import fr.rlebranchu.ytblight.ui.rememberGmsSignInController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Thème sombre uniquement : icônes claires dans les barres système.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = application as YtblightApplication
        setContent {
            YtblightTheme {
                YtblightApp(app.dependencies) { onSignedIn ->
                    rememberGmsSignInController(app.platformAuth, onSignedIn)
                }
            }
        }
    }
}
