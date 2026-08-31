# 좋아요 기능 설계

모집글(Post)과 댓글(Comment)에 좋아요를 누르는 기능. **이번 범위는 토글 API, 조회 응답의 좋아요 수·여부 노출, cascade 정리**다.

알림은 기획에 없어 구현하지 않는다. 부록 참조.

## 1. 설계 결정 요약

| 항목 | 결정 | 근거 |
|---|---|---|
| 좋아요 대상 | **모집글·댓글 공용 `likes(type, target_id, user_id)`** | 대상이 처음부터 둘. `comment-feature-spec.md`의 "대상이 늘면 그때 승격" 규칙에서 지금이 그때 |
| 마커 인터페이스 | **만들지 않음.** `type`은 컨트롤러가 주입 | `Bookmarkable` 구현이 유령 컬럼 사고를 냈고 CLAUDE.md §2에 경고로 남았다. 같은 지뢰를 하나 더 놓지 않는다 |
| 토글 방식 | `PUT`/`DELETE` 분리 | 멱등. 토글 `POST`는 재시도 시 상태가 뒤집힌다 (2.3) |
| 경로 | `/posts/{postId}/like`, `/comments/{commentId}/like` | 좋아요엔 전역 고유 id가 없고 `(대상, 나)`로만 식별되므로 부모 경로가 유일한 식별 정보. shallow nesting 규칙(§3)의 "중복 정보" 사유에 해당하지 않음 |
| 삭제 | hard delete | soft delete가 필요 없는 관계 데이터. `bookmarks` 전례 |
| 좋아요 수 | 조회 시점 `GROUP BY` 집계 | 비정규화 컬럼은 쓰기 경합과 cascade 재계산 정합성을 떠안는다. 목록당 추가 쿼리 2회 고정 |
| 탈퇴 복원 | **복원하지 않음** | `bookmarks` 전례. `follows`처럼 되살리려면 `deleted_at`과 reactivate 경로가 필요 |
| 알림 | 이번 범위 제외 | 기획에 없음 |

## 2. API

### 2.1 엔드포인트

```
PUT    /posts/{postId}/like        PostLikeController
DELETE /posts/{postId}/like        PostLikeController
PUT    /comments/{commentId}/like  CommentLikeController
DELETE /comments/{commentId}/like  CommentLikeController
```

두 컨트롤러 모두 `domain/like/controller/`에 두고 **같은 `@Tag(name = "좋아요")`** 를 준다 (CLAUDE.md §1, §3). 부모 컨트롤러에 얹으면 Swagger에서 "모집글"·"댓글" 태그로 쪼개진다.

본문은 네 메서드 모두 `LikeResponse`이고, 상태 코드만 갈린다.

| 요청 | 상태 | 본문 |
|---|---|---|
| `PUT` — 좋아요가 없던 상태 | **201 Created** | `{ "liked": true, "likeCount": 42 }` |
| `PUT` — 이미 좋아요한 상태 | **200 OK** | `{ "liked": true, "likeCount": 42 }` |
| `DELETE` | **200 OK** | `{ "liked": false, "likeCount": 41 }` |

### 2.2 상태 코드를 `201`/`200`으로 가르는 이유

RFC 9110 §9.3.4는 `PUT`이 없던 리소스를 생성하면 원 서버가 **201**로, 기존 표현을 대체했으면 200이나 204로 응답하도록 정한다. 201은 `POST` 전용이 아니다 — §15.3.2의 정의도 "새 리소스가 생성됨"일 뿐 메서드를 가리지 않는다.

`PUT`을 고른 근거 자체가 메서드 의미론(2.3)이므로, 같은 명세의 상태 코드 규칙만 무시하면 앞뒤가 맞지 않는다. 비용도 작다 — 서비스가 멱등 처리를 위해 이미 `existsById`로 생성 여부를 알고 있어 그 값을 위로 올리기만 하면 된다 (6장 `LikeResult`).

**멱등성은 그대로다.** 멱등성은 *결과 상태*가 같음을 뜻하지 응답이 바이트 단위로 같아야 한다는 뜻이 아니다. `PUT`을 두 번 보내면 상태도 본문도 동일하고 상태 코드만 201 → 200으로 바뀐다.

### 2.2.1 `204`를 쓰지 않는 이유

