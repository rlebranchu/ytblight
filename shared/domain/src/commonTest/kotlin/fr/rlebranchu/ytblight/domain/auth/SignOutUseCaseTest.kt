package fr.rlebranchu.ytblight.domain.auth

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.feed.FeedRepository
import fr.rlebranchu.ytblight.domain.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignOutUseCaseTest {
    private val events = mutableListOf<String>()

    private val auth =
        object : AuthRepository {
            override val state: StateFlow<AuthState> = MutableStateFlow(AuthState.SIGNED_IN)

            override suspend fun checkSession() = Unit

            override suspend fun signIn(): SignInResult = SignInResult.SignedIn

            override suspend fun completeConsent(
                response: ConsentResponse,
            ): SignInResult = SignInResult.SignedIn

            override suspend fun signOut() {
                events += "auth.signOut"
            }
        }

    private val feed =
        object : FeedRepository {
            override suspend fun loadFeed(
                forceRefresh: Boolean,
            ): DomainResult<List<Video>> =
                DomainResult.Success(
                    value = emptyList(),
                )

            override suspend fun clear() {
                events += "feed.clear"
            }
        }

    @Test
    fun signsOutThenForgetsTheFeed() =
        runTest {
            SignOutUseCase(
                authRepository = auth,
                feedRepository = feed,
            )()

            assertEquals(listOf("auth.signOut", "feed.clear"), events)
        }
}
