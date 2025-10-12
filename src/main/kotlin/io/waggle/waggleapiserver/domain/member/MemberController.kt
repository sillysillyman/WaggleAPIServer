package io.waggle.waggleapiserver.domain.member

import io.waggle.waggleapiserver.domain.member.service.MemberService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/members")
@RestController
class MemberController(
    private val memberService: MemberService,
)
