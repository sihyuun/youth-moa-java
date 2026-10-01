# 관리자 헤더 글로벌 검색 — 서술 계약 (A8-search)

- 추출 기준: Q 결정 (사용자 컨펌 A 전부, 2026-10-01)
- 관련 기계 계약: `e2e/contracts/admin-search.ts` + `e2e/contracts/admin-shell.ts` 보강
- 기능 E2E: `e2e/tests/admin-search.spec.ts` (chromium 4 TC)

## 결정 (Q&A)

| Q | 결정 |
|---|---|
| Q1 UX | HTMX 드롭다운 (keyup debounce 200ms) |
| Q2 범위 | 프로그램 + 사용자 (공지 제외) |
| Q3 RBAC | SYSTEM_ADMIN 전역 · CENTER_ADMIN 자기 센터로만 격리 |
| Q4 엔드포인트 | `/admin/search/dropdown` (사용자 `/search` 와 분리) |
| Q5 센터 도메인 | 미포함 |

## 정보 구조

- `<form class="admin-header-search">` — pill 입력박스 (180×32)
    - `<span class="admin-header-search-icon">` 돋보기 아이콘 (좌측 absolute)
    - `<input class="admin-header-search-input" type="search" name="q" maxlength="100">`
        - HTMX: `hx-get=/admin/search/dropdown · hx-trigger=keyup changed delay:200ms · hx-target=#admin-search-dropdown · hx-swap=outerHTML`
    - `<button class="admin-header-search-clear">×` — 입력 비우기 (JS onclick) + 닫힌 드롭다운 재요청
    - `<div id="admin-search-dropdown" class="admin-header-search-dropdown">` — 300px 라이트 드롭다운
        - `is-open` 클래스가 있을 때만 `display:block`
        - 섹션: 프로그램 (최대 4) + 사용자 (최대 3) — 각 섹션 사이 1px separator
        - 각 아이템: 28px 아이콘 (프로그램 `#F1EFFC` + `#3F30E9` · 사용자 `#ECFDF5` + `#10B981`) + 제목 + 서브(센터명/이메일)
        - 빈 상태 (쿼리는 있고 결과 0): "검색 결과가 없어요"

## 백엔드 계약

- `AdminSearchController.dropdown(q)` → `admin/fragments/_search-dropdown :: dropdown`
- `AdminSearchService.search(q)`
    - q null/blank → empty result
    - q > 100자 → 앞 100자 trim
    - `AdminScope.effectiveCenterId()` 로 RBAC 격리:
        - null → SYSTEM_ADMIN 전역
        - 값 있음 → CENTER_ADMIN 자기 센터 FK 격리 (프로그램 center FK + 사용자 center FK)
    - 프로그램 쿼리: `ProgramSpec.withKeyword(q)` 재사용 + createdAt DESC
    - 사용자 쿼리: name/email LIKE OR + SYSTEM_ADMIN 제외 (CENTER_ADMIN 조회 시) + createdAt DESC

## RBAC

| 호출자 | 응답 |
|---|---|
| 미인증 | 302 → /admin/login |
| USER | 403 |
| SYSTEM_ADMIN | 200, 전역 결과 |
| CENTER_ADMIN | 200, 자기 센터 결과만 |

`@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")` 로 게이트.

## 이탈 · 이월

| 항목 | 종류 | 사유 |
|---|---|---|
| 공지 검색 | **deviation** | Q2 B안 (사용자 `/search` 와 분리된 짧은 드롭다운 전용) |
| 센터 도메인 | **deviation** | Q5 (별도 센터 관리 트랙에서 다룸) |
| Esc 키 입력 지우기 | **deferred** | UX 보강 추후 (× 버튼은 있음) |
| 최근 검색어 / 자동완성 | **deferred** | 운영 데이터 축적 후 결정 |

## 검증 자산

- 단위: `AdminSearchServiceTest` (6 TC) — 빈 쿼리 · 100자 trim · SYSTEM 전역 · CENTER 격리 (프로그램·사용자)
- 렌더: `AdminSearchControllerRenderTest` (6 TC) — fragment 열림/닫힘 상태 · 빈 결과 · RBAC 403/302
- 계약: `visual-admin-search.spec.ts` (contracts project)
- 기능 E2E: `admin-search.spec.ts` (chromium) — debounce · 클릭 이동 · 지우기 · 빈 결과
