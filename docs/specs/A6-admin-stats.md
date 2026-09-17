# A6 — 관리자 통계 대시보드 (`/admin/stats`)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-16 사용자 결정: Qn-A~QD + 세부 11건 **모두 권장 A** · birthDate 계산 로직 신설 확인) |
| 브랜치 | `feature/A6-admin-stats` |
| 선행 | A1 admin-shell (#205 · 완결) · A2 (#211) · A5 (#215) |
| 파생 | A7 header-live (`NEW_APPLICATION`·`NEW_USER` 리스너) 는 별개 트랙 |
| 추정 규모 | **M~L** — 파일 20~28 · +2000~2600 LOC · V17~V18 · `@EnableScheduling` 신설 |
| 성격 | **read-only 화면**. 상태 변경 없음. 스케줄러 1건 (심야 집계) + Interceptor 1건 신설 |

---

## 1. 배경 · 스코프

ADMIN-00 §5-A6 원안 + Q5 (HandlerInterceptor + `DailyVisit` 집계) 확정 사항을 이행. A1 에서 `/admin` shell 은 완성했으므로 본 티켓은 **`/admin/stats` 신규 라우트 + `DailyVisit` 수집 인프라 + 성별/연령 도넛 + 프로그램별 통계 테이블**을 다룬다.

### 포함
- `/admin/stats` — 방문자 라인/막대 + 프로그램별 참여 테이블 + 성별/연령 도넛
- `DailyVisit` 엔티티 · Interceptor · 심야 스케줄러 · V17 마이그레이션
- 성별/연령 실측 집계 (User.gender + User.birthDate 파생)
- SVG 자체 렌더 차트 (외부 CDN 금지 · prototype 방식 이식)
- CSV export (프로그램 통계) 는 A8 통합 대상 → **본 티켓 제외**

### 제외 (이월 · deferred 표기 필수)
- 실시간 대시보드 (`deferred: A7`)
- Cohort / 전환율 심화 통계 (`deferred: A6-followup`)
- Excel 다운로드 (`deferred: A8`)
- Redis 캐시 (`deferred: A6-perf` · 초기 데이터량 낮아 불필요)
- 방문자 원시 로그 (`deviation: Q5-A · 개인정보 미저장 원칙`)

---

## 2. 디자인 출처 (3자산 정독)

| 자산 | 위치 |
|---|---|
| prototype.html STATS 블록 | L790~900 (Visitor chart · Program stats table · Gender/Age donut) |
| createBarChart / createDonutChart | prototype.html L3406~3492 / L3494~3511 |
| stats 데이터 (mock) | prototype.html L3608~3666 (chartMode · genderData · ageData) |
| StatsScreen (tsx) | prototype.tsx L466 (스텁만 존재 — 실제 렌더는 html 이 원본) |
| A1 대시보드 계약 | `e2e/contracts/admin-dashboard.ts` + `docs/design-contracts/admin/dashboard.md` |
| HANDOFF 관련 | `docs/00_assets/admin/HANDOFF.md` — 존댓말 톤·`role="dialog"` 규칙 |
| 참고 코드 | `admin/AdminDashboardService.java` (센터 격리 패턴 재사용) |

**주의**: html L806 `<div style="width:100%;height:220px">{{ activeChart }}</div>` 처럼 `activeChart` 는 React 요소 슬롯이므로 서버 렌더링 시 `th:utext` 로 SVG 문자열을 주입한다. prototype 의 `createBarChart` / `createDonutChart` 함수를 **Java 로 이식**한다 (외부 라이브러리 없음).

---

## 3. 자산 간 갭 표

| 항목 | wireframe | prototype.html | 채택 |
|---|---|---|---|
| 방문자 차트 종류 | 방문자 현황 단일 차트 | 월별 막대 + 연도별 라인 **탭 전환** | **prototype** — 탭 전환 |
| 프로그램별 통계 | 단순 리스트 | 신청률 progress bar (≥80% 빨강 / ≥50% 주황) + 조회수 | **prototype** |
| 성별/연령 | 별도 화면 (또는 없음) | Stats 화면 하단 도넛 2개 | **prototype** |
| 무응답 gender | (미정의) | "무응답" 3% 별도 세그먼트 (`#E3E1E8`) | **prototype** — `gender IS NULL` 그룹 |
| 연령 그룹 | (미정의) | 19-24 / 25-29 / 30-34 / 35-39 | **prototype** + 40+ 그룹 추가 (Qn-2) |
| 사이드바 연도별 통계 카드 | 있음 (구버전) | 없음 (전월/전년 대비 % 로 대체) | **prototype** |

---

## 4. 데이터 모델 gap 표 (필수)

### 4-1. DailyVisit (신규)

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long PK | IDENTITY |
| `visitDate` | LocalDate UNIQUE | 집계 기준일 (KST) |
| `uniqueVisitors` | int | 익명·인증 합계 (Qn-3 = A) |
| `totalVisits` | int | 페이지뷰 (path=`/`, `/programs/**`, `/notices/**` 등 admin 제외) |
| `authenticatedVisits` | int | 로그인 사용자 페이지뷰 (Qn-3 세부) |
| `createdAt` / `updatedAt` | BaseTimeEntity | 감사 |

### 4-2. 파생 소스 (엔티티 컬럼 추가 **없음**)

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| `genderData` | `User.gender` (`UserGender` — MALE/FEMALE/OTHER/null) ✅ | 파생 (`COUNT + GROUP BY`) — 컬럼 추가 없음 |
| `ageData` | `User.birthDate` (LocalDate) ✅ | 런타임 계산 — `AgeBucket.of(birthDate, today)` 도메인 헬퍼 신설 |
| `programStats.views` | `Program.viewCount` ✅ (A2 시점 확인) | 파생 |
| `programStats.appliedText` | `ApplicationRepository.countByProgramAndStatus` | 파생 |
| 방문자 전월/전년 대비 % | `DailyVisit` 최근 2개월/2년 SUM 비교 | 파생 |

### 4-3. 도메인 헬퍼 신설

- `AgeBucket` enum (`AGE_19_24`, `AGE_25_29`, `AGE_30_34`, `AGE_35_39`, `AGE_40_PLUS`, `UNKNOWN`) + `of(LocalDate birth, LocalDate today)` 정적 팩토리. `birthDate == null` → `UNKNOWN`

---

## 5. 데이터 소비 지점

`DailyVisit` 은 A6 신설이므로 소비 지점 = **본 티켓만**. 후속 A7 알림·A8 CSV 는 별도 티켓.

| 소비 지점 | prototype 참조 | 조치 |
|---|---|---|
| `/admin/stats` 방문자 차트 | html L794~821 | 본 티켓 신설 |
| `/admin` 대시보드 KPI 카드 (증감 문구) | dashboard.md `deferred:A6` | **본 티켓에서 해소** — 최근 30일 vs 이전 30일 비교로 "전월 대비 +N%" 계산 |

---

## 6. 화면 · 라우팅

### 6-1. GET `/admin/stats`

- 쿼리스트링:
  - `?chartMode=month|year` (default `month`)
  - `?scope=<centerName>` (SYSTEM_ADMIN 만 · CENTER_ADMIN 은 자기 센터 강제)
- 뷰: `templates/admin/stats.html`
- 인증: `hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')`
- 회귀 방지: `/admin` shell fragment 재사용 (`layout.html` · `topnav` GNB 활성 = `stats`)

### 6-2. HTMX 부분 갱신

- 월별/연도별 탭 클릭 → `hx-get="/admin/stats/chart?mode=year"` + `hx-target="#chart-container" hx-swap="innerHTML"` → SVG 만 교체
- 반환: `admin/stats-chart.html` fragment (`th:fragment="chart"`)

### 6-3. 마크업 매핑

| prototype 슬롯 | 서버 렌더 |
|---|---|
| `{{ activeChart }}` (L806) | `th:utext="${chartSvg}"` — String SVG |
| `{{ chartStat1Val }}` etc | `${stats.currentPeriodLabel}` 등 |
| `<sc-for list="{{ programStats }}">` | `th:each="ps : ${programStats}"` |
| `{{ genderChartSvg }}` | `th:utext="${genderChartSvg}"` |
| `<sc-for list="{{ genderData }}">` | `th:each="gd : ${genderData}"` |

---

## 7. 구현 · 컴포넌트

### 7-1. 신규 클래스

| 클래스 | 역할 |
|---|---|
| `stats/DailyVisit` | 엔티티 (BaseTimeEntity 상속) |
| `stats/DailyVisitRepository` | JPA · `findByVisitDate(LocalDate)`, `findAllByVisitDateBetween(start, end)` |
| `stats/VisitTrackingInterceptor` | HandlerInterceptor · `postHandle` 에서 카운트 in-memory 누적 (`ConcurrentHashMap<LocalDate, VisitCounter>`) |
| `stats/VisitAggregator` | `@Scheduled(cron="0 5 0 * * *", zone="Asia/Seoul")` — 자정 5분 후 어제 카운트 flush + DB upsert. **매 요청 DB write 회피** |
| `stats/VisitorPathFilter` | admin `/admin/**` · 정적 리소스 · webjars · actuator 제외 판정 유틸 |
| `admin/AdminStatsController` | `GET /admin/stats` · `GET /admin/stats/chart` |
| `admin/AdminStatsService` | 방문자/성별/연령/프로그램 집계 + SVG 생성 orchestration |
| `admin/chart/BarChartRenderer` | prototype `createBarChart` (월별) 이식 — String SVG 반환 |
| `admin/chart/LineChartRenderer` | prototype `createBarChart(year)` 이식 |
| `admin/chart/DonutChartRenderer` | prototype `createDonutChart` 이식 — 성별/연령 공용 |
| `user/AgeBucket` | enum + `of(LocalDate, LocalDate)` |

### 7-2. Config 변경

- `YouthMoaApplication` 또는 별도 `SchedulingConfig` 에 `@EnableScheduling` 추가 (**신설**)
- `WebMvcConfig` (없으면 신설) 에 Interceptor 등록 + 제외 패턴 (`/admin/**`, `/css/**`, `/js/**`, `/images/**`, `/webjars/**`, `/actuator/**`)
- `application.yml` — 신규 프로퍼티 없음 (cron 은 코드 상수)

### 7-3. 마이그레이션

**V17__create_daily_visit.sql**
```sql
CREATE TABLE daily_visit (
  id BIGSERIAL PRIMARY KEY,
  visit_date DATE NOT NULL UNIQUE,
  unique_visitors INT NOT NULL DEFAULT 0,
  total_visits INT NOT NULL DEFAULT 0,
  authenticated_visits INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_daily_visit_date ON daily_visit(visit_date DESC);
```

---

## 8. 회귀 방지 (최우선)

| 회귀 위험 | 방어 |
|---|---|
| A1 `/admin` 대시보드 shell 레이아웃 깨짐 | `AdminShellRenderTest` · `admin-dashboard.ts` 계약 검사 재실행 필수. layout.html 신규 GNB 항목만 추가·기존 항목 순서 유지 |
| Interceptor 가 사용자 페이지 응답 시간 증가 | `postHandle` 에서 `ConcurrentHashMap.compute` 로 O(1) 누적. DB write 없음. p50 응답시간 +2ms 이내 목표 |
| Interceptor 가 apply/signup flow 오작동 | 제외 패턴: `/api/**` (있으면), `/actuator/**`, HTMX fragment 요청은 `HX-Request` 헤더 있는 케이스도 카운트 (Qn-6 결정 필요) |
| Signup·Login 무영향 | Filter 아닌 Interceptor 라서 SecurityFilter 후단 실행 — 인증 flow 자체는 무영향 |
| `@EnableScheduling` 이 다른 `@Scheduled` 유발 | 현재 프로젝트에 `@Scheduled` 부착 클래스 없음 확인 필요 (grep 검증) |
| Observability Counter (PR #111) 충돌 | 별개 이름공간 — `youth_moa.visit.*` vs 기존 `youth_moa.apply.*` 등. 신설 시 prefix 통일 (`stats.` 사용 안 함) |
| Testcontainers 통합 테스트에서 스케줄러 발화 | `@SpringBootTest(properties="spring.task.scheduling.enabled=false")` 또는 `@Profile("!test")` — Qn-7 |

---

## 9. 테스트

### 9-1. 정적

| 테스트 | 대상 |
|---|---|
| `AdminStatsServiceTest` | `@DataJpaTest` + 시드 · 성별/연령 파생 · 프로그램 신청률 |
| `AgeBucketTest` | 경계값 (생일 당일·전일·1900-01-01·null) |
| `VisitAggregatorTest` | in-memory counter → DB upsert 시나리오 (동일 날짜 재발화 시 덮어쓰기) |
| `VisitTrackingInterceptorTest` | `MockMvc` — 정적/admin/api 제외 확인 + 인증/비인증 카운트 분리 |
| `AdminStatsRenderTest` | Thymeleaf 실 렌더 (F0h-c2 필수) · 3섹션 (chart · programStats · donut) 마크업 검증 |
| `BarChartRendererTest` | SVG XML 파싱 + 12개 rect + y축 tick 3개 확인 |
| `DonutChartRendererTest` | path arc 개수 = 세그먼트 수 · fill 색상 매핑 확인 |

### 9-2. 동적 (curl · bootRun e2e 프로파일 · 8090)

- `curl -o /dev/null -w "%{http_code}" /admin/stats` → 302 (미인증) → 로그인 후 200
- `curl /admin/stats?chartMode=year | grep 'linearGradient'` → 라인 차트 gradient 렌더 확인
- `curl /admin/stats/chart?mode=month` → fragment 200 + `<svg` prefix
- Thymeleaf 표현식 잔존 0건 (`${.*}` grep)

### 9-3. 계약 신설

- `e2e/contracts/admin-stats.ts` — 방문자 카드 · 프로그램 통계 테이블 컬럼 7개 · 도넛 2개 · 색상 팔레트 (Qn-5)
- `docs/design-contracts/admin/stats.md` — 서술 계약

### 9-4. 기능 E2E

- `tests/admin-stats.spec.ts` — 로그인 → `/admin/stats` 진입 → 월별 탭 클릭 → 연도별 탭 클릭 (`hx-swap` 결과 확인) → 성별 도넛 세그먼트 3개 확인
- **인터랙션 조항 (CLAUDE.md #144)**: 탭 클릭 후 SVG 실제 변경 확인 · URL query string 갱신 확인

### 9-5. Visual

- Claude Preview snapshot ↔ prototype.html L790~897 대조 (컬럼 폭·색상·gap)

---

## 10. 결정 필요 항목 (Qn — 15건)

### 핵심 (사용자 결정 필수)

| # | 질문 | 권장안 | 이탈 시 영향 |
|---|---|---|---|
| **Qn-A** | 성별 분포 데이터 소스 | **A: `User.gender` GROUP BY 파생** (실시간, 요청 시 계산 · 캐시 없음). 초기 사용자 수 낮아 부담 없음 | 별도 매트릭 수집은 오버스펙 |
| **Qn-B** | 연령대 계산 방식 | **A: `User.birthDate` 실시간 계산 + `AgeBucket.of()` 도메인 헬퍼**. 캐시 없음 | age 캐시 컬럼 도입 시 생일마다 갱신 필요 (스케줄러 추가) |
| **Qn-C** | DailyVisit 수집 시점 | **A: `HandlerInterceptor.postHandle` in-memory 누적 + 심야 스케줄러 flush**. 로그인 이벤트나 Actuator 메트릭 아님 | 매 요청 DB write 는 p50 응답 악화. 로그인만 카운트 시 익명 방문자 손실 |
| **Qn-D** | 스케줄러 프레임워크 | **A: Spring `@Scheduled` (`@EnableScheduling`)** — Quartz 는 학습 프로젝트 오버스펙 | Quartz 도입 시 별도 DB 테이블 (`qrtz_*`) · 이월 타당성 없음 |

### 세부 (권장안 채택 시 스킵 가능)

| # | 질문 | 권장안 |
|---|---|---|
| Qn-1 | 차트 라이브러리 | **A: 서버 렌더 String SVG** (prototype 이식). Chart.js·ApexCharts CDN 도입 시 SecurityConfig CSP 완화 필요 (**CSP 는 현재 미설정** — grep 결과 확인. 도입해도 무관하지만 학습 목적상 자체 SVG 우선) |
| Qn-2 | 연령 그룹 | **`19-24 / 25-29 / 30-34 / 35-39 / 40+ / UNKNOWN`** — prototype 4구간 + 40+ 추가 (40대 이상 사용자 대비) |
| Qn-3 | 방문자 정의 | **A: 익명 세션 + 인증 사용자 합산** (uniqueVisitors) + **authenticatedVisits 별도 컬럼** — DB 에 두 값 모두 저장, 화면은 total 노출 |
| Qn-4 | 캐시 전략 | **A: 매 요청 계산** — 초기 규모 낮음. 사용자 100만 명 이상 시 Redis 도입 (deferred: A6-perf) |
| Qn-5 | 도넛 색상 팔레트 | **prototype 매핑 그대로**: `#3F30E9`(primary) → `#8B7FF0`(secondary) → `#D6CFFA`(light) → `#E3E1E8`(muted). design-tokens.css 에 `--admin-chart-palette-*` 5색 신설 |
| Qn-6 | HTMX fragment 요청 방문자 카운트 여부 | **A: 포함** — 사용자 실제 조작이므로 (즐겨찾기 토글·검색 등). `HX-Request` 헤더 있어도 총 페이지뷰에 합산. Interceptor 는 모든 200 응답 카운트 |
| Qn-7 | 테스트 시 스케줄러 disable 방식 | **A: `spring.task.scheduling.enabled=false` in `application-test.yml`** — 테스트 시 flush 안 함. 통합 테스트가 aggregator 발화하지 않도록 |
| Qn-8 | 방문자 라인차트 레거시 데이터 부재 | **A: DailyVisit 없는 기간은 0으로 표시 + 회색 tick + "데이터 없음" 라벨** — 신설 후 첫 30일간 |
| Qn-9 | KPI 카드 증감 문구 | **A: 최근 30일 vs 이전 30일 방문자 SUM 비교 % — dashboard.md deferred:A6 해소** |
| Qn-10 | 마감임박 D-day 기준 | **A: `applyEndDate` 필드 있으면 그것 · 없으면 `endDate` 파생** — A3 에서 `applyEndDate` 도입됐는지 impl 시점 확인 필요 (dashboard.md deferred:A3 해소 시도) |
| Qn-11 | Actuator 노출 | **A: 현행 유지** — Qn-C 결정으로 Actuator metrics 사용 안 함. `/actuator/health` 만 노출 (기존 P0-4 결정) |

---

## 11. 스코프 예상 규모

| 항목 | 예상 |
|---|---|
| 신규 파일 | ~22 (엔티티 1 · repo 1 · interceptor 1 · scheduler 1 · service 3 · controller 1 · renderer 3 · config 1 · template 2 · css 조각 1 · test ~8) |
| 수정 파일 | ~6 (`YouthMoaApplication` `@EnableScheduling` · `WebMvcConfig` · `admin.css` · `admin/layout.html` GNB active · `AdminDashboardService` KPI 증감 문구 · `application-test.yml`) |
| LOC | +2000 ~ +2600 |
| 마이그레이션 | V17 (필요시 V18 은 사용 안 함) |
| PR 분할 | **단일 PR 권장** — infra(V17+interceptor+scheduler) 와 UI 분리 시 첫 PR 이 dead code 상태로 머지됨 · 원자성 우선 |

---

## 12. deviation · deferred 요약

| 항목 | 종류 | 사유 |
|---|---|---|
| 실시간 대시보드 | `deferred: A7-realtime` | HTMX polling·SSE 트랙 별도 |
| Cohort 분석 | `deferred: A6-followup` | 사용자 규모 도달 시 |
| Excel export | `deferred: A8` | CSV 도 함께 A8 통합 |
| 방문자 원시 로그 (path·referrer·userAgent) | `deviation: Q5-A 개인정보 미저장 원칙` | 일별 집계만 유지 |
| 사용자별 방문 히스토그램 | `deferred: A6-followup` | |
| 실시간 chart hover 툴팁 (prototype `chartHoverIdx`) | `deferred: A8-polish` | 서버 렌더 SVG 에는 상시 툴팁 없이 정적 렌더. hover 는 클라 JS 추가 시 지원 |

---

## 13. 다음 단계 인계

> 명세 산출 완료했습니다. 결정 필요 항목 15건 (핵심 4 + 세부 11) 에 대한 사용자 컨펌 후 `ym-impl` 으로 인계합니다. 특히 **Qn-A ~ Qn-D 4건은 아키텍처 갈래**라 반드시 확정 후 진행합니다.

---

## 14. §후속 — 사용자 결정 반영 (2026-09-16)

### 핵심 4

| Qn | 결정 | 근거 |
|---|---|---|
| **QA** 성별 소스 | `User.gender` GROUP BY 실시간 | 캐시 없음 · 매 요청 계산 |
| **QB** 연령 계산 | `AgeBucket.of(birthDate)` 실시간 | **birthDate 저장만 있고 계산 로직 지금까지 없었음. A6 에서 처음 도입** |
| **QC** DailyVisit 수집 | `HandlerInterceptor.postHandle` in-memory 누적 + 심야 스케줄러 flush | p50 +2ms 이내 목표 |
| **QD** 스케줄러 | Spring `@Scheduled` | Quartz 오버스펙 |

### 세부 11 (모두 A)

- Qn-1 차트 = **서버 렌더 SVG** (외부 CDN 회피 · CSP 안전 · prototype `createBarChart`/`createDonutChart` Java 이식)
- Qn-2 연령 5구간 + UNKNOWN (19-24 · 25-29 · 30-34 · 35-39 · 40+)
- Qn-3 방문자 = 익명+인증 합산 + 별도 컬럼 (`authenticatedVisits`)
- Qn-4 캐시 없음 (실시간)
- Qn-5 도넛 색상 팔레트 = design-token 정합
- Qn-6 HTMX fragment 카운트 (부분 갱신)
- Qn-7 테스트 시 `spring.task.scheduling.enabled=false`
- Qn-8 레거시 데이터 0표시 (UNKNOWN 은 0 이 아님)
- Qn-9 KPI 증감 = 30일 전 대비 (A1 deferred 해소)
- Qn-10 마감임박 기준 = `applyEndDate` 우선
- Qn-11 Actuator 현행 유지 (충돌 X)

### birthDate 계산 로직 신설 확인 (사용자 질문 반영)

- 현재 `User.birthDate: LocalDate` 저장만 있고 나이·연령대 계산 로직 **어디에도 없음** (2026-09-16 실측)
- SignUp / ProfileUpdate 에서 입력 · V1 baseline 컬럼 · DataInitializer 시드값 존재
- `ProgramEligibility.age` 는 문자열 (`"만 19세 이상"`) 저장 · 계산 X
- A6 에서 `AgeBucket` enum + `AgeBucket.of(birthDate)` 도메인 헬퍼 **최초 신설**
- 기존 코드 영향 없음 (신규 도메인 · 통계 집계 전용)

### V17 마이그레이션 확정

```sql
CREATE TABLE daily_visit (
    id BIGSERIAL PRIMARY KEY,
    visit_date DATE NOT NULL UNIQUE,
    unique_visitors INTEGER NOT NULL DEFAULT 0,
    total_visits INTEGER NOT NULL DEFAULT 0,
    authenticated_visits INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_daily_visit_date ON daily_visit (visit_date DESC);
```

### 회귀 방어 최우선 3점

1. **A1 dashboard shell 무회귀** — `admin-dashboard.ts` 계약 재검사 필수
2. **Interceptor 성능** — p50 +2ms 이내 · in-memory 누적 · DB write 심야 1회
3. **Observability PR #111 Counter 충돌 없음** — 별개 이름공간 · Actuator 현행 유지

### 다음 액션

`ym-impl` 인계 프롬프트에 위 결정 반영. 특히 회귀 방어 3점은 명시적 검증 요구.

### 관련 파일 (참조 절대경로)

- 마스터 지시서: `C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\ADMIN-00-master-directive.md` §5-A6
- prototype 원본: `C:\Users\User\IdeaProjects\youth-moa-java\docs\00_assets\admin\prototype.html` L790~900 · L3406~3511
- 기존 대시보드 계약: `C:\Users\User\IdeaProjects\youth-moa-java\docs\design-contracts\admin\dashboard.md`
- 기존 dashboard 서비스: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\admin\AdminDashboardService.java`
- User 엔티티 (gender·birthDate): `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\user\User.java` L58~62
- Flyway 최신: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\db\migration\V16__add_users_active_and_admin_fields.sql`
- 신설 대상 계약: `e2e/contracts/admin-stats.ts` + `docs/design-contracts/admin/stats.md` (impl 시 생성)

---

## 15. 구현 매핑 (impl 완료 · 2026-09-16)

행 단위 spec → 파일:라인 매핑. CLAUDE.md `ym-impl` 규정에 따라 필수.

| Spec 항목 | 코드 |
|---|---|
| §4-1 DailyVisit 엔티티 | `src/main/java/io/github/sihyuuun/youthmoa/stats/DailyVisit.java` |
| §4-1 V17 마이그레이션 | `src/main/resources/db/migration/V17__create_daily_visit.sql` |
| §4-3 AgeBucket 헬퍼 | `src/main/java/io/github/sihyuuun/youthmoa/user/AgeBucket.java` |
| §6-1 GET /admin/stats | `admin/AdminStatsController.java:26` |
| §6-2 HTMX chart fragment | `admin/AdminStatsController.java:42` (반환 `admin/stats :: chart`) |
| §7-1 Repository | `stats/DailyVisitRepository.java` |
| §7-1 Interceptor | `stats/VisitTrackingInterceptor.java` (`postHandle`) |
| §7-1 Scheduler | `stats/DailyVisitScheduler.java` (`@Scheduled cron="0 0 2 * * *"`) |
| §7-1 Aggregator (Collector) | `stats/DailyVisitCollector.java` (ConcurrentHashMap + LongAdder) |
| §7-1 BarChart / LineChart / DonutChart | `admin/chart/SvgChartRenderer.java` |
| §7-1 AdminStatsService | `admin/AdminStatsService.java` |
| §7-2 @EnableScheduling + WebMvcConfig | `common/config/WebMvcConfig.java` |
| §7-2 Interceptor 제외 패턴 | `WebMvcConfig#addInterceptors` — /admin/**, /actuator/**, /css/**, /js/**, /images/**, /webjars/**, /favicon.ico, /error, /login, /logout, /h2-console/**, /__test__/** |
| §7-3 V17 스키마 | 위와 동일 |
| §8 Interceptor 성능 | LongAdder + ConcurrentHashMap · DB write 없음 (postHandle) |
| Qn-1 서버 SVG | `SvgChartRenderer` (외부 CDN 없음) |
| Qn-2 연령 5+UNKNOWN | `AgeBucket` enum 6개 |
| Qn-3 방문자 = 익명+인증 합산 + authenticatedVisits 별도 | `DailyVisit.uniqueVisitors` + `authenticatedVisits` 컬럼 |
| Qn-5 도넛 팔레트 | `SvgChartRenderer.DONUT_PALETTE` |
| Qn-6 HTMX fragment 카운트 | Interceptor path 필터에 특별 제외 없음 (HX-Request 포함) |
| Qn-7 테스트 스케줄러 disable | `src/test/resources/application.properties` `spring.task.scheduling.enabled=false` + `DailyVisitScheduler` `@ConditionalOnProperty` |
| Qn-9 KPI 증감 30일 | `AdminStatsService#pctDelta` (cur30 vs prev30) |
| Qn-10 마감임박 applyEndDate 우선 | `AdminStatsService#deadlineOf` |
| §9-3 계약 신설 | `e2e/contracts/admin-stats.ts` + `docs/design-contracts/admin/stats.md` |
| §9-1 정적 테스트 | `AgeBucketTest` · `DailyVisitCollectorTest` · `SvgChartRendererTest` |
| GNB 통계 링크 | `templates/admin/fragments/header.html` L46~48 |

### 이월 (impl 미포함)

| 항목 | 사유 |
|---|---|
| Program.viewCount (조회수) | 엔티티에 컬럼 미도입. 화면은 0 표시 · `deferred: A6-followup` |
| Excel export | `deferred: A8` |
| 차트 hover 툴팁 | 서버 SVG 정적 렌더. `deferred: A8-polish` |
| 기능 E2E (`tests/admin-stats.spec.ts`) | 계약(contracts) 검사로 우선 커버. 기능 E2E 는 후속 (ym-qa 단계 확장) |
| Visual E2E (`tests/visual-admin-stats.spec.ts`) | 상동 (ym-qa 단계 확장) |
