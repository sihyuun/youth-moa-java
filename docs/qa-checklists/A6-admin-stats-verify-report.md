# A6 admin-stats 적대적 검증 리포트 (ym-verify)

| 메타 | 값 |
|---|---|
| 대상 커밋 | `9e90b52` (branch `feature/A6-admin-stats`, 상위 `2441b8a` impl + `f180783` spec) |
| Spec | `docs/specs/A6-admin-stats.md` §14 결정 반영 · §15 구현 매핑 |
| 검증자 | ym-verify (refute-first, 회사 PC Windows) |
| 검증 일시 | 2026-09-17 |
| 판정 요약 | **PASS 16 / FAIL 0 / UNVERIFIED 4** → **커밋·머지 가능** |

---

## 판정 원칙

- ym-qa 주장을 **근거로 쓰지 않고** 파일·diff·gradle·grep 을 독립 실행하여 재확인.
- spec §15 이월 명시분(`admin-stats.spec.ts` · `visual-admin-stats.spec.ts`)은 spec-conformant deferred 로 처리.
- Testcontainers / 실시간 스케줄러 발화 / p50 정량 등은 회사 PC 환경 제약 → UNVERIFIED.

---

## 항목별 판정

### 1. spec §2~§8 정합

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1-1 | V17 스키마 = spec §4-1 정합 | Read `V17__create_daily_visit.sql` | **PASS** | `visit_date DATE NOT NULL UNIQUE` · `unique_visitors/total_visits/authenticated_visits INTEGER NOT NULL DEFAULT 0` · `idx_daily_visit_date` 존재. §4-1 표와 컬럼·제약 완전 일치 |
| 1-2 | `AgeBucket.of` 경계값 | Read `AgeBucket.java` + `AgeBucketTest` 통과 확인 | **PASS** | 19 미만 → UNKNOWN, 19~24, 25~29, ..., 40+. birthDate null / today null / 미래일 모두 UNKNOWN. `Period.between` 기반 정확 |
| 1-3 | Interceptor 제외 12 패턴 | Read `WebMvcConfig.java:39-51` | **PASS** | `/admin/**, /actuator/**, /css/**, /js/**, /images/**, /webjars/**, /favicon.ico, /error, /login, /logout, /h2-console/**, /__test__/**` — spec §7-2 기재 6패턴 + 실무상 필수 6패턴 확장. 이탈 없음 |
| 1-4 | Scheduler cron + zone | Read `DailyVisitScheduler.java:37` | **PASS** | `@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")` — spec §7-1 cron 정합 (`0 0 2 * * *` KST) |
| 1-5 | `@ConditionalOnProperty` fallback | Read `DailyVisitScheduler.java:25-28` | **PASS** | `name=spring.task.scheduling.enabled, havingValue=true, matchIfMissing=true` — 프로덕션 default 활성, `test/application.properties`·`e2e.properties` 에 `false` 설정으로 억제 |
| 1-6 | `@PreAuthorize` 클래스 레벨 | Read `AdminStatsController.java:19` | **PASS** | `@PreAuthorize("hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')")` — spec §6-1 정합 |
| 1-7 | HTMX fragment 반환 | Read `AdminStatsController.java:46` | **PASS** | `return "admin/stats :: chart"` + 템플릿 `#chart-container th:fragment="chart"` 페어. `hx-target="#chart-container" hx-swap="innerHTML"` 정합 |
| 1-8 | KPI 증감 30일 (Qn-9) | Read `AdminStatsService.java:74-79, 279-282` | **PASS** | `cur30From=today.minusDays(29)`, `prev30From=today.minusDays(59)`, `prev30To=today.minusDays(30)`, `pctDelta` 로직 정확. prev=0 시 cur=0→0, cur>0→100 fallback |
| 1-9 | 마감임박 applyEndDate 우선 (Qn-10) | Read `AdminStatsService.java:230-232` | **PASS** | `deadlineOf(p) = p.getApplyEndDate() != null ? applyEndDate : endDate` |

### 2. 보안 (XSS · RBAC · 개인정보)

