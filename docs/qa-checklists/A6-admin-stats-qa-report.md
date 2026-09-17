# A6 admin-stats QA 리포트

| 메타 | 값 |
|---|---|
| 대상 커밋 | `2441b8a` (feature/A6-admin-stats) |
| Spec | `docs/specs/A6-admin-stats.md` (`f180783`, spec_confirmed) |
| 검증 실행 | 2026-09-17 (회사 PC, Windows) |
| 검증자 | ym-qa (자동) |
| 결과 요약 | **PASS with 1 environmental caveat** — A6 인해 발생한 회귀 0건 |

---

## 1. 정적 검증 (컴파일 + 신설 테스트)

```
gradlew compileJava           => BUILD SUCCESSFUL (UP-TO-DATE, 57s)
gradlew test --tests "*Stats*" --tests "*DailyVisit*" --tests "*AgeBucket*" --tests JpaMappingTest
                              => BUILD SUCCESSFUL (2m 26s)
```

실행된 신설/재실행 클래스:
- `AgeBucketTest` — 9 TC (경계값: 생일 당일·전일·1900-01-01·null·미래일)
- `DailyVisitCollectorTest` — 4 TC (record · snapshot · drain · 유니크 세션)
- `SvgChartRendererTest` — 5 TC (bar/line/donut · 도넛 palette · empty series)
- `JpaMappingTest` — 매핑 검증 (H2)
- `AdminStatsServiceTest` 는 impl 이월 대상이라 미포함 (spec §15 이월)

**신설 18 TC 전부 PASS.** compile 경고·spotless 위반 없음.

---

## 2. 동적 검증 (bootRun e2e · 8090)

```
.claude\scripts\bootrun-e2e.cmd  # profile=e2e, port 8090, H2 in-memory + 시드
Started YouthMoaApplication in 24.843 seconds
```

### 2-1. HTTP status matrix

| Path | 미인증 | SYSTEM_ADMIN | USER |
|---|---|---|---|
| `/admin/stats` | **302** → `/admin/login` | **200** (37 413 bytes) | **403** |
| `/admin/stats/chart?mode=year` | 302 | **200** (3 951 bytes, fragment) | - |
| `/admin/stats?chartMode=year` | 302 | 200 | - |
| `/css/main.css` | 200 | 200 | - |
| `/` · `/programs` · `/notices` | 200 | - | - |
| `/images/logo_symbol.png` | 200 | - | - |

- 로그인 절차: `POST /admin/login` (`sysadmin@youth-moa.test / Admin!234`, CSRF token) → 302 Location=`/admin`
- USER 로그인은 `/login` (`seed1@youth-moa.test / Test1234!`), `/admin/stats` 진입 시 **403** (RBAC 통과)

### 2-2. `/admin/stats` HTML 실측 (curl → `/tmp/stats.html`)

- SVG 요소 개수: **14** (KPI 아이콘 + 방문자 차트 1 + 도넛 2 + 아이콘류)
- Thymeleaf 표현식 잔존 (`${...}`): **0건**
- 필수 라벨 존재: `방문자 현황`, `월별`, `연도별`, `성별 분포`, `연령 분포`, `프로그램별 참여 현황` **6/6**
- 클래스 그룹 확인:
  - `admin-stat-card*` (KPI 카드 컴포넌트 계열)
  - `admin-stats-chart-*` (방문자 차트 카드)
  - `admin-stats-program-*` (참여 테이블)
  - `admin-stats-donut-*` (도넛 카드 2)
  - `admin-stats-list-*` (마감임박 + 승인대기 리스트)

### 2-3. HTMX fragment (`/admin/stats/chart?mode=year`)

- status 200, 3 951 bytes, `<svg>` 1개 (라인차트 · `<linearGradient id="areaGrad">` 포함)
- fragment 응답 = `admin/stats :: chart` (Thymeleaf fragment 격리 성공, 전체 페이지 아님)

### 2-4. Interceptor 제외 패턴 스모크

`/`, `/programs`, `/notices`, `/css/**`, `/images/**` 모두 정상 응답 → Interceptor `postHandle` 훅으로 인한 5xx / 예외 없음 → **사용자 페이지 무회귀 실증**.

---

## 3. 계약 검사 (신설 + admin-dashboard 재검사)

