# 댓글 기능 설계

모집글(Post)에 댓글·답글을 다는 기능. **이번 범위는 본문만 있는 댓글과 1-depth 답글**이다 — CRUD, tombstone 삭제, cascade, 모집글 응답의 댓글 수 노출.

알림과 멘션은 현재 기획에 없어 **설계만 해두고 구현하지 않았다.** 부록 참조.

## 0. 선행 조건 — username 검증

현재 `users.username`은 `varchar(255)` UNIQUE에 검증이 `@field:NotBlank` 하나뿐이라 공백·`@`·이모지가 전부 허용된다.

**미루면 단조롭게 비싸지는 종류의 결정이라 지금 넣는다.** 지금은 전원이 규칙을 만족해 백필이 0이지만, 규칙에 어긋나는 username이 하나라도 등록되면 제약 추가에 데이터 마이그레이션이 붙는다. 게다가 **username 변경 엔드포인트가 없어** 해당 사용자가 스스로 고칠 수도 없다.

(부록의 멘션도 이 규칙 위에서만 성립하지만, 그건 부수적 이득이지 주된 근거가 아니다.)

```
^[가-힣a-zA-Z0-9]{2,20}$
```

**기존 데이터 24건이 전부 통과하므로 백필이 없다.** 순한글(`김재영`, `녕냥뇽냥`), 영숫자(`43metns`, `LSZ0313`), 혼용(`Rak입니다`), 최단 `예은`(2자), 최장 `sillysillyman`(13자)까지 모두 만족한다. 실제로 쓰이고 있는 규칙을 명문화하는 것에 가깝다. NULL인 7건은 프로필 미완성 사용자라 `setupProfile`에서 검증을 탄다.

**적용 지점은 `UserSetupProfileRequest.username` 하나뿐이다.** username 쓰기 경로가 `setupProfile` 하나이고 **변경 엔드포인트가 아예 없다** (`UserUpdateRequest`에도 `username`이 없고, 탈퇴 시 NULL이 될 뿐이다).

`GET /users/check`에는 검증을 걸지 않는다. 이 엔드포인트는 "이 이름이 비어 있는가"만 답하면 되고, 형식이 틀린 이름은 어차피 생성 시점에 막힌다. 중복확인에서 한 번 더 볼 이유가 없다.

**커스텀 제약을 만들지 않고 `@Pattern`을 그대로 쓴다.**

```kotlin
@Schema(description = "사용자명 (한글·영문·숫자 2~20자)", example = "testUser")
@field:NotBlank
@field:Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,20}$")
val username: String,
```

`WebUrl`이 애너테이션 + validator 클래스를 가진 건 URL 검증이 정규식으로 표현되지 않아 `WebUrlValidator`에 실제 로직이 있기 때문이다. username은 정규식 하나가 전부라 래퍼를 씌우면 로직 없는 간접층만 늘어난다 (CLAUDE.md §7). 적용 지점이 하나뿐이라 정규식을 상수로 뺄 이유도 없다.

`message`는 오버라이드하지 않는다. 코드베이스의 어느 DTO도 오버라이드하지 않고 기본 영문 메시지를 그대로 쓰며, `GlobalExceptionHandler`가 `defaultMessage`를 `detail`에 실어 보낸다. `@Size(max = 1000)`이 이미 `max`를 노출하는 것과 같은 수준이다.

길이 상한 20자는 현재 최장값(13자)에 여유를 둔 값이다.

> 유저 도메인 변경이지만 이번 PR에 함께 넣는다. 대신 **커밋을 맨 앞에 독립적으로** 두어 리뷰 시 댓글 변경과 섞이지 않게 한다 (9장).

## 1. 설계 결정 요약

| 항목 | 결정 | 근거 |
|---|---|---|
| 댓글 대상 | 모집글(Post) 전용, `post_id` 직결 | 대상이 하나뿐이라 `Bookmarkable` 식 다형성은 오버엔지니어링. 대상이 늘면 그때 승격 |
| 답글 깊이 | `parent_id` nullable, 서비스에서 1-depth 강제 | 확장 시 컬럼 추가 없이 상수만 완화. `depth`/`root_id`는 depth ≥ 2가 실제로 올 때 추가 |
| 삭제 | 답글 있으면 tombstone(`tombstoned_at`), 없으면 soft delete(`deleted_at`) | 두 컬럼을 분리해 **전역 soft delete 필터를 그대로 활용**. 아래 2.2 참조 |
| 정렬 | 최상위·답글 모두 `id ASC` (등록순) | CLAUDE.md §2 — 시간순 정렬은 `id` 기준 |
| 페이지네이션 | 최상위 댓글에 커서, 해당 스레드의 답글은 전량 동봉 | 커서가 `Long` 하나로 끝나 `CursorGetQuery`를 그대로 씀 |
| 알림·멘션 | **이번 범위 제외** | 기획에 없다. 설계는 부록에 보존 — 도입 시 전부 additive라 계약이 안 깨진다 |
| 좋아요 | 이번 범위 제외 | 별도 후속 작업 |

## 2. 스키마

### 2.1 마이그레이션 `V24__create_comments.sql`

```sql
CREATE TABLE comments
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id       BIGINT        NOT NULL,
    user_id       BINARY(16)    NOT NULL,
    parent_id     BIGINT        NULL,
    content       VARCHAR(1000) NOT NULL,
    tombstoned_at DATETIME(6)   NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    deleted_at    DATETIME(6)   NULL
);

CREATE INDEX idx_comments_post_parent ON comments (post_id, parent_id);
CREATE INDEX idx_comments_user ON comments (user_id);
```

인덱스는 이 둘로 충분하다.

- `idx_comments_post_parent` — 최상위 조회(`post_id = ? AND parent_id IS NULL`), 답글 조회(`post_id = ? AND parent_id IN (…)`), 답글 존재 확인(`post_id = ? AND parent_id = ?`)을 전부 커버한다. equality prefix 뒤에 PK `id`가 자동으로 붙으므로 `ORDER BY id` / `ORDER BY parent_id, id`가 filesort 없이 처리된다 (CLAUDE.md §2).
- `idx_comments_user` — 탈퇴 cascade(`WHERE user_id = ?`)용. `idx_posts_user`와 같은 이유.
- `parent_id` 단독 인덱스는 만들지 않는다. 모든 답글 조회 경로가 `post_id`를 이미 알고 있어 복합 인덱스로 커버된다.

