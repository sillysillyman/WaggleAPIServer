package io.waggle.waggleapiserver.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import org.hibernate.annotations.SQLDelete
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = NOW() WHERE id = ?")
@FilterDef(
    name = "deletedFilter",
    parameters = [ParamDef(name = "isDeleted", type = Boolean::class)],
)
@Filter(
    name = "deletedFilter",
    condition = "(:isDeleted = false AND deleted_at IS NULL) OR (:isDeleted = true)",
)
abstract class AuditingEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @LastModifiedDate
    @Column(nullable = false)
    lateinit var updatedAt: Instant

    var deletedAt: Instant? = null
}
