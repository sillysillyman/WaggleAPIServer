package io.waggle.waggleapiserver.common.dto.response

data class CursorResponse<T>(
    val data: List<T>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)
