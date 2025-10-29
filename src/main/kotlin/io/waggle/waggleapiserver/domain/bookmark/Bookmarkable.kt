package io.waggle.waggleapiserver.domain.bookmark

interface Bookmarkable {
    val bookmarkableId: Long
    val bookmarkType: BookmarkType
}
