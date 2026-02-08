package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException.SyncDomainException
import com.github.ousmane_hamadou.domain.exception.recoverDomainError
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InboundSyncService(
    private val providers: List<ExternalInformationProvider>,
    private val postRepository: PostRepository
) {

    private val SYSTEM_OFFICIAL_ID = Uuid.parse("00000000-0000-0000-0000-000000000000")

    suspend fun syncAllSources(): Result<Unit> = runCatching {
        providers.forEach { provider ->
            // On récupère les posts (le provider retourne un Result)
            val externalPosts = provider.fetchLatestPosts().getOrThrow()

            externalPosts.forEach { ext ->
                if (!postRepository.existsByExternalId(ext.externalId)) {
                    val post = Post(
                        id = Uuid.random(), // Assure-toi d'avoir un ID unique
                        authorId = SYSTEM_OFFICIAL_ID,
                        title = ext.title ?: "Communiqué ${provider.sourceName}",
                        content = ext.content,
                        category = PostCategory.OFFICIAL,
                        status = PostStatus.PUBLISHED,
                        source = PostSource.EXTERNAL_OFFICIAL,
                        externalId = ext.externalId,
                        originName = provider.sourceName,
                        createdAt = ext.date,
                        visibility = VisibilityScope(
                            establishment = provider.targetEstablishment,
                            department = null
                        )
                    )

                    // On sauve chaque post avec son propre catch pour transformer l'erreur
                    runCatching {
                        postRepository.save(post)
                    }.recoverDomainError { msg, cause ->
                        SyncDomainException.PersistenceFailed(msg, cause)
                    }.getOrThrow() // On remonte l'erreur métier pour arrêter la boucle si besoin
                }
            }
        }
    }.recoverDomainError { msg, cause ->
        // Capture les erreurs globales (ex: erreur réseau du provider non gérée)
        SyncDomainException.ProviderError(msg, cause)
    }
}