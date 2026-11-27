package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.external.auth.PlatformSignInFailure
import fr.rlebranchu.ytblight.external.auth.TokenGrant
import fr.rlebranchu.ytblight.external.auth.TokenRequestException
import fr.rlebranchu.ytblight.external.auth.TokenRequester
import kotlinx.coroutines.await

/** Demande les jetons à Google Identity Services, par l'intermédiaire du pont TypeScript. */
internal class GoogleTokenRequester(
    private val bridge: GoogleIdentityBridge,
) : TokenRequester {
    override suspend fun request(
        interactive: Boolean,
    ): TokenGrant {
        val response =
            bridge
                .requestToken(
                    interactive = interactive,
                ).await()
        val token = response.accessToken
        if (response.error != null || token == null) {
            throw TokenRequestException(
                reason = response.error.toFailure(),
                detail = response.error,
            )
        }
        return TokenGrant(
            accessToken = token,
            expiresInSeconds = response.expiresInSeconds?.toLong() ?: DEFAULT_EXPIRES_IN_SECONDS,
        )
    }

    override suspend fun revoke(
        accessToken: String,
    ) {
        bridge
            .revokeToken(
                accessToken = accessToken,
            ).await()
    }

    private fun String?.toFailure(): PlatformSignInFailure =
        when (this) {
            "popup_closed", "access_denied", "user_cancel" -> PlatformSignInFailure.CANCELLED
            "invalid_client", "redirect_uri_mismatch", "origin_mismatch" -> PlatformSignInFailure.CONFIGURATION
            else -> PlatformSignInFailure.UNAVAILABLE
        }

    private companion object {
        const val DEFAULT_EXPIRES_IN_SECONDS = 3_600L
    }
}
