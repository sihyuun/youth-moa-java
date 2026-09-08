# F0c-dynamic-fields — 관리자 설정 동적 추가정보

| 메타 | 값 |
|---|---|
| 작업 ID | `F0c-dynamic-fields` |
| 상태 | **spec_confirmed** (2026-09-08 사용자 결정: Qn-1~10 A · Qn-Δ A · Qn-11 B · Qn-8 C · 모두 §11 권장안. ADR §Q4 재판정 승인 — F0c-dynamic-fields 를 A3 흡수 대신 파생 큐 세 번째로 선행) |
| 트랙 | admin 파생 큐 (세 번째) — read-only 명세 |
| 선행 | ✅ A1 admin-shell (PR #205) · ✅ A-admin-notice-attachment (PR #206) · ✅ A-admin-terms-crud (PR #207) · ✅ F0c-remainder (PR #75/85 impl_done — apply.html 3단계 위저드) |
| 후행 | F4 자격요건 admin 입력 폼 → A2/A3 (프로그램 목록·폼) |
| ADR | `docs/adr/admin-track-roadmap-2026-09.md` §2 Q4-A "F0c-dynamic-fields · F4 는 A3 에 흡수" 를 파생 큐 분리로 재판정 (아래 §12) |
| 관련 지시서 | `docs/specs/ADMIN-00-master-directive.md` §3-1 (ApplyQuestion 정의) · §3-3 (ApplyAnswer 정의) · Q3-A |
| 사용자 사이드 스펙 | `docs/specs/F0c-remainder.md` Q5-A "동적 추가 정보 범위 제외 — F0c-dynamic-fields 큐 등재" |
| 예상 규모 | 1 PR — 파일 18~22개 · 순증 ≈ 1,400~1,800 LOC (테스트 포함) · 리스크: 사용자 apply flow 회귀 |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설한다
  - `docs/design-contracts/admin/program-dynamic-fields.md`
  - `e2e/contracts/admin-dynamic-field.ts`
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말 "…했어요/됐어요") 준수
- **사용자 사이드 계약**: `docs/design-contracts/apply.md` + `e2e/contracts/apply.ts` — 이번 티켓의 dynamic-fields 렌더가 여기에 영향. **완료 판정 시 apply 계약 갭 0 유지**가 회귀 방어 핵심
- **재활용 계약**: `admin-notice.ts` · `admin-term.ts` (list/form/edit 셀렉터 네이밍 · confirm 모달 · 400 매핑 규칙)
- **admin/prototype.html 부재 근거**: `docs/00_assets/admin/prototype.tsx` L469 `ProgramFormScreen` 은 stub (`/* 탭: 정보/신청/약관 */`). notice·terms 와 같은 상황 → notice/terms 계약 신설 근거(admin POLICY + 사용자 CRUD 패턴) 그대로 적용

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `docs/00_assets/prototype.tsx` `ProgramApply` L1074~1163 | 신청 폼 UI — 지원 동기 textarea 1개만, dynamic field 렌더 없음 | prototype 은 dynamic field 를 그리지 않음. F0c-remainder Q5 로 이월된 이유 |
| `docs/00_assets/prototype.html` L1108~1197 | tsx 와 동일 (같은 소스) | 동일 |
| `docs/00_assets/HANDOFF.md` L1179~1183 | `Application` 데이터 계약: `{ id, programId, userId, appStatus, appliedAt, extraAnswers, rejectReason? }` — **`extraAnswers` 필드 명시** | 사용자 신청 데이터에 dynamic 응답 배열이 존재해야 함이 HANDOFF 계약. UI 는 안 그렸지만 데이터 계약은 있음 |
| `docs/00_assets/HANDOFF.md` (문자열 grep) L1182 `extraAnswers` · admin/prototype.tsx L163 `answers?: { question, answer }[]` | 관리자 화면에서 **신청 상세 모달** 안에 `answers` 를 보여준다는 계약 | 이번 티켓 스코프는 관리자 필드 CRUD + 사용자 응답 저장. **관리자 신청 상세 모달 렌더는 A5 admin-applications 로 이월** (스코프 격리) |
| `docs/00_assets/wireframe.png` (F0c-remainder 인용 L33) | "관리자 설정 동적 · 주관식 0/100 · 객관식 dropdown · 첨부파일 업로드, 모두 필수" + 강좌 선택 dropdown | **정책 원본** — 필드 3종 · 각 필드 필수 여부는 관리자 지정 · 강좌 dropdown 존재 (강좌 = A3 Course 엔티티, 이번 티켓 밖) |
| `docs/00_assets/admin/prototype.tsx` L163 · L469 | `answers?: { question, answer }[]` + `ProgramFormScreen` stub "탭: 정보/신청/약관" | 관리자 프로그램 편집이 **탭 구조 (정보 / 신청 / 약관)** — dynamic-fields 는 "신청" 탭 인라인 섹션이 자연스러움 (Qn-9 참조) |
| `docs/specs/ADMIN-00-master-directive.md` §3-1 L137 | "**`ApplyQuestion` 엔티티 신설** (program FK, type 주관식/객관식, 보기 목록, `sortOrder` — 드래그앤드롭 순서)" | 엔티티 이름·필드 이미 확정. 이 티켓은 이 계약을 그대로 구현 |
| `docs/specs/ADMIN-00-master-directive.md` §3-3 L159 | "**`ApplyAnswer` 엔티티 신설** (application FK + question FK + 답변) — A3 의 ApplyQuestion 과 세트" | 응답 저장 방식 = 별도 row (Qn-6 참조) |
| `docs/specs/ADMIN-00-master-directive.md` §5-A3 L232 | "**F0c-dynamic-fields (사용자 신청 폼 동적 질문) 와 엔티티 공유 — admin 이 선행**" | admin 이 먼저 만들고 사용자가 소비. 지금 순서 일치 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype (사용자/admin) | HANDOFF | 축 | 채택 |
|---|---|---|---|---|---|
| dynamic 필드 CRUD UI | "관리자 설정" 언급만 | 없음 (stub) | 데이터 계약(`extraAnswers`)만 | **형태 없음 / 존재는 wireframe·HANDOFF 모두 인정** | ⚠️ prototype 누락 = 정책 폐기 아님 (POLICY 비대칭 3원칙 1). 형태는 이번 티켓에서 신설 · admin CRUD 패턴 재활용 |
| 필드 타입 | 주관식·객관식·첨부 3종 | 없음 | 없음 | 존재 (규칙) | wireframe 채택 → 3종 (Qn-3 참조) |
| 필드 필수 여부 | "모두 필수" (원 기획) | 없음 | 없음 | 존재 (규칙) | ⚠️ Qn-2 — "관리자가 필드 별로 지정" 으로 완화 (권장) vs 원본 유지 |
| 강좌 선택 dropdown | 있음 | 없음 | 없음 | 존재 (규칙) | **이번 티켓 밖** — Course 엔티티가 A3 스코프. dynamic-fields 는 자유 dropdown 로 커버 |
| 신청 폼 통합 위치 | 언급 없음 (스코프 밖) | 3단계 위저드 (Step 2 = 지원 동기) | 없음 | 형태 | ⚠️ Qn-Δ — Step 2 병합 vs 신규 Step 4 (권장: **Step 2 병합** — 위저드 UX 유지) |

