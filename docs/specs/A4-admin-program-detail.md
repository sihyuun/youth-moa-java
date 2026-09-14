# 작업 명세: A4 — admin-program-detail (관리자 프로그램 상세 · 신청 관리 · 상태 변경 · 담당자 의견)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-15 사용자 결정: **Qn 24건 모두 권장안 A**. §11 그대로 이행) |
| 브랜치 | `feature/A4-admin-program-detail` |
| 스코프 | `/admin/programs/{id}/applications` 신청 관리 화면 · 신청 상세 (모달 or 페이지) · 상태 변경 (APPROVED/REJECTED) · **`adminNote` V15 신설** · 알림 발송 (기존 이벤트 재활용) |
| 선행 | ✅ A2 admin-programs-list (#211) · ✅ A3-1 admin-program-form (#212) · ✅ A3-2 admin-program-form-integration (#213 · V13/V14) · ✅ F0c ApplyQuestion / ApplyAnswer (#208) · ✅ ApplicationNotificationListener (F2b) |
| 후행 | A5 admin-users · A6 stats (승인율) · A8 bulk action · CSV · A9 Program-Center FK (CENTER_ADMIN 격리 강화) |
| 마스터 지시서 | `ADMIN-00-master-directive.md` §5-A4 · §3-3 Applicant · Q6 (담당자 의견 병합) · Q7 waitlist 이월 · Q10 소프트 삭제 |
| 상위 spec | `A3-admin-program-form.md` §12 A4 이월 (신청 현황 · 상태 변경 · 담당자 의견) |
| prototype | `admin/prototype.html` L1370~1441 (신청 현황 테이블) · L2677~2749 (신청 상세 모달) · L1148 program-form applied 카운트 · L1900 알림 벨 링크 |
| main 기준 | `7bb5920` (V14 최신 · A3 완결) |
| 예상 규모 | **중~대 — 파일 20~30개 · 순증 1,800~2,800 LOC**. 리스크 중 (V15 마이그레이션 1개 · 상태 머신 재활용 · 사용자 apply flow 회귀 방어 최우선) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설
  - `docs/design-contracts/admin/program-applications.md` — 아키텍처(테이블 + 상세 모달/페이지) · 상태 머신(PENDING↔APPROVED/REJECTED · CANCELLED read-only) · CTA 라우팅
  - `e2e/contracts/admin-program-applications.ts` — 필드 셀렉터·상태 뱃지 색상·CTA 라벨· `proto:` L1370~1441 · L2677~2749 인용
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말) 승계
- **재활용 계약**: `admin-program-form.ts` (A3 상세 카드 · 확인 모달) · `admin-programs.ts` (A2 필터·정렬·페이지네이션) · `admin-notice.ts` (테이블 패턴)
- **prototype 우선순위**:
  - L1370~1441: 신청 현황 테이블 (9열 grid: check · No · 이름 · 이메일 · 성별 · 핸드폰 · 접수일시 · 참여횟수 · 상태) — **이 원문이 유일 근거**
  - L2677~2749: 신청 상세 모달 (480px width · 상단 신청자 정보 grid · 신청 답변 · 승인/반려 일시 · 상태 select · 담당자 의견 textarea · 확인/취소)
  - 프로토는 **모달 방식** — Qn-B 별도 페이지 옵션은 prototype 미준수 이탈

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `admin/prototype.html` L1370~1396 | 헤더 영역: 좌 "신청 현황" + 요약 배지(신청 X명 · 승인 Y명 · 정원 Z명) · 우 "대기 전체 승인" + "CSV 내보내기" 버튼 | 요약 배지 3종 필수 · **CSV 는 A8 이월** · **대기 전체 승인은 Bulk action → 이월 여부 Qn-Δ1** |
| L1397~1412 | Bulk selection bar (인디고 배경 "N명 선택됨" + 승인/반려/해제) | **Bulk action 이월 (A8)** · UI 자리만 두거나 완전 제거 (Qn-Δ1) |
| L1413~1440 | 9열 grid `44px 44px 100px 1fr 44px 120px 150px 80px 90px` · Header `#F0EFF3` · 각 row hover 없음 · 참여횟수 파생 `countByUserAndStatus(APPROVED)` · 상태 셀은 dropdown/badge (`ap.statusCell`) | 컬럼 스키마 확정 · **참여횟수 파생 쿼리 신설** |
| L2677~2749 (모달) | 480px width · 헤더 "프로그램 신청 상세" + 닫기 X · **신청자 정보 grid 6칸** (이름/이메일/성별/핸드폰/접수일시/참여횟수) · **신청 답변** (F0c ApplyAnswer 렌더 — TEXT: 회색 박스 / DROPDOWN: 보라 pill) · **승인/반려 일시** (조건부) · **상태 select** (승인/대기/반려) · **담당자 의견 textarea** (placeholder "사용자에게 노출됩니다") · Footer 취소/확인 | 프로토 전면 준수 · **`adminNote` 저장 컬럼 V15 신설** |
| L2794~2810 (state) | `applicantStatuses`: `{1:'승인',...}` · `openStatusDropdown` · `selectedApplicants:[]` · `formTab`·`courseProvided`·`termsProvided` | 상태 매핑: 승인=APPROVED · 대기=PENDING · 반려=REJECTED · **취소=CANCELLED** 는 프로토 dropdown 옵션 없음 (read-only 표시) |
| L2833~2834 (알림) | `승인 대기 신청이 7건 있어요` · `프로그램 A 마감이 D-1` → `screen:'program-detail'` | 알림 벨 → A4 화면 링크 (헤더 Advice A7 스코프이지만 링크 target 은 `/admin/programs/{id}/applications` 로 확정) |
| L4185 (CSV) | `downloadCSV('신청자_목록.csv', ['No.','이름',...])` | CSV **A8 이월** — spec §14 deferred |
| `Application.java` L82~104 (도메인) | `approve(admin)` · `reject(admin, reason)` · `cancel(reason)` · `reapply(applyReason)` · idempotent no-op 가능 | **재활용**. 스코프 위반 없음 |
| `ApplicationService.java` L285~354 | `@Transactional approve/reject/cancel` · idempotent · `ApplicationEventPublisher.publishEvent` (Approved/Rejected/Cancelled 3종 이벤트) | **재활용** · 사용자 apply flow 무회귀 |
| `ApplicationNotificationListener.java` L33~84 | `@TransactionalEventListener(AFTER_COMMIT) + REQUIRES_NEW` · APPROVED/REJECTED/CANCELLED 알림 자동 발행 | **재활용** — spec §5 신규 이벤트 없음 |
| `AdminScope.java` L44~52 | SYSTEM_ADMIN → null · CENTER_ADMIN → `Center.name` 매칭 (organization 문자열 근사) | A2/A3 승계 · Qn-1 |
| ADMIN-00 §5-A4 (L235~242) | "정보 카드 · 신청 현황 테이블 · 상태 dropdown 직접 변경 · 신청 상세 모달 · 대기자 관리 (Q7 미채택) · **기존 ApplicationEvent 재활용**" | 원 지시 그대로 계승 |
| ADMIN-00 §3-3 Applicant | email/gender/phone `@EntityGraph fetch join` · visits 파생 · 대기자 미도입 (Q7) · **담당자 의견 = adminComment 컬럼 or 처리 이력** (Q6) | Q6 재확인 → **`adminNote` VARCHAR(1000) 채택** (spec §5) |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype | HANDOFF/ADMIN-00 | 축 | 채택 |
|---|---|---|---|---|---|
| 신청 현황 테이블 (9열) | 언급 | L1413~1440 명시 | ADMIN-00 §5-A4 승계 | 형태 | prototype 무조건 |
| 담당자 의견 저장 | wireframe 원본에 명시 (§ADMIN-00 §1-A 표) | L2739 textarea | Q6 병합 결정 | 존재 | **adminNote V15 신설** |
| 신청 상세 UI | wireframe: 페이지 형태 (신청 이력 + 담당자 의견) | 모달 480px | ADMIN-00 §1-A "병합" | 형태 | ⚠️ Qn-B — 모달(prototype) vs 페이지(wireframe) · 프로토 우선 원칙상 **A안 모달 권장** |
| 상태 dropdown 옵션 (승인/대기/반려) | 없음 | L2732~2734 3옵션 | ADMIN-00 §3-3 4종 (+CANCELLED) | 존재 | **prototype 3옵션 + CANCELLED read-only 노출** (Qn-C 관리자 강제 CANCELLED 여부 별도) |
| CSV / Bulk action | 없음 | 명시 (L1391 L1402~1409) | ADMIN-00 §5-A4 제외 | 형태+존재 | **A8 이월** — UI 자리 (Qn-Δ1: 남길지 제거) |
| 대기 전체 승인 (`approveAllPending`) | 없음 | L1387 명시 | ADMIN-00 §5-A4 미명시 | 존재 | ⚠️ Qn-Δ2 — 이번 티켓 vs A8 이월 |
| 반려 사유 입력 | 없음 | prototype 모달 dropdown 만 있고 사유 입력 필드 없음 | Application.reject(reason) 시그니처 존재 | 존재 | ⚠️ Qn-2 — 사유 필수 강제 vs 선택 (별도 modal step or 담당자 의견에 흡수) |
| 참여횟수 컬럼 | 언급 | 명시 (L1436) | ADMIN-00 §3-3 파생 | 존재+형태 | **파생 쿼리 신설** (`ApplicationRepository.countByUserAndStatus(APPROVED)`) |
| Course 별 신청 | 없음 | L1554 `courseList` 별도 카드 | ADMIN-00 §5-A4 `course-detail` 화면 | 존재 | ⚠️ Qn-Δ3 — 이번 티켓 course-detail 화면 신설 vs 이월 |