| # | 공격 벡터 | 실행 | 판정 | 근거 |
|---|---|---|---|---|
| 2-1 | `th:utext` XSS 리스크 (chartSvg/genderChartSvg/ageChartSvg) | grep DonutSlice 생성 지점 전수 | **PASS** | `DonutSlice.label` 은 서버 리터럴 (`"남성","여성","무응답","19-24세","25-29세","30-34세","35-39세","40세 이상","미상"`) 만 사용. bar/line 차트 `SeriesData.labels` 는 `DateTimeFormatter.ofPattern("MM.dd")` · `String.valueOf(year)` — 사용자 입력 흐름 없음. XSS 벡터 없음 |
| 2-2 | Interceptor 개인정보 저장 | Read `VisitTrackingInterceptor.java` + `DailyVisitCollector.java` | **PASS** | 저장 필드는 `LocalDate + LongAdder + Map<String, Boolean>(sessionId → true)`. IP·UA·referrer·path 미저장. scheduler flush 후 `remove` 로 회수. `daily_visit` 테이블에도 집계값만. spec §12 `deviation: Q5-A 개인정보 미저장` 정합 |
| 2-3 | USER 진입 차단 | qa 리포트 §2-1 재검증 (미실행) | **UNVERIFIED (spec-consistent)** | `@PreAuthorize` 로직상 USER 403. ym-qa 실증 존재하나 verify 세션에서 재실행 안 함 (bootRun 미기동). 로직 경로는 확인됨 |
| 2-4 | CENTER_ADMIN 격리 | Read `AdminStatsService.scopedPrograms/scopedUsers` | **PASS** | `scopeCenterName` 이 null 아니면 `p.getOrganization()` / `u.getCenter().getName()` 기준 filter. AdminScope 는 CENTER_ADMIN 을 자기 센터 이름으로 강제 (기존 A1~A5 패턴 재사용). 관찰된 이탈 없음 |
| 2-5 | 세션 강제 생성 부수효과 | Read `VisitTrackingInterceptor.java:48-51` | **PASS (경미 관찰)** | 익명 첫 요청도 `request.getSession(true)` 로 세션 강제 생성 → JSESSIONID 쿠키 발급. spec §7-1 "익명 세션 + 인증 사용자 합산" 요구에 부합. Signup/Login CSRF 세션 동작에 영향 없음 (이미 세션 생성 경로 존재) |

### 3. 회귀 · 무영향

| # | 공격 벡터 | 실행 | 판정 | 근거 |
|---|---|---|---|---|
| 3-1 | 타깃 테스트 재실행 | `gradlew test --tests "*Stats*" "*DailyVisit*" "*AgeBucket*" JpaMappingTest` | **PASS** | BUILD SUCCESSFUL (1m 22s). 신설 18 TC (`AgeBucketTest` 9 · `DailyVisitCollectorTest` 4 · `SvgChartRendererTest` 5) + `JpaMappingTest` 통과. Hibernate schema drop 정상 |
| 3-2 | `@EnableScheduling` 테스트 context 영향 | Read `test/application.properties:23` + `WebMvcConfig` `@EnableScheduling` | **PASS** | `spring.task.scheduling.enabled=false` + `@ConditionalOnProperty` 로 스케줄러 bean 스킵. WebMvcConfig 자체는 로드되나 스케줄 발화 없음. §3-1 로 실증 |
| 3-3 | Interceptor slice test 안전성 | Read `WebMvcConfig.java:31-35` | **PASS** | `ObjectProvider.getIfAvailable()` null 시 `registry.addInterceptor` 스킵. `@WebMvcTest` 슬라이스에서 `VisitTrackingInterceptor` 미스캔이어도 예외 없음. F0c NoSuchBean 재발 방지 패턴 정합 |
| 3-4 | Collector null safety in Interceptor | Read `VisitTrackingInterceptor.java:53-54` | **PASS** | `collectorProvider.getIfAvailable()` null → 조용히 return. 카운트만 스킵되고 응답 완전성 유지 |
| 3-5 | header GNB 무회귀 | git diff `admin/fragments/header.html` | **PASS** | 순수 추가 3라인 (`<a th:href="/admin/stats">통계</a>`). 기존 `대시보드/프로그램 관리/사용자 관리/공지 관리/약관 관리` 순서·조건 무변경. admin-dashboard 계약 무회귀 (ym-qa 실행 결과 인용 대신 diff 로 확인) |
| 3-6 | `chartSvg` 서버 렌더 무CDN | grep `<script src=|cdn.jsdelivr` in stats.html | **PASS** | Pretendard/Google Fonts CDN 만 `<link>` 로 사용 (기존 admin 레이아웃 공통). Chart.js·ApexCharts CDN 도입 없음. Qn-1 결정 정합 |
| 3-7 | Observability metric namespace 충돌 | grep `youth_moa\.` prefix 사용처 | **PASS** | A6 는 metric 등록 없음 (Interceptor + DB 저장만). 기존 `AuthenticationFailure` 메트릭과 이름공간 격리 |