### 1-B. 데이터 모델 gap 표 (필수)

`ApplyQuestion` (신설) + `ApplyAnswer` (신설) 두 엔티티. HANDOFF 계약(`extraAnswers`)과 매핑.

| HANDOFF 필드 | 신설 엔티티·컬럼 | 조치 |
|---|---|---|
| `answers[].question` (표시용) | `ApplyQuestion.label` VARCHAR(200) | 컬럼 신설 |
| `answers[].answer` (표시용) | `ApplyAnswer.value` TEXT (주관식 · 객관식 선택값 문자열) 또는 `attachmentPath` VARCHAR(500) (첨부) | 컬럼 신설 |
| (파생) question type | `ApplyQuestion.fieldType` ENUM(`TEXT`,`DROPDOWN`,`ATTACHMENT`) | 컬럼 신설 |
| (파생) required | `ApplyQuestion.required` BOOLEAN | 컬럼 신설 |
| (파생) 정렬 | `ApplyQuestion.sortOrder` INT — ADMIN-00 §3-1 명시 | 컬럼 신설 (드래그 UI 는 이번 스코프 밖 · 숫자만) |
| (파생) 프로그램 스코프 | `ApplyQuestion.program_id` FK → program | 컬럼 신설 |
| (파생) DROPDOWN 옵션 | `ApplyQuestion.options` TEXT (JSON 배열 · Qn-4 참조) | 컬럼 신설 |
| (파생) TEXT 상한 | `ApplyQuestion.max_length` INT nullable | 컬럼 신설 |
| (파생) 활성 여부 | `ApplyQuestion.isActive` BOOLEAN | Qn-11 — 삭제 대신 비활성 flag 로 이력 보존 (권장) |
| (파생) 신청 참조 | `ApplyAnswer.application_id` FK · `ApplyAnswer.question_id` FK | 컬럼 신설 |
| `Program` 자체 필드 | 변경 없음 | — |
| `Application` 자체 필드 | 변경 없음 (`applyReason` 는 지원 동기 그대로 유지) | — |

