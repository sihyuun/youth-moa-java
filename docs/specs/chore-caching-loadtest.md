# 작업 명세: chore/caching-loadtest — 캐싱 + 가상 스레드 + k6 부하 테스트 (수치 기반 성능 개선 스토리)

> 산출: ym-spec, 2026-07-20. 상태: **spec_done** (사용자 결정 Q1~Q9 대기)
>
> 포트폴리오 핵심 목적: **"측정 → 개선 → 재측정"** 의 수치 기반 성능 개선 스토리.
> 4조합 비교 매트릭스 (캐시 off/on × 가상스레드 off/on) 로 각 기법의 기여도를 분리 입증한다.

## 0. 디자인 자산 (해당 없음 선언)

- 본 티켓은 **chore (인프라·성능) 티켓**으로 화면 변경이 없다. prototype.html / prototype.tsx / wireframe.png / HANDOFF.md 대조 대상 없음.
- 단, 캐시 stale 이 화면에 미치는 영향 분석은 §3-C 에서 **CapacityBar (D5 spec, prototype L945~951)** 를 기준으로 수행했다.
- 데이터 모델 gap 표: **스키마 변경 없음** — 엔티티·컬럼 추가/삭제 0건. (Redis 는 외부 인프라, JPA 무관)

---

## 1. 개념 설명 + 비교표 (학습 규칙 — CLAUDE.md "개념 설명 우선")

### 1-A. Spring Cache 추상화란

Spring 의 `@Cacheable` / `@CacheEvict` / `@CachePut` 은 **AOP 프록시 기반의 캐시 추상화 레이어**다. 메서드 호출을 가로채 "캐시에 있으면 메서드 실행 생략 후 캐시 값 반환, 없으면 실행 후 저장" 을 수행한다.

- 저장소 구현(Caffeine / Redis / EhCache …) 은 `CacheManager` 빈 교체만으로 바뀐다 — **서비스 코드는 어노테이션 그대로**. 이것이 1단계(Caffeine) → 2단계(Redis) 전환으로 실증하려는 "추상화의 가치".
- JPA 2차 캐시 (`@Cache` / Hibernate L2) 와 다른 층위다: L2 는 영속성 컨텍스트 아래에서 엔티티 단위로 동작, Spring Cache 는 **서비스 메서드 결과(주로 DTO)** 단위. 이 프로젝트는 화면 조합 결과를 통째로 캐시하는 것이 목적이므로 Spring Cache 채택.
- **주의 (AOP 공통 함정)**: 같은 빈 내부의 self-invocation (`this.findTopPrograms()`) 은 프록시를 우회해 캐시가 동작하지 않는다. 현재 `HomeService.findTopProgramCards()` → `findTopPrograms()` 내부 호출 (HomeService.java:84~87) 이 정확히 이 패턴 → 캐시 어노테이션은 **외부에서 호출되는 메서드**에만 부착 (§3-B 대상표 참고).

### 1-B. Caffeine (로컬) vs Redis (분산) 비교

| 항목 | Caffeine | Redis |
|---|---|---|
| 위치 | JVM 힙 내부 (in-process) | 별도 프로세스/서버 (out-of-process) |
| 조회 지연 | ~수십 ns (메서드 호출 수준) | ~0.1–1 ms (네트워크 왕복) |
| 직렬화 | 불필요 (객체 참조 그대로) | 필요 (JSON/JDK 직렬화) — **DTO 만 캐시하는 규칙의 근거** |
| 다중 인스턴스 일관성 | ❌ 인스턴스별 독립 (scale-out 시 불일치) | ✅ 공유 저장소 — 전 인스턴스 동일 뷰 |
| 재기동 시 | 소멸 (cold start) | 유지 |
| 캐시 외 용도 | 없음 | rate limit·세션·분산 락·pub/sub (SmsRateLimiter 이전 근거) |
| 운영 비용 | 0 (의존성 1개) | 로컬 docker / Fly.io Upstash 프로비저닝 |
| 도입 난도 | `caffeine` 의존성 + spec 문자열 | Spring Data Redis + 직렬화 설정 + 인프라 |

**이 프로젝트 규모(단일 Fly.io 인스턴스, 트래픽 소규모)의 권장 단계**:

1. **1단계 Caffeine** — 단일 인스턴스에서는 Caffeine 이 기술적으로 충분 + 오버헤드 최소. 여기서 전 캐시 포인트·evict 지점·테스트를 완성한다.
2. **2단계 Redis 전환** — 기술 필요라기보다 **포트폴리오 학습 목적** (분산 캐시 + 추상화 실증): `CacheManager` 빈과 yml 설정만 교체하고 서비스 코드 diff 가 0 임을 PR 로 보여준다. scale-out (Fly.io machine 2대) 시나리오의 정합성 논거 포함.
3. SmsRateLimiter Redis 이전 (Q6) — 캐시가 아닌 **원자적 카운터** 용도. 재기동 시 리셋되는 현 인메모리 한계 (SmsRateLimiter.java:13 주석에 이미 명시) 해소.

