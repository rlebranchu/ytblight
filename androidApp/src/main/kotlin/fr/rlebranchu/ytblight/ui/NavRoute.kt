package fr.rlebranchu.ytblight.ui

import kotlinx.serialization.Serializable

/** Destinations de la navigation (type-safe, via kotlinx.serialization). Une page = une destination. */
sealed interface NavRoute {
    @Serializable
    data object Splash : NavRoute

    @Serializable
    data object SignIn : NavRoute

    @Serializable
    data object Feed : NavRoute

    @Serializable
    data class Player(
        val videoId: String,
    ) : NavRoute
}
