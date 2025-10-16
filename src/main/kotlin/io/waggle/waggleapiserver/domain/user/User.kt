package io.waggle.waggleapiserver.domain.user

import com.github.f4b6a3.uuid.UuidCreator
import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Sido
import io.waggle.waggleapiserver.domain.user.enums.WorkTime
import io.waggle.waggleapiserver.domain.user.enums.WorkWay
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
    indexes = [Index(name = "idx_email", columnList = "email")],
)
class User(
    @Id val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(nullable = false, length = 20) val provider: String,
    @Column(name = "provider_id", nullable = false) val providerId: String,
    @Column(nullable = false) val email: String,
    @Column(name = "profile_image_url", nullable = false) var profileImageUrl: String?,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var role: UserRole = UserRole.USER,
) : AuditingEntity() {
    var username: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "work_time")
    var workTime: WorkTime? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "work_way")
    var workWay: WorkWay? = null

    @Enumerated(EnumType.STRING)
    var sido: Sido? = null

    @Enumerated(EnumType.STRING)
    var position: Position? = null

    @Column(name = "year_count")
    var yearCount: Int? = null

    @Column(columnDefinition = "TEXT")
    var detail: String? = null

    fun update(
        username: String,
        workTime: WorkTime,
        workWay: WorkWay,
        sido: Sido,
        position: Position,
        yearCount: Int?,
        detail: String?,
    ) {
        this.username = username
        this.workTime = workTime
        this.workWay = workWay
        this.sido = sido
        this.position = position
        this.yearCount = yearCount
        this.detail = detail
    }
}
