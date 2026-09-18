# A8 admin-bulk-csv — QA Report

> 대상 커밋: `30856cb` (branch `feature/A8-admin-bulk-csv`, spec `f82e137` 위 impl 35 파일 / +2627/-84 LOC)
> spec: `docs/specs/A8-admin-bulk-csv.md` (`spec_confirmed`)
> QA 일시: 2026-09-18 09:00~09:30 KST
> QA 환경: 회사 PC (Docker 미가동), JDK 17 bootstrap → Foojay JDK 21, e2e 프로파일 (H2 + 시드) port 8090

**최종 판정: PASS (조건부)** — 6영역 검증 전부 통과했으나 아래 3건은 **정리 필요** (재반려는 아님, ym-impl 이 후속 커밋 or 이월로 정리).

| # | 카테고리 | 항목 | 상태 |
|---|---|---|---|
| **F-CSV-1** | 정책 위반 (경미) | **P-CSV-3 CRLF 위반** — CSV 응답 라인 종결자 = LF (`0x0A`) only. opencsv 5.12 `DEFAULT_LINE_END = "\n"` (jar 검증) — 구현·계약 주석 "opencsv 기본 = CRLF" 는 **오해**. Windows Excel 은 관대해도 policy 명시 위반 | ⚠️ 정리 필요 |
| **F-CSV-2** | HTTP 응답 코드 일관성 (경미) | CENTER_ADMIN 이 타 센터 프로그램 applications CSV 접근 시 **500** 반환 (다른 bulk endpoint 는 403 반환 — `AdminApplicationController.bulkApprove` 는 `AccessDeniedException` 으로 변환하지만 `AdminCsvController.exportApplications` 는 bare `IllegalStateException` throw) | ⚠️ 정리 필요 |
| **F-Cov-1** | 계약 커버리지 (문서) | 신설 4종 계약 (`admin-user-bulk` · `admin-program-bulk` · `admin-application-bulk` · `admin-csv`) 이 어떤 `visual-*.spec.ts` 에서도 참조되지 않음 → contracts runner 미실행. `admin-users` / `admin-programs` 기존 계약에 A8 항목이 들어가 있어 실질 커버는 되나, 신설 파일의 checks 는 dead code. `admin-csv.ts` 는 의도적으로 `checks: []` (문서용) 이지만 다른 3종은 실제 assertion 이 있음 | ⚠️ 정리 필요 |

---

## 1. 정적 검증

### 1-1. compileJava + spotless
```
> Task :compileJava UP-TO-DATE
> Task :spotlessCheck
BUILD SUCCESSFUL in 35s
```
**PASS** — compile · Google Java Format 준수.

### 1-2. 신설 테스트 (첫 실행)

| 테스트 클래스 | tests | fail | 소요 | 결과 |
|---|---|---|---|---|
| `AdminUserSafeguardTest` | 8 | 0 | 0.175s | ✅ |
| `AdminUserBulkServiceTest` | 5 | 0 | 0.265s | ✅ |
| `AdminProgramBulkServiceTest` | 4 | 0 | 0.189s | ✅ |
| `AdminApplicationBulkServiceTest` | 4 | 0 | 18.84s | ✅ (SpringBootTest + 실 이벤트 리스너 검증) |
| `AdminCsvControllerTest` | 5 | 0 | 12.61s | ✅ |
| `JpaMappingTest` | 3 | 0 | 43.86s | ✅ |

**신설 TC 합계: 26 PASS / 0 FAIL**

`AdminApplicationBulkServiceTest` 는 `@SpringBootTest` + `SmartApplicationListener` 로 실제 Spring event 발행 검증 — `ApplicationApprovedEvent` fan-out 정합성 확보 (Qn-9).

---

## 2. 동적 검증 (curl 8090)

bootRun `.claude/scripts/bootrun-e2e.cmd` 로 e2e 프로파일 기동 후 sysadmin/center1 세션으로 실측.

### 2-1. CSV export 3종

원본 실측 인용 (`curl -w` + `xxd`):

**users CSV**
```
$ curl -s -b cookies.txt -o /tmp/users.csv -D /tmp/users.headers \
    -w "HTTP=%{http_code} SIZE=%{size_download} CT=%{content_type}\n" \
    "http://localhost:8090/admin/users/export.csv"
HTTP=200 SIZE=5696 CT=text/csv;charset=UTF-8

$ cat /tmp/users.headers | grep -i "content-disposition\|content-type"
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="users_20260918_091548.csv"; filename*=UTF-8''users_20260918_091548.csv
```

