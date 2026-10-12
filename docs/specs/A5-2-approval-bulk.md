# A5-2 Admin Approval Bulk (중간 범위)

- 상태: `spec_confirmed`
- 작성일: 2026-10-12
- 선행 spec: `docs/specs/A4-admin-program-detail.md`, `docs/specs/A8-admin-bulk-csv.md`
- 트랙: admin · 신청 관리

---

## 1. 목적

A8 에서 Reject bulk 와 "대기 전체 승인"을 이월했었다. A5-2 는 이 두 가지를 완결하고, 신청 그리드에 성별 열을 추가해 운영자가 지원자 성비를 즉시 파악할 수 있게 한다.

## 2. 변경 범위

### A. Reject bulk 편입 (P0)
- 신규 `AdminApplicationBulkService.bulkReject(programId, ids, adminEmail, reason)` — row 당 REQUIRES_NEW tx. REJECTED/CANCELLED 는 idempotent skip. APPROVED → REJECTED 전이 시 `WaitlistPromotionService.promoteIfEligible()` 호출 (A4 개별 reject 와 동일 방어선).
- 신규 endpoint `POST /admin/programs/{id}/applications/bulk/reject` — `ids`, `reason` (@Size(max=200) · blank 금지).
- `_bulk-action-bar.html` `domain='applications'` 블록에 "일괄 반려" 버튼 (`admin-bulk-btn--danger`).
- `admin-bulk.js` reject 액션 등록 — `requiresReason: true`, path 는 `data-reject-path` 로 주입.
- 각 성공 건마다 `ApplicationRejectedEvent` 발행 → 기존 리스너가 알림/메일 자동 커버.

### B. 대기 전체 승인 (P0)
- 신규 `AdminApplicationBulkService.bulkApproveAllPending(programId, adminEmail)` — Repository `findIdsByProgramIdAndStatus(PENDING)` 로 ids 수집 후 `bulkApprove` 위임.
- 신규 endpoint `POST /admin/programs/{id}/applications/bulk/approve-all-pending`.
- `applications.html` 툴바에서 CSV 내보내기 옆에 "대기 전체 승인" 버튼. 조건: `summary['PENDING'] > 0`.
- 500 건 하드 cap (초과 시 flashError, 처리 중단).
- data-testid: `approve-all-pending-btn`.

### C. 성별 열 추가 (중간 범위 추가 결정)
- `applications.html` grid: `44px 44px 100px 1fr 44px 120px 150px 80px 90px 90px` (10열, 체크·No·이름·이메일·성별·핸드폰·접수일시·참여횟수·상태·관리). 관리 열은 상태 셀 안으로 흡수하지 않고 기존 패턴을 유지하기 위해 상태 다음 액션 열 90px — **최종 10열**: `44 · 44 · 100 · 1fr · 44 · 120 · 150 · 80 · 90 · 90`.
- 성별 라벨: `UserGender.label` ("남"/"여"), null → "-".
- CSV 성별 컬럼 추가 (`gender`) — `AdminCsvController.exportApplications` 헤더 11열로 확장.
- 반응형 `@media (max-width: 1200px)` — 성별·참여횟수 축소 or 숨김 (현재 CSS 에 1200 미디어 쿼리 없으므로 최초 도입).

## 3. 사용자 결정 (전부 권장안)

| ID | 결정 |
|---|---|
| Q1 | Reject bulk 사유 = 공통 1건 (모달 input 1개 재사용) |
| Q2 | 사유 최대 200자 (@Size(max=200)) |
| Q3 | bulk 최대 500건, 초과 시 flashError 로 중단 |
| Q4 | CENTER_ADMIN 지원 — effectiveCenterId 매칭 (기존 bulkApprove 와 동일) |
| Q5 | 성별 열 포함 |
| Q6 | 응답 = redirect (A4/A8 패턴 유지) |
| Q7 | cross-page select 없이 "대기 전체 승인"으로 대체 |
| Q8 | 반려 이메일 = 기존 ApplicationRejectedEvent 리스너 자동 커버 |
| Q9 | 액션바 다크 유지 (POLICY §QA deviation) |
| Q10 | "N건" → "N명" — applications 도메인만 적용 |

## 4. 파일 범위

### 신설
- `docs/specs/A5-2-approval-bulk.md` (본 문서)
- `src/test/java/.../admin/AdminApplicationBulkRejectServiceTest.java`
- `src/test/java/.../admin/AdminApplicationBulkApproveAllPendingTest.java`
- `e2e/tests/admin-application-bulk-reject.spec.ts`

### 수정
- `AdminApplicationBulkService.java` (bulkReject + bulkApproveAllPending + MAX_BULK_SIZE)
- `AdminApplicationController.java` (POST /bulk/reject, POST /bulk/approve-all-pending)
- `ApplicationRepository.java` (findIdsByProgramIdAndStatus)
- `AdminCsvController.java` (gender 컬럼)
- `templates/admin/program/applications.html` (grid 9열 + 성별 셀 + 대기 전체 승인 버튼)
- `templates/admin/fragments/_bulk-action-bar.html` (반려 버튼 + rejectPath attr)
- `static/js/admin-bulk.js` (reject 액션 등록 + applications 도메인 count 라벨 "명")
- `static/css/admin.css` (grid 9열 + 1200px 반응형)
- `e2e/contracts/admin-program-applications.ts` (grid 열 추가 체크 · 대기 전체 승인 deferred → active)
- `e2e/contracts/admin-application-bulk.ts` (reject.absent → reject.exists)
- 기존 테스트 cascade: `admin-bulk-csv.spec.ts` 신청 CSV 헤더 (gender 포함)

## 5. 명세 체크 (구현 매핑 준비)

| 요구 | 코드 | QA 확인 |
|---|---|---|
| bulkReject row 단위 tx | `AdminApplicationBulkService#bulkReject` | 각 row 실패 다음 row 영향 없음 |
| 사유 @Size(max=200) + blank 금지 | Controller validate | 공백/201자 모두 flashError |
| 500건 cap | `AdminApplicationBulkService.MAX_BULK_SIZE` | 501건 → flashError |
| CENTER_ADMIN scope | `adminScope.effectiveCenterId()` 체크 (기존 bulkApprove 재사용) | 타 센터 요청 → AccessDenied |
| APPROVED→REJECTED 공석 승격 | bulkReject 내부 `promoteIfEligible` | PENDING 1건 자동 승인 |
| 대기 전체 승인 | `bulkApproveAllPending` + `findIdsByProgramIdAndStatus` | PENDING 전부 → APPROVED, 이벤트 발행 |
| 대기 전체 승인 버튼 조건 | `summary['PENDING'] > 0` | PENDING 0 → 버튼 숨김 |
| 성별 열 (화면) | `applications.html` 셀 + `UserGender.label` | 남/여/- 렌더 |
| 성별 열 (CSV) | `AdminCsvController` 헤더+셀 | 다운로드 CSV 11열 |
| grid 10열 | admin.css L2312 | 헤더·행 모두 10열 정렬 |
| 반응형 | admin.css 1200px | 1200 이하 — 축소/숨김 |
| 액션바 "명" 라벨 | admin-bulk.js applications 도메인 분기 | 다른 도메인은 "건" 유지 |

## 6. 금지

- row 별 반려 사유 (Q1 결정: 공통 1건)
- 액션바 색상 변경 (Q9 유지)
- Q 결정 범위 외 리팩토링
- 커밋 (사용자 승인 + qa + verify 후)

## 7. 다음

- 구현 완료 → ym-qa (단위·회귀·E2E)
- 그 다음 ym-verify (적대적 검증 + 매핑 재대조)
