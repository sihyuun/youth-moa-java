# 검증 리포트: D-admin-calendar / commit a2790c5b4d11dfc1e (미커밋 작업트리)

> 작업: `/admin/programs?view=calendar` 신설. 42셀 월간 캘린더 + 뷰 토글 세그먼트 + 300px slide 패널.
> ym-spec: `a70601edd02b47cba` · ym-impl: `a2790c5b4d11dfc1e` (feature/D-admin-calendar, uncommitted)
> QA 환경: Windows 11, JDK 17.0.14 (build) / JDK 21 (bootRun), Gradle 9.8.1, bootRun on **:8090** (e2e profile, 기존 세션 재사용)
> QA 일시: 2026-10-12

---

## 1. 결정 반영 확인 (Q0~Q10)

- Q0 admin 트랙: `AdminProgramController` 에 `view=calendar` 분기 추가 — ✅
- Q1 CalendarGridBuilder 추출: `common/calendar/CalendarGridBuilder.java` 신설, 사용자 `ProgramCalendarService` 리팩터로 공용화 — ✅ (리팩터 회귀 10 TC PASS)
- Q2 42셀: `CalendarGridBuilderTest.build42Cells / 2026-08: cells[36]=31 / 2026-08-01` 등 10 TC PASS — ✅
- Q3 300px slide panel: `admin-program-calendar.spec.ts §3` "셀 클릭 → 우측 패널 slide-in" PASS, contracts `grid.42cells` 외 패널 selector 포함 — ✅
- Q4 월간만: 주/리스트 뷰 selector 자체 없음, 계약 문서에 명시 — ✅
- Q5 read-only: 캘린더에 CRUD action 미도입 — ✅
- Q6 SUSPENDED 포함: 범례 "종료 · 운영중단" + `AdminProgramCalendarServiceTest` SUSPENDED 포함 테스트 PASS — ✅
- Q7 URL pass-through: `admin-program-calendar.spec.ts §5` q·status pass-through PASS — ✅
- Q8 카드 placeholder: `AdminProgramListRenderTest` card view placeholder TC PASS — ✅
- Q9 F0f CSS 재사용: contracts selector `.program-calendar-grid .program-calendar-cell` 그대로 적중 — ✅
- Q10 POLICY P-6: `docs/design-contracts/POLICY.md` §P-6 신규 명시, contracts `grid.42cells` 42 enforced — ✅

---

## 2. 정적 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` | **SUCCESS** (UP-TO-DATE) |
| `CalendarGridBuilderTest` | **10 TC PASS** (failures=0 errors=0) |
| `ProgramCalendarServiceTest` (리팩터 회귀) | **10 TC PASS** |
| `AdminProgramCalendarServiceTest` | **7 TC PASS** |
| `AdminProgramListRenderTest` | **9 TC PASS** |
| 신규 TC 합계 | **36 TC PASS, 실패 0건** (스펙 10+10+7+9와 완전 일치) |

리포트 파일 경로:
- `build/test-results/test/TEST-io.github.sihyuuun.youthmoa.common.calendar.CalendarGridBuilderTest.xml`
- `build/test-results/test/TEST-io.github.sihyuuun.youthmoa.program.ProgramCalendarServiceTest.xml`
- `build/test-results/test/TEST-io.github.sihyuuun.youthmoa.admin.program.AdminProgramCalendarServiceTest.xml`
- `build/test-results/test/TEST-io.github.sihyuuun.youthmoa.admin.AdminProgramListRenderTest.xml`

---

## 3. 회귀 검증 (admin.* + program.* + common.calendar.*)

```
./gradlew test --tests "io.github.sihyuuun.youthmoa.admin.*" \
               --tests "io.github.sihyuuun.youthmoa.program.*" \
               --tests "io.github.sihyuuun.youthmoa.common.calendar.*"
```

- 결과: **BUILD SUCCESSFUL** (14m 11s)
- 집계: **473 TC PASS, failures=0 errors=0 skipped=0**
- 주요 통과 (일부 발췌):
  - `AdminApplicationServiceTest` 24 TC
  - `AdminProgramServiceTest` 11 TC
  - `AdminProgramDuplicateServiceTest` 16 TC
  - `AdminProgramDetailRenderTest` 7 TC
  - `AdminUserServiceTest` 38 TC
  - `ProgramListRenderTest` 13 TC
  - `ProgramServiceFilterTest` 7 TC
  - `V21DefensiveBlockIntegrationTest` 2 TC
- **CalendarGridBuilder 추출 리팩터로 ProgramCalendarService 를 공용화한 영향 — 사용자 캘린더 서비스 테스트·렌더 테스트 포함 전수 그린.**

---

## 4. 동적 검증 (bootRun :8090 e2e profile)

### 4-1. Preview/curl 수준 (unauth probe)

- `GET /admin/programs?view=calendar` → **302** (로그인 리다이렉트, SecurityConfig 정상 보호)
- `GET /admin/programs?view=card` → **302**
- `GET /admin/programs?view=list` → **302**

실제 뷰 렌더링은 아래 Playwright E2E (admin 세션 포함) 로 전수 검증.

### 4-2. 신규 기능 E2E (admin-program-calendar.spec.ts, chromium)

