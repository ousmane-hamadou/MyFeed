package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException
import com.github.ousmane_hamadou.domain.exception.DomainException.ReportDomainException
import com.github.ousmane_hamadou.domain.exception.recoverDomainError
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.UserService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ModerationService(
    private val reportRepository: ReportRepository,
    private val postRepository: PostRepository,
    private val userService: UserService
) {
    private val AUTO_QUARANTINE_THRESHOLD = 5

    /**
     * Crée un signalement avec vérification anti-spam et quarantaine auto.
     */
    suspend fun reportPost(
        reporterId: Uuid,
        postId: Uuid,
        reason: ReportReason,
        details: String?
    ): Result<Report> = runCatching {
        // 1. Vérification anti-spam
        if (reportRepository.existsByReporterAndPost(reporterId, postId)) {
            throw ReportDomainException.Duplicate(
                reporterId.toString(),
                postId.toString()
            )
        }

        // 2. Sauvegarde
        val report = Report(
            reporterId = reporterId,
            postId = postId,
            reason = reason,
            details = details
        )
        val savedReport = reportRepository.save(report)

        // 3. Quarantaine automatique
        val count = reportRepository.countReportsForPost(postId)
        if (count >= AUTO_QUARANTINE_THRESHOLD) {
            postRepository.updateStatus(postId, PostStatus.ARCHIVED)
        }

        savedReport
    }.recoverDomainError { msg, cause -> ReportDomainException.PersistenceFailed(msg, cause) }

    /**
     * Confirmation manuelle par un modérateur.
     * Applique les sanctions sur le TrustScore et supprime le post.
     */
    suspend fun confirmReport(adminId: Uuid, reportId: Uuid): Result<Unit> = runCatching {
        val report = reportRepository.findById(reportId)
            ?: throw ReportDomainException.NotFound(reportId.toString())

        val post = postRepository.findById(report.postId)
            ?: throw PostDomainException.NotFound(report.postId.toString())

        // 1. Calcul de l'impact sur le TrustScore
        val impact = when (report.reason) {
            ReportReason.FAKE_NEWS -> TrustImpact.FAKE_NEWS_PUBLISHED
            else -> TrustImpact.REPORT_VALIDATED
        }

        // 2. Application de la sanction (On propage l'erreur si le UserService échoue)
        userService.adjustUserTrust(post.authorId, impact).getOrThrow()

        // 3. Nettoyage
        postRepository.delete(post.id)
        reportRepository.updateStatus(reportId, ReportStatus.VALIDATED)
    }.recoverDomainError { msg, cause -> ReportDomainException.ActionFailed(msg, cause) }

    /**
     * Rejette un signalement et réhabilite le post.
     */
    suspend fun rejectReport(reportId: Uuid): Result<Unit> = runCatching {
        val report = reportRepository.findById(reportId)
            ?: throw ReportDomainException.NotFound(reportId.toString())

        postRepository.updateStatus(report.postId, PostStatus.PUBLISHED)
        reportRepository.updateStatus(reportId, ReportStatus.REJECTED)
    }.recoverDomainError { msg, cause -> ReportDomainException.ActionFailed(msg, cause) }
}