### 1-B. 데이터 모델 gap 표 (필수)

A3 완료 후 스키마 (V14) 기준. **A4 신설 대상만**.

| prototype 필드 | 현재 스키마 | 조치 |
|---|---|---|
| 신청번호 (row id) | `application.id BIGSERIAL` ✓ | 유지 |
| 신청자 이름·이메일·성별·핸드폰 | `Application.user @ManyToOne` 경유 ✓ | `@EntityGraph(attributePaths={"user"})` fetch join 추가 |
| 접수일시 | `application.applied_at TIMESTAMP` ✓ | 유지 · `@CreatedDate` |
| 상태 (승인/대기/반려/취소) | `application.status VARCHAR(20)` ENUM ✓ | 라벨 매핑 상수 신설 (PENDING=대기, APPROVED=승인, REJECTED=반려, CANCELLED=취소) |
| 참여횟수 (visits) | ❌ 없음 (파생) | **`ApplicationRepository.countByUserAndStatus(user, APPROVED)`** 신설 · N+1 방지 위해 페이지 로드 시 userId 일괄 조회 (`countBy...GroupByUser`) |
| 신청 답변 (질문·답변 리스트) | `ApplyAnswer @ManyToOne Application/Question` ✓ (F0c) | `applyAnswerRepository.findByApplicationOrderByQuestionSortOrder` 조회 |
| 승인/반려 일시 | `application.processed_at` ✓ | 유지 |
| 처리한 관리자 | `application.processed_by @ManyToOne User` ✓ | 유지 |
| **담당자 의견 (adminNote)** | ❌ 없음 | **`application.admin_note VARCHAR(1000)` 신설 (V15)** |
| 반려 사유 | `application.reject_reason VARCHAR(500)` ✓ | 유지 · 모달 상 UI 위치 결정 (Qn-2) |
| 취소 사유 | `application.cancel_reason VARCHAR(200)` ✓ (D5) | 유지 · 관리자 화면에서는 read-only 노출 |

**신설 마이그레이션**:

```sql
-- V15__add_application_admin_note.sql
ALTER TABLE application ADD COLUMN admin_note VARCHAR(1000);
```