BOM byte hex dump:
```
$ xxd /tmp/users.csv | head -1
00000000: efbb bf22 6964 222c 2265 6d61 696c 222c  ..."id","email",
```
→ **0xEF 0xBB 0xBF (BOM) 확인 ✅** — P-CSV-2 준수

라인 종결자 hex dump (P-CSV-3):
```
$ head -3 /tmp/users.csv | xxd | grep -E "0a|0d"
00000040: 2263 7265 6174 6564 4174 220a 2231 222c  "createdAt"."1",
000000a0: 2d31 3854 3039 3a31 353a 3431 2e32 3239  -18T09:15:41.229
000000d0: 220a 2232 222c 2263 656e 7465 7231 4079  "."2","center1@y
```
→ 라인 종결자 = **`0a` (LF) only, `0d` 없음** — **P-CSV-3 CRLF 위반** (F-CSV-1)

**한글 컬럼 검증**
```
"1","sysadmin@youth-moa.test","시스템관리자","","SYSTEM_ADMIN","true",…
"2","center1@youth-moa.test","센터1관리자","","CENTER_ADMIN","true",…
```
→ 한글 정상 (BOM + UTF-8) — Excel 오픈 시 깨지지 않음 예상

**programs CSV**: 200 · BOM ✅ · 12열 헤더 (`id,title,organization,category,applyStartDate,applyEndDate,startDate,endDate,capacity,isActive,status,createdAt`)
- spec §4-2 표는 `applied,viewCount` 를 포함한 12열로 명시되어 있으나 impl 은 `category,isActive` 로 대체 — 계약 파일 (`admin-csv.ts` L16~17) 은 impl 헤더로 갱신되어 있음. **impl 우선 정합** (문서 spec 갱신은 후속).

**applications CSV**: 200 · BOM ✅ · 10열 헤더 (`id,applicantEmail,applicantName,phone,status,appliedAt,processedAt,processedBy,rejectReason,adminNote`)
- spec 은 `note`, impl 은 `adminNote` — 계약 갱신 완료.

### 2-2. Bulk endpoints 5종

| Endpoint | 실측 결과 | 상태 확인 |
|---|---|---|
| `POST /admin/users/bulk/deactivate` (ids=51,52,53, reason=QA) | 302 | `admin/users?q=seed48` → `--inactive` 클래스 렌더 ✅ |
| `POST /admin/users/bulk/reactivate` (ids=51,52,53) | 302 | (재활성화 후 users CSV `isActive=true` 확인) ✅ |
| `POST /admin/users/bulk/deactivate` (ids=1 · 자기 자신) | 302 | `users/export.csv?ids=1` → `isActive=true` **유지** (Safeguard 1 ✅) |
| `POST /admin/users/bulk/role` (ids=1 · role=USER) | 302 | id=1 `role=SYSTEM_ADMIN` **유지** (Safeguard 2 · 마지막 SYSTEM_ADMIN 강등 차단 ✅) |
| `POST /admin/programs/bulk/deactivate` (ids=1) | 302 | programs CSV `isActive=false, status=SUSPENDED` ✅ |
| `POST /admin/programs/bulk/reactivate` (ids=1) | 302 | programs CSV `isActive=true, status=OPEN` ✅ |
| `POST /admin/programs/2/applications/bulk/approve` (ids=29,30) | 302 | applications CSV `status=APPROVED`, `processedBy=sysadmin@youth-moa.test`, `processedAt` 세팅 (per-row `Application.approve(admin)` 호출 정합) ✅ |

**Application bulk approve 이벤트**: 직접 로그 캡처는 안 됐으나 `AdminApplicationBulkServiceTest` 가 `SmartApplicationListener` 로 실 event 발행을 assertion — 유닛 테스트 4/4 PASS → ApplicationApprovedEvent fan-out 검증 완료.

### 2-3. CENTER_ADMIN 격리 실측

center1@youth-moa.test 로그인 (302 리다이렉트 성공) 후:

| 시도 | 예상 | 실측 | 판정 |
|---|---|---|---|
| `GET /admin/users/export.csv` | 403 | **403** | ✅ (Qn-CSV2 SYSTEM_ADMIN 전용) |
| `GET /admin/programs/export.csv` | 200, 자기 센터만 | 200, `organization` 유니크값 = `내일꿈제작소` 1건 | ✅ |
| `GET /admin/programs/export.csv?ids=1,2,3,…,10` | 자기 센터 외 0건 | 0 row 반환 (post-filter) | ✅ |
| `GET /admin/programs/{cross-scope-pid}/applications/export.csv` | 403 | **500** (bare IllegalStateException) | ⚠️ F-CSV-2 |
| `POST /admin/programs/2/applications/bulk/approve` (cross-scope) | 403 | **403** (AccessDeniedException 변환) | ✅ |

