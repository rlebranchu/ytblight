package fr.rlebranchu.ytblight.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.domain.error.DomainError
import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class FeedViewModel(
    private val getFeed: GetFeedUseCase,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FeedUiState(isLoading = true))
    val state: StateFlow<FeedUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    /** Action de l'utilisateur (tirer pour rafraîchir) : interroge à nouveau l'API. */
    fun refresh() = load(forceRefresh = true)

    /** Ré-essaie après une erreur, en repartant du cache s'il existe. */
    fun retry() = load(forceRefresh = false)

    private fun load(
        forceRefresh: Boolean,
    ) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                mutableState.update { current ->
                    current.copy(
                        isLoading = current.videos.isEmpty(),
                        isRefreshing = current.videos.isNotEmpty(),
                        error = null,
                    )
                }
                when (val result = getFeed(forceRefresh)) {
                    is DomainResult.Success ->
                        mutableState.value =
                            FeedUiState(videos = result.value.map { it.toItemUi(clock.now()) })
                    // On garde la liste déjà affichée : une erreur ne doit pas vider l'écran.
                    is DomainResult.Failure ->
                        mutableState.update {
                            it.copy(isLoading = false, isRefreshing = false, error = result.error.toFeedError())
                        }
                }
            }
    }
}

private fun DomainError.toFeedError(): FeedError =
    when (this) {
        DomainError.NotSignedIn -> FeedError.NOT_SIGNED_IN
        DomainError.QuotaExceeded -> FeedError.QUOTA_EXCEEDED
        DomainError.Network -> FeedError.NETWORK
        is DomainError.Unknown -> FeedError.UNKNOWN
    }
