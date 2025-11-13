package io.waggle.waggleapiserver.domain.member

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/members")
@RestController
class MemberController(
    private val memberService: MemberService,
) {
    @PatchMapping("/{memberId}")
    fun updateMemberRole(
        @PathVariable memberId: Long,
        @RequestBody request: MemberUpdateRoleRequest,
        @CurrentUser user: User,
    ): MemberResponse = memberService.updateMemberRole(memberId, request, user)

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        @PathVariable memberId: Long,
        @CurrentUser user: User,
    ) {
        memberService.removeMember(memberId, user)
    }
}
