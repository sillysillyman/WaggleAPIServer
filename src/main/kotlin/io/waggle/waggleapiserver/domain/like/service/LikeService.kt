package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.Like
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LikeService(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @Transactional
    fun like(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResponse {
        checkTargetExists(type, targetId)

        // 이미 눌린 상태면 INSERT 생략. save()를 무조건 부르면 merge가 일어나 created_at이 갱신됨.
        val likeId = LikeId(type = type, targetId = targetId, userId = user.id)
        if (!likeRepository.existsById(likeId)) {
            likeRepository.save(Like(likeId))
        }

        return LikeResponse.of(true, likeRepository.countByIdTypeAndIdTargetId(type, targetId))
    }

    @Transactional
    fun unlike(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResponse {
        checkTargetExists(type, targetId)

        likeRepository.deleteById(LikeId(type = type, targetId = targetId, userId = user.id))

        return LikeResponse.of(false, likeRepository.countByIdTypeAndIdTargetId(type, targetId))
    }

    // existsById는 count 쿼리라 soft delete 필터가 적용됨. findByIdOrNull(em.find)은 필터를 타지 않아 사용 불가.
    private fun checkTargetExists(
        type: LikeType,
        targetId: Long,
    ) {
        val exists =
            when (type) {
                LikeType.POST -> postRepository.existsById(targetId)
                LikeType.COMMENT -> commentRepository.existsByIdAndTombstonedAtIsNull(targetId)
            }
        if (!exists) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "$type not found: $targetId")
        }
    }
}
