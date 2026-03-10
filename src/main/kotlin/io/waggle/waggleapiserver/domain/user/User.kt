package io.waggle.waggleapiserver.domain.user

import com.github.f4b6a3.uuid.UuidCreator
import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
    indexes = [Index(name = "idx_users_email", columnList = "email")],
)
class User(
    @Id
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(nullable = false)
    val provider: String,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 36.5")
    var temperature: Double = 36.5,
    @Column(name = "profile_image_url", columnDefinition = "VARCHAR(2083)")
    var profileImageUrl: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    var role: UserRole = UserRole.USER,
) : AuditingEntity() {
    var username: String? = null

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    var position: Position? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_skills", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, columnDefinition = "VARCHAR(50)")
    val skills: MutableSet<Skill> = mutableSetOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_portfolios", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "portfolio_url", nullable = false, columnDefinition = "VARCHAR(2048)")
    val portfolioUrls: MutableList<String> = mutableListOf()

    @Column(columnDefinition = "VARCHAR(1000)")
    var bio: String? = null

    fun setupProfile(
        username: String,
        position: Position,
        bio: String?,
        profileImageUrl: String?,
        skills: Set<Skill>,
        portfolioUrls: List<String>,
    ) {
        this.username = username
        this.position = position
        this.bio = bio
        this.profileImageUrl = profileImageUrl
        this.skills.clear()
        this.skills.addAll(skills)
        this.portfolioUrls.clear()
        this.portfolioUrls.addAll(portfolioUrls)
    }

    fun update(
        position: Position,
        bio: String?,
        profileImageUrl: String?,
        skills: Set<Skill>,
        portfolioUrls: List<String>,
    ) {
        this.position = position
        this.bio = bio
        this.profileImageUrl = profileImageUrl
        this.skills.clear()
        this.skills.addAll(skills)
        this.portfolioUrls.clear()
        this.portfolioUrls.addAll(portfolioUrls)
    }

    fun isProfileComplete(): Boolean = this.username != null && this.position != null

    fun checkProfileComplete() {
        if (!isProfileComplete()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Profile is not set up yet")
        }
    }
}
