package fr.rlebranchu.ytblight.domain.error

/** Résultat d'un traitement métier, plus lisible côté Swift et TypeScript que `kotlin.Result`. */
sealed interface DomainResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : DomainResult<T>

    data class Failure(
        val error: DomainError,
    ) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(
    transform: (T) -> R,
): DomainResult<R> =
    when (this) {
        is DomainResult.Success ->
            DomainResult.Success(
                value = transform(value),
            )
        is DomainResult.Failure -> this
    }
