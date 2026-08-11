# 디자인 계약 — 회원가입 `/signup`

> **추출 기준**: `docs/00_assets/prototype.tsx` L1842~1983 (`SignupScreen`) + 공통 `Btn` L84~102 · `inputStyle` L105 · `inputRO` L106 / 2026-08-10
> **검증 상태**: 서술 계약 신설 (`spec_review`) — Q-1~Q-3 사용자 결정 대기. 기계 계약 확장은 본 문서와 동시 커밋
> **기계 계약**: `e2e/contracts/signup.ts` — 총 N check (9 → 확장). px·색·개수는 그쪽에서 자동 검사. 이 문서는 **판단이 필요한 구조**만 담는다.

## 1. 화면 아키텍처

**단일 카드형 3섹션 폼** — 헤더 없이 중앙 정렬 560px 폭 + Footer.

```
┌──────────────── viewport (bg=#F7F7F8) ───────────────────┐
│                                                            │
│               (flex center, min-h calc(100vh - 72px))      │
│                                                            │
│         ┌────────── signup-inner (560px) ──────────┐       │
│         │                                          │       │
│         │       [logo_primary 34h]                 │       │
│         │           회원가입 (h2 26/700)            │       │
│         │       몇 가지 정보만 입력하면… (14/sec)    │       │
│         │                                          │       │
│         │  ┌─── form-card #1: 계정 정보 ───┐        │       │
│         │  │ 아이디 [input 42h] [중복확인 42h] │        │       │
│         │  │ 비밀번호 [input 42h] 👁          │        │       │
│         │  │ 비밀번호 확인 [input 42h] 👁     │        │       │
│         │  └───────────────────────────────┘        │       │
│         │  ┌─── form-card #2: 개인 정보 ───┐        │       │
│         │  │ 이름 [input 42h]                │        │       │
│         │  │ 핸드폰 [input] [인증요청 42h]    │        │       │
│         │  │   (SENT) [코드 6자리 3:00][확인]│        │       │
│         │  │   (VERIFIED) ✓ 인증 완료         │        │       │
│         │  │ 성별 [남][여]  생년월일 [date]  │        │       │
│         │  │   (2컬럼 flex gap 14)           │        │       │
│         │  │ 주소                             │        │       │
│         │  │   [우편번호 RO] [검색 42h]       │        │       │
│         │  │   [주소 RO]                      │        │       │
│         │  │   [상세주소]                     │        │       │
│         │  └───────────────────────────────┘        │       │
│         │  ┌─── form-card #3: 이용약관 동의 ─┐        │       │
│         │  │ ☑ 전체 동의 (강조 박스)          │        │       │
│         │  │ ✓ 회원가입약관 (필수)  약관보기 › │        │       │
│         │  │ ✓ 개인정보처리… (필수) 약관보기 › │        │       │
│         │  └───────────────────────────────┘        │       │
│         │  [ 회원가입 (primary L fullWidth 50h) ]   │       │
│         │  이미 계정이 있으신가요? 로그인            │       │
│         └──────────────────────────────────────────┘       │
├───────────────────── Footer ─────────────────────────────┤
```

- 컴포넌트 계층: `body > main.signup-screen(flex center, padding 48px 80px) > .signup-inner(560px) > .signup-header + form > 3× section.form-card + .signup-actions + p.signup-login-link`
- prototype L1866~1867 padding `48px 80px` · width 560
- **헤더 slot 없음** — prototype L1865 `screen-enter` 직속 자식만 렌더 (전역 `Header` 미사용)
- form-card 스타일: `background surface + border-radius 16 + border-light + shadow-sm + padding 24px 28px + margin-bottom 16` (prototype L1854 `card`)

## 2. 상태 머신

회원가입은 4단계 검증 phase 가 있고 각 phase 는 완료돼야 submit 이 성립한다.

| useState / 서버 상태 | 초기값 | 트리거 | 검증 phase 영향 |
|---|---|---|---|
| `form.id/pw/pw2/name/phone/dob/zip/addr/addr2` | `''` | onChange | filled UI 전환 (border primary) |
| `gender` | `''` | pill 클릭 | radio hidden 동기화 |
| **`emailChecked` (구현 hidden)** | `false` | 중복확인 API 성공 | phase 1 통과 |
| **`verify.sent`** | `false` | 인증요청 클릭 | 코드 입력 row 노출 |
| **`verify.code`** | `''` | onChange, 숫자 6자리 clip | 확인 버튼 disabled 해제 |
| **`verify.left`** | `180` | 매 초 감소 | 0 시 만료 안내 표시 |
| **`verify.done`** | `false` | 확인 클릭 | phase 2 통과 + 배지 노출 + phone lock |
| `agree.terms/privacy/…` | `false×N` | 개별 체크박스 · 전체 동의 | phase 3 통과 |
| `allAgreed` (derived) | `false` | 모든 필수 term true | 전체 동의 강조 박스 색 전환 |

