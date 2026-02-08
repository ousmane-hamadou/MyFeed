package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Department
import com.github.ousmane_hamadou.domain.user.Establishment
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@Serializable
data class VisibilityScope(
    val establishment: Establishment? = null,
    val department: Department? = null
) {
    // Si les deux sont nuls, c'est public à toute l'université
    fun isPublic(): Boolean = establishment == null && department == null
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Post(
    val id: Uuid = Uuid.random(),
    val authorId: Uuid,
    val title: String,
    val content: String,
    val category: PostCategory,
    val status: PostStatus = PostStatus.PENDING,
    val createdAt: Instant = Clock.System.now(),
    val upVotes: Int = 0,
    val downVotes: Int = 0,
    val source: PostSource = PostSource.COMMUNITY,
    val externalId: String? = null,
    val originName: String? = null,
    val visibility: VisibilityScope = VisibilityScope()
) {
    val totalScore: Int get() = upVotes - downVotes

    fun canBeAutoPublished(userTrustScore: Int): Boolean =
        userTrustScore >= 80 || source == PostSource.EXTERNAL_OFFICIAL
}