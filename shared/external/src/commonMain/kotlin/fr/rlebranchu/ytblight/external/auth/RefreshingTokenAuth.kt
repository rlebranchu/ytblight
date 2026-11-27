package fr.rlebranchu.ytblight.external.auth

import fr.rlebranchu.ytblight.local.token.StoredToken
import fr.rlebranchu.ytblight.local.token.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Connexion sans serveur : le jeton (valable environ 1 h) est enregistré avec sa date d'expiration,
 * réutilisé tant qu'il est valide, puis renouvelé en silence auprès de Google. Il n'existe pas de
 * jeton de rafraîchissement sans serveur : si Google refuse le renouvellement silencieux (session
 * Google fermée, navigateur restrictif), la session se termine et l'utilisateur se reconnecte.
 */
class RefreshingTokenAuth(
    private val requester: TokenRequester,
    private val storage: TokenStorage,
    private val clock: Clock = Clock.System,
    private val expiryMargin: Duration = DEFAULT_EXPIRY_MARGIN,
) : PlatformAuth {
    private val mutex = Mutex()

    override suspend fun currentToken(): String? =
        mutex.withLock {
            validStoredToken() ?: silentlyRenewedToken()
        }

    override suspend fun refreshToken(): String? =
        mutex.withLock {
            storage.clear()
            silentlyRenewedToken()
        }

    override suspend fun signIn(): PlatformSignInOutcome =
        mutex.withLock {
            try {
                store(
                    grant =
                        requester.request(
                            interactive = true,
                        ),
                )
                PlatformSignInOutcome.Authorized
            } catch (e: TokenRequestException) {
                PlatformSignInOutcome.Failed(
                    reason = e.reason,
                    detail = e.detail,
                )
            }
        }

    /** Sans objet sur le web : la fenêtre de Google est ouverte par [signIn], il n'y a pas d'écran à part. */
    override suspend fun completeConsent(
        response: Any?,
        isGranted: Boolean,
    ): PlatformSignInOutcome = signIn()

    override suspend fun signOut() {
        val token = storage.read()
        storage.clear()
        if (token != null) {
            try {
                requester.revoke(
                    accessToken = token.accessToken,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (
                // Hors ligne : la déconnexion locale suffit.
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // ignoré volontairement
            }
        }
    }

    private fun validStoredToken(): String? =
        storage
            .read()
            ?.takeIf { it.expiresAtEpochMillis - expiryMargin.inWholeMilliseconds > clock.now().toEpochMilliseconds() }
            ?.accessToken

    private suspend fun silentlyRenewedToken(): String? =
        try {
            store(
                grant =
                    requester.request(
                        interactive = false,
                    ),
            )
        } catch (e: TokenRequestException) {
            storage.clear()
            null
        }

    private fun store(
        grant: TokenGrant,
    ): String {
        storage.save(
            token =
                StoredToken(
                    accessToken = grant.accessToken,
                    expiresAtEpochMillis =
                        clock.now().toEpochMilliseconds() + grant.expiresInSeconds * MILLIS_PER_SECOND,
                ),
        )
        return grant.accessToken
    }

    private companion object {
        val DEFAULT_EXPIRY_MARGIN = 1.minutes
        const val MILLIS_PER_SECOND = 1_000L
    }
}
