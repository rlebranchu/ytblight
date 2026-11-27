package fr.rlebranchu.ytblight.data.auth

import fr.rlebranchu.ytblight.domain.auth.AuthRepository
import fr.rlebranchu.ytblight.domain.auth.AuthState
import fr.rlebranchu.ytblight.domain.auth.ConsentRequest
import fr.rlebranchu.ytblight.domain.auth.ConsentResponse
import fr.rlebranchu.ytblight.domain.auth.SignInError
import fr.rlebranchu.ytblight.domain.auth.SignInResult
import fr.rlebranchu.ytblight.external.auth.PlatformAuth
import fr.rlebranchu.ytblight.external.auth.PlatformSignInFailure
import fr.rlebranchu.ytblight.external.auth.PlatformSignInOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val platformAuth: PlatformAuth,
) : AuthRepository {
    private val mutableState =
        MutableStateFlow(
            value = AuthState.CHECKING,
        )
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    override suspend fun checkSession() {
        val hasToken =
            try {
                platformAuth.currentToken() != null
            } catch (e: CancellationException) {
                throw e
            } catch (
                // Plateforme indisponible, hors ligne… : on repasse par l'écran de connexion.
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                false
            }
        mutableState.value = if (hasToken) AuthState.SIGNED_IN else AuthState.SIGNED_OUT
    }

    override suspend fun signIn(): SignInResult =
        platformAuth
            .signIn()
            .toSignInResult()

    override suspend fun completeConsent(
        response: ConsentResponse,
    ): SignInResult =
        platformAuth
            .completeConsent(
                response = response.data,
                isGranted = response.isGranted,
            ).toSignInResult()

    private fun PlatformSignInOutcome.toSignInResult(): SignInResult =
        when (this) {
            PlatformSignInOutcome.Authorized -> {
                mutableState.value = AuthState.SIGNED_IN
                SignInResult.SignedIn
            }
            is PlatformSignInOutcome.ConsentRequired ->
                SignInResult.ConsentRequired(
                    request =
                        ConsentRequest(
                            handle = request,
                        ),
                )
            is PlatformSignInOutcome.Failed ->
                SignInResult.Failed(
                    error = reason.toSignInError(),
                    detail = detail,
                )
        }

    private fun PlatformSignInFailure.toSignInError(): SignInError =
        when (this) {
            PlatformSignInFailure.CANCELLED -> SignInError.CANCELLED
            PlatformSignInFailure.CONFIGURATION -> SignInError.CONFIGURATION
            PlatformSignInFailure.FAILED -> SignInError.FAILED
            PlatformSignInFailure.UNAVAILABLE -> SignInError.UNAVAILABLE
        }

    override suspend fun signOut() {
        try {
            platformAuth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (
            // Hors ligne : la déconnexion locale doit réussir malgré tout.
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // ignoré volontairement
        } finally {
            mutableState.value = AuthState.SIGNED_OUT
        }
    }
}
