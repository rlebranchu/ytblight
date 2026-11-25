package fr.rlebranchu.ytblight.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
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
    suspend fun authorize(): AuthorizationResult {
        val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(scopes)
                .apply { savedAccount()?.let { setAccount(it) } }
                .build()
        return client.authorize(request).await()
    }

    /** Résultat de l'écran de consentement affiché par Play Services. */
    fun resultFromIntent(
        data: Intent?,
    ): AuthorizationResult = client.getAuthorizationResultFromIntent(data)

    /** Retient le compte et le jeton d'un résultat réussi. Renvoie le jeton, ou `null` s'il manque. */
    fun accept(
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

    override suspend fun currentToken(): String? = accept(authorize())

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
                runCatching { revokeToken(token) }
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
                "https://oauth2.googleapis.com/revoke?token=" + URLEncoder.encode(token, "UTF-8"),
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
        const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val PREFERENCES_NAME = "ytblight_auth"
        const val KEY_ACCOUNT_EMAIL = "account_email"
    }
}
