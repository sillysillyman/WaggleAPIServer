package io.waggle.waggleapiserver.domain.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.auth.dto.request.OttRedeemRequest
import io.waggle.waggleapiserver.domain.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.domain.auth.dto.response.WsTokenResponse
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증 토큰")
@RequestMapping("/auth")
@RestController
class AuthController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "OAuth OTT 교환",
        description = "OAuth 콜백 후 받은 1회용 토큰(OTT)을 액세스 토큰으로 교환함. OTT는 1분 후 만료되며 1회 사용 시 폐기됨.",
    )
    @PostMapping("/oauth/redeem")
    fun redeemOtt(
        @Valid @RequestBody request: OttRedeemRequest,
    ): AccessTokenResponse = authService.redeemOtt(request.ott)

    @Operation(
        summary = "액세스 토큰 재발급",
        description = "유효한 리프레시 토큰으로 액세스 토큰을 재발급함",
    )
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refreshToken") refreshToken: String,
        response: HttpServletResponse,
    ): AccessTokenResponse = authService.refresh(refreshToken, response)

    @Operation(
        summary = "로그아웃",
        description = "리프레시 토큰 무효화",
    )
    @PostMapping("/logout")
    fun logout(
        @CookieValue("refreshToken") refreshToken: String,
        response: HttpServletResponse,
    ) {
        authService.logout(refreshToken, response)
    }

    @Operation(
        summary = "WebSocket 연결 토큰 발급",
        description = "일회용 WebSocket 연결 토큰을 발급함 (1분 유효)",
    )
    @PostMapping("/ws-token")
    fun issueWsToken(
        @CurrentUser user: User,
    ): WsTokenResponse = authService.issueWsToken(user.id)
}
