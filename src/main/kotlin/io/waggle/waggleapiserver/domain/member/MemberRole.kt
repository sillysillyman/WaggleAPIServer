package io.waggle.waggleapiserver.domain.member

enum class MemberRole(
    val level: Int,
    val permissions: Set<Permission>,
) {
    MEMBER(
        level = 1,
        permissions = setOf(Permission.CREATE_POST),
    ),
    MANAGER(
        level = 2,
        permissions =
            setOf(
                Permission.CREATE_POST,
                Permission.MANAGE_MEMBERS,
                Permission.DELETE_POST,
            ),
    ),
    LEADER(
        level = 3,
        permissions =
            setOf(
                Permission.CREATE_POST,
                Permission.MANAGE_MEMBERS,
                Permission.DELETE_POST,
                Permission.DELETE_PROJECT,
                Permission.MODIFY_PROJECT,
            ),
    ),
    ;

    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)

    fun isHigherThan(other: MemberRole): Boolean = this.level > other.level

    fun isEqualOrHigherThan(other: MemberRole): Boolean = this.level >= other.level

    enum class Permission {
        CREATE_POST,
        MANAGE_MEMBERS,
        DELETE_POST,
        DELETE_PROJECT,
        MODIFY_PROJECT,
    }
}
