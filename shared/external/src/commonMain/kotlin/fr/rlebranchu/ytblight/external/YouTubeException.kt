package fr.rlebranchu.ytblight.external

/** Erreurs de l'API YouTube, indépendantes de Ktor pour que `data` n'en dépende pas. */
sealed class YouTubeException(
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause)

/** L'API a répondu avec un code d'erreur HTTP. */
class YouTubeHttpException(
    val statusCode: Int,
    /** Raison fournie par Google, par exemple `quotaExceeded`. */
    val reason: String?,
    message: String?,
) : YouTubeException("HTTP $statusCode${reason?.let { " ($it)" }.orEmpty()} ${message.orEmpty()}".trim())

/** Le réseau a échoué avant d'obtenir une réponse. */
class YouTubeNetworkException(
    cause: Throwable,
) : YouTubeException(cause.message, cause)
