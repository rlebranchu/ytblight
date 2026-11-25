package fr.rlebranchu.ytblight.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.session.SessionViewModel
import fr.rlebranchu.ytblight.umbrella.SharedDependencies

private const val ROUTE_FEED = "feed"
private const val ROUTE_PLAYER = "player/{videoId}"
private const val ARG_VIDEO_ID = "videoId"

/**
 * @param signInController fabrique du contrôleur de connexion : Play Services en production,
 * une fausse version dans les tests d'interface.
 */
@Composable
fun YtblightApp(
    dependencies: SharedDependencies,
    signInController: @Composable (onSignedIn: () -> Unit) -> SignInController,
) {
    val session: SessionViewModel =
        viewModel(
            factory = viewModelFactory { initializer { dependencies.sessionViewModel() } },
        )
    val authState by session.state.collectAsStateWithLifecycle()
    val signIn =
        signInController(
            session::onSignedIn,
        )

    when (authState) {
        AuthState.CHECKING ->
            Surface(Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        AuthState.SIGNED_OUT ->
            SignInScreen(
                controller = signIn,
            )
        AuthState.SIGNED_IN ->
            SignedInNavigation(
                dependencies = dependencies,
                onSignOut = session::signOut,
            )
    }
}

@Composable
private fun SignedInNavigation(
    dependencies: SharedDependencies,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = ROUTE_FEED) {
        composable(ROUTE_FEED) {
            val viewModel: FeedViewModel =
                viewModel(
                    factory = viewModelFactory { initializer { dependencies.feedViewModel() } },
                )
            val state by viewModel.state.collectAsStateWithLifecycle()
            FeedScreen(
                state = state,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::retry,
                onOpenVideo = { videoId -> navController.navigate("player/$videoId") },
                onSignOut = onSignOut,
                onReconnect = onSignOut,
            )
        }
        composable(ROUTE_PLAYER, arguments = listOf(navArgument(ARG_VIDEO_ID) { type = NavType.StringType })) { entry ->
            PlayerScreen(
                videoId = entry.arguments?.getString(ARG_VIDEO_ID).orEmpty(),
                onClose = { navController.popBackStack() },
            )
        }
    }
}