- 도메인 메서드: `Application.updateAdminNote(String note)` 신설 · `@Setter` 금지 규칙 준수
- `AdminScope` 규칙 위반 시 `IllegalStateException` (Service 계층)
- 재기동 시드 무회귀 — 기존 신청 시드는 `admin_note NULL` 로 유지

### 1-C. 데이터 소비 지점 (필수 · **회귀 방어 최우선**)

Application 상태·adminNote 를 소비하는 모든 지점.

| 소비 지점 | 위치 | 이번 티켓 영향 | 회귀 방어 |
|---|---|---|---|
| **A4 신청 관리 테이블 (신설)** | `AdminApplicationController.list` (신설) | 이번 티켓 신설 | — |
| **A4 신청 상세 모달 (신설)** | 동 | 이번 티켓 신설 | — |
| **A4 상태 변경** | `AdminApplicationController.approve/reject/updateNote` (신설) | 이번 티켓 신설 · **ApplicationService.approve/reject 도메인 메서드 재활용** | 사용자 apply flow 무회귀 (E2E) |
| A3 편집 폼 정보 카드 "신청 X / Y명" | `AdminProgramController.detail` (A3-1) | 카운트 로직 무회귀 | `admin-program-form.spec.ts` 재실행 |
| A3 상세 → A4 진입 링크 (신설) | A3 detail 툴바 or applicants 섹션 헤더 | 링크 1개 추가 (`/admin/programs/{id}/applications`) | A3 계약 갭 0 확인 |
| A2 목록 신청현황 컬럼 | `AdminProgramController.list` | 무회귀 (기존 파생 유지) | `admin-programs-list.spec.ts` |
| A1 대시보드 "승인 대기 알림" | `AdminDashboardService` | 무회귀 (기존 파생) · 알림 링크 target 은 A4 로 변경 (`/admin/programs/{id}/applications`) — Qn-Δ4 | `admin-dashboard-*.spec.ts` |
| **🔒 사용자 apply flow** | `ApplicationService.apply` L107~165 | **변경 없음** · idempotent 유지 · A4 는 read/approve/reject/updateNote 만 호출 | **`apply.spec.ts` 전수 재실행 · 무회귀 필수** |
| **🔒 사용자 취소 flow** | `ApplicationService.cancel` L329~354 | **변경 없음** · Qn-C 관리자 강제 CANCELLED 는 별도 메서드 신설 (기존 cancel 재사용 여부는 결정 사항) | `mypage-cancel.spec.ts` |
| **🔒 사용자 재신청 flow** | `Application.reapply` L107~116 (CANCELLED → PENDING) | **변경 없음** · A4 REJECTED 후 재신청 정책 유지 (거절 후 재신청 차단 = 현행 유지) | `apply.spec.ts` |
| **🔒 사용자 알림 (approve/reject/cancel)** | `ApplicationNotificationListener` L33~84 | **재활용 · 이벤트 발행 로직 변경 없음** | `notifications-*.spec.ts` |
| 마이페이지 신청 이력 | `MyApplicationsController` (기구현) | 무회귀 (admin_note 컬럼은 신설되나 마이페이지 표시 여부는 Qn-Δ5) | `mypage-applications.spec.ts` |

### 1-D. write→read 왕복 통합 시나리오 (필수)

- **목록 조회**: SYSTEM_ADMIN `/admin/programs/{id}/applications` → 필터(상태·신청자 검색) + 페이지네이션 → 9열 테이블 렌더 → 요약 배지(신청 N · 승인 M · 정원 Z)
- **상세 열기 (Qn-B A 모달)**: row 클릭 → HTMX GET `/admin/programs/{id}/applications/{aid}/detail` → fragment 렌더 → 신청자 정보 · F0c 답변 · 상태 select · adminNote textarea · 처리 이력
- **상세 열기 (Qn-B B 별도 페이지)**: row 클릭 → `/admin/programs/{id}/applications/{aid}` 페이지 이동
- **상태 변경 (승인)**: 모달 select "승인" → 확인 버튼 → POST `/admin/programs/{id}/applications/{aid}/approve` → `ApplicationService.approve` → `ApplicationApprovedEvent` → AFTER_COMMIT 알림 발행 → PRG redirect → 목록 뱃지 색상 갱신 (승인=`#D1FAE5` bg + `#047857` fg) + 요약 배지 승인 카운트 +1
- **상태 변경 (반려)**: select "반려" → adminNote/rejectReason 입력 (Qn-2 A: rejectReason 필수 · B: adminNote 로 흡수) → POST → `ApplicationService.reject(id, admin, reason)` → 이벤트 → 알림 → PRG
- **담당자 의견 저장**: adminNote textarea 편집 → 확인 → POST `/admin/programs/{id}/applications/{aid}/note` → `Application.updateAdminNote(note)` → 재로드 시 노출
- **재로드 후 값 확인**: 모달 재오픈 → 저장한 adminNote 유지 · 처리한 관리자 이름 · 처리일시 노출
- **관리자 강제 CANCELLED (Qn-C A 채택 시)**: dropdown "취소" 노출 → confirm 모달 → POST `/admin/programs/{id}/applications/{aid}/force-cancel` → `Application.cancel(reason)` → `ApplicationCancelledEvent` 발행 → 사용자에게 알림
- **RBAC 격리**: CENTER_ADMIN 이 타 센터 프로그램 URL 직접 접근 → 403 + 목록에서도 비노출
- **알림 클릭 왕복**: 사용자가 `승인 대기 알림` 클릭 → `/admin/programs/{id}/applications` 진입 → PENDING 필터 자동 적용 (Qn-Δ4)

---

## 2. 배경 · 스코프

### 2-1. 포함

