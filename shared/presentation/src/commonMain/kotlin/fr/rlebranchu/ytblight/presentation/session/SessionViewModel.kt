package fr.rlebranchu.ytblight.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.SignOutUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** État de connexion partagé : l'interface choisit entre l'écran de connexion et le fil. */
class SessionViewModel(
    private val authRepository: AuthRepository,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    val state: StateFlow<AuthState> = authRepository.state

    init {
        viewModelScope.launch { authRepository.checkSession() }
    }

    /** À appeler quand la page de connexion native vient de réussir. */
    fun onSignedIn() = authRepository.onSignedIn()

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
