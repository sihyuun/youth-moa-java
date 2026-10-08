# FOLLOW-admin-program-detail-readonly — 관리자 프로그램 상세 (read-only) 재도입

- 상태: `spec_confirmed` → `impl_in_progress` (2026-10-07)
- 산출: ym-spec (af16f30801f15aa61)
- 결정: Q1~Q8 모두 권장안 적용
- 선행 결정: A3-1 (2026-09-10) 의 "상세 = 편집 폼" (Qn-A A) 결정을 되돌림. 사유는 §1 배경 참조

---

## 1. 배경

A3-1 당시 "상세와 편집을 분리할 필요가 없다" 는 가정으로 `GET /admin/programs/{id}` 를 편집 폼 하나로 통합했으나, 실 운영 중 다음 문제가 재발견됐다.

1. 메뉴/대시보드/검색에서 "상세 보기" 를 기대하고 진입한 사용자가 입력 폼을 마주함
2. 저장 CTA 가 상시 노출돼 실수 저장 위험
3. CENTER_ADMIN 은 등록·편집·삭제 권한이 없는데도 편집 폼이 그대로 보임 (혼란 유발)

본 티켓은 상세와 편집을 재분리한다.

---

## 2. 변경 범위

### 신설
- `admin/program/detail.html` — 상세 전용 템플릿
- `AdminProgramController.detail(Long id)` — `GET /admin/programs/{id}` 신규 매핑
- `e2e/contracts/admin-program-detail.ts` — 디자인 계약
- `docs/design-contracts/admin/program-detail.md` — 서술 계약
- `e2e/tests/admin-program-detail.spec.ts` — 상세→편집→저장→상세 복귀 흐름 1건

### 수정
- `AdminProgramController`:
  - 기존 `GET /admin/programs/{id}` editForm → **삭제**
  - 신규 `GET /admin/programs/{id}/edit` editForm
  - `POST /admin/programs/{id}` → `POST /admin/programs/{id}/edit`, redirect → `/admin/programs/{id}` (상세)
- `admin/program/form.html`:
  - breadcrumb `← 프로그램 상세`, href = `/admin/programs/{id}`
  - watch-button 블록 제거 (상세 헤더 전용)
  - form `th:action` 변경 (`.../{id}` → `.../{id}/edit`)
  - 삭제 모달·버튼은 상세로 이동. 편집 폼의 "삭제" 버튼은 제거
- `admin/program/list.html`: 프로그램명 링크는 그대로 `/admin/programs/{id}` 유지 (이제 상세로 라우팅됨)
- 기존 테스트 `AdminProgramDetailRenderTest` + `AdminProgramFormRenderTest`: URL 분리 반영
- `e2e/contracts/admin-program-form.ts`: 편집 path `/admin/programs/1` → `/admin/programs/1/edit`, breadcrumb 라벨 갱신
- `docs/design-contracts/admin/program-form.md`: CTA 라우팅 표 갱신
- `e2e/contracts/admin-programs.ts` legacy re-export 제거

---

## 3. 결정 요약 (Q1~Q8)

| Q | 결정 | 비고 |
|---|---|---|
| Q1 | 편집 URL = `/admin/programs/{id}/edit` | 서브경로 분리 |
| Q2 | 수정 저장 후 redirect = `302 /admin/programs/{id}` (상세) | 자기 자신 아닌 상세 복귀 |
| Q3 | 폼 breadcrumb = `← 프로그램 상세` | href = `/admin/programs/{id}` |
| Q4 | 대기자/자동승인 토글 | **이월** (별도 FOLLOW 티켓) — 배너 미노출 |
| Q5 | 복제 액션 | **이월** — ⋯ 더보기에 삭제만 |
| Q6 | 신청 현황 | 기존 `/applications` 유지 + 상세 하단 "신청 현황 N건 보기" 버튼 |
| Q7 | content HTML | **`th:text` plain** (사용자 `/programs/{id}` 템플릿과 동일. 저장 시 sanitize 미도입 상태라 `th:utext` 는 XSS 위험. sanitize 도입은 별도 티켓으로 이월) |
| Q8 | watch-button | 상세 헤더 전용. 편집 폼에서 제거 |

