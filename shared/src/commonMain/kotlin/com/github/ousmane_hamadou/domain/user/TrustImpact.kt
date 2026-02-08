package com.github.ousmane_hamadou.domain.user

/**
 * Définit les types d'impacts sur la réputation dans le système Wanda.
 */
enum class TrustImpact(val points: Int) {
    FAKE_NEWS_PUBLISHED(-10),
    HARASSMENT_DETECTED(-50),
    STRICT_VIOLATION(-100),
    POSITIVE_CONTRIBUTION(5),
    REPORT_VALIDATED(2) // Récompense pour un bon signalement
}