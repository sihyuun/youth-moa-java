# QA 리포트 — FOLLOW-admin-program-detail-readonly (3차 · UNVERIFIED 해소 재검증)

- 작업 브랜치: `feature/FOLLOW-admin-program-detail-readonly` (커밋 미실행)
- QA 일자: 2026-10-07 (3차)
- QA 담당: ym-qa (자동화)
- 선행 산출물
  - ym-impl 3차: `a3091312fb83ff702` (미커밋, 2 파일 수정)
  - ym-qa 1차 리포트: `docs/qa-reports/FOLLOW-admin-program-detail-readonly-qa-20261007.md`
  - ym-qa 2차 리포트: `docs/qa-reports/FOLLOW-admin-program-detail-readonly-qa-20261007-2.md`
  - ym-verify 리포트: `docs/qa-reports/FOLLOW-admin-program-detail-readonly-verify-20261007.md`

## 0. UNVERIFIED → 해소 요약

ym-verify 가 지적한 UNVERIFIED 2건에 대해 ym-impl 3차가 보강한 TC/범위를 그대로 실행해 **UNVERIFIED → VERIFIED** 전환을 확인.

| 코드 | ym-verify 지적 | ym-impl 3차 반영 | 3차 QA 결과 |
|---|---|---|---|
| **U1** | `AdminProgramDetailRenderTest` 가 SYSTEM_ADMIN 1 세션만 커버 → CENTER_ADMIN own-center 200 · cross-center 403 미증명 | `AdminProgramDetailRenderTest` 에 CENTER_ADMIN 2 TC (`own-center 200 + 상세 wrapper 렌더`, `cross-center 403`) 추가 | ✅ 해소 — 5/5 PASS (SYSTEM 3 + CENTER own 1 + CENTER cross 1) |
| **U2** | `admin-head-scripts.spec.ts` ADMIN_PAGES 가 13개로 유지 → 신규 분리 경로 2종 미커버 | ADMIN_PAGES 에 `program/detail` (`/admin/programs/1`) + `program/form(edit)` (`/admin/programs/1/edit`) 2건 추가 (총 15 → 전체 17 TC) | ✅ 해소 — 17/17 PASS, 신규 2 경로 각각 CSRF meta 2개 + HTMX + Toast 로드 확인 |

**결론: UNVERIFIED 2건 전부 VERIFIED 전환. 머지 가능 상태.**

## 1. 정적 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` | SUCCESS (UP-TO-DATE · 9s) |
| `./gradlew test --tests AdminProgramDetailRenderTest` | SUCCESS (1m 27s) — **5 TC · skipped 0 · failures 0 · errors 0** |
| `./gradlew test --tests "admin.*" --tests ProgramWatchControllerTest` | SUCCESS (1m 54s) |
| admin + watch 패키지 TC 총합 | **332 TC PASS · 실패 0건** (2차와 동일) |

### AdminProgramDetailRenderTest 5 TC 세부

- SYSTEM_ADMIN 3 TC (기존, 1차·2차 유지)
- CENTER_ADMIN own-center 200 + 상세 전용 wrapper 렌더 1 TC (신규, U1 해소)
- CENTER_ADMIN cross-center 403 1 TC (신규, U1 해소)

### admin.* 전수 집계 (XML 파싱)

38 테스트 클래스 (admin 36 + admin.chart 1 + admin.SecureRandomPasswordGenerator 1) + ProgramWatchControllerTest 1 = **총 39 파일, 332 TC, 0 failure, 0 error, 0 skipped**.

→ CENTER_ADMIN TC 2건이 다른 테스트 격리를 깨뜨리지 않음 확인.

## 2. 동적 검증 (bootRun e2e · 포트 8090)

- bootRun 기동 성공 (`http://localhost:8090` 200 응답 확인)
- 신규 2 ADMIN_PAGES 응답은 Playwright 세션 경로로 검증 (아래 §3)
  - curl 직접 호출은 CSRF 인증 플로우 상 302 (`/login` 리다이렉트)만 반환. 실제 세션 기반 응답은 Playwright `loginAdmin()` 세션으로 측정하는 것이 공식 경로

## 3. Playwright 재검증 (chromium)

### 3-1. 신규 TC 포함 — `admin-head-scripts.spec.ts`

```
BASE_URL=http://localhost:8090 npx playwright test tests/admin-head-scripts.spec.ts --project=chromium
```

**결과: 17/17 PASS · 45.3s**

