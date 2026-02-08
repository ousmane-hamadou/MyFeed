package com.github.ousmane_hamadou.domain.user

enum class Department(val establishment: Establishment) {
    // Faculté des Sciences (FS)
    BIOMEDICAL_SCIENCES(Establishment.FS),
    RADIOLOGY(Establishment.FS),
    PUBLIC_HEALTH(Establishment.FS),
    BIOLOGY(Establishment.FS),
    BIOCHEMISTRY(Establishment.FS),
    CHEMISTRY(Establishment.FS),
    GEOLOGY(Establishment.FS),
    COMPUTER_SCIENCE(Establishment.FS),
    MATHEMATICS(Establishment.FS),

    // Institut Universitaire de Technologie (IUT)
    FOOD_GENIUS(Establishment.IUT),
    CHEMICAL_GENIUS(Establishment.IUT),
    ELECTRICAL_GENIUS(Establishment.IUT),
    IT_GENIUS(Establishment.IUT),
    MECHANICAL_GENIUS(Establishment.IUT),
    INDUSTRIAL_MAINTENANCE(Establishment.IUT),

    // ENSAI
    AGRO_INDUSTRY(Establishment.ENSAI),
    PROCESS_ENGINEERING(Establishment.ENSAI),
    FOOD_INDUSTRIES(Establishment.ENSAI),

    // FSJP
    PRIVATE_LAW(Establishment.FSJP),
    PUBLIC_LAW(Establishment.FSJP),
    INTERNATIONAL_LAW(Establishment.FSJP),
    POLITICAL_SCIENCE(Establishment.FSJP),
    INTERNATIONAL_RELATIONS(Establishment.FSJP),

    // FSEG
    ACCOUNTING(Establishment.FSEG),
    FINANCE(Establishment.FSEG),
    MANAGEMENT(Establishment.FSEG),
    MARKETING(Establishment.FSEG),
    ECONOMY(Establishment.FSEG),

    // FLSH
    GEOGRAPHY(Establishment.FLSH),
    HISTORY(Establishment.FLSH),
    SOCIOLOGY(Establishment.FLSH),
    PSYCHOLOGY(Establishment.FLSH),
    LANGUAGES(Establishment.FLSH),
    URBANISM(Establishment.FLSH),

    // EMVT
    VETERINARY_SCIENCES(Establishment.EMVT),
    ANIMAL_HEALTH(Establishment.EMVT)
}