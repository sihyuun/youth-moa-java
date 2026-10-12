# 검증 리포트: A5-2 승인 처리 페이지 (2차 재검증)

- 날짜: 2026-10-12
- 브랜치: `feature/A5-2-approval-bulk` (미커밋 작업트리)
- 선행: ym-qa 1차 `ad765998337cbec9a` (BLOCK) · ym-impl 2차 `a767e58fb49c61f72` (BUG-1 fix)
- 커밋 가능 여부: ✅ **PASS** — BUG-1 해소 확인, 블로커 0건

---

## 0. 요약 (1차 → 2차 전환)

| 항목 | 1차 (ad76599) | 2차 (a767e58) |
|---|---|---|
| summary 뱃지 숫자 | ❌ 전부 공란 (`""`) | ✅ 4개 모두 숫자 렌더 |
| `approve-all-pending-btn` DOM | ❌ `th:if` false → 미렌더 | ✅ PENDING>0 시 렌더 |
| PENDING=0 시 버튼 숨김 | n/a (렌더 자체 X) | ✅ `th:if` 로 숨김 |
| E2E `admin-application-bulk-reject.spec.ts` | ❌ 1 FAIL + 2 did-not-run | ✅ 4/4 PASS |
| 전체 판정 | 🚫 BLOCK | ✅ PASS |

**impl 2차 수정 범위**: `AdminApplicationController.java` 1 파일 — `summaryCounts()` 결과(`Map<ApplicationStatus, Long>`)를 Controller 에서 `Map<String, Long>`(key=`status.name()`)로 재패킹. Service 시그니처 미변경 → 기존 테스트 영향 0 (impl 2차 자체 정적 PASS 확인됨).

---

## 1. 동적 검증 (bootRun 8090, e2e 프로파일 · sysadmin 세션)

### 1.1 GET /admin/programs/3/applications (초기 상태: PENDING=6)

| 체크 | 1차 | 2차 |
|---|---|---|
| 응답 코드 | 200 | ✅ 200 |
| `summary-total` | 6 | ✅ 6 |
| `summary-pending` | `""` | ✅ **6** |
| `summary-approved` | `""` | ✅ **0** |
| `summary-rejected` | `""` | ✅ **0** |
| `summary-cancelled` | `""` | ✅ **0** |
| `data-testid="approve-all-pending-btn"` DOM | ❌ not found | ✅ **렌더 확인** |

### 1.2 POST /admin/programs/3/applications/bulk/approve-all-pending

| 체크 | 결과 |
|---|---|
| 응답 코드 | ✅ 302 (redirect) |
| 재조회 후 `summary-pending` | ✅ 0 (6→0 전환) |
| 재조회 후 `summary-approved` | ✅ 6 (0→6 전환) |

### 1.3 PENDING=0 program 시 버튼 숨김 (`th:if` 분기)

- 재조회 HTML 에 `approve-all-pending-btn` 문자열 존재 여부:
  - ✅ **미존재** (`th:if="${summary['PENDING'] != null and summary['PENDING'] > 0}"` false 로 평가 → form 자체가 DOM 에서 제외됨)
- 1차 BUG-1 의 근본 원인(SpEL 문자열 키 조회) 해소 확정

### 1.4 상태 복구

- `POST /__test__/reset-application-status` body `{"programId":3}` → 204
- 재조회 PENDING=6 복원 확인 (E2E 재실행 전제 조건)

---

## 2. E2E 재검증 (Playwright chromium, bootRun 8090)

**실행**: `cd e2e && BASE_URL=http://localhost:8090 npx playwright test tests/admin-application-bulk-reject.spec.ts --project=chromium`

| # | 테스트 | 1차 | 2차 |
|---|---|---|---|
| 1 | 신청 일괄 반려 — PENDING 2건 선택 → 사유 모달 → confirm → 302 + REJECTED 반영 | ✅ (re-run) | ✅ **PASS** |
| 2 | 신청 일괄 반려 — 사유 공백 → alert → 요청 미발송 | ✅ | ✅ **PASS** |
| 3 | 대기 전체 승인 — 버튼 → confirm → 302 + PENDING 0 | ❌ FAIL (BUG-1) | ✅ **PASS** |
| 4 | 성별 열 — 각 신청 행에 성별 셀 노출 (남/여/-) | ⏸ did not run | ✅ **PASS** |

