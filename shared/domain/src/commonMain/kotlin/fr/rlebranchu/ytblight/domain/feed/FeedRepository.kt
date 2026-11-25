package fr.rlebranchu.ytblight.domain.feed

import fr.rlebranchu.ytblight.domain.error.DomainResult
import fr.rlebranchu.ytblight.domain.model.Video

/** Source des dernières vidéos des abonnements. Implémentée par la couche `data`. */
interface FeedRepository {
    /**
     * @param forceRefresh `false` autorise à servir le cache en mémoire de la session ;
     * `true` interroge à nouveau l'API (action explicite de l'utilisateur).
     */
    suspend fun loadFeed(forceRefresh: Boolean): DomainResult<List<Video>>

    /** Oublie le fil en mémoire (déconnexion). */
    suspend fun clear()
}
