package com.github.ousmane_hamadou.domain.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface UserRepository {
    suspend fun findById(id: Uuid): User?
    suspend fun findByMatricule(matricule: String): User?
    suspend fun save(user: User): User
    suspend fun delete(id: Uuid)
    suspend fun findAllByDepartment(department: Department): List<User>
}

