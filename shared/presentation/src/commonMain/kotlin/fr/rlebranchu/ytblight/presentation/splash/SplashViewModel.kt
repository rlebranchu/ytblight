package fr.rlebranchu.ytblight.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Page de démarrage : vérifie la session, puis indique à l'interface où aller. */
class SplashViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.checkSession()
            mutableState.value =
                SplashUiState(
                    destination =
                        when (authRepository.state.value) {
                            AuthState.SIGNED_IN -> SplashDestination.FEED
                            AuthState.CHECKING, AuthState.SIGNED_OUT -> SplashDestination.SIGN_IN
                        },
                )
        }
    }
}
