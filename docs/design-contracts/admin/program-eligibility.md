# admin/program-eligibility — 관리자 프로그램 자격요건 편집 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/programs/{programId}/eligibility` (편집 폼) |
| 계약 파일 | [e2e/contracts/admin-eligibility.ts](../../../e2e/contracts/admin-eligibility.ts) |
| 원본 | admin prototype 부재 — POLICY + admin-notice/term/dynamic-field 계약 승계 |
| 착수 티켓 | F4-admin-eligibility (2026-09-09) |
| 명세 | [docs/specs/F4-admin-eligibility.md](../../specs/F4-admin-eligibility.md) |

## 공통 정책

- 다크 헤더 (admin/fragments/header.html) + 인디고 primary (#3F30E9)
- 존댓말 톤 ("…해주세요 / 저장했어요")
- 스키마 변경 없음 — `ProgramEligibility` @Embeddable 은 F4-detail (PR #73) 에서 이미 신설
- 파괴적 액션 (`삭제`) 은 3필드 공란 저장으로 대체 (Qn-2 A) — hard delete endpoint 없음

## 화면 구성

### 편집 폼 `/admin/programs/{programId}/eligibility`

- 상단: 페이지 타이틀 "자격요건 편집" + 프로그램명 + "프로그램 상세 보기" 링크 (target=_blank)
- 필드 3종:
  - `age` — `input[type=text]` `maxlength=100` · placeholder "예: 만 19세 ~ 39세 청년"
  - `region` — `input[type=text]` `maxlength=100` · placeholder "예: 경기도 거주 또는 활동 중인 청년"
  - `etc` — `textarea[rows=4]` `maxlength=200` · placeholder "예: 전 회차 참석 가능자 우대"
- 각 필드 하단에 도움말 문구 (공란 시 사용자 화면 기본 문구 안내)
- 저장 성공 시 flash 배너 노출 "자격요건을 저장했어요."
- 액션: `저장` 버튼 (primary) + `취소` 링크 (outline, 프로그램 상세로 이동)

## RBAC (Qn-1 A)

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN |
|---|---|---|
| GET /admin/programs/{id}/eligibility | ✓ | 403 |
| POST /admin/programs/{id}/eligibility | ✓ | 403 |

구현: 컨트롤러 클래스 레벨 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`. CENTER_ADMIN 은 A3 (Program-Center FK) 이후 확장.

## 결정 요약 (§11 spec)

| Qn | 결정 |
|---|---|
| Qn-1 | **A** — SYSTEM_ADMIN 만 |
| Qn-2 | **A** — 3필드 공란 저장 = 삭제 (별도 endpoint 없음) |
| Qn-3 | **A** — 3필드 고정 (연령·거주지·기타) |
| Qn-4 | **A** — 엔티티 length 승계 (100/100/200) |
| Qn-5 | **A** — 별도 페이지 (A3 착수 시 인라인 이식) |
| Qn-6 | **A** — reset endpoint 미신설 (스키마 변경 없음) |
| Qn-7 | **A** — UX 톤 "…해주세요" |
| Qn-8 | **A** — PRG redirect |

## 사용자 프로그램 상세 회귀 방지

- `templates/program/detail.html` L184~236 3-grid 카드 렌더 무회귀
- `eligibility` null-safe 바인딩 유지 — 3필드 공란 저장 시 기본 문구 노출 (연령 제한 없음 / 거주지 제한 없음 / 별도 조건 없음)
- 시드 12건 자격요건 값 보존 (DataInitializer idempotent)

## 이월 항목

- 관리자 프로그램 편집 폼 "정보" 탭 통합 → A3 `admin-program-form`
- CENTER_ADMIN 접근 확장 → A3 (Program-Center FK)
- 자격요건 자동 매칭 (사용자 신청 자격 검증) → 별도 티켓
- 카테고리 다중 row 확장 → 별도 티켓
- 프로그램 목록 자격요건 기반 검색·필터 → A2/A3 이후

## 계약 assertion 요약

- form: **11건**

## 구현 매핑

| §11 spec 항목 | 코드 파일:라인 |
|---|---|
| ProgramEligibility @Embeddable (§4) | `program/ProgramEligibility.java` (PR #73 재활용) |
| Program.eligibility @Embedded (§4) | `program/Program.java:62` (기존) |
| Program.update(...) 재활용 | `program/Program.java:104` (자격요건만 교체) |
| AdminProgramEligibilityService (§5) | `admin/AdminProgramEligibilityService.java` |
| AdminProgramEligibilityController (§5) | `admin/AdminProgramEligibilityController.java` |
| Qn-1 A @PreAuthorize | `AdminProgramEligibilityController.java` 클래스 레벨 |
| Qn-2 A 공란=삭제 로직 | `AdminProgramEligibilityService.update` (null 처리) |
| Qn-4 A 검증 (100/100/200) | `AdminProgramEligibilityService.validate` |
| Qn-7 A UX 톤 | 검증 메시지 "…해주세요" |
| Qn-8 A PRG redirect + flash | `AdminProgramEligibilityController.update` RedirectAttributes |
| admin/form.html (§6) | `templates/admin/program-eligibility/form.html` |
| CSS 재활용 (셀렉터 병기) | `static/css/admin.css` (`.admin-eligibility-*` selector list) |
| AdminExceptionHandler 재활용 | 기존 `admin/AdminExceptionHandler.java` |
| 사용자 사이드 렌더 (회귀 방지) | `templates/program/detail.html:184~236` (변경 없음) |
