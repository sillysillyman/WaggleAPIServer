package io.waggle.waggleapiserver.common.dto.request

data class CursorGetQuery(
    val cursor: Long?,
    val size: Int = 20,
)
