package fr.rlebranchu.ytblight.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.CheckSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Page de démarrage : vérifie la session, puis indique à l'interface où aller. */
class SplashViewModel(
    private val checkSession: CheckSessionUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val authState = checkSession()
            mutableState.value =
                SplashUiState(
                    destination =
                        when (authState) {
                            AuthState.SIGNED_IN -> SplashDestination.FEED
                            AuthState.CHECKING, AuthState.SIGNED_OUT -> SplashDestination.SIGN_IN
                        },
                )
        }
    }
}
