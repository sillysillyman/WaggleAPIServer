package io.waggle.waggleapiserver.domain.recruitment

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "recruitments")
class Recruitment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val position: Position,
    @Column(nullable = false)
    var currentCount: Int = 0,
    @Column(nullable = false)
    val recruitingCount: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    val project: Project,
) : AuditingEntity()