**답글 존재 확인에 `postId`를 함께 넘기는 이유**: `parentId`는 PK라 논리적으로는 단독으로 유일하다(생성 시 부모의 `postId` 일치를 강제하므로 불일치도 없다). 하지만 leftmost prefix 규칙상 `(post_id, parent_id)` 인덱스를 `parent_id` 단독 조건으로는 탐색에 쓸 수 없다.

EXPLAIN 실측 (댓글 600행):

| 쿼리 | type | rows | Extra |
|---|---|---|---|
| 최상위 커서 조회 | `range` | 4 | Using index condition |
| 답글 조회 | `ref` | 2 | — |
| 답글 존재 확인 (`post_id` + `parent_id`) | `ref` | 2 | Using index |
| 답글 존재 확인 (`parent_id`만) | `index` | **600** | Using where; Using index |
| `countByPostId` | `ref` | 12 | Using index |

`parent_id` 단독은 `type=index`로 인덱스 전체를 훑는다. **어느 쿼리에도 `Using filesort`가 없다** — equality prefix 뒤에 PK가 붙어 `ORDER BY id`가 정렬 없이 처리된다는 전제가 확인됐다.

`content`는 `VARCHAR(1000)`. 모집글 본문(`TEXT`)과 달리 길이 상한이 명확하다.

### 2.2 두 개의 삭제 컬럼 — 핵심 설계 포인트

`AuditingEntity`의 `@Filter("deleted_at IS NULL")`이 `SoftDeleteFilterAspect`를 통해 모든 `@Transactional` 메서드에서 켜진다. tombstone을 `deleted_at`으로 표현하면 목록 조회가 tombstone을 걸러버리므로 **핵심 조회 경로 전체가 native 쿼리가 되어야 한다.**

그래서 삭제 상태를 두 컬럼으로 나눈다.

| 상황 | 컬럼 | 전역 필터 | 목록 노출 |
|---|---|---|---|
| 답글 없는 댓글 삭제 | `deleted_at` 세팅 | 걸림 → 자동 제외 | ✗ |
| 답글 있는 댓글 삭제 | `tombstoned_at` 세팅 (`deleted_at`은 NULL 유지) | 통과 | ✓ "삭제된 댓글입니다" |
| 모집글/팀 삭제 cascade | `deleted_at` 세팅 | 걸림 → 자동 제외 | ✗ |

결과적으로 **조회 쿼리는 전부 평범한 JPQL/derived method로 쓸 수 있다.** native가 필요한 건 CLAUDE.md §2가 이미 요구하는 cascade UPDATE뿐이다.

두 컬럼은 삭제의 강약이 아니라 **서로 다른 상태**다 — `deleted_at`은 "행이 없는 것으로 취급", `tombstoned_at`은 "본문만 감추고 스레드 앵커로 남김". `content_deleted_at` 같은 이름은 `deleted_at`의 하위 변종처럼 읽혀 이 구분을 흐리므로 쓰지 않는다.

API 응답 필드도 `tombstoned: Boolean`으로 컬럼과 어휘를 맞춘다.

`deleted`로 두지 않는 이유는 컬럼 이름을 고를 때와 같다. **응답에 실려 나오는 댓글 중 `true`인 것은 언제나 tombstone이다** — 진짜 삭제된 댓글은 전역 필터에 걸려 응답에 아예 들어오지 않는다. 그런데 이 API 전반에 `deletedAt` 기반 soft delete 개념이 있어서, 클라이언트가 `deleted: true`를 "이 행이 soft delete됐다"로 읽을 여지가 생긴다. 클라이언트가 절대 마주칠 수 없는 의미다.

코드베이스의 Boolean 응답 필드가 `recruiting`, `complete`, `visible`처럼 `is` 접두사 없는 형용사형이므로 `tombstoned`로 쓴다.

**삭제 상태를 `content`에 인코딩하지 않는 이유** — `content`를 nullable로 만들어 `content IS NULL`을 tombstone 표시로 쓰면 컬럼 하나를 아낄 수 있지만 채택하지 않는다.

- **감사가 불가능해진다.** 신고당한 댓글을 작성자가 삭제하면 운영자가 무엇이 신고됐는지 확인할 방법이 없다. 되돌릴 수도 없다.
- `content NOT NULL` 제약을 잃는다. 버그로 들어간 NULL과 의도된 tombstone이 DB 상에서 구별되지 않는다.
- 삭제 시각을 잃는다. `updated_at`은 이후 cascade·마이그레이션 UPDATE에 덮어써진다.

`content`는 항상 `NOT NULL`로 원문을 보존하고, 삭제 상태는 전용 타임스탬프 컬럼으로만 표현한다. 응답 마스킹은 `CommentResponse.of`가 전담한다.

### 2.3 엔티티

```kotlin
@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comments_post_parent", columnList = "post_id, parent_id"),
        Index(name = "idx_comments_user", columnList = "user_id"),
    ],
)
class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "parent_id", updatable = false)
    val parentId: Long? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var content: String,
) : AuditingEntity() {
    @Column(name = "tombstoned_at")
    var tombstonedAt: Instant? = null

    val isReply: Boolean get() = parentId != null

    val isTombstoned: Boolean get() = tombstonedAt != null

    fun update(content: String) {
        if (isTombstoned) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Cannot update deleted comment: $id")
        }
        this.content = content
    }

    fun tombstone() {
        this.tombstonedAt = Instant.now()
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Not the owner of the comment")
        }
    }
}
```

## 3. API

**shallow nesting** — 컬렉션은 부모 아래, 개별 항목은 최상위에 둔다. Rails 라우팅의 `resources :comments, shallow: true`가 만들어내는 형태이고, 이 코드베이스가 이미 같은 패턴을 쓰고 있다.