**V11 마이그레이션 개요** (main 최신 V10 다음):
```sql
CREATE TABLE apply_question (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
  field_type VARCHAR(20) NOT NULL,  -- TEXT / DROPDOWN / ATTACHMENT
  label VARCHAR(200) NOT NULL,
  required BOOLEAN NOT NULL DEFAULT false,
  sort_order INT NOT NULL DEFAULT 1,
  options TEXT,          -- JSON 배열 (DROPDOWN 만)
  max_length INT,        -- TEXT 만
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_apply_question_program ON apply_question(program_id, is_active, sort_order);

CREATE TABLE apply_answer (
  id BIGSERIAL PRIMARY KEY,
  application_id BIGINT NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  question_id BIGINT NOT NULL REFERENCES apply_question(id),  -- 삭제 정책은 Qn-8
  value TEXT,
  attachment_path VARCHAR(500),  -- ATTACHMENT 만
  attachment_filename VARCHAR(200),
  attachment_size BIGINT,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_apply_answer_application ON apply_answer(application_id);
CREATE INDEX idx_apply_answer_question ON apply_answer(question_id);
```

**시드 데이터**: DataInitializer `seedApplyQuestions()` 신설 — **파생 시드 금지 규칙 (CLAUDE.md L376~397) 준수**: 프로그램 1개(예: `seed program #7`)에 샘플 3필드 (주관식 1 + dropdown 1 + 첨부 1). 나머지 프로그램은 dynamic field 0건 → **사용자 apply flow 회귀 방어 핵심**. `SEED_APPLY_QUESTION_COUNT` 상수 노출 (reset endpoint 참조용).

### 1-C. 데이터 소비 지점 (필수)

`ApplyQuestion` · `ApplyAnswer` 를 사용하는 모든 화면·flow 를 열거.

| 소비 지점 | 위치 | 이번 티켓 영향 | prototype 매칭 |
|---|---|---|---|
| 관리자 dynamic-fields 목록·CRUD | `AdminProgramDynamicFieldController` (신설) | **본 티켓 신설** | admin prototype 없음 → 계약 신설 (§0) |
| 관리자 프로그램 편집 폼 진입 지점 | A2/A3 미착수 → **임시로 `/admin/programs/{id}/dynamic-fields` 직접 URL** 로 열림 (Qn-9 참조) | 임시 화면. A3 착수 시 "신청" 탭에 이식 | — |
| 사용자 신청 폼 렌더 | `ApplicationController.applyForm` L37~45 → `apply.html` Step 2 | **활성 dynamic field 조회 + 렌더 로직 추가** (회귀 위험 있음) | prototype 은 dynamic 필드 없는 apply. 시드에 dynamic 없는 프로그램이 대부분이면 회귀 없음 |
| 사용자 신청 저장 | `ApplicationService.apply` L67~109 | **동적 응답 저장 로직 병행 (트랜잭션 내)** — 검증 실패 시 rollback | — |
| 사용자 신청 완료 화면 | `ApplicationController.complete` L78~107 | 변경 없음 (응답은 스토리지에 저장. complete 는 요약만) | prototype 완료 페이지 동일 |
| 관리자 신청 상세 모달 (`answers` 표시) | admin prototype.tsx L163 — **A5 admin-applications 스코프** | **이번 티켓 밖** — A5 착수 시 소비 | 이 티켓 완료 후 A5 가 read-only 로 소비 |
| 마이페이지 내 신청 상세 | `mypage` 어딘가 (현재 상세 페이지 없음) | 이번 스코프 밖 | — |

### 1-D. write→read 왕복 통합 시나리오 (필수)

- 관리자가 프로그램 A 에 dynamic field 3개 등록 (TEXT 필수 / DROPDOWN 선택 / ATTACHMENT 필수) → `GET /programs/A/apply` → Step 2 카드에 세 필드가 sort_order 순서로 렌더 → 사용자가 유효 값 입력 후 제출 → `ApplyAnswer` 3 row 저장 + `Application` 1 row 저장 → `GET /admin/programs/A/dynamic-fields` 편집 진입 시 사용법 가이드에 "현재 응답 N건" 표시 → 관리자가 필드 삭제 시도하면 응답이 있으면 400 (Qn-8-A) 또는 soft delete (Qn-8-B)
- 관리자가 dropdown options `["A","B","C"]` → 사용자가 `"Z"` 를 직접 조작해 제출 → 서버 400 "허용되지 않은 옵션이에요"
- 관리자가 TEXT max_length=100 지정 → 사용자가 200자 입력 → 서버 400 (Bean Validation)
- 관리자가 ATTACHMENT 필드에 5MB 파일 업로드 정책 (Qn-5-A 전역 승계) → 사용자가 6MB pdf 업로드 → 서버 400 "5MB 이하만 올릴 수 있어요"
- 관리자가 필드 `isActive=false` 전환 → `GET /programs/A/apply` → 해당 필드 미렌더 → 과거 응답은 `ApplyAnswer` 에 이력 그대로 (`question_id` 참조 유지)

