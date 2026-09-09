# 디자인 계약 — 관리자 프로그램 관리 (목록 · 상세)

| 메타 | 값 |
|---|---|
| 스크린 | `admin-program-list` (`/admin/programs`) · `admin-program-detail` (`/admin/programs/{id}`) |
| 담당 스펙 | [`docs/specs/A2-admin-programs-list.md`](../../specs/A2-admin-programs-list.md) |
| 계약 파일 | [`e2e/contracts/admin-programs.ts`](../../../e2e/contracts/admin-programs.ts) |
| 검사 spec | [`e2e/tests/visual-admin-programs.spec.ts`](../../../e2e/tests/visual-admin-programs.spec.ts) |
| 출처 | admin POLICY + prototype.html L907~1033 (사용자 사이드 PROGRAMS) + spec §3 |

## 아키텍처

- **SSR + 페이지네이션 (10건)** — HTMX 미사용. 필터/검색/페이지 전환은 모두 URL 쿼리스트링 (`?q=...&status=OPEN&page=1`)
- **JpaSpecificationExecutor** — `AdminProgramService.list()` 가 scope · keyword · status 3개 Spec 조합
- **RBAC** — `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")` 클래스 레벨 + `AdminScope.effectiveCenterName()` 강제
- **N+1 방지** — 목록 렌더 시 `applicationRepository.countByProgramIdsAndStatuses` 배치 조회 1회

## 상태머신 (필터 → URL 쿼리스트링)

| 사용자 액션 | URL 변화 |
|---|---|
| 초기 진입 | `/admin/programs` (page=0, status="", q="") |
| 필터 클릭 | `/admin/programs?q=<유지>&status=<선택>` (page 초기화) |
| 검색 submit | `/admin/programs?q=<입력>&status=<유지>` (page 초기화) |
| 페이지 이동 | 현재 q/status 유지 + `page=<n>` |
| 상세 이동 | `/admin/programs/{id}` |

## CTA 라우팅 표

| CTA | 위치 | 동작 (A2) | 담당 티켓 |
|---|---|---|---|
| `+ 프로그램 등록` | 목록 헤더 | disabled + tooltip | A3 |
| 필터 탭 5종 | 목록 액션바 | `?status=<enum>` 이동 | — |
| 검색 submit | 목록 액션바 | `?q=<입력>` 이동 | — |
| 프로그램명 링크 | 목록 row | 상세 페이지 이동 | — |
| `편집` (row) | 목록 row 액션 | 상세 페이지 이동 (실 편집은 A3) | A3 |
| `사용자 화면 미리보기 ↗` | 상세 헤더 | 새 창 `/programs/{id}` | — |
| `편집` (상세 헤더) | 상세 헤더 | disabled + tooltip | A3 |
| `삭제` (상세 헤더) | 상세 헤더 | disabled + tooltip | A3 |
| `자격요건 편집` | 상세 하위 관리 | `/admin/programs/{id}/eligibility` (F4) | — |
| `동적 필드 관리` | 상세 하위 관리 | `/admin/programs/{id}/dynamic-fields` (F0c) | — |
| `신청 현황 보기` | 상세 하위 관리 | disabled + tooltip | A4 |

## 컬럼 명세 (9종 — 목록)

| # | 컬럼 | 소스 | 비고 |
|---|---|---|---|
| 1 | 프로그램명 (썸네일 + 텍스트) | `Program.title` · `imageUrl` | 프로그램명 클릭 시 상세 이동 |
| 2 | 카테고리 | `Program.category` | null → "-" |
| 3 | 청년센터 | `Program.organization` | — |
| 4 | 상태 | `Program.getStatus()` | 뱃지 (open/upcoming/ended/suspended) |
| 5 | 신청기간 | — | **A3 이월** — "-" 표시 |
| 6 | 신청현황 | `applied / capacity` | 배치 조회 |
| 7 | 조회수 | — | **A6 이월** — "-" 표시 |
| 8 | 등록일 | `createdAt` | yyyy-MM-dd |
| 9 | 관리 | — | "편집" 링크 (상세로 이동) |

## deferred (계약에는 포함, 검사에서 제외)

| 항목 | 담당 티켓 |
|---|---|
| 신청기간 실 데이터 컬럼 | A3 (`applyStartDate` / `applyEndDate` Program 컬럼 신설) |
| 조회수 컬럼 실 데이터 | A6 (`viewCount` + 통계 트랙) |
| 카드/캘린더 뷰 토글 | A8 |
| CSV 내보내기 버튼 | A8 |
| 체크박스 컬럼 (일괄 선택) | A8 |
| 프로그램 등록/편집/삭제 실동작 | A3 |
| 신청 현황 보기 (상세 하위 관리) | A4 |

## 회귀 방지 체크

- 사용자 사이드 `/programs`·`/programs/{id}` 무회귀 (Program 엔티티 변경 없음)
- F0c/F4 서브 페이지 진입 링크 정합 (경로 유지)
- admin GNB "프로그램 관리" 활성화가 다른 GNB 상태 변경 없이 이뤄지는지
