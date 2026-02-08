package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException
import com.github.ousmane_hamadou.domain.exception.recoverDomainError
import com.github.ousmane_hamadou.domain.user.UserRole
import com.github.ousmane_hamadou.domain.user.UserService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostService(
    private val postRepository: PostRepository,
    private val userService: UserService
) {
    suspend fun createPost(
        authorId: Uuid,
        title: String,
        content: String,
        category: PostCategory
    ): Result<Post> = runCatching {
        // 1. Récupérer l'auteur via le UserService (qui retourne déjà un Result)
        val author = userService.getUserProfile(authorId).getOrNull()
            ?: throw PostDomainException.AuthorNotFound("Author $authorId not found")

        // 2. Déterminer le statut initial selon la confiance
        val initialStatus = when {
            author.role == UserRole.ADMIN || author.role == UserRole.DELEGATE -> PostStatus.PUBLISHED
            author.trustScore.value >= 80 -> PostStatus.PUBLISHED
            else -> PostStatus.PENDING
        }

        // 3. Déterminer la portée (Scope) selon le rôle
        // Rule: Admins are global, others are restricted to their department.
        val visibilityScope = when (author.role) {
            UserRole.ADMIN -> VisibilityScope()
            else -> VisibilityScope(department = author.department)
        }

        val newPost = Post(
            authorId = authorId,
            title = title,
            content = content,
            category = category,
            status = initialStatus,
            source = PostSource.COMMUNITY,
            visibility = visibilityScope
            // createdAt est géré par défaut dans le constructeur de la data class Post
        )

        postRepository.save(newPost)
    }.recoverDomainError { msg, cause -> PostDomainException.PersistenceFailed(msg, cause) }

    suspend fun changePostStatus(postId: Uuid, newStatus: PostStatus): Result<Unit> = runCatching {
        if (!postRepository.existsByExternalId(postId.toString())) { // ou findById
            throw PostDomainException.NotFound(postId.toString())
        }
        postRepository.updateStatus(postId, newStatus)
    }.recoverDomainError { msg, cause -> PostDomainException.PersistenceFailed(msg, cause) }
}