# QA 리포트 — FOLLOW-admin-program-detail-readonly

- 작업 브랜치: `feature/FOLLOW-admin-program-detail-readonly` (커밋 미실행)
- QA 일자: 2026-10-07
- QA 담당: ym-qa (자동화)
- 선행 산출물: ym-spec `af16f30801f15aa61` · ym-impl `a0eecc7badc7c5c35`

## 1. 정적 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` | SUCCESS (UP-TO-DATE, 7s) |
| `./gradlew test --tests io.github.sihyuuun.youthmoa.admin.* --tests ProgramWatchControllerTest` | SUCCESS (2m 5s) |
| admin + watch 패키지 TC 총합 | **332 TC PASS · 실패 0건** |
| 핵심 신규/수정 TC | `AdminProgramDetailRenderTest` 3/3 · `AdminProgramFormRenderTest` 5/5 · `ProgramWatchControllerTest` (watch) PASS |

→ 서버 측 분리 (controller 매핑, Thymeleaf render) 는 정적 기준 전부 PASS.

## 2. 동적 검증 (bootRun e2e · 8090)

| 요청 | 상태 | 비고 |
|---|---|---|
| `GET /admin/programs/1` | 200 | `admin-program-detail-page` wrapper · `.admin-program-detail-title` · `data-testid="detail-menu-delete"` · `data-testid="link-applications"` · `#program-delete-modal` 모두 렌더 확인 |
| `GET /admin/programs/1/edit` | 200 | `form.admin-program-form` · `.admin-program-form-title` · `.admin-program-form-breadcrumb` 모두 렌더 확인 |
| `GET /admin/programs/1/applications` | 200 | 기존 흐름 영향 없음 |
| `POST /admin/programs/1/edit` (최소 필드만) | 400 | 필드 누락 시 BindException 예상 범위. 전체 필드 submit 은 E2E 에서 별도 검증 |

### 상세 템플릿에서 발견된 결함

- `watch-button fragment` 가 상세 헤더에 렌더되지만 **`data-testid="watch-button"` 속성이 존재하지 않음**. fragment (`admin/fragments/watch-button.html`) 가 `class="detail-watch-btn watch-btn"` 만 부여.
- 영향: 계약 `header.watch.button` (P0) FAIL (아래 §4 참조).

## 3. E2E Playwright (chromium)

### 신규 스펙

| 스펙 | 결과 |
|---|---|
| `tests/admin-program-detail.spec.ts` (3 TC) | **2 PASS · 1 FAIL** |
| `tests/admin-program-watch.spec.ts` (3 TC) | 3 PASS |