구조가 같은 GitHub star API(`PUT`/`DELETE /user/starred/{owner}/{repo}`)는 둘 다 **204 No Content**를 준다. 우리가 본문을 싣는 것은 `likeCount`를 함께 돌려줘 클라이언트의 재조회를 없애기 위해서다. 이 API의 실질 가치가 거기 있으므로 204는 채택하지 않는다.

### 2.3 토글 `POST`를 쓰지 않는 이유

좋아요는 "이 대상에 대한 내 좋아요"라는 리소스의 생성·삭제다. `PUT`/`DELETE`는 멱등이라 몇 번을 보내도 결과 상태가 같다. 토글 `POST`는 비멱등이라 **네트워크 타임아웃 후 재시도하면 상태가 뒤집히고**, 서버는 재시도인지 두 번 누른 것인지 구분할 수 없다. 더블 탭도 같은 문제다.

`bookmarks`·`follows`가 토글 `POST`를 쓰고 있어 모양이 달라지지만, 그건 컨벤션 취향이고 이건 실제 결함 가능성이다.

### 2.4 프로필 완성 가드

`@CurrentUser user: User` (non-null)를 쓰므로 resolver 부수효과로 자동 적용된다 (CLAUDE.md §3 표). 미완성 프로필 사용자는 좋아요를 누를 수 없다 — 지원·작성과 같은 기준.

## 3. 스키마

### 3.1 마이그레이션 `V25__create_likes.sql`

`V24`는 `create_comments.sql`이 점유했다.

```sql
-- 모집글·댓글 좋아요. 대상은 (type, target_id)로 지정하며 FK 미설정 (bookmarks 전례, 정리는 도메인 이벤트 담당).
-- 토글 OFF는 hard delete. soft delete가 불필요한 관계 데이터라 deleted_at 미보유.
CREATE TABLE likes
(
    type       VARCHAR(20) NOT NULL,
    target_id  BIGINT      NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (type, target_id, user_id)
);

-- 탈퇴 cascade와 "내가 누른 좋아요" 배치 조회용.
-- InnoDB가 PK 나머지(type, target_id)를 순서대로 덧붙여 실질 (user_id, type, target_id) 커버링 인덱스가 됨.
CREATE INDEX idx_likes_user ON likes (user_id);
```

### 3.2 PK 컬럼 순서를 `(type, target_id, user_id)`로 두는 이유

카운트 집계 `WHERE type = 'POST' AND target_id IN (...)`와 존재확인 `WHERE type = ? AND target_id = ? AND user_id = ?`가 **둘 다 PK prefix로 커버**된다.

`bookmarks`는 PK가 `(target_id, user_id, type)`라 `type` 선행 조회가 PK로 안 풀려 `idx_bookmarks_type_target`을 따로 만들었다. 처음부터 `type`을 앞에 두면 그 여분 인덱스가 필요 없다.

### 3.3 `idx_likes_user` 하나로 충분한 이유

"내가 누른 좋아요" 배치 조회는 `WHERE user_id = ? AND type = ? AND target_id IN (...)`다. InnoDB는 secondary index에 PK 컬럼을 PK 순서대로 덧붙이므로 이 인덱스는 실질 `(user_id, type, target_id)`가 되어 equality 2개 + `IN` 범위까지 전부 커버한다. `(user_id, type, target_id)`를 명시적으로 만들 필요가 없다 (CLAUDE.md §2의 근거와 같은 원리).

## 4. 엔티티 — `domain/like/`

```kotlin
@Embeddable
data class LikeId(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(20)")
    val type: LikeType,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
) : Serializable

@Entity
@Table(name = "likes")
class Like(
    @EmbeddedId
    val id: LikeId,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}
```

```kotlin
enum class LikeType {
    POST,
    COMMENT,
}
```

- `AuditingEntity`를 상속하지 않는다. hard delete이고 `updated_at`이 없다 (`Bookmark`와 같은 예외 사유, CLAUDE.md §2).
- `@Access(AccessType.FIELD)`를 붙이지 않는다 (CLAUDE.md §2).
- 비영속 편의 프로퍼티는 두지 않는다. 필요해지면 반드시 `get() =`로 쓴다.

## 5. 리포지토리 — `domain/like/repository/LikeRepository.kt`

메서드는 반환 타입 순으로 선언한다 (CLAUDE.md §1). 프로젝션은 같은 파일 아래.