### 검증 phase 다이어그램

```
INITIAL
  │  이메일 입력 + 중복확인 클릭
  ▼
EMAIL_CHECKED          ── 이메일 재편집 시 emailChecked=false (구현 규칙)
  │  핸드폰 입력 + 인증요청
  ▼
PHONE_SENT (타이머 3:00)
  │  코드 6자리 + 확인   │  3:00 만료 → EXPIRED (재요청 필요)
  ▼
PHONE_VERIFIED         ── phone input readonly / 인증요청 → 재인증 라벨
  │  약관 전체 동의
  ▼
TERMS_AGREED
  │  submit → 서버 검증 (Bean Validation + emailChecked + phoneVerified + agreements)
  ▼
POST /signup 성공 → /welcome (onLogin() → go('welcome'))
              실패 → 폼 재표시 + 입력값 보존 + errorMsg / globalErrors
```

- **prototype 은 phone 재인증 시 verify state 를 초기화**한다 (L1909) — 구현은 이 동작을 유지해야 SENT → INITIAL 회귀가 가능
- **prototype 은 이메일 재편집 시 emailChecked 초기화 규칙이 없다** — 구현이 UX 안전을 위해 추가 (P-5). signup.html L494 참조

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 중복확인 버튼 | `alert('사용 가능한 아이디입니다.')` (L1882) | `GET /api/users/check-email?email=…` | **정보구조 강화** — prototype 은 mock, 구현은 실제 API |
| 인증요청 / 재요청 / 재인증 | 클라이언트 state 만 (L1909) | 서버 SMS 발송 (F-signup-01) | 정보구조 강화 |
| 확인 (코드 검증) | 클라이언트 state 만 (L1917) | 서버 코드 검증 API | 정보구조 강화 |
| 우편번호 검색 | mock 주소 채움 (L1946) | Daum Postcode API | 정보구조 강화 |
| 약관보기 › | `go('terms')` (L1970) | `/terms` · `/privacy` fetch → 오버레이 모달 (F-signup-terms-agreement) | **정보구조 개선** — 새 탭 이탈 없이 회원가입 흐름 유지 |
| 회원가입 submit | `onLogin(); go('welcome')` (L1861~62) | POST `/signup` → `/welcome` (성공) | 정합 |
| 로그인 링크 | `go('login')` (L1977) | GET `/login` | 정합 |
| 로고 클릭 | 없음 | GET `/` | 구현 추가 (P-5) |

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ✅ 준수 | prototype 카피 대부분 유지. 검증 실패 메시지는 CLAUDE.md 어조 통일안(`~해야 합니다.`) 적용 — signup 도메인 특화 정책이라 계약 이탈 아님 |
| P-2 그림자 | ✅ 준수 | form-card 는 `--shadow-sm` (브랜드 틴트) 사용 |
| P-3 SVG 아이콘 | ⚠️ 부분 이탈 | 성별 check·눈·검색·약관 체크는 SVG. **전체 동의 체크는 `content: '✓'` 문자 (main.css L2736), 개별 term 체크도 `content: '✓'` (L2778)** — P-3 위반이나 구현 완료 시점 규칙 신설 전이라 `deferred` 후보. 사용자 결정 필요 (Q-3) |
| P-4 폭 토큰 | ✅ 준수 | `.signup-inner` 는 560 하드코딩 (prototype 명시값). `--content-max` 미사용 |
| P-5 prototype 없는 추가 | ✅ 기록 | 눈 아이콘·로고 링크·서버 검증 슬롯·CSRF meta·이메일 재편집 emailChecked reset·약관 오버레이 모달·Daum Postcode·비밀번호 정책 실시간 검증 (§5) |

## 5. prototype 에 없는 구현 추가 요소

POLICY P-5. 계약 검사 대상 아님.

- **비밀번호 눈 아이콘 토글** — `.signup-eye-btn` type=password ↔ text
- **비밀번호 정책 실시간 검증** (`pw-policy-msg`) — signup.html L331~378. `~해야 합니다.` 어조로 미충족 조건 조립. 충족 시 초록색 `signup-field-ok`
- **비밀번호 확인 실시간 mismatch** — signup.html L381~397
- **이메일 재편집 시 emailChecked reset** — 우회 차단 (signup.html L494)
- **CSRF meta** — POST /signup 필요 (`_csrf`, `_csrf_header`)
- **서버 검증 슬롯** — `errorMsg`, `#fields.hasErrors('*')`, `passwordPolicyMsg`, `passwordMatched` @AssertTrue 가상 필드
- **로고 클릭 시 홈 이동** — prototype 로고는 클릭 불가
- **자동완성 힌트** — `autocomplete="email/new-password/name/tel"`
- **약관 오버레이 모달** — `/terms` fetch → `.policy-page` injection. JS off 시 새 탭 fallback (F-signup-terms-agreement)
- **Daum Postcode API 연동** — 우편번호 검색
- **native date picker** (`type=date`) — prototype 은 text placeholder="YYYY / MM / DD". 접근성·모바일 UX·유효성 자동검증 이유로 의도적 이탈 (2026-08-10 결정, 계약 `birthdate.native` deviation)
- **성별 pill empty=흰 배경** — prototype 은 `#F7F8FA` 회색. 레퍼런스 패턴 A 확장 (input 과 동일 정책, deviation)
- **약관 개별/전체 동의 4 항목 세분화 가능성** — 현재 구현은 SERVICE·PRIVACY 2종. prototype 은 2종. 추후 마케팅 수신 등 확장 시 계약이 아닌 policy 축으로 처리 (P-1 카피 예외의 "정보구조" 규칙 해당)

