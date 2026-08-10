# 디자인 계약 — 로그인 `/login`

> **추출 기준**: `docs/00_assets/prototype.tsx` L1987~2024 (`LoginScreen`) / 2026-08-10
> **검증 상태**: 계약 확정 (`spec_confirmed`) — Q-1~Q-4 결정 반영 2026-08-10. 갭 검사 · impl 대기
> **기계 계약**: `e2e/contracts/login.ts` — 총 29 check (filled 상태 2건 추가). px·색·폰트·개수는 그쪽에서 자동 검사. 이 문서는 **판단이 필요한 구조**만 담는다.

## 1. 화면 아키텍처

**단일 카드형 폼** — 헤더 없이 중앙 정렬 400px 폭 + Footer.

```
┌─────────────── viewport (bg=#F7F7F8) ───────────────┐
│                                                       │
│                (flex center, min-h 100vh)             │
│                                                       │
│         ┌────── auth-screen-inner ─────┐              │
│         │  width 400 · text-align center │            │
│         │                                │            │
│         │      [logo_primary 36h]        │            │
│         │           로그인 (h2 26/700)    │            │
│         │                                │            │
│         │   [input: username / 42h]      │            │
│         │   [input: password / 42h] 👁   │            │
│         │                                │            │
│         │  ☑ 아이디저장   찾기 | 비번찾기 │            │
│         │                                │            │
│         │   [ 로그인 (primary L 50h)  ] │            │
│         │   [ 회원가입 (secondary L)  ] │            │
│         └────────────────────────────────┘            │
│                                                       │
├───────────────────── Footer ─────────────────────────┤
```

- 컴포넌트 계층: `body.auth-page-body > main.auth-screen(flex center) > .auth-screen-inner(400px)`
- `.auth-screen` 은 `flex: 1` 이라 Footer 를 하단으로 밀어냄 (뷰포트 min-height 100vh)
- 헤더 slot 은 없음 — prototype L1996~2022 에 `Header` 컴포넌트 자체가 렌더되지 않음

## 2. 상태 머신

로그인 화면은 클라이언트 상태가 거의 없다.

| useState | 초기값 | 변화 트리거 | UI 변화 |
|---|---|---|---|
| `id` (username) | `''` | onChange | 입력 시 prototype inputStyle 이 `filled ? primary border + white bg : gray bg` 전환 (구현은 focus 시만 primary 로 전환하고 filled 상태 색 전환은 없음 — Q-3) |
| `pw` | `''` | onChange | 위와 동일 + Enter → handle() |
| — | — | 눈 아이콘 클릭 | type=password ↔ text 토글 (prototype 없음, 구현 추가) |

**서버 상태 (Thymeleaf)**:
- `param.registered` — 회원가입 완료 후 리다이렉트 → success alert
- `param.reset` — 비번 재설정 완료 → success alert
- `param.withdraw` — 탈퇴 완료 → toast (Wireframe WF-3-003-02)
- `logoutMsg` / `errorMsg` — flash 로 주입
- `savedUsername` — 로그인 실패 시 username 보존

prototype 은 이 알림 슬롯을 다루지 않는다 (React 로컬 alert() 로만 처리). 계약은 이 슬롯을 강제하지 않음.

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 로그인 버튼 | `home` (`onLogin()` → `go('home')`) | POST `/login` (SecurityConfig 처리) | 정합 (동작 동일) |
| 회원가입 버튼 | `signup` | GET `/signup` | 정합 |
| 아이디 찾기 | `find-id` | GET `/find-id` | 정합 |
| 비밀번호 찾기 | **`find-id`** (prototype 단순화) | GET `/find-password` | **정보구조 개선 — Q-2** |
| 로고 클릭 | 없음 | GET `/` | 구현 추가 (문제 없음) |

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ✅ 이탈 표시 | "아이디 저장" → "로그인 상태 유지" 로 이탈. `options.checkbox.label` 에 `deviation` |
| P-2 그림자 | — | 로그인 화면에 그림자 사용 없음 |
| P-3 SVG 아이콘 | ✅ 준수 | 눈 아이콘이 이미 SVG. 문자 대체 없음 |
| P-4 폭 토큰 | ✅ 준수 | 400px 를 그대로 명시. `--content-max` 미사용. 아이디/비번찾기 4화면도 480→400 통일 |
| P-5 prototype 없는 추가 | ✅ 기록 | 눈 아이콘 · 로고 링크 · 서버 알림 슬롯 (§5) · **input 배경 패턴 A** (empty=흰, prototype 이탈) |

## 5. prototype 에 없는 구현 추가 요소

POLICY P-5 에 따라 여기 기록만 (계약 검사 대상 아님).