```kotlin
interface LikeRepository : JpaRepository<Like, LikeId> {
    fun countByIdTypeAndIdTargetId(
        type: LikeType,
        targetId: Long,
    ): Long

    @Query(
        """
        SELECT l.id.targetId AS targetId, COUNT(l) AS likeCount
        FROM Like l
        WHERE l.id.type = :type AND l.id.targetId IN :targetIds
        GROUP BY l.id.targetId
        """,
    )
    fun countLikesGroupByTargetId(
        type: LikeType,
        targetIds: List<Long>,
    ): List<TargetLikeCount>

    @Query(
        """
        SELECT l.id.targetId FROM Like l
        WHERE l.id.userId = :userId AND l.id.type = :type AND l.id.targetId IN :targetIds
        """,
    )
    fun findTargetIdsByUserIdAndTypeAndTargetIdIn(
        userId: UUID,
        type: LikeType,
        targetIds: List<Long>,
    ): List<Long>

    fun deleteByIdTypeAndIdTargetId(
        type: LikeType,
        targetId: Long,
    )

    fun deleteByIdUserId(userId: UUID)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        WHERE l.type = 'COMMENT' AND c.post_id = :postId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentPostId(postId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN posts p ON p.id = l.target_id
        WHERE l.type = 'POST' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        JOIN posts p ON p.id = c.post_id
        WHERE l.type = 'COMMENT' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN posts p ON p.id = l.target_id
        WHERE l.type = 'POST' AND p.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByPostUserId(userId: UUID)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        WHERE l.type = 'COMMENT' AND c.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentUserId(userId: UUID)
}

interface TargetLikeCount {
    val targetId: Long
    val likeCount: Long
}
```

native DELETE가 다섯인 이유는 **부모 삭제 이벤트 × 대상 타입** 조합이 그만큼이기 때문이다. 이 개수는 전용 테이블(`post_likes` + `comment_likes`)로 쪼개도 줄지 않는다 — 다형성으로 아낀 것은 엔티티·DTO·컨트롤러 계층이지 cascade가 아니다.

native 쿼리에는 `@SQLRestriction`이 적용되지 않는다 (CLAUDE.md §2). 여기서는 **의도한 동작**이다 — 이미 soft delete된 모집글·댓글에 남은 좋아요도 함께 지워야 한다. restriction은 끌 수 없으므로 native가 삭제된 행에 닿는 유일한 경로이기도 하다.

### 5.1 `CommentRepository`에 추가

```kotlin
fun existsByIdAndTombstonedAtIsNull(id: Long): Boolean
```

`@SQLRestriction("deleted_at IS NULL")`이 항상 적용되므로 soft delete된 댓글은 자동으로 걸러지고, 여기에 tombstone 조건을 더해 한 번에 검사한다 (6.1 참조).

## 6. 서비스 — `domain/like/service/LikeService.kt`

타입별 위임 메서드(`likePost`/`likeComment` …)는 두지 않는다. 이름만 바꿔 넘기는 층이라 값을 못 하므로 `LikeType`을 컨트롤러가 직접 넘긴다.

```kotlin
@Service
@Transactional(readOnly = true)
class LikeService(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @Transactional
    fun like(type: LikeType, targetId: Long, user: User): LikeResult {
        checkTargetExists(type, targetId)

        // 이미 눌린 상태면 INSERT 생략. created는 PUT의 201/200을 가르는 데만 쓰임.
        val likeId = LikeId(type = type, targetId = targetId, userId = user.id)
        val created = !likeRepository.existsById(likeId)
        if (created) {
            likeRepository.save(Like(likeId))
        }

        return LikeResult(
            created = created,
            response = LikeResponse.of(
                liked = true,
                likeCount = likeRepository.countByIdTypeAndIdTargetId(type, targetId),
            ),
        )
    }

    @Transactional
    fun unlike(type: LikeType, targetId: Long, user: User): LikeResponse {
        checkTargetExists(type, targetId)

        likeRepository.deleteById(LikeId(type = type, targetId = targetId, userId = user.id))

        return LikeResponse.of(
            liked = false,
            likeCount = likeRepository.countByIdTypeAndIdTargetId(type, targetId),
        )
    }

    // tombstone은 soft delete가 아니라 @SQLRestriction에 안 걸리므로 조건을 명시함.
    private fun checkTargetExists(type: LikeType, targetId: Long) {
        val exists =
            when (type) {
                LikeType.POST -> postRepository.existsById(targetId)
                LikeType.COMMENT -> commentRepository.existsByIdAndTombstonedAtIsNull(targetId)
            }
        if (!exists) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "$type not found: $targetId")
        }
    }
}
```

