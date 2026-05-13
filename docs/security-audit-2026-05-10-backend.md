# 보안 점검 리포트 (백엔드) — 2026-05-10

| 항목 | 내용 |
|---|---|
| 점검일 | 2026-05-10 |
| 브랜치 | `fix/security-vulnerabilities` (점검 시점 기준 main 동일) |
| 점검 범위 | WaggleAPIServer (Spring Boot Kotlin) — API 코드, 인프라 설정 |
| 경로 | `C:\Users\user\IdeaProjects\WaggleAPIServer` |
| 점검 영역 | 인증·인가, 입력 검증, 인젝션, 인프라/설정, 시크릿, CORS, 토큰 관리 |
| 관련 문서 | 프론트 점검 결과는 [security-audit-2026-05-10-frontend.md](security-audit-2026-05-10-frontend.md) |

---

## 0. 한눈에 보기

| ID | 제목 | 심각도 | 처리 |
|---|---|---|---|
| C2 | OAuth 콜백이 access 토큰을 URL 쿼리스트링으로 전달 | 🔴 Critical | ✅ BE OTT redeem 구현 (FE 동시 배포 필요) |
| H3 | MySQL/Redis 컨테이너 포트가 호스트로 노출 | 🟠 High | ✅ |
| H4 | Docker 컨테이너 root 실행 + JDK 이미지 사용 | 🟠 High | ✅ |
| H6 | API nginx CSP / Referrer-Policy 등 보안 헤더 부재 | 🟠 High | ✅ |
| H7 | Refresh 토큰 rotation 부재 | 🟠 High | ✅ |
| H8 | fail2ban 필터 과민 (정상 사용자 ban 위험) | 🟠 High | ✅ |
| M10 | STOMP `MessageSendRequest`에 `@Valid` 누락 | 🟡 Medium | ✅ |
| ~~M11~~ | ~~`CursorGetQuery.size` 상한 부재~~ | 🟡 Medium | ⊘ false positive — 이미 적용됨 |
| ~~M12~~ | ~~`MemberUpdateRoleRequest`에 `@Valid` 누락~~ | 🟡 Medium | ⊘ false positive — 이미 적용됨 |
| M13 | prod CORS에 localhost origin 포함 + `allowCredentials=true` | 🟡 Medium | ✅ |
| M14 | `GlobalExceptionHandler`가 Spring 예외 메시지 raw 노출 | 🟡 Medium | ✅ |
| M17 | Kakao 미인증 이메일로 다른 사용자 가입 차단(DoS) | 🟡 Medium | ✅ |
| L1 | `RateLimitFilter`가 unverified `X-Forwarded-For` 신뢰 | 🟢 Low | ✅ |
| L2 | `AuthCookieManager`가 `Set-Cookie` 중복 송신 | 🟢 Low | ✅ |
| L3 | Docker 이미지 floating tag (digest 미고정) | 🟢 Low | ✅ (fail2ban 한정) |
| L4 | DTO 일부에 `@Size` 상한 없음 (Post/Team/Message 본문) | 🟢 Low | ✅ |

**처리 상태 범례**
- ✅ 이 브랜치(`fix/security-vulnerabilities`)에서 처리됨
- ⏸ 보류 (외부 협의/추가 결정 필요)
- ⊘ false positive 또는 이미 다른 PR에서 처리됨

