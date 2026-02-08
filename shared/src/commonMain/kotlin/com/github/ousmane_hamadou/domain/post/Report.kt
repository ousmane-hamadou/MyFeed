package com.github.ousmane_hamadou.domain.post

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Report(
    val id: Uuid = Uuid.random(),
    val reporterId: Uuid,
    val postId: Uuid,
    val reason: ReportReason,
    val details: String? = null,
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: Instant = Clock.System.now()
)