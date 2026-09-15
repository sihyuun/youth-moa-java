# admin/users — 관리자 사용자 관리 목록 계약

| 항목 | 값 |
|---|---|
| 화면 | `/admin/users` |
| 계약 파일 | [e2e/contracts/admin-users.ts](../../../e2e/contracts/admin-users.ts) |
| 원본 | `docs/00_assets/admin/prototype.html` L1166~1272 |
| 착수 티켓 | A5-admin-users (2026-09-15) |
| spec | `docs/specs/A5-admin-users.md` |

## 공통 정책

- 다크 헤더 (admin/fragments/header.html) + 인디고 primary (#3F30E9)
- 존댓말 톤
- Qn-A 이월 결정: `/admin/staff` 별도 화면 없음 · `/admin/users?role=…` 필터로 통합
- Qn-9 이월 결정: bulk selection · CSV export UI 제거 (A8 착수 시 재도입)

## prototype 인용 요약

| 라인 | 내용 |
|---|---|
| L1171~1196 | 액션 바 — 검색 입력 (placeholder "이름, 이메일 검색") + role filter 4옵션 (전체 · 시스템 관리자 · 관리자 · 사용자) |
| L1214~1228 | 테이블 헤더 10컬럼 — checkbox · No · 이름 · 이메일 · 성별 · 권한 · 핸드폰 · 가입일 · 최근접속일 · 상태 |
| L1230~1244 | 행 반복 — 이름 클릭 → 상세 (`#3F30E9` 링크색), role badge (roleCfg), status badge |
| L1248~1257 | Empty state — "검색 결과가 없어요" |
| L1259~1270 | 페이지네이션 — 활성 `#3F30E9`, 좌우 화살표 |

## 이월/이탈 요약

| 항목 | prototype | 채택 | 사유 |
|---|---|---|---|
| checkbox 컬럼 | 있음 | **제거** | Qn-9 이월 → A8 |
| CSV 내보내기 버튼 | 있음 | **제거** | Qn-9 이월 → A8 |
| bulk selection 다크바 | 있음 | **제거** | Qn-9 이월 → A8 |
| "등록하기" 버튼 | 있음 | **제거** (이번 스코프) | Qn-4 이월 = 기존 사용자 승격 flow 만 유지 → A5-1 |

## 구현 매핑

| 계약 행 | 구현 위치 |
|---|---|
| GNB "사용자 관리" active | `templates/admin/fragments/header.html` currentPage="users" |
| 페이지 타이틀 | `templates/admin/user/list.html:27` |
| 검색 인풋 placeholder | `templates/admin/user/list.html:44-46` |
| role filter 4탭 | `templates/admin/user/list.html:52-61` |
| 컬럼 헤더 9열 | `templates/admin/user/list.html:69-79` |
| 행 렌더 | `templates/admin/user/list.html:86-112` |
| Empty state | `templates/admin/user/list.html:81-84` |
| 페이지네이션 | `templates/admin/user/list.html:116-130` |
| Role badge 스타일 | `static/css/admin.css` `.admin-role-badge--system_admin/--center_admin/--user` |
| Status badge 스타일 | `static/css/admin.css` `.admin-user-status-badge--active/--inactive` |

## 검증 방식

- 계약 검사: `npx playwright test --project=contracts admin-users`
- 기능 E2E: `admin-users.spec.ts`
- 정적: `AdminUserListRenderTest`
