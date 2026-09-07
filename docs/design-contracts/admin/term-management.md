# admin/term-management — 관리자 약관 관리 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/terms` (목록), `/admin/terms/new` (신규), `/admin/terms/{id}` (편집) |
| 계약 파일 | [e2e/contracts/admin-term.ts](../../../e2e/contracts/admin-term.ts) |
| 원본 | admin prototype 부재 (spec §0) — POLICY + admin-notice-management 계약 승계 |
| 착수 티켓 | A-admin-terms-crud (2026-09-04) |
| 명세 | [docs/specs/A-admin-terms-crud.md](../../specs/A-admin-terms-crud.md) |

## 공통 정책

- 다크 헤더 (admin/fragments/header.html) + 인디고 primary (#3F30E9)
- 존댓말 톤 ("…했어요/됐어요")
- 파괴적 액션 (삭제) 은 커스텀 confirm 모달 1단계

## 화면 구성

### 목록 `/admin/terms`
- 상단: 페이지 타이틀 "약관 관리" + "+ 신규 등록" 버튼 (SYSTEM_ADMIN 만)
- 상태 배너:
  - 활성 필수 약관 0건 → 경고 배너 (`.admin-term-empty-banner`) "회원가입 시 약관 동의 절차가 노출되지 않아요"
  - 활성 필수 약관 1건 이상 → 요약 배지 (`.admin-term-summary`) "활성 필수 약관 N건"
- 테이블: 번호 · Code pill · 제목(링크) · 필수 · 정렬 · 버전 · 상태 pill · 등록일 · 관리(편집)
- 정렬: `sortOrder ASC, id ASC`

### 신규 등록 `/admin/terms/new`
- 필드: code(A-Z_만, unique), title(100자), contentPath(/시작, 200자), content(TEXT HTML), sortOrder(1~999), required, isActive
- 등록 성공 시 `/admin/terms/{new_id}` 리다이렉트 (PRG · Qn-7 A)
- content 저장 전 OWASP HTML sanitizer 적용 (`<script>` · 이벤트 핸들러 제거)

### 편집 `/admin/terms/{id}`
- 신규 폼과 동일 필드 + prefilled
- **code readonly** (Qn-9 A) — UserAgreement FK 무결성 위험
- **버전 자동 증가 체크박스** (Qn-6 A) — 체크 시만 version++
- 삭제 버튼 (SYSTEM_ADMIN 만) → confirm 모달 → POST `/admin/terms/{id}/delete`
- FK 참조(UserAgreement) 있으면 400 + "회원 동의 이력이 N건 있어 삭제할 수 없어요. 비활성으로 처리해주세요." 안내

## RBAC (Qn-1 B, 사용자 확정)

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN |
|---|---|---|
| GET /admin/terms | ✓ | ✓ (조회 전용) |
| GET /admin/terms/{id} | ✓ | ✓ (수정 불가 배너 노출) |
| GET /admin/terms/new | ✓ | 403 |
| POST /admin/terms | ✓ | 403 |
| POST /admin/terms/{id} | ✓ | 403 |
| POST /admin/terms/{id}/delete | ✓ | 403 |

구현: 컨트롤러 UD endpoint 에 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`. 조회는 SecurityConfig `/admin/**` 매처의 `hasAnyRole` 로 커버.

## 결정 요약

| Qn | 결정 |
|---|---|
| Qn-1 | **B** — SYSTEM/CENTER 조회 · SYSTEM UD only |
| Qn-2 | **B** — content DB TEXT 저장 + OWASP HTML sanitizer + 임시 textarea |
| Qn-3 | **A** — hard delete + FK 있으면 400 |
| Qn-4 | **A** — 활성 약관 0건 허용 (signup empty state) |
| Qn-5 | **A** — in-place update + version++ (UNIQUE code 유지) |
| Qn-6 | **A** — "이번 수정은 개정입니다" 체크박스 |
| Qn-7 | **A** — PRG redirect |
| Qn-8 | **A** — `POST /__test__/reset-terms` 신설 |
| Qn-9 | **A** — 편집 모드 code readonly |

## 사용자 signup flow 회귀 방지

- signup 화면 각 약관 row 에 `data-term-content` 로 term.content embed
- 모달 JS 는 DB embed 우선, 없으면 legacy contentPath fetch fallback
- signup GET → 200 유지, 활성 약관 0건일 때도 empty state 로 렌더 성공

## 이월 항목

- content 편집 UX (WYSIWYG) — `A-admin-terms-crud-ux-polish` 별도 티켓
- 개정 시 회원 자동 재동의 요청 — `A-terms-re-agreement`
- 회원별 동의 이력 관리 화면 — A6 이후

## 계약 assertion 요약

- list: 9건 · form: 10건 · edit: 6건 = **25건**
