package fr.rlebranchu.ytblight.domain.feed

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.error.map
import fr.rlebranchu.ytblight.domain.model.Video

/** Fil chronologique des abonnements, sans Shorts. */
class GetFeedUseCase(
    private val repository: FeedRepository,
    private val policy: FeedPolicy = FeedPolicy(),
) {
    suspend operator fun invoke(
        forceRefresh: Boolean = false,
    ): DomainResult<List<Video>> =
        repository
            .loadFeed(
                forceRefresh = forceRefresh,
            ).map(policy::apply)
}
