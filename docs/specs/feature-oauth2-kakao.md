# 작업 명세: feature-oauth2-kakao — OAuth2 카카오 소셜 로그인

> 산출: ym-spec, 2026-07-20. 상태: **spec_done** (Q1~Q10 사용자 결정 대기)
> 브랜치 후보: `feature/oauth2-kakao` / 포트폴리오 강화 트랙

---

## 0. 개념 설명 (학습 프로젝트 규칙 — 개념 + 비교 우선)

### 0-1. OAuth2 Authorization Code Flow 란

지금까지 이 프로젝트의 로그인은 **우리 서버가 직접 자격증명(이메일+비밀번호)을 검증**하는 구조였다 (`formLogin` + `DaoAuthenticationProvider` + BCrypt). OAuth2 로그인은 검증 책임을 **외부 인증 제공자(카카오)에 위임**하고, 우리는 "카카오가 이 사용자를 인증했다"는 증거(access token)만 받아 회원을 식별한다.

Authorization Code Flow 왕복 (서버 사이드 웹앱 표준 — 우리 케이스):

```
[브라우저]                [우리 서버 (Client)]                [카카오 (Authorization Server / Resource Server)]
    │  ① GET /oauth2/authorization/kakao
    │ ────────────────────────►│
    │  ② 302 → kauth.kakao.com/oauth/authorize?client_id=..&redirect_uri=..&state=..
    │ ◄────────────────────────│
    │  ③ 카카오 로그인 + 동의 화면 ────────────────────────────────►│
    │  ④ 302 → {우리서버}/login/oauth2/code/kakao?code=..&state=..◄│
    │ ────────────────────────►│
    │                          │  ⑤ POST kauth.kakao.com/oauth/token (code → access_token)  [서버 간 통신]
    │                          │  ⑥ GET  kapi.kakao.com/v2/user/me  (access_token → 사용자 정보)
    │                          │  ⑦ CustomOAuth2UserService: providerId 로 User 조회/생성 → UserPrincipal
    │  ⑧ 302 / (성공 핸들러)   │
    │ ◄────────────────────────│
```

- **code(인가 코드) 를 브라우저에, token 을 서버 간 통신에 분리**하는 것이 이 플로우의 핵심 보안 설계 — access token 이 브라우저 URL 에 노출되지 않는다.
- `state` 파라미터는 CSRF 방어 (Spring 이 자동 생성·검증).
- Spring Security 의 `oauth2Login()` DSL 이 ①~⑥ 전 구간을 자동 처리한다. 우리가 작성하는 것은 ⑦ (사용자 정보 → 우리 도메인 User 매핑) 과 ⑧ (성공 후 라우팅) 뿐이다.

### 0-2. formLogin vs oauth2Login 비교 (기존 코드와의 대응)

| 항목 | formLogin (현재) | oauth2Login (이번 추가) |
|---|---|---|
| 자격증명 검증 | 우리 DB (BCrypt matches) | 카카오가 수행 |
| 사용자 로딩 | `UserDetailsService.loadUserByUsername` (`UserService.java:21`) | `OAuth2UserService.loadUser` (신규 `CustomOAuth2UserService`) |
| principal 타입 | `UserDetails` (`UserPrincipal`) | 기본은 `DefaultOAuth2User` — **`UserDetails` 아님** (§5 통합 필요) |
| 트리거 URL | `POST /login` | `GET /oauth2/authorization/kakao` (Spring 자동 매핑) |
| 콜백 URL | 없음 | `GET /login/oauth2/code/kakao` (Spring 자동 매핑) |
| 비밀번호 | 필수 (BCrypt) | **없음** (§3 정책 필요) |
| remember-me | PersistentToken 발급 | 발급 안 됨 — `remember-me` 파라미터가 콜백 요청에 없음. 세션 로그인만 (한계로 명시) |
| CSRF | POST 폼 + 토큰 | ① 은 GET 이라 무관, `state` 가 별도 방어 |

### 0-3. 카카오 provider 특성 (표준 provider 와 다른 점)