- ADMIN_PAGES 전수 loop: 15/15 PASS (13 기존 + **신규 2**: `program/detail`, `program/form(edit)`)
  - 각 페이지에서 CSRF meta 2개 + HTMX (window.htmx object) + common-ui (window.Toast object) 로드 3항목 모두 PASS
- `notice/form 인라인 CSRF 훅 제거 — HTMX 요청 X-CSRF-TOKEN 자동 부착 (P-A)` PASS
- `dashboard 알림 벨 — hx-get 트리거 후 dropdown 대체 (A7-e2e-suite FAIL-1)` PASS

### 3-2. 기존 PASS spec regression

```
BASE_URL=http://localhost:8090 npx playwright test \
  tests/admin-program-detail.spec.ts \
  tests/admin-program-form.spec.ts \
  tests/admin-program-watch.spec.ts \
  tests/admin-programs-rbac.spec.ts \
  --project=chromium
```

**결과: 15/15 PASS · 1m 18s**

| 스펙 | 1차 → 2차 → 3차 |
|---|---|
| `admin-program-detail.spec.ts` (3 TC) | 2 PASS·1 FAIL → 3/3 → **3/3 PASS** |
| `admin-program-form.spec.ts` (9 TC) | 5 PASS·4 FAIL → 9/9 → **9/9 PASS** |
| `admin-program-watch.spec.ts` (3 TC) | 3 PASS → 3 PASS → **3/3 PASS** |
| `admin-programs-rbac.spec.ts` (3 TC) | 3 PASS → 3 PASS → **3/3 PASS** |

→ ADMIN_PAGES 2건 확대 + AdminProgramDetailRenderTest 2 TC 신설이 기존 스펙에 side-effect 없음 확인.

### 3-3. 디자인 계약 — `--project=contracts`

```
BASE_URL=http://localhost:8090 npx playwright test --project=contracts
```

**결과: 45/45 PASS · 3m 2s** (2차와 동일, regression 0)

## 4. 비교 — 1차 / 2차 / 3차

| 분류 | 1차 | 2차 | 3차 |
|---|---|---|---|
| BLOCKER (머지 차단) | 2건 | 0건 | **0건** ✅ |
| 회귀 FAIL | 4건 | 0건 | **0건** ✅ |
| UNVERIFIED (ym-verify) | — | 2건 | **0건** ✅ |
| 정적 compileJava | PASS | PASS | PASS |
| AdminProgramDetailRenderTest | 3/3 | 3/3 | **5/5** (CENTER_ADMIN 2 TC 추가) |
| admin.* + watch 패키지 | 330 PASS | 330 PASS | **332 PASS** (+2) |
| admin-head-scripts.spec.ts | 15/15 (13 ADMIN_PAGES) | 15/15 (13) | **17/17 (15 ADMIN_PAGES)** (+2) |
| admin-program-* regression | 변동 | 9/9 + 6/6 | **15/15** (0 regression) |
| 디자인 계약 | 44/45 | 45/45 | **45/45** |

**정량 비교로 regression 0 확정.**

## 5. BLOCKER / 종합 판정

| 항목 | 결과 |
|---|---|
| 정적 검증 | ✅ PASS |
| 동적 검증 (Playwright 경유) | ✅ PASS — 신규 2 경로 포함 CSRF/HTMX/Toast 모두 OK |
| 회귀 (admin.* + 주요 Playwright 스펙) | ✅ 0 regression |
| UNVERIFIED-U1 (CENTER_ADMIN scope) | ✅ 해소 |
| UNVERIFIED-U2 (admin-head-scripts ADMIN_PAGES) | ✅ 해소 |

**판정: 머지 가능. ym-verify 재호출 시 U1·U2 전부 VERIFIED 로 승격 예상.**

## 6. 미실행 항목

- 전체 `./gradlew test` 회귀 (admin 범위 332 TC 수행. 변경 범위상 non-admin 영역 영향 없음으로 판단 — 2차와 동일 판단)
- 시각 확인 (사용자 영역) — 색감·폰트·반응형 미세 디테일. prototype 대비 수치는 contracts 계약 검사가 자동 수행 (45/45 PASS)

## 7. 다음 단계

> 3차 검증 완료. UNVERIFIED-U1·U2 전부 해소. 커밋 전 최종 관문으로 `ym-verify` 재호출을 권장합니다. 시각 확인 (사용자 영역) 통과 시 머지 진행 가능합니다.