`LikeResult`는 서비스 계층 전용 타입이다 (`domain/like/service/LikeResult.kt`). 응답 본문에는 나가지 않고 컨트롤러가 상태 코드를 고르는 데만 쓴다.

```kotlin
data class LikeResult(
    val created: Boolean,
    val response: LikeResponse,
)
```

컨트롤러는 이 값으로 201/200을 가른다. `DELETE`는 생성 개념이 없어 `LikeResponse`를 그대로 반환한다 (항상 200).

```kotlin
@PutMapping
fun likePost(@PathVariable postId: Long, @CurrentUser user: User): ResponseEntity<LikeResponse> {
    val result = likeService.like(LikeType.POST, postId, user)
    return ResponseEntity
        .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
        .body(result.response)
}
```

두 상태 코드는 `@Operation(description = ...)`으로 문서화한다. 코드베이스에 `@ApiResponse` 사용 전례가 없어 새 애너테이션 패턴을 도입하지 않았고, 스키마 유실 위험도 피했다.

### 6.1 대상 존재 검증

soft delete는 엔티티의 `@SQLRestriction("deleted_at IS NULL")`이 담당한다. 이 restriction은 `em.find()`를 포함해 native가 아닌 모든 경로에 항상 적용되므로, `existsById` 하나로 **삭제된 대상에는 좋아요를 누를 수 없다**가 보장된다.

> 이 설계를 처음 쓸 때는 `@Filter` + 애스펙트 방식이라 `em.find()`(=`findByIdOrNull`)가 필터를 우회했고, 그래서 `existsById`를 골라야 할 이유가 따로 있었다. PR #140에서 `@SQLRestriction`으로 전환되며 그 함정이 사라졌다.

댓글은 여기에 tombstone 검사가 더 붙는다. **tombstone은 soft delete가 아니라 `tombstoned_at`을 쓰므로 restriction에 걸리지 않아** 조건을 명시해야 한다. tombstone된 댓글은 본문이 사라지고 "삭제된 댓글입니다"로만 렌더링되므로 좋아요 대상이 아니다.

### 6.2 카운트를 변경 뒤에 조회해도 되는 이유

리포지토리 조회가 자동 flush를 선행하므로 `save`/`deleteById` 직후의 `countByIdTypeAndIdTargetId`는 반영된 값을 본다 (CLAUDE.md §2에 실측으로 명문화됨). 수동 `flush()`를 부르지 않는다.

### 6.3 동시성

같은 사용자가 `PUT`을 동시에 두 번 보내면 둘 다 `exists = false`를 보고 INSERT해 PK 중복(`DataIntegrityViolationException`)이 날 수 있다. `bookmarks`·`follows`의 토글에도 똑같이 존재하는 조건이고 실사용 빈도가 극히 낮아 **이번 범위에서는 처리하지 않는다** (CLAUDE.md §7 — 최소 변경). 잡으려면 `DataIntegrityViolationException`을 멱등 성공으로 흡수하는 방식이 맞다.

### 6.4 응답 DTO

```kotlin
@Schema(description = "좋아요 응답 DTO")
data class LikeResponse(
    @Schema(description = "좋아요 여부", example = "true")
    val liked: Boolean,
    @Schema(description = "좋아요 수", example = "42")
    val likeCount: Long,
) {
    companion object {
        fun of(liked: Boolean, likeCount: Long): LikeResponse =
            LikeResponse(liked = liked, likeCount = likeCount)
    }
}
```

## 7. 조회 응답 노출

카운트 타입은 `Long`으로 통일한다 (`commentCount: Long` 전례).

### 7.1 변경 대상

| DTO | 추가 필드 | 삽입 위치 |
|---|---|---|
| `PostDetailResponse` | `likeCount: Long`, `liked: Boolean` | `commentCount` 다음 |
| `PostSimpleResponse` | `likeCount: Long`, `liked: Boolean` | `commentCount` 다음 |
| `CommentResponse` | `likeCount: Long`, `liked: Boolean` | `tombstoned` 다음 |

팩토리 파라미터는 목록 끝에 붙이지 말고 **할당되는 자리에 끼워넣는다** (CLAUDE.md §4).

### 7.2 컨트롤러 시그니처 변경