> Q7 재해석 근거: spec 산출 당시 "th:utext + Jsoup Safelist" 로 표기됐으나, 사용자 `/programs/{id}` 템플릿 (`src/main/resources/templates/program/detail.html` L256) 이 `th:text` 평문을 사용하고 Program.content 저장 시 sanitize 가 걸려있지 않다. admin 쪽만 `th:utext` 를 도입하면 XSS 공격면 확대. 패턴 일치 우선으로 `th:text` 로 결정하고 sanitize 도입을 선행 티켓으로 둠.

---

## 4. 상세 화면 구성 (HANDOFF L234-238)

### Header strip
- 뒤로가기 `← 프로그램 목록` → `/admin/programs`
- 제목 + 상태뱃지 + 신청수 "신청 N명"
- watch-button (`admin/fragments/watch-button :: button`)
- 수정 CTA (primary) → `/admin/programs/{id}/edit`
- ⋯ 더보기 메뉴 — 삭제 1개 (복제는 Q5 이월)

### 2열 그리드
- 좌 `.admin-program-detail-info` (max-width 400): 썸네일·청년센터·진행기간·신청기간·모집인원·장소·문의처·첨부 (다운로드 링크만)
- 우 `.admin-program-detail-desc` (flex-1): `program.content` `th:text` 평문, 비어있으면 섹션 생략

### 하단
- "신청 현황 N건 보기" 버튼 (`a[data-testid="link-applications"]`) → `/admin/programs/{id}/applications`

### 삭제 모달
- `#program-delete-modal` — 기존 form.html 과 동일 markup, 상세 템플릿으로 이동
- confirm → `POST /admin/programs/{id}/delete` → 302 `/admin/programs`

---

## 5. RBAC

- 상세 조회: SYSTEM_ADMIN + CENTER_ADMIN (A2 승계)
- 편집 폼 (`/edit`) + update: SYSTEM_ADMIN only (A3-1 승계)
- 삭제: SYSTEM_ADMIN only (A3-1 승계)
- 지켜보기: 인증된 관리자 (A7 승계)

---

## 6. 구현 매핑 (impl 완료 후 파일:라인)

| 결정/필드 | 구현 위치 |
|---|---|
| Q1 편집 URL `/edit` | `AdminProgramController.editForm` `@GetMapping("/{id}/edit")` |
| Q1 편집 URL `/edit` | `AdminProgramController.update` `@PostMapping("/{id}/edit")` |
| Q1 상세 URL | `AdminProgramController.detail` `@GetMapping("/{id}")` |
| Q2 저장 후 redirect = 상세 | `AdminProgramController.update` return 문 |
| Q3 폼 breadcrumb `← 프로그램 상세` | `admin/program/form.html` L24~26 |
| Q4 대기자 배너 미노출 | `admin/program/detail.html` — 배너 markup 자체 미작성 |
| Q5 복제 미노출 | `admin/program/detail.html` ⋯ 메뉴 — 삭제 항목 1개만 |
| Q6 "신청 현황 N건 보기" | `admin/program/detail.html` — `a[data-testid="link-applications"]` |
| Q7 content `th:text` | `admin/program/detail.html` desc 섹션 |
| Q8 watch-button 상세 전용 | `admin/program/detail.html` header + `admin/program/form.html` 에서 블록 삭제 |
| 삭제 모달 상세 이관 | `admin/program/detail.html` `#program-delete-modal` |
| 테스트 분리 | `AdminProgramDetailRenderTest` + `AdminProgramFormRenderTest` |

---

## 7. 작업 큐 메타

- 상태: `impl_done` (2026-10-07)
- 다음 단계: ym-qa → ym-verify → 머지
- 이월: 대기자 토글 / 복제 / content sanitize + th:utext 전환 — 각각 별도 FOLLOW 티켓
