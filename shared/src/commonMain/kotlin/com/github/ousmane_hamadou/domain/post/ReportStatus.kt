package com.github.ousmane_hamadou.domain.post

import kotlinx.serialization.Serializable

@Serializable
enum class ReportStatus { PENDING, VALIDATED, REJECTED }