| URL | 메서드 | RBAC (Qn-1) | 목적 |
|---|---|---|---|
| `/admin/programs/{id}/applications` | GET | SYSTEM + CENTER (본인 센터만) | 신청 관리 화면 (테이블 + 요약 + 필터) |
| `/admin/programs/{id}/applications/{aid}/detail` | GET | 동 | 상세 fragment (Qn-B A 모달 시 HTMX) |
| `/admin/programs/{id}/applications/{aid}` | GET | 동 | 상세 페이지 (Qn-B B 채택 시) |
| `/admin/programs/{id}/applications/{aid}/approve` | POST | 동 | 승인 · reason 없음 · ApplicationApprovedEvent |
| `/admin/programs/{id}/applications/{aid}/reject` | POST | 동 | 반려 · rejectReason 필드 · ApplicationRejectedEvent |
| `/admin/programs/{id}/applications/{aid}/note` | POST | 동 | 담당자 의견 저장만 (상태 미변경) |
| `/admin/programs/{id}/applications/{aid}/force-cancel` | POST | 동 (Qn-C A 시) | 관리자 강제 취소 · ApplicationCancelledEvent |
| `/__test__/reset-applications-note` | POST | test profile | E2E 재현용 초기화 |

**쿼리 파라미터**:
- `status` = PENDING/APPROVED/REJECTED/CANCELLED/all (기본 all · Qn-Δ6)
- `q` = 신청자 이름·이메일 (like)
- `from`/`to` = 신청일 범위 (LocalDate)
- `page` = 페이지 번호 (기본 0)
- `size` = 페이지 크기 (Qn-Δ7 · 기본 20)

**컨트롤러 시그니처 예**:

```java
@Controller
@RequestMapping("/admin/programs/{programId}/applications")
@RequiredArgsConstructor
public class AdminApplicationController {
  private final AdminApplicationService adminApplicationService;
  private final AdminScope adminScope;
  // GET / , GET /{aid}/detail , POST /{aid}/approve , POST /{aid}/reject ...
}
```

### 2-2. 제외 (이월)

| 항목 | 이월처 | 근거 |
|---|---|---|
| CSV 내보내기 (신청자 목록) | **A8** | ADMIN-00 §5-A4 는 뷰·CSV·bulk 를 A8 로 분리 · prototype L4185 |
| Bulk 승인/반려 (선택 다건) | **A8** | 동 |
| 대기 전체 승인 (`approveAllPending`) | **A8** (Qn-Δ2 B) or 본 티켓 (Qn-Δ2 A) | prototype L1387 |
| 프로그램별 승인율 통계 | **A6** | ADMIN-00 §5-A6 |
| Course 별 신청 현황 (course-detail 화면) | 본 티켓 Qn-Δ3 A or 후행 | prototype L1554 |
| 마이페이지 adminNote 노출 | 후행 소형 티켓 | Qn-Δ5 |
| 대기자 (waitlist) | 후행 (Q7 이월) | ADMIN-00 §8-Q7 |
| CENTER_ADMIN 격리 강화 (Program-Center FK) | **A9** | ADMIN-00 Q2 |
| 반려 후 재신청 허용 정책 변경 | 별도 티켓 | 현행 `case REJECTED -> throw` 유지 |

---

## 3. RBAC (Qn-1)

### 옵션 A (권장 · A2/A3 승계): SYSTEM+CENTER
- SYSTEM_ADMIN: 전체 프로그램 신청 관리
- CENTER_ADMIN: 자기 센터 (organization 문자열 매칭) 프로그램의 신청만
- 근거: A2/A3 는 이미 CENTER_ADMIN 조회·편집 허용 · A4 도 일관성
- **AdminScope 재활용**: `Program.organization == currentAdminCenterName` 검사를 Service 진입점에 강제 (`AdminApplicationService.assertProgramInScope(programId)`)

### 옵션 B: SYSTEM_ADMIN only
- 승인/반려 권한은 SYSTEM 만 · CENTER_ADMIN 은 조회만
- 근거: 승인은 보수적으로 · A9 이후 격리 견고화되면 확장
- 단점: 실 운영 시 SYSTEM 이 병목

**권장**: 옵션 A. Qn-1 사용자 결정 대기.

---

## 4. 상세 UI (Qn-B)

### 옵션 A (권장 · prototype 준수): 모달 (480px)
- 목록 row 클릭 → HTMX GET fragment → 오버레이 모달 렌더
- prototype L2679~2748 UI 그대로
- 장점: 목록 이탈 없음 · 빠른 처리 (승인/반려 후 목록 상태 즉시 갱신)
- 단점: 답변 많은 신청은 모달 스크롤 · 처리 이력 카드 좁음

### 옵션 B (wireframe 준수): 별도 페이지
- `/admin/programs/{id}/applications/{aid}` 페이지
- 처리 이력 전체 · adminNote 편집 이력 · 첨부 다운로드 등 확장 여지
- 단점: prototype 이탈 · 목록 왕복 UX

### 옵션 C: 하이브리드
- 기본은 모달 · 답변 5개 이상 or 첨부 존재 시 "전체 보기" 링크로 페이지 이동
- 단점: 복잡

**권장**: 옵션 A. Qn-B 사용자 결정 대기.

---

## 5. 관리자 강제 CANCELLED (Qn-C)

### 옵션 A (권장 · 운영 유연성): 도입
- 새 URL `POST /admin/programs/{id}/applications/{aid}/force-cancel` · reason 필드 (Qn-2 결정과 연동)
- `Application.cancel(reason)` 재사용 (도메인 메서드는 신청자 검증 없음)
- `ApplicationCancelledEvent` 발행 → 사용자 알림 자동
- 상태 dropdown 옵션에 "취소" 추가 (prototype 은 3개지만 확장 · deviation 표기 필요)
- 사용자 사이드 취소와 구분: `processedBy` 는 관리자 · `cancelReason` prefix "관리자 취소: ..."

### 옵션 B: 미도입 (현행 유지)
- 관리자는 승인/반려만 · 취소는 사용자 본인만
- prototype 준수 (dropdown 3옵션)
- 단점: 오등록/이중 신청 정정 불가 (실운영 걸림돌)

**권장**: 옵션 A. Qn-C 사용자 결정 대기.

---

## 6. 화면 · 라우팅

