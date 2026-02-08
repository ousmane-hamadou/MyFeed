package com.github.ousmane_hamadou.domain.post


import com.github.ousmane_hamadou.domain.exception.DomainException.ReportDomainException
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ModerationServiceTest : FunSpec({

    val reportRepository = mockk<ReportRepository>()
    val postRepository = mockk<PostRepository>()
    val userService = mockk<UserService>()
    val moderationService = ModerationService(reportRepository, postRepository, userService)

    beforeTest {
        clearMocks(reportRepository, postRepository, userService)
    }

    test("should fail with ReportDomainException.Duplicate when user reports same post twice") {
        // Given
        val reporterId = Uuid.random()
        val postId = Uuid.random()
        coEvery { reportRepository.existsByReporterAndPost(reporterId, postId) } returns true

        // When
        val result = moderationService.reportPost(reporterId, postId, ReportReason.SPAM, null)

        // Then
        result.shouldBeFailure { exception ->
            exception.shouldBeInstanceOf<ReportDomainException.Duplicate>()
        }
    }

    test("should archive post automatically when report threshold is reached") {
        // Given
        val postId = Uuid.random()
        val reporterId = Uuid.random()

        coEvery { reportRepository.existsByReporterAndPost(any(), any()) } returns false
        coEvery { reportRepository.save(any()) } returns mockk()
        coEvery { reportRepository.countReportsForPost(postId) } returns 5
        coEvery { postRepository.updateStatus(postId, PostStatus.ARCHIVED) } just Runs

        // When
        val result =
            moderationService.reportPost(reporterId, postId, ReportReason.HARASSMENT, "Méchant")

        // Then
        result.shouldBeSuccess()
        coVerify(exactly = 1) { postRepository.updateStatus(postId, PostStatus.ARCHIVED) }
    }

    test("given valid report when confirmed then should penalize author and delete post") {
        // Given
        val adminId = Uuid.random()
        val reportId = Uuid.random()
        val authorId = Uuid.random()
        val postId = Uuid.random()

        val report = Report(
            id = reportId,
            reporterId = Uuid.random(),
            postId = postId,
            reason = ReportReason.FAKE_NEWS,
            createdAt = Clock.System.now(),
            details = null
        )

        val post = Post(
            id = postId,
            authorId = authorId,
            title = "Post à supprimer",
            content = "Contenu problématique",
            category = PostCategory.OFFICIAL,
            status = PostStatus.ARCHIVED,
            source = PostSource.COMMUNITY,
            createdAt = Clock.System.now(),
            visibility = VisibilityScope(establishment = null)
        )

        coEvery { reportRepository.findById(reportId) } returns report
        coEvery { postRepository.findById(postId) } returns post
        coEvery {
            userService.adjustUserTrust(
                authorId,
                any()
            )
        } returns Result.success(mockk<User>())
        coEvery { postRepository.delete(postId) } just Runs
        coEvery { reportRepository.updateStatus(reportId, ReportStatus.VALIDATED) } just Runs

        // When
        val result = moderationService.confirmReport(adminId, reportId)

        // Then
        result.shouldBeSuccess()
        coVerify {
            userService.adjustUserTrust(authorId, TrustImpact.FAKE_NEWS_PUBLISHED)
            postRepository.delete(postId)
            reportRepository.updateStatus(reportId, ReportStatus.VALIDATED)
        }
    }

    test("given valid report when rejected then should restore post status and close report") {
        // Given
        Uuid.random()
        val reportId = Uuid.random()
        val postId = Uuid.random()

        val report = Report(
            id = reportId,
            reporterId = Uuid.random(),
            postId = postId,
            reason = ReportReason.WRONG_CATEGORY,
            createdAt = Clock.System.now(),
            details = null,
        )

        coEvery { reportRepository.findById(reportId) } returns report
        coEvery { postRepository.updateStatus(postId, PostStatus.PUBLISHED) } just Runs
        coEvery { reportRepository.updateStatus(reportId, ReportStatus.REJECTED) } just Runs

        // When
        val result = moderationService.rejectReport(reportId)

        // Then
        result.shouldBeSuccess()
        coVerify(exactly = 1) {
            postRepository.updateStatus(postId, PostStatus.PUBLISHED)
            reportRepository.updateStatus(reportId, ReportStatus.REJECTED)
        }
        coVerify(exactly = 0) { userService.adjustUserTrust(any(), any()) }
    }
})