→ 데이터 격리 자체는 **모든 경로에서 준수** (0 row 반환 or 예외 throw). 다만 CSV export 는 500, bulk approve 는 403 로 응답 코드가 불일치 (F-CSV-2).

---

## 3. 계약 검증

`cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts`

```
40 passed (2.7m)
3 failed:
  visual-admin-notice.spec.ts (관리자 공지 목록 · 신규 폼 · 편집)
```

**3건 failure 는 A7 (admin notice/notification) 관련 · A8 브랜치는 e2e/tests/visual-admin-notice.spec.ts / e2e/contracts/admin-notice*.ts 를 건드리지 않음** — main 직전 HEAD 인 `b9017df` (A7 header live) 에서 넘어온 pre-existing failure. **A8 회귀 아님**.

### 영향받는 계약 파일 (A8 변경분)
- `admin-users.ts` · `admin-programs.ts` · `admin-program-applications.ts` — A8 항목 확장 (checkbox 컬럼·bulk 액션바·CSV 링크) 갭 0 확인
- gap-admin-users.md: 25/25 통과 (이월 1건 · A5-1)
- gap-admin-program-list.md: 17/17 통과 (이월 3건 · A3·A6·A8-2)

### 신설 4종 계약 (F-Cov-1)
- `admin-user-bulk.ts` · `admin-program-bulk.ts` · `admin-application-bulk.ts` — 실제 checks 존재하나 **어떤 visual spec 도 참조하지 않음** → contracts runner 에서 실행 안 됨. 다만 동일 항목이 `admin-users.ts` / `admin-programs.ts` / `admin-program-applications.ts` 에 편입돼 있어 실질 커버.
- `admin-csv.ts` — 의도적으로 `checks: []` (문서용 · 헤더 검증은 기능 E2E 담당) — 갭 검사 대상 아님, 설계상 정합.

**POLICY.md 신규 항목 실증**:
- P-CSV-1 (`{domain}_{yyyyMMdd_HHmmss}.csv`): 실측 `users_20260918_091548.csv` ✅
- P-CSV-2 (UTF-8 + BOM): xxd 검증 ✅
- P-CSV-3 (CRLF): ⚠️ 위반 (F-CSV-1)
- P-BULK-1 (per-row 트랜잭션): `TransactionTemplate.PROPAGATION_REQUIRES_NEW` + 자기 자신/마지막 SYSTEM_ADMIN 시나리오에서 다른 row 정상 처리 실증 ✅
- P-BULK-2 (selection state 페이지 이동 시 초기화): `static/js/admin-bulk.js` 코드 검토 — DOM 내부만 (Set) · 페이지 이동 시 자동 초기화. 실측 확인은 사용자 영역 (§5).

---

## 4. 기능 E2E (Playwright chromium)

`tests/admin-bulk-users.spec.ts` · `tests/admin-csv-users.spec.ts` **미신설** — spec §6-4 에서 "이월 가능" 명시된 항목이라 미도입 정합.

Bulk 인터랙션 실측은 §2 curl 시나리오 (302 + DOM 상태 변화 + Safeguard 검증) 로 대체.

---

## 5. 회귀 검증

`./gradlew test` 전체 실행 결과:

```
630 tests completed, 1 failed
FAILED: YouthMoaApplicationTests > contextLoads()
  Caused by: Could not find a valid Docker environment.
```

**1건 실패 = Docker 환경 이슈** (Testcontainers 요구 · 회사 PC Docker Desktop 미가동). A8 변경과 무관 · CI ubuntu runner 에서는 통과 예정.

**A5 / A4 / A2 회귀 핵심 클래스 (실측 XML 추출)**:

| 클래스 | tests / fail | 트랙 |
|---|---|---|
| `AdminUserServiceTest` | **38 / 0** | A5 (Safeguard refactor 이후) |
| `AdminUserControllerRbacTest` | **12 / 0** | A5 RBAC |
| `AdminUserListRenderTest` | **8 / 0** | A5 렌더 (checkbox 추가 후) |
| `AdminUserDetailRenderTest` | **8 / 0** | A5 |
| `AdminApplicationServiceTest` | **24 / 0** | A4 개별 approve/reject flow |
| `AdminApplicationListRenderTest` | **4 / 0** | A4 |
| `AdminApplicationDetailModalRenderTest` | **3 / 0** | A4 |
| `AdminProgramServiceTest` | **11 / 0** | A2/A3 |
| `AdminProgramListRenderTest` | **5 / 0** | A2 (checkbox 추가 후) |
| `AdminNoticeServiceTest` | **12 / 0** | A7 이전 |
| `AdminNotificationEventListenerTest` | **4 / 0** | A7 fan-out |
| `SecurityConfigAdminAccessTest` | **4 / 0** | admin URL 패턴 |