1. **`CommonOAuth2Provider` 미포함** — Spring 은 google/github/facebook/okta 4종만 URI 프리셋 제공. 카카오는 `provider.kakao.*` 4개 URI 를 yml 에 수동 등록해야 한다 (§2).
2. **client_secret 전송 방식** — 카카오 token endpoint 는 HTTP Basic 인증을 지원하지 않고 **POST body 파라미터**만 받는다 → `client-authentication-method: client_secret_post` 필수. (기본값 `client_secret_basic` 이면 토큰 교환 401 — 가장 흔한 카카오 연동 실패 원인)
3. **user-name-attribute 는 `id`** — 카카오 응답 최상위의 회원번호(Long). google 의 `sub` 에 해당.
4. **응답이 중첩 구조** — `properties.nickname`, `kakao_account.profile.nickname`, `kakao_account.email` 등. `DefaultOAuth2User` 는 평탄화하지 않으므로 파싱 헬퍼 필요 (§1-B).
5. **email 은 기본 미제공** — `account_email` scope 는 **비즈앱 전환** 후에만 동의 항목 활성화 가능. 개인 학습 앱 기본 상태에선 nickname 만 수집 가능 (→ Q3 의 근본 원인).
6. **client_secret 자체가 선택 항목** — 카카오 콘솔 [보안] 탭에서 활성화해야 발급. 활성화 권장 (코드 탈취 방어).

---

## 1. 디자인 출처 (3자산 모두 정독 — 결정 사항 인용)

- **prototype.tsx**: `LoginScreen` **L1863~1900**. `useState` 는 `id`, `pw` 2개뿐. 버튼은 로그인(primary L)·회원가입(secondary L) column 2개 (L1891~1894). **소셜 로그인 버튼·구분선 없음**. CTA 라우팅: 로그인 성공 → `go('home')` (L1869), 회원가입 → `go('signup')` (route 이동, 인라인 패널 아님)
- **prototype.html**: LoginScreen 섹션 (login.html 주석 기준 line 1217~1254) — tsx 와 동일 구성. 소셜 버튼 없음
- **HANDOFF.md**: **§5.5 (L428~436)** — "중앙 정렬 (400px) / 아이디·비밀번호 입력 / 아이디 저장 + 찾기 링크 / 로그인·회원가입 버튼". 소셜 로그인 언급 없음. §8 API 표 (L750) 도 `POST /api/auth/login` 뿐. `⚠️` 블록 (§12) 중 본 티켓 관련 항목 없음
- **wireframe.png**: 로그인 화면에 소셜 로그인 요소 없음
- **`sns_kakaotalk.png` 자산의 실제 용도**: tsx **L449~453, L471** — Footer 의 SNS 채널 아이콘 4종 (Instagram/YouTube/KakaoTalk/Facebook) 이며 **로그인 버튼용 자산이 아님**
- **비교 대상 Thymeleaf**: `templates/user/login.html` (auth-form-prototype 폼, L49~94)

### LoginScreen 상태 다이어그램 (tsx L1863~1900 + 본 티켓 추가분)

```
[비로그인] ── /login 진입
   │
   ├─ (기존) id/pw 입력 → POST /login ──성공──► / (defaultSuccessUrl)
   │                                └─실패──► /login?error (savedUsername 보존)
   │
   └─ (신규) [카카오로 시작하기] 클릭 → GET /oauth2/authorization/kakao
          → 카카오 동의 → /login/oauth2/code/kakao
             ├─ 기존 소셜 회원 ──► / (일반 로그인과 동일)
             ├─ 최초 로그인(신규 생성) ──► /welcome (온보딩, Q6)
             └─ 실패/거부 ──► /login?error (기존 alert 영역 재사용)
```

## 1-A. 자산 간 갭 (3자산 비교)

| 항목 | wireframe.png | prototype.html | prototype.tsx | 채택 |
|---|---|---|---|---|
| 소셜 로그인 버튼 | 없음 | 없음 | 없음 (L1891~1894 버튼 2개뿐) | **prototype 에 없는 신규 기능** — 임의 추가 아닌 사용자 발주 티켓이므로 충돌 아님. 단 버튼 디자인·배치는 자산 근거가 없어 **카카오 공식 디자인 가이드 기준으로 제안** (Q1) |
| 로그인 입력 필드·옵션 row | 있음 | 있음 | 있음 (L1877~1890) | 기존 구현 유지 — **변경 없음** |
| sns_kakaotalk.png | — | 푸터 아이콘 (L487) | 푸터 아이콘 (L452) | 로그인 버튼에 유용(流用) 금지 — 카카오 심볼 SVG 신규 (Q1) |

→ 본 티켓은 "prototype 에 없는 기능 추가" 유형. CLAUDE.md 협업 규칙 5 (기획서 일관성 — 충돌 시 고지) 에 따라 **로그인 화면 구성 변경분은 전부 Q1 로 사용자 승인** 받는다.

## 1-B. 데이터 모델 gap 표 (카카오 userinfo 응답 ↔ User 엔티티)

`GET https://kapi.kakao.com/v2/user/me` 응답 프로퍼티 전수 매핑 (scope: `profile_nickname` 기준):

