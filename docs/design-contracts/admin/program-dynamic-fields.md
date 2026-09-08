# admin/program-dynamic-fields — 관리자 동적 신청 필드 관리 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/programs/{programId}/dynamic-fields` (목록), `/new` (신규), `/{fieldId}` (편집) |
| 계약 파일 | [e2e/contracts/admin-dynamic-field.ts](../../../e2e/contracts/admin-dynamic-field.ts) |
| 원본 | admin prototype 부재 — POLICY + admin-notice / admin-term 계약 승계 |
| 착수 티켓 | F0c-dynamic-fields (2026-09-08) |
| 명세 | [docs/specs/F0c-dynamic-fields.md](../../specs/F0c-dynamic-fields.md) |

## 공통 정책

- 다크 헤더 (admin/fragments/header.html) + 인디고 primary (#3F30E9)
- 존댓말 톤 ("…했어요/됐어요")
- 파괴적 액션 (**비활성 처리**) 은 커스텀 confirm 모달 1단계. hard delete 는 절대 신설하지 않음 (Qn-8 C)

## 화면 구성

### 목록 `/admin/programs/{programId}/dynamic-fields`
- 상단: 페이지 타이틀 + 프로그램명 표시 + "+ 신규 등록" 버튼
- 테이블: 번호 · 정렬 · 타입 pill (TEXT/DROPDOWN/ATTACHMENT) · 라벨(링크) · 필수 · 상태 · 편집
- 정렬: `sort_order ASC, id ASC`
- 비활성 필드는 `.admin-dynamic-field-row--inactive` 회색 처리 (표시는 유지)

### 신규 등록 `/admin/programs/{programId}/dynamic-fields/new`
- 필드: fieldType(select 3옵션) · label(200) · sortOrder(1~999) · isRequired · maxLength(TEXT 전용) · options(DROPDOWN 전용, 한 줄에 하나)
- 등록 성공 시 `/admin/programs/{programId}/dynamic-fields/{new_id}` 리다이렉트 (PRG)
- 타입별 JS 조건부 표시: TEXT 선택 → maxLength row, DROPDOWN → options row, ATTACHMENT → 정책 안내 문구

### 편집 `/admin/programs/{programId}/dynamic-fields/{fieldId}`
- 신규 폼과 동일 필드 + prefilled
- 하단 액션 영역:
  - 활성 필드: **"비활성 처리"** 버튼 → confirm 모달 → POST `/deactivate`
  - 비활성 필드: **"활성 복구"** 버튼 → POST `/reactivate`
  - **hard delete 버튼 없음** (Qn-8 C, 응답 이력 보존)
- 응답 이력 개수 표시: "현재 응답 N건" (편집 시 안내)

## RBAC (Qn-1 A)

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN |
|---|---|---|
| GET /admin/programs/{id}/dynamic-fields | ✓ | 403 |
| GET /admin/programs/{id}/dynamic-fields/new | ✓ | 403 |
| POST /admin/programs/{id}/dynamic-fields | ✓ | 403 |
| POST /admin/programs/{id}/dynamic-fields/{fid} | ✓ | 403 |
| POST /admin/programs/{id}/dynamic-fields/{fid}/deactivate | ✓ | 403 |

구현: 컨트롤러 클래스 레벨 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`.

CENTER_ADMIN 은 A3 (Program-Center FK 도입) 이후 확장 예정.

## 결정 요약 (§11 spec)

| Qn | 결정 |
|---|---|
| Qn-1 | **A** — SYSTEM_ADMIN 만 |
| Qn-2 | **A** — isRequired 필드별 관리자 지정 |
| Qn-3 | **A** — TEXT / DROPDOWN / ATTACHMENT 3종 |
| Qn-4 | **A** — options JSON in column |
| Qn-5 | **A** — 전역 승계 (5MB · pdf/hwp/docx/xlsx) |
| Qn-6 | **A** — ApplyAnswer row per (application × question) |
| Qn-7 | **A** — `POST /__test__/reset-apply-questions` 신설 |
| Qn-8 | **C** — soft delete only (`isActive=false`) |
| Qn-9 | **A** — `/admin/programs/{id}/dynamic-fields` 별도 페이지 |
| Qn-10 | **A** — seed program #7 에만 3필드 |
| Qn-Δ | **A** — 사용자 apply.html Step 2 병합 (위저드 3단계 유지) |
| Qn-11 | **B** — 항상 multipart |

## 사용자 apply flow 회귀 방지

- 시드 프로그램 #7 외 나머지 프로그램은 dynamic 필드 0건 → `th:each` 자연스럽게 skip
- form encoding 은 항상 `multipart/form-data` (기존 apply.spec 무회귀 확인)
- Bean Validation 대신 `ApplicationService.validateDynamicAnswers()` 로 필드별 검증 → IllegalArgumentException → controller flash 처리

## 이월 항목

- 관리자 프로그램 편집 폼 "신청" 탭 통합 → A3 `admin-program-form`
- 관리자 신청 관리 화면 응답 표시 (HANDOFF `answers`) → A5 `admin-applications`
- 필드 드래그 정렬 UI → A3 통합 시
- Course 강좌 dropdown → A3 (Course 엔티티 신설)
- 조건부 필드 (A 선택 시 B 노출) → F0c-conditional-fields (미큐)
- 마이페이지 신청 상세 응답 조회 → 별도 티켓

## 계약 assertion 요약

- list: 10건 · form: 9건 · edit: 5건 = **24건**

## 구현 매핑

| §7 spec 항목 | 코드 파일:라인 |
|---|---|
| ApplyQuestion 엔티티 (§4) | `program/ApplyQuestion.java` |
| ApplyAnswer 엔티티 (§4) | `application/ApplyAnswer.java` |
| V11 마이그레이션 (§4) | `db/migration/V11__apply_questions_and_answers.sql` |
| ApplyQuestionRepository (§5) | `program/ApplyQuestionRepository.java` |
| ApplyAnswerRepository (§5) | `application/ApplyAnswerRepository.java` |
| AdminApplyQuestionService (§5) | `admin/AdminApplyQuestionService.java` |
| AdminApplyQuestionController (§5) | `admin/AdminApplyQuestionController.java` |
| admin/list.html (§6) | `templates/admin/program-dynamic-field/list.html` |
| admin/form.html (§6, TEXT/DROPDOWN/ATTACHMENT 조건부 JS) | `templates/admin/program-dynamic-field/form.html` |
| apply.html Step 2 병합 (§7 Qn-Δ A) | `templates/application/apply.html` |
| ApplicationController multipart 추출 (§7) | `application/ApplicationController.java:extractAttachments` |
| ApplicationService.apply(userEmail, programId, request, attachments) (§7 · §8) | `application/ApplicationService.java` |
| Qn-5 A 첨부 검증 (5MB · 확장자) | `ApplicationService.validateAttachment` |
| TestFixtureController.resetApplyQuestions (§7 Qn-7 A) | `test/TestFixtureController.java` |
| SEED_APPLY_QUESTION_COUNT 상수 | `common/DataInitializer.java` |
| Qn-8 C soft delete (activate/deactivate) | `program/ApplyQuestion.java:deactivate` · `AdminApplyQuestionService.deactivate` |
