package fr.rlebranchu.ytblight.presentation.player

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerViewModelTest {
    @Test
    fun startsWithTheRequestedVideo() {
        val viewModel =
            PlayerViewModel(
                videoId = "abc",
            )

        assertEquals(PlayerUiState(videoId = "abc"), viewModel.state.value)
    }

    @Test
    fun marksTheVideoAsEndedSoThePageCloses() {
        val viewModel =
            PlayerViewModel(
                videoId = "abc",
            )

        viewModel.onVideoEnded()

        assertEquals(PlayerUiState(videoId = "abc", isEnded = true), viewModel.state.value)
    }
}