### 1-C. 플랫폼 스레드 vs 가상 스레드 (Java 21)

| 항목 | 플랫폼 스레드 | 가상 스레드 |
|---|---|---|
| 실체 | OS 스레드 1:1 | JVM 스케줄링, carrier 스레드에 M:N 마운트 |
| 생성 비용 | ~1MB 스택, ms 단위 | ~수 KB, µs 단위 — 수십만 개 가능 |
| blocking I/O 시 | OS 스레드 점유 (풀 고갈 위험) | unmount → carrier 반납 (I/O 대기 무비용) |
| 적합 워크로드 | CPU-bound | **I/O-bound (DB 조회·외부 API — 이 앱의 전형)** |

- `spring.threads.virtual.enabled=true` 한 줄이면 **Tomcat 커넥터가 요청당 가상 스레드**를 사용한다 (기존: `server.tomcat.threads.max` 기본 200개 플랫폼 스레드 풀). `@Async`·스케줄러 executor 도 함께 가상 스레드화.
- **효과의 한계 명시 (정직한 스토리)**: 가상 스레드는 "동시 대기 요청 수용량" 을 키우는 것이지 개별 요청을 빠르게 하지 않는다. 진짜 상한은 **HikariCP 커넥션 풀 (기본 max 10)** — 가상 스레드 1만 개가 떠도 DB 커넥션 10개가 병목. 부하 테스트에서 "VT on 인데 p95 개선 없음 → 풀이 병목" 을 **수치로 확인하는 것 자체가 학습 포인트**. 캐시(=DB 접근 자체를 제거)와의 조합에서 시너지가 드러나는 구조.

#### synchronized pinning 점검 (Java 21 필수)

Java 21 에서 가상 스레드가 `synchronized` 블록 안에서 blocking 하면 carrier 스레드에 **pinning** (unmount 불가) — VT 이점 상실. (JDK 24 / JEP 491 에서 해소됐으나 본 프로젝트는 21.)

| 점검 항목 | 결과 | 조치 |
|---|---|---|
| 우리 코드 `synchronized` 전수 grep | **1건 — `SmsRateLimiter.tryAcquire` (SmsRateLimiter.java:29)** | `ReentrantLock` 으로 교체 (블록 내 I/O 없어 실해는 미미하나, pinning-free 코드베이스 선언 + 학습 목적) |
| PostgreSQL JDBC (pgjdbc) | 42.6.0+ 에서 synchronized → `ReentrantLock` 전환 완료 | Boot 4.1 BOM 버전 확인만 (`./gradlew dependencies --configuration runtimeClasspath | grep postgresql`) |
| HikariCP | ReentrantLock 기반 — VT 안전 | 조치 없음 |
| Caffeine | lock-free/ReentrantLock — VT 안전 | 조치 없음 |
| 런타임 검증 | — | 부하 테스트 시 JVM 옵션 `-Djdk.tracePinnedThreads=full` 부착 → pinning 스택 출력 0건 확인을 검증 항목에 포함 |

#### SSE 티켓 (feature/F2c-sse-notifications) 시너지

SSE 는 연결을 **장시간 유지**하는 패턴 — 플랫폼 스레드 모델에서는 동시 SSE 사용자 수가 스레드 풀 크기에 직결된다 (`SseEmitter` 가 async servlet 으로 워커를 즉시 반납하긴 하나 heartbeat·전송 시점마다 스레드 소비). 가상 스레드 활성화 시 SSE 동시 연결 비용이 사실상 0 에 수렴 → **본 티켓의 VT 활성화가 SSE 티켓의 수용량 전제 조건**이 된다. 부하 테스트 시나리오에 SSE 는 포함하지 않되 (F2c 미구현), 리포트 결론에 "SSE 도입 시 기대 효과" 로 연결해 스토리를 잇는다.

---

## 2. 변경 범위 (파일 단위)

### PR-1 `chore/cache-caffeine` — Spring Cache + Caffeine
- [ ] `build.gradle.kts` — `spring-boot-starter-cache`, `com.github.ben-manes.caffeine:caffeine`, (Q5 채택 시) `spring-boot-starter-actuator`
- [ ] `src/main/java/.../common/config/CacheConfig.java` **신규** — `@EnableCaching` + `CaffeineCacheManager` (캐시명 별 TTL 상이 → `registerCustomCache` 또는 캐시명-스펙 맵)
- [ ] `common/CacheNames.java` **신규** — 캐시명 상수 (문자열 하드코딩 방지)
- [ ] `web/HomeService.java` — `@Cacheable` 부착 (§3-B 표)
- [ ] `program/ProgramService.java` — 필터 소스 4메서드 `@Cacheable`
- [ ] `center/CenterService.java` — `distinctActiveRegions()` `@Cacheable`
- [ ] `application/ApplicationService.java` — `apply/approve/reject/cancel` 에 `@CacheEvict` (§3-D)
- [ ] `application.yml` — actuator 노출 설정 (Q5), 캐시 로그 레벨
- [ ] `src/test/java/.../CacheBehaviorTest.java` **신규** — 히트/미스/evict 단위 검증
- [ ] `application-e2e.properties` — **`spring.cache.type=none`** (E2E 는 캐시 없는 순수 동작 회귀 유지 — 기존 Playwright spec 이 stale 로 흔들리는 것 차단)

