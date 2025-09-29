package io.waggle.waggleapiserver.common.dto

data class ArrayResponse<T>(
    val data: Sequence<T>,
)
