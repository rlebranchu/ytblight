package fr.rlebranchu.ytblight.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.google.android.gms.common.api.ApiException
import fr.rlebranchu.ytblight.auth.GmsPlatformAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Ce que l'écran de connexion sait faire. Une fausse version sert aux tests d'interface. */
interface SignInController {
    val inProgress: Boolean
    val errorMessage: String?

    fun start()
}

@Composable
fun rememberGmsSignInController(
    auth: GmsPlatformAuth,
    onSignedIn: () -> Unit,
): SignInController {
    val scope = rememberCoroutineScope()
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val controller = remember(auth) { GmsSignInController(scope, auth) { currentOnSignedIn() } }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            controller.onConsentResult(result.resultCode == Activity.RESULT_OK, result.data)
        }
    controller.launchConsent = { request -> launcher.launch(request) }
    return controller
}

private class GmsSignInController(
    private val scope: CoroutineScope,
    private val auth: GmsPlatformAuth,
    private val onSignedIn: () -> Unit,
) : SignInController {
    override var inProgress by mutableStateOf(false)
        private set
    override var errorMessage by mutableStateOf<String?>(null)
        private set

    var launchConsent: (IntentSenderRequest) -> Unit = {}

    override fun start() {
        if (inProgress) return
        inProgress = true
        errorMessage = null
        scope.launch {
            try {
                val result = auth.authorize()
                val pendingIntent = result.pendingIntent
                if (result.hasResolution() && pendingIntent != null) {
                    launchConsent(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    finish(auth.accept(result) != null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                fail(e.toMessage())
            }
        }
    }

    fun onConsentResult(
        ok: Boolean,
        data: android.content.Intent?,
    ) {
        if (!ok) {
            fail("Connexion annulée.")
            return
        }
        val accepted =
            try {
                auth.accept(auth.resultFromIntent(data)) != null
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                fail(e.toMessage())
                return
            }
        finish(accepted)
    }

    private fun finish(
        signedIn: Boolean,
    ) {
        inProgress = false
        if (signedIn) onSignedIn() else errorMessage = "La connexion a échoué."
    }

    private fun fail(
        message: String,
    ) {
        inProgress = false
        errorMessage = message
    }

    private fun Exception.toMessage(): String =
        when {
            this is ApiException && statusCode == DEVELOPER_ERROR ->
                "Configuration Google incorrecte (code 10) : vérifie le package et l'empreinte SHA-1 du client Android."
            this is ApiException -> "Connexion impossible (code $statusCode)."
            else -> "Connexion impossible : ${message ?: javaClass.simpleName}."
        }

    private companion object {
        const val DEVELOPER_ERROR = 10
    }
}
