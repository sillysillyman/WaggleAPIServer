package io.waggle.waggleapiserver.domain.member.dto.response

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant
import java.util.UUID

data class MemberResponse(
    val memberId: Long,
    val userId: UUID,
    val role: MemberRole,
    val username: String,
    val profileImageUrl: String?,
    val createdAt: Instant,
) {
    companion object {
        fun of(
            member: Member,
            user: User,
        ): MemberResponse =
            MemberResponse(
                memberId = member.id,
                userId = member.userId,
                role = member.role,
                username = user.username!!,
                profileImageUrl = user.profileImageUrl,
                createdAt = member.createdAt,
            )
    }
}
