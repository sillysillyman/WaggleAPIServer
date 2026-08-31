# WaggleAPIServer 컨벤션

이 문서는 코드 작성·리뷰 시 따라야 할 규약을 정의한다. 새 코드는 이 컨벤션을 준수할 것.

## 1. 스택 & 구조

- Kotlin 1.9.25 / Spring Boot 3.5.6 / Java 21, Gradle Kotlin DSL
- 주요 의존성: Spring Web · Data JPA · Data Redis · Security · OAuth2 · WebSocket · Validation, Flyway + MySQL, AWS S3, Bucket4j, springdoc-openapi, JJWT, uuid-creator
- 패키지 루트: `io.waggle.waggleapiserver.{common, domain, security}`
- 도메인 하위 구조 (예: `domain/team/`):
  - `*Controller.kt` — REST 엔드포인트
  - `Team.kt` — 엔티티
  - `dto/{request,response}/` — 요청/응답 DTO
  - `repository/` — JPA 레포지토리
  - `service/` — 비즈니스 로직
  - `enums/` — 도메인 enum
- **컨트롤러가 둘 이상인 도메인은 `controller/` 하위로 모을 것** (예: `domain/comment/controller/`). 하나뿐이면 도메인 루트에 그대로 둔다.
  - `domain/message/`는 아직 루트에 둘을 두고 있다 — 이 규칙 도입 이전 코드이며, 정리 대상.
- **리포지토리 메서드는 반환 타입 순으로 선언할 것**: 단순 타입(`Boolean`/`Int`/`Long`) → 엔티티 단건(`Entity?`) → 목록(`List<Entity>`, `List<Projection>`) → 쓰기(`Unit`: `update*`/`delete*`/`markAs*`).
  - 프로젝션 인터페이스는 리포지토리 인터페이스 아래 같은 파일에 둔다 (`ApplicationRepository`의 `PostApplicantCount` 전례).

## 2. 엔티티 / 영속성

- 수정 추적과 soft delete가 필요한 엔티티는 `AuditingEntity`를 상속할 것 (`createdAt`, `updatedAt`, `deletedAt` 자동 관리).
  - 예외: append-only 성격이거나 hard delete만 하는 엔티티는 자체 관리. 예) `Notification`은 `read_at`으로 상태를 추적하고 `created_at`만 자체 관리, `Bookmark`는 복합키 + 토글 시 hard delete.
- **Soft delete 필터**: soft delete 대상 엔티티는 `@Entity` 아래에 `@SQLRestriction("deleted_at IS NULL")`을 붙일 것. 항상 적용되므로 켜는 코드가 필요 없고, `em.find()`(=`findByIdOrNull`)와 트랜잭션 밖 호출까지 모두 가려진다.
  - **`AuditingEntity`(`@MappedSuperclass`)에 붙이면 상속되지 않는다** (실측 확인). 엔티티마다 각각 붙일 것.
  - **native 쿼리에는 적용되지 않는다.** restriction은 끌 수 없으므로 native가 삭제된 행에 접근하는 유일한 경로다.
  - 이미 삭제된 행을 조회/수정하는 쿼리(`deleted_at IS NOT NULL` 조건 포함) 작성 시 **반드시 `nativeQuery = true`**, 컬럼명은 snake_case로 작성할 것. 재가입 시 멤버·팔로우 복원과 탈퇴자 조회가 여기 의존한다 (`findByIdIgnoringDeletion`, `findByUserIdAndTeamIdIncludingDeleted` 등).
  - 메서드 이름에 `DeletedAt`·`IncludingDeleted`·`IgnoringDeletion` 키워드가 들어가면 native 여부를 한 번 더 확인할 것.
  - **삭제된 행을 되살릴 때는 같은 트랜잭션 안에서 native로 로드해 dirty checking에 맡길 것.** detached 상태로 `save()`를 부르면 `merge()`가 restriction에 막혀 행을 못 찾고 INSERT를 시도한다 (`Duplicate entry` 또는 `StaleObjectStateException`). 재가입 멤버 복원(`ApplicationService`), 재팔로우(`FollowService`), 재가입 사용자(`CustomOAuth2UserService`)가 모두 이 형태다.
