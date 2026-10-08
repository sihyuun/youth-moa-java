# QA 리포트 — FOLLOW-admin-program-detail-readonly (2차)

- 작업 브랜치: `feature/FOLLOW-admin-program-detail-readonly` (커밋 미실행)
- QA 일자: 2026-10-07 (2차 재검증)
- QA 담당: ym-qa (자동화)
- 선행 산출물: ym-qa 1차 `a48845513cded27a5` · ym-impl 2차 `a389e8dfd2d4e3563`
- 1차 리포트: `docs/qa-reports/FOLLOW-admin-program-detail-readonly-qa-20261007.md`

## 0. 1차 BLOCKER/FAIL 해소 요약

| 1차 식별 결함 | 심각도 | 2차 결과 |
|---|---|---|
| `header.watch.button` P0 갭 (watch-button fragment 에 `data-testid` 누락) | BLOCKER | ✅ 해소 — fragment 에 `data-testid="watch-button"` 추가 확인 (line 30) |
| `admin-program-detail.spec.ts` 저장 TC 실패 (seed applyDate 가정) | BLOCKER | ✅ 해소 — 3/3 PASS |
| `admin-program-form.spec.ts` 4 TC 회귀 (상세 분리 영향) | FAIL × 4 | ✅ 해소 — 9/9 PASS |
| `visual-admin-programs.spec.ts (detail)` 동반 복구 | FAIL | ✅ 해소 — contracts 통과 |

**결론: 1차 BLOCKER 2건 + FAIL 4건 전부 해소. 머지 가능.**

## 1. 정적 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` | SUCCESS (UP-TO-DATE, 14s) |
| `./gradlew test --tests admin.* --tests ProgramWatchControllerTest` | SUCCESS (3m 17s) |
| admin + watch 패키지 TC 총합 | **332 TC PASS · 실패 0건** (1차와 동일) |

→ 정적 기준 전부 PASS.

## 2. 동적 검증 (bootRun e2e · 포트 8090)

| 요청 | 상태 | 비고 |
|---|---|---|
| `GET /` | 200 | bootRun 기동 확인 (50s 소요) |
| `GET /admin/programs/1` (SYSTEM_ADMIN 세션) | 200 | 상세 템플릿 렌더 확인 |
| `GET /admin/programs/1/edit` | 200 | 편집 폼 렌더 (`admin-program-form` class 155개) |
| `grep 'data-testid="watch-button"'` on `/admin/programs/1` | **1건 확인** | ✅ 1차 결함 해소 |
| `grep 'data-testid="detail-menu-delete"'` | 1건 확인 | 유지 |
| `grep 'data-testid="link-applications"'` | 1건 확인 | 유지 |
| `.admin-program-detail-header-actions` wrapper | 렌더 확인 | 주석 `Q8: watch-button 상세 헤더 전용` 동반 |

**정적 리소스**: contracts 테스트가 1440x900 뷰포트 전수 렌더 통과 → 정적 리소스 302/404 사고 없음 (CSS/JS/IMG 로드 정상).

## 3. 디자인 계약 검사 (`--project=contracts`)

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts
```

**결과: 45/45 PASS · 3m 7s**

| 화면 | 1차 → 2차 |
|---|---|
| `admin-program-detail` | 23/24 → **24/24 통과** (header.watch.button P0 갭 해소) |
| `admin-program-form-edit` | 0 갭 → 0 갭 |
| `admin-program-form-new` | 0 갭 → 0 갭 |
| `admin-program-list` | 0 갭 → 0 갭 |
| `visual-admin-programs (detail)` | FAIL → PASS (동반 복구) |
| `visual-admin-programs (list)` | PASS → PASS (side-effect 없음) |
| 전체 contracts | 44 PASS · 1 FAIL → **45 PASS · 0 FAIL** |

## 4. E2E Playwright 재검증 (chromium)

### 수정된 2 spec

```
npx playwright test tests/admin-program-detail.spec.ts tests/admin-program-form.spec.ts --project=chromium
```

**결과: 9/9 PASS · 29.8s**

| 스펙 | 1차 → 2차 |
|---|---|
| `admin-program-detail.spec.ts` (3 TC) | 2 PASS·1 FAIL → **3/3 PASS** |
| `admin-program-form.spec.ts` (9 TC) | 5 PASS·4 FAIL → **9/9 PASS** |

- `admin-program-detail.spec.ts:17` 저장 TC → PASS (seed 가정 보완 반영)
- `admin-program-form.spec.ts` 4 회귀 TC (line 37/98/128/168) → 전부 PASS (경로/셀렉터 갱신 반영)

### Side-effect 영역

```
npx playwright test tests/admin-programs-rbac.spec.ts tests/admin-program-watch.spec.ts --project=chromium
```

**결과: 6/6 PASS · 24.1s**

| 스펙 | 결과 |
|---|---|
| `admin-program-watch.spec.ts` (3 TC) | 3 PASS — watch-button testid 추가가 outerHTML swap·styleClass 왕복에 영향 없음 확인 |
| `admin-programs-rbac.spec.ts` (3 TC) | 3 PASS |

## 5. BLOCKER / 종합 판정

| 분류 | 1차 | 2차 |
|---|---|---|
| BLOCKER (머지 차단) | 2건 | **0건** ✅ |
| 회귀 FAIL | 4건 | **0건** ✅ |
| 정적 검증 | PASS | PASS |
| 동적 검증 | 핵심 경로 200 (watch-button testid 누락 결함 있음) | **핵심 경로 200 + 모든 testid 렌더 확인** ✅ |
| 디자인 계약 | 44/45 | **45/45** ✅ |

**판정: 머지 가능 (ym-verify 로 적대적 검증 권장)**

## 6. 미실행 항목

- 전체 `./gradlew test` 회귀 (admin 범위만 332 TC 수행. 변경 범위상 non-admin 영역 영향 없음으로 판단)
- 시각 확인 (사용자 영역) — 색감·폰트·반응형 미세 디테일. prototype 대비 수치 검증은 contracts 계약 검사가 자동 수행함

## 7. 다음 단계

> 검증 완료. 커밋 전 최종 관문으로 `ym-verify` (적대적 검증) 호출을 권장합니다. 시각 확인 (사용자 영역) 통과 시 머지 진행 가능합니다.
