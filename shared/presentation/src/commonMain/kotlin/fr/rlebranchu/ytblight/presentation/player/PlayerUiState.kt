package fr.rlebranchu.ytblight.presentation.player

data class PlayerUiState(
    val videoId: String,
    /** La vidéo est terminée : l'interface referme le lecteur. */
    val isEnded: Boolean = false,
)
