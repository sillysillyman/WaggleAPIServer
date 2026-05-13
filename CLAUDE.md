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

## 2. 엔티티 / 영속성

- 수정 추적과 soft delete가 필요한 엔티티는 `AuditingEntity`를 상속할 것 (`createdAt`, `updatedAt`, `deletedAt` 자동 관리).
  - 예외: append-only 성격이거나 hard delete만 하는 엔티티는 자체 관리. 예) `Notification`은 `read_at`으로 상태를 추적하고 `created_at`만 자체 관리, `Bookmark`는 복합키 + 토글 시 hard delete.
- **Soft delete 필터**: `AuditingEntity`의 `@Filter("deleted_at IS NULL")`가 `SoftDeleteFilterAspect`를 통해 모든 `@Transactional` 메서드에서 활성화된다. JPQL은 이 필터의 영향을 받지만 native 쿼리는 받지 않는다.
  - 이미 삭제된 행을 조회/수정하는 쿼리(`deleted_at IS NOT NULL` 조건 포함) 작성 시 **반드시 `nativeQuery = true`**, 컬럼명은 snake_case로 작성할 것.
  - 메서드 이름에 `DeletedAt` 키워드가 들어가면 native 여부를 한 번 더 확인할 것.
- **타임스탬프 UPDATE 쿼리**: native 쿼리 + MySQL `UTC_TIMESTAMP(6)` 사용. 앱에서 `Instant.now()`를 파라미터로 전달하지 말 것.
  - 이유: prod는 JDBC URL에 `serverTimezone=UTC`가 설정되어 있으나 local은 미설정 → `CURRENT_TIMESTAMP`는 환경 의존적. `UTC_TIMESTAMP()`는 세션 timezone 무관하게 항상 UTC를 반환한다.
  - `datetime(6)` 컬럼이므로 정밀도 손실을 막기 위해 `UTC_TIMESTAMP(6)`를 쓸 것 (괄호 없는 형태 금지).
- ID 전략: 사용자(`User`)는 `UuidCreator.getTimeOrderedEpoch()` (UUID v7 계열), 일반 도메인은 Long auto-increment.

## 3. Controller / API

- OpenAPI 문서화: 컨트롤러에 `@Tag`, 메서드에 `@Operation` 부여할 것.
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
- 베이스 브랜치는 항상 `main`.
- **커밋 메시지**: `<type>(<scope>): <한글 설명>`
  - `<scope>`은 브랜치 토픽 (예: 브랜치 `fix/security-vulnerabilities` → `fix(security-vulnerabilities): ...`).
  - 브랜치 내 커밋들의 type이 반드시 같을 필요는 없다 — 예) `fix/security-vulnerabilities` 브랜치 안에 `docs(security-vulnerabilities): 보안 점검 리포트 추가` 가능.
  - 한 커밋은 한 가지 결함/기능에 집중. 변경 폭이 넓으면 여러 커밋으로 분리.
- **PR 제목**: 동일하게 `<type>(<scope>): <한글 설명>`. 여러 커밋을 묶는 PR이면 제목은 커밋들의 상위 요약으로 한 단계 추상화 (예: PR #109 `fix(missing-cascade-on-delete): 누락된 cascade 정리 결함 수정` — 하위에 `fix(missing-cascade-on-delete): ...` 커밋 여러 개).

## 9. 인프라 (참고)

- 프로덕션은 EC2 + Docker 기반 (nginx 리버스 프록시 + Spring API 컨테이너). 구체 호스트/도메인은 별도 운영 문서 참조.
- 환경 차이: prod는 JDBC `serverTimezone=UTC`, local은 미설정 — `UTC_TIMESTAMP()` 컨벤션(2번)의 근거.
- SSL은 Let's Encrypt standalone 모드 + nginx stop/start hook으로 갱신 (갱신 시 짧은 다운타임 발생).
