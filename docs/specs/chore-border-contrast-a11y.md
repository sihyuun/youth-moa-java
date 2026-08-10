---
name: chore-border-contrast-a11y
status: spec_skeleton
created: 2026-08-10
origin: login-contract-fix ym-verify UNVERIFIED U2
---

# `--color-border` WCAG 대비 개선 (접근성)

## 배경

`login-contract-fix` 트랙(2026-08-10)에서 레퍼런스 패턴 A(폼 input 배경 흰색화) 채택 후, **`--color-border` (oklch(0.9 …) ≈ `#E1E1E7`) 와 흰 배경의 대비율이 약 1.4:1** 로 측정됨. WCAG SC 1.4.11 (Non-text Contrast) 요구 사항인 **3:1 미달**.

이전(empty=회색 배경) 상태에선 배경 자체가 필드 영역을 표시했으나, 패턴 A 이후 border 만으로 필드 경계를 표시해야 하는 상황에서 대비 부족이 시각·접근성 문제로 노출됨.

**참고**: 이 문제는 login-contract-fix PR 범위를 벗어나는 전역 디자인 토큰 이슈. ym-verify UNVERIFIED U2 로 판정 유예 → 별도 스펙(이 문서)으로 분리.

## 범위

- `--color-border` 토큰 값 조정 (또는 폼 요소 전용 별도 토큰 신설)
- 영향받는 컴포넌트 전수 조사·재검증
- WCAG SC 1.4.11 3:1 이상 달성

## 결정 대기 항목 (Q)

### Q-1. 전역 토큰 조정 vs 폼 요소 전용 토큰 분리

| 옵션 | 방법 | 장점 | 단점 |
|---|---|---|---|
| (a) `--color-border` 전역 어둡게 | `oklch(0.9 …)` → `oklch(0.82 …)` (약 3:1) | 일관성 유지, 관리 단순 | 모든 UI(카드·구분선·버튼)에 시각 영향, 회귀 폭 큼 |
| (b) 폼 전용 `--color-border-form` 신설 | 폼 요소만 새 토큰 사용 | 회귀 최소 | 토큰 이원화 관리 부담, 규칙 학습 비용 |
| (c) input 등 특정 요소만 어둡게 하드코드 | `.form-input { border-color: oklch(0.82 …) }` | 즉시 적용 가능 | 토큰 원칙 이탈, 유지보수 어려움 |

**추천 초안 (a)** — 이유: 디자인 시스템 전체가 대비 부족이라 토큰 조정이 정직한 대응. 다만 회귀 검증이 필수 (계약 재실행 + 시각 스캔).

### Q-2. 3:1 달성 최소 오클루드 값?

`oklch(0.9 …)` → 얼마까지 어둡게 낮춰야 3:1 을 넘는가?
- 흰 배경 대비 3:1 = 상대 명도 약 0.65 이하 필요
- oklch L 축 기준 대략 **0.75 ~ 0.80** 근방으로 추정 (측정 필요)
- 색조(hue) 축은 유지 (280°, primary 계열 미묘한 청보라 tint)

## 회귀 검증 방법

1. `oklch(x …)` 후보값 3~5개(0.85 · 0.82 · 0.80 · 0.78 · 0.75) 로 순차 교체 후 계약 재실행
   - `cd e2e && npx playwright test --project=contracts` — 계약 갭 회귀 확인
   - `--project=chromium` — 기능 E2E 회귀 확인
2. 실제 대비율 측정
   - `docs/00_assets/prototype.tsx` 렌더 스크린샷과 `/login`, `/signup`, `/programs`, `/centers` 실측 대비
   - Chrome DevTools > Rendering > Emulate CSS `prefers-contrast` (더 진한 대비 모드) 대응 확인
3. 시각 스캔: home / programs / centers / program-detail / signup / login / mypage 7화면 스크린샷 비교
   - 카드 border · 구분선 · form input · popover · 버튼 outline 모두 확인
4. 접근성 도구
   - Lighthouse Accessibility 스코어
   - axe DevTools "color-contrast" 룰

## 검증 스크립트 (미구현 — 착수 시 작성)

```javascript
// 예시: 흰 배경에서 border-color 대비율 자동 측정
// canvas 픽셀 색 추출 → WCAG relative luminance 공식 → 대비율 계산
async function measureContrast(page, selector, expectedBg = 'white') {
  return await page.locator(selector).evaluate((el, bg) => {
    const s = window.getComputedStyle(el);
    // ... canvas pixel extraction + luminance calc ...
    return { borderColor, contrastRatio };
  }, expectedBg);
}
```

## 영향받는 컴포넌트 (초안 — 착수 시 확정)

`--color-border` 사용처 (grep):
- `.form-input` · `.auth-input` · `.signup-gender-pill` 등 폼 요소
- `.center-card` · `.program-card` 등 카드
- `.filter-popover` · dropdown 등 팝오버
- `.btn-outline-sm` 등 아웃라인 버튼
- 각종 구분선 (`border-top` · `border-bottom`)

**총 사용처 수** — 착수 시 `grep -c` 로 확정 (수백 곳 예상)

## 진행 조건

- login-contract-fix 커밋 완료 후
- 남은 9화면 계약 트랙 종료 후 (별도 트랙으로 이 스펙이 화면 계약을 방해하지 않도록)
- 또는 사용자가 별도 우선순위 부여 시 즉시

## 참고

- ym-verify 판정 회신 (login-contract-fix, 2026-08-10) — U2 UNVERIFIED 사유
- 레퍼런스 패턴 A 도입 이력 — `docs/design-contracts/login.md` §8 Q-3
- POLICY P-4 (폭 토큰 규칙) 참고 — 토큰 변경 관리 원칙
