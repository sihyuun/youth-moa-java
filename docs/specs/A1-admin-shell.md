# 작업 명세: A1 — admin-shell (관리자 로그인 · 대시보드 · 헤더)

| 메타 | 값 |
|---|---|
| 상태 | `spec_confirmed` (2026-09-03 사용자 결정 — Qn-1 A · Qn-2 A · Qn-3 B · Qn-4 B · Qn-5 A · Qn-6 B, 모두 §7 권장안. Qn-5 는 상세 확인 후 재확정) |
| 브랜치 | `feature/A1-admin-shell` (Q6 확정) |
| 선행 | P0-1 Flyway (PR #109 완료) · P0-2 매처·CSRF (PR #89 완료) · **P0-2 이월분 `/admin/login` formLogin 은 본 PR 에 흡수** (Q3 확정) |
| 관련 문서 | ADR: `docs/adr/admin-track-roadmap-2026-09.md` · 마스터: `docs/specs/ADMIN-00-master-directive.md` §5-A1 |

## 0. 배경 · 스코프

관리자 트랙(A1~A9) 의 파일럿. **A1 은 뼈대만 세운다** — 실 CRUD 는 A2~A9 각 Phase 담당.

### 포함 (A1)

1. **`/admin/login`** — formLogin 재설정. 밝은 헤더 (Auth Header) + 420px 중앙 카드 (prototype.html L60~96)
2. **`/admin`** — 관리자 대시보드 shell. 다크 헤더 (`#111827`, 56px sticky) + 스탯 카드 4개 + 최근 프로그램 + 승인 대기 카드 + 마감 임박 프로그램 (prototype.html L602~788)
3. **admin 헤더 fragment** — `templates/admin/fragments/header.html`. 로고+ADMIN 뱃지 / 센터 스코프 셀렉터 (placeholder, A7 에서 실동작) / GNB (통계·프로그램 관리·사용자 관리 — link href 만, 목적지 미구현이면 `#` + disabled 스타일) / 우측 검색·알림·유저 드롭다운 (A7 에서 실동작. A1 은 자리 + 로그아웃 항목만 실동작)
4. **admin.css** — `templates/admin/*` 전용 CSS. main.css 오염 금지 (POLICY 별도)
5. **시드 계정 확인** — `DataInitializer.seedAdmins()` 이미 존재 (L130~170). 본 PR 은 검증만
6. **사용자 ↔ 관리자 왕복 링크** — ADMIN-00 §8-1 결정
   - 사용자 헤더 드롭다운 → `sec:authorize` 조건부 "관리자 페이지" 링크 노출
   - admin 헤더 유저 드롭다운 → "사용자 페이지" 링크 노출

### 제외 (후속 A2~A9)

- 프로그램 목록·상세·등록 (A2~A4)
- 사용자 관리 (A5)
- 통계 화면 (A6)
- 알림 벨 실동작·글로벌 검색·설정·마이페이지 (A7)
- 카드/캘린더 뷰·반응형 정규화·스켈레톤 (A8)
- 센터 CRUD (A9)
- 파일 업로드 인프라 (P0-3 — A3 직전)

### 스탯 카드 데이터 소스 (A1 범위 내 실동작)

prototype 스탯 카드 4개는 실제 count 를 렌더한다. A1 은 조회 로직만 담당.

| 카드 | 데이터 소스 | 센터 격리 |
|---|---|---|
| 진행중 프로그램 | `ProgramRepository.count(status=OPEN)` (파생 status 는 java 필터) | SYSTEM_ADMIN=전체 / CENTER_ADMIN=자기 센터만 |
| 마감 프로그램 | 동 (status=CLOSED) | 동 |
| 진행 예정 | 동 (status=UPCOMING) | 동 |
| 전체 회원 | `UserRepository.count(role=USER)` | SYSTEM_ADMIN=전체 / CENTER_ADMIN=자기 센터 USER 만 (단순 count) |
| 승인 대기 (우측 카드) | `ApplicationRepository.countByStatus(PENDING)` | 소속 센터 프로그램의 신청 건만 |

"지난달보다 증가" 등 비교 문구는 prototype 이 하드코딩 — **A1 은 카피 그대로 렌더** (실 비교 계산은 A6 통계). 계약에 `deferred` 로 기록.

### 최근 프로그램 / 마감 임박 프로그램

- 최근 프로그램: `ProgramRepository.findTop5ByOrderByCreatedAtDesc()` (센터 격리 적용)
- 마감 임박: `applyEndDate` 컬럼이 A3 에 도입될 예정 — **A1 은 `endDate` 로 임시 파생** + `deferred: A3` 기록. 5행 렌더

## 1. 디자인 출처 (3자산)

- **prototype.html** — L60~96 (Auth Header + Login) · L602~788 (Dashboard)
- **prototype.tsx** — L407 (`LoginScreen`) · L464~465 (스텁 컴포넌트) · L458 (`AdminHeader` 스텁). tsx 는 스캐폴드만 있어 실제 마크업은 prototype.html 이 유일한 근거
- **HANDOFF.md** — L27~46 (전역 레이아웃) · L64 (dashboard) · L83~156 (디자인 토큰) · L206~216 (헤더) · L191~197 (반응형 — A8 이월) · L199~200 (마이크로카피 톤 "…했어요/됐어요")

## 1-A. 자산 간 갭 (3자산)

prototype.tsx L464 는 `function LoginScreen(_) { return <div />; }` 로 **스캐폴드만**이라 prototype.html vs tsx 대조는 실행 불가. wireframe.png 는 admin 최신본에서 이미 hifi 로 갈음됨 (ADMIN-00 §1-A). 갭 표는 생략.

| 항목 | prototype.html | prototype.tsx | HANDOFF | 채택 |
|---|---|---|---|---|
| Auth Header 높이 | 64px (L61) | 스텁 | 명시 없음 | 64px |
| 인증 카드 max-width | 420px (L69) | 스텁 | 420px (L45) | 420px |
| 로그인 h1 크기 | 26px/700 (L70) | 스텁 | 명시 없음 (본문 h2 는 18/700) | 26/700 |
| 인풋 padding | 11px 14px (L76) | 스텁 | inputs 8px radius (L153) | 11/14 · radius 8 |
| 로그인 버튼 | primary #3F30E9, padding 13px, radius 10 (L86) | 스텁 | 명시 없음 | 그대로 |
| 다크 헤더 배경 | 명시 없음 (L602~ 는 GNB 내부만) | 스텁 | `#111827` 56px sticky (L28~44) | HANDOFF |
| ADMIN 뱃지 | 명시 없음 | 스텁 | 10px, `#1E293B` bg, `#334155` border, `#A6A3B3` text (L209) | HANDOFF |
| 스탯 카드 크기 (숫자) | 32px/700 Inter (L623) | 스텁 | 22px+ / 700 (L150) | prototype 32px 채택 (구체값 우선) |
| 대시보드 그리드 | `1fr 268px` (L678) | 스텁 | 명시 없음 | prototype 채택 |
| 반가워요 텍스트 | "반가워요, 박시현님 👋" (L608) | 스텁 | 명시 없음 | prototype (실제 로그인 사용자명 주입) |

## 1-B. 데이터 모델 gap 표

A1 은 **엔티티 신설 없음**. 기존 필드만 사용.

| prototype 화면 요소 | 참조 필드 | 현재 엔티티 | 조치 |
|---|---|---|---|
| 대시보드 스탯 count | `Program.status` (파생) | ✅ 존재 | 없음 |
| 최근 프로그램 | `Program.createdAt`, `title`, `center.name`, count(applications) | ✅ 존재 | 없음 |
| 마감 임박 D-day | `Program.applyEndDate` | ❌ **A3 도입 예정** | **`deferred: A3`** — A1 은 `endDate` 임시 파생 |
| 승인 대기 count | `Application.status = PENDING` | ✅ 존재 | 없음 |
| 관리자 이름 표시 | `User.name` | ✅ 존재 | 없음 |
| 센터 스코프 셀렉터 | `User.center`, `User.centerScope` | ✅ 존재 | A7 에서 실동작. A1 은 CENTER_ADMIN 은 `.center.name` 고정 표기, SYSTEM_ADMIN 은 "전체" 표기만 |
| 알림 벨 미읽음 개수 | `Notification.isRead` | ✅ 존재 | A7 에서 드롭다운 실동작. A1 은 count 만 렌더 |

## 1-C. 데이터 소비 지점

| 소비 지점 | 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/admin/login` 폼 | 신규 | 없음 | 신설 |
| `/admin` 대시보드 스탯 | 신규 | 없음 | 신설 |
| 사용자 헤더 드롭다운 | `templates/fragments/header.html` L49~ | admin 링크 없음 | **"관리자 페이지" 항목 추가** (`sec:authorize="hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')"`) |
| admin 유저 드롭다운 | 신규 | 없음 | 신설 시 "사용자 페이지" 링크 포함 |

## 2. 변경 범위 (파일 단위)

### 신규 파일

- [ ] `src/main/java/.../admin/AdminLoginController.java` — `GET /admin/login` 만 담당. POST 는 Spring Security formLogin 이 처리
- [ ] `src/main/java/.../admin/AdminDashboardController.java` — `GET /admin` → `admin/dashboard`
- [ ] `src/main/java/.../admin/AdminDashboardService.java` — 스탯 count · 최근 프로그램 · 마감 임박 · 승인 대기 조회 (센터 격리 반영)
- [ ] `src/main/java/.../admin/AdminScope.java` (또는 유틸) — `SecurityContext` → `effectiveCenter` 파생. `SYSTEM_ADMIN → null (전체)` / `CENTER_ADMIN → user.center`
- [ ] `src/main/resources/templates/admin/login.html`
- [ ] `src/main/resources/templates/admin/dashboard.html`
- [ ] `src/main/resources/templates/admin/fragments/auth-header.html` — Auth Header 64px
- [ ] `src/main/resources/templates/admin/fragments/header.html` — 다크 헤더 56px + GNB + 유저 드롭다운
- [ ] `src/main/resources/templates/admin/fragments/footer.html` — 콘텐츠와 함께 스크롤 (HANDOFF L262)
- [ ] `src/main/resources/static/css/admin.css` — 인디고 토큰 + 다크 헤더 + 대시보드 스탯 카드 + Auth 카드
- [ ] `src/test/java/.../admin/AdminLoginRenderTest.java`
- [ ] `src/test/java/.../admin/AdminDashboardRenderTest.java`
- [ ] `src/test/java/.../admin/AdminSecurityTest.java` — RBAC 슬라이스
- [ ] `docs/design-contracts/admin/README.md`
- [ ] `docs/design-contracts/admin/POLICY.md` — admin 공통 정책 (인디고 다크 헤더·존댓말 톤·`logo_white.png`)
- [ ] `docs/design-contracts/admin/shell.md` — 서술 계약
- [ ] `docs/design-contracts/admin/dashboard.md` — 서술 계약
- [ ] `docs/design-contracts/admin/login.md` — 서술 계약
- [ ] `e2e/contracts/admin-shell.ts` — 헤더·푸터·GNB·유저 드롭다운 기계 계약 (~30 checks)
- [ ] `e2e/contracts/admin-dashboard.ts` — 스탯 카드 4개·최근 프로그램·마감 임박 (~40 checks)
- [ ] `e2e/contracts/admin-login.ts` — Auth Header · 카드 · 인풋 · 버튼 (~20 checks)
- [ ] `e2e/tests/visual-admin-shell.spec.ts` · `visual-admin-dashboard.spec.ts` · `visual-admin-login.spec.ts`
- [ ] `e2e/tests/admin-login.spec.ts` — 기능 E2E
- [ ] `e2e/tests/admin-dashboard.spec.ts` — 기능 E2E (왕복 링크 포함)

### 수정 파일

- [ ] `src/main/java/.../common/config/SecurityConfig.java` — `/admin/login` formLogin 재설정 (§4 상세)
- [ ] `src/main/resources/templates/fragments/header.html` — 사용자 헤더 드롭다운에 `sec:authorize` 조건부 "관리자 페이지" 링크 삽입
- [ ] `src/main/resources/application.yml` — `admin.seed.password.*` 기본값 재확인 (env override 가능하도록 이미 구성됨. 문서화만)
- [ ] `docs/design-contracts/README.md` — admin 계약 3화면 표에 추가
- [ ] `docs/STATE.md` — A1 착수 · 다음 큐 갱신

## 3. SecurityConfig 변경

### 3-1. 현재 상태 (SecurityConfig.java L131~137)

```java
.formLogin(form -> form.loginPage("/login")
    .loginProcessingUrl("/login")
    .defaultSuccessUrl("/", true)
    .failureHandler(loginFailureHandler())
    .permitAll())
```

사용자 formLogin **1개만** 등록. `/admin/login` GET 은 permitAll 매처(L83)에 등록됐지만 POST 처리는 없음.

### 3-2. 결정 필요 — Spring Security 로그인 페이지 2개 공존 전략

Spring Security 는 `formLogin()` 을 **다중 등록**할 수 없다 (마지막 것만 유효). 두 로그인 페이지를 지원하는 표준 방법은 3가지.

| 방식 | 설명 | 평가 |
|---|---|---|
| **A. 통합 formLogin + adminAware SuccessHandler** | 단일 `/login` 을 유지하되, 로그인 성공 후 principal.roles 를 보고 admin 이면 `/admin` 으로, USER 면 `/` 로 리다이렉트 | admin 이 사용자 로그인 페이지를 통과 → 카피/디자인 관점에서 부적절 (prototype "관리자 로그인" h1 별도) |
| **B. 별도 SecurityFilterChain 2개 (`@Order`)** | `/admin/**` 만 매칭하는 별도 `SecurityFilterChain` 을 `@Order(1)` 로 등록, 사용자 chain 은 `@Order(2)` | Spring Security 7 권장. 완전 분리. 관리자 chain 은 `securityMatcher("/admin/**")` + 자체 formLogin(`/admin/login`) |
| **C. 통합 formLogin + AuthenticationEntryPoint 커스텀** | 401 → URL 이 `/admin/**` 이면 `/admin/login` 리다이렉트, 그 외 `/login`. 실제 POST 는 두 URL 모두 같은 프로세싱 URL 하나로 | 로그인 처리 URL 이 하나라 form action 을 URL 로 분기하는 로직 필요 |

**본 스펙 채택: B (별도 SecurityFilterChain 2개)** — 근거:
- 관리자 트랙과 사용자 트랙의 인증 UX 완전 분리 가능 (successUrl·failureHandler·rememberMe 정책 개별)
- CSRF · session policy 를 admin 에만 강화하기 쉬움 (향후 A7 에서)
- 사용자 formLogin 변경 시 admin 회귀 리스크 0

**Qn-1 로 사용자에게 확인 필요** (아래 §7 참조).

### 3-3. B 안 상세

```java
@Bean
@Order(1)
public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, ...) {
    http.securityMatcher("/admin/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/login").permitAll()
            .anyRequest().hasAnyRole("CENTER_ADMIN", "SYSTEM_ADMIN"))
        .formLogin(form -> form
            .loginPage("/admin/login")
            .loginProcessingUrl("/admin/login")
            .defaultSuccessUrl("/admin", true)
            .failureUrl("/admin/login?error")
            .failureHandler(adminLoginFailureHandler())
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/admin/logout")
            .logoutSuccessUrl("/admin/login?logout")  // Qn-2 결정 대기
            .deleteCookies("JSESSIONID"))
        .rememberMe(...)  // Qn-3 결정 대기
        .csrf(csrf -> { /* 기본 활성 */ });
    return http.build();
}

