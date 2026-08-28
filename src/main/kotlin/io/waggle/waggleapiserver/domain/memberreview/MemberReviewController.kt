package io.waggle.waggleapiserver.domain.memberreview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.memberreview.dto.request.MemberReviewUpsertRequest
import io.waggle.waggleapiserver.domain.memberreview.dto.response.MemberReviewResponse
import io.waggle.waggleapiserver.domain.memberreview.service.MemberReviewService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀원 리뷰")
@RequestMapping("/members/{memberId}/reviews")
@RestController
class MemberReviewController(
    private val memberReviewService: MemberReviewService,
) {
    @Operation(summary = "팀원 리뷰 작성/수정")
    @PutMapping
    fun upsertReview(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: MemberReviewUpsertRequest,
        @CurrentUser user: User,
    ): MemberReviewResponse = memberReviewService.upsertReview(memberId, request, user)
}
