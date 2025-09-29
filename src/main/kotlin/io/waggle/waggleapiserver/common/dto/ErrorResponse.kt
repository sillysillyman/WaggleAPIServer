package io.waggle.waggleapiserver.common.dto

data class ErrorResponse(
    val code: Int = 500,
    val title: String,
    val detail: String,
)