@Bean
@Order(2)
public SecurityFilterChain userSecurityFilterChain(HttpSecurity http, ...) {
    // 기존 코드 그대로. 단 authorizeHttpRequests 에서 "/admin/**" hasAnyRole 매처는 제거
    // (order 1 chain 이 이미 담당)
}
```

**주의사항**:
- Spring Security 7 은 `securityMatcher("/admin/**")` 를 chain 매칭 조건으로 씀. `/admin/**` 이외 요청은 이 chain 을 스킵하고 order 2 로 넘어감
- 기존 P0-2 매처(L87~88) 의 `/admin/**` hasAnyRole 은 order 2 사용자 chain 에서 **제거** (order 1 이 담당). 이월 매처(L83 permitAll `/admin/login`) 도 제거
- `e2eProfile` 분기의 `/__test__/**` permit 은 order 2 chain 유지 (admin 은 접근 불가)
- CSRF 두 chain 모두 활성. e2e `/__test__/**` ignore 도 order 2 chain 에만

### 3-4. adminLoginFailureHandler

사용자 `loginFailureHandler()` 와 동일 패턴 재사용 — 실패 시 `savedUsername` 세션 보존.

## 4. 시드 계정 (환경변수 규약)

**기존 구현 재확인** (`DataInitializer.java` L52~62, L130~170).

| 계정 email | 역할 | env override | 기본값 |
|---|---|---|---|
| `sysadmin@youth-moa.test` | SYSTEM_ADMIN | `ADMIN_SEED_PASSWORD_SYSTEM` | `Admin!234` |
| `center1@youth-moa.test` | CENTER_ADMIN (centers[0]) | `ADMIN_SEED_PASSWORD_CENTER1` | `Admin!234` |
| `center2@youth-moa.test` | CENTER_ADMIN (centers[1]) | `ADMIN_SEED_PASSWORD_CENTER2` | `Admin!234` |

**A1 액션**: DataInitializer 는 이미 완비. 본 PR 은 **문서화만** 담당 (`docs/design-contracts/admin/POLICY.md` 에 시드 계정 목록 명시 + prod env 주입 지침).

**요청안 (사용자 명세와 차이)**: 사용자는 `ADMIN_SEED_SYSTEM_EMAIL` 등 email 도 env 로 하자고 제안했으나, **email 은 고정** 이 낫다 (E2E 시드 계정으로 spec 에 하드코딩된 email 이 여러 곳에서 인용될 것). 비밀번호만 env 화 유지. **Qn-4 로 확인**.

## 5. 화면 · 라우팅

### 5-1. `/admin/login` (Auth 레이아웃)

**렌더 요소** (prototype.html L60~96 기준):
- Auth Header (64px, white bg, border-bottom #E3E1E8, padding 0 40px)
  - 로고 `logo_primary.png` height 32px + ADMIN 뱃지 (10px, `#3F30E9` bg, white text)
- 본문 (`flex:1`, center align, padding 40px 16px)
  - Container 420px
    - h1 "관리자 로그인" (26/700 `#2B2A3D`, center, mb 8)
    - p "청년모아 관리자 페이지에 오신 것을 환영합니다" (13/regular `#A6A3B3`, mb 32)
    - Card (white, radius 16, padding 32, shadow `0 1px 4px rgba(0,0,0,0.06)`, border 1px `#F0EFF3`)
      - 아이디 label + input (padding 11/14, border 1px `#E3E1E8`, radius 8)
      - 비밀번호 label + input
      - 에러 alert (`#FEF2F2` bg, `#FEE2E2` border, `#DC2626` text) — `?error` 파라미터로 노출
      - 로그인 버튼 (primary `#3F30E9`, padding 13, radius 10, 15/700, mb 10)
      - 회원가입 버튼 → **prototype 은 있으나 A1 은 제거** (Q8: signup 미구현). **prototype 이탈 → `deviation` 기록**
    - 하단 링크: "아이디 찾기 | 비밀번호 찾기" (12/regular `#A6A3B3`) — 사용자 페이지 `/find-id` · `/find-password` 로 이동 (ADMIN-00 Q8)

**form 속성**:
```html
<form th:action="@{/admin/login}" method="post" novalidate>
    <input name="username" th:value="${savedUsername}" />
    <input name="password" type="password" />
    <button type="submit">로그인</button>
</form>
```
- CSRF: Spring Security 가 자동 hidden 삽입
- 클라 검증: 사용자 login.html 패턴 재사용 (novalidate + JS 인라인 에러)

### 5-2. `/admin` 대시보드

**렌더 요소** (prototype.html L602~788):
- 다크 헤더 (`admin/fragments/header.html`, 아래 5-3)
- MAIN (`#FAFAFB` bg, padding 24/28)
  - Welcome section (mb 22)
    - h2 "반가워요, [사용자명]님 👋" (21/700 `#2B2A3D`)
    - p "오늘도 청년모아 프로그램 운영을 함께합니다." (13/regular `#6E6B82`)
  - Stat cards (grid `repeat(4,1fr)`, gap 14, mb 22)
    - 진행중 프로그램 (green icon `#10B981` on `#D1FAE5`)
    - 마감 프로그램 (gray icon)
    - 진행 예정 (orange icon `#EA580C` on `#FFF7ED`)
    - 전체 회원 (purple icon `#7C3AED` on `#EDE9FE`)
    - 각 카드: 라벨 + 아이콘 + 큰 수치 32/700 Inter + 증감 문구 (하드코딩 유지, deferred: A6)
  - Body row (grid `1fr 268px`, gap 16)
    - Recent programs table (프로그램명·청년센터·신청현황·상태, 5행)
    - Right column
      - Quick links 카드 (프로그램 등록·통계 보기·사용자 관리)
      - Pending approval 카드 (승인 대기 count + "바로 처리하기 →")
  - 마감 임박 프로그램 (mt 16, D-7 이내 배지, 4행 테이블)

**CENTER_ADMIN 격리**: AdminDashboardService 조회 시 `AdminScope.effectiveCenter()` 로 필터. 격리 회귀 검증은 §8-3.

### 5-3. admin 다크 헤더 (`admin/fragments/header.html`)

- 56px sticky, `#111827` bg, z-index 500
- 좌 (`flex:1` justify-start): `logo_white.png` height 34 + ADMIN 뱃지 (10px `#1E293B` bg, `#334155` border 1px, `#A6A3B3` text)
- 좌측 이어서: 센터 스코프 셀렉터 (🏢 SVG + 센터명 + ▾)
  - CENTER_ADMIN: `.center.name` 텍스트 고정 (disabled)
  - SYSTEM_ADMIN: "전체" 텍스트 (A7 에서 드롭다운 실동작)
- 중앙 절대 정렬 (`position:absolute; left:50%; transform:translateX(-50%)`): GNB
  - 통계 → `#` (A6 미구현) + `.nav-link--disabled`
  - 프로그램 관리 → `#` (A2 미구현)
  - 사용자 관리 → `#` (A5 미구현)
  - 활성 표시: 현재 화면이 dashboard 라 3개 모두 비활성
- 우 (`flex:1` justify-end): 검색 아이콘 · 알림 벨 (미읽음 count 뱃지 표시만) · 유저 아바타 드롭다운
  - 유저 드롭다운 메뉴 항목: 내 정보 (`#` A7) · 설정 (`#` A7) · **사용자 페이지 → `/`** · 로그아웃 (`POST /admin/logout` — CSRF 포함 form 제출)

**"권한 전환" 버튼**: **미구현** (HANDOFF: 데모용 → 프로덕션 제거).

### 5-4. 사용자 헤더 → 관리자 페이지 링크 (기존 파일 수정)

`templates/fragments/header.html` L49~ 의 인증 사용자 드롭다운(존재) 에 아래 항목 추가:

```html
<a sec:authorize="hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')"
   th:href="@{/admin}" class="header-dropdown-item">관리자 페이지</a>
```

위치: 드롭다운 첫 항목 (마이페이지 위). USER 계정은 `sec:authorize` 로 완전 미노출.

## 6. 필드 / 컴포넌트 명세

| 요소 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| 아이디 (username) | email input | ✓ | JS: 비어 있으면 "아이디를 입력해주세요." | prototype 은 `type="text"` 이나 실 계정 email 이므로 `type="email"` |
| 비밀번호 | password input | ✓ | JS: 비어 있으면 "비밀번호를 입력해주세요." | eye toggle 미포함 (Auth 카드는 단순 유지) |
| 로그인 버튼 | submit | — | — | primary |
| 아이디 찾기 링크 | anchor | — | — | `/find-id` (사용자 페이지 재사용) |
| 비밀번호 찾기 링크 | anchor | — | — | `/find-password` |

## 7. 결정 필요 사항 (Qn-1~Qn-6)

### Qn-1. SecurityConfig 재설정 방식

| 안 | 설명 |
|---|---|
| **A (권장)** | **§3-2 B 안** — 별도 SecurityFilterChain 2개 (`@Order(1)` admin, `@Order(2)` user) |
| B | 통합 formLogin + adminAware SuccessHandler (§3-2 A 안) |
| C | 통합 formLogin + AuthenticationEntryPoint 커스텀 (§3-2 C 안) |

### Qn-2. 로그아웃 후 리다이렉트

| 안 | 설명 |
|---|---|
| **A (권장)** | `/admin/login?logout` — admin 세션 종료 후 admin 로그인 화면. "관리자 페이지에서 로그아웃했다" 는 컨텍스트 유지 |
| B | `/` — 사용자 홈. 완전 로그아웃 후 브랜드 홈 |
| C | 사용자 헤더 드롭다운 로그아웃과 통일 (기존 `/login?logout` 이지만 admin 은 별개니 A 권장) |

### Qn-3. remember-me admin 적용

| 안 | 설명 |
|---|---|
| A | admin 도 사용자와 동일한 `remember-me` (14일, PersistentToken) 적용 |
| **B (권장)** | admin 은 remember-me **미적용** — 세션 종료 시 재로그인 필요. 관리자 권한은 세션 유출 위험이 크므로 편의보다 안전 우선 |
| C | admin 만 짧은 유효기간 (예: 1일) |

### Qn-4. 시드 계정 email 을 env override 가능하게 할지

| 안 | 설명 |
|---|---|
| A | email 도 env 화 (`ADMIN_SEED_SYSTEM_EMAIL` 등). 사용자 명세 원안 |
| **B (권장)** | email 은 고정 (`sysadmin@youth-moa.test` 등), **비밀번호만 env**. 이유: E2E · Playwright 스크립트가 email 을 인용할 것. env 로 바뀌면 테스트 파괴. 기존 DataInitializer 도 이 방식 |

### Qn-5. SYSTEM_ADMIN 과 CENTER_ADMIN 대시보드 초기 화면 동일 여부

| 안 | 설명 |
|---|---|
| **A (권장)** | 동일 화면. 다만 스탯 count 만 격리 반영 (SYSTEM=전체, CENTER=자기 센터). 초기에는 화면 분기가 사고 원인이 되므로 스코프 로직만 다르게 |
| B | SYSTEM 은 "전체 센터 요약" 카드 별도, CENTER 는 "자기 센터 상세" 카드 별도. A9 센터 CRUD 완료 후 A6 통계에서 결정 |

### Qn-6. admin 헤더 GNB placeholder 처리

목적지 미구현 화면(통계·프로그램·사용자) 의 GNB 링크 처리.

| 안 | 설명 |
|---|---|
| A | `#` 링크 + `.nav-link--disabled` 스타일 (회색·클릭 시 아무 일 없음) |
| **B (권장)** | 아예 렌더하지 않음. A2 완료 시 프로그램 관리 등장, A5 시 사용자 관리 등장. **prototype 은 3개 항목 다 있어 이탈** → `deviation: 'A2/A5/A6 미구현 - 순차 도입'` 기록 |
| C | 링크는 렌더하되 클릭 시 toast "곧 제공됩니다" 표시 |

## 8. 검증 시나리오

### 8-1. 정적 검증

- `./gradlew compileJava`
- `./gradlew test --tests JpaMappingTest` (엔티티 변경 없으니 회귀만)
- `./gradlew test --tests AdminLoginRenderTest` — Thymeleaf 실 렌더 (F0h-c2 사고 재발 방지)
- `./gradlew test --tests AdminDashboardRenderTest` — `@WithMockUser(roles={"SYSTEM_ADMIN"})` · `@WithMockUser(roles={"CENTER_ADMIN"})` 두 조건
- `./gradlew test --tests AdminSecurityTest` — RBAC 슬라이스
  - 비인증 `/admin` → 302 → `/admin/login`
  - `USER` 로그인 상태로 `/admin` → 403
  - `CENTER_ADMIN` → 200
  - `SYSTEM_ADMIN` → 200
  - `USER` 로그인 상태로 `/admin/login` GET → 200 (permitAll)

### 8-2. 동적 검증 (curl / preview_start)

```bash
# preview_start(name: "youth-moa-e2e") 로 8090 기동
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin/login
# → 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin
# → 302 (redirect to /admin/login)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/css/admin.css
# → 200
curl -s http://localhost:8090/admin/login | grep -c "관리자 로그인"
# → >= 1

# 로그인 세션으로 /admin 진입 (cookie-jar 사용)
curl -c cookies.txt -b cookies.txt -X POST http://localhost:8090/admin/login \
  -d "username=sysadmin@youth-moa.test" -d "password=Admin!234" \
  -d "_csrf=<token>"
curl -b cookies.txt -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin
# → 200
```

### 8-3. 센터 격리 회귀 (필수 — ADMIN-00 §7)

`center1@youth-moa.test` 로 로그인 후:
- 대시보드 스탯 count 가 centers[0] 프로그램 count 와 일치
- centers[1] 프로그램은 최근 프로그램 목록에서 미노출
- 승인 대기 count = centers[0] 프로그램의 PENDING 신청 건수만

E2E: `admin-center-isolation.spec.ts` (A2 시 확장 예정 — A1 은 스탯 count 만 검증)

### 8-4. 계약 검사

```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-login
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-shell
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-dashboard
```

**완료 판정**: 위 3 spec 모두 갭 0 (deferred/deviation 제외).

### 8-5. 기능 E2E (인터랙션 · 필수)

- `e2e/tests/admin-login.spec.ts`
  - 비인증 `/admin` 접근 → `/admin/login` 리다이렉트 확인
  - 잘못된 자격증명 → 에러 alert + username 보존
  - 성공 시 `/admin` 도달
  - `CENTER_ADMIN` 로그인 → `/admin` 접근 가능
  - `USER` 로그인 후 `/admin` 접근 → 403
- `e2e/tests/admin-dashboard.spec.ts`
  - 스탯 카드 4개 렌더 + 실제 count 표시
  - 유저 드롭다운 → "사용자 페이지" 클릭 → `/` 이동 (세션 유지)
  - 유저 드롭다운 → 로그아웃 → `/admin/login?logout` 도달 (Qn-2)
  - 사용자 페이지(`/`) 로그인 상태에서 헤더 드롭다운 → `sec:authorize` 조건부 "관리자 페이지" 링크 표시 확인 (USER 계정은 미노출)

### 8-6. 시각 확인 (사용자 영역)

- 다크 헤더 시각 (`#111827`, 로고 white)
- 스탯 카드 아이콘 컬러 4종
- Auth Header 로고 primary
- 반응형 (≤1024 / ≤680) — **A8 이월** (deferred 기록)

## 9. 의존성 · 선행 작업

- P0-1 Flyway (완료) — 스키마 변경 없으니 V 파일 신설 없음
- P0-2 SecurityConfig 매처·CSRF (완료)
- P0-2 이월분 `/admin/login` formLogin — **본 PR 에 흡수 (Q3)**
- 관리자 시드 계정 — DataInitializer 이미 존재 (재확인만)

## 10. 이월 / 이탈 요약

| id | 필드 | 사유 |
|---|---|---|
| `dashboard.stat.trend-copy` | `deferred: A6` | "지난달보다 증가" 계산 미구현. 하드코딩 카피 유지 |
| `dashboard.urgent-programs.d-day` | `deferred: A3` | `applyEndDate` 컬럼 A3 도입 예정. A1 은 `endDate` 임시 파생 |
| `login.signup-button` | `deviation: 'ADMIN-00 Q8 관리자 signup 미구현'` | prototype 회원가입 버튼 제거 |
| `header.gnb.disabled-items` | `deviation: 'A2/A5/A6 순차 도입'` (Qn-6 A/C 채택 시) 또는 `deferred: A2/A5/A6` (Qn-6 B 채택 시) | GNB 목적지 미구현 |
| `header.search`, `header.bell.dropdown`, `header.center-scope-selector` | `deferred: A7` | 실동작 A7 |
| `dashboard.responsive` | `deferred: A8` | 반응형 정규화 A8 |
| `login.eye-toggle` | (구현 안 함) | 사용자 login 은 있으나 admin auth 카드는 단순 유지 |

## 11. 작업 큐 메타

- 작업 ID: `A1-admin-shell`
- 우선순위: 최상 (admin 트랙 파일럿)
- 추정 단위: 1 PR (헤더·GNB·인증·대시보드·계약·시드 확인)
- 브랜치: `feature/A1-admin-shell`
- 상태: **`spec_done`** — Qn-1~Qn-6 결정 후 `spec_confirmed`

## 12. 다음 단계

1. 사용자가 Qn-1~Qn-6 결정
2. 본 문서 반영 → `spec_confirmed`
3. ym-impl 호출 → `feature/A1-admin-shell` 브랜치 생성 → 구현
4. ym-qa → 정적/동적/계약/기능 E2E 4영역 분리 검증
5. ym-verify → RBAC 우회 시도·CSRF 누락 form 스캔
6. PR 머지 후 ADMIN-00 §9 큐에서 A1 체크

---

## 부록 A. 관련 라인 인용 요약

| 근거 | 파일 | 라인 |
|---|---|---|
| Auth Header | prototype.html | L60~64 |
| Login 카드 | prototype.html | L66~96 |
| 다크 헤더 정책 | HANDOFF.md | L27~44, L206~216 |
| Dashboard Welcome | prototype.html | L607~610 |
| Stat cards | prototype.html | L612~675 |
| Body row 그리드 | prototype.html | L678 |
| Recent programs | prototype.html | L680~702 |
| Quick links | prototype.html | L708~730 |
| Pending approval | prototype.html | L733~749 |
| 마감 임박 | prototype.html | L754~786 |
| RBAC 지시 | ADMIN-00 | §2-4 |
| 왕복 링크 결정 | ADMIN-00 | §8-1 |
| P0-2 이월 formLogin | SecurityConfig | L81~83 (주석) |
| 관리자 시드 완비 | DataInitializer | L52~62, L130~170 |
