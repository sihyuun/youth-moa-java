# 디자인 계약 — 프로그램 신청 폼 `/programs/{id}/apply`

> **추출 기준**: `docs/00_assets/prototype.tsx` `ProgramApply` L1125~1214 · 2026-08-11
> **검증 상태**: 계약 초안 (`spec_draft`) — Q-1~Q-3 사용자 결정 대기
> **기계 계약**: `e2e/contracts/apply.ts` — 총 38 check. 이 문서는 **판단이 필요한 구조**만 담는다
> **auth**: 필요. spec 이 `helpers.login(page, seedEmail(30))` 으로 선로그인 후 진입

## 1. 화면 아키텍처

**3단계 위저드 (단일 폼)** — 로그인 사용자 전용 · 700px 폭 중앙 정렬.

```
┌─── header.header (공용 헤더 · prototype 미렌더 · Q-1) ────┐
│                                                              │
│           ┌────── apply-inner ─────┐                         │
│           │  max-width 700          │                        │
│           │                         │                        │
│           │ [← 뒤로가기 (38x38)]     │                       │
│           │       프로그램 신청 (26/700 중앙) │              │
│           │                         │                        │
│           │  ① ─── ② ─── ③          │                        │
│           │ 신청자   추가   약관     │                       │
│           │                         │                        │
│           │  [Summary 카드 · 항상 노출 · 64썸네일+뱃지] │    │
│           │                         │                        │
│           │  ┌── step-card.is-active ──┐                     │
│           │  │  (step1: readonly 3필드)│                     │
│           │  │  (step2: 지원 동기 textarea)│                 │
│           │  │  (step3: 약관 박스 + 원형 라디오)│             │
│           │  └─────────────────────────┘                     │
│           │                         │                        │
│           │  [ 이전 flex:1 ][ 다음/신청완료 flex:2 ]           │
│           └─────────────────────────┘                        │
│                                                              │
├───────────────── Footer ─────────────────────────────────────┤
```

- 컴포넌트 계층: `header > main.apply-screen > .apply-inner > (뒤로가기 · 타이틀 · stepper · summary · form)`
- 위저드는 **단일 `<form>` 안에서 JS 가 `.apply-step-card.is-active` 토글**로 카드 전환 (SPA 없이 순수 client-side step)
- 서버 검증 실패 시 `window.__applyErrors` 로 에러 있는 스텝을 초기 노출

## 2. 상태 머신

| useState / DOM state | 초기값 | 트리거 | UI 변화 |
|---|---|---|---|
| `current` (JS) | `1` | `#applyNavNext` 클릭 → `+1` | `.apply-step-card[data-step-card="{current}"]` 만 `.is-active` |
| `current` | — | `#applyNavPrev` / `.apply-back-btn`(step>=2) | `-1` |
| `agreed` | `false` (checkbox unchecked) | privacyAgreed change | `#applyNavSubmit.disabled = !checked` |
| 서버 검증 실패 | — | POST 응답 | `window.__applyErrors.applyReason` → step=2 초기 노출<br>`window.__applyErrors.privacyAgreed` → step=3 초기 노출 |

prototype 은 이 위저드를 `step` state 로 동일하게 처리 (proto L1128). 구현은 순수 JS 로 재구현.

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 뒤로가기 (step=1) | `program-detail` | GET `/programs/{id}` | 정합 |
| 뒤로가기 (step>=2) | 이전 스텝 | 이전 스텝 (client) | 정합 |
| 다음 | 다음 스텝 | 다음 스텝 (client) | 정합 |
| 신청 완료 | `apply-complete` (client route) | POST `/programs/{id}/apply` → redirect `/apply/complete?applicationId={id}` | 정합 (동작 동일) |
| 마이페이지 안내 (텍스트) | — | 링크 아님 (안내 문구) | 정보 |

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ✅ 준수 | prototype 문구 그대로 (`프로그램 신청`, `회원정보 자동입력`, `신청 완료` 등) |
| P-2 그림자 | ✅ 준수 | Summary·step-card 모두 `--shadow-sm` |
| P-3 SVG 아이콘 | ❌ **이탈** | 뒤로가기가 `&larr;` HTML 엔티티 → SVG fragment `arrowL` 로 교체 필요. `back-btn.icon.svg` deferred |
| P-4 폭 토큰 | ✅ 준수 | 700px 를 그대로 명시. `--content-max` 미사용 |
| P-5 prototype 없는 추가 | ✅ 기록 | 서버 알림 슬롯 (applyError flash) · CSRF meta · `window.__applyErrors` 스크립트 (§5) |