### 6-1. 목록 `/admin/programs/{id}/applications`

**공통 헤더**: 브레드크럼 "프로그램 관리 > {프로그램명} > 신청 관리" · 툴바 "← 프로그램 상세로"

**필터 바**:
1. 상태 세그먼트 (전체/대기/승인/반려/취소) — 기본 "전체" (Qn-Δ6)
2. 신청자 검색 (이름 or 이메일 · 300ms debounce · HTMX)
3. 신청일 range (from ~ to · LocalDate)
4. 초기화 버튼

**요약 배지** (L1374~1384):
- 신청 총 N명 · 승인 M명 (green) · 정원 Z명 (indigo)

**테이블** (9열 grid `44px 44px 100px 1fr 44px 120px 150px 80px 90px`):
- ☐ (check · Bulk 이월 시 disabled or 제거) · No · 이름 (신청자 링크) · 이메일 · 성별 · 핸드폰 · 접수일시 · 참여횟수 · **상태** (badge · Qn-Δ8 direct dropdown or badge only)

**빈 상태**: "아직 신청이 없어요"

**페이지네이션**: 20건/페이지 (Qn-Δ7)

**클릭 → 모달 (Qn-B A)**: HTMX `hx-get="/admin/programs/{id}/applications/{aid}/detail" hx-target="#modal-root" hx-swap="innerHTML"`

### 6-2. 신청 상세 모달 (Qn-B A)

prototype L2679~2748 그대로 재현.

**섹션**:
1. **헤더**: "프로그램 신청 상세" · 닫기 X
2. **신청자 정보 grid 6칸** (bg `#F0EFF3`):
   - 이름 · 이메일 · 성별 · 핸드폰 · 접수일시 · 참여횟수 (파생)
3. **신청 답변** (F0c ApplyAnswer 재활용):
   - TEXT: 회색 박스 (`#F0EFF3` bg + `#E3E1E8` border · 1.5 line-height)
   - DROPDOWN: 보라 pill (`#EDE9FE` bg + `#7C3AED` fg)
   - ATTACHMENT: 파일명 링크 (다운로드 endpoint 재사용 — F0c apply-answer download)
   - Empty 시 섹션 미노출 (`hasAnswers`)
4. **처리 이력** (조건부 · `hasStatusDate`):
   - "승인일시 2026-09-11 14:23" (green) or "반려일시 ...":
5. **상태 select**: `<select>` 옵션 승인/대기/반려 (+Qn-C A 시 취소)
6. **담당자 의견 textarea** (max 1000자 · placeholder "담당자 의견을 입력해주세요. 사용자에게 노출됩니다.")
7. **반려 사유** (Qn-2 A · 상태=반려 시 조건부 표시): 별도 textarea (max 500자) or adminNote 로 흡수 (Qn-2 B)
8. **Footer**: "취소" · "확인" (인디고)

**저장 흐름** (Qn-Δ9):
- **A (권장 · 원자적)**: 확인 클릭 → 상태 + adminNote + rejectReason 을 한 번의 POST 로 저장 · Service 는 상태 전이 후 note 갱신 트랜잭션 통합
- **B (분리)**: 상태는 dropdown 직접 변경(자동 저장) · adminNote 는 별도 저장 버튼

### 6-3. 상세 페이지 (Qn-B B)

동일 컴포넌트를 페이지 레이아웃으로 렌더. 우측 사이드 액션 카드에 상태 변경 폼.

### 6-4. 프로그램 상세 (A3) → 신청 관리 진입

A3 편집 폼 상단에 링크 배지 추가:
- "신청 관리 (N건 대기)" → `/admin/programs/{id}/applications?status=PENDING`
- N 은 `countByProgramAndStatus(PENDING)` 파생

**A3 spec 갱신**: 이번 티켓 완료 후 A3 계약 확장 (링크 배지 추가 셀렉터).

---

## 7. 신설 컴포넌트

| 항목 | 위치 |
|---|---|
| `AdminApplicationController` | `admin/AdminApplicationController.java` |
| `AdminApplicationService` | `admin/AdminApplicationService.java` (scope 검사 + list 쿼리 + updateNote) |
| `AdminApplicationListRow` (DTO) | `admin/dto/` — user_id · name · email · gender · phone · appliedAt · visits · status · id |
| `AdminApplicationDetailView` (DTO) | 신청자 정보 + answers + note + 처리이력 |
| `ApplicationAdminNoteRequest` (Bean Validation) | `@Size(max=1000)` note · `@Size(max=500)` rejectReason |
| `Application.updateAdminNote(String)` | 도메인 메서드 (Application.java 확장) |
| `ApplicationRepository` 쿼리 추가 | `countByUserIdInAndStatus(Set<Long>, ApplicationStatus)` (참여횟수 일괄) · `findByProgramWithUser(programId, filter, pageable)` `@EntityGraph` |
| `templates/admin/program-applications/list.html` | 목록 화면 |
| `templates/admin/program-applications/detail-modal.html` | HTMX fragment |
| `templates/admin/program-applications/fragments/row.html` | row fragment (상태 변경 후 partial swap) |
| `V15__add_application_admin_note.sql` | migration |
| `docs/design-contracts/admin/program-applications.md` + `e2e/contracts/admin-program-applications.ts` | 계약 신설 |

---

## 8. 회귀 방지 (최우선)

### 8-1. 사용자 사이드 무회귀

| 대상 | 검증 | 리스크 |
|---|---|---|
| `/apply/{programId}` | `apply.spec.ts` 재실행 | 신설 컬럼 admin_note 는 apply flow 미소비 → 무회귀 |
| `/mypage/applications` | `mypage-applications.spec.ts` | admin_note 표시 여부 Qn-Δ5 결정 — B(미표시) 채택 시 무회귀 |
| `/apply/complete?applicationId=..` | 재실행 | 무회귀 |
| 취소 flow (`MyPageCancelController`) | `mypage-cancel.spec.ts` | `Application.cancel(reason)` 도메인 메서드 무회귀 |
| 재신청 flow (REJECTED → 차단) | `apply.spec.ts` | **현행 유지** (Qn-Δ10 B) |

