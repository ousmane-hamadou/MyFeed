package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException
import com.github.ousmane_hamadou.domain.user.Department
import com.github.ousmane_hamadou.domain.user.TrustScore
import com.github.ousmane_hamadou.domain.user.User
import com.github.ousmane_hamadou.domain.user.UserRole
import com.github.ousmane_hamadou.domain.user.UserService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostServiceTest : FunSpec({

    val postRepository = mockk<PostRepository>()
    val userService = mockk<UserService>()
    val postService = PostService(postRepository, userService)

    test("given delegate author when creating post then visibility should be restricted to their department") {
        // Given
        val delegateId = Uuid.random()
        val delegate = User(
            id = delegateId,
            matricule = "D1",
            fullName = "Delegate IT",
            role = UserRole.DELEGATE,
            department = Department.COMPUTER_SCIENCE,
            level = "L3"
        )

        coEvery { userService.getUserProfile(delegateId) } returns Result.success(delegate)
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When & Then
        postService.createPost(delegateId, "Urgent", "TP annulé", PostCategory.ALERT)
            .shouldBeSuccess { post ->
                post.status shouldBe PostStatus.PUBLISHED
                post.visibility.department shouldBe Department.COMPUTER_SCIENCE
                post.visibility.establishment.shouldBeNull()
            }
    }

    test("given student author when creating post then visibility should also be restricted to their department") {
        // Given
        val studentId = Uuid.random()
        val student = User(
            id = studentId,
            matricule = "S1",
            fullName = "Student",
            role = UserRole.STUDENT,
            department = Department.BIOLOGY,
            level = "L1"
        )

        coEvery { userService.getUserProfile(studentId) } returns Result.success(student)
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When & Then
        postService.createPost(studentId, "Question", "Livre à prêter", PostCategory.INFO)
            .shouldBeSuccess { post ->
                post.visibility.department shouldBe Department.BIOLOGY
            }
    }

    test("given admin author when creating post then visibility should be global") {
        // Given
        val adminId = Uuid.random()
        val admin = User(
            id = adminId,
            matricule = "A1",
            fullName = "Admin",
            role = UserRole.ADMIN,
            department = Department.COMPUTER_SCIENCE,
            level = "N/A"
        )

        coEvery { userService.getUserProfile(adminId) } returns Result.success(admin)
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When & Then
        postService.createPost(adminId, "Global", "Maintenant", PostCategory.ALERT)
            .shouldBeSuccess { post ->
                post.visibility.department.shouldBeNull()
                post.visibility.establishment.shouldBeNull()
            }
    }

    test("given low trust student when creating post then status should be PENDING") {
        // Given
        val studentId = Uuid.random()
        val student = User(
            id = studentId,
            matricule = "S2",
            fullName = "Troll",
            role = UserRole.STUDENT,
            department = Department.MATHEMATICS,
            trustScore = TrustScore(10),
            level = "L2"
        )

        coEvery { userService.getUserProfile(studentId) } returns Result.success(student)
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When & Then
        postService.createPost(studentId, "Titre", "Contenu", PostCategory.INFO)
            .shouldBeSuccess { post ->
                post.status shouldBe PostStatus.PENDING
            }
    }

    test("given unknown author when creating post then should fail with PostAuthorNotFound") {
        // Given
        val unknownId = Uuid.random()
        // On simule un échec de récupération du profil
        coEvery { userService.getUserProfile(unknownId) } returns
                Result.failure(PostDomainException.AuthorNotFound("User not found"))

        // When
        val result = postService.createPost(unknownId, "Titre", "Contenu", PostCategory.INFO)

        // Then
        result.shouldBeFailure { exception ->
            exception.shouldBeInstanceOf<PostDomainException.AuthorNotFound>()
        }
    }
})