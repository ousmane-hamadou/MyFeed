package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException
import com.github.ousmane_hamadou.domain.user.TrustImpact
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ValidationServiceTest : FunSpec({

    val validationRepository = mockk<ValidationRepository>()
    val postRepository = mockk<PostRepository>()
    val userService = mockk<UserService>(relaxed = true)
    val validationService = ValidationService(validationRepository, postRepository, userService)

    beforeTest {
        clearMocks(postRepository, userService)
    }

    fun createDummyPost(id: Uuid, authorId: Uuid) = Post(
        id = id,
        authorId = authorId,
        title = "Titre Test",
        content = "Contenu de test",
        category = PostCategory.INFO,
        status = PostStatus.PENDING,
        createdAt = Clock.System.now()
    )

    beforeTest {
        clearMocks(validationRepository, postRepository, userService)
    }

    test("given valid data when validating post then should return success") {
        val validatorId = Uuid.random()
        val postId = Uuid.random()
        val authorId = Uuid.random()
        val post = createDummyPost(postId, authorId)

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(validatorId, postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0
        coEvery { userService.adjustUserTrust(any(), any()) } returns Result.success(mockk<User>())
        coEvery { validationRepository.countByType(any(), any()) } returns 0
        coEvery { postRepository.save(any()) } returnsArgument 0

        val result = validationService.validatePost(validatorId, postId, ValidationType.CONFIRM)

        result.shouldBeSuccess()
    }

    test("given non-existent post when validating then should fail with PostNotFoundException") {
        val postId = Uuid.random()
        coEvery { postRepository.findById(postId) } returns null

        val result = validationService.validatePost(Uuid.random(), postId, ValidationType.CONFIRM)

        result.shouldBeFailure<DomainException.PostDomainException.NotFound>()
    }

    test("given author when validating own post then should fail with SelfValidationException") {
        val authorId = Uuid.random()
        val post = createDummyPost(Uuid.random(), authorId)
        coEvery { postRepository.findById(any()) } returns post

        val result = validationService.validatePost(authorId, post.id, ValidationType.CONFIRM)

        result.shouldBeFailure<DomainException.ValidationDomainException.SelfValidation>()
    }

    test("given user who already validated when validating again then should fail with DoubleValidationException") {
        val userId = Uuid.random()
        val postId = Uuid.random()
        val post = createDummyPost(postId, Uuid.random())

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(userId, postId) } returns true

        val result = validationService.validatePost(userId, postId, ValidationType.CONFIRM)

        result.shouldBeFailure<DomainException.ValidationDomainException.DoubleValidation>()
    }

    test("given confirm validation then should notify userService with POSITIVE_CONTRIBUTION") {
        val validatorId = Uuid.random()
        val authorId = Uuid.random()
        val postId = Uuid.random()
        val post = createDummyPost(postId, authorId)

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(validatorId, postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0
        coEvery { validationRepository.countByType(any(), any()) } returns 1
        coEvery { postRepository.save(any()) } returnsArgument 0
        coEvery {
            userService.adjustUserTrust(
                authorId,
                TrustImpact.POSITIVE_CONTRIBUTION
            )
        } returns Result.success(mockk<User>())

        validationService.validatePost(validatorId, postId, ValidationType.CONFIRM)
            .shouldBeSuccess()

        coVerify(exactly = 1) {
            userService.adjustUserTrust(authorId, TrustImpact.POSITIVE_CONTRIBUTION)
        }
    }

    test("given pending post when reaching 5th confirmation then status should be PUBLISHED") {
        val postId = Uuid.random()
        val post = createDummyPost(postId, Uuid.random()).copy(status = PostStatus.PENDING)

        coEvery { postRepository.findById(postId) } returns post
        coEvery { validationRepository.hasUserValidatedPost(any(), postId) } returns false
        coEvery { validationRepository.save(any()) } returnsArgument 0
        coEvery { userService.adjustUserTrust(any(), any()) } returns Result.success(mockk())
        coEvery { validationRepository.countByType(postId, ValidationType.CONFIRM) } returns 5
        coEvery { validationRepository.countByType(postId, ValidationType.REFUTE) } returns 0
        coEvery { postRepository.save(any()) } returnsArgument 0

        validationService.validatePost(Uuid.random(), postId, ValidationType.CONFIRM)

        coVerify {
            postRepository.updateStatus(postId, PostStatus.PUBLISHED)
        }

        coVerify(exactly = 0) { postRepository.save(any()) }
    }
})