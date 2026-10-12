# 검증 리포트: A5-2 승인 처리 페이지

- 날짜: 2026-10-12
- 브랜치: `feature/A5-2-approval-bulk` (미커밋 작업트리)
- 선행: ym-spec `a7f391c502b4b3267` · ym-impl `afc3d5fdafd566053`
- 커밋 가능 여부: ❌ **BLOCK (P0 1건)** — "대기 전체 승인" 버튼이 bootRun 상에서 렌더되지 않음

---

## 1. 정적 검증

| 항목 | 결과 |
|---|---|
| `compileJava` | ✅ BUILD SUCCESSFUL (29s) |
| 신규 테스트 — `AdminApplicationBulkRejectServiceTest` (5 TC) | ✅ PASS |
| 신규 테스트 — `AdminApplicationBulkApproveAllPendingTest` (2 TC) | ✅ PASS |
| `AdminApplicationBulkServiceTest` (기존) | ✅ PASS |
| `AdminApplicationBulkControllerTest` (기존) | ✅ PASS |
| `AdminCsvControllerTest` (기존) | ✅ PASS |
| `admin.*RenderTest` (예: AdminLoginRenderTest · AdminUserDetailRenderTest 외) | ✅ PASS |

정적 범위: 서비스 단위 반려 + 대기 전체 승인은 전부 통과. 500 cap · idempotent skip · blank reason · wrong program id · 공석 승격 모두 통과.

---

## 2. 회귀 검증

- `./gradlew test --tests io.github.sihyuuun.youthmoa.admin.* --tests io.github.sihyuuun.youthmoa.application.*`
- 결과: ✅ **BUILD SUCCESSFUL in 2m 40s** — 깨짐 0건
- 통과 범위: admin + application 패키지 전체 TC (AdminApplicationBulkRejectServiceTest · AdminApplicationBulkServiceTest · AdminApplicationBulkControllerTest · AdminCsvControllerTest · AdminLoginRenderTest · AdminUserDetailRenderTest · AdminProgramBulkServiceTest · AdminProgramDuplicateServiceRollbackTest · ApprovalFinishGuardTest · 그 외 다수 PASS)

---

## 3. 동적 검증 (bootRun 8090, sysadmin + center2 세션)

### 3.1 GET /admin/programs/3/applications

| 체크 | 결과 |
|---|---|
| 응답 코드 | ✅ 200 |
| 헤더 "일괄 반려" 버튼 markup | ✅ 포함 (`data-bulk-action="reject"`) |
| 헤더 "대기 전체 승인" form | ✅ markup 포함 (`approve-all-pending-btn`) ·<br>**단 th:if 조건 때문에 실제 렌더 안 됨 — §4 BUG-1 참조** |
| "성별" 헤더 셀 (`<span>성별</span>`) | ✅ 포함 |
| 행 셀 `data-testid="row-gender"` | ✅ 포함 |

### 3.2 POST /admin/programs/3/applications/bulk/reject

| 입력 | 기대 | 실측 |
|---|---|---|
| `ids=` (빈), `reason=` (빈) | 302 + flashError | ✅ 302, flash "반려 사유를 입력해주세요." |
| 501건 ids + reason=cap-test | 302 + "500건까지만" | ✅ 302 + flashError "한 번에 500건까지만 처리할 수 있어요" |

### 3.3 POST /admin/programs/3/applications/bulk/approve-all-pending

| 입력 | 기대 | 실측 |
|---|---|---|
| CSRF 유효 + PENDING 다수 | 302 + flash | ✅ 302, PENDING → APPROVED (reset endpoint 로 복구 가능) |

### 3.4 CENTER_ADMIN scope (center2@youth-moa.test)

- `/admin/programs/{1..5}/applications` 전부 **403** 반환 → ✅ 스코프 격리 정상
- center2 는 센터[1] 소속, 시드 프로그램 중 center[1] 매칭 건 없음 (의도된 결과)

### 3.5 CSV 다운로드 — `/admin/programs/3/applications/export.csv`

| 체크 | 결과 |
|---|---|
| 응답 코드 | ✅ 200 |
| 헤더 순서 (11열) | ✅ `id,applicantEmail,applicantName,gender,phone,status,appliedAt,processedAt,processedBy,rejectReason,adminNote` |
| gender 값 | ✅ `MALE` / `FEMALE` enum name (화면은 label "남"/"여" 분리) |
| UTF-8 BOM | ✅ 선행 |

