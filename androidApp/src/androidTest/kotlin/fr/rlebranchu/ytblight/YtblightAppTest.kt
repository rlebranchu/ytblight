package fr.rlebranchu.ytblight

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.rlebranchu.ytblight.presentation.signin.SignInViewModel
import fr.rlebranchu.ytblight.testing.fakeDependencies
import fr.rlebranchu.ytblight.ui.YtblightApp
import fr.rlebranchu.ytblight.ui.YtblightTheme
import fr.rlebranchu.ytblight.ui.signin.SignInStarter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests de l'interface sur données factices : aucun appel à Google, aucun quota consommé. */
@RunWith(AndroidJUnit4::class)
class YtblightAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FakeSignInStarter(
        private val viewModel: SignInViewModel,
    ) : SignInStarter {
        var starts = 0

        override fun start() {
            starts++
            viewModel.onSucceeded()
        }
    }

    private fun launch(
        signedIn: Boolean,
    ): List<FakeSignInStarter> {
        val starters = mutableListOf<FakeSignInStarter>()
        composeRule.setContent {
            YtblightTheme {
                YtblightApp(
                    dependencies =
                        fakeDependencies(
                            signedIn = signedIn,
                        ),
                    signInStarter = { viewModel ->
                        remember {
                            FakeSignInStarter(
                                viewModel = viewModel,
                            ).also(starters::add)
                        }
                    },
                )
            }
        }
        return starters
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
        val starters =
            launch(
                signedIn = false,
            )

        composeRule.onNodeWithText("Connecte-toi avec ton compte Google", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertDoesNotExist()

        composeRule.onNodeWithText("Se connecter avec Google").performClick()

        composeRule.onNodeWithText("Un long format sur Kotlin Multiplatform").assertIsDisplayed()
        assertEquals(1, starters.sumOf { it.starts })
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