| 메서드 | 경로 | 가드 | 설명 |
|---|---|---|---|
| `POST` | `/posts/{postId}/comments` | `@CurrentUser user: User` | 댓글·답글 작성 |
| `GET` | `/posts/{postId}/comments` | `@AllowIncompleteSetup` | 스레드 목록 커서 조회 |
| `PUT` | `/comments/{commentId}` | `@CurrentUser user: User` | 본문 수정 |
| `DELETE` | `/comments/{commentId}` | `@CurrentUser user: User` | 삭제 |

경로 모양은 기존 선례와 같다.

| | 컬렉션 (부모 아래) | 개별 항목 (최상위) |
|---|---|---|
| Application | `POST`·`GET /teams/{teamId}/applications` | `PUT`·`DELETE /applications/{applicationId}` |
| Post | `GET /teams/{teamId}/posts` | `PUT`·`DELETE /posts/{postId}` |
| Comment | `POST`·`GET /posts/{postId}/comments` | `PUT`·`DELETE /comments/{commentId}` |

`commentId`가 전역 고유하므로 수정·삭제에 `postId`를 요구하면 중복 정보다. 검증하지 않으면 `/posts/999/comments/42`도 동작해버려 오히려 해롭다.

**컨트롤러는 둘로 나누되 둘 다 `domain/comment/`에 둔다.**

컨트롤러가 둘이므로 `domain/comment/controller/` 하위로 모은다 (CLAUDE.md §1).

| 컨트롤러 | `@RequestMapping` | 담당 |
|---|---|---|
| `PostCommentController` | `/posts/{postId}/comments` | 작성, 목록 조회 |
| `CommentController` | `/comments` | 수정, 삭제 |

Rails에서 중첩 리소스에 별도 컨트롤러를 쓰는 정규 방법(`module:` 네임스페이스 → `Posts::CommentsController`)과 같은 구성이다. Spring에는 JAX-RS의 sub-resource locator에 해당하는 장치가 없어 컨트롤러 분리가 그 역할을 한다. 이 코드베이스에도 `domain/message/`에 컨트롤러 둘을 둔 선례가 있다.

**두 컨트롤러에 같은 `@Tag(name = "댓글")`을 준다.** OpenAPI 태그는 문자열 매칭이라 Swagger에서 한 그룹으로 합쳐진다 — `/v3/api-docs`를 실제로 뽑아 네 엔드포인트가 모두 `tags=[댓글]`로 나오는 것을 확인했다. 이게 중요한 이유는, 이 코드베이스가 클래스 레벨 `@Tag`만 쓰기 때문에 엔드포인트를 부모 컨트롤러에 얹으면 **한 도메인의 API가 두 그룹으로 쪼개지기** 때문이다. 실제로 `TeamController`가 제공하는 `/teams/{teamId}/applications`는 "팀 지원"이 아니라 "팀" 아래에 나온다. 같은 문제를 물려받지 않는다.

대안 두 가지를 검토했고 채택하지 않았다.

- **`PostController`에 얹기** — `TeamController` 선례를 따르는 형태지만 위의 Swagger 분산 문제가 생기고, 모집글 쪽이 `CommentService`를 주입받아야 한다.
- **컨트롤러 하나에 전체 경로 쓰기** — 클래스 레벨 `@RequestMapping`을 포기해야 한다(접두사가 붙어 `/comments/posts/{postId}/comments`가 된다). 코드베이스의 다른 REST 컨트롤러가 전부 클래스 레벨 매핑을 갖고 있어 혼자 어긋난다.

수정이 `PATCH`가 아니라 `PUT`인 이유: 코드베이스에서 "~ 수정"은 예외 없이 `PUT /{id}`이고(`PUT /posts/{postId}`, `PUT /teams/{teamId}`, `PUT /users/me`), `PATCH`는 하위 경로의 특정 상태 변경에 쓴다. 댓글 수정은 변경 가능한 상태 전체를 교체하므로 `PUT`이 맞다.

조회는 비로그인도 허용한다 (모집글 상세가 이미 그렇다). 응답에 로그인 사용자 의존 필드가 없으므로 `@CurrentUser` 파라미터를 두지 않고 `@AllowIncompleteSetup`만 붙인다 (CLAUDE.md §3).

### 3.1 요청 DTO

```kotlin
@Schema(description = "댓글 생성 요청 DTO")
data class CommentCreateRequest(
    @Schema(description = "부모 댓글 ID (답글이 아니면 null)", example = "10")
    @field:Positive
    val parentId: Long? = null,
    @Schema(description = "댓글 본문", example = "저 지원하고 싶은데요")
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
)

@Schema(description = "댓글 수정 요청 DTO")
data class CommentUpdateRequest(
    @Schema(description = "댓글 본문", example = "저 지원하고 싶은데요")
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
)
```

`postId`는 경로 변수로 받으므로 DTO에 없다. 애너테이션 순서는 import 순서(`NotBlank` → `Positive` → `Size`)를 따른다 (CLAUDE.md §4).

### 3.2 응답 DTO

```kotlin
@Schema(description = "댓글 응답 DTO")
data class CommentResponse(
    val id: Long,
    val postId: Long,
    val parentId: Long?,
    @Schema(description = "댓글 본문 (삭제된 댓글이면 null)")
    val content: String?,
    @Schema(description = "작성자 정보 (삭제된 댓글이면 null)")
    val user: UserSimpleResponse?,
    @Schema(description = "삭제된 댓글 여부 — true면 '삭제된 댓글입니다'로 렌더링")
    val tombstoned: Boolean,
    @Schema(description = "답글 목록 (답글 자신은 항상 빈 목록)")
    val replies: List<CommentResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun of(
            comment: Comment,
            author: UserSimpleResponse?,
            replies: List<CommentResponse> = emptyList(),
        ): CommentResponse =
            CommentResponse(
                id = comment.id,
                postId = comment.postId,
                parentId = comment.parentId,
                content = if (comment.isTombstoned) null else comment.content,
                user = if (comment.isTombstoned) null else author,
                tombstoned = comment.isTombstoned,
                replies = replies,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
    }
}
```

tombstone 마스킹을 DTO 팩토리 안에 가둔다 — 서비스가 마스킹을 잊는 경로가 생기지 않는다 (CLAUDE.md §4).

`author`가 nullable인 이유: 탈퇴 사용자의 댓글은 tombstone으로 남지만 user 행은 soft delete되어 배치 조회에 잡히지 않는다. 어차피 마스킹 대상이라 문제되지 않는다.