**전체: 4 passed (8.7s)**

---

## 3. 정적 검증

- impl 2차 (`a767e58`) 자체 자가 확인: `./gradlew test --tests io.github.sihyuuun.youthmoa.admin.*` 전수 PASS 보고됨
- ym-qa 2차에서는 재돌림 생략 (지시대로) — Service 시그니처 미변경이라 기존 테스트 영향 0

---

## 4. Side-effect 재스캔

| 체크 | 결과 |
|---|---|
| `AdminApplicationService.summaryCounts()` 시그니처 변경 여부 | ✅ 변경 없음 (`Map<ApplicationStatus, Long>` 그대로) |
| 다른 Controller/Service 가 `summaryCounts()` 호출하는지 | 영향 범위 없음 (Controller 1곳에서만 재패킹) |
| 기존 `AdminApplicationBulkServiceTest`, `AdminApplicationBulkControllerTest`, `AdminApplicationBulkRejectServiceTest`, `AdminApplicationBulkApproveAllPendingTest` | ✅ Service·BulkService 레이어 unchanged → 영향 0 |
| CSV `/admin/programs/3/applications/export.csv` | ✅ 1차에서 PASS 확인, 이번 변경 범위 밖 |
| CENTER_ADMIN 403 격리 | ✅ 1차에서 PASS 확인, 변경 범위 밖 |

---

## 5. Pre-existing 노이즈 (재언급만, 비차단)

- `--project=contracts` 러너 미존재 (`admin-application-bulk.ts` / `admin-program-applications.ts` contract 는 정의만 있고 `visual-*.spec.ts` 소비자 없음). **A4 (2026-09-15) 생성 이래 유지된 리포지토리 수준 공백**. 본 PR 변경 아님. contract 구문적 정합성은 1차 QA 수동 확인 완료. 별도 운영 트랙에서 visual runner 작성 필요.
- `__test__/reset-application-status` 를 POST 로 호출할 때 `programId` body(JSON) 필수 — 운영 QA 시 참고 (본 리포트는 적법한 호출로 복구 성공).

---

## 6. 시각 확인 (사용자 영역, 1차와 동일)

- ① 성별 열 cell gap 수치 (`admin.css` 9열 grid-template)
- ② 1200px 반응형 축소 — 성별·참여횟수 셀 축소/숨김 질감
- ③ 반려 모달 — reason textarea placeholder, length counter 등 UX 디테일

(curl/E2E 로 커버 불가한 영역만 남김)

---

## 7. 결론 & 다음 단계

### ✅ PASS — BLOCK 해소

- 1차 P0 BUG-1 (summary enum/String 키 불일치 → 뱃지 공란 + 버튼 미렌더) **완전 해소**.
- 재패킹 로직은 Controller 1곳에 격리됨 — Service 레이어 안정성 유지.
- E2E 4/4 PASS, 동적 검증 전 체크 PASS, 회귀 영향 없음.

### 📋 작업 큐 메타

- 상태: **qa_passed**
- 머지 가능 조건:
  1. ✅ BUG-1 해결 + 재QA PASS
  2. ⏸ 사용자 시각 확인 (영역 ①~③) 통과

### 🧭 ym-verify 인계 가이드

- 변경 범위가 Controller 1 메서드 (`listApplications`) 재패킹 + 신규 2개 핸들러(`bulkApproveAllPending`, `bulkReject`) 로 제한적
- 적대적 재검증은 다음에 집중 권장:
  1. `bulkApproveAllPending` 의 CENTER_ADMIN 격리 (타 센터 programId → AccessDenied)
  2. `bulkReject` 의 reason length boundary (200 자 경계 ±1)
  3. summary 재패킹이 enum 값 추가(향후) 시 자동 전파되는지 (`for..entrySet()` 이라 OK, 단 `name()` 가변 시 템플릿 하드코딩 리스크)

### 부록 A. 코드/테스트 수정 없음

본 QA 세션 중 소스/테스트 파일 수정 0건. bootRun 재기동 1회 + 상태 reset 2회 외 side-effect 없음.
