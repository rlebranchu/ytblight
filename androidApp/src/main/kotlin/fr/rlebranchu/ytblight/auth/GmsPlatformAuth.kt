package fr.rlebranchu.ytblight.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.PlatformSignInFailure
import fr.rlebranchu.ytblight.external.auth.PlatformSignInOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Connexion Google via l'API d'autorisation de Play Services.
 *
 * Aucun jeton n'est stocké par l'application : Play Services les gère et en renvoie un neuf
 * (silencieusement) tant que l'utilisateur a accepté le scope. Seule l'adresse du compte choisi
 * est mémorisée, pour ne pas redemander quel compte utiliser.
 */
class GmsPlatformAuth(
    context: Context,
) : PlatformAuth {
    private val appContext = context.applicationContext
    private val client = Identity.getAuthorizationClient(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var lastToken: String? = null

    private val scopes = listOf(Scope(YOUTUBE_READONLY_SCOPE))

    private fun savedAccount(): Account? =
        preferences.getString(KEY_ACCOUNT_EMAIL, null)?.let { Account(it, GOOGLE_ACCOUNT_TYPE) }

    /** Demande l'autorisation. Le résultat peut exiger l'affichage d'un écran (`hasResolution`). */
    private suspend fun authorize(): AuthorizationResult {
        val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(scopes)
                .apply { savedAccount()?.let { setAccount(it) } }
                .build()
        return client.authorize(request).await()
    }

    /** Retient le compte et le jeton d'un résultat réussi. Renvoie le jeton, ou `null` s'il manque. */
    private fun accept(
        result: AuthorizationResult,
    ): String? {
        if (result.hasResolution()) return null
        val token = result.accessToken ?: return null
        lastToken = token
        result.toGoogleSignInAccount()?.email?.let { email ->
            preferences.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
        }
        return token
    }

    override suspend fun signIn(): PlatformSignInOutcome =
        guarded {
            outcomeOf(
                result = authorize(),
            )
        }

    /** [response] est l'`Intent` renvoyé par la fenêtre de consentement de Play Services. */
    override suspend fun completeConsent(
        response: Any?,
        isGranted: Boolean,
    ): PlatformSignInOutcome {
        if (!isGranted) {
            return PlatformSignInOutcome.Failed(
                reason = PlatformSignInFailure.CANCELLED,
            )
        }
        return guarded {
            outcomeOf(
                result = client.getAuthorizationResultFromIntent(response as Intent?),
            )
        }
    }

    private fun outcomeOf(
        result: AuthorizationResult,
    ): PlatformSignInOutcome {
        val pendingIntent = result.pendingIntent
        return when {
            result.hasResolution() && pendingIntent != null ->
                PlatformSignInOutcome.ConsentRequired(
                    request = pendingIntent.intentSender,
                )
            accept(result) != null -> PlatformSignInOutcome.Authorized
            else ->
                PlatformSignInOutcome.Failed(
                    reason = PlatformSignInFailure.FAILED,
                )
        }
    }

    /** Traduit les exceptions de Play Services en échecs lisibles par le code partagé. */
    private suspend fun guarded(
        block: suspend () -> PlatformSignInOutcome,
    ): PlatformSignInOutcome =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            when {
                e is ApiException && e.statusCode == DEVELOPER_ERROR ->
                    PlatformSignInOutcome.Failed(
                        reason = PlatformSignInFailure.CONFIGURATION,
                    )
                e is ApiException ->
                    PlatformSignInOutcome.Failed(
                        reason = PlatformSignInFailure.UNAVAILABLE,
                        detail = "code ${e.statusCode}",
                    )
                else ->
                    PlatformSignInOutcome.Failed(
                        reason = PlatformSignInFailure.UNAVAILABLE,
                        detail = e.message ?: e.javaClass.simpleName,
                    )
            }
        }

    override suspend fun currentToken(): String? =
        accept(
            result = authorize(),
        )

    /** Le jeton a été refusé par l'API : on l'invalide dans le cache de Play Services, puis on en redemande un. */
    override suspend fun refreshToken(): String? {
        lastToken?.let { stale ->
            withContext(Dispatchers.IO) { runCatching { GoogleAuthUtil.clearToken(appContext, stale) } }
        }
        lastToken = null
        return currentToken()
    }

    override suspend fun signOut() {
        val account = savedAccount()
        lastToken?.let { token ->
            withContext(Dispatchers.IO) {
                // Retire l'autorisation côté Google même si l'adresse du compte n'a pas pu être retenue.
                runCatching {
                    revokeToken(
                        token = token,
                    )
                }
                runCatching { GoogleAuthUtil.clearToken(appContext, token) }
            }
        }
        lastToken = null
        preferences.edit().clear().apply()
        if (account != null) {
            // Retire aussi l'autorisation côté Google. Hors ligne, la déconnexion locale suffit.
            val revoke =
                RevokeAccessRequest
                    .builder()
                    .setAccount(account)
                    .setScopes(scopes)
                    .build()
            runCatching { client.revokeAccess(revoke).await() }
        }
    }

    private fun revokeToken(
        token: String,
    ) {
        val connection =
            URL(
                "https://oauth2.googleapis.com/revoke?token=" +
                    URLEncoder.encode(token, "UTF-8"),
            ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = REVOKE_TIMEOUT_MS
            connection.readTimeout = REVOKE_TIMEOUT_MS
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val REVOKE_TIMEOUT_MS = 5_000
        const val DEVELOPER_ERROR = 10
        const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val PREFERENCES_NAME = "ytblight_auth"
        const val KEY_ACCOUNT_EMAIL = "account_email"
    }
}
