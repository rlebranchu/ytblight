package fr.rlebranchu.ytblight.presentation.feed

import app.cash.turbine.test
import fr.rlebranchu.ytblight.domain.error.DomainError
import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.feed.GetFeedUseCase
import fr.rlebranchu.ytblight.domain.model.Video
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private class ScriptedRepository : FeedRepository {
        val responses = ArrayDeque<CompletableDeferred<DomainResult<List<Video>>>>()
        val requests = mutableListOf<Boolean>()

        fun enqueue(): CompletableDeferred<DomainResult<List<Video>>> =
            CompletableDeferred<DomainResult<List<Video>>>().also { responses.addLast(it) }

        override suspend fun loadFeed(forceRefresh: Boolean): DomainResult<List<Video>> {
            requests += forceRefresh
            return responses.removeFirst().await()
        }

        override suspend fun clear() = Unit
    }

    private val repository = ScriptedRepository()

    private fun video(id: String) =
        Video(id, "Titre $id", "UC1", "Chaîne", null, Instant.fromEpochMilliseconds(0), 10.minutes)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FeedViewModel(GetFeedUseCase(repository))

    @Test
    fun startsLoadingThenShowsTheVideos() =
        runTest {
            val response = repository.enqueue()
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(FeedUiState(isLoading = true), awaitItem())
                response.complete(DomainResult.Success(listOf(video("V1"))))
                assertEquals(FeedUiState(videos = listOf(video("V1"))), awaitItem())
            }
        }

    @Test
    fun showsAnErrorWhenTheFirstLoadFails() =
        runTest {
            repository.enqueue().complete(DomainResult.Failure(DomainError.QuotaExceeded))

            val state = viewModel().state.value

            assertEquals(FeedUiState(error = FeedError.QUOTA_EXCEEDED), state)
        }

    @Test
    fun mapsEveryDomainError() =
        runTest {
            val expected =
                mapOf(
                    DomainError.NotSignedIn to FeedError.NOT_SIGNED_IN,
                    DomainError.Network to FeedError.NETWORK,
                    DomainError.Unknown("x") to FeedError.UNKNOWN,
                )
            expected.forEach { (domainError, feedError) ->
                val scripted = ScriptedRepository()
                scripted.enqueue().complete(DomainResult.Failure(domainError))

                assertEquals(feedError, FeedViewModel(GetFeedUseCase(scripted)).state.value.error)
            }
        }

    @Test
    fun keepsTheListVisibleWhileRefreshing() =
        runTest {
            repository.enqueue().complete(DomainResult.Success(listOf(video("V1"))))
            val viewModel = viewModel()
            val refreshed = repository.enqueue()

            viewModel.refresh()

            assertEquals(
                FeedUiState(videos = listOf(video("V1")), isRefreshing = true),
                viewModel.state.value,
            )
            refreshed.complete(DomainResult.Success(listOf(video("V2"), video("V1"))))
            assertEquals(FeedUiState(videos = listOf(video("V2"), video("V1"))), viewModel.state.value)
            assertEquals(listOf(false, true), repository.requests)
        }

    @Test
    fun keepsTheListWhenARefreshFails() =
        runTest {
            repository.enqueue().complete(DomainResult.Success(listOf(video("V1"))))
            val viewModel = viewModel()
            repository.enqueue().complete(DomainResult.Failure(DomainError.Network))

            viewModel.refresh()

            assertEquals(
                FeedUiState(videos = listOf(video("V1")), error = FeedError.NETWORK),
                viewModel.state.value,
            )
        }

    @Test
    fun retryAsksTheRepositoryWithoutForcingARefresh() =
        runTest {
            repository.enqueue().complete(DomainResult.Failure(DomainError.Network))
            val viewModel = viewModel()
            repository.enqueue().complete(DomainResult.Success(listOf(video("V1"))))

            viewModel.retry()

            assertEquals(listOf(false, false), repository.requests)
            assertEquals(FeedUiState(videos = listOf(video("V1"))), viewModel.state.value)
        }

    @Test
    fun aNewRequestCancelsTheOneInFlight() =
        runTest {
            val slow = repository.enqueue()
            val viewModel = viewModel()
            repository.enqueue().complete(DomainResult.Success(listOf(video("NEW"))))

            viewModel.refresh()
            slow.complete(DomainResult.Success(listOf(video("OLD"))))

            assertEquals(
                listOf("NEW"),
                viewModel.state.value.videos
                    .map { it.id },
            )
        }
}
