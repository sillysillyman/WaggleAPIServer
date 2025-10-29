package io.waggle.waggleapiserver.domain.post.dto.request

import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PostUpsertRequest(
    val projectId: Long?,
    @field:NotBlank val title: String,
    @field:NotBlank val content: String,
    @field:NotNull val recruitments: List<RecruitmentUpsertRequest>,
)
