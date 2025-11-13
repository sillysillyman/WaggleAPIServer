package io.waggle.waggleapiserver.domain.invitation.repository

import io.waggle.waggleapiserver.domain.invitation.Invitation
import org.springframework.data.jpa.repository.JpaRepository

interface InvitationRepository : JpaRepository<Invitation, Long>