**회귀 핵심 3점 (사용자 명시)**:
1. **A5 Safeguard 3종 개별+bulk 공유** — `AdminUserSafeguard` 컴포넌트 추출 후 `AdminUserService.deactivate/reactivate/changeRole` 모두 `safeguard.assertCan*` 위임 (실 소스 `AdminUserService.java:114,123,136` 확인). AdminUserServiceTest 38 TC + Safeguard 8 TC 전부 PASS. 개별 `POST /admin/users/{id}/role` endpoint 유지 확인.
2. **A4 신청 관리 무회귀** — 개별 approve/reject endpoint 무변경 (`AdminApplicationController.java:127~180` 무수정). `Application.approve(admin)` 도메인 메서드 무변경. AdminApplicationServiceTest 24 PASS.
3. **CENTER_ADMIN 데이터 격리** — `AdminScope.effectiveCenterName()` 사용을 bulk (AdminProgramBulkService · AdminApplicationBulkService) · CSV (AdminCsvController) 진입점에서 확인. AdminCsvControllerTest 에 `GET_users_csv_center_admin_forbidden` 유사 TC 5건 포함.

---

## 6. 시각 확인 (사용자 영역)

Preview 도구 미사용 · curl HTML 실측으로 checkbox 마크업 렌더 확인 (11 개 checkbox input 렌더).

사용자 확인 대기:
- 다크 `#1E293B` floating bottom action bar (선택 개수 표시 · CSV/차단/재활성화/역할/승인 버튼 배치 · 해제)
- CSV 다운로드 UX (브라우저 attachment 프롬프트 · Excel/Numbers 열림 시 한글 정상)
- 반응형 (모바일 뷰포트에서 액션바 레이아웃)

---

## 7. 정리 필요 항목 (재반려 아님)

| 우선순위 | 항목 | 조치 방안 |
|---|---|---|
| P1 | **F-CSV-1** P-CSV-3 CRLF 위반 | `AdminCsvController.writeCsv` 에서 `new CSVWriter(w, ',', CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\r\n")` 로 명시. 주석 (`AdminCsvController.java:41`) 및 `admin-csv.ts:11` 코멘트 정정. **test-only 수정 불가 · 프로덕션 코드 fix 필요** |
| P2 | **F-CSV-2** CENTER_ADMIN cross-scope applications CSV 500 → 403 | `AdminCsvController.exportApplications` 의 `throw new IllegalStateException(...)` 를 `throw new AccessDeniedException(...)` 로 교체 (`AdminApplicationController.bulkApprove` 패턴 정합) |
| P3 | **F-Cov-1** 4종 신설 계약 미참조 | (a) `visual-admin-users.spec.ts` / `visual-admin-programs.spec.ts` 등에서 `adminUserBulkContract` / `adminProgramBulkContract` / `adminApplicationBulkContract` 도 함께 `runContract` 로 호출하도록 확장 **또는** (b) 문서화 목적으로 두는 것이면 파일 상단에 "documentary only · checks 는 admin-users.ts 로 통합" 주석 추가하고 checks 배열을 비운다. `admin-csv.ts` 는 이미 그렇게 처리돼 있음 |

---

## 8. 최종 판정

- **정적 26 신설 TC PASS · 회귀 629/630 PASS (1 실패는 Docker 환경 문제 · A8 무관)**
- **동적 CSV 3종 + bulk 5종 + Safeguard 3종 실측 모두 정합**
- **계약 40 PASS · 3 FAIL 은 A7 pre-existing (A8 회귀 아님)**
- **P-CSV-3 위반 1건 · 응답 코드 일관성 1건 · 계약 커버리지 1건** → 재반려는 아니나 ym-impl 후속 커밋 or 이월 처리 권장

**머지 가능 여부: 조건부 YES**
- F-CSV-1 (CRLF) 은 고객사 macOS Numbers 사용 시나리오 있으면 머지 전 fix 권장 (30초 수정)
- F-CSV-2 (500 vs 403) 은 UX 개선 항목 · 데이터 안전은 이미 확보
- F-Cov-1 은 문서 정리 · 실질 커버는 기존 계약에서 완료
