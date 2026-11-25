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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private class ScriptedRepository : FeedRepository {
        val responses = ArrayDeque<CompletableDeferred<DomainResult<List<Video>>>>()
        val requests = mutableListOf<Boolean>()

        fun enqueue(): CompletableDeferred<DomainResult<List<Video>>> =
            CompletableDeferred<DomainResult<List<Video>>>().also { responses.addLast(it) }

        override suspend fun loadFeed(
            forceRefresh: Boolean,
        ): DomainResult<List<Video>> {
            requests += forceRefresh
            return responses.removeFirst().await()
        }

        override suspend fun clear() = Unit
    }

    private val repository = ScriptedRepository()

    private val fixedClock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(0) + 3.days
        }

    private fun item(
        id: String,
    ) = FeedItemUi(
        id = id,
        title = "Titre $id",
        channelTitle = "Chaîne",
        thumbnailUrl = null,
        durationLabel = "10:00",
        publishedLabel = "il y a 3 jours",
    )

    private fun video(
        id: String,
    ) = Video(
        id = id,
        title = "Titre $id",
        channelId = "UC1",
        channelTitle = "Chaîne",
        thumbnailUrl = null,
        publishedAt = Instant.fromEpochMilliseconds(0),
        duration = 10.minutes,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FeedViewModel(getFeed = GetFeedUseCase(repository = repository), clock = fixedClock)

    @Test
    fun startsLoadingThenShowsTheVideos() =
        runTest {
            val response = repository.enqueue()
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(
                    FeedUiState(
                        isLoading = true,
                    ),
                    awaitItem(),
                )
                response.complete(
                    DomainResult.Success(
                        value =
                            listOf(
                                video(
                                    id = "V1",
                                ),
                            ),
                    ),
                )
                assertEquals(
                    FeedUiState(
                        videos =
                            listOf(
                                item(
                                    id = "V1",
                                ),
                            ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun showsAnErrorWhenTheFirstLoadFails() =
        runTest {
            repository.enqueue().complete(
                DomainResult.Failure(
                    error = DomainError.QuotaExceeded,
                ),
            )

            val state = viewModel().state.value

            assertEquals(
                FeedUiState(
                    error = FeedError.QUOTA_EXCEEDED,
                ),
                state,
            )
        }

    @Test
    fun mapsEveryDomainError() =
        runTest {
            val expected =
                mapOf(
                    DomainError.NotSignedIn to FeedError.NOT_SIGNED_IN,
                    DomainError.Network to FeedError.NETWORK,
                    DomainError.Unknown(
                        message = "x",
                    ) to FeedError.UNKNOWN,
                )
            expected.forEach { (domainError, feedError) ->
                val scripted = ScriptedRepository()
                scripted.enqueue().complete(
                    DomainResult.Failure(
                        error = domainError,
                    ),
                )

                assertEquals(
                    feedError,
                    FeedViewModel(
                        getFeed =
                            GetFeedUseCase(
                                repository = scripted,
                            ),
                        clock = fixedClock,
                    ).state.value.error,
                )
            }
        }

    @Test
    fun keepsTheListVisibleWhileRefreshing() =
        runTest {
            repository.enqueue().complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "V1",
                            ),
                        ),
                ),
            )
            val viewModel = viewModel()
            val refreshed = repository.enqueue()

            viewModel.refresh()

            assertEquals(
                FeedUiState(
                    videos =
                        listOf(
                            item(
                                id = "V1",
                            ),
                        ),
                    isRefreshing = true,
                ),
                viewModel.state.value,
            )
            refreshed.complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "V2",
                            ),
                            video(id = "V1"),
                        ),
                ),
            )
            assertEquals(
                FeedUiState(
                    videos =
                        listOf(
                            item(
                                id = "V2",
                            ),
                            item(id = "V1"),
                        ),
                ),
                viewModel.state.value,
            )
            assertEquals(listOf(false, true), repository.requests)
        }

    @Test
    fun keepsTheListWhenARefreshFails() =
        runTest {
            repository.enqueue().complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "V1",
                            ),
                        ),
                ),
            )
            val viewModel = viewModel()
            repository.enqueue().complete(
                DomainResult.Failure(
                    error = DomainError.Network,
                ),
            )

            viewModel.refresh()

            assertEquals(
                FeedUiState(
                    videos =
                        listOf(
                            item(
                                id = "V1",
                            ),
                        ),
                    error = FeedError.NETWORK,
                ),
                viewModel.state.value,
            )
        }

    @Test
    fun retryAsksTheRepositoryWithoutForcingARefresh() =
        runTest {
            repository.enqueue().complete(
                DomainResult.Failure(
                    error = DomainError.Network,
                ),
            )
            val viewModel = viewModel()
            repository.enqueue().complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "V1",
                            ),
                        ),
                ),
            )

            viewModel.retry()

            assertEquals(listOf(false, false), repository.requests)
            assertEquals(
                FeedUiState(
                    videos =
                        listOf(
                            item(
                                id = "V1",
                            ),
                        ),
                ),
                viewModel.state.value,
            )
        }

    @Test
    fun aNewRequestCancelsTheOneInFlight() =
        runTest {
            val slow = repository.enqueue()
            val viewModel = viewModel()
            repository.enqueue().complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "NEW",
                            ),
                        ),
                ),
            )

            viewModel.refresh()
            slow.complete(
                DomainResult.Success(
                    value =
                        listOf(
                            video(
                                id = "OLD",
                            ),
                        ),
                ),
            )

            assertEquals(
                listOf("NEW"),
                viewModel.state.value.videos
                    .map { it.id },
            )
        }
}
