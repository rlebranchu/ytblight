package fr.rlebranchu.ytblight

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.rlebranchu.ytblight.fixtures.FakeAuthRepository
import fr.rlebranchu.ytblight.ui.YtblightApp
import fr.rlebranchu.ytblight.ui.YtblightTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

/** Tests de l'interface sur données factices : aucun appel à Google, aucun quota consommé. */
@RunWith(AndroidJUnit4::class)
class YtblightAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var fakeModule: Module? = null

    @After
    fun removeFakeModule() {
        fakeModule?.let(::unloadKoinModules)
    }

    private fun launch(
        signedIn: Boolean,
    ): FakeAuthRepository {
        // Koin est démarré par YtblightApplication : on y surcharge les repositories par des fakes.
        val auth =
            FakeAuthRepository(
                signedIn = signedIn,
            )
        fakeModule =
            fakeModule(
                auth = auth,
            ).also(::loadKoinModules)
        composeRule.setContent {
            YtblightTheme {
                YtblightApp()
            }
        }
        return auth
    }

    @Test
    fun signedInUserSeesTheFeedWithoutShorts() {
        launch(
            signedIn = true,
        )

        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertIsDisplayed()
        composeRule.onNodeWithText("24:10").assertIsDisplayed()
        composeRule.onNodeWithText("Chaîne Kotlin · il y a 5 jours", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Un court extrait").assertDoesNotExist()
        composeRule.onNodeWithText("Un autre Short").assertDoesNotExist()
    }

    @Test
    fun feedIsSortedFromNewestToOldest() {
        launch(
            signedIn = true,
        )

        val titles =
            listOf("Conférence d'une heure", "Visite guidée d'un atelier", "Un long format sur Kotlin Multiplatform")
        val tops =
            titles.map { title ->
                composeRule
                    .onNodeWithText(title)
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun signedOutUserSeesTheSignInScreenThenTheFeed() {
        val auth =
            launch(
                signedIn = false,
            )

        composeRule.onNodeWithText("Connecte-toi avec ton compte Google", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertDoesNotExist()

        composeRule.onNodeWithText("Se connecter avec Google").performClick()

        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertIsDisplayed()
        assertEquals(1, auth.signInCalls)
    }

    @Test
    fun signingOutReturnsToTheSignInScreen() {
        launch(
            signedIn = true,
        )

        composeRule.onNodeWithText("Se déconnecter").performClick()

        composeRule.onNodeWithText("Se connecter avec Google").assertIsDisplayed()
        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertDoesNotExist()
    }
}