### PR-2 `chore/virtual-threads-loadtest` — 가상 스레드 + k6 + 측정
- [ ] `application.yml` — `spring.threads.virtual.enabled: ${VIRTUAL_THREADS:false}` (env 토글 — 매트릭스 측정용)
- [ ] `user/SmsRateLimiter.java` — `synchronized` → `ReentrantLock` (pinning 청산)
- [ ] `src/main/resources/application-perf.properties` **신규** — 측정 전용 프로파일 (e2e 복제 + 로그 억제 + 시드 볼륨 확대 Q4)
- [ ] `perf/k6/browse-mix.js`, `perf/k6/logged-in.js`, `perf/k6/common.js` **신규**
- [ ] `perf/run-matrix.ps1` (+ `.sh`) **신규** — 4조합 × 시나리오 자동 실행, k6 `--summary-export` JSON 수집
- [ ] `docs/perf/TEMPLATE.md`, `docs/perf/2026-07-caching-vt-report.md` **신규** — §6 템플릿
- [ ] `common/DataInitializer.java` — perf 프로파일 조건부 벌크 시드 (Q4 채택 시)

### PR-3 `chore/cache-redis` — Redis 전환 (추상화 실증)
- [ ] `build.gradle.kts` — `spring-boot-starter-data-redis`
- [ ] `common/config/CacheConfig.java` — 프로파일/프로퍼티 분기: `youthmoa.cache.provider=caffeine|redis` → `RedisCacheManager` (TTL per-cache `RedisCacheConfiguration`, `GenericJackson2JsonRedisSerializer`)
- [ ] `docker-compose.yml` **신규** (repo 루트) — redis:7-alpine 로컬 개발용
- [ ] `application.yml` — `spring.data.redis.url=${REDIS_URL:redis://localhost:6379}`
- [ ] (Q6 채택 시) `user/SmsRateLimiter.java` — Redis 기반 구현 추가 (인터페이스 추출 → `InMemorySmsRateLimiter` / `RedisSmsRateLimiter` 프로파일 분기, `SmsSender`/`MockSmsSender` 와 동일 패턴)
- [ ] `docs/perf/` — Redis 조합 재측정 부록 (Caffeine 대비 지연 증가를 **정직하게 수치로** — "왜 그래도 Redis 인가" 논거가 포트폴리오 차별점)
- [ ] **서비스 코드 (`HomeService` 등) diff 0 임을 PR 본문에 명시** — 추상화 가치 실증의 증거

---

## 3. 캐시 설계

### 3-A. 대상 선정 기준

| 기준 | 설명 |
|---|---|
| ① 조회 빈도 | 홈(`/`)·목록은 전 방문자 공통 경로 — 최다 호출 |
| ② 계산 비용 | count 집계·GROUP BY·다중 repository 조합 쿼리 |
| ③ 변경 빈도 | admin 편집·신청 이벤트 외 변경 없음 = 캐시 적합 |
| ④ 개인화 여부 | **사용자별 결과는 1단계 제외** (키 폭발 + 히트율 저하) |
| ⑤ stale 허용도 | 화면 표시용 수치는 짧은 TTL 허용, **신청 가능 여부 판정은 캐시 금지** (§3-C) |

**캐시 값 규칙 (필수)**: 캐시에는 **엔티티 금지, DTO·불변 값만** 저장한다.
- 근거 ①: detached 엔티티의 lazy 필드 접근 → `LazyInitializationException` (CLAUDE.md 기존 사고 유형과 동일 계열).
- 근거 ②: PR-3 Redis 전환 시 JSON 직렬화 필요 — Hibernate 프록시·양방향 참조가 섞인 엔티티는 직렬화 실패. DTO 로 시작해야 2단계 전환이 무 diff 로 된다.
- 현재 `findMainNotice()/findSubNotices()/findSpaceImages()` 는 엔티티(Notice/SiteImage)를 반환 → 캐시 부착 전 **경량 record DTO 로 변환하는 리팩터 포함** (아래 표 "조치").

### 3-B. 대상별 설계 표

캐시명 규칙: `도메인:용도`. TTL 은 Q2 결정으로 확정.

