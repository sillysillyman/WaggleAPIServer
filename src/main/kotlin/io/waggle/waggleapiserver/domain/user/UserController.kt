package io.waggle.waggleapiserver.domain.user

import io.waggle.waggleapiserver.common.dto.SingleItemResponse
import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/users")
@RestController
class UserController(
    private val userService: UserService,
) {
    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UserUpdateRequest,
        @CurrentUser user: User,
    ): ResponseEntity<SingleItemResponse<UserSimpleResponse>> =
        ResponseEntity.ok(
            SingleItemResponse(
                userService.updateUser(
                    user.id,
                    request,
                ),
            ),
        )
}
