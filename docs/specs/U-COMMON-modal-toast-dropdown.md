---
id: U-COMMON-modal-toast-dropdown
status: spec_confirmed
created: 2026-07-13
decided_by: 사용자 + HANDOFF §4-S
---

# U-COMMON — 공통 Modal / Toast / Dropdown 인프라

> HANDOFF §4-S 확정 스펙 준수. 이 문서는 우리 코드베이스 적용 계획.

## 2 PR 분리

- **PR-A · U-COMMON-01 Modal/Toast** — HANDOFF §4-S.1~2 구현
- **PR-B · U-COMMON-02 Header dropdown** — HANDOFF §4-S.3 구현

## 확정 사항

### B-Q1 = HANDOFF M2 (바텀시트, 사용자 이전 응답 정정)
- `<768px` 브레이크포인트에서 콘텐츠형 Modal → **하단 시트** (그랩 핸들 · `max-height: 90vh` · 상단 radius 16 · `width: 100vw`)
- `ConfirmDialog` (짧은 확인창) 은 모바일에서도 중앙 카드 유지 (좌우 마진 20px)

### B-Q2 = HANDOFF D1 (클릭 토글, hover 폐지)
- Header dropdown + 필터 드롭다운 모두 클릭 토글
- CSS `:hover`/`:focus-within` 오픈 로직 제거 → JS `open` 상태 제어
- `slideDown 180ms` 애니메이션 (`.dropdown-enter`)
- 닫힘: 바깥 클릭 · ESC · 항목 선택
- `aria-haspopup="menu"`, `aria-expanded` 부착

### B-Q3 = HANDOFF D2 (A안 2항목)
- 유저 드롭다운: `[프로필 카드 (아바타 + 이름 + 이메일, 클릭 시 마이페이지)]` + `[로그아웃]`
- **알림 설정은 드롭다운에서 제외** — 마이페이지 상단 탭에서만 접근 (`tab=noti`)
- 알림 벨 드롭다운(`NotificationPanel`)은 별도 유지

### B-Q4 = **후속 티켓 이월** (memory 필수 갱신)
- alert() 14곳 치환은 U-COMMON-01 PR 에서 제외
- 별도 후속 티켓 `refactor/alert-to-toast` 로 분리 → 관련 memory 갱신
- 이유: U-COMMON-01 은 인프라 도입에 집중, alert 치환은 각 화면 회귀 검증 필요해 스코프 팽창 방지

### B-Q5 = c (선별 통합)
- `.mypage-modal` → 공통 `.modal-card` 로 치환 (mypage/history.html + main.css dead code 제거)
- `.mypage-toast` → **개념·CSS 존치** (인라인 flash 카테고리로 유지). 명확성 위해 `.page-flash` rename 검토
- 신설 `.toast-stack .toast` 는 JS 트리거용 (서버 flash 는 `.page-flash`, JS toast 는 `.toast-stack`)

### B-Q6 = 이번 범위 제외
- `WaitlistModal` / `FilterModal` 도메인 모달은 그룹 C (F0f-fix-1) 로 이관 (C-Q5=B 알림 백엔드 포함으로 이관됨)

## HANDOFF 스펙 (§4-S) 준수 사항

### Modal
- `.modal-backdrop` (rgba(0,0,0,0.45) · z-index base 500)
- `.modal-card` (radius `--radius-lg` · shadow `--shadow-lg`)
- 계층: backdrop / card / card__header / card__title / card__close / card__body / card__footer
- variants: `--sm/md/lg` (400/460/560)
- **M1 focus trap**: `role="dialog"` `aria-modal="true"`, 카드 내부 포커서블 전체 순환, 첫 포커스 = 제목 다음 첫 인터랙티브 요소, Esc/닫기 시 트리거로 포커스 복귀, 배경 `inert`
- **M3 스태킹**: 최상위 모달만 백드롭 렌더 (누적 금지), z-index 스택마다 +10, 2단까지만

### Toast (전역 단일 인스턴스)
- 상단 중앙 `top:90px`, `z-index:1000`, 흰 카드 (radius 12 · shadow `0 8px 32px rgba(0,0,0,0.14)`)
- 좌측 성공 아이콘 22px · `#22C55E` 체크 + 15px/500 메시지
- 진입 `slideDown 300ms`, **2800ms 자동 소멸**
- `role="status"` `aria-live="polite"`
- variants: `--success` (기본) · `--error` · `--info`

### Dropdown
- **패널**: 흰 배경, radius **14px**, shadow `0 12px 40px rgba(0,0,0,0.14)`, `border 1px borderLight`
- 유저메뉴 width 220~240px / 알림 패널 380px
- 항목 hover: `background: var(--color-primary-bg)` (translateY 없음)
- 항목 padding `11~16px 18px`, 아이콘-라벨 gap 10px
- 항목 구분선 `1px borderLight`

## 파일 배치

| 신규 | 경로 |
|---|---|
| CSS | `static/css/main.css` — `/* ===== §4-S Common UI (Modal/Toast/Dropdown) ===== */` 섹션 |
| JS | `static/js/common-ui.js` — `window.Toast`, `window.Modal` API |
| Fragment | `templates/fragments/common-ui.html` — `toast-stack`, `modal` 파라미터 fragment |
| RenderTest | `CommonUiRenderTest`, `HeaderDropdownRenderTest` |

## JS API 명세

```js
window.Toast = {
  show(message, variant = 'success', duration = 2800) { ... }
};

window.Modal = {
  open(id) { ... },
  close(id) { ... },
  confirm({ title, message, confirmText, cancelText, variant, onConfirm }) { ... }
    // → Promise, focus trap 자동 설정
};
```

## 검증

- 정적: `CommonUiRenderTest` (fragment 렌더 · toast-stack 존재 · aria-*)
- 동적: `curl /` 응답에 `<div class="toast-stack"`, `curl /js/common-ui.js` 200
- 시각 (Preview + 스크린샷 대조): d3-user-dropdown.png 와 대조
- 접근성: keyboard tab 순환 · Esc 닫기 · outside click 닫기