| 캐시명 | 부착 지점 | 키 | 값 (DTO) | TTL 권장 | 무효화 |
|---|---|---|---|---|---|
| `home:stats` | `HomeService.countActivePrograms/countCenters/countTotalApplicants` → **`getQuickStats()` 1메서드로 합침** (self-invocation 회피 + 캐시 1건) | 고정 `'stats'` | `QuickStatsDto(long,long,long)` record 신규 | 10분 | TTL + `@CacheEvict`(신청 생성/취소 — applicants 반영) |
| `home:hero` | `HomeService.getHeroImageUrls()` | 고정 | `List<String>` (이미 값 타입) | 1시간 | TTL (admin SiteImage CRUD 시 evict — admin 트랙 후속) |
| `home:spaceImages` | `HomeService.findSpaceImages()` | 고정 | `SiteImageDto(slot,imageUrl,sortOrder)` record 신규 | 1시간 | 동상 |
| `home:notices` | `findMainNotice()` + `findSubNotices()` → `getHomeNotices()` 합침 | 고정 | `HomeNoticesDto(main, subs)` — `NoticeSummaryDto` record | 10분 | TTL (admin Notice CRUD evict — 후속) |
| `home:topPrograms` | `HomeService.findTopProgramCards()` | 고정 | `List<ProgramCardDto>` (기존 DTO — 엔티티 필드 참조 여부 ym-impl 이 확인, 필요 시 값 복사형으로 보강) | **60초** (§3-C) | TTL + `@CacheEvict`(apply/approve/reject/cancel) |
| `program:filterSources` | `ProgramService.getSidebarRegions/getAllRegions/getSidebarCenters/getAllCenters` | 메서드별 4키 | `RegionDto`/`CenterOptionDto` record (엔티티 반환 중 → DTO 화) | 1시간 | TTL (admin 트랙 후속) |
| `center:regions` | `CenterService.distinctActiveRegions()` | 고정 | `List<String>` | 1시간 | TTL |

**1단계 제외 (사유 명시)**:

| 후보 | 제외 사유 |
|---|---|
| `HomeService.findRecommendedProgramCards(userId)` | ④ 개인화 — 사용자 수만큼 키 증가, 히트율 낮음. VT+커넥션 재사용으로 충분 |
| `ProgramService.search(...)` 목록 페이지 | 키 = status×regions×centers×sort×page 조합 폭발. 필터 소스 캐시(`program:filterSources`)만으로 목록 요청당 쿼리 4→1 회 절감 효과가 이미 큼. 2차 후보로 리포트에 기록 |
| `ProgramController.detail` 의 appliedCount/CapacityBar | ⑤ 신청 직전 화면 — stale 시 UX 혼란 최대 지점 (§3-C). 캐시 금지 |
| `CenterService.list(...)` | `now`/`isHoliday` 파라미터가 분 단위로 변함 (운영중 배지) — 키에 시각이 섞여 히트 불가. `countActiveGroupByOrganization()` repository 레벨 캐시는 2차 후보 |
| `SearchService` | 검색어 = 무한 키. 제외 |

### 3-C. stale 데이터 UX 영향 분석 (필수 항목)

**핵심 안전선**: 신청 가능 여부의 **판정**은 `ApplicationService.apply()` 가 트랜잭션 안에서 실시간 조회로 수행 (ApplicationService.java:56~74) — 캐시 대상이 아니므로 **stale 로 인한 초과 신청·오승인은 구조적으로 불가능**. stale 영향은 전부 "표시" 층위다.

| 화면 | stale 시나리오 | 영향 | 판정 |
|---|---|---|---|
| 홈 CapacityBar (`home:topPrograms`, TTL 60s) | 신청 마감 직전 프로그램이 60초간 "9/10 · 마감임박(90%)" 대신 "8/10(80%)" 표시 — **colorClass 경계 (D5: 90% 경계에서 warning→error 전환) 가 한 단계 뒤처짐** | 사용자가 상세로 이동하면 실시간 값 (detail 은 캐시 금지) → 신청 시점 정합 보장. 홈 카드가 잠시 낙관적으로 보이는 수준 | **허용** — 단 TTL 60s 상한 + 신청 이벤트 evict 병행으로 창을 더 줄임 (Q2) |
| 홈 Quick Stats "누적 참여자" | 신청 직후 본인 수치 미반영 (최대 10분) | 집계 지표 — 개인 행동 피드백 아님 | 허용 |
| 홈 공지 | admin 공지 등록 후 최대 10분 미노출 | admin 트랙 착수 시 evict 추가 예정. 현재 admin 화면 자체가 없어 실위험 0 | 허용 |
| 프로그램 목록 필터 사이드바 | admin 이 센터/지역 추가 후 최대 1시간 미반영 | 동상 (admin 미착수) | 허용 |
| Hero/공간 이미지 | 동상 | 동상 | 허용 |
| 프로그램 상세 CapacityBar·경쟁률 | — | **캐시 미적용** (제외 표) — 신청 결정 직전 화면은 항상 실시간 | 원칙 |