`liked`를 채우려면 현재 사용자가 필요한데 두 목록 엔드포인트가 사용자를 받지 않는다. `@AllowIncompleteSetup`은 유지한 채 nullable로 추가한다 (CLAUDE.md §3 — 인증이 선택적인 경우).

```kotlin
// PostController.getPosts
@CurrentUser user: User?

// PostCommentController.getComments
@CurrentUser user: User?
```

비로그인이면 `liked = false`로 나간다.

### 7.3 배치 조회

목록마다 추가 쿼리는 **2회 고정**이다. N+1이 없다.

```kotlin
val likeCountByPostId =
    if (postIds.isEmpty()) {
        emptyMap()
    } else {
        likeRepository
            .countLikesGroupByTargetId(LikeType.POST, postIds)
            .associate { it.targetId to it.likeCount }
    }
val likedPostIdSet =
    if (user == null || postIds.isEmpty()) {
        emptySet()
    } else {
        likeRepository
            .findTargetIdsByUserIdAndTypeAndTargetIdIn(user.id, LikeType.POST, postIds)
            .toSet()
    }
```

`getComments`는 **최상위 댓글과 답글의 id를 합친 목록**으로 같은 두 쿼리를 돌린다. 답글에도 좋아요가 달리므로 `content + replies`의 id 전부가 대상이다.

변수명은 CLAUDE.md §6 컬렉션 네이밍을 따른다 (`Map` → `valueByKey`, `Set` → `~Set`).

### 7.4 tombstone된 댓글의 좋아요 수

tombstone 시점에 좋아요를 지우므로 `likeCount = 0`, `liked = false`로 나간다. `content`·`user`처럼 nullable로 만들지 않는다 — 0이 의미상 정확하고, 클라이언트는 `tombstoned = true`로 좋아요 UI 자체를 감춘다.

### 7.5 `PostSimpleResponse` 공유 호출처

`BookmarkService.getUserBookmarkables`가 같은 DTO를 쓴다. 값을 채우지 않으면 북마크 목록이 좋아요 수 0을 거짓으로 보고하므로 **같은 배치 조회를 넣어야 한다.** 이 메서드는 `user`를 이미 받으므로 `liked`도 채운다.

### 7.6 범위에서 뺀 것 — `TeamPostSimpleResponse`

팀 상세의 모집글 목록(`GET /teams/{teamId}/posts`)에는 넣지 않았다. 별도 DTO라 선택 가능하고, 이번 요청 범위는 모집글 목록·상세와 댓글이다. 같은 카드 UI를 재사용한다면 배치 조회 2줄로 추가할 수 있다.

## 8. Cascade

### 8.1 `CommentDeletedEvent` 신설

현재 `CommentService.deleteComment`는 이벤트를 발행하지 않고 `delete()`/`tombstone()`을 직접 호출한다. 댓글 좋아요 정리를 이벤트로 붙이려면 이벤트가 필요하다. PR #137(event-driven-cascade)의 연장선이다.

```kotlin
// domain/comment/event/CommentDeletedEvent.kt
data class CommentDeletedEvent(
    val commentId: Long,
)
```

`deleteComment`의 **두 분기 모두에서** 발행한다. tombstone도 본문이 사라지므로 좋아요가 남을 이유가 없다.

```kotlin
if (comment.isReply) {
    eventPublisher.publishEvent(CommentDeletedEvent(commentId))
    comment.delete()
    cleanUpTombstonedParent(comment)
    return
}

eventPublisher.publishEvent(CommentDeletedEvent(commentId))
if (commentRepository.existsByPostIdAndParentId(comment.postId, commentId)) {
    comment.tombstone()
} else {
    comment.delete()
}
```

`cleanUpTombstonedParent`가 지우는 부모는 tombstone 시점에 이미 좋아요가 정리되었으므로 추가 발행이 필요 없다.

### 8.2 `LikeCascadeListener` — `domain/like/event/`

```kotlin
@Component
class LikeCascadeListener(
    private val likeRepository: LikeRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onCommentDeleted(event: CommentDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.COMMENT, event.commentId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.POST, event.postId)
        likeRepository.deleteByCommentPostId(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        likeRepository.deleteByPostTeamId(event.teamId)
        likeRepository.deleteByCommentPostTeamId(event.teamId)
    }

    // 그 사용자가 누른 좋아요와 그 사용자의 글·댓글이 받은 좋아요를 모두 정리.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        likeRepository.deleteByIdUserId(event.userId)
        likeRepository.deleteByPostUserId(event.userId)
        likeRepository.deleteByCommentUserId(event.userId)
    }
}
```