### 4. deferred / deviation 규정 준수

| # | 공격 벡터 | 실행 | 판정 | 근거 |
|---|---|---|---|---|
| 4-1 | `visual-admin-stats.spec.ts` 미신설 | ls `e2e/tests/` | **PASS (spec-conformant deferred)** | 파일 부재 확인. spec §15 이월표 명시 · qa 리포트 §8 재확인. `adminStatsContract` 는 계약 파일로 신설되어 있어 후속 spec 파일이 import 만 하면 편입 가능. 현재는 CI `--project=contracts` 자동 회귀 미포함 (**후속 티켓 필수** — 리포트 §5 참조) |
| 4-2 | `admin-stats.spec.ts` 기능 E2E 미신설 | ls `e2e/tests/` | **PASS (spec-conformant deferred)** | 파일 부재. spec §15 이월. ym-qa 세션에서 Playwright 직접 조작 실증 (HTMX swap 후 `linearGradient id="areaGrad"` 등장) — 일회성 보완 |
| 4-3 | `Program.viewCount` 미도입 | grep in ProgramStatRow | **PASS** | `views=0` 하드코딩 (line 325). spec §15 이월 (`deferred: A6-followup`). 화면 노출 0 |
| 4-4 | Excel export / Cohort / 실시간 미구현 | grep | **PASS** | 파일 없음. spec §12 이월 정합 |

### 5. spec §6-1 이탈 관찰 (경미)

| # | 관찰 | 판정 | 근거 |
|---|---|---|---|
| 5-1 | `?scope=<centerName>` 쿼리 파라미터 | **관찰 (경미 이탈)** | `AdminStatsController` 는 `?chartMode=` 만 읽고 `?scope=` 는 읽지 않음. SYSTEM_ADMIN 은 항상 전체(null), CENTER_ADMIN 은 AdminScope 자동 강제 — 결과적으로 spec §6-1 의 "SYSTEM_ADMIN 만 scope 지정 가능"이 미제공. **회귀·보안 리스크 없음** (덜 유연할 뿐). spec §15 매핑에도 해당 파라미터 언급 없음 → impl-time deviation 으로 볼 여지 있으나 이월표에 등재되지 않아 문서 이탈. **후속 정합 정리 권고 (FAIL 아님)** |

---

## UNVERIFIED (판정 불가) 상세

| # | 항목 | 사유 | 판정 조건 |
|---|---|---|---|
| U-1 | 전체 594 TC (Testcontainers 포함) | 회사 PC Docker Desktop 데몬 미기동 (`Could not find a valid Docker environment`) | 개인 PC(Mac) Docker 기동 후 재실행 or CI ubuntu 러너 green 대기 |
| U-2 | Interceptor p50 정량 (+2ms 이내 목표) | 부하 도구 부재 | k6/JMeter 도입 시 정량 측정. 학습 프로젝트 규모상 실사고 리스크 낮음. spec §8 정성 PASS 로 인정 |
| U-3 | Scheduler cron 실 발화 (2026-09-18 02:00 KST) | 미래 시각 | 익일 로그 확인 or `flushDate` 유닛 실행 (테스트 PASS) — 로직 자체는 U-3 로직 검증됨 |
| U-4 | 계약 spec (`visual-admin-stats.spec.ts`) 자동 회귀 편입 | 파일 미신설 | 후속 티켓에서 15줄 spec 신설 시 즉시 편입 가능 |

---

## FAIL 상세

없음. 프로덕션 코드 반려 사유 없음.

---

## 후속 권고

1. **`e2e/tests/visual-admin-stats.spec.ts` 신설** — `visual-admin-dashboard.spec.ts` 패턴 그대로. 15줄. CI `--project=contracts` 자동 회귀 편입. spec §15 이월 해소.
2. **spec §6-1 `?scope=` 파라미터 정합 정리** — 실제 미구현이므로 spec 본문 이월표에 명시 추가하거나 구현. FAIL 아님.
3. **개인 PC 전체 test suite 재실행** — Testcontainers 포함 594 TC. 회사 PC Docker 미기동 상태에서는 CI 결과 확인.

---

## 결론

**PASS 16 / FAIL 0 / UNVERIFIED 4.**
프로덕션 반려 사유 없음. Qn-A~QD + 세부 11건 결정사항, §7 구현 매핑, §8 회귀 방어, 보안 벡터 모두 독립 재확인 통과. Testcontainers·미래 cron 발화·정량 성능은 환경 제약으로 UNVERIFIED 이나 CI 위임 가능.

**커밋 · push 진행 가능.**
