package com.github.ousmane_hamadou.domain.user

import com.github.ousmane_hamadou.domain.exception.DomainException.UserDomainException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UserServiceTest : FunSpec({

    val userRepository = mockk<UserRepository>()
    val userService = UserService(userRepository)

    test("given admin user when promoting student then should update role and trust score") {
        // Given
        val adminId = Uuid.random()
        val studentId = Uuid.random()

        val admin = User(
            id = adminId,
            matricule = "ADM-001",
            fullName = "Admin User",
            role = UserRole.ADMIN,
            department = Department.COMPUTER_SCIENCE,
            level = "N/A"
        )
        val student = User(
            id = studentId,
            matricule = "STU-001",
            fullName = "Student User",
            role = UserRole.STUDENT,
            department = Department.COMPUTER_SCIENCE,
            level = "L3"
        )

        coEvery { userRepository.findById(adminId) } returns admin
        coEvery { userRepository.findById(studentId) } returns student
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When
        val result = userService.promoteToDelegate(adminId, studentId)

        // Then
        result.shouldBeSuccess { updatedUser ->
            updatedUser.role shouldBe UserRole.DELEGATE
            updatedUser.trustScore shouldBe TrustScore.MAX
        }
    }

    test("given non-admin user when promoting student then should fail with UnauthorizedAdminAction") {
        // Given
        val fakeAdminId = Uuid.random()
        val studentId = Uuid.random()
        val studentActingAsAdmin = User(
            id = fakeAdminId,
            matricule = "STU-002",
            fullName = "Regular Student",
            role = UserRole.STUDENT,
            department = Department.PUBLIC_LAW,
            level = "L1"
        )

        coEvery { userRepository.findById(fakeAdminId) } returns studentActingAsAdmin

        // When & Then
        val result = userService.promoteToDelegate(fakeAdminId, studentId)

        result.shouldBeFailure { exception ->
            exception.shouldBeInstanceOf<UserDomainException.UnauthorizedAdminAction>()
        }
    }

    test("given existing matricule when registering then should fail with AlreadyExists") {
        // Given
        val matricule = "20A045FS"
        coEvery { userRepository.findByMatricule(matricule) } returns mockk<User>()

        // When
        val result =
            userService.registerUser(matricule, "Jane Doe", Department.COMPUTER_SCIENCE, "GIM2")

        // Then
        result.shouldBeFailure { it.shouldBeInstanceOf<UserDomainException.AlreadyExists>() }
    }

    test("given valid user id when getting profile then should return user") {
        // Given
        val userId = Uuid.random()
        val expectedUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "Ousmane",
            role = UserRole.STUDENT,
            department = Department.MATHEMATICS,
            level = "L3"
        )
        coEvery { userRepository.findById(userId) } returns expectedUser

        // When & Then
        userService.getUserProfile(userId).shouldBeSuccess {
            it.id shouldBe userId
            it.fullName shouldBe "Ousmane"
        }
    }

    test("given positive validation when adjusting trust then should increase score by correct impact") {
        // Given
        val userId = Uuid.random()
        val initialUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "X",
            role = UserRole.STUDENT,
            department = Department.MATHEMATICS,
            level = "L1",
            trustScore = TrustScore(50)
        )

        coEvery { userRepository.findById(userId) } returns initialUser
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When & Then
        userService.adjustUserTrust(userId, TrustImpact.POSITIVE_CONTRIBUTION).shouldBeSuccess {
            it.trustScore.value shouldBe 55
        }
    }

    test("given trust adjustment when score goes below zero then should be clamped to 0") {
        // Given
        val userId = Uuid.random()
        val initialUser = User(
            id = userId,
            matricule = "MAT-1",
            fullName = "X",
            role = UserRole.STUDENT,
            department = Department.COMPUTER_SCIENCE,
            level = "L1",
            trustScore = TrustScore(10)
        )

        coEvery { userRepository.findById(userId) } returns initialUser
        coEvery { userRepository.save(any()) } returnsArgument 0

        // When & Then
        userService.adjustUserTrust(userId, TrustImpact.FAKE_NEWS_PUBLISHED).shouldBeSuccess {
            it.trustScore.value shouldBe 0
        }
    }
})