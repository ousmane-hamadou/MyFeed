package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.exception.DomainException
import com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException
import com.github.ousmane_hamadou.domain.user.Establishment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InboundSyncServiceTest : FunSpec({

    // Initialisation des mocks
    val postRepository = mockk<PostRepository>()
    val provider = mockk<ExternalInformationProvider>()
    val syncService = InboundSyncService(listOf(provider), postRepository)

    val SYSTEM_OFFICIAL_ID = Uuid.parse("00000000-0000-0000-0000-000000000000")

    // Nettoyage et config par défaut avant chaque test
    beforeTest {
        clearMocks(postRepository, provider)
        every { provider.sourceName } returns "Facebook IUT"
        every { provider.targetEstablishment } returns Establishment.IUT
    }

    test("given new external posts when syncing then should save with correct visibility and system id") {
        // Given
        val externalId = "fb_123"
        val externalPost = ExternalInboundPost(
            externalId = externalId,
            title = "Avis de concours",
            content = "Détails du concours...",
            date = Clock.System.now(),
            rawUrl = "https://facebook.com/post/123"
        )

        coEvery { provider.fetchLatestPosts() } returns Result.success(listOf(externalPost))
        coEvery { postRepository.existsByExternalId(externalId) } returns false
        coEvery { postRepository.save(any()) } returnsArgument 0

        // When
        val result = syncService.syncAllSources()

        // Then
        result.shouldBeSuccess()
        coVerify(exactly = 1) {
            postRepository.save(match {
                it.authorId == SYSTEM_OFFICIAL_ID &&
                        it.category == PostCategory.OFFICIAL &&
                        it.visibility.establishment == Establishment.IUT &&
                        it.externalId == externalId
            })
        }
    }

    test("given existing external posts when syncing then should not save duplicates") {
        // Given
        val externalId = "fb_existing"
        val externalPost = ExternalInboundPost(
            externalId = externalId,
            title = "Déjà présent",
            content = "Contenu...",
            date = Clock.System.now(),
            rawUrl = null
        )

        coEvery { provider.fetchLatestPosts() } returns Result.success(listOf(externalPost))
        coEvery { postRepository.existsByExternalId(externalId) } returns true

        // When
        syncService.syncAllSources()

        // Then
        coVerify(exactly = 0) { postRepository.save(any()) }
    }

    test("given database failure when saving then should fail with PersistenceFailed") {
        // Given
        val externalPost = ExternalInboundPost(
            externalId = "id_fail",
            title = "Titre",
            content = "Contenu",
            date = Clock.System.now(),
            rawUrl = null
        )

        coEvery { provider.fetchLatestPosts() } returns Result.success(listOf(externalPost))
        coEvery { postRepository.existsByExternalId(any()) } returns false
        coEvery { postRepository.save(any()) } throws RuntimeException("DB Error")

        // When
        val result = syncService.syncAllSources()

        // Then
        result.shouldBeFailure { exception ->
            // On vérifie que recoverDomainError a bien wrappé l'exception
            exception.shouldBeInstanceOf<DomainException.SyncDomainException.PersistenceFailed>()
            exception.cause?.message shouldContain "DB Error"
        }
    }

    test("given provider failure when syncing then should return the integration error") {
        // Given
        val integrationError = PostDomainException.ExternalIntegrationError("Network Down")
        coEvery { provider.fetchLatestPosts() } returns Result.failure(integrationError)

        // When
        val result = syncService.syncAllSources()

        // Then
        result.shouldBeFailure { exception ->
            // Vérifie que l'erreur métier du provider est remontée telle quelle
            exception.shouldBeInstanceOf<PostDomainException.ExternalIntegrationError>()
        }
    }
})