package fr.rlebranchu.ytblight.data.feed

import fr.rlebranchu.ytblight.domain.error.DomainError
import fr.rlebranchu.ytblight.external.YouTubeHttpException
import fr.rlebranchu.ytblight.external.YouTubeNetworkException

private val QUOTA_REASONS =
    setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded", "userRateLimitExceeded")

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

internal fun Throwable.toDomainError(): DomainError =
    when (this) {
        is YouTubeHttpException ->
            when {
                statusCode == HTTP_UNAUTHORIZED -> DomainError.NotSignedIn
                statusCode == HTTP_FORBIDDEN && reason in QUOTA_REASONS -> DomainError.QuotaExceeded
                else ->
                    DomainError.Unknown(
                        message = message,
                    )
            }
        is YouTubeNetworkException -> DomainError.Network
        else ->
            DomainError.Unknown(
                message = message,
            )
    }
