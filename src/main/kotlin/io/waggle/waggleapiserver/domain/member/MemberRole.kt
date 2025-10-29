package io.waggle.waggleapiserver.domain.member

enum class MemberRole(
    val level: Int,
) {
    MEMBER(level = 1),
    MANAGER(level = 2),
    LEADER(level = 3),
}
