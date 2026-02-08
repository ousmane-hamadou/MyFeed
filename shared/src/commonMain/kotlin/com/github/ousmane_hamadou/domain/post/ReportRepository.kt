package com.github.ousmane_hamadou.domain.post

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface ReportRepository {
    suspend fun save(report: Report): Report
    suspend fun findById(id: Uuid): Report?
    suspend fun findPendingByEstablishment(establishmentId: String): List<Report>
    suspend fun updateStatus(id: Uuid, status: ReportStatus)
    suspend fun countReportsForPost(postId: Uuid): Long
    suspend fun existsByReporterAndPost(reporterId: Uuid, postId: Uuid): Boolean
}