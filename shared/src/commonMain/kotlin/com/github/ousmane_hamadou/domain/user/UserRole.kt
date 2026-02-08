package com.github.ousmane_hamadou.domain.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT, DELEGATE, ADMIN
}