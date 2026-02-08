package com.github.ousmane_hamadou.domain.post

import com.github.ousmane_hamadou.domain.user.Establishment

/**
 * Interface représentant une source de données externe (Facebook, RSS, API Université).
 */
interface ExternalInformationProvider {
    /**
     * Nom lisible de la source (ex: "Facebook IUT", "Portail Scolarité").
     */
    val sourceName: String

    /**
     * L'établissement auquel les posts importés seront rattachés.
     */
    val targetEstablishment: Establishment

    /**
     * Récupère les derniers posts depuis la source externe.
     * * @return Un [Result] contenant la liste des [ExternalInboundPost].
     * En cas d'erreur (réseau, parsing), il doit retourner un échec contenant
     * une [com.github.ousmane_hamadou.domain.exception.DomainException.PostDomainException.ExternalIntegrationError].
     */
    suspend fun fetchLatestPosts(): Result<List<ExternalInboundPost>>
}