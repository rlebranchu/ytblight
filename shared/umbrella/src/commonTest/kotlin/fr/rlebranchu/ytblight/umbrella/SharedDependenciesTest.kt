package fr.rlebranchu.ytblight.umbrella

import fr.rlebranchu.ytblight.presentation.feed.FeedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SharedDependenciesTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fakeFeedIsWiredEndToEndAndHidesShortsInNewestFirstOrder() =
        runTest {
            val state: FeedUiState =
                SharedDependencies
                    .fake()
                    .feedViewModel()
                    .state.value

            assertEquals(listOf("fake-4", "fake-3", "fake-1"), state.videos.map { it.id })
            assertNull(state.error)
            assertEquals(false, state.isLoading)
        }

    @Test
    fun eachViewModelIsANewInstance() {
        val dependencies = SharedDependencies.fake()

        assertEquals(false, dependencies.feedViewModel() === dependencies.feedViewModel())
    }
}