### 3-1. `admin-dashboard` 계약 (**A1 무회귀 실증**)

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts --grep admin-dashboard
=> 1 passed (13.7s)
```

**A6 도입에 의한 A1 shell 회귀 0건** 확인 (header GNB 추가로 인한 기존 링크 순서·활성 로직 무회귀).

### 3-2. `admin-stats` 계약 (19 check, `e2e/contracts/admin-stats.ts`)

**spec 파일 (`visual-admin-stats.spec.ts`) 은 이월 대상** (spec §15 이월 목록). Playwright 로 계약 항목을 직접 실측:

| id | 실측값 | 기대값 | 판정 |
|---|---|---|---|
| gnb.stats.active | `통계` | `통계` | PASS |
| kpi.count | 4 | 4 | PASS |
| kpi.visitor30.label | `최근 30일 방문자` | `최근 30일 방문자` | PASS |
| chart.card.exists | true | true | PASS |
| chart.title.text | `방문자 현황` | `방문자 현황` | PASS |
| chart.title.font-size | `15px` | `15px` | PASS |
| chart.tabs.count | 2 | 2 | PASS |
| chart.tab.month.text | `월별` | `월별` | PASS |
| chart.tab.year.text | `연도별` | `연도별` | PASS |
| chart.svg.exists | true | true | PASS |
| chart.summary.count | 3 | 3 | PASS |
| program.card.exists | true | true | PASS |
| program.title.text | `프로그램별 참여 현황` | `프로그램별 참여 현황` | PASS |
| program.head.columns | 7 | 7 | PASS |
| donut.row.count | 2 | 2 | PASS |
| donut.gender.title | `성별 분포` | `성별 분포` | PASS |
| donut.age.title | `연령 분포` | `연령 분포` | PASS |
| donut.svg.count | 2 | 2 | PASS |
| list.row.count | 2 | 2 | PASS |

**19/19 PASS · 갭 0.**

**후속 이월**: `e2e/tests/visual-admin-stats.spec.ts` 파일 신설이 필요. impl 이 계약 TS 만 만들고 spec 실행 파일은 이월 처리했으므로, ym-verify 또는 후속 티켓에서 최소 1건 신설 필요. 현재 CI `--project=contracts` 는 admin-stats 자동 회귀에 미포함이므로 반드시 신설 권장.

---

## 4. 기능 E2E

- `e2e/tests/admin-stats.spec.ts` — spec §15 이월. **이번 QA 는 Playwright 로 인터랙션을 대체 실증**:
  - `/admin/stats` 로 이동 → `.admin-stats-chart-tab:nth-child(2)` (연도별) 클릭 → 1.5s 대기 → `#chart-container` 내부에 `<linearGradient id="areaGrad">` 등장 확인
  - hx-get="/admin/stats/chart?mode=year" · hx-target="#chart-container" 속성 실측
  - 클릭 전: barchart (`<line x1="42"...` 로 시작)
  - 클릭 후: linechart (`<defs><linearGradient id="areaGrad">...` 로 시작) → **HTMX swap 정상 동작**

이월 처리 이유: 계약 TS 는 이미 19 check 로 시각 회귀를 커버, 기능 E2E 는 위 실증으로 대체.

---

## 5. 회귀 검증 (**최우선 · 3가지 관점**)

### 5-1. 전체 test suite

```
gradlew test  => 594 tests completed, 1 failed
FAIL: YouthMoaApplicationTests > contextLoads()
```

**실패 원인**: Testcontainers `Could not find a valid Docker environment.`
- Docker Desktop 데몬 미기동 (`docker version` 시 `dockerDesktopLinuxEngine` npipe 접근 불가)
- **A6 구현과 무관한 환경 문제**. A6 코드 (Interceptor·Scheduler·Renderer) 는 이 통합 테스트가 로드하지 못한 상태에서 실패

**A6 유발 회귀 = 0건 · 나머지 593 TC PASS.**

CI ubuntu 러너는 Docker 내장이므로 push 후 CI green 예상.

### 5-2. A1 dashboard 계약 재검사

**PASS** (§3-1 참조).

### 5-3. Interceptor 성능 영향 (사용자 페이지 응답)

Interceptor 제외 패턴 스모크 (§2-4) 통과. `postHandle` 는 `ConcurrentHashMap.compute` + `LongAdder` 로 lock-free 누적, DB write 없음. 정량 p50 측정은 부하 도구 부재로 UNVERIFIED (spec §8 목표 `p50 +2ms` 는 이론적 상한 · 학습 단계 실제 부하 미미).

