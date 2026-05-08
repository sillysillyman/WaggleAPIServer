package io.waggle.waggleapiserver.domain.memberreview.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.memberreview.MemberReview
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewTag
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant
import java.util.UUID

@Schema(description = "팀원 리뷰 응답 DTO")
data class MemberReviewResponse(
    @Schema(description = "리뷰 ID", example = "1")
    val id: Long,
    @Schema(description = "리뷰 대상 정보")
    val reviewee: RevieweeResponse,
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "리뷰 타입", example = "LIKE")
    val type: ReviewType,
    @Schema(description = "리뷰 태그 목록")
    val tags: Set<ReviewTag>,
    @Schema(description = "리뷰 생성일시")
    val createdAt: Instant,
    @Schema(description = "리뷰 수정일시")
    val updatedAt: Instant,
) {
    @Schema(description = "리뷰 대상 정보")
    data class RevieweeResponse(
        @Schema(description = "사용자 ID")
        val id: UUID,
        @Schema(description = "사용자명", example = "reviewee")
        val username: String,
    ) {
        companion object {
            fun from(user: User): RevieweeResponse =
                RevieweeResponse(
                    id = user.id,
                    username = user.username!!,
                )
        }
    }

    companion object {
        fun of(
            review: MemberReview,
            reviewee: User,
        ) = MemberReviewResponse(
            id = review.id,
            reviewee = RevieweeResponse.from(reviewee),
            teamId = review.teamId,
            type = review.type,
            tags = review.tags.toSet(),
            createdAt = review.createdAt,
            updatedAt = review.updatedAt,
        )
    }
}