| 카카오 응답 필드 | 의미 | 현재 User 엔티티 | 조치 |
|---|---|---|---|
| `id` (Long, 최상위) | 카카오 회원번호 — 불변 식별자 | ❌ | **`providerId` 컬럼 추가** (String 변환 저장) |
| — (제공자 구분) | | ❌ | **`provider` enum 컬럼 추가** (`AuthProvider.LOCAL/KAKAO`) — 기존 row 는 default LOCAL |
| `connected_at` | 연결 시각 | ❌ | 미저장 (BaseTimeEntity.createdAt 으로 충분) |
| `properties.nickname` | 닉네임 (legacy 경로) | `name` | `kakao_account.profile.nickname` 우선, fallback 으로 사용 |
| `kakao_account.profile.nickname` | 닉네임 | `name` (NOT NULL 50) | **`name` 에 저장** — 헤더 displayName 소스 |
| `kakao_account.profile.profile_image_url` | 프로필 이미지 | ❌ | 미저장 (User 에 이미지 필드 없음 — 후속 후보) |
| `kakao_account.email` | 이메일 | `email` (NOT NULL unique) | **scope 미수집** (비즈앱 필요) → 합성 email 저장 (Q3) |
| `kakao_account.is_email_verified` | 이메일 검증 여부 | — | email 미수집 동안 무관. 수집 전환 시 자동연동 안전판 (Q4) |
| — | 비밀번호 | `password` (nullable ✓ — `User.java:39~40` 이미 nullable) | **null 저장** — 스키마 변경 불필요. 소비 지점 파급은 §7 |
| — | 휴대폰 | `phone` (nullable ✓), `phoneVerified` (default false) | null / false 유지 (Q5) |
| — | birthDate/gender/주소 | nullable ✓ | null 유지 — 전부 nullable 이라 마이그레이션 없음 |

**마이그레이션 요약**: 추가 컬럼은 `provider`(NOT NULL default 'LOCAL' — `phoneVerified` 의 `columnDefinition` 패턴 재사용, `User.java:101`), `providerId`(nullable) 2개뿐. `ddl-auto: update` 에서 additive 라 안전. 유니크 제약 `(provider, provider_id)` 는 `@Table(uniqueConstraints=...)` 명시 (update 모드에서 신규 제약 생성됨).

---

## 2. application.yml — 카카오 수동 registration/provider 설정

