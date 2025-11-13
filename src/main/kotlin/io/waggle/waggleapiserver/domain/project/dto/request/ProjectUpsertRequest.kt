package io.waggle.waggleapiserver.domain.project.dto.request

import jakarta.validation.constraints.NotBlank

data class ProjectUpsertRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val description: String,
    val thumbnailUrl: String?,
)
