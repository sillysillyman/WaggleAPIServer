package io.waggle.waggleapiserver.common

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant

@MappedSuperclass
abstract class BaseEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private val createdAt: Instant? = null

    @LastModifiedDate
    @Column(nullable = false)
    private var updatedAt: Instant? = null
}