| # | 시나리오 | 결과 |
|---|---|---|
| 1 | 뷰 토글 세그먼트 3버튼 렌더 + 캘린더 클릭 → view=calendar URL | **PASS** (16.0s) |
| 2 | 월 이동 arrow 클릭 → year/month URL 갱신 | **PASS** (11.7s) |
| 3 | 셀 클릭 → 우측 패널 slide-in (hidden 해제) + 날짜 라벨 갱신 | **PASS** (8.9s) |
| 4 | 패널 × 버튼 → 패널 close + 셀 선택 해제 | **PASS** (9.5s) |
| 5 | q·status URL 파라미터 → 월 이동 nav 링크 pass-through | **PASS** (6.7s) |

→ **5/5 PASS**. Q3·Q7·Q9 핵심 상호작용 전수 검증.

### 4-3. 특별 체크 포인트 (impl 인계)

- `th:if + th:replace` 함정 — `_calendar-fragment.html` + `list.html` 템플릿 파싱 성공 (E2E 진입 자체가 템플릿 OK 의 proof) → ✅
- F0f fragment / CSS 공유 — F0f programs-calendar.spec.ts 23 TC + contracts 7+3 TC 전수 PASS (아래 4-5) → **침입 영향 0** ✅
- SUSPENDED pill 색 매핑 — contracts `grid.42cells` + `admin-programs-calendar.ts` 적용. 범례 "종료 · 운영중단" 라벨 ✅ (별도 색 분리 아님 — spec 결정)

---

## 5. 디자인 계약 검증 (`--project=contracts`)

### 5-1. 신규: `admin-programs-calendar`

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-programs-calendar
```

- `tests/visual-admin-programs-calendar.spec.ts` **1 passed (33.6s)** — 전체 check PASS, 갭 0건

### 5-2. 사용자 F0f 회귀: `visual-programs-calendar`

- 4 passed (41.5s) — 공통 CSS 재사용으로 **F0f 계약 영향 0** 확인

---

## 6. E2E 회귀 — 사용자 F0f (`programs-calendar.spec.ts`, chromium)

**23/23 PASS** (3.1m) — view 토글, status 유지, [오늘] 버튼, pill 버블링 차단, HTMX 팝오버 호환, D-N chip, 모바일 시트 브레이크포인트 등 전수. **F0f 영향 0** 재확인.

---

## 7. side-effect 스캔 결과

| 영역 | 영향 분석 | 결과 |
|---|---|---|
| `CalendarGridBuilder` 추출 | `ProgramCalendarService` 공용 유틸 사용으로 단순화 (98 line → ~delta -98/+98). 10 TC 리팩터 회귀 PASS | ✅ 안전 |
| `admin.css` 신규 클래스 (`admin-program-view-toggle` 등 52 line 추가) | 네임스페이스 격리 (admin- 접두사), F0f CSS 침범 0 | ✅ 안전 |
| `admin/program/list.html` (35 line 수정) | fragment include 패턴. 템플릿 파싱 OK (E2E 통과) | ✅ 안전 |
| `AdminProgramController` (29 line 추가) | `view=calendar` 분기 추가. 기존 경로 영향 없음 (list/card view E2E 회귀 포함 PASS) | ✅ 안전 |
| `AdminProgramService` (13 line 추가) | 신규 메서드 추가만. 기존 메서드 무변경 | ✅ 안전 |

---

## 8. 시각 확인 (사용자 영역, 대기)

Preview 도구 세션 미보유 환경이라 실제 reader-visible 색상/간격/애니메이션은 사용자 확인 요망:

- ① 300px slide 패널 slide-in 애니메이션 질감 (E2E 는 open/close 상태 검증, 움직임 자체는 미검증)
- ② SUSPENDED pill 색 — ended 색 재사용 시 사용자에게 "운영중단"이 "종료"처럼 보이는지 UX 체감
- ③ 모바일 미대응 상태 (admin 데스크톱 위주 설계) — iPad/9인치 태블릿 체감
- ④ 뷰 토글 세그먼트 active indicator 선 두께·색

---

## 9. 미실행 (환경 한계)

- Preview 도구 (preview_snapshot/inspect/screenshot) 세션 없음 — 색상·spacing computed style 실측 생략 (E2E count/toBeVisible 수준으로 대체)
- 다중 브라우저 (webkit/firefox) 미실행 — chromium 전용 (프로젝트 표준)
- Gap-report 산출 안 됨 (contracts PASS 면 report 생성 skip — gap-reports 는 FAIL 시만, memory `feedback_gap_reports_untracked.md` 참조)

---

## 10. 작업 큐 메타

- 상태: **qa_done**
- 머지 가능 여부: **시각 확인 (§8) 통과 후 가능**
- 커밋 전 ym-verify (적대적 검증) 권장

---

## 11. 요약

| 검증 영역 | 결과 |
|---|---|
| 정적 (compile + 신규 36 TC) | ✅ 36/36 PASS |
| 회귀 (admin + program + calendar 473 TC) | ✅ 473/473 PASS |
| 동적 (admin 신규 E2E 5 TC) | ✅ 5/5 PASS |
| 동적 (F0f 사용자 캘린더 회귀 23 TC) | ✅ 23/23 PASS |
| 디자인 계약 (admin-programs-calendar) | ✅ 전수 PASS 갭 0 |
| 디자인 계약 (F0f 회귀) | ✅ 4/4 PASS |
| 시각 (사용자 영역) | ⏳ 대기 (§8 4항목) |

**정적·동적·계약·회귀 전 영역 그린. 시각 확인 통과 시 머지 가능.**
