# admin/notice-management — 관리자 공지 관리 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/notices` (목록), `/admin/notices/new` (신규), `/admin/notices/{id}` (편집) |
| 계약 파일 | [e2e/contracts/admin-notice.ts](../../../e2e/contracts/admin-notice.ts) |
| 원본 | admin prototype 부재 (spec §0) — POLICY + 사용자 사이드 notice/list.detail 톤 근거 신설 |
| 착수 티켓 | A-admin-notice-attachment (2026-09-03) |

## 공통 정책

- 다크 헤더 (admin/fragments/header.html) + 인디고 primary (#3F30E9)
- 존댓말 톤 ("…했어요/됐어요")
- 파괴적 액션 (삭제) 은 커스텀 confirm 모달 1단계 (Qn-9 A)

## 화면 구성

### 목록 `/admin/notices`
- 상단: 페이지 타이틀 "공지 관리" + "+ 신규 등록" 버튼
- 테이블: 번호 · 분류 pill · 제목(링크) · 작성자 · 등록일 · 관리(편집)
- 페이징 5개 단위, 페이지당 20건 (ADMIN_PAGE_SIZE)
- 정렬: pinned DESC, id DESC (핀 상단 후 최신)

### 신규 등록 `/admin/notices/new`
- 폼: title(required, 255자), content(required, textarea), category(select), isPinned(checkbox), imageUrl
- 등록 성공 시 `/admin/notices/{new_id}` (편집 화면) 로 리다이렉트

### 편집 `/admin/notices/{id}`
- 신규 폼과 동일 필드 + prefilled
- 첨부 섹션 (`_attachments-fragment.html`) 노출
  - 업로드 폼: multipart, accept="pdf,hwp,docx,xlsx", 5MB 상한
  - 첨부 목록: 파일명·크기·삭제 버튼 (canEdit true 만)
  - HTMX outerHTML swap 으로 부분 갱신 (Qn-7 A)
- 삭제 버튼 (canEdit true 만) → 커스텀 confirm 모달 → POST `/admin/notices/{id}/delete`
- CENTER_ADMIN 이 남의 공지 진입 시 `.admin-notice-forbidden-banner` 안내 + fieldset disabled

## RBAC (Qn-8 Custom, 사용자 확정)

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN |
|---|---|---|
| Create / Read | ✅ | ✅ |
| Update / Delete / 첨부 CRUD | ✅ | ⚠️ 본인 작성만 |

## 구현 매핑

| 계약 행 | 구현 위치 |
|---|---|
| GNB "공지 관리" active | `templates/admin/fragments/header.html:44-45` |
| 신규 버튼 | `templates/admin/notice/list.html:30` |
| 목록 테이블 헤더 | `templates/admin/notice/list.html:36-42` |
| 목록 행 12건 (시드) | `DataInitializer.seedNotices()` |
| 신규 폼 필드 | `templates/admin/notice/form.html:47-91` |
| 편집 폼 prefilled | 동일 파일, `th:value="${notice.title}"` |
| 첨부 fragment | `templates/admin/notice/_attachments-fragment.html:8` (root id) |
| 업로드 폼 | 동일 fragment L37 (hx-post + multipart) |
| 삭제 확인 모달 | `templates/admin/notice/form.html:104-124` |

## 검증 방식

- 계약 검사: `npx playwright test --project=contracts admin-notice-*`
- 기능 E2E: `admin-notice-{list,form,upload,rbac}.spec.ts`
- 정적: `AdminNoticeServiceTest` (RBAC 매트릭스 + 업로드 검증), `LocalFileStorageTest` (round-trip)