- **엔티티 변경은 영속성 컨텍스트에만 반영되고 DB로는 즉시 나가지 않는다.** 리포지토리 조회는 JPQL·derived·`@Query(nativeQuery = true)` 모두 자동 flush가 선행되므로 수동 `flush()`를 부르지 말 것 (JPA `FlushModeType.AUTO`, 실측 확인).
  - 예외는 **테스트의 `JdbcTemplate`**뿐이다. Hibernate를 우회하므로 flush가 트리거되지 않아 **변경 이전 상태를 읽는다.** 통합 테스트에서 `count(...)`로 검증할 때 주의할 것.

    ```kotlin
    comment.delete()                                        // 메모리에만 반영
    commentRepository.existsByPostIdAndParentId(...)        // ✓ 자동 flush 후 조회
    jdbcTemplate.queryForObject("SELECT ... FROM comments") // ✗ 변경 이전 상태를 봄
    ```
  - 벌크 UPDATE 후 같은 트랜잭션에서 그 엔티티를 다시 읽어야 하면 `@Modifying(clearAutomatically = true)`를 쓴다. 현재 코드베이스는 cascade 벌크 UPDATE 뒤에 해당 엔티티를 읽지 않아 기본값으로 충분하다.
- **타임스탬프 UPDATE 쿼리**: native 쿼리 + MySQL `UTC_TIMESTAMP(6)` 사용. 앱에서 `Instant.now()`를 파라미터로 전달하지 말 것.
  - 이유: prod는 JDBC URL에 `serverTimezone=UTC`가 설정되어 있으나 local은 미설정 → `CURRENT_TIMESTAMP`는 환경 의존적. `UTC_TIMESTAMP()`는 세션 timezone 무관하게 항상 UTC를 반환한다.
  - `datetime(6)` 컬럼이므로 정밀도 손실을 막기 위해 `UTC_TIMESTAMP(6)`를 쓸 것 (괄호 없는 형태 금지).
- **엔티티의 비영속 프로퍼티는 반드시 `get() =`로 쓸 것. 초기화식(`= value`)은 백킹 필드를 만들어 유령 컬럼이 된다.**

  ```kotlin
  override val type: BookmarkType = BookmarkType.POST        // ✗ 백킹 필드 → posts.type 컬럼을 찾다 에러
  override val type: BookmarkType get() = BookmarkType.POST  // ✓ getter만 생성, Hibernate가 무시
  ```

  `Bookmarkable` 같은 인터페이스 구현 시 특히 실수하기 쉽다. `@Access(AccessType.FIELD)`는 이 문제를 막아주지 않는다 — 오히려 필드를 보게 하는 애너테이션이다. JPA 접근 방식은 `@Id`가 필드에 붙는 것으로 이미 FIELD로 결정되므로 `@Access`는 붙이지 말 것.
- ID 전략: 사용자(`User`)는 `UuidCreator.getTimeOrderedEpoch()` (UUID v7 계열), 일반 도메인은 Long auto-increment.
- **시간순 정렬은 `created_at`이 아니라 `id` 기준으로 할 것** (Long auto-increment 도메인 기준). id가 삽입 순서와 단조 증가해 정렬 결과가 `created_at`과 동일하고(동일 시각 tie는 id가 더 정확), `WHERE <equality-prefix> ORDER BY id`는 InnoDB가 secondary index에 PK(`id`)를 덧붙여 filesort 없이 처리된다.
  - `created_at` 전용 정렬 컬럼/인덱스를 신설하지 말 것. 시간순 인덱스는 `(team_id)`, `(user_id)`처럼 equality 컬럼만 만들면 PK가 자동으로 붙는다.
  - `role, created_at` 같은 복합 정렬의 tie-break도 `role, id`로 작성할 것.

## 3. Controller / API