### 8-2. 알림 리스너 무회귀

| 대상 | 검증 |
|---|---|
| `ApplicationApprovedEvent` 발행 | A4 approve → NotificationListener 자동 발행 → `notifications-approved.spec.ts` 재실행 |
| `ApplicationRejectedEvent` 발행 | 동 |
| `ApplicationCancelledEvent` 발행 | Qn-C A 시 관리자 강제 CANCELLED 도 동일 리스너 소비 → 사용자에게 알림 (deviation: 알림 문구는 `'취소했습니다'` 그대로 · 관리자 취소 구분 안 함 · Qn-Δ11) |

### 8-3. Application 상태 머신 무회귀

- PENDING → APPROVED/REJECTED (A4 · 기존)
- PENDING → CANCELLED (사용자 · 기존)
- CANCELLED → PENDING (사용자 재신청 · 기존)
- REJECTED → (차단 · 현행 유지)
- **Qn-C A 시 신규**: PENDING/APPROVED → CANCELLED (관리자 강제) · 도메인 메서드는 `Application.cancel(reason)` 재사용
- **APPROVED → PENDING 전환** (Qn-Δ12): 관리자가 승인 취소를 시도할 때. 현행 도메인 메서드 없음 · 이번 티켓은 **미허용** (한 방향만 · REJECTED 전환은 가능 · 사용자에게 rejectEvent 발행)

### 8-4. F0c ApplyAnswer 소비 무회귀

- A4 모달에서 답변 렌더는 read-only · 편집 없음
- F0c 별도 페이지 (`/admin/programs/{id}/dynamic-fields`) 무회귀
- ATTACHMENT 다운로드 endpoint 재사용

---

## 9. 마이그레이션

```sql
-- V15__add_application_admin_note.sql
ALTER TABLE application ADD COLUMN admin_note VARCHAR(1000);
```

- **한 번 main 머지된 V 파일은 수정 금지 규칙 준수** → adminNote 컬럼 스펙은 이번에 확정
- 시드: `DataInitializer.seedApplications()` — admin_note 는 NULL default · 샘플 승인 신청 1건에만 "확인 완료" 부착 여부 결정 (Qn-Δ13)
- 재기동 idempotent 유지

---

## 10. 테스트

### 10-1. 정적

- `AdminApplicationControllerTest` (@WebMvcTest) — GET list · GET detail · POST approve/reject/note/force-cancel · CSRF · RBAC 403 · scope 격리
- `AdminApplicationServiceTest` — scope 검사 · updateNote · 반려 사유 검증 · idempotent
- `AdminApplicationListRepositoryTest` (@DataJpaTest) — filter (status · q · from/to) · pagination · EntityGraph fetch join · countByUserIdIn 참여횟수 일괄 조회
- `ApplicationAdminNoteRequestValidationTest` — Bean Validation
- `AdminApplicationListRenderTest` — Thymeleaf 렌더 · 요약 배지 · 9열 grid · 빈 상태
- `AdminApplicationDetailModalRenderTest` — 답변 렌더 · adminNote textarea · 상태 select 옵션
- `JpaMappingTest` — Application.adminNote 컬럼 매핑
- **회귀**: `AdminProgramFormRenderTest` (A3 신청 관리 링크 배지 · `admin-program-form.spec.ts`) · `AdminDashboardRenderTest` (알림 링크 target)

### 10-2. 동적 (curl · 8091)

```bash
# 목록 렌더
curl -s -o /dev/null -w "%{http_code}\n" -b jsessionid http://localhost:8091/admin/programs/1/applications
# → 200
curl -s -b jsessionid "http://localhost:8091/admin/programs/1/applications?status=PENDING" | grep -c '<tr'

# 상세 fragment
curl -s -o /dev/null -w "%{http_code}\n" -b jsessionid -H "HX-Request: true" \
  http://localhost:8091/admin/programs/1/applications/5/detail
# → 200

# 승인
curl -s -X POST -b jsessionid -H "HX-Request: true" -F "_csrf=..." \
  http://localhost:8091/admin/programs/1/applications/5/approve
# → 302 or 200 + row swap fragment

# 반려
curl -s -X POST -b jsessionid -F "_csrf=..." -F "rejectReason=..." -F "adminNote=..." \
  http://localhost:8091/admin/programs/1/applications/5/reject
# → 302

# 담당자 의견 저장만
curl -s -X POST -b jsessionid -F "_csrf=..." -F "adminNote=..." \
  http://localhost:8091/admin/programs/1/applications/5/note
# → 302

# RBAC 격리 (다른 센터 관리자)
curl -s -o /dev/null -w "%{http_code}\n" -b centeradmin_jsessionid \
  http://localhost:8091/admin/programs/{otherCenterProgramId}/applications
# → 403
```

### 10-3. 계약 신설

- `docs/design-contracts/admin/program-applications.md` — 아키텍처 · 상태 머신 · CTA
- `e2e/contracts/admin-program-applications.ts` — proto L1370~1441 · L2677~2749 인용
- `npx playwright test --project=contracts` → 갭 0

### 10-4. 기능 E2E (Playwright)

| spec | 검증 |
|---|---|
| `admin-program-applications-list.spec.ts` | 목록 렌더 · 필터 · 페이지 · 요약 배지 카운트 · 빈 상태 |
| `admin-program-applications-detail-modal.spec.ts` | HTMX 모달 open · 신청자 정보 · F0c 답변 렌더 · 처리 이력 |
| `admin-program-applications-approve.spec.ts` | 승인 왕복 · 뱃지 색상 변화 · 요약 배지 증가 · 알림 자동 발행 (사용자 세션 확인) |
| `admin-program-applications-reject.spec.ts` | 반려 + rejectReason · 알림 발행 · Qn-2 검증 |
| `admin-program-applications-note.spec.ts` | adminNote write→read 왕복 |
| `admin-program-applications-rbac.spec.ts` | SYSTEM 전체 · CENTER 자기 센터만 · 타 센터 URL 403 |
| `admin-program-applications-force-cancel.spec.ts` (Qn-C A) | 관리자 강제 CANCELLED · 이벤트 발행 |
| **회귀** | apply.spec · mypage-cancel.spec · mypage-applications.spec · notifications-*.spec · admin-programs-list.spec · admin-program-form.spec · admin-dashboard-*.spec 전수 무회귀 |

