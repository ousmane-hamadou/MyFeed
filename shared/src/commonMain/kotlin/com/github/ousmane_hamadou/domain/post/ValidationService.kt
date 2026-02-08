package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException
import com.github.ousmane_hamadou.domain.exception.DomainException.ValidationDomainException
import com.github.ousmane_hamadou.domain.exception.recoverDomainError
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.UserService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ValidationService(
    private val validationRepository: ValidationRepository,
    private val postRepository: PostRepository,
    private val userService: UserService,
) {
    private val PUBLICATION_THRESHOLD = 5
    private val SUSPICION_THRESHOLD = 3

    suspend fun validatePost(
        validatorId: Uuid,
        postId: Uuid,
        type: ValidationType
    ): Result<Validation> = runCatching {
        // 1. Vérification existence post
        val post = postRepository.findById(postId)
            ?: throw PostDomainException.NotFound(postId.toString())

        // 2. Règles métier
        if (post.authorId == validatorId) throw ValidationDomainException.SelfValidation()

        if (validationRepository.hasUserValidatedPost(validatorId, postId)) {
            throw ValidationDomainException.DoubleValidation(
                validatorId.toString(),
                postId.toString()
            )
        }

        // 3. Sauvegarde
        val validation = Validation(postId = postId, validatorId = validatorId, type = type)
        val savedValidation = validationRepository.save(validation)

        // 4. Impact Trust Score (On propage l'erreur si adjustUserTrust échoue)
        val impact = if (type == ValidationType.CONFIRM)
            TrustImpact.POSITIVE_CONTRIBUTION
        else
            TrustImpact.FAKE_NEWS_PUBLISHED

        userService.adjustUserTrust(post.authorId, impact).getOrThrow()

        // 5. Mise à jour statut
        updatePostStatusIfNeeded(postId)

        savedValidation
    }.recoverDomainError { msg, cause -> ValidationDomainException.ActionFailed(msg, cause) }

    private suspend fun updatePostStatusIfNeeded(postId: Uuid) {
        val post = postRepository.findById(postId) ?: return
        if (post.status == PostStatus.ARCHIVED) return

        val confirms = validationRepository.countByType(postId, ValidationType.CONFIRM)
        val refutes = validationRepository.countByType(postId, ValidationType.REFUTE)

        val newStatus = when {
            refutes >= SUSPICION_THRESHOLD -> PostStatus.SUSPECT
            confirms >= PUBLICATION_THRESHOLD && post.status == PostStatus.PENDING -> PostStatus.PUBLISHED
            else -> post.status
        }

        if (newStatus != post.status) {
            postRepository.updateStatus(postId, newStatus)
        }
    }
}