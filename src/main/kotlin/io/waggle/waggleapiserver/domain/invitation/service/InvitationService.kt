package io.waggle.waggleapiserver.domain.invitation.service

import io.waggle.waggleapiserver.domain.invitation.repository.InvitationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InvitationService(
    private val invitationRepository: InvitationRepository,
)