- OpenAPI 문서화: 컨트롤러에 `@Tag`, 메서드에 `@Operation` 부여할 것.
- **중첩 리소스는 shallow nesting**: 컬렉션(생성·목록)만 부모 경로 아래 두고, 개별 항목(수정·삭제)은 최상위에 둔다. 자식 id가 전역 고유하므로 `PUT /posts/1/comments/42`는 `postId`가 중복 정보이고, 검증하지 않으면 `/posts/999/comments/42`도 통과해버린다.

  ```
  POST   /posts/{postId}/comments   PostCommentController
  GET    /posts/{postId}/comments   PostCommentController
  PUT    /comments/{commentId}      CommentController
  DELETE /comments/{commentId}      CommentController
  ```

  - **두 컨트롤러 모두 자식 도메인(`domain/comment/controller/`)에 두고 부모 컨트롤러에 얹지 말 것.** 부모에 얹으면 그 엔드포인트가 부모 태그로 묶여 한 도메인 API가 Swagger에서 쪼개진다. `/teams/{teamId}/posts`와 `POST /teams/{teamId}/applications`가 각각 "모집글"·"팀 지원"이 아니라 **"팀"** 으로 나가는 것이 실제 사례다 (정리 대상).
  - **두 컨트롤러에 같은 `@Tag(name = ...)`을 줄 것.** OpenAPI 태그는 문자열 매칭이라 컨트롤러가 달라도 한 그룹으로 합쳐진다.
  - **메서드 레벨 `@Tag`로 때우려 하지 말 것.** 클래스 레벨 태그를 덮어쓰지 않고 **더해져서** 해당 오퍼레이션이 두 그룹에 중복 노출된다. 위 두 항목은 `/v3/api-docs`를 실제로 뽑아 확인했다.
  - URL 계층상의 부모는 경로 변수로, 그 외 참조는 요청 본문으로 받는다. 예) `POST /teams/{teamId}/applications`는 `teamId`가 경로, `postId`가 본문.
- **쿼리 파라미터 객체 바인딩**: `@ParameterObject` (springdoc) 사용. `@ModelAttribute`는 Swagger에서 개별 쿼리 파라미터가 아닌 JSON 객체로 표시되므로 금지.
- 페이지네이션: 공통 DTO `common/dto/request/CursorGetQuery`, `common/dto/response/CursorResponse` 사용.
- **프로필 완성 가드** — 두 메커니즘이 공존하므로 상황에 맞게 선택할 것:

  | 상황 | 사용 |
  |---|---|
  | 메서드 본문에서 user 사용 | `@CurrentUser user: User` (resolver 부수효과로 자동 가드) |
  | 메서드 본문에서 user 미사용 | `@RequireCompleteProfile` (인터셉터, 파라미터 없이 가드만) |
  | 가드 면제 (조회 등) | `@AllowIncompleteProfile` |
  | 인증 자체가 선택적 (비로그인도 허용) | `@CurrentUser user: User?` (nullable) |

  - 가드 트리거만을 위해 미사용 `@CurrentUser user: User` 파라미터를 끼워두지 말 것 — `@RequireCompleteProfile`로 의도를 명시할 것.
  - SecurityConfig는 인증(JWT)만 처리. 프로필 완성은 위 메커니즘으로만 강제됨.

## 4. DTO

- **Response DTO는 companion object의 `of()` 또는 `from()` 정적 팩토리로만 생성할 것.** Service/Controller에서 생성자 직접 호출 금지.
  - 도메인 → DTO 변환 로직을 DTO에 캡슐화. 필드 추가 시 팩토리 시그니처와 호출처를 같이 갱신.
  - **팩토리 파라미터 순서는 생성자 할당 순서와 일치시킬 것.** 필드를 추가할 때 파라미터를 목록 끝에 붙이지 말고 할당되는 자리에 맞춰 끼워넣는다.

    ```kotlin
    fun of(post: Post, user: …, recruitments: … = emptyList(), commentCount: Long = 0, applicationStatus: … = null) =
        PostDetailResponse(
            …
            recruitments = recruitments,
            commentCount = commentCount,        // 파라미터 순서와 동일
            applicationStatus = applicationStatus,
        )
    ```
- 같은 필드에 여러 애너테이션을 붙일 때 **import 순서와 동일한 순서로 위→아래 정렬할 것.**

  ```kotlin
  @field:Valid          // jakarta.validation.Valid
  @field:NotNull        // jakarta.validation.constraints.NotNull
  @field:UniquePosition // 커스텀 제약
  val recruitments: List<RecruitmentUpsertRequest>,
  ```

## 5. 예외 처리

- `BusinessException(ErrorCode)` 또는 `BusinessException.of(ErrorCode, message?)`로 던질 것.
- HTTP 매핑은 `common/exception/ErrorCode` enum에 추가. `GlobalExceptionHandler`가 일괄 변환.

