package com.github.ousmane_hamadou.domain.post

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface ValidationRepository {
    suspend fun save(validation: Validation): Validation
    suspend fun hasUserValidatedPost(userId: Uuid, postId: Uuid): Boolean
    suspend fun findByPostId(postId: Uuid): List<Validation>
    suspend fun countByType(postId: Uuid, type: ValidationType): Int
}