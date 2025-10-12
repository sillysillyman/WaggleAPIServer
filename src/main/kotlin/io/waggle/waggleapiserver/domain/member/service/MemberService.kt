package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
)
