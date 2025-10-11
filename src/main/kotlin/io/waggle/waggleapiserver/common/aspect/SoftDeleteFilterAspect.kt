package io.waggle.waggleapiserver.common.aspect

import jakarta.persistence.EntityManager
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.hibernate.Session
import org.springframework.stereotype.Component

@Aspect
@Component
class SoftDeleteFilterAspect(
    private val entityManager: EntityManager,
) {
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)",
    )
    fun applySoftDeleteFilter() {
        val session = entityManager.unwrap(Session::class.java)
        val filter = session.enableFilter("deletedFilter")
        filter.setParameter("isDeleted", false)
    }
}
