package com.github.ousmane_hamadou.domain.exception

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> Result<T>.recoverDomainError(
    crossinline factory: (String, Throwable) -> DomainException
): Result<T> {
    return recoverCatching { exception ->
        // 1. Priorité absolue : laisser filer l'annulation
        if (exception is CancellationException) throw exception

        // 2. Si c'est déjà une exception de notre domaine, on ne la re-transforme pas
        // On la relance pour que recoverCatching la capture dans le Result final
        if (exception is DomainException) throw exception

        // 3. Sinon, on utilise la factory pour l'envelopper (ex: PersistenceFailed)
        throw factory(exception.message ?: "Unknown error", exception)
    }
}

/**
 * Hiérarchie scellée pour une gestion exhaustive des erreurs du domaine.
 */
sealed class DomainException(
    override val message: String, override val cause: Throwable? = null
) : Throwable(message, cause) {

    // Erreurs liées aux Utilisateurs
    sealed class UserDomainException(msg: String, cause: Throwable? = null) :
        DomainException(msg, cause) {
        class AlreadyExists(matricule: String) :
            UserDomainException("An account with matricule $matricule already exists.")

        class NotFound(userId: String) : UserDomainException("User with ID $userId was not found.")

        class UnauthorizedAdminAction(adminId: String) :
            UserDomainException("Action denied: User $adminId does not have administrative privileges.")

        class TrustAdjustment(userId: String, reason: String) :
            UserDomainException("Could not adjust trust score for user $userId: $reason")

        class UpdateFailed(message: String, cause: Throwable? = null) :
            UserDomainException("Failed to update user data: $message", cause)

        class PersistenceFailed(message: String, cause: Throwable? = null) :
            UserDomainException("Database persistence error: $message", cause)
    }

    // Dans DomainException.kt
    sealed class PostDomainException(message: String, cause: Throwable? = null) :
        DomainException(message, cause) {
        class AuthorNotFound(
            message: String,
            cause: Throwable? = null
        ) : PostDomainException(message, cause)

        class NotFound(postId: String) : PostDomainException("Post with ID $postId was not found.")
        class UnauthorizedAction(userId: String) :
            PostDomainException("User $userId is not authorized to perform this action on the post.")

        class ContentInvalid(reason: String) :
            PostDomainException("Post content is invalid: $reason")

        class PersistenceFailed(message: String, cause: Throwable? = null) :
            PostDomainException("Post database error: $message", cause)

        /**
         * Erreur lors de la récupération de données depuis un fournisseur externe (API, RSS, etc.)
         */
        class ExternalIntegrationError(
            message: String,
            cause: Throwable? = null
        ) : PostDomainException(message, cause)
    }

    sealed class ReportDomainException(message: String, cause: Throwable? = null) :
        DomainException(message, cause) {
        class NotFound(id: String) : ReportDomainException("Report $id was not found.")
        class Duplicate(userId: String, postId: String) :
            ReportDomainException("User $userId already reported post $postId.")

        class ActionFailed(message: String, cause: Throwable? = null) :
            ReportDomainException(message, cause)

        class PersistenceFailed(message: String, cause: Throwable? = null) :
            ReportDomainException("Report storage error: $message", cause)
    }

    sealed class SyncDomainException(message: String, cause: Throwable? = null) :
        DomainException(message, cause) {
        class ProviderError(source: String, cause: Throwable?) :
            SyncDomainException("Failed to fetch data from provider: $source", cause)

        class PersistenceFailed(message: String, cause: Throwable? = null) :
            SyncDomainException("Failed to save external post: $message", cause)

        class GeneralSyncError(message: String, cause: Throwable?) :
            SyncDomainException("Global synchronization process failed", cause)
    }

    sealed class ValidationDomainException(message: String, cause: Throwable? = null) :
        DomainException(message, cause) {
        class DoubleValidation(userId: String, postId: String) :
            ValidationDomainException("User $userId has already validated post $postId.")

        class SelfValidation :
            ValidationDomainException("An author cannot validate their own post.")

        class ActionFailed(message: String, cause: Throwable? = null) :
            ValidationDomainException(message, cause)

        class PersistenceFailed(message: String, cause: Throwable? = null) :
            ValidationDomainException("Validation storage error: $message", cause)
    }
}