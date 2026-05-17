package io.waggle.waggleapiserver.domain.term

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.term.dto.request.TermsAgreeRequest
import io.waggle.waggleapiserver.domain.term.dto.response.TermResponse
import io.waggle.waggleapiserver.domain.term.service.TermService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Term", description = "약관 동의 API")
@RestController
@RequestMapping("/terms")
class TermController(
    private val termService: TermService,
) {
    @AllowIncompleteSetup
    @PostMapping("/agree")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "약관 동의")
    fun agreeToTerms(
        @Valid @RequestBody request: TermsAgreeRequest,
        @CurrentUser user: User,
    ) = termService.agreeToTerms(request, user)

    @AllowIncompleteSetup
    @GetMapping
    @Operation(summary = "약관 목록 조회 (현재 사용자의 동의 여부 포함)")
    fun getTerms(
        @CurrentUser user: User?,
    ): List<TermResponse> = termService.getTerms(user)
}
