package fr.rlebranchu.ytblight.presentation.player

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Page du lecteur : pas de lecture suivante, la page se ferme à la fin de la vidéo. */
class PlayerViewModel(
    videoId: String,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            PlayerUiState(
                videoId = videoId,
            ),
        )
    val state: StateFlow<PlayerUiState> = mutableState.asStateFlow()

    fun onVideoEnded() {
        mutableState.update {
            it.copy(
                isEnded = true,
            )
        }
    }
}
