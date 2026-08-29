package io.waggle.waggleapiserver.domain.like

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class LikeId(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(20)")
    val type: LikeType,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
) : Serializable

@Entity
@Table(
    name = "likes",
    indexes = [Index(name = "idx_likes_user", columnList = "user_id")],
)
class Like(
    @EmbeddedId
    val id: LikeId,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}
