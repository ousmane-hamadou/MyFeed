package com.github.ousmane_hamadou.domain.post

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface PostRepository {
    suspend fun save(post: Post): Post
    suspend fun findById(id: Uuid): Post?
    suspend fun findAllPublished(): List<Post>
    suspend fun delete(id: Uuid)
    suspend fun existsByExternalId(externalId: String): Boolean
    suspend fun updateStatus(id: Uuid, status: PostStatus)
}