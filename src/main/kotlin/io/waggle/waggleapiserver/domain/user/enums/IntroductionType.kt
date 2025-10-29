package io.waggle.waggleapiserver.domain.user.enums

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.reflect.KClass

enum class IntroductionType(
    @JsonIgnore
    private val enumClass: KClass<out Enum<*>>,
) {
    COMMUNICATION_STYLE(CommunicationStyle::class),
    COLLABORATION_STYLE(CollaborationStyle::class),
    WORK_STYLE(WorkStyle::class),
    PROBLEM_SOLVING_APPROACH(ProblemSolvingApproach::class),
    MBTI(Mbti::class),
    ;

    fun getValues(): List<Map<String, String>> =
        when (this) {
            COMMUNICATION_STYLE ->
                CommunicationStyle.entries.map {
                    mapOf("name" to it.name)
                }
            COLLABORATION_STYLE ->
                CollaborationStyle.entries.map {
                    mapOf("name" to it.name)
                }
            WORK_STYLE ->
                WorkStyle.entries.map {
                    mapOf("name" to it.name)
                }
            PROBLEM_SOLVING_APPROACH ->
                ProblemSolvingApproach.entries.map {
                    mapOf("name" to it.name)
                }
            MBTI ->
                Mbti.entries.map {
                    mapOf("name" to it.name)
                }
        }
}
