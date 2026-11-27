package fr.rlebranchu.ytblight.ui.signin

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.rlebranchu.ytblight.R
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.presentation.signin.SignInUiError
import fr.rlebranchu.ytblight.presentation.signin.SignInUiState
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
) {
    val viewModel: SignInViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Seul rôle Android : afficher la fenêtre de consentement de Play Services et rendre son résultat brut.
    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onConsentResult(
                response =
                    ConsentResponse(
                        isGranted = result.resultCode == Activity.RESULT_OK,
                        data = result.data,
                    ),
            )
        }

    LaunchedEffect(state.consentRequest) {
        state.consentRequest?.let { request ->
            consentLauncher.launch(IntentSenderRequest.Builder(request.handle as IntentSender).build())
            viewModel.onConsentLaunched()
        }
    }
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    SignInScreen(
        state = state,
        onSignIn = viewModel::onSignInClicked,
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
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.signin_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (state.inProgress) {
                CircularProgressIndicator()
            } else {
                Button(onClick = onSignIn) { Text(stringResource(R.string.signin_button)) }
            }
            state.error?.let {
                Text(
                    it.message(
                        detail = state.errorDetail,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SignInUiError.message(
    detail: String?,
): String =
    stringResource(
        when (this) {
            SignInUiError.CANCELLED -> R.string.signin_error_cancelled
            SignInUiError.CONFIGURATION -> R.string.signin_error_configuration
            SignInUiError.FAILED -> R.string.signin_error_failed
            SignInUiError.UNAVAILABLE ->
                if (detail == null) {
                    R.string.signin_error_unavailable
                } else {
                    R.string.signin_error_unavailable_detail
                }
        },
        // Ignoré par les textes sans emplacement (%1$s).
        detail.orEmpty(),
    )