- **비밀번호 눈 아이콘 토글** (`.auth-eye-btn`) — signup 패턴 재사용. 접근성·UX 표준
- **로고 클릭 시 홈 이동** (`a.auth-logo-link`) — prototype 로고는 클릭 불가
- **서버 알림 슬롯** — `param.registered/reset/withdraw`, `logoutMsg`, `errorMsg` (Thymeleaf `th:if`)
- **CSRF meta** — `_csrf` / `_csrf_header` (POST /login 은 CSRF 필요)
- **탈퇴 완료 토스트 스크립트** — Wireframe WF-3-003-02 정책 (`docs/wireframe-policy/` 참조)
- **자동완성 힌트** — `autocomplete="email"` / `autocomplete="current-password"` (브라우저 UX 표준)

## 6. 계약이 커버하지 않는 항목

- input focus 시 border 색 전환 애니메이션 (`transition: border 150ms, background 150ms`)
- prototype inputStyle 의 **filled 상태 스타일 전환** (proto L105) — 값이 있으면 배경 white + border primary. **구현은 focus 만 primary, filled 상태 색 전환 없음** (Q-3)
- Enter 키로 로그인 제출 (form submit 이라 브라우저 기본 동작)
- 회원가입/비번재설정/탈퇴 완료 후 리다이렉트 알림 표시 (동적 검증 필요)
- Footer 자체 정합성 — `common.ts` 계약이 커버

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

계약 파일에 기록된 표기.

| id | 필드 | 사유 |
|---|---|---|
| `eye.icon.svg` | `deferred` | prototype 에 없는 추가 요소 — 화면 문서에만 기록 (POLICY P-5) |
| `options.checkbox.label` | `deviation` | POLICY P-1. Spring Security remember-me 동작에 맞춘 카피 |
| `options.find-pw.text` | `deviation` | Q-2 확정 — 구현이 정보구조상 낫다는 판정 |
| `input.pw.type` | `deviation` | CSS display 로 type 검증 불가 — sanity check 만 |

## 8. 결정 확정 (2026-08-10)

### ✅ Q-1. input 높이·radius·border — **prototype 값 채택 (42/10/1)**

- **현재 구현**: `.auth-input` height 46 · radius 8 · border 1.5px
- **prototype**: height 42 · radius 10 · border 1px (proto L105)
- **결정**: 맞춘다. `.auth-input` 을 signup 의 `.form-input` 과 같은 42/10/1px 로 통일 → **42/10/1 을 폼 전 화면 표준으로 승격**
- **영향 범위**: `login`, `signup`, `find-id`, `find-password`, `find-password-reset`, `mypage/profile-edit`, `mypage/profile-verify` (총 7화면)
- 계약 3건 (`input.id.height`, `input.id.border-radius`, `input.pw.height`) 이 갭으로 남고 ym-impl 이 청산

### ✅ Q-2. 비밀번호 찾기 라우팅 — **구현 유지 (별개 라우팅)**

- **결정**: `/find-id` · `/find-password` 별개 라우팅 유지. prototype 이 두 별개 흐름을 하나로 축소한 것을 정보구조 약화로 판정
- `options.find-pw.text` deviation 확정 (login.ts 반영)

### ✅ Q-3. input filled 상태 스타일 전환 — **레퍼런스 패턴 A 채택 (2026-08-10 재결정)**

- **최초 결정**: prototype L105 filled 스타일 (empty=회색, filled=흰+primary) 그대로 채택
- **재결정 (사용자 지시)**: 국내 참조군(토스·카카오·당근·컬리) 관용에 맞춰 **패턴 A** 로 전환
  - empty (편집 가능·값 없음): **흰 배경** + 회색 border  ← prototype 대비 이탈
  - focus: 흰 배경 + primary border
  - filled: 흰 배경 + primary border (prototype 정합)
  - readonly: **회색 배경** + secondary text  ← 편집 불가 시그널 압축
- **근거**: "회색=편집불가" 단일 시그널로 학습 비용 최소. filled 를 border 색만으로 표현해 시각 밀도를 낮춤
- **적용 범위**: `.auth-input` · `.form-input` (`.form-input.form-input--date:invalid/:valid` 포함) 폼 전 7화면
- **계약 검증**: filled 상태는 border 색으로만 검증 (bg 는 항상 white). 계약 파일은 P-5(추가 요소) 로 재분류
- **placeholder 속성 필수 조건 유지**: filled selector `:not(:placeholder-shown)` 작동을 위해 폼 input 은 placeholder 를 반드시 보유

### ✅ Q-4. 상단 로고 — **`logo_primary.png` (풀 로고) 유지**

- prototype `assets/logo.png` ≡ 구현 `logo_primary.png`. 정합.

## 9. 다음 단계

1. ~~Q-1 ~ Q-3 사용자 결정 반영~~ ✅ 2026-08-10 완료
2. ~~`login.ts` 의 `deviation`/`deferred` 필드 확정~~ ✅
3. ym-impl 인계 — Q-1 (`.auth-input` → 42/10/1) + Q-3 (filled 상태 CSS) 폼 전 7화면 일괄
4. bootRun 후 `npx playwright test --project=contracts visual-login` 실행 → 갭 0 확인
5. ym-verify 최종 관문 → 커밋

## 관련

- 폼 계열 계약: [signup.md](signup.md) · `e2e/contracts/signup.ts`
- 공통 헤더·푸터: [common.md](common.md)
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