`replies`를 같은 타입으로 중첩시켜 두면 depth 확장 시 응답 형태가 바뀌지 않는다. 목록 응답은 `CursorResponse<CommentResponse>`이고 `data`에는 최상위 댓글만 담긴다.

## 4. 조회 로직

쿼리 세 번으로 스레드를 구성한다.

```kotlin
// 1) 최상위 댓글 커서 페이지
@Query(
    """
    SELECT c FROM Comment c
    WHERE c.postId = :postId
    AND c.parentId IS NULL
    AND (:cursor IS NULL OR c.id > :cursor)
    ORDER BY c.id ASC
    """,
)
fun findRootsByPostIdWithCursor(postId: Long, cursor: Long?, pageable: Pageable): List<Comment>

// 2) 해당 스레드들의 답글 전량
fun findByPostIdAndParentIdInOrderByParentIdAscIdAsc(postId: Long, parentIds: List<Long>): List<Comment>
```

3번은 작성자 배치 조회(`userRepository.findAllById`)다. 최상위와 답글의 작성자 id를 합쳐 한 번에 긁는다.

전역 필터가 `deleted_at IS NULL`을 붙여주므로 조건에 명시하지 않는다. tombstone은 `deleted_at`이 NULL이라 그대로 조회된다.

답글은 `groupBy { it.parentId }`로 묶어 부모에 붙인다. `hasNext` / `nextCursor` 산출은 `PostService.getPosts`와 동일하게 `size + 1`을 조회해 판정한다.

**한계**: 답글이 수백 개인 스레드는 한 번에 전량 내려간다. 실제로 문제가 되면 `GET /comments/{commentId}/replies`를 분리하고 최상위 응답에는 `replyCount` + 상위 N개만 담는 형태로 확장한다. 지금 미리 만들지 않는다.

## 5. 작성 / 수정 / 삭제

### 5.1 작성

1. `postRepository.findByIdOrNull(postId)` — 없으면 `ENTITY_NOT_FOUND`.
2. `parentId != null`이면 **부모를 `PESSIMISTIC_WRITE`로 조회**한 뒤 검증 (5.4 참조):
   - 같은 `postId`에 속하는지 (`INVALID_STATE`)
   - `parent.isReply`면 1-depth 초과 → `INVALID_STATE`. *(depth 확장 시 여기만 완화)*
   - 부모가 tombstone이어도 답글은 **허용**한다. 스레드 자체는 살아 있다.

### 5.2 수정

작성자만(`checkOwnership`). tombstone 댓글은 수정 불가(`INVALID_STATE`).

### 5.3 삭제

작성자만. 관리자/글 작성자 삭제나 신고 모더레이션은 이번 범위 밖이다.

```kotlin
@Transactional
fun deleteComment(commentId: Long, user: User) {
    val comment = commentRepository.findWithLockById(commentId)
        ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Comment not found: $commentId")
    comment.checkOwnership(user.id)

    if (comment.isReply) {
        comment.delete()
        cleanUpTombstonedParent(comment)
        return
    }

    if (commentRepository.existsByPostIdAndParentId(comment.postId, commentId)) {
        comment.tombstone()          // 답글이 남아 있으므로 스레드 앵커로 남긴다
    } else {
        comment.delete()
    }
}
```

`cleanUpTombstonedParent` — 답글이 지워져 부모 tombstone이 빈 껍데기로 남는 것을 막는다.

```kotlin
private fun cleanUpTombstonedParent(reply: Comment) {
    val parentId = reply.parentId ?: return
    val parent = commentRepository.findWithLockById(parentId) ?: return
    if (!parent.isTombstoned) return
    if (commentRepository.existsByPostIdAndParentId(parent.postId, parentId)) return
    parent.delete()
}
```

1-depth이므로 재귀가 필요 없다. depth를 늘릴 때 여기가 while 루프가 된다.

> **본문은 삭제 후에도 DB에 그대로 남는다.** tombstone이든 soft delete든 `content`는 건드리지 않고, 마스킹은 `CommentResponse.of`에서만 일어난다 (모집글 soft delete와 동일). 신고·분쟁 처리 시 삭제된 댓글의 원문을 확인할 수 있어야 하기 때문이다.
>
> 개인정보 삭제 요청 등으로 저장 자체를 지워야 하는 요구가 생기더라도 `content`를 제자리에서 덮어쓰지 말 것 — 되돌릴 수 없고 감사 근거가 통째로 사라진다. 그런 요구는 별도 보관소로 옮긴 뒤 원본을 지우는 형태로 설계해야 하며, 이번 범위 밖이다.

### 5.4 삭제-답글 경합과 행 잠금

락이 없으면 고아 답글이 생긴다.

```
A: 댓글 10 삭제 → 답글 없음 확인 → deleted_at 세팅 → COMMIT
B: 댓글 10에 답글 작성 → 부모 존재 확인 통과(A 커밋 전 스냅샷) → INSERT → COMMIT
```

REPEATABLE READ에서 두 트랜잭션이 겹치면 부모는 삭제되고 답글만 살아남는다. 목록에는 안 보이지만 행은 남아 **`commentCount`가 틀어진다.** `cleanUpTombstonedParent`에도 같은 경합이 있다.

같은 행에 잠금을 걸어 직렬화한다.

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findWithLockById(id: Long): Comment?
```

- 답글 작성 시 **부모**를 이 메서드로 조회
- 삭제 시 **대상**을 이 메서드로 조회
- `cleanUpTombstonedParent`에서 **부모**를 이 메서드로 조회

단일 행 잠금이라 경합 범위가 좁다. 뒤늦게 도착한 답글 작성은 부모가 사라진 것을 보고 `ENTITY_NOT_FOUND`가 된다.

## 6. Cascade

### 6.1 `CommentCascadeListener`

기존 `PostCascadeListener` / `NotificationCascadeListener`와 동일한 형태 — `@EventListener` + `Propagation.MANDATORY`.

| 이벤트 | 처리 |
|---|---|
| `PostDeletedEvent` | 해당 글의 댓글·답글 전부 `deleted_at` 세팅 |
| `TeamDeletedEvent` | 팀의 모집글에 달린 댓글 전부 `deleted_at` 세팅 (`JOIN posts`) |
| `UserDeactivatedEvent` | 답글 있는 댓글은 **tombstone**, 없는 댓글은 `deleted_at` |

```kotlin
@Modifying
@Query(
    """
    UPDATE comments SET deleted_at = UTC_TIMESTAMP(6)
    WHERE post_id = :postId AND deleted_at IS NULL
    """,
    nativeQuery = true,
)
fun updateDeletedAtByPostIdAndDeletedAtIsNull(postId: Long)