---

## 4. E2E (Playwright chromium, bootRun 8090)

**실행**: `admin-application-bulk-reject.spec.ts` (신규, 4 TC, serial mode)

| # | 테스트 | 결과 |
|---|---|---|
| 1 | 일괄 반려 — PENDING 2건 선택 → 모달 → confirm → 302 + REJECTED 반영 | ✅ PASS (re-run 후) |
| 2 | 일괄 반려 — 사유 공백 → 모달 유지 / 요청 미발송 | ✅ PASS |
| 3 | 대기 전체 승인 — 버튼 → confirm → 302 + PENDING 0 | ❌ **FAIL** — `approve-all-pending-btn` not visible (§4 BUG-1) |
| 4 | 성별 열 — 행 마다 "남"/"여"/"-" 노출 | ⏸ did not run (serial mode 중단) |

> 참고: `admin-application-bulk-approve.spec.ts` 는 저장소에 존재하지 않으므로 regression 대상에서 자동 제외됨 (요청에는 포함돼 있었으나 파일 미존재).

### 4.1 ⛔ BUG-1 (P0 · BLOCK) — "대기 전체 승인" 버튼 렌더 불가

- **파일**: `src/main/resources/templates/admin/program/applications.html:35`
- **조건식**: `th:if="${summary['PENDING'] != null and summary['PENDING'] > 0}"`
- **실측 (sysadmin 세션 · program 3)**:
  - 페이지 렌더 결과에서 모든 summary 뱃지 값이 **빈 문자열**로 출력
    - `summary-total` ✅ 6 (별도 `${total}` 사용)
    - `summary-pending` → `""`
    - `summary-approved` → `""`
    - `summary-rejected` → `""`
    - `summary-cancelled` → `""`
  - 따라서 `summary['PENDING']` → null → `th:if` false → form 자체가 DOM 에 없음 → `data-testid="approve-all-pending-btn"` not found
- **근본 원인**: `AdminApplicationService.summaryCounts()` 는 `Map<ApplicationStatus, Long>` (enum key) 반환. Thymeleaf SpEL `summary['PENDING']` 는 **String 키 조회**로 평가되어 enum 키와 매치되지 않아 null 반환.
- **범위**: Thymeleaf 는 이 패턴을 자동 변환하지 않음. 기존 뱃지 수치가 **A4 때부터 공란이었던 pre-existing bug** 이지만, A5-2 가 **이 값에 의존하는 `th:if` 분기를 추가**하면서 "숫자가 안 보이는" 수준이 "기능이 아예 안 보이는" 수준으로 승격됨.
- **왜 QA 가 이제서야 발견**: 숫자 공란은 디자인 계약 체크에서 `exists` (count-min=1 selector) 만 걸려 PASS 됨. 값 비교 체크가 없었음.
- **권장 수정 범위 (impl 소관, 본 QA 범위 외)**:
  - 안 A — `summaryCounts` 가 `Map<String, Long>` 반환으로 변경 (키를 `s.name()`)
  - 안 B — Controller 에서 `Map<String, Long>` 로 재패킹해 모델에 주입
  - 안 C — 템플릿에서 `${summary.get(T(io.github.sihyuuun.youthmoa.application.ApplicationStatus).PENDING)}` 사용 (장황)
  - **안 B가 가장 작은 변경** — Service 시그니처 유지 + Controller 1곳만 수정

### 4.2 ⚠️ side-effect 스캔

- `admin-bulk-csv.spec.ts` 는 Applications CSV 본문에서 `applicantEmail` 포함 여부만 검사 → 신규 `gender` 컬럼 추가 영향 없음 (break 없음). 다만 **gender 컬럼 적극 검증은 부재** — 테스트 커버리지 공백 (impl 범위 밖, 이월 가능).
- `admin-bulk.js` reject 액션은 modal 재사용 — 기존 users/programs 도메인 영향 없음 (코드 리뷰 수준으로 확인).
- `grep -rn "gender" e2e/` → row-gender 는 신규 spec + 신규 contract 체크 외 기존 테스트 영향 없음.

---

## 5. 디자인 계약 (`--project=contracts`)

