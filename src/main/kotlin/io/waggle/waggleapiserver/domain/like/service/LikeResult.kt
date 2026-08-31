package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse

// PUT이 실제로 행을 생성했는지를 컨트롤러에 전달해 201/200을 가름. 응답 본문에는 노출되지 않음.
data class LikeResult(
    val created: Boolean,
    val response: LikeResponse,
)
