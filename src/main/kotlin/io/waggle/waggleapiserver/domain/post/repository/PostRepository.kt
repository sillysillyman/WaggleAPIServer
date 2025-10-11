package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {
    @Query(
        """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.project
        WHERE p.id = :id
    """,
    )
    fun findByIdOrNull(
        @Param("id") id: Long,
    ): Post?

    @Query(
        """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.project
        WHERE p.title LIKE CONCAT('%', :query, '%')
    """,
    )
    fun findWithFilter(
        @Param("query") query: String,
        pageable: Pageable,
    ): Page<Post>
}
