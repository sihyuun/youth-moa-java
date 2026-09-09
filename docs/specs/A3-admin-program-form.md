# 작업 명세: A3 — admin-program-form (관리자 프로그램 등록·편집·삭제 · 파생 큐 인라인 통합)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed (A3-1)`** (2026-09-10 사용자 결정: **A3-1 + A3-2 분할** · A3-1 만 이번 착수 · Qn-A/B/C/1~8/Δ1~6 모두 권장안 A) |
| 브랜치 | `feature/A3-1-admin-program-form` (A3-2 는 후속 티켓) |
| A3-1 스코프 | 기본 CRUD (등록/편집/삭제) · 3탭 (정보/신청/약관) · V12 Program 컬럼 확장 (applyPeriod · venue · contact · approval_mode · terms_* · description) · F4/F0c 별도 페이지 유지 (인라인 이월) · 이미지 URL 입력 (파일 업로드 이월) |
| A3-2 이월 | F4/F0c 인라인 통합 · Course · ProgramAttachment · 이미지 파일 업로드 |
| 선행 | ✅ P0-1 Flyway (#109) · ✅ P0-2 SecurityConfig 매처+CSRF (#89) · ✅ P0-3 FileStorage (2026-09-03 · A-admin-notice-attachment) · ✅ A1 admin-shell (#205) · ✅ #206 admin-notice · ✅ #207 admin-terms · ✅ #208 F0c-dynamic-fields · ✅ #210 F4-admin-eligibility · ✅ #211 A2 admin-programs-list |
| 후행 | **A4** admin-program-detail (신청 현황 · 상태 변경) · **A5** admin-users · **A8** carts/calendar/CSV · **A9** center CRUD (Program-Center FK 도입) |
| ADR | `docs/adr/admin-track-roadmap-2026-09.md` §Q4-A "F0c/F4 는 A3 에 흡수" — **파생 큐 분리 후 A3 인라인 통합** 으로 재판정 (§ADR 재판정 참조) |
| 마스터 지시서 | `docs/specs/ADMIN-00-master-directive.md` §5-A3 · §3-1 Program 컬럼 추가 (`applyPeriod` · Course · Attachment · ProgramTerms) · Q3-A (강좌·질문 엔티티 · 약관 컬럼 3개) · Q10 (소프트 삭제) |
| prototype | `docs/00_assets/admin/prototype.html` L2422~2618 (Program Form 3탭: 정보 / 신청 / 약관) + L2678~ 신청 상세 모달 (A4 스코프) |
| main 기준 | `92397b6` (V11 최신 · A2 impl_done) |
| 예상 규모 | **XL — 파일 40~60개 · 순증 3,500~5,500 LOC**. 리스크 최상 (엔티티 3~4개 신설 · 마이그레이션 V12~V14 · 인라인 통합 · multipart · 회귀). **§11 분할 강력 권장** |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설한다
  - `docs/design-contracts/admin/program-form.md`
  - `e2e/contracts/admin-program-form.ts`
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말 "…했어요/됐어요") 준수
- **재활용 계약**: `admin-programs.ts` (A2) · `admin-notice.ts` · `admin-term.ts` · `admin-dynamic-field.ts` · `admin-eligibility.ts` — 셀렉터 네이밍·confirm 모달·400 매핑·PRG 규칙
- **prototype 우선순위**: prototype.html L2422~2618 이 **유일한 실 마크업 근거** (prototype.tsx `ProgramFormScreen` L469 는 stub). 3탭 구조·라벨·placeholder·검증 마커·radio 카드 UI 는 이 원문에서 그대로 계승

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `admin/prototype.html` L2429~2436 | Form card + tabs 3종 "프로그램 정보 / 신청 정보 / 약관 정보" | 3탭 구조 확정 |
| `admin/prototype.html` L2442~2461 | 썸네일 190×190 dash-border 카드 + primary "이미지 업로드" 버튼 + 제목·설명 입력 | 이미지 업로드 UI (Qn-Δ-이미지) |
| `admin/prototype.html` L2463~2482 | 진행 기간 · 신청 기간 (`type=date` × 2 pairs · 필수 · 검증 border) | **applyPeriod 컬럼 신설** (§3-1 Program) |
| `admin/prototype.html` L2483~2492 | 진행 장소 · 문의처 | **venue · contact 컬럼 신설** |
| `admin/prototype.html` L2493~2509 | 청년센터 select (하드코딩 9종 옵션) + 모집 인원 number | **Program-Center FK 도입 or organization select** (Qn-A) |
| `admin/prototype.html` L2510~2521 | 상세 내용 — 툴바 B/I/U + textarea min-height 110px | 경량 에디터 (Qn-Δ-에디터) |
| `admin/prototype.html` L2522~2529 | 첨부파일 — dashed button "첨부파일 업로드 (PDF, HWP)" | **ProgramAttachment 엔티티 신설** (Qn-C 결정) |
| `admin/prototype.html` L2533~2578 | **신청 정보 탭** — 승인 여부(자동/수동 radio) · 강좌 제공 radio + 강좌 구성 (조건부) · 신규 질문 관리 (주관식/객관식 + 리스트) | **F0c 인라인 통합** + Course 엔티티 신설 + 승인방식 컬럼 |
| `admin/prototype.html` L2581~2609 | **약관 정보 탭** — 약관 제공 radio + 약관명 + 약관 내용(1000자) | **약관 관리 3컬럼 또는 별도 페이지 승계** (Qn-B-약관) |
| `admin/prototype.html` L2611~2615 | Form actions — "취소" · "저장" 하단 중앙 | PRG redirect (Qn-Δ-응답) |
| `admin/prototype.tsx` L469 | `function ProgramFormScreen(_: any) { /* 탭: 정보 / 신청 / 약관 */ }` | **stub** — html 원문이 유일 근거 |
| HANDOFF.md §7 폼 검증 규칙 | 검증 실패 border `#EF4444` + 11px 에러 텍스트 + 저장 후 토스트 | 이미 A1/notice/term 에서 흡수 |
| ADMIN-00 §5-A3 (L225~232) | "탭 3 · 강좌 조건부 · 드래그앤드롭 순서 · Bean Validation 매핑 · 신청 마감 < 진행 시작 · edit prefill" | 원 지시 그대로 계승 |
| ADMIN-00 §3-1 (L120~139) | Program 컬럼 추가 대상 12종 열거 (`center FK · applyPeriod · viewCount · Course · venue · contact · Attachment · ApplyQuestion · ProgramTerms · thumbnail 실업로드`) | §3-1 gap 표 근거 |
| ADMIN-00 Q3-A 결정 | "Course·ApplyQuestion 엔티티 · 약관은 Program 컬럼 3개. admin(A3) 선행 → F0c 후행" | 실 진행은 F0c 파생 큐 분리 (§ADR 재판정) |
| ADMIN-00 Q10 결정 | "프로그램·사용자 모두 소프트 삭제, 물리 삭제 미제공" | Qn-3 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype.html | HANDOFF/ADMIN-00 | 축 | 채택 |
|---|---|---|---|---|---|
| 3탭 구조 | 없음 (구버전 hifi 대체됨) | 3탭 명시 | ADMIN-00 §5-A3 명시 | 형태 | **prototype** 무조건 |
| 청년센터 dropdown | 없음 | 9종 하드코딩 | ADMIN-00 Q2: Center FK | 존재 (규칙) | ⚠️ Qn-A — Center FK 도입 vs organization select 유지 |
| 승인 여부 (자동/수동) | 없음 | 신청 정보 탭 명시 | ADMIN-00 §3-1 없음 (신규) | 존재 (규칙) | prototype 채택 → **Program.approvalMode 컬럼 신설** (Qn-Δ-승인) |
| 강좌 제공 여부 + Course 구성 | 언급 | 조건부 UI 명시 | ADMIN-00 §3-1 명시 | 존재+형태 | prototype+ADMIN-00 채택 → Course 엔티티 (Qn-C-Course) |
| 신청 질문 (주관식/객관식) | 있음 | 신청 정보 탭 명시 | F0c-dynamic-fields 완결 (#208) | 존재+형태 | F0c 재활용 · 인라인 통합 (Qn-B) |
| 약관 (제공 여부 + 약관명 + 내용 1000자) | 없음 | 약관 정보 탭 명시 (**Program 종속 1:1**) | ADMIN-00 Q3: Program 컬럼 3개 | 존재 | ⚠️ Qn-A-약관 — Program 컬럼 vs 별도 페이지 (#207) 재활용 |
| 첨부파일 (PDF/HWP) | 없음 | 대시드 버튼 | ADMIN-00 §3-1 명시 (ProgramAttachment) | 존재 | prototype+ADMIN-00 채택 → **ProgramAttachment 엔티티 신설** (Qn-C-Attachment) |
| 상세 내용 에디터 (B/I/U) | 없음 | 툴바 3버튼 + textarea | ADMIN-00 §5-A3 "Toast UI Editor webjar 비교" | 형태 | ⚠️ Qn-Δ-에디터 — 툴바 flavor UI 만 vs 실제 rich editor |

### 1-B. 데이터 모델 gap 표 (필수)

prototype L2429~2609 폼 필드 전체 대비 현재 Program 엔티티. **A3 스코프에서 신설/추가하는 필드만** 표기 (F4/F0c 재활용은 별도 표).

| prototype 필드 | 현재 스키마 (Program 테이블 · V11 기준) | 조치 |
|---|---|---|
| 프로그램 제목 | `title` VARCHAR(255) ✓ | 없음 |
| 프로그램 설명 (짧은) | ❌ 없음 (`content` LONGVARCHAR 만 존재) | **`description` VARCHAR(500) 신설** (선택) |
| 진행 기간 (start/end) | `startDate` · `endDate` LocalDate ✓ | 없음 |
| **신청 기간** (applyStart/applyEnd) | ❌ 없음 | **`apply_start_date` · `apply_end_date` DATE 신설** (V12) |
| 진행 장소 | ❌ 없음 | **`venue` VARCHAR(200) 신설** |
| 문의처 | ❌ 없음 | **`contact` VARCHAR(200) 신설** |
| 청년센터 | `organization` VARCHAR(100) | Qn-A · 유지 or Center FK 승격 (V12) |
| 모집 인원 | `capacity` Integer ✓ | 없음 |
| 카테고리 | `category` VARCHAR(50) ✓ | prototype 폼엔 없음. **폼 유지 vs 제거 결정** (Qn-Δ-카테고리) |
| 이미지 URL | `imageUrl` VARCHAR(500) ✓ | 신설 X · **실 업로드 연동** (P0-3 FileStorage 재사용) |
| 상세 내용 | `content` LONGVARCHAR ✓ | 유지 · 에디터 UI 결정 (Qn-Δ-에디터) |
| 활성 여부 | `isActive` boolean ✓ | 유지 · 소프트 삭제 신호로 활용 (Qn-3) |
| 자격요건 | `ProgramEligibility` @Embeddable ✓ (F4) | F4 재활용 · 인라인 통합 (Qn-B) |
| 승인 여부 (자동/수동) | ❌ 없음 | **`approval_mode` VARCHAR(20) 신설 · enum AUTO/MANUAL** (V12) |
| 강좌 제공 여부 + Course 구성 | ❌ 없음 | **`has_courses` boolean + `Course` 엔티티 신설** (V12) |
| 신청 질문 | `ApplyQuestion` (F0c) 별도 페이지 존재 | **F0c 재활용 · 인라인 통합 (신청 정보 탭 내부)** (Qn-B) |
| 약관 제공 여부 · 약관명 · 약관 내용 (1000자) | ❌ 없음 · `Term` 엔티티는 존재 (전역 약관, #125/#207) | Qn-A-약관 — Program 컬럼 3개 vs Term 별도 페이지 재활용 |
| 첨부파일 (PDF/HWP) | ❌ 없음 | **`ProgramAttachment` 엔티티 신설** (V12 · NoticeAttachment 패턴 복제) |

**신설 엔티티 종합** (Qn 결정 채택 시):

1. `Program` 컬럼 추가 (V12 마이그레이션 1개): `apply_start_date` · `apply_end_date` · `venue` · `contact` · `approval_mode` · `has_courses` (+ Qn-A-약관 A안: `terms_enabled` · `terms_title` · `terms_content`) (+ Qn-Δ-desc: `description`)
2. `Course` 엔티티 신설 (V13): program_id FK · name · schedule · capacity · sortOrder
3. `ProgramAttachment` 엔티티 신설 (V14): program_id FK · filename · path · size · sortOrder — `NoticeAttachment` 패턴 복제

**V 마이그레이션 개수 및 순서** (Qn-C 결정 종속):
- 최소 (분할 A3-1): V12 Program 컬럼만 → V13 는 A3-2 로 이월
- 최대 (통합): V12 (Program 컬럼) + V13 (Course) + V14 (ProgramAttachment) 세 파일 별도

### 1-C. 데이터 소비 지점 (필수)

Program (+ 신설 필드) 를 소비하는 모든 지점을 열거. write→read 왕복 회귀 방어 대상.

| 소비 지점 | 위치 | 이번 티켓 영향 | 회귀 방어 |
|---|---|---|---|
| A3 등록/편집 폼 (신설) | `AdminProgramController.new/edit/create/update/delete` (신설) | **본 티켓 신설** | — |
| A2 목록 | `AdminProgramController.list` L47 | applyPeriod 컬럼 활성화 → "-" 자리에 실 값 렌더 | **A2 spec §9 deviation 갱신 필요** |
| A2 상세 | `AdminProgramController.detail` L77 | 상세 카드에 신설 필드 (venue · contact · apply_period · approval_mode · courses · terms · attachments) 노출 | 상세 templates 확장 |
| A4 신청 현황 (미착수) | 미착수 | Course 별 신청 표시 필요 (course-detail 화면) | A4 spec 착수 시 승계 |
| 사용자 목록 `/programs` | `ProgramController.list` | 신설 컬럼 사용 안 함 (사용자 UI 는 기존 필드만) → **무회귀** | E2E `programs.spec.ts` 재실행 |
| 사용자 상세 `/programs/{id}` | `ProgramController.detail` L140 | 신설 컬럼 (venue · contact · apply_period) 표시 여부 결정 필요 (Qn-Δ-사용자표시) | **prototype.tsx L992+ 확인 필요** — 이번 spec 은 admin 만, 사용자 노출은 후행 티켓 |
| 사용자 신청 폼 `apply.html` | `ApplicationController.applyForm` | F0c ApplyQuestion 소비 그대로. 승인방식 (auto/manual) 은 후행 (신청 → 자동승인 로직) | F0c apply.spec 유지 |
| 홈 최근/마감 임박 | `HomeController` · `ProgramRepository.findTop4By...` | 무회귀 (기존 필드만 사용) | — |
| 캘린더 뷰 | `ProgramCalendarService` | applyPeriod 활성화 시 캘린더 이벤트 재산정 여부 결정 (Qn-Δ-캘린더) | 후행 판단 |
| A1 대시보드 | `AdminDashboardService` | 마감 임박 = endDate 기존 파생 유지 (변경 안 함) | — |
| F0c 하위 페이지 | `AdminApplyQuestionController` | 유지 (인라인 통합 채택 시 이중 진입) or 제거 (Qn-B) | E2E `admin-dynamic-field-*.spec.ts` |
| F4 하위 페이지 | `AdminProgramEligibilityController` | 동 | E2E `admin-eligibility-*.spec.ts` |
| #206 admin-notice · #207 admin-terms | 별도 페이지 | Qn-A-약관 결정에 따라 term 별도 페이지 유지 or 흡수 | E2E 재실행 |

### 1-D. write→read 왕복 통합 시나리오 (필수)

- 관리자가 `/admin/programs/new` 진입 → 3탭 폼 렌더 (정보 탭 default) → 필수값 채우고 저장 → PRG redirect → `/admin/programs/{id}` 상세에 값 노출 → A2 목록에서 "-" 였던 applyPeriod 자리에 실 값 노출 → 사용자 사이드 `/programs/{id}` 무회귀 (신설 필드 미노출 or 노출은 후행 결정)
- 편집: `/admin/programs/{id}` 상세에서 편집 진입 (Qn-1 A: 별도 라우팅 `/edit` vs Qn-1 B: 상세 자체가 편집 폼) → 프리필 → 부분 변경 저장 → 상세 재로드 → 값 반영
- 삭제: 상세 페이지 "삭제" 클릭 → confirm 모달 "…삭제할까요? 삭제 후 복구할 수 없어요." → POST `/admin/programs/{id}/delete` → **소프트 삭제 (Q10, isActive=false)** → 목록에서 SUSPENDED 상태 뱃지 or 필터 시 미노출 (Qn-3-b)
- 신청 정보 탭: 승인방식 radio · 강좌 제공 radio · Course 다중 row · 신규 질문 관리 (F0c ApplyQuestion 인라인 CRUD) → 저장 → `/admin/programs/{id}/dynamic-fields` 별도 페이지에서도 동일 값 조회
- 약관 정보 탭 (Qn-A-약관 A안 · Program 컬럼): "제공 여부 예" 선택 → 약관명·내용 입력 (1000자 카운터) → 저장 → 사용자 사이드 신청 시 이 약관을 표시 (후행 티켓 F-signup-agreements 승계 여부 결정 필요)
- 검증 실패 왕복: `title` 미입력 저장 → 400 flash + 폼 재표시 + 다른 필드 입력값 보존 (Bean Validation → BindingResult → th:errors + 인라인 border)
- 이미지 업로드: multipart submit → FileStorage.save → Supabase URL 저장 → 상세 카드에 썸네일 렌더
- 첨부 업로드: multipart submit (다중 파일) → ProgramAttachment row N개 · Storage upload → 상세 카드에 첨부 리스트 렌더 · 클릭 다운로드
- Course 등록: 강좌 제공 "예" 선택 → 조건부 강좌 구성 카드 노출 → 다중 row 입력 → 저장 → 상세 카드에 강좌 리스트 렌더

---

## 2. 배경 · 스코프 · 인라인 통합 결정

### 2-1. 포함

| URL | 메서드 | RBAC | 목적 |
|---|---|---|---|
| `/admin/programs/new` | GET | Qn-2 결정 (권장: SYSTEM+CENTER · CENTER 는 자기 센터 자동 고정) | 신규 등록 폼 (3탭) |
| `/admin/programs` | POST (multipart) | 동 | Create |
| `/admin/programs/{id}` | GET | 동 | 상세 조회 **+ 편집 폼 (Qn-1)** |
| `/admin/programs/{id}` | POST (multipart) | 동 (본인 센터만) | Update |
| `/admin/programs/{id}/delete` | POST | 동 (본인 센터만) | Delete (소프트 · Q10) |
| `/admin/programs/{id}/attachments/{attachmentId}/delete` | POST | 동 | 첨부 단건 삭제 |
| `/admin/programs/{id}/courses/{courseId}/delete` | POST | 동 | 강좌 단건 삭제 (조건부: Qn-C-Course A안 채택 시) |

### 2-2. 인라인 통합 결정 (Qn-B — 핵심)

**옵션 A (권장): 프로그램 편집 폼 안 3탭 인라인**
- 정보 탭: Program 필드 (자격요건 F4 재활용) — F4 별도 페이지 병존 유지
- 신청 탭: F0c ApplyQuestion 인라인 CRUD (drag reorder · 주관식/객관식 추가) — F0c 별도 페이지 병존 유지
- 약관 탭: Qn-A-약관 결정 종속 (Program 컬럼 or Term 별도 페이지)

**옵션 B: F4/F0c 별도 페이지 유지, A3 는 순수 Program CRUD 만**
- 폼 단순 (스코프 축소) · 3탭 중 신청·약관 탭은 "하위 페이지로 이동" 링크만
- prototype 3탭 UI 미충족

**옵션 C: 인라인만 + 별도 페이지 제거**
- F4/F0c 별도 URL 폐기 (마이그레이션 필요)
- A2 상세에서 인라인 편집만 진입

**옵션 A 상세 (권장 근거)**:
- prototype 원문 준수 (3탭 UI)
- F0c/F4 컨트롤러·서비스 재사용 (`AdminApplyQuestionService` · `AdminProgramEligibilityService` 그대로 호출)
- 별도 페이지 병존 이유: 딥링크·부분 편집 UX (신청 질문만 관리하고 싶은 경우) — 이중 진입 허용
- 회귀 방어: F0c/F4 별도 페이지 URL 유지 → 기존 E2E `admin-dynamic-field-*.spec.ts` · `admin-eligibility-*.spec.ts` 무회귀
- 편집 저장 시 트랜잭션 통합 (Program + Course + ApplyQuestion + Terms 모두 하나의 POST)

### 2-3. 제외 (이월)

| 항목 | 이월처 | 근거 |
|---|---|---|
| 신청 현황 테이블 · 신청 상세 모달 · 상태 변경 드롭다운 · 담당자 의견 | **A4** | ADMIN-00 §5-A4 · Q6 |
| 프로그램 복제 (`bulkClone`) | A4 or A8 | prototype 관리 컬럼 |
| 카드/캘린더 뷰 · CSV · 일괄 선택 | **A8** | ADMIN-00 §5-A8 |
| 조회수 컬럼 (`viewCount`) | **A6** | 통계 트랙 |
| Program-Center FK 승격 (Qn-A B안 시) | A3 vs A9 스코프 판단 필요 | ADMIN-00 Q2 |
| 자동 승인 로직 (approvalMode=AUTO 시 신청 즉시 승인) | A4 (신청 상태 변경 로직에 흡수) | prototype 폼은 저장만 · 실행 로직은 신청 사이드 |
| Course 별 신청 현황 (course-detail 화면) | A4 | ADMIN-00 §5-A4 |
| 사용자 사이드 신설 필드 노출 (venue · contact · apply_period) | 후행 소형 티켓 | prototype.tsx 사용자 상세 재확인 필요 |
| 대기자 (waitlist) | 별도 티켓 (Q7 이월) | ADMIN-00 §8-Q7 |
| WYSIWYG rich editor (Toast UI Editor) | Qn-Δ-에디터 B안 채택 시 후행 | 이번 티켓은 툴바 flavor UI 만 |

---

## 3. A2 상세 vs A3 편집 화면 관계 (Qn-1)

**현황**: A2 (#211) 는 `/admin/programs/{id}` 를 조회 전용으로 만들었고 상단에 "편집" 버튼을 disabled 로 배치.

### 옵션 A: 별도 라우팅 `/edit`
- `/admin/programs/{id}` = 상세 (A2 유지, read-only)
- `/admin/programs/{id}/edit` = 편집 폼 (A3 신설)
- 사용자 흐름: 상세 → 편집 진입 → 저장 → 상세로 돌아옴
- A2 detail.html 유지 · A3 는 form.html 신설 · 명확한 분리
- prototype 실 흐름과 다름 (prototype 은 "상세 = 편집 폼" 형태)

### 옵션 B (권장): 상세 페이지를 편집 폼으로 대체
- `/admin/programs/{id}` = 편집 폼 (툴바에 저장 버튼)
- A2 detail.html 확장 or 대체
- prototype 준수 (관리자 편집 중심 화면 · read-only 상세 별도 화면 없음)
- 사용자 흐름 단순 (A2 목록 → 편집 화면 바로)
- 단점: A2 spec 의 detail 화면 정의가 폐기됨 (A2 상세 = 편집 폼 초기 상태)

### 옵션 C: 두 화면 병존 (상세 + 편집)
- 조회는 상세, 편집은 별도 라우팅
- 옵션 A 와 동일

**권장**: 옵션 B (prototype 준수 + UX 단순). 단 A2 spec 갱신 필수. **Qn-1 사용자 결정 대기**.

---

## 4. F4 · F0c 인라인 통합 (Qn-B)

### 옵션 A (권장): 인라인 통합 + 별도 페이지 병존
- **정보 탭 하단**에 F4 자격요건 입력 (`ProgramEligibility` age/region/etc 3필드 인라인)
- **신청 정보 탭 안**에 F0c ApplyQuestion 인라인 CRUD (주관식/객관식 추가 · 삭제 · 순서 sortOrder 숫자 편집 · Qn-Δ-드래그)
- **F4 별도 페이지 (`/admin/programs/{id}/eligibility`)** 유지 — 딥링크 · 부분 편집
- **F0c 별도 페이지 (`/admin/programs/{id}/dynamic-fields`)** 유지 — 동
- 트랜잭션: A3 form POST 시 Program + Course + ApplyQuestion (upsert list) + Eligibility + Terms 모두 하나의 서비스 메서드에서 저장
- 회귀: F4/F0c 별도 페이지 URL/파라미터 그대로 유지 → 기존 E2E 무회귀

### 옵션 B: 별도 페이지만 유지 (인라인 없음)
- A3 폼은 Program 필드만 저장 · 나머지는 하위 페이지 링크
- prototype 3탭 UI 미준수 (신청 정보 탭 · 약관 정보 탭 실체 없음)

### 옵션 C: 인라인만 + 별도 페이지 제거 (마이그레이션)
- 하위 페이지 폐기 → deep link 사용자 없음 (내부 도구라 가능)
- 마이그레이션 비용 (redirect 30일 + 문서 정리)
- E2E 대량 갱신

**권장**: **옵션 A** — prototype 준수 + 회귀 최소 + F4/F0c 재활용 극대. Qn-B 사용자 결정 대기.

---

## 5. 신설 엔티티 (Qn-C)

### 5-1. Program 컬럼 추가 (V12 마이그레이션 · 필수)

```sql
-- V12__extend_program_for_admin_form.sql
ALTER TABLE program ADD COLUMN apply_start_date DATE;
ALTER TABLE program ADD COLUMN apply_end_date DATE;
ALTER TABLE program ADD COLUMN venue VARCHAR(200);
ALTER TABLE program ADD COLUMN contact VARCHAR(200);
ALTER TABLE program ADD COLUMN approval_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE program ADD COLUMN has_courses BOOLEAN NOT NULL DEFAULT false;
-- Qn-A-약관 A안 채택 시 (권장):
ALTER TABLE program ADD COLUMN terms_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE program ADD COLUMN terms_title VARCHAR(200);
ALTER TABLE program ADD COLUMN terms_content TEXT;
-- Qn-Δ-desc 채택 시:
ALTER TABLE program ADD COLUMN description VARCHAR(500);
```

- 시드 (`DataInitializer.seedPrograms()`) — 기존 12+건 프로그램에 default 값 주입 (apply_start_date = startDate - 14일 · apply_end_date = startDate - 1일 · approval_mode='MANUAL' · has_courses=false · terms_enabled=false). **파생 시드 금지 규칙**과 상충하는지 판단: 시드 데이터는 원본 fixture 이므로 명시 입력 허용 (파생 시드 금지는 lat/lng 처럼 관리자 편집 대상에 대한 규칙 · applyPeriod 는 하드코딩 시드값 · 편집 후 재기동 시 idempotent 체크 유지)

### 5-2. Course 엔티티 (V13 — Qn-C-Course 채택 시)

```sql
-- V13__create_course.sql
CREATE TABLE course (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  schedule VARCHAR(200),
  capacity INT,
  sort_order INT NOT NULL DEFAULT 1,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_course_program ON course(program_id, is_active, sort_order);
```

- 도메인: `Course` 엔티티 · `Program` 은 컬렉션 미보유 (CLAUDE.md 규칙 — 단방향 `@ManyToOne`)
- 이월 옵션 (Qn-C-Course B안): Course 스코프 밖 → strings JSON in Program.courses TEXT (인라인) · A4 course-detail 없이 라벨만
- **권장 A안**: 이번 티켓에서 신설 (prototype UI 준수 · A4 course-detail 준비)

### 5-3. ProgramAttachment 엔티티 (V14 — Qn-C-Attachment 채택 시)

```sql
-- V14__create_program_attachment.sql
CREATE TABLE program_attachment (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
  filename VARCHAR(200) NOT NULL,
  path VARCHAR(500) NOT NULL,
  size BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_program_attachment_program ON program_attachment(program_id, sort_order);
```

- `NoticeAttachment` 패턴 복제
- `FileStorage` bucket 신설: `program-attachments`
- **권장 A안**: 이번 티켓에서 신설. B안 이월 시 prototype 첨부 UI 만 렌더 + 실 저장 X (deviation)

**Qn-C 총 결정**: A안 (Course + Attachment 모두 신설) vs B안 (Attachment 만 A3 · Course 는 이월) vs C안 (둘 다 이월 · A3 는 Program 컬럼만).

---

## 6. 화면 · 라우팅

### 6-1. 등록 폼 `/admin/programs/new`

**공통 헤더**: 브레드크럼 "프로그램 관리 > 신규 등록" · 툴바 "취소" + "저장"

**탭 1: 프로그램 정보** (prototype L2439~2530)
1. 이미지 업로드 카드 (190×190 · dashed · 실 업로드 P0-3 재사용)
2. 프로그램 제목 * (255자) · 프로그램 설명 (500자 · Qn-Δ-desc 채택 시)
3. 진행 기간 * (date range) · 신청 기간 * (date range) — 검증: 신청 마감 < 진행 시작, 시작 ≤ 종료
4. 진행 장소 (200자) · 문의처 (200자)
5. 청년센터 * (organization select or Center FK select · Qn-A)
6. 모집 인원 * (양수)
7. 자격요건 3필드 * (F4 인라인 재활용 · age/region/etc)
8. 상세 내용 * (LONGVARCHAR · Qn-Δ-에디터: 툴바 flavor or Toast UI Editor)
9. 첨부파일 (다중 · PDF/HWP/DOCX/XLSX · Qn-Δ-첨부정책: F0c ATTACHMENT 정책 승계 5MB or 프로그램 자체 정책 별도)

**탭 2: 신청 정보** (prototype L2534~2578)
1. 승인 여부 * (radio card · 자동/수동)
2. 강좌 제공 * (radio card · 예/아니오) → "예" 조건부 Course 다중 row (name · schedule · capacity · + 행 추가 · 삭제)
3. 신청 질문 관리 (F0c ApplyQuestion 인라인 CRUD · 주관식/객관식 추가 버튼 · 리스트 렌더 · sortOrder · 삭제 = soft delete `isActive=false`)

**탭 3: 약관 정보** (prototype L2582~2609)
1. 약관 제공 * (radio · 예/아니오) → "예" 조건부 약관명 * · 약관 내용 * (1000자 카운터)
2. Qn-A-약관 A안: Program 컬럼 3개로 저장 · B안: Term 엔티티 (#207) 로 저장 · Program 과의 관계는 별도 join 테이블

**필드 검증** (Bean Validation · A1/notice/term/F0c/F4 승계):
- `@NotBlank title`, `@Size(max=255)`
- `@NotNull startDate/endDate/applyStartDate/applyEndDate`, `@AssertTrue` 순서 검증
- `@NotBlank organization`
- `@NotNull @Positive capacity`
- `@NotNull content`
- `@Size(max=200) venue/contact`
- `@Size(max=500) description`
- 등등 · 인라인 border `#EF4444` + 11px 에러 텍스트 (prototype L2452 · L2471)

### 6-2. 편집 폼 `/admin/programs/{id}` (Qn-1 B 채택 시)

- 등록 폼과 동일 UI · 기존값 prefilled
- 툴바에 "삭제" 버튼 추가 (confirm 모달 · 소프트 삭제)
- 페이지 타이틀 "프로그램 편집"

### 6-3. 삭제 흐름

- 편집 폼 툴바 "삭제" → `data-confirm-*` 모달 "…삭제할까요? 삭제 후 복구할 수 없어요." → 확인 → POST `/admin/programs/{id}/delete`
- 소프트 삭제: `Program.deactivate()` (isActive=false) — SUSPENDED 상태로 A2 목록에 남음
- 물리 삭제 미제공 (Q10)

### 6-4. 인라인 하위 관리 UI 상세

- **Course**: 강좌 제공 "예" 선택 시 조건부 카드 노출. Row: name/schedule/capacity + "+ 행 추가" 버튼 + 각 row 삭제 버튼. 저장 시 서버로 리스트 POST (JSON or repeated form fields · Qn-Δ-course-encoding)
- **ApplyQuestion**: F0c 목록 renderer 재사용 · "+ 주관식 질문" · "+ 객관식 질문" 버튼 · 각 question row 에 label · options (dropdown 만) · required · sortOrder · 삭제 · 저장 시 서버로 upsert 리스트 POST

---

## 7. RBAC (Qn-2)

### 옵션 A (권장 · A1/A2 승계): SYSTEM+CENTER
- SYSTEM_ADMIN: 전체 CRUD
- CENTER_ADMIN: 자기 센터 (organization 문자열 매칭) 프로그램만 CRUD, 신규 등록 시 organization 자동 고정 (select disabled)
- 근거: A2 는 이미 CENTER_ADMIN 조회 허용. A3 도 편집·삭제 확장 · 자연스러운 승계

### 옵션 B: SYSTEM_ADMIN only
- F0c/F4 정책 승계 (SYSTEM only)
- 근거: Program-Center FK 미도입 = 격리 fragile. A9 (Center FK) 이후 CENTER_ADMIN 확장
- 단점: A2 는 CENTER_ADMIN 허용인데 A3 는 SYSTEM only → 일관성 저하

**권장**: 옵션 A. Qn-2 사용자 결정 대기.

CENTER_ADMIN 확장은 A9 이후로 확정 이월된 상태이므로 **본 티켓 이후** 원 A9 시점에서 organization 문자열 → Center FK 전환 시 CENTER_ADMIN 격리 로직 견고화.

---

## 8. 회귀 방지 (최우선)

### 8-1. 사용자 사이드 무회귀

| 대상 | 검증 | 리스크 |
|---|---|---|
| `/programs` 목록 | `programs.spec.ts` 재실행 | 신설 컬럼이 조회 SELECT 에 포함되지만 UI 렌더 X → 무회귀 |
| `/programs/{id}` 상세 | `program-detail.spec.ts` 재실행 · 계약 갭 0 | 신설 필드 (venue · contact · apply_period) 사용자 노출 여부는 이번 티켓 밖 → 무회귀 |
| `/apply/{programId}` | `apply.spec.ts` 재실행 | approval_mode=AUTO 시 자동 승인 로직은 A4 이후 · 이번 티켓은 저장만 → 무회귀 |
| 홈 최근/마감임박 | 재실행 | 무회귀 |
| 캘린더 | 재실행 | apply_period 캘린더 반영은 후행 티켓 → 무회귀 |

### 8-2. 파생 큐 무회귀

| 대상 | 검증 |
|---|---|
| A2 목록 · 상세 | `admin-programs-list.spec.ts` · `admin-programs-detail.spec.ts` — **applyPeriod "-" 이던 자리에 실 값 노출로 spec 갱신 필요** (A2 spec §11 deviation 갱신) |
| F0c 별도 페이지 | `admin-dynamic-field-*.spec.ts` 전체 무회귀 |
| F4 별도 페이지 | `admin-eligibility-*.spec.ts` 전체 무회귀 |
| #206 notice · #207 term | 무회귀 |
| A1 대시보드 | 무회귀 |

### 8-3. 시드 데이터 안전

- 기존 12+건 프로그램에 default 값 (V12) 주입
- ApplyQuestion 시드 (F0c seed program #7) 무회귀
- Eligibility 시드 무회귀
- 재기동 시 `existsBy...` idempotent 체크로 편집값 보존

### 8-4. Program 엔티티 컬렉션 원칙

- **Program 에 `@OneToMany` 컬렉션 미추가** (CLAUDE.md 규칙)
- Course · ApplyQuestion · ProgramAttachment 는 단방향 `@ManyToOne` 만 · 조회는 Repository query 로

---

## 9. 마이그레이션

Qn-C 결정 종속:

| Qn-C | V12 | V13 | V14 |
|---|---|---|---|
| A (권장 · 통합) | Program 컬럼 확장 | Course | ProgramAttachment |
| B (분할 A3-1) | Program 컬럼 확장 | — | — (A3-2 로 이월) |
| C (최소 · 이월) | Program 컬럼만 | — | — |

시드 재정비: `DataInitializer.seedPrograms()` — apply_start/end · approval_mode · has_courses default 값. 필요 시 샘플 Course · Attachment 부착 (회귀 방어 위해 최소 1건 프로그램만).

**한 번 main 머지된 V 파일은 절대 수정 금지 규칙** — Qn-C 결정 이전에 초기 V12 를 잘못 담으면 롤백 불가 → 분할 시 V12 는 A3-1 로 확정, V13/V14 는 A3-2 로.

---

## 10. 테스트

### 10-1. 정적

- `AdminProgramFormControllerTest` (@WebMvcTest) — GET new/edit · POST create/update/delete · CSRF · RBAC 403 · multipart
- `AdminProgramFormServiceTest` — 트랜잭션 저장 (Program + Course + ApplyQuestion + Eligibility + Terms) · 검증 실패 롤백 · 소프트 삭제
- `AdminProgramFormRequestValidationTest` — Bean Validation TC (~30건)
- `CourseRepositoryTest` (@DataJpaTest) — sortOrder 정렬 · findAllByProgram
- `ProgramAttachmentRepositoryTest` — 파일 메타 · sortOrder
- `AdminProgramFormRenderTest` — Thymeleaf 3탭 렌더 · edit prefill · 검증 마커 · confirm 모달
- `JpaMappingTest` — 신규 엔티티 매핑
- 회귀: `AdminProgramListRenderTest` · `AdminProgramDetailRenderTest` · F0c/F4 render tests · Program 사용자 사이드 render tests

### 10-2. 동적 (curl · 8091)

```bash
# 등록 폼 렌더
curl -s -o /dev/null -w "%{http_code}\n" -b jsessionid http://localhost:8091/admin/programs/new
# → 200

# CSRF 토큰 확보 후 등록
curl -s -X POST -b jsessionid -F "_csrf=..." -F "title=..." -F "..." \
  http://localhost:8091/admin/programs
# → 302 /admin/programs/{newId}

# 상세 (Qn-1 B: 편집 폼)
curl -s -o /dev/null -w "%{http_code}\n" -b jsessionid http://localhost:8091/admin/programs/{id}
# → 200 · prefilled

# 편집 저장
curl -s -X POST -b jsessionid ... http://localhost:8091/admin/programs/{id}
# → 302

# 삭제
curl -s -X POST -b jsessionid -F "_csrf=..." http://localhost:8091/admin/programs/{id}/delete
# → 302 /admin/programs (목록)

# RBAC 회귀 — 타 센터 편집
curl -s -o /dev/null -w "%{http_code}\n" -b centeradmin_jsessionid http://localhost:8091/admin/programs/{타센터_id}
# → 403
```

### 10-3. 계약 신설

- `docs/design-contracts/admin/program-form.md` — 아키텍처 (3탭 · SSR + PRG · multipart) · 상태머신 (탭 전환 · 조건부 렌더) · CTA 라우팅 (저장/취소/삭제/편집)
- `e2e/contracts/admin-program-form.ts` — 필드 셀렉터 · 라벨 · placeholder · 검증 border 색 · radio 카드 스타일 · 조건부 노출 · `proto:` 라인 인용 (prototype.html L2422~2618)
- `npx playwright test --project=contracts` → 갭 0

### 10-4. 기능 E2E (Playwright)

| spec | 검증 |
|---|---|
| `admin-program-form-create.spec.ts` | 3탭 왕복 · 신규 등록 · 상세 반영 확인 |
| `admin-program-form-edit.spec.ts` | 편집 prefill · 부분 변경 · 저장 · 왕복 |
| `admin-program-form-delete.spec.ts` | 소프트 삭제 confirm 모달 · SUSPENDED 상태 목록 노출 |
| `admin-program-form-validation.spec.ts` | 각 필수값 · 기간 순서 · Bean Validation 메시지 |
| `admin-program-form-inline-eligibility.spec.ts` | 인라인 F4 저장 · 사용자 상세 무회귀 |
| `admin-program-form-inline-questions.spec.ts` | 인라인 F0c CRUD · 별도 페이지 무회귀 |
| `admin-program-form-inline-terms.spec.ts` (A안) | Program 약관 3필드 저장 · 재로드 |
| `admin-program-form-course.spec.ts` | 조건부 Course 다중 row · 저장 · 삭제 |
| `admin-program-form-attachment.spec.ts` | multipart 업로드 · 다운로드 · 단건 삭제 · 5MB 상한 |
| `admin-program-form-rbac.spec.ts` | CENTER_ADMIN 자기 센터만 · SYSTEM 전체 · USER 403 |
| **회귀** | apply.spec · programs.spec · program-detail.spec · admin-programs-list.spec · admin-programs-detail.spec · admin-dynamic-field-*.spec · admin-eligibility-*.spec · admin-notice-*.spec · admin-term-*.spec 전수 무회귀 |

### 10-5. reset endpoint (Qn-Δ-reset)

- 기존: reset-notices · reset-terms · reset-apply-questions · reset-applications 확장 or 신설
- **권장**: `POST /__test__/reset-programs-extended` — Program.applyStart/End · venue · contact · approval_mode · has_courses · terms_* 원본 시드로 재설정 · Course·ProgramAttachment 전체 삭제 후 시드 재적용
- 대안: reset-programs (기존이 있으면 확장)

---

## 11. 결정 필요 항목 (Qn)

**핵심 3개 (스코프 결정)**:

### Qn-A · A2 상세 vs A3 편집 화면 관계
- **A (권장 · prototype 준수)**: 옵션 B — 상세 페이지를 편집 폼으로 대체 (`/admin/programs/{id}` = 편집 폼)
- B: 옵션 A — 별도 라우팅 (`/edit`)
- C: 두 화면 병존
- **A 채택 시 A2 spec 갱신 필요** (§3-2 detail = 편집 폼 초기 상태로 재정의)

### Qn-B · F4 · F0c 인라인 통합 방식
- **A (권장 · 회귀 최소)**: 옵션 A — 인라인 통합 + 별도 페이지 병존
- B: 옵션 B — 별도 페이지만 유지 (인라인 없음 · prototype 3탭 미준수)
- C: 옵션 C — 인라인만 + 별도 페이지 제거 (마이그레이션)

### Qn-C · 신설 엔티티 스코프
- **A (권장 · prototype 준수 · 통합)**: Course + ProgramAttachment 모두 A3 에 신설 (V12+V13+V14 세 마이그레이션)
- B: Attachment 만 A3 · Course 는 이월 (A3-2 or 후행)
- C: 최소 스코프 · Program 컬럼만 (V12) · Course·Attachment 이월

**세부**:

### Qn-1 · 3탭 구조 유지 vs 축소
- **A (권장)**: 3탭 유지 (prototype 준수)
- B: 2탭 (정보 · 신청) · 약관 별도
- C: 1탭 (모든 필드 세로 스택)

### Qn-2 · RBAC 정책
- **A (권장 · A2 승계)**: SYSTEM+CENTER
- B: SYSTEM only (F0c/F4 승계)

### Qn-3 · 삭제 정책
- **A (권장 · Q10 승계)**: 소프트 삭제 (isActive=false · SUSPENDED 상태) · 물리 삭제 미제공
- B: 물리 삭제 + 신청 이력 cascade
- C: 신청 이력 있으면 400 · 없으면 물리 삭제

### Qn-4 · 이미지 업로드 방식
- **A (권장)**: FileStorage (P0-3) 재사용 · Supabase bucket `program-thumbnails` 신설 · imageUrl 에 URL 저장
- B: `imageUrl` 텍스트 입력만 (기존 방식 유지 · prototype UI 미준수)
- C: prototype UI 만 렌더 · 실 업로드 X (deviation)

### Qn-5 · 폼 응답 방식
- **A (권장 · admin CRUD 일관성)**: PRG redirect + flash 토스트
- B: HTMX fragment 부분 갱신 (미검증)

### Qn-6 · reset endpoint 확장
- **A (권장)**: `POST /__test__/reset-programs-extended` 신설
- B: 기존 reset endpoint 조합 (없으면 각 테스트가 setup 명시)

### Qn-7 · 계약 신설
- **A (권장 · POLICY 승계)**: 계약 신설 (form.md + admin-program-form.ts)
- B: 미신설 (원문 대조만)

### Qn-8 · 회귀 방어 시드
- **A (권장)**: 기존 시드 12+건 프로그램에 default 값 주입 (편집 없이 유지) · 신규 샘플 프로그램 별도 추가 안 함
- B: 신규 샘플 프로그램 1건 추가 (Course + Attachment 부착) · 나머지는 기본값

**Qn-Δ 세부**:

### Qn-Δ-desc · Program.description 컬럼 신설
- **A**: 신설 (500자 · prototype "프로그램 설명" 필드)
- **B (권장 · 스코프 최소)**: 미신설 · prototype "프로그램 설명" 입력만 렌더 · 실 저장 X (deviation) or content 로 흡수

### Qn-Δ-약관 · 약관 저장 방식
- **A (권장 · prototype 준수)**: Program 컬럼 3개 (terms_enabled · terms_title · terms_content)
- B: Term 엔티티 (#207) 재활용 · Program-Term join 테이블 신설 (스코프 대폭 확장)
- C: 이번 티켓 이월 · 약관 정보 탭은 렌더만

### Qn-Δ-에디터 · 상세 내용 에디터
- **A (권장 · 스코프 최소)**: 툴바 flavor UI 만 (B/I/U 버튼 렌더 · 실 리치 편집 X · textarea 그대로)
- B: Toast UI Editor webjar 실 rich editor (스코프 확장)

### Qn-Δ-첨부정책 · ProgramAttachment 정책
- **A (권장 · F0c 승계)**: 5MB · pdf/hwp/docx/xlsx (F0c ATTACHMENT 정책 승계)
- B: 프로그램 자체 정책 (10MB · 다중 파일 5개 상한 등)

### Qn-Δ-Course-encoding · Course 다중 row 저장 encoding
- **A (권장 · Spring 표준)**: repeated form fields `courses[0].name` `courses[1].name`
- B: JSON hidden field

### Qn-Δ-드래그 · ApplyQuestion sortOrder 편집 UI
- **A (권장 · 스코프 최소)**: 숫자 sortOrder 직접 입력 (F0c 별도 페이지와 동일)
- B: 드래그·드롭 UI (SortableJS webjar)

### Qn-Δ-카테고리 · Program.category 폼 노출
- **A (권장 · 유지)**: 폼에 카테고리 select 포함 (prototype 에는 없으나 A2 spec 이 category 노출 결정)
- B: 폼에서 제거 · prototype 준수 (category 는 A2 목록 컬럼으로만 유지 · admin 편집 UI 없음)

**Qn 총합**: 3 (핵심) + 8 (세부) + 6 (Δ) = **17개** (사용자 결정 필요)

---

## 12. 스코프 예상 규모 · 분할 판단

### 통합 진행 (권장 A안 조합)

| 항목 | 규모 |
|---|---|
| 신규 Java 파일 | 15~20 (Controller 1 · Service 2 · DTO 5 · Entity 2~3 · Repository 2~3 · Renderer helpers) |
| 수정 Java 파일 | 5~8 (Program · AdminProgramController · AdminProgramService · DataInitializer · ProgramService 무회귀 확인) |
| 신규 템플릿 | 3~5 (form.html · form/tab-info.html · form/tab-apply.html · form/tab-terms.html · course fragment · attachment fragment) |
| 수정 템플릿 | 4~5 (admin/program/detail.html · A2 list.html · fragments/header.html · fragments 확장) |
| 신규 테스트 (JVM) | 8~12 |
| 신규 E2E spec | 10~12 |
| 신규 계약 | 2 (form.md + program-form.ts) |
| Flyway V | 3 (V12 · V13 · V14) |
| 총 diff | **3,500~5,500 라인** |
| 리스크 | **최상** — 3 엔티티 · 3 마이그레이션 · 인라인 통합 · multipart · 회귀 폭 넓음 |

### 분할 대안 (강력 권장 검토)

**분할 A3-1** (기본 CRUD · 회귀 최소):
- Program 컬럼 확장 (V12): applyPeriod · venue · contact · approval_mode · has_courses · terms_* · description
- 폼 3탭 UI 렌더 (정보 필드 위주)
- F4/F0c 인라인 렌더만 (편집은 별도 페이지 링크)
- 삭제 (소프트)
- 이미지 업로드 (P0-3 재사용)
- 규모: 파일 ~25 · 순증 ~2,000 LOC · 리스크 중

**분할 A3-2** (인라인 통합 · Course · Attachment):
- Course 엔티티 (V13)
- ProgramAttachment 엔티티 (V14)
- 인라인 F0c/F4 편집 (트랜잭션 통합)
- 인라인 Course 다중 row
- 인라인 첨부 업로드/삭제
- 규모: 파일 ~20 · 순증 ~2,000 LOC · 리스크 중

**분할 A3-3** (선택 · 후행):
- 리치 에디터 (Qn-Δ-에디터 B안)
- 드래그·드롭 (Qn-Δ-드래그 B안)
- 카테고리 편집 UI (Qn-Δ-카테고리)

### 분할 판정 권장

**A3-1 + A3-2 로 2회 분할** 권장. 근거:
1. 통합 5,000 LOC PR 은 리뷰 부담 최상 · 회귀 감지 어려움 (E2E 10건 동시 갱신)
2. A3-1 완료 후 A2 spec 갱신 · 계약 갱신 · CI Green 확인 후 A3-2 착수 → 리스크 계단화
3. Qn-C B안 (Course·Attachment 분리) 와 자연스러운 매핑
4. 사용자 실 사용 흐름상 Course·Attachment 없이도 A3-1 만으로 프로그램 CRUD 가능 (MVP)

**Qn-분할 사용자 결정 대기**:
- A (권장): A3-1 + A3-2 분할
- B: 통합 진행 (5,000 LOC 단일 PR)

---

## 13. ADR 재판정

`docs/adr/admin-track-roadmap-2026-09.md` §2 Q4-A 는 "F0c-dynamic-fields · F4 는 A3 에 흡수" 라고 결정했다. 실 진행은 파생 큐 분리 후 A3 인라인 통합 (별도 페이지 병존) 로 변경됨.

**재판정 근거**:
1. **파생 큐 분리로 위험 계단화**: F0c/F4 를 A3 대형 PR 안에 넣었으면 A3 리스크가 최상 (스키마 3개 + 인라인 3개 + Program CRUD). 파생 큐 분리로 A3 리스크가 "인라인 통합 + Program CRUD" 로 축소
2. **F0c 사용자 사이드 flow 즉시 활성화**: 파생 큐 완결로 사용자 apply.html 에서 dynamic 필드 소비 가능. A3 흡수 방식이었으면 A3 완료까지 대기 필요
3. **A3 인라인 통합은 소형 리팩터**: F0c/F4 컨트롤러·서비스 재사용 → A3 는 form UI 만 새로 그림

**ADR 반영**: `admin-track-roadmap-2026-09.md` §2 Q4-A 항목에 각주 추가:
> "**dynamic-fields·eligibility 는 A3 흡수 대신 파생 큐 3·4번째로 분리 완료 (2026-09-08/09 · #208 · #210). A3 에서는 인라인 통합 + 별도 페이지 병존 (Qn-B A안)**"

---

## 14. deferred / deviation

### deferred

| 항목 | 이월처 |
|---|---|
| 신청 현황 테이블 · 신청 상세 모달 · 상태 변경 드롭다운 · 담당자 의견 | **A4** |
| 프로그램 복제 (bulkClone) | A4 or A8 |
| 카드·캘린더 뷰 · CSV · 일괄 선택 | **A8** |
| 조회수 (`viewCount`) · 상세 진입 시 증가 | **A6** |
| Program-Center FK 승격 | **A9** |
| 대기자 (waitlist) | 후행 (Q7) |
| WYSIWYG rich editor | Qn-Δ-에디터 B안 채택 시 후행 |
| 드래그·드롭 sortOrder UI | Qn-Δ-드래그 B안 채택 시 후행 |
| 사용자 사이드 신설 필드 노출 (venue · contact · apply_period) | 후행 소형 티켓 |
| 자동 승인 로직 실행 (approvalMode=AUTO 시 신청 즉시 승인) | **A4** |
| Course-detail 화면 | **A4** |
| 마이페이지 프로그램 편집 이력 | 별도 티켓 |

### deviation

| 항목 | 사유 |
|---|---|
| 통합 진행 시 (Qn-분할 B 채택) | 대형 PR 리스크 감수 |
| 별도 페이지 병존 (F0c/F4 URL 유지 · Qn-B A안) | 회귀 방어 · 딥링크 유지 · 이중 진입 허용 |
| Program 엔티티에 `@OneToMany` 컬렉션 미추가 | CLAUDE.md 엔티티 규칙 (단방향 `@ManyToOne` 원칙) |
| A2 상세 페이지를 편집 폼으로 대체 (Qn-1 A) | prototype 준수 · A2 spec 갱신 필요 |
| Qn-Δ-에디터 A안 (툴바 flavor UI) | 스코프 최소 · 실 rich editor 는 후행 |
| Qn-Δ-desc B안 (description 컬럼 미신설) 채택 시 | 스코프 최소 · prototype "프로그램 설명" 은 이번 티켓 이월 |
| Qn-Δ-카테고리 A안 (폼 포함) | prototype 에 없으나 A2 spec category 활용 결정 승계 |
| Qn-Δ-Course-encoding A안 (repeated form fields) | Spring 표준 · JSON 없이 처리 |

---

## 15. 작업 큐 메타

- 작업 ID: **A3** (분할 시 A3-1 + A3-2)
- 우선순위: 높음 (파생 큐 완결 후 admin 트랙 최대 티켓)
- 추정 단위:
  - 통합: 1 PR (5,000 LOC · 리스크 최상)
  - 분할: 2 PR (A3-1 · A3-2)
- 상태: **`spec_done`** (Qn 결정 대기)
- 다음 단계: 사용자 Qn-A~C · Qn-1~8 · Qn-Δ-* · Qn-분할 (총 17+1개) 결정 → `spec_confirmed` → ym-impl 인계

---

## 다음 단계 인계

명세 산출 완료. 결정 필요 항목은 **핵심 3 (Qn-A/B/C) + 세부 8 (Qn-1~8) + Δ 6 + 분할 1 = 총 18개**. 대규모 티켓이므로 **분할 판단 (Qn-분할) 을 먼저 확정** 하는 것이 후속 결정 흐름 단순화에 유리합니다.

권장 세트: **"모두 A안 + 분할 (A3-1 + A3-2)"** — prototype 준수 · 회귀 최소 · 스코프 계단화.

사용자 결정 이후 spec 상태를 `spec_confirmed` 로 갱신하고 §11 각 Qn 오른쪽에 채택 안 표기 (A2 spec §11 헤더 형식 참조).
