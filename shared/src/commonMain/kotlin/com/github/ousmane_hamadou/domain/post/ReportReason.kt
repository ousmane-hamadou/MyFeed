package com.github.ousmane_hamadou.domain.post

import kotlinx.serialization.Serializable

@Serializable
enum class ReportReason {
    SPAM,
    FAKE_NEWS,
    HARASSMENT,
    INAPPROPRIATE_CONTENT,
    WRONG_CATEGORY
}