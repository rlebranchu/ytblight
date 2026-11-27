package fr.rlebranchu.ytblight.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.rlebranchu.ytblight.presentation.splash.SplashDestination
import fr.rlebranchu.ytblight.ui.feed.FeedRoute
import fr.rlebranchu.ytblight.ui.player.PlayerRoute
import fr.rlebranchu.ytblight.ui.signin.SignInRoute
import fr.rlebranchu.ytblight.ui.splash.SplashRoute

/**
 * Navigation de l'application (type-safe, voir [NavRoute]). Chaque page a son
 * [androidx.lifecycle.ViewModel], obtenu via Koin.
 *
 * Koin est démarré dans `YtblightApplication`. Les tests d'interface y surchargent les repositories
 * par des données factices (`loadKoinModules`), sans jamais passer par Google.
 */
@Composable
fun YtblightApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoute.Splash,
    ) {
        composable<NavRoute.Splash> {
            SplashRoute(
                onNavigate = { destination ->
                    navController.navigateClearing(
                        route = destination.route(),
                        clearing = NavRoute.Splash,
                    )
                },
            )
        }
        composable<NavRoute.SignIn> {
            SignInRoute(
                onSignedIn = {
                    navController.navigateClearing(
                        route = NavRoute.Feed,
                        clearing = NavRoute.SignIn,
                    )
                },
            )
        }
        composable<NavRoute.Feed> {
            FeedRoute(
                onOpenVideo = { videoId -> navController.navigate(NavRoute.Player(videoId = videoId)) },
                onSignedOut = {
                    navController.navigateClearing(
                        route = NavRoute.SignIn,
                        clearing = NavRoute.Feed,
                    )
                },
            )
        }
        composable<NavRoute.Player> { entry ->
            PlayerRoute(
                videoId = entry.toRoute<NavRoute.Player>().videoId,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

private fun SplashDestination.route(): NavRoute =
    when (this) {
        SplashDestination.SIGN_IN -> NavRoute.SignIn
        SplashDestination.FEED -> NavRoute.Feed
    }

/** Va à [route] en retirant [clearing] de la pile : le bouton Retour ne revient pas en arrière. */
private fun NavController.navigateClearing(
    route: NavRoute,
    clearing: NavRoute,
) {
    navigate(route) {
        popUpTo(clearing) { inclusive = true }
    }
}
