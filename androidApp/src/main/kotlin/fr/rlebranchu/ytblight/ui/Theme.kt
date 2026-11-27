package fr.rlebranchu.ytblight.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Thème sombre uniquement, sans couleurs dynamiques. */
@Composable
fun YtblightTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
