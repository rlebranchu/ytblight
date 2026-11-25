package fr.rlebranchu.ytblight.external.dto

import kotlinx.serialization.Serializable

/** Corps des réponses d'erreur de l'API YouTube. */
@Serializable
data class ApiErrorEnvelopeDto(
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val message: String? = null,
    val errors: List<ApiErrorDetailDto> = emptyList(),
)

@Serializable
data class ApiErrorDetailDto(
    val reason: String? = null,
)
