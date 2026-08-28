package io.waggle.waggleapiserver.domain.post.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.post.dto.response.TeamPostSimpleResponse
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "모집글")
@RequestMapping("/teams/{teamId}/posts")
@RestController
class TeamPostController(
    private val postService: PostService,
) {
    @AllowIncompleteSetup
    @Operation(summary = "팀 모집글 목록 조회")
    @GetMapping
    fun getTeamPosts(
        @PathVariable teamId: Long,
        @CurrentUser user: User?,
    ): List<TeamPostSimpleResponse> = postService.getTeamPosts(teamId, user)
}
