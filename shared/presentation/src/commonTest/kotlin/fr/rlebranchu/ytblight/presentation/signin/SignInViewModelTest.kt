package fr.rlebranchu.ytblight.presentation.signin

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.CompleteConsentUseCase
import fr.rlebranchu.ytblight.domain.auth.ConsentRequest
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInResult
import fr.rlebranchu.ytblight.domain.auth.SignInUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import fr.rlebranchu.ytblight.domain.auth.SignInError as DomainSignInError

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private class FakeAuthRepository : AuthRepository {
        override val state: StateFlow<AuthState> = MutableStateFlow(AuthState.SIGNED_OUT)
        var signInResult: SignInResult = SignInResult.SignedIn
        var consentResult: SignInResult = SignInResult.SignedIn
        val consentResponses = mutableListOf<ConsentResponse>()

        override suspend fun checkSession() = Unit

        override suspend fun signIn(): SignInResult = signInResult

        override suspend fun completeConsent(
            response: ConsentResponse,
        ): SignInResult {
            consentResponses += response
            return consentResult
        }

        override suspend fun signOut() = Unit
    }

    private val auth = FakeAuthRepository()

    private fun viewModel() =
        SignInViewModel(
            signIn = SignInUseCase(authRepository = auth),
            completeConsent = CompleteConsentUseCase(authRepository = auth),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun leavesThePageWhenSignInSucceedsDirectly() {
        val viewModel = viewModel()

        viewModel.onSignInClicked()

        assertEquals(
            SignInUiState(
                isSignedIn = true,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun asksTheInterfaceForTheConsentWindowThenFinishesSignIn() {
        val request = ConsentRequest(handle = Any())
        auth.signInResult = SignInResult.ConsentRequired(request = request)
        val viewModel = viewModel()

        viewModel.onSignInClicked()

        assertEquals(
            SignInUiState(
                inProgress = true,
                consentRequest = request,
            ),
            viewModel.state.value,
        )

        viewModel.onConsentLaunched()
        viewModel.onConsentResult(
            response = ConsentResponse(isGranted = true, data = "intent"),
        )

        assertEquals(
            SignInUiState(
                isSignedIn = true,
            ),
            viewModel.state.value,
        )
        assertEquals("intent", auth.consentResponses.single().data)
    }

    @Test
    fun reportsACancelledConsentWithoutCallingTheDomain() {
        auth.signInResult = SignInResult.ConsentRequired(request = ConsentRequest(handle = Any()))
        val viewModel = viewModel()
        viewModel.onSignInClicked()
        viewModel.onConsentLaunched()

        viewModel.onConsentResult(
            response = ConsentResponse(isGranted = false, data = null),
        )

        assertEquals(
            SignInUiState(
                error = SignInError.CANCELLED,
            ),
            viewModel.state.value,
        )
        assertEquals(0, auth.consentResponses.size)
    }

    @Test
    fun showsTheFailureAndAllowsANewAttempt() {
        auth.signInResult = SignInResult.Failed(error = DomainSignInError.CONFIGURATION, detail = "10")
        val viewModel = viewModel()

        viewModel.onSignInClicked()

        assertEquals(
            SignInUiState(
                error = SignInError.CONFIGURATION,
                errorDetail = "10",
            ),
            viewModel.state.value,
        )

        auth.signInResult = SignInResult.SignedIn
        viewModel.onSignInClicked()

        assertEquals(
            SignInUiState(
                isSignedIn = true,
            ),
            viewModel.state.value,
        )
    }
}