@Modifying
@Query(
    """
    UPDATE comments c JOIN posts p ON p.id = c.post_id
    SET c.deleted_at = UTC_TIMESTAMP(6)
    WHERE p.team_id = :teamId AND c.deleted_at IS NULL
    """,
    nativeQuery = true,
)
fun updateDeletedAtByPostInTeamIdAndDeletedAtIsNull(teamId: Long)
```

`post_id`는 답글에도 들어 있으므로 모집글/팀 cascade는 한 방에 답글까지 잡는다.

**탈퇴 cascade만 tombstone을 쓰는 이유**: 탈퇴자의 댓글을 `deleted_at`으로 지우면 거기 달린 남의 답글이 부모 없는 고아가 되어 목록에는 안 보이면서 행은 남는다. tombstone은 스레드를 보존하면서 본문·작성자를 응답에서 가리므로 "흔적 제거" 요구도 충족한다.

다만 **답글이 있는 댓글만** 앵커가 필요하므로 둘로 나눠 처리한다.

```sql
-- 답글 있음 → tombstone (스레드 앵커로 필요)
UPDATE comments c JOIN comments r ON r.parent_id = c.id AND r.deleted_at IS NULL
SET c.tombstoned_at = UTC_TIMESTAMP(6)
WHERE c.user_id = :userId AND c.deleted_at IS NULL AND c.tombstoned_at IS NULL

-- 답글 없음 → 완전 삭제 (앵커가 필요 없음)
UPDATE comments c LEFT JOIN comments r ON r.parent_id = c.id AND r.deleted_at IS NULL
SET c.deleted_at = UTC_TIMESTAMP(6)
WHERE c.user_id = :userId AND c.deleted_at IS NULL AND r.id IS NULL
```

세 번째 쿼리로 **빈 껍데기가 된 부모 tombstone**을 정리한다. 단건 삭제의 `cleanUpTombstonedParent`에 해당하는 벌크 경로다.

```sql
UPDATE comments p
JOIN comments mine ON mine.parent_id = p.id AND mine.user_id = :userId
LEFT JOIN comments alive ON alive.parent_id = p.id AND alive.deleted_at IS NULL
SET p.deleted_at = UTC_TIMESTAMP(6)
WHERE p.tombstoned_at IS NOT NULL AND p.deleted_at IS NULL AND alive.id IS NULL
```

두 경우를 한 번에 덮는다.

- 본인 댓글에 본인이 답글을 단 경우 — 답글이 2단계에서 지워지면 부모가 빈 tombstone이 된다
- 남의 tombstone에 남아 있던 **마지막 답글이 탈퇴자 것**이었던 경우

`mine`에 `deleted_at` 조건을 걸지 않는 것이 핵심이다. 2단계에서 이미 지워진 답글이라도 "있었다"는 사실로 부모를 찾아야 한다. `mine.user_id`로 대상을 좁혀 `idx_comments_user`를 탄다.

앞 두 쿼리는 대상이 겹치지 않아 순서가 무관하지만, **세 번째는 반드시 마지막에 실행해야 한다.**

> 초안에서는 "self-table 참조는 MySQL 에러 1093에 걸린다"고 보고 전부 tombstone 처리했으나 **잘못된 판단이었다.** 1093은 서브쿼리(`WHERE EXISTS (SELECT ... FROM comments)`)에만 적용되고, **다중 테이블 UPDATE의 JOIN에는 해당하지 않는다** — 조인 대상이 자기 자신이어도 된다. 같은 파일의 팀 cascade가 이미 `UPDATE comments c JOIN posts p ON ...` 형태를 쓰고 있었다.
>
> 영향이 작지 않았다. 탈퇴자의 댓글은 대부분 답글이 없어서, 전부 tombstone으로 두면 탈퇴가 일어날 때마다 읽을 수 없는 자리표시자가 영구히 쌓인다.

## 7. 모집글 응답의 댓글 수

`commentCount`는 **목록에 렌더링되는 항목 수와 일치**시킨다. tombstone은 "삭제된 댓글입니다"로 화면에 남으므로 함께 센다. 완전히 삭제된 댓글만 빠진다.

배지에 "댓글 3개"라고 표시했는데 열어보니 4개가 보이는 불일치를 피하는 쪽을 택했다. tombstone을 제외하면 "실제 내용이 있는 댓글 수"라는 다른 의미가 되지만, 사용자가 마주하는 화면과 어긋난다.

- 상세: `commentRepository.countByPostId(postId)` — 전역 필터가 `deleted_at IS NULL`을 붙인다.
- 목록: `ApplicationRepository.countApplicantsGroupByPostId` 전례대로 프로젝션 인터페이스 + `GROUP BY` 배치 집계.

  ```kotlin
  interface PostCommentCount {
      val postId: Long
      val commentCount: Long
  }

  @Query(
      """
      SELECT c.postId AS postId, COUNT(c) AS commentCount
      FROM Comment c WHERE c.postId IN :postIds GROUP BY c.postId
      """,
  )
  fun countCommentsGroupByPostId(postIds: List<Long>): List<PostCommentCount>
  ```

`PostDetailResponse.of` / `PostSimpleResponse.of`에 `commentCount` 파라미터를 추가하고 호출처를 전부 갱신한다 — `PostService`의 `createPost`, `getPosts`, `getPost`, `updatePost`. 생성 직후는 항상 `0`이다.

`TeamPostSimpleResponse`는 이번 범위 밖이다.

## 8. 변경 파일

**신규**

```
db/migration/V24__create_comments.sql
domain/comment/Comment.kt
domain/comment/controller/CommentController.kt
domain/comment/controller/PostCommentController.kt
domain/comment/dto/request/CommentCreateRequest.kt
domain/comment/dto/request/CommentUpdateRequest.kt
domain/comment/dto/response/CommentResponse.kt
domain/comment/event/CommentCascadeListener.kt
domain/comment/repository/CommentRepository.kt
domain/comment/service/CommentService.kt