`deleteByCommentUserId`는 `WHERE c.user_id = :userId`만 보므로 탈퇴 cascade에서 tombstone된 댓글과 soft delete된 댓글의 좋아요를 **한 번에** 정리한다. 두 분기를 따로 다룰 필요가 없다.

### 8.3 탈퇴 후 재가입

좋아요는 복원되지 않는다. `bookmarks`와 같은 선택이며 `follows`와는 다르다 (`follows`는 soft delete 후 `updateDeletedAtNull…`로 되살린다). 복원이 필요해지면 `deleted_at` 컬럼과 reactivate 경로가 추가로 든다.

## 9. 테스트

### 9.1 `LikeServiceTest`

- `PUT` 2회 → `liked = true`, `likeCount = 1` (멱등)
- `DELETE` 2회 → `liked = false`, `likeCount = 0` (없는 행 삭제는 no-op)
- 없는 모집글·댓글 → `ENTITY_NOT_FOUND`
- soft delete된 모집글 → `ENTITY_NOT_FOUND`
- tombstone된 댓글 → `ENTITY_NOT_FOUND`

### 9.2 cascade 통합 테스트

`CascadeIntegrationTestSupport`에 헬퍼를 추가한다.

```kotlin
protected fun createLike(userId: UUID, type: LikeType, targetId: Long): Like
```

검증 추가 대상:

| 테스트 | 검증 |
|---|---|
| `PostServiceCascadeTest` | 글 삭제 시 그 글과 그 글 댓글들의 좋아요 정리, 형제 글 좋아요 보존 |
| `CommentServiceCascadeTest` | 댓글 삭제·tombstone 시 좋아요 정리, 형제 댓글 좋아요 보존 |
| `TeamServiceCascadeTest` | 팀 삭제 시 산하 글·댓글 좋아요 정리 |
| `UserServiceCascadeTest` | 탈퇴 시 누른 좋아요·받은 좋아요 모두 정리 |

**주의**: 검증에 쓰는 `JdbcTemplate`은 Hibernate를 우회해 flush가 트리거되지 않는다. 서비스 호출이 엔티티 변경만 하고 끝나면 `count(...)`가 변경 이전 상태를 본다 (CLAUDE.md §2). 좋아요 정리는 native DELETE라 즉시 나가지만, 같은 테스트에서 검증하는 댓글 tombstone 등은 영향을 받을 수 있다.

## 10. 범위 밖

| 항목 | 사유 |
|---|---|
| 좋아요 알림 | 기획에 없음. 부록 참조 |
| "내가 좋아요한 목록" 조회 API | 기획에 없음. `idx_likes_user`가 이미 대비되어 있어 추가는 additive |
| `TeamPostSimpleResponse` 노출 | 7.6 |
| 비정규화 `like_count` 컬럼 | 1장. 실측으로 뜨거워지면 별건 전환 |
| 동시 `PUT` 레이스 | 6.3 |

## 11. 브랜치 · 커밋 · PR

fork 기반 triangular workflow를 따른다 (CLAUDE.md §8).

```bash
git switch -c feat/like upstream/main
git push -u origin feat/like
gh pr create --repo Team-Waggle/WaggleAPIServer --base main --head sillysillyman:feat/like
```

### 11.1 커밋 분할

`upstream/main` 위에 쌓인 실제 순서는 다음과 같다.

```
docs(like): fork 기반 리모트·PR 전략 성문화
  - CLAUDE.md §8에 upstream/origin 역할과 파생·PR 절차 추가

feat(like): 댓글 삭제 이벤트 신설
  - CommentDeletedEvent 추가, deleteComment에서 분기 이전에 한 번 발행

feat(like): 좋아요 도메인 추가
  - V25__create_likes.sql, Like/LikeId/LikeType
  - LikeRepository, LikeService, LikeResponse
  - PostLikeController, CommentLikeController, LikeCascadeListener
  - CommentRepository.existsByIdAndTombstonedAtIsNull
  - 본 설계 문서

chore(like): gradlew 실행 권한을 저장소에 반영
  - 파일 모드 100755 커밋 (11.4)

refactor(like): 좋아요 서비스 주석·인자 정리
  - named argument 적용, @SQLRestriction 전환에 맞춰 주석 갱신

feat(like): 모집글·댓글 응답에 좋아요 수·여부 노출
  - PostDetailResponse, PostSimpleResponse, CommentResponse에 필드 추가
  - getPosts, getComments에 @CurrentUser user: User? 추가
  - BookmarkService.getUserBookmarkables 배치 조회 반영
  - LikeServiceTest 및 cascade 테스트 4종

docs(like): soft delete 서술을 @SQLRestriction 전환에 맞게 갱신
  - PR #140으로 @Filter가 걷히면서 무효가 된 5장·6.1 서술 교체

feat(like): 좋아요 PUT을 201/200으로 구분
  - LikeResult 도입, 컨트롤러가 ResponseEntity로 상태 코드 결정 (2.2)
```

