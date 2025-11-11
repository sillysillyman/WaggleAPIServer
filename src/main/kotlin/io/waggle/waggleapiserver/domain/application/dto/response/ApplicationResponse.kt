package io.waggle.waggleapiserver.domain.application.dto.response

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.time.Instant
import java.util.UUID

data class ApplicationResponse(
    val applicationId: Long,
    val position: Position,
    val status: ApplicationStatus,
    val projectId: Long,
    val userId: UUID,
    val createdAt: Instant,
) {
    companion object {
        fun from(application: Application): ApplicationResponse =
            ApplicationResponse(
                applicationId = application.id,
                position = application.position,
                status = application.status,
                projectId = application.projectId,
                userId = application.userId,
                createdAt = application.createdAt,
            )
    }
}
