package fr.rlebranchu.ytblight.umbrella

import org.koin.core.KoinApplication

/**
 * Conteneur Koin isolé, pour `KoinIsolatedContext` côté Compose (`koinViewModel()`).
 * Réservé à Android : Swift et TypeScript passent par les méthodes de [SharedDependencies].
 */
fun SharedDependencies.koinApplication(): KoinApplication = koinApplication