> ℹ️ 백엔드는 `updatePost`에서 `teamId` 변조를 이미 차단함 (PR #116, `post-team-immutable` — [PostUpdateRequest.kt](src/main/kotlin/io/waggle/waggleapiserver/domain/post/dto/request/PostUpdateRequest.kt)에 teamId 필드 없음). 다만 프론트는 여전히 편집 UI에 팀 선택 컨트롤이 있고 PUT 페이로드에 teamId를 포함해 송신함 → [frontend 문서 M9](security-audit-2026-05-10-frontend.md#m9-편집-폼이-팀-선택-ui--teamid-페이로드를-유지) 참조.

> ℹ️ M11/M12는 점검 후 코드 재확인 결과 모든 `@RequestBody`/`@ParameterObject`에 `@Valid`가 적용되어 있고 `CursorGetQuery.size`에 `@Min(1)`/`@Max(100)`이 이미 있음. 에이전트가 stale 코드 기준으로 잡은 false positive로 판명. PR a4459cf 일괄 처리 시점에 이미 해결됨.

> ℹ️ L3은 `mysql:8.0`, `redis:7-alpine`, `nginx:alpine`은 기존 minor 버전 핀이 적절하다고 판단하여 변경하지 않음. `:latest`로 떠 있던 `crazymax/fail2ban`만 `1.1.0`으로 핀.

---

## 1. Critical

### C2. OAuth 콜백이 access 토큰을 URL 쿼리스트링으로 전달 (백엔드 작업분)

- **위치**: [OAuth2LoginSuccessHandler.kt:32-39](src/main/kotlin/io/waggle/waggleapiserver/security/oauth2/OAuth2LoginSuccessHandler.kt#L32)
- **심각도**: Critical
- **영향**: OAuth 로그인하는 모든 사용자
- **연관**: 프론트 작업분은 [frontend 문서 C2](security-audit-2026-05-10-frontend.md#c2-oauth-콜백이-access-토큰을-url-쿼리스트링으로-전달-프론트-작업분)
- **처리 상태**: ✅ Option B (OTT) 채택 구현 완료. FE 짝 작업 후 동시 배포 필요.

#### 적용된 구현

1. `ErrorCode.OAUTH_OTT_INVALID` 추가 ([ErrorCode.kt](src/main/kotlin/io/waggle/waggleapiserver/common/exception/ErrorCode.kt))
2. `AuthService.issueOttForOAuth(userId, role, response)` — access 토큰을 Redis `oauth-ott:$uuid`에 1분 저장 후 OTT 반환 ([AuthService.kt](src/main/kotlin/io/waggle/waggleapiserver/domain/auth/service/AuthService.kt))
3. `AuthService.redeemOtt(ott)` — `getAndDelete`로 1회용 보장
4. `OAuth2LoginSuccessHandler` — redirect URL을 `?accessToken=...`에서 `?ott=$uuid`로 변경 ([OAuth2LoginSuccessHandler.kt](src/main/kotlin/io/waggle/waggleapiserver/security/oauth2/OAuth2LoginSuccessHandler.kt))
5. `POST /auth/oauth/redeem` 엔드포인트 추가 ([AuthController.kt](src/main/kotlin/io/waggle/waggleapiserver/domain/auth/AuthController.kt))
6. `SecurityConfig`의 permitAll에 `/auth/oauth/redeem` 추가 ([SecurityConfig.kt](src/main/kotlin/io/waggle/waggleapiserver/security/config/SecurityConfig.kt))

#### 현재 코드

```kotlin
val accessToken = authService.issueTokens(userId, role, response)

val targetUrl =
    UriComponentsBuilder
        .fromUriString(redirectUri)
        .queryParam("accessToken", accessToken)
        .build()
        .toUriString()

redirectStrategy.sendRedirect(request, response, targetUrl)
```

#### 문제

OAuth Authorization Code Flow에서 표준적으로 URL 쿼리스트링에 실리는 것은 **authorization code** (1회용, 10초 수명, client_secret 없으면 사용 불가)다. 그러나 현재 waggle 구현은 자체 발급한 **access JWT** (재사용 가능, 1시간 유효, bearer만으로 전체 API 인증)를 같은 자리에 싣는다.

이 모양은 OAuth 2.1에서 보안 사유로 제거된 **Implicit Flow** (`response_type=token`)와 동일하다. RFC 9700 §2.1.2가 명시적으로 금지한다.

URL 쿼리스트링으로 토큰이 흐를 때 발생하는 유출 채널:

1. **브라우저 history**: 영구 보관
2. **Referer 헤더**: 로그인 직후 페이지가 외부 리소스 요청 시 송신
3. **서버 access log**: nginx access.log, 사이의 모든 프록시가 full URL 기록
4. **분석 SDK**: GA, Sentry, Datadog RUM은 URL 캡처가 기본
5. **북마크/스크린샷 공유**: 사용자가 그 URL 그대로 공유

#### 공격 시나리오

- 로그인 직후 사용자가 게시글의 외부 링크 클릭 → 브라우저가 `Referer` 헤더에 `https://waggle.lol/login?accessToken=eyJ...` 송신 → 외부 서버 운영자가 토큰 획득 → 1시간 동안 victim 사칭 가능
- 운영 서버 nginx access.log를 볼 수 있는 누구나 모든 OAuth 로그인 사용자의 토큰 수집 가능

#### 수정 방법 (택1)

**Option A — URL Fragment** (가장 작은 변경)

Fragment(`#` 뒤)는 서버로 송신되지 않으므로 access log/Referer 누출이 차단된다.

```kotlin
val targetUrl = "$redirectUri#accessToken=$accessToken"
redirectStrategy.sendRedirect(request, response, targetUrl)
```

**Option B — One-Time Token (OTT) 교환** (가장 안전, 권장)

이미 WebSocket용으로 구현된 패턴([AuthService.kt:75-83](src/main/kotlin/io/waggle/waggleapiserver/domain/auth/service/AuthService.kt#L75)의 `issueWsToken`/`validateAndConsumeWsToken`)을 OAuth 콜백에도 적용.

```kotlin
// 1) redirect 시 OTT만 발급, access 토큰은 Redis에 1분 보관
val ott = UUID.randomUUID().toString()
redisTemplate.opsForValue().set(
    "oauth-ott:$ott",
    accessToken,
    Duration.ofMinutes(1),
)
val targetUrl = "$redirectUri?ott=$ott"
```

```kotlin
// 2) 교환 엔드포인트
@PostMapping("/oauth/exchange")
fun exchange(@RequestBody body: OttExchangeRequest): AccessTokenResponse {
    val accessToken = redisTemplate.opsForValue().getAndDelete("oauth-ott:${body.ott}")
        ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Invalid or expired OTT")
    return AccessTokenResponse(accessToken)
}
```

OTT는 1회용이고 1분이면 만료되어 유출돼도 무해. Implicit Flow → Authorization Code 패턴으로 격상.

**Option C — httpOnly 쿠키로 임시 access 토큰 전달**

BE가 access 토큰을 짧은 만료(예: 30초)의 httpOnly 쿠키로 set하고 redirect → FE는 도착 직후 `/auth/me` 호출로 인증 확인 → 쿠키는 만료. 단점: 쿠키 만료 사이에 사용자가 사이트를 닫으면 인증 상실.

#### 검증 방법

- 로그인 후 nginx access.log에 `accessToken=` 쿼리 파라미터가 기록되지 않는지 확인
- 브라우저 history에 `accessToken=` 패턴이 남지 않는지 확인
- DevTools Network → 외부 도메인 요청 시 `Referer` 헤더에 토큰이 없는지 확인

#### ⚠️ 배포 주의

BE/FE 동시 배포 필수. BE만 fragment로 바꾸고 FE는 query 읽으면 로그인 깨짐. 단계적 전환:

1. BE가 한동안 fragment + query 둘 다 송신
2. FE 배포 후 충분한 캐시 기간(2주~) 대기
3. BE에서 query 송신 제거

또는 feature flag로 제어.

#### 참고

- [RFC 9700 §2.1.2 — Implicit grant deprecated](https://datatracker.ietf.org/doc/rfc9700/)

---

## 2. High

### H3. MySQL/Redis 컨테이너 포트가 호스트로 노출

- **위치**: [docker-compose.yml](docker-compose.yml) (`database`, `redis` 서비스의 `ports:` 매핑)
- **심각도**: High
- **영향**: 운영 데이터 전체

#### 문제

`database` 서비스가 `3306:3306`, `redis` 서비스가 `6379:6379`로 호스트 포트를 publish한다. 앱 컨테이너는 도커 내부 네트워크(`DB_HOST: database`)로 접근하므로 호스트 publish는 불필요하다. EC2 보안 그룹이 한 번 잘못 풀리거나 (예: 인바운드 0.0.0.0/0 임시 허용 → 원복 누락) 운영자가 다른 서비스용으로 SG 오픈 시 인터넷에 DB가 직결된다.

MySQL은 `caching_sha2_password` 기본 사용으로 평문 비번 인증은 없지만, 비번 brute-force / CVE 노출 / `LOAD DATA LOCAL` 등 부가 공격면이 즉시 열린다. Redis는 더 위험 — 비밀번호 미설정 시 누구나 `FLUSHALL`/`KEYS *`/`SET` 가능.

#### 수정 방법

```yaml
# docker-compose.yml
database:
  # ports:  ← 제거
  #   - "3306:3306"
  expose:
    - "3306"  # 도커 네트워크 내부에만 노출

redis:
  # ports:  ← 제거
  #   - "6379:6379"
  expose:
    - "6379"
```

DB에 접속해서 점검할 일이 있으면 `docker exec -it waggle-database mysql -u ...` 또는 SSH 터널로 처리.

#### 검증 방법

```bash
# 호스트에서 → 연결 거부되어야 함
nc -zv localhost 3306
nc -zv localhost 6379

# 외부에서 EC2 public IP 대상 → 거부되어야 함
nmap -p 3306,6379 <EC2_PUBLIC_IP>
```

---

### H4. Docker 컨테이너 root 실행 + JDK 이미지 사용

- **위치**: [Dockerfile:2-14](Dockerfile#L2)
- **심각도**: High
- **영향**: 컨테이너 침해 시 권한 상승 표면

#### 문제

1. `eclipse-temurin:21-jdk-alpine`를 베이스로 사용 — JDK는 컴파일러/도구 포함, 런타임에선 JRE면 충분. 공격면 불필요하게 큼.
2. `USER` 지시어가 없어 컨테이너 내부에서 `root`로 실행됨. 앱 RCE 발생 시 컨테이너 내 모든 작업 가능 + (구성에 따라) 호스트로의 escape 가능성 증가.

#### 수정 방법

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 비특권 사용자 생성
RUN addgroup -S app && adduser -S app -G app

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 파일 소유권 이전
RUN chown -R app:app /app

USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### 검증 방법

```bash
docker exec waggle-app id
# uid=...(app) gid=...(app) groups=...(app)  ← root 아니어야 함

docker exec waggle-app java -version
# OpenJDK Runtime Environment ... (JRE만 있어야 함)
```

---

### H6. API nginx CSP / Referrer-Policy 등 보안 헤더 부재

- **위치**: [nginx/nginx.conf:38-40](nginx/nginx.conf#L38)
- **심각도**: High
- **영향**: API 응답에 대한 방어층 부재
- **연관**: 프론트 서빙 측 헤더는 [frontend 문서 H6](security-audit-2026-05-10-frontend.md#h6-csp--referrer-policy-등-보안-헤더-부재-프론트-서빙-측)

#### 문제

API nginx에 설정된 헤더는 `HSTS`, `X-Frame-Options`, `X-Content-Type-Options` 정도. API 응답에 추가로 필요한 헤더:

- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` — API는 자체 리소스 로드를 안 하므로 strict하게
- `Referrer-Policy: no-referrer` — 에러 페이지에서도 토큰 누출 방지 (C2와 직결)
- `Cross-Origin-Resource-Policy: same-site` — 외부 사이트가 API 응답을 직접 fetch 못 하게

#### 수정 방법

API 측 nginx (현재 [nginx/nginx.conf](nginx/nginx.conf)):

```nginx
# server 블록 또는 location / 내부에
add_header Content-Security-Policy "default-src 'none'; frame-ancestors 'none'" always;
add_header Referrer-Policy "no-referrer" always;
add_header Cross-Origin-Resource-Policy "same-site" always;
add_header Permissions-Policy "interest-cohort=()" always;
```

API에 직접 접근하는 경우는 거의 없지만(주로 SPA를 통한 fetch), 응답 헤더는 일관성 있게 설정.

#### 검증 방법

```bash
curl -I https://api.waggle.lol/posts | grep -iE "(csp|referrer|cross-origin)"
# 위 헤더들이 모두 응답에 있어야 함
```

---

### H7. Refresh 토큰 rotation 부재

- **위치**: [AuthService.kt:44-61](src/main/kotlin/io/waggle/waggleapiserver/domain/auth/service/AuthService.kt#L44)
- **심각도**: High
- **영향**: 인증된 모든 사용자

#### 현재 코드

```kotlin
fun refresh(refreshToken: String): AccessTokenResponse {
    if (!jwtProvider.isTokenValid(refreshToken)) {
        throw BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")
    }

    val userId = jwtProvider.getUserIdFromToken(refreshToken)
    val role = jwtProvider.getRoleFromToken(refreshToken)

    val stored = redisTemplate.opsForValue().get("refresh-token:$userId")
        ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token not found")

    if (stored != refreshToken) {
        throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token mismatch")
    }

    return AccessTokenResponse(jwtProvider.generateAccessToken(userId, role))
}
```

#### 문제

`/auth/refresh`가 access 토큰만 재발급한다. Refresh 토큰은 7일 동안 동일하게 유지됨. 따라서:

- Refresh 토큰이 한 번 새면(예: 백업 디스크 유출, 디버깅 로그 캡처) 7일간 무한 갱신 가능
- 도용 탐지 신호 부재 — 공격자와 정상 사용자가 같은 refresh로 access 토큰을 발급받아도 시스템이 모름
- 비밀번호 변경(현재는 OAuth-only라 해당 없지만 향후 추가 시) / 계정 정지 후에도 refresh로 다시 살아남
- Access 토큰 폐기 메커니즘 부재(별도 이슈)와 결합되면 침해 대응 시간이 길어짐

#### 수정 방법

매 refresh마다 새 refresh JWT를 발급하고 Redis 키 교체. 이전 토큰 재사용 시 (정상이면 발생 안 함) 침해로 간주하고 family 전체 폐기.

```kotlin
fun refresh(
    refreshToken: String,
    response: HttpServletResponse,
): AccessTokenResponse {
    if (!jwtProvider.isTokenValid(refreshToken)) {
        throw BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")
    }

    val userId = jwtProvider.getUserIdFromToken(refreshToken)
    val role = jwtProvider.getRoleFromToken(refreshToken)

    val stored = redisTemplate.opsForValue().get("refresh-token:$userId")
        ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token not found")

    if (stored != refreshToken) {
        // 재사용 탐지 → 즉시 family 전체 폐기 (강제 로그아웃)
        redisTemplate.delete("refresh-token:$userId")
        // (선택) 보안 알림 이벤트 발행
        throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token reuse detected")
    }

    // Rotation: 새 access + 새 refresh 발급
    val newAccessToken = jwtProvider.generateAccessToken(userId, role)
    val newRefreshToken = jwtProvider.generateRefreshToken(userId, role)

    redisTemplate.opsForValue().set(
        "refresh-token:$userId",
        newRefreshToken,
        Duration.ofMillis(refreshTokenTtl),
    )
    val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
    authCookieManager.addRefreshTokenCookie(response, newRefreshToken, maxAgeSeconds)

    return AccessTokenResponse(newAccessToken)
}
```

`AuthController.refresh`도 `HttpServletResponse` 받아서 넘기도록 시그니처 조정.

> ⚠️ 동시성: 같은 사용자의 두 탭이 동시에 `/auth/refresh` 호출하면 한쪽은 reuse로 판정될 수 있음. 짧은 grace window (마지막 토큰 1개를 추가로 인정, 5초 정도) 두거나, 프론트에서 refresh 호출을 mutex로 직렬화. AWS Cognito는 grace window 방식.

#### 검증 방법

```bash
# 1) 로그인 → refreshToken=A 쿠키 받음
# 2) /auth/refresh 호출 → Set-Cookie로 refreshToken=B 받아야 함 (A와 달라야 함)
# 3) /auth/refresh를 A로 다시 호출 → 401 + Redis 키 삭제됨
# 4) /auth/refresh를 B로 호출 → 401 (이미 폐기됨)
```

#### FE 영향

FE는 무변경. 쿠키 갱신은 서버가 응답에 새 `Set-Cookie`로 자동 처리.

---

### H8. fail2ban 필터 과민 (정상 사용자 ban 위험)

- **위치**: [fail2ban/filter.d/nginx-bad-request.conf:2](fail2ban/filter.d/nginx-bad-request.conf#L2)
- **심각도**: High
- **영향**: 정상 사용자 (특히 CGNAT/공유 IP)

#### 문제

regex가 모든 403/404/405를 ban 트리거로 잡는다. 정상 사용자가 다음 행동을 했을 때도 카운트됨:

- 삭제된 게시글/팀 페이지 접근 → 404
- 비로그인 상태에서 인증 필요한 API 호출 → 403
- 봇 크롤러가 `/wp-admin` 등 흔한 경로 시도 (정상이지만 카운트)

CGNAT 환경(국내 일부 모바일 통신사, 학교/회사 NAT)에서는 같은 외부 IP를 다수 사용자가 공유. 한 명의 행동이 전체 사용자 ban으로 이어짐.

#### 수정 방법

명백히 의심스러운 패턴만 카운트:

```ini
# fail2ban/filter.d/nginx-bad-request.conf
[Definition]
failregex = ^<HOST> .* "(?:GET|POST|HEAD) (?:/wp-admin|/wp-login|/\.env|/\.git|/phpmyadmin|/\.aws|/admin\.php|/xmlrpc\.php|/cgi-bin) HTTP/[\d.]+" \d+ .*$
            ^<HOST> .* "[A-Z]+ /[^"]*\.(?:php|asp|aspx|jsp|cgi|env|sql) HTTP/[\d.]+" \d+ .*$
            ^<HOST> .* " 444 .*$

ignoreregex =
```

또는 임계값 조정 (현재 maxretry/findtime이 무엇이든 더 관대하게).

#### 검증 방법

- 로컬에서 `fail2ban-regex /var/log/nginx/access.log fail2ban/filter.d/nginx-bad-request.conf` 로 실제 로그 매칭 확인
- 정상 사용자 경로(404 페이지 방문 등)가 매칭되지 않는지 확인

---

## 3. Medium

### M10. STOMP `MessageSendRequest`에 `@Valid` 누락

- **위치**: [MessageStompController.kt:18](src/main/kotlin/io/waggle/waggleapiserver/domain/message/MessageStompController.kt#L18), [MessageSendRequest.kt:11-15](src/main/kotlin/io/waggle/waggleapiserver/domain/message/dto/request/MessageSendRequest.kt#L11)
- **심각도**: Medium

#### 문제

DTO에 `@field:NotBlank`/`@field:NotNull`이 선언되어 있으나, `@Payload` 파라미터에 `@Valid`가 없어서 `PayloadMethodArgumentResolver`가 검증을 트리거하지 않는다. 결과: 빈 content / null receiverId 등 제약 위반 페이로드가 그대로 service 로직에 도달.

#### 수정 방법

```kotlin
@Controller
class MessageStompController(...) {
    @MessageMapping("/messages")
    fun sendMessage(
        @Payload @Valid request: MessageSendRequest,  // ← @Valid만 추가
        principal: Principal,
    ) { ... }
}
```

`PayloadMethodArgumentResolver`는 파라미터의 `@Valid` 또는 `@Validated`만 검사하므로 **클래스 레벨 `@Validated`는 불필요**. 클래스 레벨 `@Validated`는 `MethodValidationPostProcessor`가 AOP로 메서드 파라미터 제약(`@Min`/`@Max` on primitives)을 처리할 때 쓰는 별개 메커니즘.

검증 실패 시 `MethodArgumentNotValidException`이 STOMP 에러 프레임으로 라우팅됨 (Spring Boot 자동 설정의 기본 동작). 별도 핸들러 불필요.

#### 검증 방법

- 빈 content STOMP 프레임 송신 → 메시지 저장되지 않고 에러 응답
- 단위 테스트에서 `@Validated` 클래스에 잘못된 payload 넣고 검증

---

### M11. `CursorGetQuery.size` 상한 부재 (메모리 DoS)

- **위치**: [CursorGetQuery.kt:21-22](src/main/kotlin/io/waggle/waggleapiserver/common/dto/request/CursorGetQuery.kt#L21)
- **심각도**: Medium

#### 문제

```kotlin
val size: Int,  // ← 상한 없음
```

`PageRequest.of(0, cursorQuery.size + 1)`로 사용됨. 공격자가 `?size=10000000` 같은 값을 전달하면:

- Hibernate가 거대 결과셋 로드 시도 → OOM
- 음수 전달 시 `IllegalArgumentException` → 500 에러
- 정수 오버플로우(`Int.MAX_VALUE`) → 예측 불가 동작

`/applications`, `/notifications`, `/conversations` 등 모든 커서 페이지네이션 API에 영향.

#### 수정 방법

```kotlin
@ParameterObject
data class CursorGetQuery(
    @Schema(description = "커서")
    val cursor: String? = null,
    @Schema(description = "페이지 크기")
    @field:Min(1)
    @field:Max(100)
    val size: Int = 20,
)
```

추가로 `@ParameterObject`는 컨트롤러 메서드 파라미터에 `@Valid` 있어야 검증 트리거됨 — 컨트롤러에 `@Validated` 클래스 레벨 추가 필요할 수 있음.

#### 검증 방법

- `?size=999999` → 400 응답 (validation error)
- `?size=0`, `?size=-1` → 400
- `?size=20` → 정상

---

### M12. `MemberUpdateRoleRequest`에 `@Valid` 누락

- **위치**: [MemberController.kt:46](src/main/kotlin/io/waggle/waggleapiserver/domain/member/MemberController.kt#L46), [MemberUpdateRoleRequest.kt:10](src/main/kotlin/io/waggle/waggleapiserver/domain/member/dto/request/MemberUpdateRoleRequest.kt#L10)
- **심각도**: Medium

#### 문제

DTO에 `@field:NotNull`이 있으나 컨트롤러에서 `@Valid` 누락. `{ "role": null }` 페이로드가 통과되어 service 단까지 도달. 최근 PR a4459cf에서 다른 DTO들에 `@Valid`를 일괄 추가했는데 이 하나가 빠짐.

#### 수정 방법

```kotlin
@PatchMapping("/{teamId}/members/{memberId}/role")
fun updateMemberRole(
    @PathVariable teamId: Long,
    @PathVariable memberId: Long,
    @Valid @RequestBody request: MemberUpdateRoleRequest,  // @Valid 추가
    @CurrentUser user: User,
): ResponseEntity<...> { ... }
```

전체 컨트롤러 일괄 점검 권장 — `@RequestBody` 있는 모든 메서드에 `@Valid`가 붙어 있는지 grep:

```bash
# @RequestBody인데 @Valid 없는 패턴 찾기
grep -B1 "@RequestBody" --include="*Controller.kt" -r src/ | grep -v "@Valid"
```

---

### M13. prod CORS에 localhost origin 포함 + `allowCredentials=true`

- **위치**: [SecurityConfig.kt:85-88](src/main/kotlin/io/waggle/waggleapiserver/security/config/SecurityConfig.kt#L85)
- **심각도**: Medium

#### 현재 코드

```kotlin
configuration.allowedOrigins = listOf(
    "http://localhost:3000",
    "http://localhost:5173",
    "https://waggle.lol",
)
configuration.allowCredentials = true
```

#### 문제

운영 서버도 `localhost:3000`/`5173`을 신뢰한다 + 자격증명 포함 허용. 공격면:

- 사용자가 로컬에서 악성 dev 서버 실행 시 (예: `npx some-untrusted-tool`이 5173에 띄움) → 그 서버가 https://api.waggle.lol에 자격증명 포함 cross-origin 요청 가능
- DNS rebinding 공격으로 외부 도메인이 `127.0.0.1`로 해석되도록 유도 → 같은 표면 노출

#### 수정 방법

profile별로 분리:

```yaml
# application-local.yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:5173
    - https://waggle.lol  # 로컬에서 prod API 직접 콜 시 (선택)

# application-prod.yaml
cors:
  allowed-origins:
    - https://waggle.lol
```

```kotlin
@Configuration
class SecurityConfig(
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>,
    ...
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600
        ...
    }
}
```

#### 검증 방법

```bash
# prod 환경에서
curl -H "Origin: http://localhost:3000" -I https://api.waggle.lol/posts
# 응답에 Access-Control-Allow-Origin: http://localhost:3000 이 없어야 함

curl -H "Origin: https://waggle.lol" -I https://api.waggle.lol/posts
# 응답에 Access-Control-Allow-Origin: https://waggle.lol 있어야 함
```

---

### M14. `GlobalExceptionHandler`가 Spring 예외 메시지 raw 노출

- **위치**: [GlobalExceptionHandler.kt:103-117](src/main/kotlin/io/waggle/waggleapiserver/common/exception/GlobalExceptionHandler.kt#L103)
- **심각도**: Medium

#### 문제

`handleExceptionInternal` 오버라이드가 Spring MVC 프레임워크 예외(`HttpMessageNotReadableException`, `MissingServletRequestParameterException` 등)에 대해 `e.message`를 그대로 응답에 포함. Jackson 파싱 에러는 내부 클래스명/필드 경로/타입 정보를 메시지에 담는다:

```
JSON parse error: Cannot deserialize value of type `io.waggle.waggleapiserver.domain.user.UserRole` from String "INVALID": ...
```

→ 패키지 구조, 도메인 enum 이름, 내부 타입 노출.

#### 수정 방법

```kotlin
override fun handleExceptionInternal(
    ex: Exception,
    body: Any?,
    headers: HttpHeaders,
    statusCode: HttpStatusCode,
    request: WebRequest,
): ResponseEntity<Any>? {
    log.warn("MVC exception: {}", ex.message, ex)  // 서버 로그에만 기록

    val errorResponse = ErrorResponse.of(
        ErrorCode.INVALID_REQUEST,
        // raw message 대신 고정 메시지
        "Invalid request format",
    )
    return ResponseEntity.status(statusCode).body(errorResponse)
}
```

검증 실패(`MethodArgumentNotValidException`)는 사용자에게 어떤 필드가 왜 잘못됐는지 알려야 하므로 별도로 처리 — `BindingResult`에서 필드명만 추출, 내부 타입 정보는 빼고.

#### 검증 방법

- `POST /users` body에 잘못된 JSON → 응답에 `io.waggle...` / Jackson 내부 메시지가 안 나와야 함
- 정상적인 비즈니스 에러(BusinessException)는 기존대로 메시지 노출 (이건 의도된 메시지)

---

### M17. Kakao 미인증/누락 이메일로 가입 시 보안 + UX 이슈

- **위치**:
  - [CustomOAuth2UserService.kt](src/main/kotlin/io/waggle/waggleapiserver/security/oauth2/CustomOAuth2UserService.kt) (이메일 검증 로직)
  - [OAuth2UserInfo.kt](src/main/kotlin/io/waggle/waggleapiserver/security/oauth2/OAuth2UserInfo.kt) (provider별 이메일 추출)
  - [ErrorCode.kt](src/main/kotlin/io/waggle/waggleapiserver/common/exception/ErrorCode.kt) (`OAUTH_EMAIL_NOT_VERIFIED`, `OAUTH_EMAIL_MISSING`)
- **심각도**: Medium

#### 문제

같은 이메일로 다른 OAuth provider 가입 시 `DUPLICATE_RESOURCE` 예외 발생 (`existsByEmail` 체크). 그런데 Kakao는:
- 사용자가 이메일 동의를 거부할 수도 있고 (Kakao 콘솔 "선택 동의" 설정 시)
- 동의해도 `is_email_verified=false`인 상태로 이메일이 반환됨
- 또한 Kakao 콘솔에 **"사용자에게 값이 없는 경우 카카오계정 정보 입력을 요청하여 수집"** 옵션이 활성화되어 있으면, 이메일 없는 사용자에게 즉석 입력시켜서 받음 → 이때도 `is_email_verified=false`

공격 시나리오:
1. 공격자가 이메일 없는 Kakao 계정으로 (또는 victim 이메일 미리 등록 후) waggle 가입 시도
2. Kakao가 victim 이메일을 즉석 등록하라고 요청 → 공격자가 `victim@gmail.com` 입력
3. waggle 수신: `email=victim@gmail.com`, `is_email_verified=false`
4. waggle DB에 `victim@gmail.com` 잠김 → 진짜 victim의 Google 가입 차단

계정 탈취는 아니지만 **신규 가입 차단(DoS)** 가능.

#### 수정 방법

두 단계 검증을 `loadUser` 진입 직후에 배치:

```kotlin
// CustomOAuth2UserService.kt
val email = userInfo.email
if (email.isNullOrBlank()) {
    throw BusinessException(
        ErrorCode.OAUTH_EMAIL_MISSING,
        "Email not provided by ${userInfo.provider}",
    )
}
if (!userInfo.isEmailVerified) {
    throw BusinessException(
        ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
        "Email not verified by ${userInfo.provider}",
    )
}
```

`OAuth2UserInfo.email`을 `String?`으로 변경하여 누락 가능성을 타입에 명시. `KakaoUserInfo`/`GoogleUserInfo`의 email 접근자도 `as? String`로 안전 캐스트:

```kotlin
// OAuth2UserInfo.kt
override val email: String?
    get() = kakaoAccount["email"] as? String
```

`isEmailVerified`는 provider별로:
- Google: `attributes["email_verified"]` 사용 (Google은 항상 verified 보장하나 명시 체크)
- Kakao: `is_email_verified` AND `is_email_valid` (둘 다 true여야 통과)

#### 추가 권장: Kakao 콘솔 옵션

[Kakao 개발자 콘솔 → 카카오 로그인 → 동의항목 → 카카오계정(이메일) → 설정]에서:

- **"사용자에게 값이 없는 경우 카카오계정 정보 입력을 요청하여 수집" 체크박스를 해제**하면 이메일 없는 사용자가 동의 화면에서 막혀서 waggle 서버로 도달하지 않음. UX 손실은 있으나 (이메일 없는 Kakao 사용자 가입 불가) defense in depth.

서버측 `OAUTH_EMAIL_MISSING` 체크와 결합하면 belt-and-suspenders.

#### 검증 방법

- 이메일 없는 Kakao 테스트 계정으로 OAuth 시도 → 401 `OAUTH_EMAIL_MISSING` 응답
- 미인증 이메일 Kakao 계정 → 401 `OAUTH_EMAIL_NOT_VERIFIED` 응답
- 정상(인증된 이메일) → 가입 성공

#### FE 안내 UX

가입 실패 시 두 에러 코드를 구분해서 안내:
- `OAUTH_EMAIL_MISSING` → "Kakao 계정에 이메일을 등록 후 다시 시도해주세요"
- `OAUTH_EMAIL_NOT_VERIFIED` → "Kakao에서 이메일 인증을 완료 후 다시 시도해주세요"

---

## 4. Low / Info

### L1. `RateLimitFilter`가 unverified `X-Forwarded-For` 신뢰

- **위치**: [RateLimitFilter.kt:66-72](src/main/kotlin/io/waggle/waggleapiserver/security/filter/RateLimitFilter.kt#L66)
- **심각도**: Low (nginx 설정에 따라 다름)

`X-Forwarded-For` 헤더 첫 번째 IP를 클라이언트 IP로 신뢰. 공격자가 헤더를 위조해서 IP-bucket을 우회 가능. 단, **운영 nginx에서 클라이언트 헤더를 strip하고 자체 헤더로 재설정한다면 안전**.

#### 검증/수정

[nginx/nginx.conf](nginx/nginx.conf)에서 `proxy_set_header X-Forwarded-For $remote_addr;` (덮어쓰기) 또는 `$proxy_add_x_forwarded_for` (append) 사용 여부 확인. Append 방식이면 가장 마지막(또는 nginx가 추가한 부분)을 신뢰해야 함. Spring `forward-headers-strategy: native`라 nginx의 헤더를 그대로 신뢰하므로, **nginx 단에서 클라 헤더 strip이 필수**.

```nginx
# 클라가 보낸 X-Forwarded-* 헤더 무시
real_ip_header X-Forwarded-For;
set_real_ip_from <neighbor_proxy_or_nothing>;

# 또는 명시적 덮어쓰기
proxy_set_header X-Forwarded-For $remote_addr;
```

---

### L2. `AuthCookieManager`가 `Set-Cookie` 중복 송신

- **위치**: [AuthCookieManager.kt:23,32](src/main/kotlin/io/waggle/waggleapiserver/domain/auth/AuthCookieManager.kt#L23)
- **심각도**: Low

`response.addCookie(cookie)` + `response.addHeader("Set-Cookie", ...)` 둘 다 호출 → 같은 쿠키가 중복 송신. 브라우저는 마지막 것을 사용하므로 동작상 문제는 없으나 응답 크기 낭비 + 디버깅 혼선.

#### 수정

둘 중 하나만 사용. SameSite 등 세부 속성 제어가 필요하면 `addHeader` 방식만 유지.

---

### L3. Docker 이미지 floating tag (digest 미고정)

- **위치**: [Dockerfile:2](Dockerfile#L2), [docker-compose.yml](docker-compose.yml)
- **심각도**: Low

`eclipse-temurin:21-jdk-alpine`, `mysql:8.0`, `redis:7-alpine`, **`crazymax/fail2ban:latest`** (가장 위험), `nginx:alpine` — 모두 floating tag. `:latest`는 supply chain 공격 표면.

#### 수정

```yaml
fail2ban:
  image: crazymax/fail2ban:0.11.2-r5@sha256:abc123...  # digest까지 고정
```

최소한 `:latest`는 명시적 버전으로 교체.

---

### L4. DTO 일부에 `@Size` 상한 없음 (Post/Team/Message 본문)

- **위치**:
  - [PostUpsertRequest.kt:15-19](src/main/kotlin/io/waggle/waggleapiserver/domain/post/dto/request/PostUpsertRequest.kt#L15)
  - [TeamUpsertRequest.kt:13-16](src/main/kotlin/io/waggle/waggleapiserver/domain/team/dto/request/TeamUpsertRequest.kt#L13)
  - [MessageSendRequest.kt:15](src/main/kotlin/io/waggle/waggleapiserver/domain/message/dto/request/MessageSendRequest.kt#L15)
- **심각도**: Low

`@NotBlank`만 있고 길이 상한 없음. 1MB 텍스트 페이로드 저장 가능 → DB 컬럼 오버플로우 또는 거대 응답으로 메모리/대역폭 낭비.

#### 수정

각 도메인의 비즈니스 룰에 맞춰 `@Size(max=...)` 추가. 예: 게시글 제목 100, 본문 5000, 팀명 30, 팀 소개 500, 메시지 2000 등. DB 컬럼 길이와 일치시킬 것.

---

## 5. 검증된 안전 항목

점검에서 깨끗하게 확인된 영역. 향후 변경 시에도 유지할 것.

### SQL Injection
- 모든 `nativeQuery=true` 쿼리가 named parameter 사용 (`:userId`, `:teamId` 등)
- JPQL `@Query`도 동일
- JPA Specifications/Criteria/QueryDSL 미사용
- 메모리 노트의 soft-delete/timestamp UPDATE 패턴 (`UTC_TIMESTAMP(6)`) 모두 안전
- FULLTEXT `MATCH...AGAINST(:q IN BOOLEAN MODE)` 파라미터화됨

### Git History
- `*.pem`, `*.key`, `*.p12`, `.env`가 한 번도 커밋된 적 없음
- `.env.example`은 placeholder만 (`DB_PASSWORD=your_db_password` 형태)
- `git log -p -S "JWT_SECRET="` / `"DB_PASSWORD="` 검색에서 실제 시크릿 흔적 없음

### Response DTO
- `password` 필드 자체가 없음 (OAuth-only 시스템)
- `provider`, `providerId`, `role`, `deletedAt` 응답에서 노출되지 않음
- `email`도 다른 사용자 응답에선 제외 (PR b74979c)
- `of()`/`from()` 정적 팩토리 컨벤션 일관성 있게 적용됨

### Mass Assignment
- `UserUpdateRequest`, `UserSetupProfileRequest`, `TeamUpsertRequest`에 `role`, `leaderId`, `verified` 같은 권한 상승 필드 없음
- `UserRole.ADMIN`은 enum에 존재하나 사용처 없음 (dead code)

### XXE / 역직렬화
- XML 파싱 코드 없음
- Jackson `activateDefaultTyping` 비활성 (기본값 유지)

### Logging
- 요청 body / 토큰 / 비밀번호 로깅 없음
- JWT 인증 필터는 성공 시 `userId`만 로깅

### File Upload
- S3 presigned PUT 방식, 파일명은 서버 생성 UUID + ext (path traversal 불가)
- `ImageContentType` enum으로 JPEG/PNG/WEBP만 허용
- 사용자 파일명을 echo 하지 않음

### SSRF
- 외부 HTTP는 Discord webhook 한 곳뿐, URL은 환경변수 (사용자 입력 아님)
- `WebUrlValidator`는 입력 검증용이며 서버에서 fetch하지 않음

### OAuth
- Authorization Code Flow + state 파라미터 (Spring Security 기본)
- 토큰 교환은 서버 간 (백엔드만 client_secret 보유)
- JWT 알고리즘 hard-pin (`verifyWith(key)` → `alg=none` confusion 불가)

### IDOR / 권한 검증
- 게시글/팀/지원서 모든 수정 엔드포인트에서 `post.checkOwnership(user.id)`, `member.checkMemberRole(MANAGER/LEADER)`, `findByIdAndUserId` 등 일관되게 적용
- `@CurrentUser` resolver는 `SecurityContextHolder`의 `UserPrincipal`만 신뢰 — request body/param에서 사용자 ID 받는 endpoint 없음

### 운영 설정
- `application-prod.yaml`: `ddl-auto: validate`, `show-sql: false`, h2 console off
- Actuator: `/health`만 노출, `show-details: never`
- Swagger UI: nginx에서 `.htpasswd` basic auth로 보호 (이중 방어 권장)
- GitHub Actions: `${{ secrets.* }}` 정상 사용, leak 없음

### 의존성 (build.gradle.kts)
- Spring Boot 3.5.6, Spring Security, Jackson, Logback 모두 최신 stable
- jjwt 0.12.5, AWS SDK 2.29.51, springdoc 2.8.13
- Log4Shell 시대 잔재 없음, 수상한 핀 없음

---

## 6. 권장 처리 순서

### Phase 1 — 즉시 (1주 이내)

가장 영향이 크고 fix가 작은 것들.

1. **H3** — 인프라: docker-compose `ports:` → `expose:` (30분)
2. **H4** — 인프라: Dockerfile 비특권 USER + JRE (1시간)
3. **M13** — prod CORS profile 분리 (1시간)
4. **M11** — `CursorGetQuery.size` 상한 (30분)
5. **M12** — `MemberUpdateRoleRequest @Valid` (10분)

### Phase 2 — 단기 (2주 이내)

조율 또는 약간의 리팩터 필요.

6. **C2** — FE와 협의 → OTT 패턴 권장. BE/FE 동시 배포 + feature flag (3-5일)
7. **H7** — Refresh rotation + reuse 탐지 (1-2일, FE 무변경)
8. **H6** — API nginx 헤더 추가 (반나절)
9. **M14** — `GlobalExceptionHandler` 메시지 sanitize (반나절)
10. **M10** — STOMP `@Validated` (반나절, STOMP 에러 핸들러도 손봐야 할 수 있음)

### Phase 3 — 중기 (1달 이내)

11. **H8** — 인프라: fail2ban 필터 좁히기 (1일, 기존 ban 로그로 검증)
12. **M17** — Kakao `is_email_verified` 검증 (반나절)

### Phase 4 — 정리 (여유 있을 때)

13. **L1** — nginx 헤더 strip 확인 (점검만)
14. **L2** — 쿠키 중복 송신 정리 (10분)
15. **L3** — 이미지 digest 핀 (1일)
16. **L4** — DTO `@Size` 상한 (반나절)

### 별도 운영 작업

- **시크릿 회전**: 점검 결과 git history에 시크릿 leak은 없음. 별도 회전 불필요. 단, 운영 환경의 `JWT_SECRET`, DB 비밀번호, OAuth client secret 등이 충분히 길고(`≥32 random bytes`) 정기 회전되는지 운영팀 확인 권장.
- **`waggle-key.pem`**: gitignore 처리됨. 단 작업 디렉터리 루트에 평문으로 보관 중이므로 디스크 암호화 + 백업에서 제외 확인.
- **`.env.example`**: prod `.env`에서 `COOKIE_SECURE=true` 설정되어 있는지 확인 (placeholder는 false).

---

## 7. 점검 방법론

이 점검은 다음 방법으로 수행됨:

- 4개의 병렬 sub-agent로 영역 분담 (인증인가 / 입력검증·인젝션 / 인프라·설정 / 프론트)
- 각 agent는 직접 코드를 read하여 분석 (자동화 스캐너 미사용)
- `git log --all -p -S "..."` 로 git history 시크릿 검색
- OWASP Top 10, RFC 9700 (OAuth BCP), OWASP Cheat Sheet Series 기준
- 발견 후 사용자(repo 소유자)와 대화로 false positive (M9) 1건 제거

다음 점검 시 이 문서와 비교하여 회귀/신규 항목 분류 가능.

---

## 8. 참고 자료

- [OWASP Top 10 (2021)](https://owasp.org/Top10/)
- [OWASP API Security Top 10 (2023)](https://owasp.org/API-Security/editions/2023/en/0x00-header/)
- [RFC 9700 — OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/rfc9700/)
- [OWASP Cheat Sheet — JWT for Java](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [OWASP Cheat Sheet — REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
- [Mozilla Web Security — HTTP Headers](https://infosec.mozilla.org/guidelines/web_security)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Auth0 — Refresh Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)
