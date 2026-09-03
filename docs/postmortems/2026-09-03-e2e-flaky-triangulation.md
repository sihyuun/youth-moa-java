# E2E flaky 3층 원인 삼각측량 — 2026-09-03

## 요약

`--project=contracts` 계열에서 오랫동안 `visual-apply-complete.spec.ts` 가 flaky 였으나
CI `contracts` non-blocking + 1회 실행 시 우연히 PASS 되는 특성 때문에 표면화되지 않고 지속됨.
2026-09-01 ~ 09-03 세션에서 원인 3층(A형·B형·backdrop) 을 순차 분리·해소.

| 층 | 원인 | 조치 PR |
|---|---|---|
| **A형** | `waitUntil:'commit'` 후 108ms 만에 click → `apply.html` IIFE 리스너 부착 전 이벤트 발사 → step 1 정체 | #199 (IIFE 최하단 `window.__applyReady` 플래그 + helper 대기), #201 (자매 spec 통일) |
| **B형** | `seedIdx = 29 + (Math.floor(Date.now()/1000) % 10)` 초 단위 rotation + `webServer.reuseExistingServer:true` 로 self-pollution → `IllegalStateException("이미 신청한 프로그램")` | #200 (`@Profile("e2e")` `POST /__test__/reset-applications` cleanup endpoint) |
| **backdrop** | `.program-calendar-mobile-sheet-backdrop` (`inset:0`) center click 이 하단 `panel` 에 가려짐. 오늘 seed 롤오버로 노출 | #200 (`click({position:{x:10,y:10}})`) |

## 왜 오래 이어졌는가

1. **`contracts` 프로젝트가 CI non-blocking** — `playwright.config.ts:56~59` 주석 유지로
   실패해도 pipeline 통과 → 실패 신호가 문서·리뷰 어디에도 남지 않음
2. **1회 실행 우연 PASS** — solo 실행 재현률 3/5(60%) 라 개발자 로컬 스팟체크로 놓침.
   `--repeat-each` 습관 없음
3. **fresh 서버 우회** — `webServer.reuseExistingServer:true` 로 로컬은 서버 재부팅 직후에는
   B형 미발동
4. **팩트체크 없이 "24/24" 인용** — 개수만 보고 문서화하는 습관 → "실은 23/24" 사실 소실

## 원인 삼각측량 절차 (재발 시 재현용)

1. `--repeat-each=5` 또는 `--repeat-each=10` 로 재현률 정량화
2. 실패 스냅샷 `error-context.md` 를 grep 로 유형 분리:
   - `grep "이미 신청한 프로그램"` → B형 (seed self-pollution)
   - `[data-step-card=\"1\"]` visible 지속 → A형 (JS 초기화 경합)
   - `intercepts pointer events` → backdrop/overlay pattern
3. 부모 커밋 (main HEAD) 에서 동일 재현 → 이번 변경 인과 배제
4. 반증 실험 (fresh 서버 재부팅 후 재실행) 으로 timing vs 상태 오염 구분

## 재발 방지 장치 (도입 완료)

- **A형**: `apply.html` IIFE 최하단 `window.__applyReady = true`, helper `applyNextStep()` 이
  `waitFor(attached) + waitForFunction(__applyReady)` 이중 방어. `waitUntil:'commit'` 도 신청 계열
  전 spec 에서 `'domcontentloaded'` 로 통일 (#201)
- **B형**: `TestFixtureController @Profile("e2e") POST /__test__/reset-applications`.
  3중 프로파일 가드 (`@Profile` + `SecurityConfig.matchesProfiles` + `TestFixtureProfileGuardTest`
  + `TestFixtureHttpGuardTest` (실 HTTP 404 assert))
- **backdrop**: `click({position:{x:10,y:10}})` 로 panel 위쪽 여백 명시 클릭

## 다음에 적용할 개선 아이디어

1. **`contracts` 프로젝트를 CI blocking 으로 승격 검토** — 지금 신호가 안 남는 근본 원인. 다만
   flaky 잔재가 있으면 blocking 승격 시 main 이 red 됨 → 안정화 확인 후 전환
2. **커밋 메시지에 요약 카운트 금지** — 반드시 실측 raw 출력 그대로 인용. ym-verify 지적
   재발 방지 (#199 → #200 사이클에서 확인된 패턴)
3. **`--repeat-each=10` 을 표준 검증 절차에 포함** — flaky spec 판단 시 최소 반복 실행

## 오버레이 감사 결과 (2026-09-03)

`position:absolute;inset:0` backdrop + 하단 panel 조합은 CSS 상 4곳 존재:
- `.program-calendar-mobile-sheet-backdrop` (오늘 hotfix 완료)
- `.filter-mobile-sheet-backdrop` — 프로그램 목록 필터 시트
- `.site-drawer-backdrop` — 사이트 드로어
- `.postcode-modal` — 다음 우편번호 검색 모달

E2E 에서 **클릭 대상은 오직 `program-calendar-mobile-sheet-backdrop` 1건이며 이미 fix 완료**.
나머지 3건은 현재 spec 에서 클릭되지 않음. 향후 새 spec 추가 시 아래 규칙 준수:

**규칙**: `position:absolute;inset:0` backdrop 을 click 할 때는 반드시
`click({ position: { x: 10, y: 10 } })` 로 panel 미점유 영역을 명시. `.click()` (center)
사용 시 하단 panel 에 intercept 될 수 있음.

## 관련 PR

- #198 F0f UNVERIFIED U-1/U-2 청산 (N3 회귀 + 정책 명제)
- #199 A형 fix (IIFE 리스너 대기)
- #200 B형 fix (cleanup endpoint) + backdrop hotfix
- #201 apply.spec.ts waitUntil 통일
- #202 spec 큐 감사 정리
- #203 세션 회고 · STATE 미러 · 오버레이 감사 결과 반영
