package com.github.ousmane_hamadou.domain.user

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class User(
    val id: Uuid = Uuid.random(),
    val matricule: String,
    val fullName: String,
    val level: String,
    val role: UserRole,
    val trustScore: TrustScore = TrustScore.DEFAULT,
    val department: Department,
) {
    val establishment: Establishment
        get() = department.establishment

    fun canPublishCertified(): Boolean = role == UserRole.DELEGATE || role == UserRole.ADMIN
    fun canVote(): Boolean = trustScore.value > 0

    fun updateReputation(points: Int): User {
        val newValue = (trustScore.value + points).coerceIn(0, 100)
        return this.copy(trustScore = TrustScore(newValue))
    }
}