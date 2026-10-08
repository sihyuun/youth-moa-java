# 관리자 프로그램 상세 (`/admin/programs/{id}`) — 디자인 계약

- 상태: 신설 (FOLLOW-admin-program-detail-readonly · 2026-10-07)
- 원본: `docs/00_assets/admin/HANDOFF.md` L234-238, `docs/00_assets/admin/prototype.html` L1446~1619
- 결정: `docs/specs/FOLLOW-admin-program-detail-readonly.md` (Q1~Q8 모두 권장안)
- 계약 파일: `e2e/contracts/admin-program-detail.ts`

## 배경

A3-1 (2026-09-10) 에서 "상세 = 편집 폼" 결정 (Qn-A A) 으로 `GET /admin/programs/{id}` 가 폼으로 대체됐으나 실사용 중 **상세 역할이 사라지면서 발생한 UX 결함** 재발견:

1. 메뉴·대시보드·검색에서 "상세" 를 기대하고 진입한 사용자가 바로 편집 가능 상태를 마주함
2. 편집 폼은 입력창 중심이라 "요약을 빠르게 훑어보기" 가 어렵고 저장 CTA 가 상시 노출돼 실수 유발
3. 정보 조회만 필요한 CENTER_ADMIN (A3-1 당시 등록·편집·삭제는 SYSTEM_ADMIN only) 에게는 편집 폼이 적합하지 않음

본 티켓은 이 결정을 되돌려 **상세는 상세 전용 페이지로 분리**하고 편집은 `/edit` 서브경로로 이동한다.

## 아키텍처

- SSR 단일 템플릿 (`admin/program/detail.html`)
- 2열 그리드 (좌 정보 400px + 우 설명 flex-1)
- 상세 자체는 폼이 아님 — 모든 쓰기 액션은 상세 외부 (수정→/edit, 삭제→POST, watch→HTMX)
- 404 처리: `AdminProgramController.detail()` 가 `ResponseStatusException(NOT_FOUND)` throw (A3-1 editForm 과 동일 패턴)

## 상태머신

| 상태 | 진입 | 다음 |
|---|---|---|
| 상세 조회 | `GET /admin/programs/{id}` | 수정 CTA → `/admin/programs/{id}/edit` |
| 삭제 confirm | ⋯ 더보기 → 삭제 → 모달 | 확인 → `POST /admin/programs/{id}/delete` → 302 `/admin/programs` |
| 지켜보기 토글 | 헤더 watch-button | `POST /admin/programs/{id}/watch/toggle` (HTMX outerHTML swap) |
| 신청 현황 보기 | "신청 현황 N건 보기" | `/admin/programs/{id}/applications` (A4 화면 유지) |
| 하위 관리 진입 | (상세에는 없음) | 편집 폼에서 F4/F0c/신청관리 링크 유지 |

## RBAC

- 조회: SYSTEM_ADMIN + CENTER_ADMIN (목록·조회는 둘 다 허용 · A2 승계)
- 삭제: SYSTEM_ADMIN only (A3-1 Qn-1 승계)
- 지켜보기: 인증된 관리자 (A7-watcher-ui 패턴)

## CTA 라우팅

| 트리거 | 경로 | 응답 |
|---|---|---|
| 뒤로가기 "← 프로그램 목록" | `/admin/programs` | 200 (목록) |
| 수정 (primary) | `/admin/programs/{id}/edit` | 200 (편집 폼) |
| ⋯ 더보기 → 삭제 → 모달 확인 | `POST /admin/programs/{id}/delete` | 302 목록 (SUSPENDED 전환) |
| watch-button | `POST /admin/programs/{id}/watch/toggle` | fragment outerHTML |
| "신청 현황 N건 보기" | `/admin/programs/{id}/applications` | 200 (신청 현황 화면) |

## 레이아웃 (prototype L1446~1540 적용)

### 1. Header strip
- 뒤로가기 링크 `← 프로그램 목록`
- 제목 (`.admin-program-detail-title`) + 상태뱃지 (`.admin-status-badge--{status}`)
- 신청수 "신청 N명"
- 지켜보기 버튼 (watch-button fragment)
- 수정 CTA (`.admin-btn--primary`) → `/edit`
- ⋯ 더보기 (`[data-detail-menu-trigger]`) — 열면 삭제 메뉴 하나만

### 2. 2열 그리드
**좌 정보 카드** (`.admin-program-detail-info`, max-width 400):
- 썸네일 (160×160) — imageUrl 없으면 플레이스홀더
- 청년센터 · 진행기간 · 신청기간 · 모집인원 (`applied/capacity`) · 장소 · 문의처 · 첨부 (다운로드 링크만)
- 각 필드는 `[data-field="..."]` + `.admin-program-detail-field-label` + `.admin-program-detail-field-value` 2계층

**우 설명 카드** (`.admin-program-detail-desc`, flex-1):
- `program.content` 평문 렌더 (Q7: `th:text` 사용, 저장 시 sanitize 미도입이라 `th:utext` 금지)
- content 비어있으면 섹션 통째로 생략

### 3. 하단
- "신청 현황 N건 보기" 버튼 (`a[data-testid="link-applications"]`) — 신청수 노출 + /applications 로 이동

## 이월 (별도 FOLLOW 티켓)

- **Q4 대기자/자동승인 배너·토글** — Application 상태 집계 + Program 설정 토글 필요 (별도 티켓)
- **Q5 프로그램 복제** — Program 깊은 복제 서비스 필요 (Course·ApplyQuestion·Attachment 포함 여부 결정 필요)
- **Q6 신청 현황 테이블 내장** — 지금은 기존 `/applications` 화면 유지. 필요 시 상세 내 축약 테이블 + 전체 보기 링크로 재배치
- **Q7 content HTML sanitize** — Program.content 저장 시 OWASP sanitizer 적용 후 `th:utext` 전환 (현재는 사용자/관리자 모두 `th:text` 평문 유지)

## deviation (prototype 대비)

- 설명 섹션에 "신청자/조회수/모집 정원" 3분할 통계 카드 (prototype L1526~1539) — **제외**. 신청수는 헤더에만 노출
- 강좌 목록 섹션 (prototype L1542~1577) — **제외**. 강좌는 편집 폼의 "강좌 제공" 섹션에서 관리
- 상세 내 신청 테이블 (prototype L1594~1619) — **제외** (Q6: /applications 유지)

## 계약 검증 포인트

갭 0 기준으로 `e2e/contracts/admin-program-detail.ts` 의 모든 check 가 PASS. 특히:
- `form.inline.missing` — 상세에 `form.admin-program-form` 이 없어야 함 (편집과 명확히 분리)
- `applications.inline.table.missing` — 상세에 신청 테이블 없음
- `waitlist.banner.missing` — 대기자 배너 미노출
- `header.more.menu.clone.missing` — 복제 메뉴 미노출
