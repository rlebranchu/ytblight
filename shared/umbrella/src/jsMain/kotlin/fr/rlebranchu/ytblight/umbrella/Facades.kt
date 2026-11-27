package fr.rlebranchu.ytblight.umbrella

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.presentation.feed.FeedViewModel
import fr.rlebranchu.ytblight.presentation.player.PlayerViewModel
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.presentation.splash.SplashViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Les façades montrent les ViewModels partagés à TypeScript : `subscribe` renvoie la fonction qui
// arrête l'écoute, `clear` libère le ViewModel (à appeler au démontage de la page). Les états sont
// recopiés dans des classes simples, avec des tableaux JS et des chaînes à la place des listes
// et des énumérations Kotlin.

/** Écoute d'un état et libération du ViewModel, communes à toutes les façades. */
internal class FacadeScope(
    private val viewModel: ViewModel,
) {
    private val scope = MainScope()

    fun <T> collect(
        state: StateFlow<T>,
        onEach: (T) -> Unit,
    ): () -> Unit {
        val job =
            scope.launch {
                state.collect(onEach)
            }
        return { job.cancel() }
    }

    fun clear() {
        scope.cancel()
        viewModel.viewModelScope.cancel()
    }
}

// ── Démarrage ────────────────────────────────────────────────────────────────

@JsExport
class SplashStateJs(
    /** `"FEED"`, `"SIGN_IN"`, ou `null` tant que la session est vérifiée. */
    val destination: String?,
)

@JsExport
class SplashFacade internal constructor(
    private val viewModel: SplashViewModel,
) {
    private val facade = FacadeScope(viewModel = viewModel)

    fun subscribe(
        listener: (SplashStateJs) -> Unit,
    ): () -> Unit =
        facade.collect(state = viewModel.state) {
            listener(SplashStateJs(destination = it.destination?.name))
        }

    fun clear() = facade.clear()
}

// ── Connexion ────────────────────────────────────────────────────────────────

@JsExport
class SignInStateJs(
    val inProgress: Boolean,
    /** `"CANCELLED"`, `"CONFIGURATION"`, `"FAILED"`, `"UNAVAILABLE"`, ou `null`. */
    val error: String?,
    val errorDetail: String?,
    val isSignedIn: Boolean,
)

@JsExport
class SignInFacade internal constructor(
    private val viewModel: SignInViewModel,
) {
    private val facade = FacadeScope(viewModel = viewModel)

    fun subscribe(
        listener: (SignInStateJs) -> Unit,
    ): () -> Unit =
        facade.collect(state = viewModel.state) {
            listener(
                SignInStateJs(
                    inProgress = it.inProgress,
                    error = it.error?.name,
                    errorDetail = it.errorDetail,
                    isSignedIn = it.isSignedIn,
                ),
            )
        }

    /** À appeler directement depuis le clic de l'utilisateur : le navigateur bloque les fenêtres sinon. */
    fun signIn() = viewModel.onSignInClicked()

    fun clear() = facade.clear()
}

// ── Fil ──────────────────────────────────────────────────────────────────────

@JsExport
class FeedItemJs(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String?,
    val durationLabel: String,
    val publishedLabel: String,
)

@JsExport
class FeedStateJs(
    val videos: Array<FeedItemJs>,
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    /** `"NOT_SIGNED_IN"`, `"QUOTA_EXCEEDED"`, `"NETWORK"`, `"UNKNOWN"`, ou `null`. */
    val error: String?,
    val isSignedOut: Boolean,
)

@JsExport
class FeedFacade internal constructor(
    private val viewModel: FeedViewModel,
) {
    private val facade = FacadeScope(viewModel = viewModel)

    fun subscribe(
        listener: (FeedStateJs) -> Unit,
    ): () -> Unit =
        facade.collect(state = viewModel.state) { state ->
            listener(
                FeedStateJs(
                    videos =
                        state.videos
                            .map {
                                FeedItemJs(
                                    id = it.id,
                                    title = it.title,
                                    channelTitle = it.channelTitle,
                                    thumbnailUrl = it.thumbnailUrl,
                                    durationLabel = it.durationLabel,
                                    publishedLabel = it.publishedLabel,
                                )
                            }.toTypedArray(),
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    error = state.error?.name,
                    isSignedOut = state.isSignedOut,
                ),
            )
        }

    fun refresh() = viewModel.refresh()

    fun retry() = viewModel.retry()

    fun signOut() = viewModel.signOut()

    fun clear() = facade.clear()
}

// ── Lecteur ──────────────────────────────────────────────────────────────────

@JsExport
class PlayerStateJs(
    val videoId: String,
    val isEnded: Boolean,
)

@JsExport
class PlayerFacade internal constructor(
    private val viewModel: PlayerViewModel,
) {
    private val facade = FacadeScope(viewModel = viewModel)

    fun subscribe(
        listener: (PlayerStateJs) -> Unit,
    ): () -> Unit =
        facade.collect(state = viewModel.state) {
            listener(PlayerStateJs(videoId = it.videoId, isEnded = it.isEnded))
        }

    fun onVideoEnded() = viewModel.onVideoEnded()

    fun clear() = facade.clear()
}