## 6. 계약이 커버하지 않는 항목

- input focus / filled 시 border 색 전환 transition (150ms)
- 인증 타이머 감소 (매초 setTimeout) — 동적 검증만 가능
- 우편번호 검색 팝업 → oncomplete 채움 흐름 — 외부 SDK 의존
- 약관 오버레이 fetch 성공/실패 분기
- 회원가입 성공 후 `/welcome` 리다이렉트 — E2E 시나리오
- Footer 정합성 — `common.ts` 가 커버
- prototype L1858 필수 필드 alert 검증 순서 — Bean Validation 이 대체

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

기계 계약 (`signup.ts`) 에 기록될 표기.

| id | 필드 | 사유 |
|---|---|---|
| `birthdate.native` | `deviation` | prototype text placeholder → 구현 native date picker (2026-08-10 결정). 접근성·모바일 UX 우선 |
| `gender.pill.empty-bg` | `deviation` | 레퍼런스 패턴 A. input filled 정책과 통일 — 편집 가능 상태 흰 배경 (POLICY P-5 확장 근거) |
| `phone.readonly-bg` | `deviation` | prototype `#F3F4F6` (L1908) 정합. verify.done 시 phone lock. `.signup-phone-locked` 규칙과 함께 확정 |
| `terms.check.svg` | `deferred: docs/specs/F-signup-svg-check.md` | POLICY P-3 위반 — 전체 동의·개별 term 체크가 `content:'✓'` 문자. 별도 티켓으로 SVG 이식 예정 (Q-3 확정 시) |
| `logo.link` | `deferred` | prototype 없는 추가 요소. §5 P-5 기록 |
| `eye.icon` | `deferred` | 동상 |

## 8. 결정 확정 (2026-08-10)

### ✅ Q-1. 성별 pill 흰 배경 — **deviation 확정 (prototype 이탈)**

- prototype `#F7F8FA` → 구현 `var(--color-surface)` 흰색
- 근거: 레퍼런스 패턴 A 확장 (login.md Q-3 동일 논리) — "회색=편집불가" 단일 시그널

### ✅ Q-2. 생년월일 native date picker — **deviation 확정 (prototype 이탈)**

- prototype `type=text` → 구현 `type=date` native picker
- 근거: 접근성·모바일 UX·유효성 자동 검증. signup.html L206~207 주석에도 이탈 명시

### ✅ Q-3. 약관 체크 아이콘 — **옵션 A 확정 (SVG 이식)**

- prototype L1959~1967 SVG 정합
- 근거: POLICY P-3 (SVG 강제, 이탈 불가) 준수
- 처리: main.css 두 규칙(L2736·L2778) `content:'✓'` → SVG mask 또는 background-image 로 교체
- signup.ts `terms.check.svg` deferred → 검사 대상으로 승격 (deferred 필드 제거)

### ✅ 추가 정합: submit.height 48 → 50

- prototype L85 `sizes.l.h:50` 정합
- 처리: main.css `.signup-submit-btn` height 48 → 50 (한 줄)
- signup.ts `submit.height` deferred → 검사 대상으로 승격

## 9. 다음 단계

1. ~~Q-1 · Q-2 · Q-3 사용자 결정~~ ✅ 2026-08-10 완료
2. ~~`signup.ts` 의 `deviation` / `deferred` 필드 확정 반영~~ ✅
3. ym-impl 인계 — 갭 청산 (약관 SVG 이식 + submit 50 정합, 예상 3~4건)
4. bootRun 후 `npx playwright test --project=contracts visual-signup` 실행 → 갭 0 확인
5. ym-verify 최종 관문 → 커밋

## 관련

- 폼 계열 계약: [login.md](login.md) · `e2e/contracts/login.ts`
- 공통 헤더·푸터: [common.md](common.md)
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
- signup.html 구현: `src/main/resources/templates/user/signup.html`
- signup 스타일: `src/main/resources/static/css/main.css` L2374~2819 · L5926~5995