---

## 2. 배경 · 스코프

### 포함

| URL | 메서드 | RBAC | 목적 |
|---|---|---|---|
| `/admin/programs/{programId}/dynamic-fields` | GET | SYSTEM+CENTER | 필드 목록 (프로그램 스코프) |
| `/admin/programs/{programId}/dynamic-fields/new` | GET | SYSTEM+CENTER (본인 센터) | 신규 필드 폼 |
| `/admin/programs/{programId}/dynamic-fields` | POST | SYSTEM+CENTER (본인 센터) | Create |
| `/admin/programs/{programId}/dynamic-fields/{fieldId}` | GET | SYSTEM+CENTER (본인 센터) | 편집 폼 (기존값 prefilled) |
| `/admin/programs/{programId}/dynamic-fields/{fieldId}` | POST | SYSTEM+CENTER (본인 센터) | Update |
| `/admin/programs/{programId}/dynamic-fields/{fieldId}/delete` | POST | SYSTEM+CENTER (본인 센터) | Delete (Qn-8 정책) |

**사용자 사이드 통합**:
- `ApplicationController.applyForm` — 활성 dynamic field 조회, model 에 `dynamicQuestions: List<ApplyQuestion>` 주입
- `apply.html` Step 2 — 지원 동기 textarea 아래에 dynamic 필드 렌더 (Qn-Δ 결정)
- `ApplyRequest` — `Map<Long, String> dynamicAnswers` + `Map<Long, MultipartFile> dynamicAttachments` 추가
- `ApplicationService.apply` — 동적 응답 검증 + `ApplyAnswer` 저장 (트랜잭션 내)

### 제외 (이월)

| 항목 | 이월 근거 |
|---|---|
| 관리자 프로그램 편집 폼의 "신청" 탭 통합 (Qn-9) | A3 `admin-program-form` 티켓 스코프 |
| 관리자 신청 관리 화면 (`answers` 표시) | A5 `admin-applications` 스코프 |
| 필드 순서 드래그·드롭 편집 | sortOrder 숫자 편집만 이번 스코프. 드래그는 A3 티켓 UX 통합 시 |
| 조건부 필드 (A 선택 시 B 노출) | 후속 티켓 (F0c-conditional-fields) |
| Course/강좌 dropdown | Course 엔티티는 A3 스코프 — `ApplyQuestion.fieldType=DROPDOWN` 의 free options 로 대체 가능 |
| 마이페이지 신청 상세 응답 표시 | 별도 티켓 |

---

## 3. RBAC 정책 (Qn-1)

`AdminNoticeController` 와 유사한 "조회 SYSTEM+CENTER · UD 는 자기 센터 프로그램만" 패턴 권장.

| 조건 | 판정 |
|---|---|
| `Program.organization` → `Center` FK 여부 | 현재 Program 은 `organization` VARCHAR(100) 만 존재 (Program.java L34). Center FK 없음 |
| ADMIN-00 §Q2 | "Program → Center FK 전환" 결정됨 but 미구현. **이번 티켓은 organization 문자열 매칭으로 임시 격리** 하거나 스코프를 SYSTEM_ADMIN 전용으로 좁힌다 |

**권장안 (Qn-1-A)**: SYSTEM_ADMIN 만 허용. 근거:
1. Program-Center FK 가 아직 없어 CENTER_ADMIN 격리 로직이 임시적 (organization 문자열 매칭 = fragile)
2. A3 착수 시 Center FK 도입 후 자연스럽게 CENTER_ADMIN 확장 예정
3. 파일럿 티켓 스코프 최소화

**대안 (Qn-1-B)**: SYSTEM_ADMIN + CENTER_ADMIN (자기 센터 organization 문자열 매칭). AdminNoticeService.canEdit 패턴 재활용

Controller `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` (Qn-1-A) 또는 Service `canEdit()` (Qn-1-B).

---

## 4. 엔티티 · 마이그레이션

