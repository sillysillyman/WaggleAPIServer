package io.waggle.waggleapiserver.domain.memberreview

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewTag
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.util.UUID

@Entity
@Table(
    name = "member_reviews",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_reviews_reviewer_reviewee_team",
            columnNames = ["reviewer_id", "reviewee_id", "team_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_member_reviews_reviewee", columnList = "reviewee_id"),
        Index(name = "idx_member_reviews_reviewer_team", columnList = "reviewer_id, team_id"),
    ],
)
class MemberReview(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "reviewer_id", nullable = false, updatable = false)
    val reviewerId: UUID,
    @Column(name = "reviewee_id", nullable = false, updatable = false)
    val revieweeId: UUID,
    @Column(name = "team_id", nullable = false, updatable = false)
    val teamId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    var type: ReviewType,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "member_review_tags",
        joinColumns = [JoinColumn(name = "member_review_id")],
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, columnDefinition = "VARCHAR(30)")
    val tags: MutableSet<ReviewTag> = mutableSetOf(),
) : AuditingEntity() {
    fun update(
        type: ReviewType,
        tags: Set<ReviewTag>,
    ) {
        this.type = type
        this.tags.clear()
        this.tags.addAll(tags)
    }
}