test/domain/comment/service/CommentServiceCascadeTest.kt
test/domain/user/dto/request/UserSetupProfileRequestTest.kt
test/domain/comment/service/CommentServiceTest.kt
```

**수정**

```
domain/user/dto/request/UserSetupProfileRequest.kt             username @Pattern
domain/post/dto/response/PostDetailResponse.kt                 commentCount
domain/post/dto/response/PostSimpleResponse.kt                 commentCount
domain/post/service/PostService.kt                             commentCount 조회·전달
test/support/CascadeIntegrationTestSupport.kt                  댓글 repo·헬퍼, 정리 테이블
CLAUDE.md                                                      엔티티 비영속 프로퍼티 규칙
```

`notification` 도메인은 변경이 없다. 댓글 삭제를 구독할 리스너가 없으므로 `CommentDeletedEvent`도 두지 않는다.

## 9. 작업 분할

브랜치 `feat/post-comment`, PR 제목 `feat(post-comment): 모집글 댓글 기능 추가`, 커밋 3개.

1. `feat(post-comment): 사용자명 문자셋·길이 검증 추가` — `UserSetupProfileRequest.username`에 `@Pattern`, 검증 테스트
2. `feat(post-comment): 댓글 도메인 CRUD 및 cascade 추가` — V24, 엔티티/repository/service/controller/DTO, `CommentCascadeListener`, 행 잠금, 테스트
3. `feat(post-comment): 모집글 응답에 댓글 수 노출` — `PostDetailResponse` / `PostSimpleResponse` / `PostService`

1번은 유저 도메인만 건드려 나머지와 파일이 겹치지 않으므로 PR 안에서도 독립적으로 리뷰할 수 있다.

`<scope>`은 브랜치 토픽을 따르므로(CLAUDE.md §8) 1번도 `post-comment`를 쓴다.

## 10. 테스트

`CascadeIntegrationTestSupport`(Testcontainers MySQL + Redis)를 확장한다. `cleanDatabase`의 테이블 목록에 `comments`를 추가하고 `createComment` 헬퍼를 더한다.

**`CommentServiceTest`** (6개)

- 1-depth 강제 — 답글에 답글 시도 시 `INVALID_STATE`
- 답글 있는 댓글 삭제 → `tombstoned: true`로 목록에 남고 `content`/`user`가 `null`, 원문은 DB에 보존
- 답글 없는 댓글 삭제 → 목록에서 사라짐
- 마지막 답글 삭제 → 부모 tombstone까지 함께 정리
- `commentCount`가 목록에 보이는 항목 수(tombstone 포함, 삭제 제외)와 일치
- 커서 페이지네이션 — 최상위 기준으로 잘리고 각 스레드의 답글은 온전히 동봉

**`UserSetupProfileRequestTest`** (17개, Spring 컨텍스트 없는 순수 단위 테스트)

- 프로덕션 실재 사용자명 형태 통과 — 순한글·영숫자·혼용
- 길이 경계 2자/20자 통과, 1자/21자 거부
- 공백·특수문자·이모지·한글 자모 거부
- 빈 문자열은 `@NotBlank`와 `@Pattern` 양쪽 위반

> `[가-힣]`은 완성형 음절만 포함하므로 `ㅋㅋㅋ` 같은 자모 조합은 거부된다. 허용하려면 `[ㄱ-ㅎㅏ-ㅣ가-힣...]`로 넓힌다.

**`CommentServiceCascadeTest`** (3개)

- `PostDeletedEvent` cascade 시 답글까지 제거, 형제 글은 보존
- `TeamDeletedEvent` cascade 시 팀 모집글의 댓글까지 제거
- `UserDeactivatedEvent` 시 답글 있는 댓글만 tombstone, 없는 댓글은 완전 삭제
- `UserDeactivatedEvent` 로 빈 껍데기가 된 부모 tombstone 정리 (본인 자답글 / 남의 tombstone의 마지막 답글)

## 11. 구현 시 주의사항

- **`deleteComment`는 auto-flush에 의존한다.** 답글을 `delete()`한 직후 `existsByPostIdAndParentId`로 형제 답글을 확인하는데, 이때 밀린 UPDATE가 먼저 flush되어야 정확한 답이 나온다. 리포지토리 조회는 JPQL·derived·native 모두 자동 flush가 선행되므로 수동 `flush()`는 넣지 않는다 (CLAUDE.md §2).
- **`SoftDeleteFilterAspect`는 `@Transactional`에서만 켜진다.** 통합 테스트 메서드는 비트랜잭션이라 repository를 직접 부르면 `deleted_at IS NULL`이 붙지 않아 **삭제된 행까지 세어진다.** 실제로 `countByPostIdAndTombstonedAtIsNull`를 테스트에서 직접 호출했다가 걸렸다. 조건을 SQL에 명시하거나, 필터가 필요한 검증은 `postService.getPost` 같은 트랜잭션 경로로 할 것.
- **`jdbcTemplate`에 `UUID`를 바인딩하지 말 것.** `user_id`가 `BINARY(16)`인데 드라이버가 문자열로 넘겨 매칭되지 않는다. 기존 `count()` 헬퍼 호출은 전부 `Long`이라 드러난 적이 없다. UUID 조건이 필요하면 repository로 읽을 것.
- **native cascade UPDATE는 `updated_at`을 갱신하지 않는다.** 기존 `PostRepository` 선례와 동일하니 그대로 따르되 알고는 있어야 한다.
- **인덱스 EXPLAIN 확인.** `(post_id, parent_id)`로 `parent_id IS NULL AND id > cursor`가 filesort 없이 도는지 확인할 것. `IS NULL` + range 조합이라 낙관만 하기엔 이르다.
- **`RateLimitFilter`는 IP당 전역 1000req/min뿐이고 엔드포인트별 제한이 없다.** 댓글 도배에 사실상 무방비지만 이건 댓글 고유 이슈가 아니라 프로젝트 전반의 문제라 이번에 같이 손대지 않는다.

---

# 부록. 향후 도입 (이번 범위 아님)

아래 두 기능은 **설계만 해두고 구현하지 않았다.** 현재 기획은 본문만 있는 댓글과 1-depth 답글뿐이라, 지금 구현하면 쓰이지 않는 테이블·엔드포인트가 남고 확정되지 않은 API 계약이 Swagger에 노출된다.

도입 시 전부 **순수 추가(additive)** 라 기존 클라이언트가 깨지지 않는다.

| 추가 항목 | 호환성 |
|---|---|
| `comment_mentions` 테이블 | 신규 테이블, 기존 스키마 무변경 (`comments`에 멘션 컬럼이 없는 설계 덕분) |
| 요청 `mentionedUserIds` | 기본값 `emptySet()` — 안 보내는 구 클라이언트 정상 동작 |
| 응답 `mentionedUsers` | 필드 추가, 모르는 필드는 무시됨 |
| `GET /users/search` | 신규 엔드포인트 |
| `NotificationType` 신규 값 | **유일하게 클라이언트 조율이 필요한 항목** — 알림 타입을 exhaustive하게 분기하면 새 값 처리 필요 |

> 주의: 멘션 도입 후 PUT은 전체 교체라 `content`만 보내는 구 클라이언트가 멘션을 지운다. PUT 의미상 올바른 동작이며 롤아웃 기간에만 해당된다.

## A. 댓글 알림

### 7.1 타입 추가

```kotlin
enum class NotificationType {
    // ...
    COMMENT_RECEIVED,    // 내 모집글에 댓글이 달림
    REPLY_RECEIVED,      // 내 댓글에 답글이 달림
    COMMENT_MENTIONED,   // 댓글에서 나를 멘션함
}
```

### 7.2 이벤트

```kotlin
data class CommentCreatedEvent(
    val teamId: Long,
    val postId: Long,
    val commentId: Long,
    val parentId: Long?,
    val postAuthorId: UUID,
    val parentAuthorId: UUID?,
    val mentionedUserIds: Set<UUID>,
    val triggeredBy: UUID,
)
```

작성 시점에 이미 로드한 값을 그대로 실어 보낸다. 리스너가 다시 조회하지 않는다.

**부모가 tombstone이면 `parentAuthorId`를 `null`로 싣는다.** 자기가 지운 댓글에 달린 답글 알림을 받는 건 어색하고, 프론트도 `user`가 null이라 자동 태그할 대상이 없다. null이면 아래 우선순위 로직이 자연히 건너뛴다.

### 7.3 리스너 — 1인 1알림

기존 `NotificationEventListener`에 `@Async` + `REQUIRES_NEW` + `@TransactionalEventListener(AFTER_COMMIT)` 형태로 추가한다.

우선순위로 중복을 제거한다.

1. 작성자 본인은 제외
2. 답글이면 부모 작성자에게 `REPLY_RECEIVED`, 최상위면 글 작성자에게 `COMMENT_RECEIVED`
3. 멘션 대상 중 위에서 이미 알림을 받지 않은 사람에게만 `COMMENT_MENTIONED`

```kotlin
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleCommentCreated(event: CommentCreatedEvent) {
    val metadata =
        mapOf(
            "teamId" to event.teamId,
            "postId" to event.postId,
            "commentId" to event.commentId,
            "triggeredBy" to event.triggeredBy.toString(),
        )
    val notifiedUserIdSet = mutableSetOf(event.triggeredBy)
    val notifications = mutableListOf<Notification>()

    val isReply = event.parentId != null
    // 답글인데 부모가 삭제됐으면(parentAuthorId == null) 직접 알림 대상이 없다.
    // 글 작성자로 대체하면 안 된다 — 내 글의 모든 답글에 알림이 가버린다.
    val directTargetUserId = if (isReply) event.parentAuthorId else event.postAuthorId
    if (directTargetUserId != null && notifiedUserIdSet.add(directTargetUserId)) {
        notifications.add(
            Notification(
                type =
                    if (isReply) {
                        NotificationType.REPLY_RECEIVED
                    } else {
                        NotificationType.COMMENT_RECEIVED
                    },
                userId = directTargetUserId,
                metadata = metadata,
            ),
        )
    }

    event.mentionedUserIds
        .filter { notifiedUserIdSet.add(it) }
        .mapTo(notifications) {
            Notification(
                type = NotificationType.COMMENT_MENTIONED,
                userId = it,
                metadata = metadata,
            )
        }

    notificationRepository.saveAll(notifications)
}
```

`teamId`를 metadata에 넣는 이유는 두 가지다. 기존 알림 9종이 전부 `team` 객체를 응답에 실어서 프론트가 팀 기준으로 목록을 그리는데 댓글 알림만 비면 곤란하고, 덤으로 기존 `deleteByTeamId` cascade에 자동으로 걸린다.

> `@Async` + `AFTER_COMMIT` 경로라 기존 알림들과 동일하게 at-least-once 보장이 없다(리스너 실패·크래시 시 영구 유실). 댓글 알림도 같은 성질을 그대로 물려받는다.

### 7.4 알림 cascade — `V26__add_notifications_comment_id.sql`

`metadata`에 `teamId`·`postId`를 넣으므로 기존 `deleteByTeamId` / `deleteByPostId` / `deleteByPostInTeamId` / `deleteByPostUserId`가 팀·모집글·탈퇴 cascade를 이미 커버한다.

댓글 단위 삭제만 새로 필요하다. V23과 같은 generated STORED 컬럼 패턴을 쓴다.

```sql
ALTER TABLE notifications
    ADD COLUMN comment_id BIGINT
        GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.commentId')) AS UNSIGNED)) STORED;

