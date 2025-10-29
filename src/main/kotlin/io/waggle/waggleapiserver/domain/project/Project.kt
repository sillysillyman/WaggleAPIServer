package io.waggle.waggleapiserver.domain.project

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "projects",
    indexes = [Index(name = "idx_projects_name", columnList = "name")],
)
class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true, nullable = false)
    var name: String,
    @Column(nullable = false)
    var description: String,
    @Column(name = "leader_id", nullable = false)
    var leaderId: UUID,
    @Column(name = "creator_id", nullable = false, updatable = false)
    val creatorId: UUID,
) : AuditingEntity(),
    Bookmarkable {
    override val bookmarkableId: Long get() = id
    override val bookmarkType: BookmarkType = BookmarkType.PROJECT

    fun update(
        name: String,
        description: String,
    ) {
        this.name = name
        this.description = description
    }

    fun isLeader(userId: UUID) = leaderId == userId
}