## 6. 코드 스타일

- spotless + ktlint 1.3.1 적용. **Kotlin 파일 수정을 마무리할 때마다 `./gradlew spotlessApply` 실행할 것.**
  - 컴파일 실패 상태에선 spotless도 실패하므로 컴파일 성공 후 실행.
  - 개별 Edit마다 돌리지 말고 작업(요청받은 기능/수정) 완료 시점에 한 번.
- **주석은 명사형 종결어미로 쓸 것.** `-한다`/`-된다`/`-없다` 같은 서술형 대신 `-함`/`-됨`/`-불필요`처럼 끝낸다. Kotlin·SQL 주석 모두 해당.

  ```kotlin
  // 삭제-답글 경합으로 고아 답글이 생기는 것을 막는다.   ✗
  // 삭제-답글 경합으로 생기는 고아 답글 방지용.            ✓

  // 1-depth이므로 재귀가 필요 없다. depth를 늘릴 때 while 루프가 된다.  ✗
  // 1-depth이므로 재귀 불필요. depth 확장 시 while 루프가 됨.           ✓
  ```
- **검증 함수 네이밍**: 조건을 어기면 던지는 함수는 `check*`, `Boolean`을 반환하면 `is*`로 쓸 것.
  - `check*`의 뒷부분은 **명사구·형용사구**로 둘 것. `checkTargetExists`처럼 술어문이면 `Boolean`을 반환할 것처럼 읽힌다 (`checkOwnership`, `checkMemberRole`, `checkCompleted`, `checkProfileComplete`, `checkAllRequiredAgreed` 전례).
  - 이름이 실제 검사 범위를 담게 할 것. 좋아요 대상 검증은 존재뿐 아니라 soft delete·tombstone까지 막으므로 `checkLikableTarget`이다.
- **boolean 리터럴 인자는 named argument로 넘길 것.** 호출부에서 `true`만 봐선 무엇을 뜻하는지 알 수 없다.

  ```kotlin
  LikeResponse.of(true, count)                  // ✗
  LikeResponse.of(liked = true, likeCount = …)  // ✓
  ```

  - 반대로 **변수명이 파라미터명과 같으면 positional로 둘 것.** `LikeId(type = type, targetId = targetId)`는 순수한 중복이다.
- **변수명에 줄임말 사용 금지** (msg/conv/etc). full name(`message`, `conversation`)을 쓸 것. 프로덕션·테스트 모두 적용.
- **컬렉션 변수 네이밍**:
  - `List<T>` → `applicationIds`, `members` (복수형 `-s/-es/-ies`)
  - `Map<K, V>` → `teamById`, `memberCountByTeamId` (`valueByKey`)
  - `Map<Pair, V>` → `unreadCountByUserIdToTeamId` (`valueByKeyToValue`)
  - `Set<T>` → `readApplicationIdSet` (`~Set`)
  - `Set<Pair>` → `userIdToReadApplicationIdSet` (`keyToValueSet`, 수식어는 value를 수식)
- **라인 길이 해소를 위한 임의 변수 추출 금지.** 원래 표현식을 유지한 채 인라인 줄바꿈으로 처리할 것:
  - 문자열: `"..." + "..."` 연결
  - 함수 호출: 인자 줄바꿈
  - 긴 표현식: 연산자 기준 줄바꿈
- **`"..." + "..."` 문자열 연결은 최후 수단.** 우선순위:
  1. Kotlin raw string `"""..."""` (`trimIndent()`는 어노테이션 인자에선 사용 불가)
  2. 문자열 interpolation
  3. 그래도 안 되면 `+` 연결 (사용자에게 양해 구할 것)

## 7. 작업 방식

- **버그 수정은 그 버그만 잡는 최소 변경.** "이왕 손대는 김에" 식의 defensive over-engineering, premature optimization, 미래 시나리오 대비 리팩터링을 같이 묶지 말 것.
- **Spring 표준 idiomatic 패턴 우선.** 예:
  - raw `TransactionSynchronization` 대신 `@TransactionalEventListener`
  - 수동 schedule 대신 `@Scheduled`