**evict 누락 감지 규칙**: 신청 상태를 바꾸는 메서드가 추가되면 `home:stats`·`home:topPrograms` evict 를 함께 검토 — `CacheBehaviorTest` 에 "apply 후 topPrograms 캐시 비어 있음" assertion 을 두어 회귀 방어.

### 3-D. @CacheEvict 지점

```java
// ApplicationService — 신청 수 변동 4메서드에 공통 부착
@Caching(evict = {
  @CacheEvict(cacheNames = CacheNames.HOME_STATS, allEntries = true),
  @CacheEvict(cacheNames = CacheNames.HOME_TOP_PROGRAMS, allEntries = true)
})
```

| 트리거 | evict 대상 | 이유 |
|---|---|---|
| `apply()` (생성/재신청) | home:stats, home:topPrograms | applicants 수·CapacityBar 즉시 반영 |
| `cancel()` | 동상 | 역방향 |
| `approve()` / `reject()` | 동상 | ACTIVE_STATUSES 가 PENDING+APPROVED 라 approve 는 카운트 불변이지만, reject 는 감소 — 4메서드 일괄 부착이 단순·안전 (미세 최적화 불채택) |
| admin Program/Notice/SiteImage/Center CRUD | (후속) 해당 캐시 | **admin 트랙 미착수 — ADMIN-00 티켓들에 "캐시 evict 추가" 항목을 본 spec 이월 항목으로 등재** (§8) |

---

## 4. 가상 스레드 도입 설계

- 토글: `spring.threads.virtual.enabled: ${VIRTUAL_THREADS:false}` — **기본 off**. 매트릭스 측정으로 효과 입증 후 리포트 결론에서 default on 전환 여부 결정 (측정 없이 켜지 않는 것 자체가 스토리).
- 사전 조치: SmsRateLimiter `ReentrantLock` 전환 (§1-C 표). 그 외 synchronized 0건 (grep 실측 완료).
- 측정 시 JVM 옵션: `-Djdk.tracePinnedThreads=full` — 리포트에 "pinning 0건" 증빙 첨부.
- HikariCP: 기본 maximumPoolSize=10 유지로 1차 측정 → 병목 확인 시 `spring.datasource.hikari.maximum-pool-size` 상향 실험을 **부록 측정**으로 추가 (Supabase 무료 티어 커넥션 상한 주의 — 실 DB 측정 시 15 이하 유지). 이 실험이 "VT 는 풀 상한까지만 동시성을 올린다" 개념의 수치 증명이 된다.

---

## 5. 부하 테스트 설계

### 5-A. k6 vs Gatling

| 항목 | k6 (권장) | Gatling |
|---|---|---|
| 스크립트 | JavaScript (ES6) — 학습 곡선 낮음 | Java/Scala DSL — JVM 프로젝트 정합성은 높으나 무거움 |
| 실행 | 단일 바이너리 CLI (`winget install k6` / `brew install k6`) | JVM + Maven/Gradle 플러그인 |
| 부하 모델 | VU / constant-arrival-rate (open model) 모두 지원 | 동등 |
| 결과 | `thresholds` 로 pass/fail 게이트 + `--summary-export` JSON → 리포트 표 자동화 용이 | HTML 리포트 우수하나 수치 추출 번거로움 |
| CI | GitHub Actions 공식 action 존재 | 가능하나 셋업 큼 |
| 리소스 | Go 기반 — 측정 PC 부하 최소 (서버와 동일 PC 실행 시 중요) | JVM 2개 동시 구동 부담 |

**권장: k6** — 이 프로젝트는 "빠른 반복 측정 + JSON 수치 추출 + 리포트 자동화" 가 목적이라 k6 이 정확히 부합. Gatling 은 리포트에 비교표만 남긴다.

### 5-B. 시나리오 스크립트 구성

**`perf/k6/browse-mix.js`** — 비로그인 열람 혼합 (메인 시나리오):

| 요청 | 비중 | 비고 |
|---|---|---|
| `GET /` | 40% | 캐시 효과 최대 지점 (repository 7회 → 캐시 히트 시 0회) |
| `GET /programs` (필터/정렬/페이지 랜덤) | 25% | 필터 소스 캐시 효과 |
| `GET /programs/{id}` (시드 id 랜덤) | 20% | 캐시 미적용 경로 — 대조군 |
| `GET /centers` | 10% | 캐시 부분 적용 |
| `GET /notices` | 5% | — |

- 부하 모델: `constant-arrival-rate` (open model — 응답 지연이 부하량에 영향 안 주도록), 단계: warm-up 30s (10 rps) → 본 측정 3m (단계별 20/50/100 rps) → 한계 탐색 ramp (에러율 1% 도달 rps 기록).
- `thresholds`: `http_req_duration: ['p(95)<500']`, `http_req_failed: ['rate<0.01']` — 조합별 통과/실패 자체가 결과 데이터.

**`perf/k6/logged-in.js`** — 로그인 사용자 시나리오:

