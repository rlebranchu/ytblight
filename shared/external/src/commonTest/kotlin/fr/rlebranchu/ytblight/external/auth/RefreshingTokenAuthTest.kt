package fr.rlebranchu.ytblight.external.auth

import fr.rlebranchu.ytblight.local.token.StoredToken
import fr.rlebranchu.ytblight.local.token.TokenStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class RefreshingTokenAuthTest {
    private class InMemoryTokenStorage(
        var token: StoredToken? = null,
    ) : TokenStorage {
        override fun read(): StoredToken? = token

        override fun save(
            token: StoredToken,
        ) {
            this.token = token
        }

        override fun clear() {
            token = null
        }
    }

    private class FakeRequester : TokenRequester {
        val requests = mutableListOf<Boolean>()
        val revoked = mutableListOf<String>()
        var next: TokenResult =
            TokenResult.Granted(grant = TokenGrant(accessToken = "neuf", expiresInSeconds = 3_600))
        var revokeFails = false

        override suspend fun request(
            interactive: Boolean,
        ): TokenResult {
            requests += interactive
            return next
        }

        override suspend fun revoke(
            accessToken: String,
        ) {
            if (revokeFails) error("hors ligne")
            revoked += accessToken
        }
    }

    private val nowMillis = 1_800_000_000_000L
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
        }
    private val requester = FakeRequester()
    private val storage = InMemoryTokenStorage()
    private val auth =
        RefreshingTokenAuth(
            requester = requester,
            storage = storage,
            clock = clock,
        )

    private fun stored(
        token: String,
        expiresInMillis: Long,
    ) = StoredToken(accessToken = token, expiresAtEpochMillis = nowMillis + expiresInMillis)

    @Test
    fun reusesAValidStoredTokenWithoutAskingGoogle() =
        runTest {
            storage.token = stored(token = "ancien", expiresInMillis = 30 * 60_000)

            assertEquals("ancien", auth.currentToken())
            assertEquals(emptyList(), requester.requests)
        }

    @Test
    fun renewsSilentlyWhenTheStoredTokenIsAboutToExpire() =
        runTest {
            storage.token = stored(token = "ancien", expiresInMillis = 30_000)

            assertEquals("neuf", auth.currentToken())
            assertEquals(listOf(false), requester.requests)
            assertEquals(nowMillis + 3_600_000, storage.token?.expiresAtEpochMillis)
        }

    @Test
    fun asksSilentlyWhenNothingIsStored() =
        runTest {
            assertEquals("neuf", auth.currentToken())
            assertEquals(listOf(false), requester.requests)
        }

    @Test
    fun endsTheSessionWhenGoogleRefusesTheSilentRenewal() =
        runTest {
            storage.token = stored(token = "ancien", expiresInMillis = -1)
            requester.next = TokenResult.Denied(reason = PlatformSignInFailure.UNAVAILABLE)

            assertNull(auth.currentToken())
            assertNull(storage.token)
        }

    @Test
    fun refreshDiscardsTheRejectedTokenAndRenewsIt() =
        runTest {
            storage.token = stored(token = "refusé", expiresInMillis = 30 * 60_000)

            assertEquals("neuf", auth.refreshToken())
            assertEquals("neuf", storage.token?.accessToken)
            assertEquals(listOf(false), requester.requests)
        }

    @Test
    fun signInAsksInteractivelyAndStoresTheToken() =
        runTest {
            val outcome = auth.signIn()

            assertEquals(PlatformSignInOutcome.Authorized, outcome)
            assertEquals(listOf(true), requester.requests)
            assertEquals("neuf", storage.token?.accessToken)
        }

    @Test
    fun signInReportsTheFailureWithoutStoringAnything() =
        runTest {
            requester.next = TokenResult.Denied(reason = PlatformSignInFailure.CANCELLED, detail = "popup_closed")

            val outcome = auth.signIn()

            assertEquals(
                PlatformSignInOutcome.Failed(reason = PlatformSignInFailure.CANCELLED, detail = "popup_closed"),
                outcome,
            )
            assertNull(storage.token)
        }

    @Test
    fun signOutRevokesAndForgetsTheToken() =
        runTest {
            storage.token = stored(token = "ancien", expiresInMillis = 30 * 60_000)

            auth.signOut()

            assertEquals(listOf("ancien"), requester.revoked)
            assertNull(storage.token)
        }

    @Test
    fun signOutForgetsTheTokenEvenWhenRevocationFails() =
        runTest {
            storage.token = stored(token = "ancien", expiresInMillis = 30 * 60_000)
            requester.revokeFails = true

            auth.signOut()

            assertNull(storage.token)
        }
}
