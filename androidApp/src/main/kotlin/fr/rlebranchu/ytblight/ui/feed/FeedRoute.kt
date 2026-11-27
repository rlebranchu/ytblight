package fr.rlebranchu.ytblight.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.rlebranchu.ytblight.R
import fr.rlebranchu.ytblight.presentation.feed.FeedError
import fr.rlebranchu.ytblight.presentation.feed.FeedItemUi
import fr.rlebranchu.ytblight.presentation.feed.FeedUiState
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedRoute(
    onOpenVideo: (String) -> Unit,
    onSignedOut: () -> Unit,
) {
    val viewModel: FeedViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedOut) {
        if (state.isSignedOut) onSignedOut()
    }

    FeedScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onOpenVideo = onOpenVideo,
        onSignOut = viewModel::signOut,
        onReconnect = viewModel::signOut,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    state: FeedUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onSignOut: () -> Unit,
    onReconnect: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed_title)) },
                actions = { TextButton(onClick = onSignOut) { Text(stringResource(R.string.feed_sign_out)) } },
            )
        },
    ) { padding ->
        val error = state.error
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            when {
                state.isLoading -> Loading()
                state.videos.isEmpty() && error != null ->
                    ErrorPanel(
                        error = error,
                        onRetry = onRetry,
                        onReconnect = onReconnect,
                    )
                state.videos.isEmpty() -> EmptyPanel()
                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        error?.let {
                            item {
                                ErrorBanner(
                                    error = it,
                                    onRetry = onRetry,
                                    onReconnect = onReconnect,
                                )
                            }
                        }
                        items(state.videos, key = { it.id }) { video ->
                            VideoRow(
                                video = video,
                                onClick = { onOpenVideo(video.id) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun EmptyPanel() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(32.dp)) {
        item {
            Text(
                stringResource(R.string.feed_empty),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    error: FeedError,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(error.message(), textAlign = TextAlign.Center)
        ErrorAction(
            error = error,
            onRetry = onRetry,
            onReconnect = onReconnect,
        )
    }
}

@Composable
private fun ErrorBanner(
    error: FeedError,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(error.message(), color = MaterialTheme.colorScheme.onErrorContainer)
        ErrorAction(
            error = error,
            onRetry = onRetry,
            onReconnect = onReconnect,
        )
    }
}

@Composable
private fun ErrorAction(
    error: FeedError,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
) {
    when (error) {
        FeedError.NOT_SIGNED_IN -> Button(onClick = onReconnect) { Text(stringResource(R.string.feed_reconnect)) }
        FeedError.QUOTA_EXCEEDED -> Unit
        FeedError.NETWORK, FeedError.UNKNOWN -> Button(onClick = onRetry) { Text(stringResource(R.string.feed_retry)) }
    }
}

@Composable
private fun FeedError.message(): String =
    stringResource(
        when (this) {
            FeedError.NOT_SIGNED_IN -> R.string.feed_error_not_signed_in
            FeedError.QUOTA_EXCEEDED -> R.string.feed_error_quota_exceeded
            FeedError.NETWORK -> R.string.feed_error_network
            FeedError.UNKNOWN -> R.string.feed_error_unknown
        },
    )

@Composable
private fun VideoRow(
    video: FeedItemUi,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text(
                video.durationLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(
                                alpha = 0.8f,
                            ),
                            RoundedCornerShape(4.dp),
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        Text(
            stringResource(R.string.feed_video_subtitle, video.channelTitle, video.publishedLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