앞의 두 커밋은 좋아요 도메인 밖의 변경이라 **맨 앞에 독립적으로** 둔다. PR #138이 username 검증을 댓글 PR 맨 앞 독립 커밋으로 넣은 것과 같은 처리다.

`CommentDeletedEvent`를 발행만 하고 리스너가 없는 상태가 한 커밋 동안 존재하지만, 이벤트 발행은 구독자가 없으면 no-op이라 그 시점에도 동작이 깨지지 않는다.

**테스트는 별도 `test(...)` 커밋으로 떼지 않는다** (PR #138의 `d788370` 전례). 다만 실제로는 도메인 커밋이 아니라 응답 노출 커밋에 함께 들어갔다 — 테스트가 응답 필드까지 검증하기 때문이다.

### 11.2 PR

제목은 커밋들의 상위 요약으로 한 단계 추상화한다 (CLAUDE.md §8).

```
feat(like): 모집글·댓글 좋아요 기능 추가
```

본문은 `.github/PULL_REQUEST_TEMPLATE.md`의 3단(요약 / 변경 사항 / 테스트)을 채운다.

### 11.3 CI

PR에는 `lint.yml`의 `./gradlew spotlessCheck`만 돈다. **테스트는 CI에서 돌지 않으므로** 로컬에서 `./gradlew build`로 확인하고 템플릿 체크박스를 채운다. 통합 테스트는 Testcontainers MySQL을 쓰므로 Docker가 필요하다.

두 워크플로 모두 `if: github.repository == 'Team-Waggle/WaggleAPIServer'` 조건이 걸려 있어 fork에서는 돌지 않는다.

`./gradlew spotlessApply`를 작업 완료 시점에 한 번 실행한다 (CLAUDE.md §6). 누락하면 lint가 실패한다.

### 11.4 `gradlew` 실행 권한

`gradlew`가 저장소에 `100644`(비실행)로 들어 있어 클론한 환경마다 `chmod +x`를 반복해야 했고, 그 mode 변경이 워킹트리에 상시 노출됐다. **파일 모드를 `100755`로 커밋해 해소한다** (내용 변경은 없다).

CI의 `chmod +x gradlew`는 **그대로 둔다.** 한 번은 제거했다가 되돌렸는데, 이유는 **fine-grained PAT로 `.github/workflows/` 변경을 push하려면 `Workflows: Read and write` 권한이 따로 필요하기 때문이다.**

```
! [remote rejected] feat/like -> feat/like
  (refusing to allow a Personal Access Token to create or update workflow
   `.github/workflows/deploy.yml` without `workflow` scope)
```

이미 실행 가능한 파일에 `chmod`를 한 번 더 하는 것은 무해한 no-op이라 제거의 이득이 작다. 반면 그 권한을 토큰에 부여하면 유출 시 CI 워크플로 변조 경로가 열린다. 워크플로 정리가 필요해지면 GitHub 웹 UI에서 직접 편집하면 PAT 제약을 타지 않는다.

## 부록 — 좋아요 알림 (구현하지 않음)

도입 시 전부 additive라 계약이 깨지지 않는다.

- `NotificationType`에 `POST_LIKED`, `COMMENT_LIKED` 추가
- 자기 글·자기 댓글 좋아요는 알림 제외
- **중복 억제 정책이 필요하다.** `PUT`이 멱등이라 재시도로 알림이 중복 발행되지는 않지만, 좋아요 → 취소 → 좋아요를 반복하면 매번 새 알림이 나간다. `likes.created_at` 기준 쿨다운 또는 "취소 시 알림도 회수" 중 하나를 정해야 한다
- 인기 글은 알림이 폭주하므로 "N명이 좋아합니다" 식 집계 알림을 검토할 것
