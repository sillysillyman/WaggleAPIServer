package io.waggle.waggleapiserver.domain.user.dto.response

import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Sido
import io.waggle.waggleapiserver.domain.user.enums.WorkTime
import io.waggle.waggleapiserver.domain.user.enums.WorkWay
import java.util.UUID

data class UserDetailResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val workTime: WorkTime,
    val workWay: WorkWay,
    val sido: Sido,
    val position: Position,
    val yearCount: Int?,
    val detail: String?,
) {
    companion object {
        fun from(user: User): UserDetailResponse =
            UserDetailResponse(
                user.id,
                user.username!!,
                user.email,
                user.profileImageUrl,
                user.workTime!!,
                user.workWay!!,
                user.sido!!,
                user.position!!,
                user.yearCount,
                user.detail,
            )
    }
}