### `program/ApplyQuestion.java` (신설)

- `@Entity @Table(name="apply_question")`
- Program 단방향 `@ManyToOne(LAZY)` (Program 엔티티는 컬렉션 추가 없음 — CLAUDE.md 규칙)
- `FieldType` enum: `TEXT`, `DROPDOWN`, `ATTACHMENT`
- `options` TEXT + Jackson `TypeReference<List<String>>` 파싱 헬퍼 도메인 메서드
- 도메인 메서드: `update(...)`, `deactivate()`, `activate()`, `validateValue(String)`, `validateOption(String)`

### `application/ApplyAnswer.java` (신설)

- Application · ApplyQuestion 단방향 `@ManyToOne(LAZY)`
- `value` TEXT (TEXT/DROPDOWN) · `attachmentPath` + `attachmentFilename` + `attachmentSize` (ATTACHMENT)
- `@CreatedDate` 만 (updatedAt 없음 — 응답 수정 UX 는 이번 스코프 밖)

### V11 마이그레이션

- 파일: `V11__create_apply_question_and_answer.sql`
- 이미 §1-B 에 DDL 초안. `main` 최신 V 확인: **V10 이 최신** — V11 로 확정

### `DataInitializer.seedApplyQuestions()`

- Program id=7 (또는 시드 프로그램 중 하나 — 착수 시 실 시드 확인) 에 3필드 부착
- `SEED_APPLY_QUESTION_COUNT = 3L` 상수 노출
- 나머지 프로그램은 0건 (회귀 방어)

---

## 5. Controller / Service / DTO 상세

### admin

- `AdminProgramDynamicFieldController` (신설, `AdminNoticeController` 패턴)
- `AdminProgramDynamicFieldService` (신설) — CRUD + Qn-8 삭제 정책
- `AdminDynamicFieldRequest` DTO — `@NotBlank label` · `@NotNull fieldType` · `required` · `sortOrder` · `options` (dropdown) · `maxLength` (TEXT)
- `AdminExceptionHandler` — 400 매핑 재활용 (`IllegalArgumentException` → flash + redirect)

### 사용자

- `ApplicationController.applyForm` — `dynamicQuestions` model 추가
- `ApplicationController.apply` — `MultipartFile[]` 파라미터 추가 (Qn-6, Qn-Δ 결정 종속)
- `ApplyRequest` — `Map<Long, String> dynamicAnswers = new HashMap<>()` + attachments 별도 파라미터 (multipart 는 @ModelAttribute 로 Map 바인딩이 까다로우니 별도 처리)
- `ApplicationService.apply` — 동적 응답 검증 + 저장. 검증 실패 시 예외 → controller 가 flash 처리
- `ApplyAnswerRepository` — `findAllByApplication(Application)`, `existsByQuestion(ApplyQuestion)`
- `ApplyQuestionRepository` — `findAllByProgramAndIsActiveTrueOrderBySortOrderAsc(Program)`

### 첨부 저장 (Qn-5)

- `FileStorage` 재활용. bucket: `apply-attachments` (신설)
- 경로: `apply-attachments/{applicationId}/{questionId}-{uuid}.{ext}` (Application 저장 후 id 확보 필요 → **응답 저장은 Application 생성 이후 별도 단계**)

---

## 6. 화면 · 템플릿 (admin)

- `admin/program-dynamic-field/list.html` — 필드 목록 (fieldType 뱃지 · required 표시 · 정렬순 · 활성)
- `admin/program-dynamic-field/form.html` — 신규·편집 폼
  - `<select name="fieldType">` (`TEXT`/`DROPDOWN`/`ATTACHMENT`)
  - `data-field-type` 기반 조건부 JS 로 옵션 필드 노출:
    - `TEXT` → `maxLength` 입력
    - `DROPDOWN` → `options` textarea (한 줄에 하나 · 서버에서 JSON 배열로 정규화)
    - `ATTACHMENT` → 정책 안내 문구 (5MB, pdf/hwp/docx/xlsx)
  - AdminExceptionHandler 400 alert

### admin POLICY 준수

- 존댓말: "등록했어요" / "삭제할까요?" / "이미 응답이 있어 삭제할 수 없어요"
- 다크 헤더 · 인디고 primary · `AdminScope.centerScopeLabel()` 렌더

---

## 7. 사용자 사이드 신청 폼 렌더 (핵심 회귀 위험)

### Qn-Δ 결정 종속 — 두 방안

**방안 A (권장): Step 2 병합** — 지원 동기 textarea 아래에 dynamic 필드 순차 렌더. 위저드 3단계 유지 (F0c-remainder 결정 존중).

