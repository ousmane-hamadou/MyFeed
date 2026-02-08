# My Feed - Core

**My Feed** est une plateforme de veille collaborative conçue pour les étudiants. L'objectif est de centraliser, filtrer et fiabiliser l'information académique et associative au sein d'un établissement. 

Fini l'éparpillement entre Facebook, les mails de l'administration et les sites officiels : My Feed regroupe tout au même endroit.

## Concept

L'application repose sur trois piliers :
1.  **Agrégation Intelligente** : Récupération automatique des communiqués officiels depuis des sources externes (réseaux sociaux, portails universitaires).
2.  **Validation par les Pairs** : Les étudiants peuvent confirmer ou réfuter les informations publiées par d'autres pour éviter la propagation de fausses informations.
3.  **Indice de Confiance (Trust Score)** : Chaque utilisateur possède un score de réputation qui évolue selon la qualité et la véracité de ses contributions.



## Aperçu Technique

Le projet est développé avec une architecture **Kotlin Multiplatform (KMP)**, permettant de partager la logique métier entre différentes plateformes tout en restant robuste et maintenable.

### Stack Technologique
* **Kotlin Multiplatform** : Partage du code domaine et des règles métier.
* **Ktor** : Gestion des appels réseaux pour la récupération des flux externes via les Providers.
* **Kotlinx-datetime & UUID** : Gestion précise des données temporelles et des identifiants uniques en multiplateforme.
* **Kotest & MockK** : Suite de tests unitaires comportementaux (BDD).

### Architecture du Code
Le projet suit les principes de la **Clean Architecture** :
* **Entities** : Modèles de données purs (Post, User, Validation) sans dépendances techniques.
* **Services** : Logique métier centrale (`ValidationService`, `InboundSyncService`, `UserService`).
* **Repositories** : Interfaces définissant la persistance des données, prêtes à être implémentées côté infrastructure (SQLDelight, etc.).
* **Error Handling** : Système de gestion d'erreurs centralisé via `DomainException` et le pattern `recoverDomainError`.



## Qualité et Fiabilité
Le noyau métier est couvert par une suite de tests unitaires automatisés garantissant :
* Les transitions d'états des publications (ex: passage au statut `PUBLISHED` après un seuil de validations).
* La protection contre l'auto-validation et les doubles votes.
* La résilience de la synchronisation face aux erreurs réseau ou API.

---
*Développé avec ❤️ pour la communauté étudiante.*
