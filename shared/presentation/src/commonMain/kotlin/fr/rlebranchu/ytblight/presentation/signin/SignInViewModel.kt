package fr.rlebranchu.ytblight.presentation.signin

import androidx.lifecycle.ViewModel
import fr.rlebranchu.ytblight.domain.auth.CompleteSignInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Page de connexion. La fenêtre Google elle-même est propre à chaque plateforme : elle prévient
 * ce ViewModel du début et du résultat de la connexion.
 */
class SignInViewModel(
    private val completeSignIn: CompleteSignInUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = mutableState.asStateFlow()

    fun onStarted() {
        mutableState.update {
            it.copy(
                inProgress = true,
                error = null,
                errorDetail = null,
            )
        }
    }

    fun onSucceeded() {
        completeSignIn()
        mutableState.update {
            it.copy(
                inProgress = false,
                isSignedIn = true,
            )
        }
    }

    fun onFailed(
        error: SignInError,
        detail: String? = null,
    ) {
        mutableState.update {
            it.copy(
                inProgress = false,
                error = error,
                errorDetail = detail,
            )
        }
    }
}
