package fr.rlebranchu.ytblight.domain.auth

import fr.rlebranchu.ytblight.domain.feed.FeedRepository

/** Déconnecte et efface le fil en mémoire : il appartient à l'utilisateur qui part. */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val feedRepository: FeedRepository,
) {
    suspend operator fun invoke() {
        authRepository.signOut()
        feedRepository.clear()
    }
}
