package fr.rlebranchu.ytblight.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.rlebranchu.ytblight.presentation.splash.SplashDestination
import fr.rlebranchu.ytblight.ui.feed.FeedRoute
import fr.rlebranchu.ytblight.ui.player.PlayerRoute
import fr.rlebranchu.ytblight.ui.signin.SignInRoute
import fr.rlebranchu.ytblight.ui.signin.SignInStarterFactory
import fr.rlebranchu.ytblight.ui.splash.SplashRoute
import fr.rlebranchu.ytblight.umbrella.SharedDependencies
import fr.rlebranchu.ytblight.umbrella.koinApplication
import org.koin.compose.KoinIsolatedContext

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_SIGN_IN = "signin"
private const val ROUTE_FEED = "feed"
private const val ROUTE_PLAYER = "player/{videoId}"
private const val ARG_VIDEO_ID = "videoId"

/**
 * Navigation de l'application. Chaque page a son [androidx.lifecycle.ViewModel], obtenu via Koin.
 *
 * Le conteneur Koin vient de [dependencies] et reste isolé : les tests d'interface lui fournissent
 * des données factices, sans jamais passer par un conteneur global ni par Google.
 *
 * @param signInStarter fabrique du démarreur de connexion : Play Services en production,
 * une fausse version dans les tests d'interface.
 */
@Composable
fun YtblightApp(
    dependencies: SharedDependencies,
    signInStarter: SignInStarterFactory,
) {
    KoinIsolatedContext(
        context = dependencies.koinApplication(),
    ) {
        val navController = rememberNavController()
        NavHost(navController, startDestination = ROUTE_SPLASH) {
            composable(ROUTE_SPLASH) {
                SplashRoute(
                    onNavigate = { destination ->
                        navController.navigateClearing(
                            route = destination.route(),
                            clearing = ROUTE_SPLASH,
                        )
                    },
                )
            }
            composable(ROUTE_SIGN_IN) {
                SignInRoute(
                    signInStarter = signInStarter,
                    onSignedIn = {
                        navController.navigateClearing(
                            route = ROUTE_FEED,
                            clearing = ROUTE_SIGN_IN,
                        )
                    },
                )
            }
            composable(ROUTE_FEED) {
                FeedRoute(
                    onOpenVideo = { videoId -> navController.navigate("player/$videoId") },
                    onSignedOut = {
                        navController.navigateClearing(
                            route = ROUTE_SIGN_IN,
                            clearing = ROUTE_FEED,
                        )
                    },
                )
            }
            composable(
                route = ROUTE_PLAYER,
                arguments = listOf(navArgument(ARG_VIDEO_ID) { type = NavType.StringType }),
            ) { entry ->
                PlayerRoute(
                    videoId = entry.arguments?.getString(ARG_VIDEO_ID).orEmpty(),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun SplashDestination.route(): String =
    when (this) {
        SplashDestination.SIGN_IN -> ROUTE_SIGN_IN
        SplashDestination.FEED -> ROUTE_FEED
    }

/** Va à [route] en retirant [clearing] de la pile : le bouton Retour ne revient pas en arrière. */
private fun NavController.navigateClearing(
    route: String,
    clearing: String,
) {
    navigate(route) {
        popUpTo(clearing) { inclusive = true }
    }
}