CREATE INDEX idx_notifications_comment ON notifications (comment_id);
```

`NotificationRepository.deleteByCommentId(commentId)` (native) + `NotificationCascadeListener.onCommentDeleted(CommentDeletedEvent)`를 추가한다. tombstone은 알림을 유지한다 — 클릭하면 스레드로 이동할 수 있다.

### 7.5 `NotificationService` 갱신

- `TRIGGERED_BY_TYPES`에 `COMMENT_RECEIVED`, `REPLY_RECEIVED`, `COMMENT_MENTIONED` 추가 (세 타입 모두 "누가"가 핵심 정보다)
- `HYDRATED_METADATA_KEYS`에 `"commentId"` 추가
- `commentId` → `comment` 객체 hydrate. `CommentRepository.findAllById` 배치 조회 후 `NotificationResponse.CommentResponse.of(comment)`:

  ```json
  { "commentId": 42, "content": "저 지원하고 싶은데요" }
  ```

  tombstone이면 `content`는 `null`.
- [docs/notification-response-spec.md](notification-response-spec.md)에 세 타입 행과 예시 응답 추가

## B. 멘션

### 8.1 전체 흐름

```
1. 사용자가 @ 입력          → 프론트가 드롭다운 오픈
2. 뒤에 타이핑              → GET /users/search?q=… 호출
3. 목록에서 선택            → 본문엔 "@예은" 텍스트 삽입, userId는 따로 보관
4. 제출                    → { content, mentionedUserIds } 전송
```

드롭다운은 UI 장식이 아니라 **설계의 전제**다. 서버가 `mentionedUserIds`를 믿을 수 있는 근거가 "사용자가 확정된 목록에서 골랐다"는 사실이다. 자유 입력만 받으면 클라이언트가 `@예은아 확인해줘`에서 이름이 어디까지인지 스스로 판단해야 하는데, 그게 바로 우리가 피하려는 파싱 문제다.

본문 텍스트는 사람이 읽는 용도, `mentionedUserIds`는 기계가 읽는 용도로 역할이 갈린다.

### 8.2 서버 검증

`userRepository.findAllById(mentionedUserIds)`로 조회한 뒤 **본문에 `@{username}`이 포함되어 있는지** 확인한다. 하나라도 없으면 `INVALID_INPUT_VALUE`. 클라이언트가 임의 사용자에게 알림을 쏘는 것을 막는다.

**단어 경계 검사는 하지 않는다.** 한국어는 조사·호칭이 이름에 바로 붙는다.

```
@커피윤님 안녕하세요     ← 경계를 엄격히 보면 400이 나버린다
```

`@커피윤(?![가-힣a-zA-Z0-9])` 같은 lookahead를 걸면 이런 정상 문장이 전부 거부된다. 그래서 단순 `contains`로 간다.

**잔여 위험 두 가지** — 둘 다 인지하고 넘어간다.

| | 시나리오 | 판단 |
|---|---|---|
| prefix 충돌 | 본문엔 `@kimchi`만 쓰고 `kim`을 멘션 목록에 넣으면 통과 | 그냥 `@kim`이라고 써도 똑같이 알림을 보낼 수 있으므로 새로 열리는 공격면이 없다 |
| `@`의 비멘션 용법 | `문의는 kim@example.com` + `example` 멘션 → 통과 | 피해자 username이 도메인의 prefix여야 해서 성립 조건이 좁다. 하이라이팅은 영향 없음 |

막으려면 `@` 앞이 공백이나 문장 시작인지 보는 규칙을 한 겹 더 얹어야 하는데, 이 정도 위험에는 과하다.

### 8.3 하이라이팅 — 프론트 계약

서버는 `mentionedUsers`로 **확정된 후보 집합**을 내려주고, 프론트는 그 목록으로만 치환한다. 본문을 추측 파싱하지 않는다.

```
본문: @예은아 그리고 @예은 확인 부탁
mentionedUsers: [예은, 예은아]
```

**반드시 username 길이 내림차순으로 치환할 것.** 짧은 것부터 하면 `@예은아`의 앞부분이 먼저 먹혀 `아`가 떨어져 나간다. 응답 배열 순서에 의존하지 말고 프론트가 정렬해야 한다.

멘션 위치는 본문 어디여도 무관하다. 문장 중간이든 끝의 `CC. @예은 @김재영` 형태든 검증·치환이 동일하게 동작한다. 후보 집합 + 문자열 검색 방식이 본질적으로 위치 독립적이라 얻는 성질이다.

### 8.4 자동 태그 UX — API 계약

"답글 버튼을 누르면 부모 작성자가 자동 태그되고, 사용자가 지우면 태그 없이 답글" 흐름은 **API 변경 없이 지원된다.**

```
답글 버튼 클릭   → 프론트가 composer에 "@예은 " 채우고 mentionedUserIds = [예은id] 보유
그대로 제출      → { parentId: 10, content: "@예은 네 맞아요", mentionedUserIds: [예은id] }
태그 지우고 제출  → { parentId: 10, content: "네 맞아요",      mentionedUserIds: [] }
수동 @ 태그      → 드롭다운 경로로 동일하게 합류
```

`mentionedUserIds`가 "누가 태그됐는가"만 표현하고 **왜 태그됐는지(자동/수동)는 구분하지 않기 때문에** 두 경로가 같은 필드로 흡수된다. 나중에 "자동 태그는 알림을 보내지 말자"로 기획이 바뀌어도 프론트가 목록에서 빼면 끝이고 서버는 안 건드린다.

`mentions: [{ userId, source: AUTO | MANUAL }]` 같은 확장은 **하지 않는다.** 서버가 `source`로 할 일이 없다. 실제로 다르게 취급할 요구가 생기면 그때 더한다.

프론트·기획이 나중에 합류했을 때 어긋나기 쉬운 계약 세 가지:

1. **답글 알림은 태그와 독립이다.** 태그를 지워도 부모 작성자에겐 `REPLY_RECEIVED`가 나간다. "내 댓글에 답글이 달렸다"는 사실은 태그 여부와 무관하다.
2. **부모 작성자는 알림을 한 번만 받는다.** 자동 태그를 그대로 두면 부모 작성자가 답글 대상이자 멘션 대상이 되는데, 7.3의 우선순위 규칙이 `REPLY_RECEIVED` 하나로 정리한다. 자동 태그 UX에서 가장 흔한 케이스다.
3. **본문과 `mentionedUserIds`는 프론트가 반드시 동기화해야 한다.** 태그 텍스트만 지우고 id를 안 빼면 8.2 검증에서 **400**이 난다. 동작 자체는 옳지만 계약으로 못박아둬야 원인 모를 400으로 헤매지 않는다.
