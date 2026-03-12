package io.waggle.waggleapiserver.domain.user

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.exp
import kotlin.math.ln

/**
 * 곰페르츠 곡선(Gompertz curve)을 사용한 사용자 온도 계산
 *
 * 수식: temperature = MAX * exp(-BASE_OFFSET * exp(-sensitivity * x))
 * - x: likeCount - dislikeCount
 * - BASE_OFFSET: x=0일 때 기준 온도 36.5도를 만드는 값
 * - sensitivity: 곡선의 기울기를 조정하는 파라미터
 */
@Component
class TemperatureCalculator(
    @Value("\${app.temperature.sensitivity}") private val sensitivity: Double,
) {
    fun calculate(
        likeCount: Long,
        dislikeCount: Long,
    ): Double {
        val x = (likeCount - dislikeCount).toDouble()
        return MAX * exp(-BASE_OFFSET * exp(-sensitivity * x))
    }

    companion object {
        private const val MAX = 100.0
        private val BASE_OFFSET = -ln(36.5 / 100.0)
    }
}