- **계층 간 결합도 주의.** API 응답 DTO를 infrastructure 이벤트 페이로드에 박아넣지 말 것.
- 변경 후 "이 수정이 정말 이 버그를 잡는 데 필요한가?"를 자문하고, 불필요한 변경은 빼서 diff를 최소화할 것.
- 트레이드오프가 있는 선택지는 옵션을 제시하되, 추천은 보수적·idiomatic 쪽으로.

## 8. Git / PR

- **브랜치 네이밍**: `<type>/<kebab-topic>` — type은 `feat`, `fix`, `hotfix`, `refactor`, `chore`, `docs`, `ci` 중 하나. 토픽은 영문 kebab-case (예: `feat/post-image-embed`, `fix/missing-cascade-on-delete`).
- **리모트 구조** — fork 기반 triangular workflow (읽기는 `upstream`, 쓰기는 `origin`):

  | 리모트 | 저장소 | 역할 |
  |---|---|---|
  | `upstream` | `Team-Waggle/WaggleAPIServer` | 진실의 원천. 읽기 전용으로 취급 — 직접 push 금지 |
  | `origin` | `sillysillyman/WaggleAPIServer` | 개인 포크. 모든 push 대상 |

- **베이스 브랜치는 항상 `upstream/main`.** `origin/main`은 fork sync 시점에 따라 뒤처져 있을 수 있으므로 파생 소스로 쓰지 말 것.

  ```bash
  git fetch upstream                              # 캐시된 remote-tracking ref 갱신
  git switch -c <type>/<topic> upstream/main
  git push -u origin <type>/<topic>
  ```

  - `upstream/main`에서 파생하면 Git이 `branch.autoSetupMerge` 기본값 탓에 추적 대상을 `upstream/main`으로 잡는다. 맨 `git push`가 upstream으로 나가는 사고를 막기 위해 `git config remote.pushDefault origin`을 걸어둘 것.
  - 작업 브랜치를 `upstream`에 직접 push하지 말 것.
- **PR은 `origin:<브랜치>` → `upstream:main`.** `gh` 사용 시 base를 명시할 것:

  ```bash
  gh pr create --repo Team-Waggle/WaggleAPIServer --base main --head sillysillyman:<브랜치>
  ```

  - 머지 후 GitHub "Sync fork"로 `origin/main`을 갱신한다. 이 동기화는 포크 정리용일 뿐, 다음 작업의 선행 조건이 아니다 (파생은 항상 `upstream/main`에서 하므로).
  - fork sync가 만드는 `Merge branch 'Team-Waggle:main' into main` 머지 커밋이 upstream 히스토리로 역유입되지 않게 하는 것도 위 파생 규칙의 목적이다.
- **커밋 메시지**: `<type>(<scope>): <한글 설명>`
  - `<scope>`은 브랜치 토픽 (예: 브랜치 `fix/security-vulnerabilities` → `fix(security-vulnerabilities): ...`).
  - 브랜치 내 커밋들의 type이 반드시 같을 필요는 없다 — 예) `fix/security-vulnerabilities` 브랜치 안에 `docs(security-vulnerabilities): 보안 점검 리포트 추가` 가능.
  - 한 커밋은 한 가지 결함/기능에 집중. 변경 폭이 넓으면 여러 커밋으로 분리.
  - **body는 산문이 아니라 `-` 불릿 목록 형식 사용**:
    ```
    fix(scope): subject

    - 첫 번째 변경 사항
    - 두 번째 변경 사항
    ```
- **PR 제목**: 동일하게 `<type>(<scope>): <한글 설명>`. 여러 커밋을 묶는 PR이면 제목은 커밋들의 상위 요약으로 한 단계 추상화 (예: PR #109 `fix(missing-cascade-on-delete): 누락된 cascade 정리 결함 수정` — 하위에 `fix(missing-cascade-on-delete): ...` 커밋 여러 개).

## 9. 인프라 (참고)

- 프로덕션은 EC2 + Docker 기반 (nginx 리버스 프록시 + Spring API 컨테이너). 구체 호스트/도메인은 별도 운영 문서 참조.
- 환경 차이: prod는 JDBC `serverTimezone=UTC`, local은 미설정 — `UTC_TIMESTAMP()` 컨벤션(2번)의 근거.
- SSL은 Let's Encrypt standalone 모드 + nginx stop/start hook으로 갱신 (갱신 시 짧은 다운타임 발생).
