package io.waggle.waggleapiserver.common.dto

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                data = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
            )
    }
}
