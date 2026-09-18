# 작업 명세: A8 — admin 대량 처리 (Bulk actions) + CSV 내보내기

> 상태: **`spec_confirmed`** (2026-09-17 사용자 결정: Qn-P1 미도입 · Qn-App1 Approve bulk 만 편입 · Reject bulk 이월 · 나머지 원안 A)
> read-only: 이 spec 은 확정 전까지 편집 금지
> 브랜치(예정): `feature/A8-admin-bulk-csv`
> 선행: A5 (users, PR #217 계열 완료) · A6 (stats, opencsv 재활용 확인) · A2/A4 (목록 화면 · applications 테이블)
> 후속: **`feature/A8-2-admin-views-polish`** (원 ADMIN-00 §5-A8 = 카드·캘린더 뷰·반응형·스켈레톤 — 이번 티켓에서 **분리**)

---

## 0. 계약 확인 (0단계) · 신설 대상

기존 계약 · 해당 화면:

| 계약 (기존) | 현재 상태 |
|---|---|
| `e2e/contracts/admin-users.ts` + `docs/design-contracts/admin/users.md` | ✅ 존재 (A5 산출). bulk / CSV 항목 없음 → **확장** 대상 |
| `e2e/contracts/admin-programs.ts` + `docs/design-contracts/admin/programs-list.md` | ✅ 존재 (A2 산출). bulk / CSV disabled 처리 → **활성화** 확장 |
| `e2e/contracts/admin-program-applications.ts` + `docs/design-contracts/admin/program-applications.md` | ✅ 존재 (A4 산출). bulk approve/reject 없음 → **확장** 대상 |

신설 대상 계약:

| 신설 | 파일 |
|---|---|
| **admin-user-bulk** | `e2e/contracts/admin-user-bulk.ts` + `docs/design-contracts/admin/user-bulk.md` |
| **admin-program-bulk** | `e2e/contracts/admin-program-bulk.ts` + `docs/design-contracts/admin/program-bulk.md` |
| **admin-application-bulk** | `e2e/contracts/admin-application-bulk.ts` + `docs/design-contracts/admin/application-bulk.md` |
| **admin-csv** | `e2e/contracts/admin-csv.ts` + `docs/design-contracts/admin/csv-export.md` |

세분화 근거 (Qn-6): 도메인별 정책·safeguard·컬럼 셋이 독립적 → 갭 리포트가 항목별로 정확히 나오도록 4종 분리. `admin-csv` 는 파일명 규칙·인코딩·Content-Disposition 등 CSV 공통 정책 전용.

**공통 정책 (`POLICY.md`) 신설 항목 후보** (Qn-Δ 결정 전 초안):
- **P-CSV-1**: CSV 파일명 규칙 = `{도메인}_{yyyyMMdd_HHmmss}.csv` (KST · `_`) — 예: `users_20260917_143025.csv`
- **P-CSV-2**: CSV 인코딩 = UTF-8 + BOM (`﻿`) · Content-Type `text/csv;charset=utf-8`
- **P-CSV-3**: CSV 라인 종결 = CRLF (`\r\n`) — Excel · macOS Numbers 호환 최대
- **P-BULK-1**: bulk action 트랜잭션 = per-row (부분 실패 허용) · 성공/실패 카운트 flash
- **P-BULK-2**: selection state = 페이지 이동 시 초기화 (필터 변경 · 검색 · 페이지네이션)

---

## 1. 디자인 출처 (3자산 병렬 정독)

### prototype 정독 (라인 인용 필수)

| 자산 | 위치 | 근거 |
|---|---|---|
| `docs/00_assets/admin/prototype.tsx` L327~336 | `downloadCSV` 헬퍼 | **BOM (`﻿`) + `text/csv;charset=utf-8`** + `"` escape 규칙 |
| `docs/00_assets/admin/prototype.tsx` L467, L471 | ProgramsScreen/UsersScreen 골격 주석 | "체크박스 일괄 + CSV" |
| `docs/00_assets/admin/prototype.tsx` L482~509 | `<BulkActionBar>` 재사용 컴포넌트 가이드 | **다크 `#1E293B`** · "N건 선택됨" + CSV/삭제/해제 |
| `docs/00_assets/admin/prototype.tsx` L507 | 주의사항 | **"프로그램 status는 신청기간·정원으로 자동 파생 → 일괄 상태변경 기능 없음(CSV/삭제만)"** — 프로그램 bulk publish/unpublish 는 **원안 재검토 필요 (Qn-P1)** |
| `docs/00_assets/admin/prototype.html` L2772 | Toast 다크 스타일 | `#1E293B` + `position:fixed;bottom:28px;left:50%` |
| `docs/00_assets/admin/prototype.html` L4091~4124 | bulk selection state 코드 | `programSelected: []` · `toggleProgramSelectAll` · `bulkExportPrograms` · `bulkDeletePrograms` · `bulkExportUsers` · `bulkDeleteUsers` |
| `docs/00_assets/admin/prototype.html` L4098 | bulk 삭제 확인 모달 | `confirmDialog: {type:'bulkPrograms', count, title:'프로그램 일괄 삭제', message:'선택한 N건의 프로그램을 삭제할까요? 삭제 후 복구할 수 없어요.'}` |
| `docs/00_assets/admin/prototype.html` L935, L1189, L1393, L1617 | 상단 CSV 버튼 | 목록 헤더 우측 (bulk selection 여부와 무관하게 항상 노출) — 실측: **상단 CSV + 하단 다크 액션바 CSV 둘 다 존재** |
| ADMIN-00 §5-A2 L219~220 | 마스터 지시 | "다크 액션바" (N건 선택됨 + CSV + 삭제 + 해제) · CSV 서버 사이드 생성 (`text/csv` + BOM) · **선택 건/전체 모두** |

### A5 · A6 이월 항목 ↔ A8 매핑 (사용자 요구)

| 원 티켓 이월 항목 | A8 매핑 |
|---|---|
| A5 §9 `Bulk deactivate` (deferred: A8-admin-bulk-actions) | **A8 §4 admin-user-bulk 로 흡수** |
| A5 §9 `CSV export` (deferred: A8-admin-bulk-actions) | **A8 §4 admin-csv 로 흡수** |
| A5 §9 `Bulk selection UI 재도입` (deferred: A8) | **A8 §4 · templates/admin/fragments/_bulk-action-bar.html 로 흡수** |
| A5 §9 `관리자 신규 계정 대량 발급 UI` (deferred: A8) | **본 티켓 제외** (여전히 이월 · A5-1 별도 티켓) — 이유: bulk *발급* 은 email 전송·비밀번호 정책까지 얽혀 A7 알림 인프라와 세트 |
| A6 §12 `Excel export` (deferred: A8) | **CSV 만 A8** · Excel(xlsx)은 재이월 (§9) |
| A2 §9 `체크박스 일괄 선택 · 다크 액션바` (deferred: A8) | **A8 §4 admin-program-bulk 로 흡수** |
| A2 §9 `CSV 내보내기` (deferred: A8) | **A8 §4 admin-csv 로 흡수** |

### HANDOFF 및 ADMIN-00 필수 반영 항목 (⚠️ 매핑)

| 항목 | 매핑 위치 |
|---|---|
| BulkActionBar 다크 `#1E293B` | §4-템플릿 fragment |
| CSV: 서버 사이드 생성 + BOM + `text/csv` | §4-엔드포인트 + §7 Qn-Δ |
| 선택 건 / 전체 모두 CSV 대상 | §4 (`selectedIds` 없으면 현재 필터 전체) |
| 프로그램 일괄 상태변경 금지 (tsx L507) | §7 Qn-P1 — publish/unpublish bulk 는 사용자 결정 필요 |
| 확인 모달 문구 톤 존댓말 "…삭제할까요? 삭제 후 복구할 수 없어요." | §4-fragment `_bulk-confirm-dialog.html` |
| 센터 격리 (CENTER_ADMIN AdminScope) | §4 · §5 회귀 방지 |

### 자산 간 갭

| 항목 | prototype.html | prototype.tsx | ADMIN-00 §5 | 채택 |
|---|---|---|---|---|
| CSV 버튼 위치 | 상단 헤더 우측 + 하단 다크바 (둘 다) | 하단만 언급 | "다크 액션바 CSV" | **둘 다 노출** (상단은 필터 결과 전체 export, 하단은 선택 건 export) — Qn-CSV1 |
| 프로그램 bulk 상태변경 | 없음 (삭제만) | "일괄 상태변경 기능 없음(CSV/삭제만)" 명시 | 언급 없음 | **미도입** (prototype 정합) — Qn-P1 |
| Users bulk 삭제 | `bulkDeleteUsers` (하드 삭제 톤) | 명세만 | A5 는 소프트 (deactivate) | **A5 정책 승계 = deactivate/reactivate** (하드 삭제 X — Q10 소프트) |
| Applications bulk approve/reject | prototype 미명시 | 미명시 | 언급 없음 | **본 티켓 신규 편입** (사용자 요구) — Qn-App1 (deviation) |

---

## 1-B. 데이터 모델 gap 표 (필수)

**엔티티 변경 없음** — 기존 도메인 메서드 재사용.

| Bulk action | 도메인 메서드 (기존) | Row 상태 변경 |
|---|---|---|
| Users bulk deactivate | `User.deactivate(admin, reason)` (A5 도입) | `isActive=false` · `deactivated_at` · `deactivated_by` · `deactivation_reason` |
| Users bulk reactivate | `User.reactivate()` (A5 도입) | `isActive=true` · reset audit |
| Users bulk role | `AdminUserService.changeRole(...)` (A5 도입, safeguard 3종) | `role` UPDATE |
| Programs bulk publish/unpublish | **N/A** (prototype 명시적 금지) — Qn-P1 | — |
| Programs bulk delete (soft) | `Program.softDelete()` (A2/A3 도입) | `deleted_at` |
| Applications bulk approve | `Application.approve(admin)` (A4 도입) | `status=APPROVED` · ApplicationEvent 발행 |
| Applications bulk reject | `Application.reject(admin, reason)` (A4 도입) | `status=REJECTED` · reason · ApplicationEvent 발행 |

**V19 마이그레이션 불필요** (Qn-Δ · A5 §12 결정 승계 · A6 §14 승계).

CSV export 는 read-only — DB 변경 없음.

---

## 1-C. 데이터 소비 지점

| 소비 지점 | prototype 참조 | 이번 티켓 갱신 여부 |
|---|---|---|
| `/admin/users` 목록 | tsx L471 · html L4111~4124 | ✅ checkbox 컬럼 + bulk action bar + 상단 CSV |
| `/admin/programs` 목록 | tsx L467 · html L4091~4109 | ✅ checkbox 컬럼 + bulk action bar + 상단 CSV |
| `/admin/programs/{id}/applications` | html L1393, L1617 | ✅ checkbox 컬럼 + bulk action bar + 상단 CSV (신규 — prototype 는 CSV 만 있음) |
| `/admin/programs/{id}` (상세) | — | 무영향 (개별 flow 유지) |
| `/admin/users/{id}` (상세) | — | 무영향 |

---

## 2. RBAC · 스코프

| 액션 | 필요 권한 | 스코프 필터 |
|---|---|---|
| Users bulk deactivate/reactivate | `SYSTEM_ADMIN` (A5 정합 — 컨트롤러 `@PreAuthorize` 이중 방어) | 전체 |
| Users bulk role 변경 | `SYSTEM_ADMIN` 전용 | 전체 |
| Users CSV export | `SYSTEM_ADMIN` (사용자 개인정보 포함) — Qn-CSV2 | 전체 |
| Programs bulk delete (soft) | `SYSTEM_ADMIN` + `CENTER_ADMIN` (본인 센터 프로그램만) | `AdminScope.effectiveCenterName()` 필터 강제 |
| Programs CSV export | `SYSTEM_ADMIN` + `CENTER_ADMIN` | 동 |
| Applications bulk approve/reject | `SYSTEM_ADMIN` + `CENTER_ADMIN` (본인 센터 프로그램만) | `program.center = AdminScope` |
| Applications CSV export | `SYSTEM_ADMIN` + `CENTER_ADMIN` | 동 |

**CENTER_ADMIN safeguard**: 요청 `selectedIds` 에 타 센터 row 가 섞이면 → 전체 요청 400 반환 (403 아님 — 요청 자체가 부정) · flash "다른 센터의 데이터가 포함되어 처리할 수 없어요."

---

## 3. 엔티티·마이그레이션

**V19 마이그레이션 불필요** — 기존 컬럼·이력 재사용.

의존성 확인:
- `com.opencsv:opencsv:5.12.0` (build.gradle.kts L34) — 이미 존재 (F0h-real-coords CSV **읽기** 용도) · **쓰기 (`CSVWriter`) 신규 사용** (Qn-Δ Writer 옵션)

---

## 4. 화면 · 라우팅

### 4-1. Bulk endpoint 신설 (`POST` · CSRF token 필수)

| Endpoint | 요청 파라미터 | 도메인 메서드 | Safeguard |
|---|---|---|---|
| `POST /admin/users/bulk/deactivate` | `ids[]` · `reason` (필수 · `@NotBlank`) | `AdminUserService.bulkDeactivate` | (1) 자기 자신 X · (2) 마지막 SYSTEM_ADMIN X — per-row `Application.approve` 패턴처럼 개별 검사 · **A5 Safeguard 3종 재적용** (Qn-8) |
| `POST /admin/users/bulk/reactivate` | `ids[]` | `AdminUserService.bulkReactivate` | (1) 자기 자신 X |
| `POST /admin/users/bulk/role` | `ids[]` · `role` (`SYSTEM_ADMIN` \| `CENTER_ADMIN` \| `USER`) | `AdminUserService.bulkChangeRole` | (1) 자기 자신 X · (2) 마지막 SYSTEM_ADMIN X · (3) SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 |
| `POST /admin/programs/bulk/deactivate` (= soft delete) | `ids[]` | `AdminProgramService.bulkSoftDelete` | AdminScope 센터 격리 |
| `POST /admin/programs/{id}/applications/bulk/approve` | `ids[]` | `AdminApplicationService.bulkApprove` | AdminScope · 정원 초과 시 per-row skip (Qn-App2) |
| `POST /admin/programs/{id}/applications/bulk/reject` | `ids[]` · `rejectReason` (필수 · `@NotBlank`) | `AdminApplicationService.bulkReject` | AdminScope |

**공통 응답**: 302 redirect → 원래 목록 경로 + flash `bulkResult = { total, success, failed, failedReasons: [{id, reason}, ...] }`.

**per-row 트랜잭션** (Qn-1 = 권장 per-row): 각 row 별 `@Transactional` 신규 · 실패 시 전체 롤백 아님. 실패 사유 flash 에 최대 5건까지 표시.

### 4-2. CSV endpoint 신설 (`GET` · 세션 인증)

| Endpoint | 필터 파라미터 | 컬럼 (Qn-2) |
|---|---|---|
| `GET /admin/users/export.csv?q=&role=&page=<무시>&ids=` | 목록과 동일 (page 는 무시, 전체 export) | `id, email, name, phone, role, isActive, lastAccessAt, createdAt` (8열) |
| `GET /admin/programs/export.csv?status=&q=&ids=` | 목록과 동일 | `id, title, organization, applyStartDate, applyEndDate, startDate, endDate, capacity, applied, viewCount, status, createdAt` (12열) |
| `GET /admin/programs/{id}/applications/export.csv?status=&ids=` | 목록과 동일 | `id, applicantEmail, applicantName, phone, status, appliedAt, processedAt, processedBy, rejectReason, note` (10열) |

**Content-Disposition** (Qn-CSV3 = attachment 권장): `attachment; filename="users_20260917_143025.csv"` (RFC 5987 `filename*=UTF-8''...` 병용 · Excel 한글 파일명 호환).

**`ids` 파라미터 처리** (Qn-3):
- `ids` 없음 (또는 빈 값) → 현재 필터 결과 **전체** export (페이지네이션 무시)
- `ids=1,2,3` 존재 → 해당 ids 만 export (필터 무시 · **명시적 선택**)
- 페이지 전체 선택 = `toggleProgramSelectAll` 로 현재 페이지 ids 만 담김 (prototype html L4105~4108 정합)

### 4-3. 템플릿 · fragment

**신설 fragment** (`templates/admin/fragments/`):

| 파일 | 용도 |
|---|---|
| `_bulk-action-bar.html` | 다크 `#1E293B` floating bottom bar — `${count}건 선택됨` + 슬롯 (각 화면이 CTA 주입: 사용자=차단·재활성화·역할·CSV, 프로그램=삭제·CSV, 신청=승인·반려·CSV) + `해제` |
| `_bulk-confirm-dialog.html` | 확인 모달 (prototype html L4098 톤) — `${type}` 별 title / message / confirmLabel 파라미터 |
| `_csv-download-button.html` | 상단 헤더 CSV 버튼 (`📥 CSV 내보내기`) — `${href}` 파라미터 |

**갱신 템플릿**:

| 파일 | 갱신 내용 |
|---|---|
| `templates/admin/user/list.html` | checkbox 컬럼 (첫 열) 추가 + 헤더 전체선택 + `_bulk-action-bar` 인클루드 + 상단 CSV 버튼 |
| `templates/admin/program/list.html` | 동상 (A2 spec §4 에서 disabled 로 남긴 CSV·checkbox 항목 활성화) |
| `templates/admin/program/applications.html` | 동상 (A4 spec §4-1 기존 테이블에 checkbox 컬럼 추가) |

**JS** (`static/js/admin-bulk.js` 신설):
- selection state 는 **DOM 내부만** (Set<Long>) — 페이지 이동/필터 변경 시 자동 초기화 (Qn-B)
- 전체선택 헤더 checkbox: 현재 페이지 rows 만 대상 (prototype toggleProgramSelectAll 정합)
- form submit 시 `<input type="hidden" name="ids" value="1,2,3">` 로 직렬화
- CSV 상단 버튼: 현재 페이지 URL 쿼리를 그대로 `.csv` 경로에 붙여 이동. 선택 있으면 `?ids=...` 추가 물음 없이 선택 우선

### 4-4. 라우팅 summary

| Method | Path |
|---|---|
| GET | `/admin/users/export.csv` |
| POST | `/admin/users/bulk/deactivate` |
| POST | `/admin/users/bulk/reactivate` |
| POST | `/admin/users/bulk/role` |
| GET | `/admin/programs/export.csv` |
| POST | `/admin/programs/bulk/deactivate` |
| GET | `/admin/programs/{id}/applications/export.csv` |
| POST | `/admin/programs/{id}/applications/bulk/approve` |
| POST | `/admin/programs/{id}/applications/bulk/reject` |
| POST | `/e2e/reset-bulk` (Qn-5 · e2e 프로파일 전용) |

---

## 5. 회귀 방지 (최우선)

| # | 회귀 리스크 | 방어 |
|---|---|---|
| R1 | **A5 개별 flow 무회귀** — `/admin/users/{id}/deactivate` · `/reactivate` · `/role` · `/admin-note` | 기존 endpoint 유지 · 신규 `/bulk/*` 는 별개 라우팅. AdminUserService 개별 메서드 그대로 두고 `bulk*` 메서드 신설 (per-row 안에서 기존 메서드 호출) |
| R2 | **A5 Safeguard 3종 재적용** — 자기 자신 · 마지막 SYSTEM_ADMIN · 승격 SYSTEM_ADMIN 만 | Qn-8 A안 = **Safeguard 로직을 별도 컴포넌트로 추출** (`AdminUserSafeguard`) → 개별·bulk 모두 재사용. bulk 는 per-row 호출 |
| R3 | **A4 Application flow 무회귀** — 상태 변경 시 `ApplicationEvent` 3종 발행 유지 (사용자 알림 연동) | bulk approve/reject 도 per-row `Application.approve/reject` 호출 → 이벤트 자동 발행 (별도 처리 없음) |
| R4 | **A2 프로그램 목록 무회귀** — 필터·검색·페이지네이션 · 카테고리·등록일 컬럼 | 컬럼 개수 유지 · checkbox 는 **첫 열 추가** (기존 컬럼 위치 불변) |
| R5 | **CSRF token** — 모든 `POST /bulk/*` 는 hidden `_csrf` 필수 | 기존 CSRF 활성화 승계 · fragment 에 `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />` |
| R6 | **CENTER_ADMIN 데이터 격리** — bulk 요청 `ids[]` 에 타 센터 row 포함 시 부정 방지 | Service 진입점에서 **모든 row 사전 조회 + AdminScope 검사** → 하나라도 걸리면 전체 400 · flash 오류 |
| R7 | **CSV 다운로드가 세션·인증 flow 영향 없음** | 세션 쿠키 그대로 · attachment 응답 · redirect 없음 |
| R8 | **opencsv 5.12.0 기존 사용 (CenterCsvLoader) 무회귀** — Reader 경로 변경 없음 | 신규는 **`CSVWriter` (Qn-Δ)** 만 사용. Reader 는 손대지 않음 |
| R9 | **CSV 대용량 응답 성능** — 초기 시드 규모(수백 건)에서 동기 stream OK. 후속 규모 증가 시 재검토 (Qn-D deferred) | 동기 방식 채택 · 응답 헤더 `Content-Length` 없이 chunked · 로그에 `csvExportSize` 항목 추가 |

---

## 6. 테스트

### 6-1. 정적

| 테스트 | 대상 |
|---|---|
| `AdminUserBulkServiceTest` (Slice · `@DataJpaTest` + `@Import(AdminUserService.class)`) | bulk deactivate 성공 · reason 누락 → 400 · 자기 자신 포함 시 skip · 마지막 SYSTEM_ADMIN 포함 시 skip · reason 은 per-row 로 동일 저장 |
| `AdminProgramBulkServiceTest` | bulk soft delete · CENTER_ADMIN 스코프 필터 · 타 센터 row 포함 시 전체 400 |
| `AdminApplicationBulkServiceTest` | bulk approve · reject · reason 필수 · 정원 초과 시 skip · ApplicationEvent 발행 확인 (`@RecordApplicationEvents`) |
| `CsvExporterTest` (도메인별 3개) | 컬럼 순서 · escape · null 처리 · BOM prefix · CRLF · 한글 인코딩 왕복 (`new String(bytes, UTF_8)` 로 검증) |
| `AdminBulkRenderTest` (Thymeleaf 실 렌더) | list.html × 3 — checkbox 컬럼 + fragment 렌더 + `_bulk-action-bar` 초기 hidden · Thymeleaf 표현식 잔존 0건 (F0h-c2 사고 재발 방지) |

### 6-2. 동적 (curl)

```bash
# CSV
curl -s -b cookies.txt -o /tmp/users.csv -w "%{http_code}\n" \
  http://localhost:8090/admin/users/export.csv
# 200 · Content-Type text/csv;charset=utf-8 · Content-Disposition attachment · BOM(0xEF 0xBB 0xBF) 시작

# Bulk (CSRF 포함)
CSRF=$(curl -s -b cookies.txt http://localhost:8090/admin/users | grep -oP 'name="_csrf" value="\K[^"]+')
curl -s -b cookies.txt -X POST -d "_csrf=$CSRF" -d "ids=100,101" -d "reason=테스트" \
  -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin/users/bulk/deactivate
# 302 · Location: /admin/users
```

### 6-3. 계약 (신설 4종)

`admin-user-bulk.ts` · `admin-program-bulk.ts` · `admin-application-bulk.ts` · `admin-csv.ts` — 각 endpoint 별 status · CSV 헤더 첫 줄 문자열 · Content-Disposition 값 assertion. `proto:` 필드로 prototype 라인 인용.

### 6-4. 기능 E2E (Playwright)

| 스펙 | 시나리오 |
|---|---|
| `tests/admin-bulk-users.spec.ts` | 목록 진입 → 3건 선택 → 액션바 노출 확인 → 차단 클릭 → 사유 모달 → 확인 → flash `3건 처리` → 목록에 isActive=false 반영 |
| `tests/admin-csv-users.spec.ts` | CSV 버튼 클릭 → 다운로드 감지 (`page.waitForEvent('download')`) → 파일명 정규식 매칭 → 로컬 저장 후 첫 줄 헤더 검증 |

**작성 규칙 (2026-08-13 CLAUDE.md 인터랙션 검증)**: HTMX 없이 순수 form submit + redirect 이므로 status 302 · DOM 변화 · URL 유지 3항목 확인.

### 6-5. 회귀 재실행

- `AdminUserServiceTest` (A5) 전체 PASS
- `AdminApplicationServiceTest` (A4) 전체 PASS
- `AdminProgramServiceTest` (A2/A3) 전체 PASS
- `--project=contracts` 갭 0 (기존 admin-users/admin-programs/admin-program-applications 확장 후 재검사)

---

## 7. 결정 필요 항목 (Qn)

### 핵심 4

| # | 질문 | A안 (권장) | B안 |
|---|---|---|---|
| **QA** | bulk action bar 위치 | ✅ **floating bottom** (prototype 정합 · tsx L483 · html L2772 스타일) | 상단 dropdown |
| **QB** | selection state 관리 | ✅ **페이지 이동 시 초기화** (Qn-3 · 단순화 · 의도치 않은 bulk 처리 방지 · POLICY.md P-BULK-2 신설) | 페이지 이동 후 유지 |
| **QC** | CSV 파일 인코딩 | ✅ **UTF-8 + BOM** (Excel 한글 호환 · prototype tsx L331 정합) | UTF-8 (BOM 없음) |
| **QD** | CSV 다운로드 방식 | ✅ **동기 stream** (초기 규모 수백 건 · 지연 <2s) | 비동기 email 발송 (이월) |

### 세부

| # | 질문 | 권장 |
|---|---|---|
| **Qn-1** | bulk 트랜잭션 | ✅ **per-row** (부분 실패 허용 · 결과 리포트 flash · POLICY.md P-BULK-1) — B안(all-or-nothing) 은 대량 처리 UX 관점 부적합 |
| **Qn-2** | CSV 컬럼 노출 항목 | ✅ **기본 셋** (§4-2 표 참조 · 개인정보 최소 원칙 — role/isActive/lastAccessAt 포함 · 비밀번호 hash·주소 상세 제외). 확장은 후속 |
| **Qn-3** | 전체 선택 범위 | ✅ **현재 페이지만** (prototype `toggleProgramSelectAll` 정합 · html L4105) — B안(필터 결과 전체)은 UX 위험 · "체크박스로 모든 페이지 선택" 은 UI 애매성 |
| **Qn-4** | flash 메시지 형식 | ✅ `"총 N건 중 M건 처리됐어요. K건은 사유로 실패했어요."` + 실패 5건까지 상세 리스트. 존댓말 톤 준수 |
| **Qn-5** | e2e reset endpoint | ✅ **신설** (`POST /e2e/reset-bulk`) — e2e 프로파일에서만 활성화 · A5 `/e2e/reset-users` 패턴 승계 |
| **Qn-6** | 계약 세분화 | ✅ **4종 분리** (`admin-user-bulk` · `admin-program-bulk` · `admin-application-bulk` · `admin-csv`) — §0 근거 |
| **Qn-7** | Content-Disposition | ✅ **attachment** (파일 다운로드 명시 · Excel 자동 실행 방지) — RFC 5987 `filename*=UTF-8''` 병용 |
| **Qn-8** | bulk role safeguard 재적용 방식 | ✅ **AdminUserSafeguard 컴포넌트 추출** (Resolver 패턴 재활용 · A1~A7 확장성 원칙 정합) → 개별·bulk 모두 재사용 |
| **Qn-9** | application bulk 시 신청자 알림 | ✅ **기존 ApplicationEvent 발행 유지** (per-row `Application.approve/reject` 호출로 자동 · A4 정합) |
| **Qn-Δ** | opencsv Writer 옵션 | ✅ **`CSVWriter` 저수준** (컬럼 순서·escape 제어 우선 · BOM 수동 prepend · CRLF 명시). B안 `StatefulBeanToCsv` 는 리플렉션 기반이라 컬럼 순서 어노테이션 관리 부담 |

### 원안 대비 결정 필요 항목 (신규)

| # | 질문 | 권장 |
|---|---|---|
| **Qn-P1** | 프로그램 bulk publish/unpublish 여부 | ⚠️ **미도입** (prototype tsx L507 명시적 금지: "일괄 상태변경 기능 없음") — Program.status 는 신청기간·정원 파생이므로 UPDATE 불가. **사용자 요구사항 §1 스코프에서 삭제** 확인 필요 |
| **Qn-App1** | Applications bulk approve/reject 편입 (prototype 미명시) | ✅ **신설 편입** (`deviation: 'prototype 미명시 · 사용자 요구 반영'`) — POLICY.md 이탈 사유 명시 |
| **Qn-App2** | bulk approve 시 정원 초과 처리 | ✅ **per-row skip** (선착순 성공 · 나머지 실패 사유 "정원 초과") — 시맨틱상 개별 approve 와 동일 |
| **Qn-CSV1** | CSV 상단 버튼과 하단 다크바 CSV 관계 | ✅ **둘 다 노출** (prototype html L935 상단 + L4099 하단). 상단 = **필터 결과 전체** (`ids` 없음), 하단 = **선택 건**(`ids=...`) — 사용자에게 두 경로 명확 |
| **Qn-CSV2** | Users CSV 접근 권한 | ✅ **SYSTEM_ADMIN 전용** (개인정보 대량 export · CENTER_ADMIN 은 자기 센터 사용자 개념 없음) |
| **Qn-CSV3** | Content-Disposition mode | ✅ `attachment` (Qn-7 승계) |

---

## 8. 스코프 예상 규모

- **중** — 파일 20~28 · +1500~2000 LOC · V 마이그레이션 없음 · endpoint 9개 (bulk 6 + CSV 3) + e2e reset 1

**파일 체크리스트** (예상):
- Controllers: `AdminUserController` (+3 endpoint) · `AdminProgramController` (+1) · `AdminApplicationController` (+2) · `AdminCsvController` (신설 · 3 endpoint 통합)
- Services: `AdminUserService` (+3 bulk method) · `AdminProgramService` (+1) · `AdminApplicationService` (+2) · `AdminCsvExporter` (신설 · 3 도메인)
- Domain helper: `AdminUserSafeguard` (신설 · 개별/bulk 공유)
- DTO: `BulkResultFlash` (성공/실패 카운트 + 사유 리스트)
- Templates: `_bulk-action-bar.html` · `_bulk-confirm-dialog.html` · `_csv-download-button.html` (신설 3) + `user/list.html` · `program/list.html` · `program/applications.html` (수정 3)
- JS: `static/js/admin-bulk.js` (신설)
- 테스트: BulkServiceTest × 3 + CsvExporterTest × 1 + RenderTest × 1 + Playwright × 2
- 계약: `.ts` × 4 + `.md` × 4 + POLICY.md 항목 5개 추가

---

## 9. deferred · deviation

### deferred

| 항목 | 담당 (제안) |
|---|---|
| Excel (xlsx) export | `deferred: A8-follow-excel` (또는 별도 후속) — 데이터 규모 증가 시 |
| 비동기 email 발송 CSV | `deferred: A8-follow-async` — 규모 만 단위 넘어가는 시점 |
| 스케줄된 정기 export | `deferred: A8-follow-scheduled` |
| Hard delete bulk (users/programs/applications) | `deferred` (Q10 정책상 하드 삭제 미제공) |
| 관리자 신규 계정 대량 발급 UI | `deferred: A5-1 관리자 계정 관리 화면` (A5 §12 이월 승계) |
| Bulk export progress bar | `deferred: A8-follow-async` (동기 방식 유지 시 불필요) |
| 프로그램 bulk publish/unpublish | `deferred: 정책 재검토 필요` — prototype 명시적 금지지만 관리자 실무 유용성 제기 시 재검토 |
| **CENTER_ADMIN 타 센터 applications CSV 500 → 403** | `deferred: A8-followup-403` (2026-09-18 QA 지적 · F-CSV-2 · 데이터 격리 자체는 정상 · UX 개선 항목 · bulkApprove 처럼 AccessDeniedException 변환 필요) |
| **admin-*-bulk.ts 3종 계약 visual spec 참조** | `deferred: A8-e2e-suite` (2026-09-18 QA 지적 · F-Cov-1 · 실질 검증은 admin-users.ts · admin-programs.ts 확장 항목이 커버) |

### deviation

| 항목 | 사유 |
|---|---|
| **Applications bulk approve/reject 신설** | prototype 미명시 · 사용자 요구 반영 (`deviation: 'prototype tsx L507 은 프로그램 bulk 만 언급 · applications bulk 는 사용자 요구 · A4 개별 flow 정합'`) |
| **CSV 상단 헤더 버튼** 자체 다크 스타일 아님 | 상단 버튼은 화면 헤더 정합 (밝은 톤) · 다크는 하단 액션바만 (prototype 상단 CSV 는 L935 등에서도 밝은 톤) |

**POLICY.md 이탈 여부**:
- P-1 (카피 현행 유지) — 준수 (bulk 확인 모달 문구는 **신규 정보구조** · 카피 이탈 아님)
- P-2 (그림자 브랜드 틴트) — 준수
- P-3 (SVG 아이콘) — 준수 (📥 이모지 사용 시 P-3 위반 — **SVG 아이콘으로 대체** 필수)
- P-4 (폭 토큰 전역 금지) — 준수 (액션바는 `max-width: min(1200px, 90vw)`)
- P-5 (prototype 없는 개선은 계약에 넣지 않음) — Applications bulk 는 `deviation` 명시로 계약에 반영

---

## 10. 작업 큐 메타

- 작업 ID: **A8-admin-bulk-csv**
- 우선순위: admin 트랙 8순위 (ADMIN-00 §9 순서 · **A8 원안 = 뷰 폴리시는 A8-2 로 분리**)
- 추정 단위: 1 PR (파일 20~28 · LOC 1500~2000) — 필요 시 CSV 만 우선 (`A8a-csv`) / bulk 후속 (`A8b-bulk`) 로 2 PR 분할 가능
- 상태: **`spec_done`** — 사용자 결정 대기 (§7 핵심 4 + 세부 11 + 신규 6)
- read-only: 이 spec 은 확정 전까지 편집 금지

---

## 11. 다음 단계 인계

명세 산출 완료. 사용자 결정 필요 항목 §7 참조.

---

## §후속 — 사용자 결정 반영 (2026-09-17)

### 핵심 결정

| Qn | 결정 |
|---|---|
| **QA** action bar 위치 | floating bottom (prototype 정합) |
| **QB** selection state | 페이지 이동 시 초기화 (단순화) |
| **QC** CSV 인코딩 | UTF-8 + BOM (Excel 한글 호환) |
| **QD** CSV 다운로드 방식 | 동기 스트림 |
| **Qn-P1** 프로그램 bulk publish/unpublish | **미도입** — prototype.tsx L507 명시 금지 · `Program.getStatus()` 는 런타임 파생 (신청기간·정원) · UPDATE 불가. Bulk deactivate/reactivate 로 대체 |
| **Qn-App1** Applications bulk | **Approve 만 편입 (deviation)** — 승인은 사유 불필요 상태 전환. **Reject 는 A4 개별 처리 유지** — 반려 사유 개별성 · 감사·항의 대응 정합 정부·청년몽땅 실무 정합 (`deferred: A8-reject-bulk`) |
| **Qn-CSV1** 상하단 CSV 이중 | 상단 = 필터 전체 export · 하단 = 선택 건 export (역할 분리) |
| Qn-1~9 · Qn-Δ | 모두 원안 A (per-row 트랜잭션 · 기본 컬럼 · 페이지 전체 선택 · flash `N건 처리·M건 실패` · reset endpoint · 계약 4종 분리 · attachment · `AdminUserSafeguard` 컴포넌트 추출 · 알림 유지 · opencsv `StatefulBeanToCsv`) |

### Qn-App1 상세 (A안 확정)

**포함**:
- `POST /admin/programs/{id}/applications/bulk/approve` — 다건 승인
  - 트랜잭션: per-row (Qn-1 · 부분 실패 허용)
  - flash: `N건 승인 · M건 실패 (사유: <이유>)`
  - 각 성공 건마다 기존 `ApplicationApprovedEvent` 발송 (Qn-9)
  - 계약 파일 `admin-application-bulk.md` 에 `deviation: 'prototype 미명시 · 사용자 요구 · 성수기 운영 효율'` 명시

**제외 (deferred: A8-reject-bulk)**:
- Reject bulk endpoint 미신설
- 반려는 A4 개별 처리 유지
- 향후 실무 사용 후 필요성 확인 시 후속 티켓에서 템플릿 사유 dropdown (Option C) 방식 검토

### 이월 해소 매핑

- **A5 §9 (Bulk deactivate · CSV · selection UI)** → A8 흡수 ✅
- **A6 §12 (Excel export)** → CSV 만 A8 · **xlsx 재이월** (`deferred: A8-xlsx`)
- **A2 §9 (체크박스·다크 액션바·CSV)** → A8 흡수 ✅
- **A5 §12 (관리자 신규 계정 대량 발급)** → **A5-1 재이월** (email·비밀번호 정책 얽힘)

### 스코프 확정 (Qn-App1 A안 반영)

- 파일 18~26 · +1400~1900 LOC (기존 20~28 에서 축소)
- endpoint 8개: bulk 5 (users 3 · programs 2 · applications 1 approve) + CSV 3
- Reject bulk 는 이월로 스코프 축소

### 회귀 방어 (핵심 3점)

1. **A5 Safeguard 3종 bulk 재적용**: `AdminUserSafeguard` 컴포넌트 추출 → 개별+bulk 재사용
2. **A4 신청 관리 flow 무회귀**: 개별 approve/reject endpoint 유지 · Application 도메인 메서드 무변경
3. **CENTER_ADMIN 데이터 격리**: bulk · CSV 모두 `AdminScope` 적용

### 다음 액션

ym-impl 인계 프롬프트에 위 결정 반영. 특히 Qn-App1 A안 (Approve 만 · Reject 이월) 명시.

---

## 12. 관련 파일 (참조 절대경로)

- 마스터 지시서: `C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\ADMIN-00-master-directive.md` §5-A2 L219~220, §5-A8 L264~269
- A5 이월: `C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\A5-admin-users.md` §9 L437~447 · §12 L508~511
- A6 이월: `C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\A6-admin-stats.md` §12 L271
- A2 이월: `C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\A2-admin-programs-list.md` §9 L366~367
- prototype: `C:\Users\User\IdeaProjects\youth-moa-java\docs\00_assets\admin\prototype.tsx` L327~336, L467, L471, L482~509 · `prototype.html` L2772, L4091~4124, L935/L1189/L1393/L1617
- 기존 계약: `C:\Users\User\IdeaProjects\youth-moa-java\e2e\contracts\admin-users.ts` · `admin-programs.ts` · `admin-program-applications.ts`
- 기존 컨트롤러: `admin\AdminUserController.java` L111~161 · `AdminProgramController.java` L254 · `AdminApplicationController.java` L127~180
- opencsv (이미 존재): `build.gradle.kts` L34 · 기존 사용처 `common\CenterCsvLoader.java`
- POLICY: `C:\Users\User\IdeaProjects\youth-moa-java\docs\design-contracts\POLICY.md` (P-CSV-1~3, P-BULK-1~2 신설 대상)
