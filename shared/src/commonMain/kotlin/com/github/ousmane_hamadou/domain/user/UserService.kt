package com.github.ousmane_hamadou.domain.user

import com.github.ousmane_hamadou.domain.exception.DomainException.UserDomainException
import com.github.ousmane_hamadou.domain.exception.recoverDomainError
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
class UserService(
    private val userRepository: UserRepository
) {

    suspend fun registerUser(
        matricule: String, fullName: String, department: Department, level: String
    ): Result<User> = runCatching {
        val existingUser = userRepository.findByMatricule(matricule)
        if (existingUser != null) {
            throw UserDomainException.AlreadyExists(matricule)
        }

        val newUser = User(
            matricule = matricule,
            fullName = fullName,
            department = department,
            level = level,
            role = UserRole.STUDENT,
            trustScore = TrustScore.DEFAULT
        )

        userRepository.save(newUser)
    }.recoverDomainError { msg, cause ->
        UserDomainException.PersistenceFailed(msg, cause)
    }


    suspend fun promoteToDelegate(adminId: Uuid, targetStudentId: Uuid): Result<User> =
        runCatching {
            val admin = getUserProfile(adminId).getOrThrow()

            if (admin.role != UserRole.ADMIN) {
                throw UserDomainException.UnauthorizedAdminAction(adminId.toString())
            }

            val student = getUserProfile(targetStudentId).getOrThrow()

            // Appliquer le changement de rôle
            val promotedUser = student.copy(
                role = UserRole.DELEGATE, trustScore = TrustScore.MAX
            )

            userRepository.save(promotedUser)

        }.recoverDomainError { msg, cause ->
            UserDomainException.PersistenceFailed(msg, cause)
        }

    /**
     * Logique d'évolution de la confiance.
     */
    suspend fun adjustUserTrust(userId: Uuid, impact: TrustImpact): Result<User> = runCatching {
        val user = getUserProfile(userId).getOrThrow()

        val updatedUser = user.updateReputation(impact.points)

        // TODO: Règle métier supplémentaire : Promotion automatique au rôle de confiance ?
        //  On pourrait imaginer qu'un étudiant avec 95 de score devient un "Vérificateur"

        userRepository.save(updatedUser)
    }.recoverDomainError { msg, cause -> UserDomainException.PersistenceFailed(msg, cause) }

    suspend fun getUserProfile(userId: Uuid): Result<User> = runCatching {
        userRepository.findById(userId) ?: throw UserDomainException.NotFound(userId.toString())
    }.recoverDomainError { msg, cause -> UserDomainException.PersistenceFailed(msg, cause) }

}