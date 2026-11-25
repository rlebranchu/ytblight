package fr.rlebranchu.ytblight.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.rlebranchu.ytblight.presentation.signin.SignInError
import fr.rlebranchu.ytblight.presentation.signin.SignInUiState
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(
    signInStarter: SignInStarterFactory,
    onSignedIn: () -> Unit,
) {
    val viewModel: SignInViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val starter = signInStarter(viewModel)

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    SignInScreen(
        state = state,
        onSignIn = starter::start,
    )
}

@Composable
private fun SignInScreen(
    state: SignInUiState,
    onSignIn: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ytblight", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Connecte-toi avec ton compte Google pour voir le fil de tes abonnements.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (state.inProgress) {
                CircularProgressIndicator()
            } else {
                Button(onClick = onSignIn) { Text("Se connecter avec Google") }
            }
            state.error?.let {
                Text(
                    it.message(state.errorDetail),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun SignInError.message(
    detail: String?,
): String =
    when (this) {
        SignInError.CANCELLED -> "Connexion annulée."
        SignInError.CONFIGURATION ->
            "Configuration Google incorrecte (code 10) : vérifie le package et l'empreinte SHA-1 du client Android."
        SignInError.FAILED -> "La connexion a échoué."
        SignInError.UNAVAILABLE -> "Connexion impossible${detail?.let { " ($it)" }.orEmpty()}."
    }