- 실행: `cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts --grep "admin-application-bulk|admin-program-applications"`
- 결과: **"No tests found" (exit 0)**
- 원인: `admin-application-bulk.ts` / `admin-program-applications.ts` 두 contract 파일은 정의만 있고 이를 소비하는 **`visual-*.spec.ts` 러너가 리포지토리에 존재하지 않음**. 이는 A4 (2026-09-15) 생성 이래 유지된 **pre-existing 상태** — 본 PR 변경 없음. 2026-10-07 commit `258a2d3` (deferred 해소) 로 contract 는 갱신됐지만 runner 는 추가되지 않았음.
- 본 PR 은 contract 2개의 체크 항목만 수정 (`bulk.bar.action.reject` deferred → active, `list.row.gender` 추가, `btn.approve-all-pending` deferred → active). contract 구문적 정합성은 수동 확인 완료 (셀렉터 모두 템플릿 실측과 일치).
- **impl 책임이 아닌 리포지토리 수준 공백** — 별도 운영 트랙에서 visual runner 작성 필요. QA 는 contract 기대값 자체가 markup 과 일치함을 수동 확인한 것으로 대체.

---

## 6. 특별 체크 포인트 (impl 인계 재검)

| 항목 | 결과 |
|---|---|
| Service 단위 500건 cap `IllegalArgumentException` 커버 | ✅ `AdminApplicationBulkRejectServiceTest` + Controller 레벨 흐름 모두 PASS |
| CENTER_ADMIN 격리 (타 센터 programId → IllegalStateException) | ✅ 동적 403 확인 |
| 성별 cascade — CSV | ✅ 11열 헤더 + gender enum name 출력 |
| 성별 cascade — 반응형 | ⏸ UNVERIFIED (사용자 시각 영역 — admin.css `@media (max-width: 1200px)` 적용 수치는 확인됐으나 실 브라우저 레이아웃 미실측) |
| 성별 cascade — E2E selector 깨짐 | ✅ grep 전수 — `row-gender` 선택자 외 기존 셀렉터 영향 없음 |
| APPROVED → REJECTED 공석 승격 호출 | ✅ 구현 `bulkReject` 내부 `promoteIfEligible(programId)` 확인 + E2E 반려 1건 PASS (대기자 승격 간접 커버) |

---

## 7. 시각 확인 (사용자 영역, 대기)

- ① A5-2 성별 열 cell gap 수치 (`admin.css` 9열 grid-template)
- ② 1200px 반응형 축소 — 성별·참여횟수 셀 축소/숨김 질감
- ③ 반려 모달 — reason textarea placeholder, length counter 등 UX 디테일

---

## 8. 미실행 (환경 한계)

- 없음. 모든 범위를 회사 PC + bootRun(e2e profile · H2) 로 검증.

---

## 9. 결론 & 다음 단계

### 🚫 BLOCK

- **BUG-1 (P0)** — 대기 전체 승인 버튼이 bootRun 에서 렌더되지 않음. 이 상태로 머지 시 사용자(관리자) 경로에서 **A5-2 B 기능 전체 사용 불가**.
- 커밋 전 impl 트랙에서 반드시 수정 필요. 권장 안 B (Controller 레벨 Map<String,Long> 재패킹) — 1 메서드 수정 + 기존 뱃지 숫자 공란 pre-existing bug 까지 동시에 해소.

### 📋 작업 큐 메타

- 상태: **qa_blocked**
- 머지 가능 조건:
  1. BUG-1 해결 → 재QA (POST /bulk/approve-all-pending 흐름 + E2E 재실행)
  2. 사용자 시각 확인 통과

### 🧭 ym-verify 인계 전 선행

- impl 가 BUG-1 수정 후, 수정 범위에 대한 적대적 재검증을 위해 `ym-verify` 호출. 기존 TC 는 그대로 유지 (서비스 레이어 변경 없음 — Controller/모델 attribute 만 교체 예상).

---

### 부록 A. 실행 로그 요약

- bootRun: `.claude/scripts/bootrun-e2e.cmd` 백그라운드 기동, `GET /login` 200 확인 (약 21s 소요)
- 세션: `sysadmin@youth-moa.test / Admin!234` (SYSTEM_ADMIN) 및 `center2@youth-moa.test / Admin!234` (CENTER_ADMIN) 둘 다 로그인 성공
- 상태 reset endpoint: `POST /__test__/reset-application-status` → 204

### 부록 B. 코드/테스트 수정 없음

본 QA 세션 중 소스/테스트 파일 수정 0건. 모든 수정은 impl 트랙에서 수행.