**방안 B: 신규 Step 4 삽입** — 4단계 위저드로 확장. 프로그램에 dynamic 필드 없으면 Step 4 자동 skip. 위저드 아키텍처 변경 리스크 큼.

### Step 2 카드 마크업 (방안 A 기준)

```html
<div class="apply-step-card" data-step-card="2">
  <div class="apply-step-card-header">
    <span class="apply-step-card-title">추가 정보</span>
  </div>
  <!-- 기존 지원 동기 (그대로 유지) -->
  <div class="apply-extra-block">...</div>

  <!-- 신규: 관리자 dynamic 필드 (활성 것만) -->
  <div th:each="q : ${dynamicQuestions}" class="apply-dynamic-field"
       th:classappend="${q.required} ? 'is-required'">
    <label th:text="${q.label}"></label>
    <!-- TEXT -->
    <textarea th:if="${q.fieldType.name() == 'TEXT'}"
              th:name="'dynamicAnswers[' + ${q.id} + ']'"
              th:maxlength="${q.maxLength}"></textarea>
    <!-- DROPDOWN -->
    <select th:if="${q.fieldType.name() == 'DROPDOWN'}"
            th:name="'dynamicAnswers[' + ${q.id} + ']'">
      <option value="">선택해주세요</option>
      <option th:each="opt : ${q.getOptionList()}" th:value="${opt}" th:text="${opt}"></option>
    </select>
    <!-- ATTACHMENT -->
    <input th:if="${q.fieldType.name() == 'ATTACHMENT'}" type="file"
           th:name="'dynamicAttachments_' + ${q.id}"
           accept=".pdf,.hwp,.docx,.xlsx">
  </div>
</div>
```

### 회귀 방어 핵심

- 시드 프로그램 대부분에 dynamic 필드 0건 → 기존 apply.spec 계약 (Step 2 = 지원 동기만) 그대로 통과
- `dynamicQuestions.isEmpty()` 이면 렌더 skip (기존 마크업과 동일)
- form encoding 은 `multipart/form-data` 로 변경 필요 (ATTACHMENT 있을 때만) — **회귀 위험**: 기존 apply form 이 `application/x-www-form-urlencoded` 이므로 서버 파라미터 파싱 방식 변경 검증 필요
- Bean Validation 은 `dynamicQuestions` 를 controller 에서 loop 로 서비스에 위임 (`ApplyRequest` 는 @Valid 로 static 검증만)

---

## 8. 첨부 처리 (Qn-5)

- 기존 `NoticeAttachment` 정책 승계: 5MB · pdf/hwp/docx/xlsx (Qn-5-A 권장)
- 필드마다 관리자 지정은 UX 복잡도 대비 이득 낮음 (Qn-5-B)
- bucket 신설 `apply-attachments` · Storage 저장 → `ApplyAnswer.attachmentPath` 만 DB
- 저장 순서: Application 저장 → id 확보 → 파일 업로드 → ApplyAnswer 저장. 파일 업로드 실패 시 트랜잭션 롤백 (Application 도 삭제) — **`FileStorage.delete()` 는 transaction rollback hook 이 아니라 별도 정리 필요**: 서비스에서 try/catch 하고 실패 시 storage.delete 명시 호출

---

## 9. 회귀 방지 (핵심)

### 사용자 apply flow 무회귀

| 기준 | 검증 |
|---|---|
| `docs/design-contracts/apply.md` 갭 0 유지 | `npx playwright test --project=contracts --grep apply` |
| `e2e/tests/apply.spec.ts` (기능 E2E) PASS | `docs/postmortems/2026-09-03-e2e-flaky-triangulation.md` 참고. A/B/backdrop 3층 회귀 재발 방지 |
| 시드 프로그램 대부분 dynamic field 0건 | DataInitializer 시드 프로그램 중 1개만 dynamic 부착 |
| form encoding 변경 검증 | multipart 로 전환 시 기존 CSRF · flash · redirect 동작 유지 |
| ApplyRequest DTO 필드 유지 | applyReason · privacyAgreed 기존 필드 그대로 |

### admin CRUD 실효성

- 관리자가 fieldType 변경 시 기존 응답 처리 (Qn-8 참조)
- 재기동 시 시드가 편집값 덮어쓰지 않는가? (`existsByProgramAndLabel` idempotent 체크)

---

## 10. 테스트

### 정적