1. `GET /login` → CSRF 토큰 파싱 → `POST /login` (perf 시드 계정 N개 순환 — VU 별 분배)
2. `GET /` (맞춤추천 경로 — 비캐시·사용자별 쿼리) → `GET /programs/{id}` → `GET /mypage/history`
3. 쓰기 포함 여부는 **Q8**: 포함 시 `POST /programs/{id}/apply` + 즉시 `POST cancel` 페어로 데이터 중립 유지 (evict 스톰 → 캐시 히트율에 미치는 영향까지 측정 가능해 스토리가 풍부해짐). 기본안은 읽기 전용.

### 5-C. 측정 지표

| 지표 | 출처 | 용도 |
|---|---|---|
| p50 / p95 / p99 latency | k6 `http_req_duration` | 핵심 비교 축 |
| RPS (달성 처리량) | k6 `http_reqs` | 한계 탐색 |
| 에러율 | k6 `http_req_failed` | 안정성 |
| 캐시 히트율 | actuator `/actuator/metrics/cache.gets?tag=result:hit|miss` (Q5) | 캐시 효과 직접 증빙 |
| Hikari active/pending | actuator `/actuator/metrics/hikaricp.connections.*` | VT 병목 분석 (§4) |
| pinning 이벤트 | `-Djdk.tracePinnedThreads` 로그 | 0건 증빙 |

### 5-D. 비교 매트릭스 (필수 4조합)

| # | 캐시 | 가상스레드 | 토글 방법 |
|---|---|---|---|
| A (baseline) | off | off | `SPRING_CACHE_TYPE=none` + `VIRTUAL_THREADS=false` |
| B | **on** | off | 캐시 default + `VIRTUAL_THREADS=false` |
| C | off | **on** | `SPRING_CACHE_TYPE=none` + `VIRTUAL_THREADS=true` |
| D | **on** | **on** | 캐시 default + `VIRTUAL_THREADS=true` |

- 각 조합 × 시나리오 2종 = 8회 본 측정 + 조합별 재기동·warm-up. `perf/run-matrix.ps1` 이 환경변수 세팅 → bootRun 기동 → readiness 대기 → k6 실행 → summary JSON 을 `docs/perf/raw/` 에 저장까지 자동화.
- (PR-3 후) 부록: D 조합에서 Caffeine vs Redis 재측정 1회 추가.

### 5-E. 측정 환경 결정 (Q3)

| 옵션 | 장점 | 단점 | 판정 |
|---|---|---|---|
| a. e2e 프로파일 (H2 in-memory) | 셋업 0, 기존 launch.json 재활용 | DB 가 인메모리라 **쿼리 지연 ~0** → 캐시 효과가 과소, VT 효과 거의 안 보임. "성능 개선 스토리" 신뢰도 훼손 | 스크립트 개발·스모크용 |
| **b. 로컬 docker PostgreSQL (권장)** | 실 DB 엔진 + 재현 가능 + 외부 변수 없음. 회사 PC Docker 가용 실증됨 (CLAUDE.md) | docker-compose 에 postgres 서비스 추가 필요 (PR-3 redis 와 같은 파일에 두면 일석이조) | **본 측정** |
| c. 실 Supabase (원격) | 운영과 동일한 네트워크 지연 → 캐시 효과 극대화로 보임 | 공유 인프라·풀러 개입으로 **측정 재현성 낮음** + 무료 티어 커넥션/트래픽 제약 + 부하 테스트가 약관상 부적절할 수 있음 | 참고 1회 측정 (부록) 이하로만 |

권장: **b 본 측정 + a 스모크**. `application-perf.properties` 는 docker postgres 를 바라보고, 시드는 perf 전용 볼륨 (Q4).

---

## 6. 결과 문서화 — `docs/perf/` 리포트 템플릿

`docs/perf/TEMPLATE.md` (블로그/포트폴리오 전환 가능 형태):

```markdown
# 성능 측정 리포트: <제목>  (YYYY-MM-DD)

## 1. 요약 (3줄) — 개선 전 → 후 핵심 수치
## 2. 환경 — HW(CPU/RAM), Java/Boot 버전, DB(docker pg 16, pool=10), k6 버전, 앱 커밋 SHA, JVM 옵션
## 3. 시나리오 — 트래픽 믹스 표, 부하 모델(arrival rate), 측정 시간, warm-up 정책
## 4. 결과 매트릭스
| 조합 | p50 | p95 | p99 | 달성 RPS | 에러율 | 캐시 히트율 | Hikari pending 피크 |
|---|---|---|---|---|---|---|---|
| A baseline | | | | | | — | |
| B cache | | | | | | | |
| C VT | | | | | | — | |
| D cache+VT | | | | | | | |
## 5. 그래프 — p95 조합별 막대, rps ramp 대비 latency 곡선 (k6 summary JSON → 스크립트 생성 SVG/PNG)
## 6. 분석 — 조합별 차이의 "왜" (캐시: 쿼리 제거, VT: 대기 수용량, 풀 병목 증거)
## 7. 한계와 다음 단계 — 미캐시 경로, Redis 지연 트레이드오프, SSE 전망
## 8. 재현 방법 — perf/run-matrix.ps1 원커맨드
```

