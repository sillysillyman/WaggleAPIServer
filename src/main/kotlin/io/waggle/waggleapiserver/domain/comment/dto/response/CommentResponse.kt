package io.waggle.waggleapiserver.domain.comment.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.comment.Comment
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import java.time.Instant
import java.util.UUID

@Schema(description = "댓글 응답 DTO")
data class CommentResponse(
    @Schema(description = "댓글 ID", example = "1")
    val id: Long,
    @Schema(description = "모집글 ID", example = "1")
    val postId: Long,
    @Schema(description = "부모 댓글 ID (최상위 댓글이면 null)", example = "10")
    val parentId: Long?,
    @Schema(description = "댓글 본문 (삭제된 댓글이면 null)")
    val content: String?,
    @Schema(description = "작성자 정보 (삭제된 댓글이면 null)")
    val user: UserSimpleResponse?,
    @Schema(description = "삭제된 댓글 여부 — true면 '삭제된 댓글입니다'로 렌더링")
    val tombstoned: Boolean,
    @Schema(description = "답글 목록 (답글 자신은 항상 빈 목록)")
    val replies: List<CommentResponse>,
    @Schema(description = "댓글 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "댓글 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    companion object {
        // 작성자가 null인 게 정상인지(=tombstone인지)는 호출자가 조회 시점에 이미 검증함.
        fun of(
            root: Comment,
            replies: List<Comment>,
            userById: Map<UUID, User>,
        ): CommentResponse =
            of(
                root,
                userById[root.userId]?.let { UserSimpleResponse.from(it) },
                replies.map { reply ->
                    of(reply, userById[reply.userId]?.let { UserSimpleResponse.from(it) })
                },
            )

        fun of(
            comment: Comment,
            user: UserSimpleResponse?,
            replies: List<CommentResponse> = emptyList(),
        ): CommentResponse =
            CommentResponse(
                id = comment.id,
                postId = comment.postId,
                parentId = comment.parentId,
                content = if (comment.isTombstoned) null else comment.content,
                user = if (comment.isTombstoned) null else user,
                tombstoned = comment.isTombstoned,
                replies = replies,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
    }
}