카카오는 `CommonOAuth2Provider` 미포함이므로 registration + provider 양쪽을 수동 작성한다:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID:kakao-client-id-not-set}      # REST API 키
            client-secret: ${KAKAO_CLIENT_SECRET:}                      # 콘솔 [보안] 탭 발급 (Q9 주석 참조)
            client-authentication-method: client_secret_post            # ★ 카카오 필수 (basic 미지원)
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"           # ★ 템플릿 변수 — 로컬/배포 이원화 자동 해결 (§6)
            scope: profile_nickname                                      # email 은 비즈앱 전환 후 (Q3)
            client-name: Kakao
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id                                      # 카카오 회원번호
```

각 항목이 §0-1 플로우의 어느 단계인지: `authorization-uri`=②, `redirect-uri`=④, `token-uri`=⑤, `user-info-uri`=⑥.

### 미설정 환경(키 없는 로컬·CI) 기동 전략 — Q9

yml registration 은 `client-id` 가 **빈 문자열이면 Boot 기동 실패** (프로퍼티 검증). 두 방식 비교:

| 방식 | 내용 | 장점 | 단점 |
|---|---|---|---|
| **A안. sentinel default (권장)** | `${KAKAO_CLIENT_ID:kakao-client-id-not-set}` — 항상 기동 성공. 컨트롤러 `@ControllerAdvice` 또는 login 컨트롤러가 `kakaoLoginEnabled` (실키 주입 여부) 를 모델로 내려 버튼 조건부 렌더 | yml 이 학습 표준 형태 유지·코드 최소. `youthmoa.kakao.map-app-key` 빈값→미렌더 패턴 (`application.yml`, `CenterController`) 과 동일 철학 | sentinel 상태에서 버튼 URL 직접 진입 시 카카오 KOE101 에러 (버튼 숨김으로 실사용 차단) |
| B안. 조건부 Java config | yml registration 제거, `@ConditionalOnProperty` 로 `ClientRegistrationRepository` 를 코드 생성 + SecurityConfig 에서 `ObjectProvider` 로 있을 때만 `.oauth2Login()` DSL 적용 | 미설정 시 필터 자체가 없어 완전 차단 | 코드량 증가, yml 학습 가치 상실, SecurityConfig 분기 복잡 |

e2e 프로파일 (`application-e2e.properties`): sentinel 이 그대로 적용되므로 **추가 설정 불필요** (A안 채택 시). E2E 는 버튼 미노출 상태를 기본으로 검증 or 테스트용 가짜 키 주입해 노출 상태 검증 (§10).

---

## 3. 계정 통합 정책 — 카카오 사용자 ↔ 기존 email 가입자

### 3-1. 식별 모델 비교 (Q2)

| | **A안. User 엔티티 확장 (권장)** | B안. SocialAccount 별도 엔티티 |
|---|---|---|
| 구조 | `User.provider` (enum) + `User.providerId` + unique(provider, providerId) | `SocialAccount(id, provider, providerId, user @ManyToOne)` — User 1 : SocialAccount N |
| 조회 | `findByProviderAndProviderId` 1쿼리 | join 1회 추가 |
| 다중 provider (카카오+네이버 동시 연동) | 불가 — User 당 1 provider | 가능 |
| 기존 계정에 소셜 "연동 추가" | User row 의 provider 를 바꿔야 해서 부자연 (LOCAL 이면서 KAKAO 연동 표현 불가) | 자연스러움 |
| 코드/마이그레이션 비용 | 컬럼 2개, 최소 | 엔티티+Repository+시드 신규 |
| 프로젝트 규칙 정합 | — | 단방향 `@ManyToOne` 규칙과 호환 ✓ |

**권장: A안.** 이번 티켓 범위(카카오 1종 + 기존 계정 연동 없음, Q4)에서 B안은 과설계. 단 A안 채택 시에도 `provider` 를 enum 으로 두어 B안 전환 마이그레이션 경로(컬럼 → 엔티티 이관)를 열어둔다. 향후 "네이버 추가 + 계정 연동 기능" 티켓이 생기면 그때 B안으로 리팩터 (spec 에 전환 조건 명시해 둠).

### 3-2. 신규 생성 vs 기존 계정 연동 (Q4)

현재 scope(`profile_nickname`)로는 **카카오가 email 을 안 주므로 기존 email 가입자와 대조할 열쇠 자체가 없다** → 이번 티켓은 **"카카오 최초 로그인 = 항상 신규 User 생성"** 이 유일하게 가능한 정책.

향후 email scope 확보 시의 선택지 (이번 티켓 범위 아님 — 정책만 기록):

| 정책 | 내용 | 위험 |
|---|---|---|
| 자동 연동 | kakao email == 기존 User.email 이면 그 계정으로 로그인 | **계정 탈취 벡터** — 반드시 `is_email_verified=true` 확인 필수. 미검증 email 자동 연동은 금지 |
| 수동 연동 | 동일 email 발견 시 "기존 계정이 있습니다" → 비밀번호 입력 후 연동 | 안전. UX 1단계 추가 |
| 항상 분리 | email 겹쳐도 별도 계정 | 안전하나 사용자 혼란 (동일 email 2계정 — email unique 제약과 충돌하므로 실제론 불가) |

### 3-3. 합성 email (Q3)

`User.email` 은 NOT NULL unique + 로그인 아이디. 카카오 사용자는 email 이 없으므로:

| | **A안. 합성 email (권장)** | B안. email nullable 마이그레이션 | C안. 비즈앱 전환 + account_email scope |
|---|---|---|---|
| 방식 | `kakao_{providerId}@social.youthmoa.invalid` 저장 (`.invalid` 는 RFC 2606 예약 TLD — 실 발송 사고 원천 차단) | email null 허용으로 스키마 변경 | 실제 email 수집 |
| 파급 | 최소 — unique 충돌 없음 (providerId 유일) | find-id/find-password/loadUserByUsername 등 email 전제 코드 전면 재점검 | 카카오 비즈앱 심사 절차. 개인 학습 앱도 전환 자체는 가능하나 외부 절차 의존 |
| 주의 | 이 합성값이 **화면에 노출되면 안 됨** — 소비 지점 점검 (§7 #6: 헤더 드롭다운이 `principal.email` 노출 중!) | | |

**권장: A안** + §7 의 email 노출 지점에서 소셜 계정일 때 마스킹/대체 표기.

---

## 4. UserPrincipal ↔ OAuth2User 통합 전략 (principal 타입 일원화)

**문제**: 기본 `oauth2Login()` 은 principal 을 `DefaultOAuth2User` 로 만든다. 이러면:
- `@AuthenticationPrincipal UserDetails principal` 주입부 (ApplicationController L40 등 **13개 컨트롤러**) → **null 주입** (타입 불일치)
- `${#authentication.principal.displayName}` (header.html L86·93·108·111) → 프로퍼티 없음 → 렌더 예외

**전략 (권장): `UserPrincipal` 이 `UserDetails` + `OAuth2User` 를 동시 구현** — principal 타입을 로그인 경로와 무관하게 단일화.

```java
public class UserPrincipal implements UserDetails, OAuth2User {
  // 기존 필드 유지: id, email, displayName, password, authorities   (UserPrincipal.java:13~17)
  private final Map<String, Object> attributes;   // 폼 로그인 = Map.of(), 소셜 = 카카오 응답
  private final boolean newUser;                   // 최초 소셜 가입 여부 → successHandler 가 /welcome 분기 (Q6)

  public UserPrincipal(User user) { this(user, Map.of(), false); }              // 기존 생성자 시그니처 보존 (13개 호출부 무수정)
  public UserPrincipal(User user, Map<String, Object> attributes, boolean newUser) { ... }

  @Override public Map<String, Object> getAttributes() { return attributes; }
  @Override public String getName() { return email; }   // OAuth2User(AuthenticatedPrincipal) 계약
}
```

- **`displayName` 템플릿 호환**: 카카오 사용자도 `user.name`(=닉네임) 기반 `displayName` 을 가지므로 header.html 4개 지점 **무수정 동작**.
- `CustomOAuth2UserService extends DefaultOAuth2UserService` 가 `loadUser()` 에서: ⑥ 응답 수신 → `KakaoUserInfo`(record) 로 중첩 파싱 → `findByProviderAndProviderId` 조회 → 없으면 User 신규 저장 (§3 정책) → `new UserPrincipal(user, attributes, isNew)` 반환.
- `OAuth2LoginSuccessHandler` (SimpleUrlAuthenticationSuccessHandler 확장): `principal.isNewUser()` → `/welcome`, 아니면 `/`.
- 대안 (불채택): 별도 `OAuth2UserPrincipal` 클래스 — 컨트롤러 13곳이 `UserDetails`/`UserPrincipal` 혼용 주입 중이라 타입 분기가 전 레이어로 번짐.

**SecurityConfig 변경** (`SecurityConfig.java:112~118` formLogin 아래에 병렬 추가):

```java
.oauth2Login(oauth -> oauth
    .loginPage("/login")                                    // 미인증 진입 시 커스텀 로그인 화면 유지
    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
    .successHandler(oAuth2LoginSuccessHandler)
    .failureUrl("/login?error"))                            // 기존 alert 영역 재사용
```

- `/oauth2/authorization/kakao`, `/login/oauth2/code/kakao` 는 oauth2Login DSL 이 자체 permit 처리 — authorizeHttpRequests 매처 추가 불필요 (검증 시나리오에 포함해 실측 확인).
- remember-me 는 소셜 경로에 적용 안 됨 (파라미터 부재) — 명세상 한계로 확정, 후속 없음.

---

## 5. 로그인 화면 카카오 버튼 UI (Q1)

prototype 에 소셜 버튼이 없으므로 (§1-A) **카카오 공식 디자인 가이드** 기준 제안:

| 속성 | 값 | 근거 |
|---|---|---|
| 배경 | `#FEE500` (카카오 옐로) | 카카오 로그인 버튼 가이드 고정값 — 디자인 토큰 예외 (브랜드 강제색). main.css 에 `--color-kakao: #FEE500` 주석과 함께 추가 |
| 라벨 | `카카오로 시작하기` | 로그인+가입 겸용 진입점이므로 "로그인" 대신 "시작하기" |
| 라벨 색 | `rgba(0,0,0,0.85)` | 가이드 (블랙 85%) |
| 심볼 | 카카오 말풍선 심볼 **인라인 SVG** | CLAUDE.md "이모지 대체 금지 / prototype SVG 규칙" 준용. `sns_kakaotalk.png`(푸터용) 유용 금지 |
| 크기 | height 46 / radius 8 / full-width | 기존 `.btn-auth` (로그인·회원가입 버튼) 와 동일 규격 — 화면 리듬 유지 |
| 위치 | 회원가입 버튼 아래, 구분선(`─ 또는 ─`, textTri 색) 다음 | 기존 prototype 레이아웃 (tsx L1891~1894) 을 침범하지 않는 최소 추가 |
| 마크업 | `<a th:href="@{/oauth2/authorization/kakao}" class="btn-auth btn-auth--kakao">` | GET 링크 — 폼/CSRF 불필요 |
| 조건부 렌더 | `th:if="${kakaoLoginEnabled}"` | §2 A안 — 키 미설정 환경에서 숨김 (map-app-key 패턴) |

## 6. redirect-uri 이원화 (로컬/Fly.io) + 시크릿 관리

| 환경 | redirect-uri 실값 | 등록 위치 |
|---|---|---|
| 로컬 dev | `http://localhost:8080/login/oauth2/code/kakao` | 카카오 콘솔 Redirect URI (복수 등록 가능) |
| e2e (8090) | `http://localhost:8090/login/oauth2/code/kakao` | 실 로그인 E2E 미수행이므로 등록 불필요 (필요 시 추가) |
| 운영 (Fly.io) | `https://youth-moa-java.fly.dev/login/oauth2/code/kakao` | 카카오 콘솔 + `force_https=true`(fly.toml L25) 라 https 고정 |

- yml 은 `redirect-uri: "{baseUrl}/login/oauth2/code/kakao"` **템플릿 변수 1줄로 양 환경 커버** — 코드/프로파일 분기 불필요. Fly 는 프록시 뒤이므로 `{baseUrl}` 이 https 로 조립되도록 `server.forward-headers-strategy: framework` 설정 추가 (Fly 가 `X-Forwarded-Proto` 를 넣어줌 — 누락 시 redirect_uri mismatch(KOE006) 발생. **동적 검증 항목**).
- **시크릿**: `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` — 로컬은 환경변수/IntelliJ 실행 구성, 운영은 `fly secrets set KAKAO_CLIENT_ID=... KAKAO_CLIENT_SECRET=...`. yml 에는 `${ENV:default}` 참조만 (기존 `REMEMBER_ME_KEY`·`COOLSMS_API_KEY` 패턴 동일) → **gitleaks 통과** (리터럴 키 커밋 없음). REST API 키는 공개돼도 secret+redirect URI 화이트리스트가 방어하지만 관례상 둘 다 env 로.

## 7. 데이터 소비 지점 (신규 provider/providerId/null password 데이터가 닿는 모든 화면)

| # | 소비 지점 | 참조 | 현재 상태 | 갭/조치 |
|---|---|---|---|---|
| 1 | 헤더 아바타 이니셜 + `displayName님` | header.html L86·93·108·111 | `#authentication.principal.displayName` | §4 통합으로 **무수정 동작** — 카카오 닉네임 렌더 (write→read 검증 #V6) |
| 2 | login.html 버튼 영역 | tsx L1891~1894 | 소셜 버튼 없음 | 이번 티켓 신규 (Q1) |
| 3 | mypage 개인정보 수정 Step1 비밀번호 재확인 | `MyPageController.java:111~`, `UserService.verifyPassword` (L69~74) | `password=null` → `matches()` false → **소셜 사용자는 프로필 수정 영구 불가** | **Q7** — 최소조치(권장): 소셜 계정이면 Step1 스킵 + "카카오 로그인으로 본인확인됨" 안내 |
| 4 | 비밀번호 찾기 | `FindAccountService`, find-password.html | 소셜 계정 email(합성) 로는 도달 불가하나, 향후 실 email 수집 시 임시비번 발급이 무의미 | **Q8** — 이번 티켓: provider != LOCAL 이면 "카카오 간편가입 계정입니다" 안내 (1 분기) or 후속 이월 |
| 5 | 아이디 찾기 (이름+phone) | FindAccountController | 소셜 사용자 phone=null → 조회 불가 (자연 차단) | 조치 없음 — 정상 (안내 문구는 후속) |
| 6 | 헤더 드롭다운 email 표기 | header.html **L113** `principal.email` | **합성 email `kakao_...@social.youthmoa.invalid` 가 그대로 노출됨** | 이번 티켓 포함 — 소셜 계정은 "카카오 로그인" 뱃지/문구로 대체 (Q3 연동) |
| 7 | 신청 폼 회원정보 자동채움 (연락처·주소) | HANDOFF §5.4 L421, ApplicationController | phone/주소 null → 빈 채움 | 기존에도 nullable — 폼에서 직접 입력 (Q5 에서 정책 확認) |
| 8 | /welcome 온보딩 (관심 저장) | WelcomeController L37~68, tsx L1607 | signup 후 autoLogin → /welcome | successHandler 가 최초 소셜 로그인도 /welcome 라우팅 (**Q6**) — updateInterests 는 email 키 조회라 합성 email 로도 동작 |
| 9 | 회원가입 화면 | signup.html | 소셜과 무관 (LOCAL 전용) | 변경 없음 |

## 8. 변경 범위 (파일 단위)

- [ ] `build.gradle.kts` — `implementation("org.springframework.boot:spring-boot-starter-oauth2-client")` 추가 (Boot 4 모듈화로 아티팩트명 이동 가능성 → impl 시 4.1 dependency 목록 대조. 참고: 본 프로젝트는 web 도 `-starter-webmvc` 신명명 사용 중, L28)
- [ ] `src/main/resources/application.yml` — §2 registration/provider + `forward-headers-strategy` (§6)
- [ ] `common/config/SecurityConfig.java` — `.oauth2Login()` DSL (§4)
- [ ] `user/User.java` — `provider`(enum, NOT NULL default LOCAL, columnDefinition), `providerId` 필드 + builder 파라미터 + unique(provider, providerId)
- [ ] `user/AuthProvider.java` — **신규** enum (LOCAL, KAKAO)
- [ ] `user/UserRepository.java` — `findByProviderAndProviderId(AuthProvider, String)`
- [ ] `user/UserPrincipal.java` — OAuth2User 동시 구현 + attributes + newUser (§4)
- [ ] `user/oauth/CustomOAuth2UserService.java` — **신규** (파싱·조회·신규생성 — 등록 로직은 `processOAuth2User(KakaoUserInfo)` 로 분리해 단위 테스트 seam 확보)
- [ ] `user/oauth/KakaoUserInfo.java` — **신규** record (중첩 attributes 안전 파싱)
- [ ] `user/oauth/OAuth2LoginSuccessHandler.java` — **신규** (/welcome 분기, Q6)
- [ ] `templates/user/login.html` — 구분선 + 카카오 버튼 (Q1)
- [ ] `templates/fragments/header.html` — L113 email 표기 소셜 대체 (§7 #6)
- [ ] `static/css/main.css` — `.btn-auth--kakao`, `.auth-divider`
- [ ] (Q7 채택 시) `user/MyPageController.java` + 관련 템플릿 — Step1 스킵 분기
- [ ] (Q8 채택 시) `user/FindAccountService.java` — 소셜 계정 안내 분기
- [ ] 테스트: `JpaMappingTest`(provider round-trip), `CustomOAuth2UserServiceTest`(단위), `LoginRenderTest`(버튼 렌더), `SecurityMockMvc oauth2Login()` 통합 (§10)
- 변경 없음: fly.toml (secrets 는 CLI), Dockerfile, docs/specs/README.md (지시)

### PR 분할 (Q10)

**권장: 1 PR** — 엔티티 2컬럼(additive)·신규 클래스 3개·템플릿 1개로 응집도 높고, 쪼개면 중간 상태(버튼 없는 백엔드)가 검증 불가. SSR 단일 PR 패턴 정합.
대안 2 PR: ① 백엔드(deps~successHandler, 버튼 없이 URL 직접 검증) ② UI+소비지점(Q1·Q7·Q8) — Q7/Q8 을 후속 이월하면 자연히 이 형태가 됨.

## 9. 검증 시나리오 (ym-qa 실행 항목)

### 정적
- `compileJava` + `JpaMappingTest`(provider/providerId/unique 제약 round-trip) + `LoginRenderTest`(카카오 버튼 마크업 + `th:*` 잔존 없음 — 화면 변경이므로 RenderTest 필수 규칙)
- `CustomOAuth2UserServiceTest` (단위, HTTP 없음 — `processOAuth2User` seam):
  - 신규 providerId → User 생성 (provider=KAKAO, 합성 email, name=닉네임, password null, role USER)
  - 기존 providerId 재로그인 → **신규 생성 없음** (count 불변) + newUser=false
  - `properties.nickname` 만 있고 `kakao_account.profile` 없는 응답 → fallback 파싱
- `UserPrincipal` 단위: 폼 로그인 생성자 → attributes 빈 Map, `getName()==email`
- MockMvc + spring-security-test (기보유 의존성 L48): `mockMvc.perform(get("/mypage").with(oauth2Login().oauth2User(userPrincipal)))` → 200 — **소셜 principal 로 인증 필요 페이지 통과** (13개 주입부 호환 증명)
- gitleaks (lint.yml) — 리터럴 키 부재

### 동적 (bootrun-e2e, 8090 — 실 카카오 키 불필요)
- `GET /login` 200 + (키 미설정) 카카오 버튼 **미노출** / (가짜 키 env 주입 재기동) 버튼 노출 + `href="/oauth2/authorization/kakao"`
- `GET /oauth2/authorization/kakao` → **302 + Location 이 `kauth.kakao.com/oauth/authorize`** (client_id·redirect_uri·state 쿼리 포함 — 필터 체인 정상 증명, 카카오 실계정 불필요)
- 기존 폼 로그인 회귀: `POST /login` (시드 계정) → 302 `/` / 실패 시 `/login?error`
- `/login/oauth2/code/kakao` 임의 접근 → 로그인 리다이렉트 아닌 OAuth 에러 처리 (401/redirect `/login?error`) — 매처 누락 감지

### write→read 왕복 (통합 — H2)
- V6: `processOAuth2User` 최초 호출 → users row 생성 → **재조회 후** `loadUserByUsername(합성 email)` 로 UserPrincipal 획득 → `displayName == 카카오 닉네임` assert → 동일 attributes 재호출 → row count 불변
- (Q6 채택 시) 최초 소셜 로그인 successHandler → `/welcome` redirect → `POST /welcome` 관심 저장 → User.interestRegions 재조회 일치 (기존 F-signup-03 시나리오 재사용)

### E2E (Playwright) / 시각 — 실계정 로그인 불가 → 분리 전략
- **Playwright (자동)**: 버튼 노출·href·카카오 authorize 로 302 되는 네트워크 응답까지만 검증. **카카오 로그인 페이지 이후는 자동화 금지** (외부 실계정 + 카카오 봇 감지 — CAPTCHA 개입 가능 영역)
- **mock 대안 (선택)**: e2e 프로파일 한정 테스트 전용 로그인 엔드포인트는 **불채택** (보안 표면 증가) — MockMvc `oauth2Login()` 통합 테스트가 동일 커버리지 제공
- **수동 시각 확인 (사용자 영역)**: ① 로컬 실키로 [카카오로 시작하기] → 동의 → 헤더에 닉네임 렌더 ② 로그아웃 → 재로그인 → 같은 계정 (신규 생성 없음, mypage 확인) ③ 버튼 색·심볼이 카카오 가이드와 일치 ④ (배포 후) fly.dev 에서 동일 왕복 — KOE006(redirect mismatch) 없는지

## 10. 의존성 / 선행 작업
- 선행 없음 (main 기준 additive). 카카오 개발자 콘솔 앱 생성 + Redirect URI 등록 + [보안] client_secret 활성화는 **사용자 수행 항목** (Claude 는 계정/콘솔 조작 불가)
- Flyway (P0-1) 활성화 전이므로 ddl-auto update 로 컬럼 자동 추가 — baseline dump 이전에 머지되면 V1 에 자연 포함
- admin 트랙과 무관. F-signup 시리즈와 파일 겹침: `User.java`, `login.html` — 동시 작업 브랜치 없는지 착수 시 확인

## 11. 작업 큐 메타
- 작업 ID: feature-oauth2-kakao / 우선순위: 포트폴리오 트랙 / 추정: **1 PR** (Q7·Q8 이월 시 본체 축소) / 상태: **spec_done**

---

## 12. 사용자 결정 필요 질문 (Q 리스트)

- **Q1. 카카오 버튼 UI 승인** — prototype 에 소셜 버튼 없음 (tsx L1863~1900 확인). §5 제안 (회원가입 버튼 아래 + "또는" 구분선 + #FEE500 full-width + 인라인 SVG 심볼 + 키 미설정 시 숨김) 대로 진행?
- **Q2. 계정 식별 모델** — **A안 (권장): User.provider+providerId 확장** / B안: SocialAccount 별도 엔티티 (다중 provider·계정연동 대비)
- **Q3. email 처리** — **A안 (권장): 합성 email `kakao_{id}@social.youthmoa.invalid`** + 헤더 노출 지점 대체 표기 / B안: email nullable 마이그레이션 / C안: 비즈앱 전환해 실 email 수집
- **Q4. 기존 email 가입자와의 연동** — 현 scope 로는 대조 불가 → **이번 티켓 "항상 신규 계정" (권장)**. email scope 확보 시 자동연동은 is_email_verified 필수 조건부로 후속 논의 — 동의?
- **Q5. 소셜 가입자 phone 인증** — **생략 (권장)**: phone=null, phoneVerified=false 유지, 신청 폼에서 직접 입력. 대안: 최초 로그인 후 인증 유도 화면 (후속 티켓)
- **Q6. WelcomeScreen 온보딩 연결** — **최초 카카오 로그인 시 /welcome 리다이렉트 (권장)** — 기존 signup 플로우와 동일 경험. 재로그인은 `/`
- **Q7. mypage 비밀번호 재확인 게이트** — 소셜 계정은 password null 로 **프로필 수정 영구 불가** 상태가 됨. A안 (권장): 이번 티켓에서 소셜 계정 Step1 스킵 + 안내 문구 / B안: 후속 티켓 이월 (그동안 소셜 사용자는 프로필 수정 불가 상태 감수)
- **Q8. 비밀번호 찾기 가드** — A안: 이번 티켓에서 provider!=LOCAL 안내 분기 (1분기 소규모) / B안 (권장): 후속 이월 — 합성 email 을 아는 사용자가 없어 실도달 불가, 위험 낮음
- **Q9. 미설정 환경 기동 전략** — **A안 (권장): sentinel default + 버튼 조건부 렌더** (map-app-key 패턴) / B안: 조건부 Java config
- **Q10. PR 분할** — **1 PR (권장)** / 2 PR (백엔드 → UI+소비지점)