### 10-5. reset endpoint

- `POST /__test__/reset-applications-note` 신설: 모든 Application.admin_note NULL 리셋 (기존 status/processedBy 유지) — E2E 각 spec 사이 독립성
- 대안: 기존 `reset-applications` 확장 (adminNote 포함)

---

## 11. 결정 필요 항목 (Qn)

### 핵심 3

#### Qn-A · RBAC 정책 (RBAC)
- **A (권장 · A2/A3 승계)**: SYSTEM + CENTER (본인 센터)
- B: SYSTEM only (승인/반려 · CENTER 는 조회만)

#### Qn-B · 상세 UI (모달 vs 페이지)
- **A (권장 · prototype 준수)**: 모달 480px (HTMX fragment)
- B: 별도 페이지 (wireframe 준수 · 확장 여지)
- C: 하이브리드

#### Qn-C · 관리자 강제 CANCELLED
- **A (권장 · 운영 유연성)**: 도입 · `Application.cancel(reason)` 재사용 · 알림 발행
- B: 미도입 (현행 유지 · prototype 준수)

### 세부

#### Qn-1 · adminNote 필드 위치
- **A (권장 · 단순)**: `application.admin_note VARCHAR(1000)` 단일 컬럼 (spec §5)
- B: 별도 `application_admin_note` 이력 테이블 (created_at · admin_id · note) — 변경 이력 보존
- C: `application_history` 통합 이력 테이블 (상태 전이 + note 모두 기록)

#### Qn-2 · 반려 사유 필수 여부
- **A (권장 · UX 명확)**: 필수 (Bean Validation `@NotBlank rejectReason`)
- B: 선택 (adminNote 로 흡수 · rejectReason 필드 미사용)
- C: 조건부 (내부 정책 안내 문구만)

#### Qn-3 · 알림 발송 방식
- **A (권장 · 재활용)**: 기존 `ApplicationApprovedEvent` / `RejectedEvent` / `CancelledEvent` 재활용 · 신규 이벤트 없음
- B: `ApplicationNoteUpdatedEvent` 신설 (adminNote 만 저장 시 사용자 알림) — 스코프 확장

#### Qn-4 · REJECTED 후 재신청 허용 정책
- **A (권장 · 현행 유지)**: REJECTED → 차단 (사용자는 재신청 불가) · admin 이 상태를 PENDING 으로 되돌리는 UI 없음
- B: 관리자가 REJECTED → PENDING 전환 가능 (§8-3 12번 Qn-Δ12 와 연동)

#### Qn-5 · 필터·정렬 기본값
- **A (권장)**: 기본 필터 = 전체 · 정렬 = 접수일시 DESC
- B: 기본 필터 = PENDING (관리자 처리 대기 우선 노출)

#### Qn-6 · 페이지당 개수
- **A (권장 · A5 승계)**: 20
- B: 10 (prototype 모바일 편의)
- C: 사용자 설정 (범위 확대)

#### Qn-7 · 통계 요약 카드 (A4 스코프)
- **A (권장 · A6 이월)**: 요약 배지 3종 (신청/승인/정원) 만 · 승인율 · 반려율 등은 A6
- B: 승인율 progress bar 추가 (prototype 미명시)

#### Qn-8 · reset endpoint 신설
- **A (권장)**: `/__test__/reset-applications-note` 신설
- B: 기존 `reset-applications` 확장

### Qn-Δ 세부

#### Qn-Δ1 · Bulk action UI 자리 처리
- **A (권장 · 이월 명시)**: prototype 의 bulk selection bar UI 완전 제거 (deviation 명시)
- B: 자리만 남기고 버튼 disabled + "A8 에서 지원" 툴팁

#### Qn-Δ2 · 대기 전체 승인 (`approveAllPending`)
- A: 이번 티켓 구현 (`POST /applications/approve-all-pending`)
- **B (권장 · 스코프 최소)**: A8 이월

#### Qn-Δ3 · Course 별 신청 (`course-detail` 화면)
- A: 이번 티켓 신설 (`/admin/programs/{id}/courses/{cid}/applications`)
- **B (권장 · 스코프 최소)**: A4 후속 or A6 이월 · 이번 티켓은 전체 프로그램 신청만

#### Qn-Δ4 · 알림 링크 target
- **A (권장)**: 승인 대기 알림 → `/admin/programs/{id}/applications?status=PENDING`
- B: `/admin/programs/{id}` (A3 편집 폼)

#### Qn-Δ5 · 마이페이지 adminNote 노출
- A: 사용자 마이페이지 신청 이력에 adminNote 표시 (사용자에게 노출됨 — prototype textarea placeholder)
- **B (권장 · 후행)**: 이번 티켓은 admin 저장만 · 사용자 노출은 후행 소형 티켓

#### Qn-Δ6 · 상태 필터 기본값
- **A (권장)**: 전체
- B: PENDING (관리자 처리 편의)

#### Qn-Δ7 · 페이지당 개수 (Qn-6 재확인)
- **A**: 20
- B: 10

#### Qn-Δ8 · 상태 표시 (badge vs direct dropdown)
- A: prototype 처럼 row 내 직접 dropdown (`ap.statusCell`) — 상태 변경도 목록에서 바로
- **B (권장 · 원자성)**: badge 만 표시 + 상세 모달에서만 변경 (오조작 방지)

#### Qn-Δ9 · 저장 원자성
- **A (권장)**: 원자적 (상태 + adminNote + rejectReason 하나의 POST)
- B: 분리 (상태 자동 저장 · adminNote 별도)

