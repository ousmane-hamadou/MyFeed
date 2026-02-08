package com.github.ousmane_hamadou.domain.post

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ExternalInboundPost(
    val externalId: String,
    val title: String?,
    val content: String,
    val date: Instant, // Utilisation de kotlin.time.Instant
    val rawUrl: String?
)