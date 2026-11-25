package fr.rlebranchu.ytblight.ui.signin

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.common.api.ApiException
import fr.rlebranchu.ytblight.auth.GmsPlatformAuth
import fr.rlebranchu.ytblight.presentation.signin.SignInError
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Ce que la page de connexion sait déclencher : ouvrir la fenêtre Google. Une fausse version sert aux tests. */
fun interface SignInStarter {
    fun start()
}

/** Fabrique du démarreur de connexion : Play Services en production, une fausse version dans les tests. */
typealias SignInStarterFactory = @Composable (viewModel: SignInViewModel) -> SignInStarter

/** Démarreur réel. La fenêtre Google (Android) reste ici ; le résultat est transmis au [SignInViewModel]. */
@Composable
fun rememberGmsSignInStarter(
    auth: GmsPlatformAuth,
    viewModel: SignInViewModel,
): SignInStarter {
    val scope = rememberCoroutineScope()
    val starter =
        remember(auth, viewModel) {
            GmsSignInStarter(
                scope = scope,
                auth = auth,
                viewModel = viewModel,
            )
        }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            starter.onConsentResult(
                ok = result.resultCode == Activity.RESULT_OK,
                data = result.data,
            )
        }
    starter.launchConsent = { request -> launcher.launch(request) }
    return starter
}

private class GmsSignInStarter(
    private val scope: CoroutineScope,
    private val auth: GmsPlatformAuth,
    private val viewModel: SignInViewModel,
) : SignInStarter {
    var launchConsent: (IntentSenderRequest) -> Unit = {}

    override fun start() {
        if (viewModel.state.value.inProgress) return
        viewModel.onStarted()
        scope.launch {
            try {
                val result = auth.authorize()
                val pendingIntent = result.pendingIntent
                if (result.hasResolution() && pendingIntent != null) {
                    launchConsent(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    finish(
                        accepted =
                            auth.accept(
                                result = result,
                            ) != null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                fail(
                    exception = e,
                )
            }
        }
    }

    fun onConsentResult(
        ok: Boolean,
        data: Intent?,
    ) {
        if (!ok) {
            viewModel.onFailed(
                error = SignInError.CANCELLED,
            )
            return
        }
        val accepted =
            try {
                auth.accept(
                    result =
                        auth.resultFromIntent(
                            data = data,
                        ),
                ) != null
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                fail(
                    exception = e,
                )
                return
            }
        finish(
            accepted = accepted,
        )
    }

    private fun finish(
        accepted: Boolean,
    ) {
        if (accepted) {
            viewModel.onSucceeded()
        } else {
            viewModel.onFailed(
                error = SignInError.FAILED,
            )
        }
    }

    private fun fail(
        exception: Exception,
    ) {
        when {
            exception is ApiException && exception.statusCode == DEVELOPER_ERROR ->
                viewModel.onFailed(
                    error = SignInError.CONFIGURATION,
                )
            exception is ApiException ->
                viewModel.onFailed(
                    error = SignInError.UNAVAILABLE,
                    detail = "code ${exception.statusCode}",
                )
            else ->
                viewModel.onFailed(
                    error = SignInError.UNAVAILABLE,
                    detail = exception.message ?: exception.javaClass.simpleName,
                )
        }
    }

    private companion object {
        const val DEVELOPER_ERROR = 10
    }
}
