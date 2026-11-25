package fr.rlebranchu.ytblight.presentation.feed

import fr.rlebranchu.ytblight.domain.model.Video

/** Ce que l'écran du fil affiche. Les interfaces n'ont aucune logique : elles montrent cet état. */
data class FeedUiState(
    val videos: List<Video> = emptyList(),
    /** Premier chargement : liste vide + indicateur. */
    val isLoading: Boolean = false,
    /** Rafraîchissement : la liste reste affichée pendant ce temps. */
    val isRefreshing: Boolean = false,
    val error: FeedError? = null,
)

/** Une énumération simple, directement utilisable en Swift et en TypeScript. */
enum class FeedError {
    NOT_SIGNED_IN,
    QUOTA_EXCEEDED,
    NETWORK,
    UNKNOWN,
}