- `AdminProgramDynamicFieldControllerTest` (@WebMvcTest + @WithMockUser) — CRUD 각 endpoint · RBAC 403
- `AdminProgramDynamicFieldServiceTest` — fieldType 별 validation · 옵션 파싱 · 삭제 정책
- `ApplyQuestionRepositoryTest` (@DataJpaTest) — `findAllByProgramAndIsActiveTrueOrderBySortOrderAsc` · sortOrder 정렬
- `ApplicationServiceTest` — dynamic 응답 저장 · 응답 검증 실패 시 rollback
- `ApplyRequestValidationTest` — Bean Validation TC
- `JpaMappingTest` — 신규 엔티티 매핑

### 동적 (curl)

- `POST /admin/programs/1/dynamic-fields` 302 + `GET /admin/programs/1/dynamic-fields` 목록 노출
- `GET /programs/{id}/apply` 렌더 확인 (dynamic 필드 유·무 시나리오 2건)
- `POST /programs/{id}/apply` multipart 정상 흐름 + 검증 실패 흐름

### 계약 신설

- `docs/design-contracts/admin/program-dynamic-fields.md` — 목록/폼 정책 (admin POLICY 승계)
- `e2e/contracts/admin-dynamic-field.ts` — 셀렉터·정량값

### 기능 E2E

- `e2e/tests/admin-dynamic-field-list.spec.ts`
- `e2e/tests/admin-dynamic-field-form.spec.ts` (CRUD 왕복)
- `e2e/tests/admin-dynamic-field-rbac.spec.ts`
- `e2e/tests/apply-dynamic-response.spec.ts` — write→read 왕복 (관리자 필드 등록 → 사용자 신청 → admin 이 응답 존재 확인)
- **회귀**: 기존 `apply.spec.ts` · `signup.spec.ts` 무회귀

### reset endpoint (Qn-7)

- `POST /__test__/reset-apply-questions` — `id > SEED_APPLY_QUESTION_COUNT` 삭제 (`apply_answer` 먼저 CASCADE 로 정리)
- `TestFixtureController` 에 `resetApplyQuestions()` 추가 (기존 패턴 재활용)

---

## 11. 결정 필요 항목 (Qn — 사용자 대기)

| # | 질문 | 선택지 | 권장 |
|---|---|---|---|
| **Qn-1** | RBAC 정책 | A. SYSTEM_ADMIN 만 (Program-Center FK 미완이라 격리 fragile) / B. SYSTEM+CENTER 조회+자기 organization UD | **A** — A3 에서 Center FK 도입 후 자연스러운 확장 |
| **Qn-2** | 필드 필수 여부 | A. 관리자가 필드별 지정 (권장) / B. wireframe 원본 "모두 필수" 하드 정책 | **A** |
| **Qn-3** | 필드 타입 | A. TEXT/DROPDOWN/ATTACHMENT 3종 (권장) / B. NUMBER/DATE/CHECKBOX 확장 | **A** — wireframe 원 기획 정합 |
| **Qn-4** | DROPDOWN 옵션 저장 | A. JSON in column (권장 — 조회 단순) / B. 별도 `apply_question_option` 테이블 | **A** |
| **Qn-5** | 첨부 정책 | A. 전역 승계 (5MB · pdf/hwp/docx/xlsx) 권장 / B. 필드마다 관리자 지정 | **A** |
| **Qn-6** | 응답 저장 방식 | A. `ApplyAnswer` row per (application×question) 권장 — ADMIN-00 §3-3 계약 / B. `Application.dynamicResponsesJson` blob | **A** — 인덱스·집계 가능 |
| **Qn-7** | reset endpoint 신설 | A. 신설 (`POST /__test__/reset-apply-questions`) 권장 / B. 미신설 (기존 reset-applications 로 충분) | **A** |
| **Qn-8** | 필드 삭제 시 응답 이력 처리 | A. FK 참조 있으면 400 (권장 — 이력 보존) / B. cascade 삭제 / C. soft delete (`isActive=false`) 만 허용 | **C** — 삭제 UI 는 `isActive=false` 전환으로 대체 |
| **Qn-9** | 관리자 화면 진입 지점 | A. `/admin/programs/{id}/dynamic-fields` 별도 페이지 (권장 — A3 미착수) / B. 프로그램 편집 페이지 인라인 섹션 (A3 통합) | **A** — A3 착수 시 인라인으로 이식 |
| **Qn-10** | 시드 데이터 프로그램 | A. seed program #7 에만 3필드 (권장 · 회귀 방어) / B. 전 프로그램에 샘플 부착 (회귀 위험) | **A** |
| **Qn-Δ** | 신청 폼 통합 방식 | A. Step 2 병합 (권장 · 위저드 3단계 유지) / B. 신규 Step 4 삽입 (4단계) / C. 조건부 4단계 (dynamic 있을 때만) | **A** |
| **Qn-11** | form encoding | A. dynamic 필드에 ATTACHMENT 없으면 기존 `application/x-www-form-urlencoded` 유지 · ATTACHMENT 있으면 multipart 전환 / B. 항상 multipart | **B** — 조건부 encoding 은 유지비용 큼. 항상 multipart 로 통일 (회귀 검증 폭 축소) |