**FAIL — `상세 → 수정 CTA → 편집 폼 → 저장 → 상세 복귀`**
- 저장 버튼 클릭 후 `page.waitForURL(/\/admin\/programs\/1$/)` 60s timeout.
- 근본 원인: seed program #1 의 `applyStartDate` / `applyEndDate` 가 비어 있음. `<input ... required>` 로 렌더되어 **브라우저 HTML5 validation 이 submit 자체를 블록**. 서버 로그에 POST 흔적이 남지 않음.
- 신규 스펙이 seed 데이터 가정을 하지 않은 버그. `admin-program-form.spec.ts:138~141` 가 이미 알려진 함정을 wide-range 날짜 주입으로 회피한 전례가 있음 (PR #206 learning).
- 조치 (ym-impl): spec 에서 `applyStartDate` / `applyEndDate` 를 먼저 wide-range 로 채운 뒤 저장하거나, 기존 값이 비어있지 않은 program id (seed 중 다른 번호) 를 사용.

## 4. 디자인 계약 검사 (`--project=contracts`)

| 화면 | 결과 |
|---|---|
| `admin-program-detail` | **23/24 통과 · 갭 1건** |
| `admin-program-form-edit` | 0 갭 |
| `admin-program-form-new` | 0 갭 |
| `admin-program-list` | 0 갭 (legacy re-export 영향 없음) |
| 전체 contracts 합계 | 44 PASS · 1 FAIL |

**갭 — `header.watch.button` (P0)**
- 셀렉터: `.admin-program-detail-header-actions [data-testid="watch-button"]`
- 기대: exists=true · 실제: false
- 원인: `admin/fragments/watch-button.html` fragment 가 `data-testid="watch-button"` 속성을 렌더하지 않음.
- 조치 (ym-impl): fragment `<button>` 에 `data-testid="watch-button"` 추가 또는 상세 템플릿에서 wrapper 로 추가. fragment 공용 영향 (`list`, `form` 사용처) 확인 필요.

## 5. 회귀 영향 (주요 chromium 스펙 재실행)

범위: `admin-program-form.spec.ts` · `admin-programs-rbac.spec.ts` · `visual-admin-programs.spec.ts`.

| 스펙 | 결과 |
|---|---|
| `admin-programs-rbac.spec.ts` | 3 PASS |
| `visual-admin-programs.spec.ts` (list) | PASS |
| `visual-admin-programs.spec.ts` (detail) | FAIL (§4 갭과 동일 사유) |
| `admin-program-form.spec.ts` | **5 PASS · 4 FAIL** |

### FAIL 4건 (모두 상세 분리 영향)

| TC (line) | 깨진 지점 | 근본 원인 |
|---|---|---|
| `신규 등록 → 편집 폼 prefilled 확인` (37) | 저장 후 `.admin-program-form-title` 로 '프로그램 편집' expect | 저장 redirect `/admin/programs/{id}` 가 이제 **상세 템플릿** → form title selector 미존재 |
| `편집 → 제목 수정 → 저장 → 반영` (91) | 저장 후 `input[name="title"]` 로 재검증 | 상세는 form 입력 없음 |
| `FK 참조가 있는 시드 프로그램(#1) 삭제` (112) | `/admin/programs/1` 진입 후 `input[name="active"]`·탭 트리거 접근 | 상세에 폼 요소 없음 |
| `FK 없는 신규 프로그램 삭제 → 목록` (150) | 저장 후 `.admin-program-form-actions .admin-btn--danger` 클릭 | 저장 redirect 가 상세 → 삭제 CTA 위치 변경 (`.admin-program-detail-header-actions`) |

**이 4건은 impl 범위 수정 필요**. ym-qa 는 테스트 수정 금지 (범위 넘음).

### 조치 가이드 (ym-impl 로 피드백)

1. 저장 후 접근 경로를 `/admin/programs/{id}/edit` 로 명시적으로 이동하여 폼 재확인
2. 삭제 CTA 를 상세 템플릿의 `.admin-program-detail-header-actions .admin-program-detail-more-menu [data-testid="detail-menu-delete"]` 로 전환
3. seed program #1 조작 TC 는 wide-range 날짜 주입을 우선 수행 (기존 전례)

## 6. BLOCKER / 종합 판정

| 분류 | 건수 |
|---|---|
| BLOCKER (머지 차단) | **2건** — contracts P0 갭 1건 + E2E 신규 spec FAIL 1건 |
| 회귀 FAIL (impl 수정 필요) | **4건** — `admin-program-form.spec.ts` |
| 정적 검증 | 전부 PASS |
| 동적 검증 | 핵심 경로 전부 200 (전체 필드 POST 는 E2E 가 검증 담당) |

**머지 전 ym-impl 재작업 필수**. 수정 범위:

- `admin/fragments/watch-button.html` 에 `data-testid="watch-button"` 추가
- `tests/admin-program-detail.spec.ts` 저장 흐름 TC 의 seed 가정 보완
- `tests/admin-program-form.spec.ts` 4 TC 를 `/edit` 경로 + 상세 삭제 CTA 셀렉터로 갱신

## 7. 미실행 항목

- 전체 `./gradlew test` 회귀 (admin 범위만 332 TC 수행. non-admin 영역은 현 변경 범위상 영향 없음으로 판단)
- 수동 시각 확인 (reviewer 영역)