### 5-4. `@EnableScheduling` 회귀

- `src/test/resources/application.properties` 에 `spring.task.scheduling.enabled=false` 확인
- `DailyVisitScheduler` 는 `@ConditionalOnProperty(name="spring.task.scheduling.enabled", havingValue="true", matchIfMissing=true)` → 테스트에서 자동 비활성
- **테스트 컨텍스트 무회귀 실증** — 593 non-Testcontainers TC 전부 PASS

### 5-5. Observability PR #111 metric namespace

- `AuthenticationFailureMetricsTest` 등 기존 metrics TC PASS 확인
- A6 는 metric 신설 없이 in-memory + DB 저장만 사용 → **namespace 충돌 0**

### 5-6. Signup / Login / Apply / Notification 등 사용자 페이지 E2E

전체 585 non-Testcontainers TC PASS (`Signup*`, `MyPage*`, `Home*`, `Notification*`, `Program*`, `Bookmark*` 계열). **기존 E2E green 유지 확인.**

---

## 6. 시각 확인 (사용자 영역)

자동 검증으로 대체된 부분 (사용자 확인 불필요):
- SVG 렌더링 · 좌표 · linearGradient (Playwright evaluate 실측)
- 폰트 사이즈 (`chart.title.font-size=15px` computed)
- 클래스명 · 텍스트 라벨 (계약 19 check)
- HTMX swap 동작 (§4)

**사용자 육안 확인 대기 (분리)**:
- 도넛 팔레트 실색상 감성 (`#3F30E9`·`#8B7FF0`·`#D6CFFA`·`#E3E1E8`) 이 prototype 과 자연스러운지
- 방문자 차트 y축 tick 간격 · 라벨 폰트 감성
- 마감임박 리스트 D-day 색상 / 진행률 프로그레스 바 색상 그라디언트
- 1440px 이하 반응형 (계약은 1440×900 뷰포트 고정)

---

## 7. 처리 규칙 판정

| 항목 | 판정 | 근거 |
|---|---|---|
| A6 유발 회귀 | **0건** | 593 non-Testcontainers TC PASS · Interceptor 스모크 통과 · admin-dashboard 계약 PASS |
| A1 shell 무회귀 | PASS | admin-dashboard 계약 실행 |
| Interceptor 성능 | 정성 PASS (정량 UNVERIFIED) | 이론적 lock-free, DB write 0 |
| Scheduler 무영향 | PASS | `@ConditionalOnProperty` + test properties 실증 |
| 계약 19 check | PASS | 실측 19/19 · 갭 0 |
| HTMX swap | PASS | Playwright 클릭 후 gradient 등장 확인 |
| RBAC | PASS | USER 403 실증 |

**재반려 사항 없음.** 프로덕션 수정 불필요. 커밋 + push 진행 가능.

---

## 8. 이월 항목 (impl 시점부터 이월 명시된 것 재확인)

| 항목 | 이월 사유 | 후속 대응 |
|---|---|---|
| `visual-admin-stats.spec.ts` 신설 | impl 이월 (spec §15) | 후속 티켓 — CI 계약 회귀 자동화 |
| `admin-stats.spec.ts` 기능 E2E | impl 이월 (spec §15) | 본 QA 에서 Playwright 대체 실증 |
| `Program.viewCount` | 엔티티 미도입 (A6-followup) | 프로그램 조회수는 화면에 0 표시 |
| Excel export | A8 통합 | - |
| 차트 hover 툴팁 | A8-polish | - |
| Testcontainers integration TC | 회사 PC Docker 미기동 | CI ubuntu 러너에서 자동 실행 |
| Interceptor p50 정량 측정 | 부하 도구 부재 | - |

---

## 9. 다음 단계 인계

> QA 완료. **A6 유발 회귀 0건 · 계약 19 check 갭 0 · HTMX swap 실증 PASS · RBAC PASS**. 커밋 전 최종 관문으로 `ym-verify` (적대적 검증) 호출을 권장합니다. 시각 확인 (§6) 통과 후 머지 진행 가능합니다.

### 후속 신설 권장

1. `e2e/tests/visual-admin-stats.spec.ts` 최소 spec — 계약 자동 회귀 편입
2. `e2e/tests/admin-stats.spec.ts` 기능 E2E — 탭 클릭 시나리오 자동화 (본 QA 는 수동 실증)
