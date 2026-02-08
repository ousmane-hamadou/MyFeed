package com.github.ousmane_hamadou.domain.post

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
enum class ValidationType { CONFIRM, REFUTE }

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Validation(
    val id: Uuid = Uuid.random(),
    val postId: Uuid,
    val validatorId: Uuid,
    val type: ValidationType,
    val createdAt: Instant = Clock.System.now()
)

