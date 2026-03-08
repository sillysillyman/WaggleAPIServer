package io.waggle.waggleapiserver.domain.post.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import java.time.Instant

@Schema(description = "모집글 응답 DTO")
data class PostSimpleResponse(
    @Schema(description = "모집글 ID", example = "1")
    val postId: Long,
    @Schema(description = "모집글 제목", example = "와글에서 기획자 구인합니다")
    val title: String,
    @Schema(description = "작성자 정보")
    val user: UserSimpleResponse,
    @Schema(description = "모집 중 여부")
    val isRecruiting: Boolean,
    @Schema(description = "모집 정보 목록")
    val recruitments: List<RecruitmentResponse>,
    @Schema(description = "지원자 수 (팀 멤버만 조회 가능)")
    val applicantCount: Int? = null,
    @Schema(description = "모집글 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "모집글 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            post: Post,
            user: UserSimpleResponse,
            recruitments: List<RecruitmentResponse> = emptyList(),
            applicantCount: Int? = null,
        ): PostSimpleResponse =
            PostSimpleResponse(
                postId = post.id,
                title = post.title,
                user = user,
                isRecruiting = recruitments.any { it.status == RecruitmentStatus.RECRUITING },
                recruitments = recruitments,
                applicantCount = applicantCount,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
    }
}