- raw 데이터 (`docs/perf/raw/*.json`) 커밋 — 수치 조작 불가능성·재현성이 포트폴리오 신뢰 포인트.
- 그래프는 k6 summary JSON 을 읽는 소형 스크립트로 생성 (도구는 ym-impl 재량 — 외부 서비스 의존 금지).

---

## 7. Redis 도입 옵션 비교 (PR-3)

### 7-A. 로컬 docker-compose vs Fly.io Upstash

| 항목 | 로컬 docker-compose (redis:7-alpine) | Fly.io Redis (Upstash 관리형) |
|---|---|---|
| 용도 | 개발·부하 측정 | 운영 (Fly.io 배포 앱과 private network 연결) |
| 비용 | 0 | 무료 티어 존재 (요청 수 제한), `fly redis create` 로 프로비저닝 |
| 지연 | <1ms (localhost) | 동일 리전 배치 시 ~1ms, eviction 정책 관리형 |
| 설정 | `REDIS_URL=redis://localhost:6379` | `fly redis create` 발급 URL 을 secret 주입 (`fly secrets set REDIS_URL=...`) |
| 판정 | **본 티켓 필수** | 티켓 범위 결정 필요 (Q7) — 측정·학습은 로컬로 완결 가능 |

### 7-B. SmsRateLimiter Redis 이전 (Q6)

- 현 구조: `@Component` 인메모리 + `synchronized` (SmsRateLimiter.java) — 재기동 리셋·다중 인스턴스 미공유. 코드 주석 스스로 "운영 확장 시 Redis 로 대체" 명시.
- 이전안: 슬라이딩 윈도 → Redis `INCR`+`EXPIRE` 2키 (분/일) 고정 윈도로 단순화 (rate limit 용도에 충분, 원자성 확보). `SmsSender/MockSmsSender` 와 동일한 인터페이스-프로파일 분기 패턴 → Redis 미기동 로컬에서도 인메모리 fallback 동작.
- 캐시(@Cacheable)와 **다른 Redis 사용 패러다임** (RedisTemplate 직접 조작) 을 한 티켓에서 함께 보여줄 수 있어 학습 가치 높음 — 단 PR-3 크기 증가. 포함 여부 Q6.

---

## 8. 의존성 / 선행 작업 / 이월

| 항목 | 관계 |
|---|---|
| **actuator (ADMIN-00 P0-4 `chore/actuator`)** | 캐시 히트율·Hikari 지표의 **동적 검증이 actuator 에 의존**. 선행 머지되면 PR-1 은 노출 설정만 추가. 미선행이면 PR-1 이 최소 도입 (`starter-actuator` + `health,caches,metrics` 노출) 하고 P0-4 는 잔여(Fly healthcheck 연결·admin 연동)만 수행 — **Q5** |
| SSE 티켓 (`feature/F2c-sse-notifications`) | 선행 불필요. 본 티켓 VT 활성화가 SSE 수용량의 전제 (§1-C) — 리포트 결론에 연결 |
| admin 트랙 (ADMIN-00) | admin CRUD 발생 시 evict 추가 필요 — **이월 항목으로 ADMIN-00 각 티켓에 "캐시 evict" 체크 등재 요청** (§3-D) |
| Flyway (P0-1) | 무관 (스키마 변경 없음) |
| 브랜치 충돌 | `application.yml`·`build.gradle.kts` 를 건드리는 타 chore 티켓과 머지 순서만 조율 |

---

## 9. 검증 시나리오 (ym-qa)

### 정적 검증
- `compileJava` + 전체 test 통과 (JaCoCo LINE 55% 유지 — CacheConfig 는 config 제외 패턴 해당)
- **`CacheBehaviorTest` (신규)** — `@SpringBootTest` (H2) 기반:
  - 히트: `getQuickStats()` 2회 호출 → repository count 쿼리 1회만 실행 (Mockito `@MockitoSpyBean` repository verify 또는 SQL 로그 카운트)
  - evict: `apply()` 후 `home:stats`/`home:topPrograms` 캐시 엔트리 부재 (`cacheManager.getCache(...).get(key) == null`)
  - TTL: Caffeine `Ticker` 주입형 테스트 or `expireAfterWrite` 설정값 assertion (시간 의존 sleep 금지)
  - e2e 프로파일 `cache.type=none` 에서 전 기능 무캐시 정상 동작 (기존 RenderTest 회귀)
