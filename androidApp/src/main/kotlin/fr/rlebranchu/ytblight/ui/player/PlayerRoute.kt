package fr.rlebranchu.ytblight.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Lecteur officiel YouTube (iframe). Seul écran où le paysage est autorisé.
 * Pas de lecture suivante : le lecteur se ferme à la fin de la vidéo (voir `PlayerViewModel`).
 */
@Composable
fun PlayerRoute(
    videoId: String,
    onClose: () -> Unit,
) {
    val viewModel: PlayerViewModel =
        koinViewModel(
            parameters = { parametersOf(videoId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isEnded) {
        if (state.isEnded) onClose()
    }

    PlayerScreen(
        videoId = state.videoId,
        onVideoEnded = viewModel::onVideoEnded,
        onClose = onClose,
    )
}

@Composable
private fun PlayerScreen(
    videoId: String,
    onVideoEnded: () -> Unit,
    onClose: () -> Unit,
) {
    val activity = LocalActivity.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)

    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    // En paysage, la vidéo prend tout l'écran : on masque les barres système.
    DisposableEffect(activity, isLandscape) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (isLandscape) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (!isLandscape) {
            TextButton(onClick = onClose, modifier = Modifier.statusBarsPadding()) { Text("← Retour") }
        }
        AndroidView(
            factory = { context ->
                YouTubePlayerView(context).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    val options =
                        IFramePlayerOptions
                            .Builder(context)
                            .controls(1)
                            .rel(0)
                            .build()
                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(
                                youTubePlayer: YouTubePlayer,
                            ) {
                                youTubePlayer.loadVideo(videoId, 0f)
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState,
                            ) {
                                if (state == PlayerConstants.PlayerState.ENDED) currentOnVideoEnded()
                            }
                        },
                        options,
                    )
                }
            },
            onRelease = { it.release() },
            modifier =
                if (isLandscape) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                },
        )
    }
}