## 5. prototype 에 없는 구현 추가 요소

POLICY P-5 — 여기 기록만, 계약 검사 대상 아님.

- **서버 알림 슬롯** (`.alert.alert-error`) — `applyError` flash / Bean Validation global error
- **CSRF meta** — POST 이므로 필수 (`_csrf` / `_csrf_header`)
- **`window.__applyErrors` 인라인 스크립트** — 서버 검증 실패 시 에러 있는 스텝을 초기 노출
- **필드 에러 표시** (`.apply-field-error`, `.has-error` classAppend) — Bean Validation 반영
- **공용 헤더** — prototype 미렌더지만 로그인 상태 정보구조상 유지 (Q-1 대기)

## 6. 계약이 커버하지 않는 항목

- step 2·3 카드 내부 필드의 크기·위치 — 초기 렌더에서 `display:none` 이라 측정 불가.
  → visual-apply.spec.ts 의 인터랙션 블록으로 step 전환 후 존재만 확인
- 위저드 스텝 전환 애니메이션 (`transition:all 0.2s`)
- 개인정보 체크박스 원형 라디오 시각화 (`::after` 가상 요소는 selector 로 잡히지만 계약 대상 아님)
- 서버 검증 실패 → 특정 스텝 초기 노출 동작 → visual-apply.spec.ts 인터랙션 블록
- 필드값 유효성 (`ApplyRequest` @Size/@AssertTrue) → `apply.spec.ts` 가 커버
- **F0c-dynamic-fields** (관리자 설정 동적 추가정보) — admin 트랙 선행 필요. 계약 미반영

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

| id | 필드 | 사유 |
|---|---|---|
| `back-btn.icon.svg` | `deferred` | 뒤로가기 `&larr;` → SVG fragment `arrowL` 이식. `fix/apply-complete-header` 인접 티켓에서 함께 처리 예정 |
| `header.present` | `deviation` | prototype 미렌더 vs 구현 렌더 — Q-1 결정 확정 시 확정 |

## 8. 결정 확정 (2026-08-11)

### ✅ Q-1. 공용 헤더 노출 — **(A) 헤더 제거 (prototype 정합)**
- 몰입도 높은 폼 흐름. 국내 관용(토스·컬리·kakao 결제 흐름) 정합
- `apply.html` 에서 `fragments/header :: header('programs')` 제거
- apply-complete 와 짝 결정 (둘 다 헤더 없음)

### ✅ Q-2. 완료 카드 radius — **(A) 12 로 통일 (proto 정합)**
- Summary 카드와 동일 `--radius-lg`

### ✅ Q-3. 위저드 nav 버튼 규격 — **impl 갭 리포트로 판별**
- 계약 50 기대 vs 실측. 갭 나오면 impl 단계에서 정합

## 9. 다음 단계

1. ~~Q-1~Q-3 사용자 결정~~ ✅ 2026-08-11 완료
2. `apply.ts` 갱신 (필요 시 deviation 정리)
3. ym-impl 인계 — 헤더 제거 · 완료 카드 radius 12 · nav 버튼 갭 청산 · 뒤로가기 SVG 이식
4. bootRun 후 `npx playwright test --project=contracts visual-apply` → 갭 0 확인

## 관련

- 완료 화면 계약: [apply-complete.md](apply-complete.md) · `e2e/contracts/apply-complete.ts`
- 프로그램 상세: [program-detail.md](program-detail.md) (신청 버튼 진입점)
- 폼 계열 계약: [login.md](login.md) · [signup.md](signup.md)
- 원본 명세: [../specs/F0c-remainder.md](../specs/F0c-remainder.md) (3단계 위저드 도입 배경)
- 파생 큐: `docs/specs/README.md §파생 큐` — `F0c-dynamic-fields`, `fix/apply-complete-header`
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