- `SmsRateLimiterTest` — ReentrantLock 전환 후 기존 한도(1분 3회·1일 20회) 동작 불변 + 병렬 호출 정합 (기존 TC 재활용)
- PR-3: 직렬화 round-trip — 각 캐시 DTO 가 `GenericJackson2JsonRedisSerializer` 왕복 후 동등 (Testcontainers redis 또는 embedded 미사용 시 단위 직렬화 테스트로 대체)

### 동적 검증 (curl / preview — 캐시 히트 로그·actuator)
- bootRun (local 또는 perf 프로파일) 기동 후:
  - `GET /` 2회 → 2회째 `org.hibernate.SQL` 로그에 count/공지 쿼리 **부재** 확인 (히트 증거)
  - `curl /actuator/caches` — 등록 캐시 7종 노출 (Q5 채택 시)
  - `curl "/actuator/metrics/cache.gets?tag=cache:home:stats&tag=result:hit"` — count 증가 확인
  - 신청 1건 생성 (e2e 시드 계정) → 직후 `GET /` 에서 Quick Stats·topPrograms 쿼리 재실행 로그 (evict 증거)
  - `VIRTUAL_THREADS=true` 기동 → 요청 로그/`Thread.currentThread()` 디버그 엔드포인트 대신 **actuator metrics `jvm.threads.*` + 기동 로그의 virtual thread 표기** 확인, `-Djdk.tracePinnedThreads=full` 로 브라우징 중 pinning 출력 0건
- 페이지 응답·정적 리소스 200 회귀 (CLAUDE.md 표준 curl 세트) — 캐시가 렌더 결과를 바꾸지 않음 (`GET /` HTML 이 캐시 on/off 간 동일 구조)
- **부하 테스트 실행 자체가 최상위 동적 검증** — 매트릭스 8회 + 리포트 산출

### 시각 검증 (사용자 영역)
1. 홈 화면이 캐시 on 상태에서 기존과 동일 렌더 (Quick Stats·Hero·공지·카드 4장)
2. 신청 → 홈 복귀 시 참여자 수 즉시 +1 (evict 동작 체감)
3. `docs/perf/` 리포트의 표·그래프 가독성

---

## 10. 작업 큐 메타

- 작업 ID: `chore/caching-loadtest` (PR 브랜치: `chore/cache-caffeine` → `chore/virtual-threads-loadtest` → `chore/cache-redis`)
- 우선순위: 포트폴리오 강화 트랙
- 추정 단위: **3 PR** (§2) — PR-1·PR-2 는 순차 (측정이 캐시 토글 필요), PR-3 은 PR-1 뒤 언제든
- 상태: **spec_done**

---

## 11. 사용자 결정 필요 항목 (Q 리스트)

| # | 질문 | 선택지 (권장 굵게) |
|---|---|---|
| Q1 | 1단계 캐시 대상 범위 | **a. §3-B 7종 (홈+필터 소스, 개인화·상세 제외)** / b. ProgramService.search 목록 페이지까지 포함 (키 폭발 감수) |
| Q2 | `home:topPrograms` (CapacityBar 포함) stale 정책 | **a. TTL 60s + 신청 이벤트 evict 병행** / b. TTL only 5분 (단순, stale 창 큼) / c. evict only (TTL 무한 — admin 미착수 상태에선 위험) |
| Q3 | 부하 측정 환경 | **a. 로컬 docker PostgreSQL 본 측정 + e2e(H2) 스모크** / b. e2e(H2) 만 (셋업 0, 신뢰도 낮음) / c. 실 Supabase 포함 (재현성·약관 리스크) |
| Q4 | perf 시드 볼륨 | **a. perf 프로파일 한정 벌크 시드 (Program 200건·User 50건·Application 1,000건 규모)** / b. 기존 시드 8건 그대로 (쿼리 비용이 작아 개선 폭 왜소) |
| Q5 | actuator 도입 경로 | **a. PR-1 에서 최소 도입 (health·caches·metrics), ADMIN-00 P0-4 는 잔여만** / b. P0-4 선행 대기 (본 티켓 동적 검증 일부 보류) |
| Q6 | SmsRateLimiter Redis 이전 | **a. PR-3 에 포함 (인터페이스 분기 + INCR/EXPIRE — RedisTemplate 학습)** / b. 별도 후속 티켓 분리 |
| Q7 | Redis 운영(Fly.io Upstash) 연결 | a. PR-3 에서 `fly redis create` 까지 수행 / **b. PR-3 은 로컬 docker 로 완결, Fly 연결은 배포 트랙에서** (측정·학습 목적엔 로컬로 충분) |
| Q8 | 로그인 시나리오에 쓰기(apply/cancel 페어) 포함 | a. 포함 — evict 스톰까지 측정 (스토리 풍부, 스크립트 복잡) / **b. 1차는 읽기 전용, 쓰기 혼합은 리포트 "다음 단계"** |
| Q9 | PR 분할 3개 (§2) 승인 | **a. 승인** / b. PR-2·PR-3 병합 등 조정 |
