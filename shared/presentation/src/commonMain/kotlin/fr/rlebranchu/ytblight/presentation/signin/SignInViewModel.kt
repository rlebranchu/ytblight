package fr.rlebranchu.ytblight.presentation.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.rlebranchu.ytblight.domain.auth.CompleteConsentUseCase
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInResult
import fr.rlebranchu.ytblight.domain.auth.SignInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import fr.rlebranchu.ytblight.domain.auth.SignInError as DomainSignInError

/**
 * Page de connexion. Pilote tout le flux : démarrage, fenêtre de consentement de la plateforme
 * (que l'interface affiche quand [SignInUiState.consentRequest] est renseigné), puis résultat.
 */
class SignInViewModel(
    private val signIn: SignInUseCase,
    private val completeConsent: CompleteConsentUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = mutableState.asStateFlow()

    fun onSignInClicked() {
        if (mutableState.value.inProgress) return
        mutableState.update {
            it.copy(
                inProgress = true,
                error = null,
                errorDetail = null,
            )
        }
        viewModelScope.launch {
            apply(
                result = signIn(),
            )
        }
    }

    /** L'interface a affiché la fenêtre de consentement : la demande ne doit plus être rejouée. */
    fun onConsentLaunched() {
        mutableState.update {
            it.copy(
                consentRequest = null,
            )
        }
    }

    /** L'interface transmet ce que la fenêtre de consentement a renvoyé. */
    fun onConsentResult(
        response: ConsentResponse,
    ) {
        if (!response.isGranted) {
            apply(
                result = SignInResult.Failed(error = DomainSignInError.CANCELLED),
            )
            return
        }
        viewModelScope.launch {
            apply(
                result =
                    completeConsent(
                        response = response,
                    ),
            )
        }
    }

    private fun apply(
        result: SignInResult,
    ) {
        mutableState.update {
            when (result) {
                SignInResult.SignedIn ->
                    it.copy(
                        inProgress = false,
                        isSignedIn = true,
                    )
                is SignInResult.ConsentRequired ->
                    it.copy(
                        consentRequest = result.request,
                    )
                is SignInResult.Failed ->
                    it.copy(
                        inProgress = false,
                        error = result.error.toUi(),
                        errorDetail = result.detail,
                    )
            }
        }
    }
}

private fun DomainSignInError.toUi(): SignInError =
    when (this) {
        DomainSignInError.CANCELLED -> SignInError.CANCELLED
        DomainSignInError.CONFIGURATION -> SignInError.CONFIGURATION
        DomainSignInError.FAILED -> SignInError.FAILED
        DomainSignInError.UNAVAILABLE -> SignInError.UNAVAILABLE
    }