#### Qn-Δ10 · REJECTED 후 재신청 정책 (Qn-4 재확인)
- **A**: 유지 (차단)
- B: 허용 (현행 로직 변경)

#### Qn-Δ11 · 관리자 강제 CANCELLED 알림 문구
- Qn-C A 채택 시 · 사용자에게 발송되는 알림 문구:
  - **A (권장)**: 기존 "취소했습니다" 문구 그대로
  - B: "관리자가 신청을 취소했습니다" 별도 (`NotificationType.APPLICATION_FORCE_CANCELLED` 신설)

#### Qn-Δ12 · APPROVED/REJECTED → PENDING 되돌리기
- A: 허용 (`Application.revertToPending()` 도메인 메서드 신설)
- **B (권장 · 현행 유지)**: 미허용 · 한 방향만

#### Qn-Δ13 · 시드 adminNote
- A: 샘플 승인 신청 1건에 "확인 완료" 부착
- **B (권장)**: 전건 NULL

**Qn 총합**: 3 (핵심) + 8 (세부) + 13 (Δ) = **24개** (권장안 채택 시 대체로 A 로 통일 · Qn-Δ2/3/5/8/10/12/13 은 B 권장)

---

## 12. 스코프 예상 규모

| 항목 | 규모 |
|---|---|
| 신규 Java 파일 | 6~9 (Controller · Service · 3~5 DTO · Repository 쿼리 추가) |
| 수정 Java 파일 | 3~4 (Application 도메인 · ApplicationRepository · AdminDashboardService 알림 링크 · A3 detail template) |
| 신규 템플릿 | 2~4 (list.html · detail-modal.html · row fragment · empty state) |
| 수정 템플릿 | 2~3 (A3 program-form 링크 · 알림 벨 target) |
| 신규 테스트 (JVM) | 5~7 |
| 신규 E2E spec | 6~7 |
| 신규 계약 | 2 (contract md + ts) |
| Flyway V | 1 (V15) |
| 총 diff | **1,800~2,800 라인** |
| 리스크 | **중** — V15 1개 · 상태 머신 재활용 · 사용자 apply flow 회귀 방어 최우선 · 알림 자동 발행 확인 |

**분할 판단**: 통합 진행 권장 (단일 PR). V15 1개 · 신설 도메인 없음(Application 확장만) · 인프라 재활용률 높음. 분할 이득 적음.

---

## 13. deferred / deviation

### deferred

| 항목 | 이월처 |
|---|---|
| CSV 내보내기 | **A8** |
| Bulk 승인/반려 | **A8** |
| 대기 전체 승인 (Qn-Δ2 B) | **A8** |
| 승인율 / 반려율 통계 | **A6** |
| Course 별 신청 (course-detail) | Qn-Δ3 B 시 후행 |
| 마이페이지 adminNote 노출 | 후행 소형 티켓 (Qn-Δ5 B) |
| 대기자 (waitlist) | 후행 (Q7) |
| CENTER_ADMIN Program-Center FK 격리 강화 | **A9** |
| APPROVED/REJECTED → PENDING 되돌리기 | Qn-Δ12 A 채택 시 후행 |
| 이벤트 outbox 신뢰성 강화 | **F2c** |

### deviation

| 항목 | 사유 |
|---|---|
| prototype bulk selection bar 제거 (Qn-Δ1 A) | A8 이월 · UI 자리 정리 |
| prototype "대기 전체 승인" 버튼 제거 (Qn-Δ2 B) | 동 |
| 상태 dropdown 옵션에 CANCELLED 추가 (Qn-C A) | prototype 3옵션 이탈 · 관리자 강제 취소 도입 |
| 상태 표시 badge only (Qn-Δ8 B) | prototype direct dropdown 이탈 · 오조작 방지 · 모달에서만 변경 |
| 모달 저장 원자적 (Qn-Δ9 A) | prototype dropdown 즉시 반영 이탈 · 데이터 정합성 |
| REJECTED 후 재신청 차단 유지 (Qn-4 A) | 현행 로직 유지 · 정책 변경은 별도 티켓 |
| Program-Center FK 없이 organization 문자열 매칭 | A9 이월 (ADMIN-00 Q2) |

---

## 14. 작업 큐 메타

- 작업 ID: **A4**
- 우선순위: 높음 (A3 완결 후 admin CRUD 완성도 · 사용자 신청 사이드 UX 완결)
- 추정 단위: 1 PR (통합)
- 상태: **`spec_done`** (Qn 결정 대기)
- 다음 단계: 사용자 Qn-A~C · Qn-1~8 · Qn-Δ1~13 (총 24개) 결정 → `spec_confirmed` → ym-impl 인계

---

## 다음 단계 인계

명세 산출 완료. 결정 필요 항목은 **핵심 3 + 세부 8 + Δ 13 = 총 24개**. 권장 세트: **핵심 3 모두 A · 세부 대부분 A · Qn-Δ 는 §11-Δ 개별 권장 참조** (A / B 혼재).

권장 요약:
- **RBAC**: SYSTEM + CENTER (A2/A3 승계 · AdminScope 재활용)
- **상세 UI**: 모달 (prototype 준수 · HTMX fragment)
- **관리자 강제 CANCELLED**: 도입 (운영 유연성 · `Application.cancel(reason)` 재활용)
- **adminNote**: 단일 컬럼 V15 (`application.admin_note VARCHAR(1000)`)
- **반려 사유**: 필수 (`@NotBlank rejectReason`)
- **알림**: 기존 이벤트 3종 재활용 (신규 이벤트 없음)
- **저장 원자성**: 원자적 (상태 + note + rejectReason 하나의 POST)
- **CSV / Bulk / Course 별 신청**: A8/A6 이월
- **사용자 apply flow · notification listener · 상태 머신**: 무회귀 최우선

사용자 결정 이후 spec 상태를 `spec_confirmed` 로 갱신하고 §11 각 Qn 오른쪽에 채택 안 표기.