---

## 12. ADR 변경 판정

`docs/adr/admin-track-roadmap-2026-09.md` §2 Q4-A 는 "F0c-dynamic-fields·F4 는 A3 에 흡수" 라고 결정했다. 지금 이 spec 은 **A3 에 흡수하지 않고 파생 큐 세 번째로 분리**해서 진행하는 방향이다.

**재판정 근거**:
1. A3 는 프로그램 폼 전체 (정보 · 신청 · 약관 3탭 + 에디터 + Course + ProgramAttachment + ProgramTerms + Center FK 전환) 로 스코프가 크다. 파일럿 이후 대형 PR 이 될 가능성이 높다
2. dynamic-fields 만 먼저 분리하면 사용자 F0c 소비 지점 (apply.html Step 2) 이 즉시 활성화 — 원 기획서 (wireframe) 의 "관리자 설정 동적" 요구 조기 충족
3. A3 착수 시점에 dynamic-fields UI 를 프로그램 편집 폼 "신청" 탭에 이식하는 리팩터가 소형 PR 로 가능 (컨트롤러·서비스·엔티티 재사용)

**이 spec 채택 시 ADR 반영 방법**: `docs/adr/admin-track-roadmap-2026-09.md` §2 Q4-A 항목에 "**dynamic-fields 는 A3 에 흡수하지 않고 파생 큐 3번째로 선행 분리** (F0c-dynamic-fields spec 2026-09-07)" 각주 추가.

---

## 13. 스코프 예상 규모

| 항목 | 규모 |
|---|---|
| 파일 수 | 18~22개 (엔티티 2 + Repository 2 + Controller 1 + Service 1 + DTO 1 + templates 2 + V11 SQL 1 + 사용자측 통합 apply.html + ApplyRequest 수정 + ApplicationService 수정 + DataInitializer + 테스트 6~8개 + 계약 2개) |
| 순증 LOC | 1,400~1,800 |
| Assertion | 정적 40~50건 · 계약 25~35건 · E2E 8~10 spec |
| 리스크 (높음) | 사용자 apply flow 회귀 (multipart 전환 · Step 2 마크업 변경) |
| 리스크 (중) | Qn-8 soft delete 정책 결정 시 관리자 UX (진짜 삭제 못하는 게 낯설 수 있음) |
| 리스크 (낮음) | Qn-1 A 시 CENTER_ADMIN 이 접근 못함 → A3 착수 전까지 SYSTEM_ADMIN 만 CRUD |

---

## 14. deferred / deviation

### deferred

| 항목 | 이월처 |
|---|---|
| 관리자 프로그램 편집 폼 "신청" 탭 통합 | A3 `admin-program-form` |
| 관리자 신청 관리 화면 응답 표시 (HANDOFF `answers`) | A5 `admin-applications` |
| 필드 드래그 정렬 UI | A3 통합 시 |
| Course 강좌 dropdown | A3 (Course 엔티티 신설) |
| 조건부 필드 | F0c-conditional-fields (미큐) |
| 마이페이지 신청 상세 응답 조회 | 별도 티켓 |
| Program-Center FK 전환 (ADMIN-00 Q2) | A3 |

### deviation

| 항목 | 사유 |
|---|---|
| admin prototype 부재 → 계약 신설 | POLICY 비대칭 3원칙 1 (prototype 누락 = 정책 폐기 아님). notice·terms 계약 신설 근거 그대로 |
| Program 엔티티에 `@OneToMany` 컬렉션 미추가 | CLAUDE.md 엔티티 규칙 (단방향 `@ManyToOne` 원칙) |
| Qn-8-C (soft delete) 채택 시 진짜 delete endpoint 없음 | admin CRUD 실효성 vs 이력 보존 트레이드오프. 이력 보존 우선 |

---

## 다음 단계 인계

명세 산출 완료. Qn-1 ~ Qn-11 (총 11개 결정 항목) 사용자 결정 이후 `ym-impl` 인계 가능.
사용자 결정 이후 spec 상태를 `spec_confirmed` 로 갱신하고 §11 표에 채택 안 표기 (A-admin-terms-crud 헤더 형식 참조).
