# 작업 명세: chore-observability — Actuator + Micrometer + Prometheus/Grafana 도입

> 산출: ym-spec, 2026-07-20. 상태: **PR-1 impl_done** (2026-07-24. Q1~Q7 전부 권장안 채택).
> **impl 중 발견**: 별도 `management.server.port` 지정 시 Boot 이 servlet child ApplicationContext 를 생성 → 통합 render 테스트에서 Thymeleaf 리졸버 미상속 → template not found. `src/test/resources/application.properties` 에 `management.server.port=-1` (management 서버 비활성화) 로 해소. Actuator 회귀 테스트는 별도 클래스에서 `@SpringBootTest(properties="management.server.port=0")` opt-in.
> 브랜치 후보: `chore/observability` (PR 분할: `chore/observability-actuator` → `chore/observability-metrics` → `docs/observability-dashboard`)

## 0. 개념 설명 (학습 규칙 — 역할 분담)

이번 티켓에서 4개 구성요소가 등장합니다. 각각 "무엇을 담당하는지"가 다르며, 하나로 뭉뚱그리면 설정이 꼬입니다.

| 구성요소 | 역할 | 비유 |
|---|---|---|
| **Spring Boot Actuator** | 앱 내부 상태를 **HTTP 엔드포인트로 노출**하는 프레임워크 기능. `/actuator/health`(살아있나), `/actuator/metrics`(수치), `/actuator/info`(빌드 정보) 등 | 자동차 계기판 커넥터 (OBD 포트) |
| **Micrometer** | 메트릭 **수집·기록 파사드(facade)**. SLF4J 가 로깅 구현체를 추상화하듯, Micrometer 는 Prometheus/Datadog 등 모니터링 백엔드를 추상화. 코드에서는 `MeterRegistry` 만 사용 | SLF4J 의 메트릭 버전 |
| **Prometheus** | 메트릭 **저장소 + 질의 엔진 (시계열 DB)**. **pull 모델** — 앱이 보내는 게 아니라 Prometheus 가 주기적으로 `/actuator/prometheus` 를 **긁어감(scrape)**. 앱은 "현재 값 스냅샷"만 텍스트로 노출 | 검침원이 주기적으로 계량기를 읽어감 |
| **Grafana** | Prometheus 에 저장된 시계열을 **시각화하는 대시보드**. 자체 저장 기능 없음 — datasource(Prometheus) 를 연결해 차트만 그림 | 계기판 화면 |

### pull 모델의 함의 (설계에 직접 영향)

- 앱은 **어디로 보낼지 모른다** — Prometheus 주소 설정이 앱에 없음. 반대로 Prometheus 설정(`prometheus.yml`)에 앱 주소가 있음.
- `/actuator/prometheus` 는 **인증 없는 GET 으로 전체 내부 지표를 반환** → 배포 환경에서 무방비 공개 시 JVM 버전·엔드포인트별 트래픽·에러율·DB 풀 상태가 전부 노출됨. **§2 접근 제어가 이 티켓의 핵심 보안 요건.**
- push 모델(예: StatsD, Grafana Cloud 의 remote_write)은 앱→저장소 방향이라 방화벽 뒤 앱에 유리하지만, 별도 agent 나 push 설정이 필요.

### Micrometer 네이밍 변환 규칙 (미리 알아둘 것)

Micrometer 에서 `youthmoa.application.submitted` 로 등록한 Counter 는 Prometheus 포맷에서 `youthmoa_application_submitted_total` 로 변환됩니다 (`.` → `_`, Counter 는 `_total` 접미사 자동 부착). 검증 시나리오의 grep 대상은 변환 후 이름입니다.

---

## 1. 디자인 출처

**해당 없음 (N/A)** — 인프라/운영 티켓으로 화면 산출물이 없어 prototype 3자산 (prototype.html / prototype.tsx / HANDOFF.md / wireframe.png) 정독 규칙 미적용. 대신 현재 상태의 근거 소스는 다음과 같음:

- `build.gradle.kts` L23~63 — **actuator 의존성 없음** 확인. L60~62 에 springdoc Boot 4 호환 대기 TODO 선례 (본 티켓도 동일한 Boot 4 좌표 검증 절차 필요)
- `src/main/resources/application.yml` — `management.*` 설정 전무. `server.port: 8080`
- `fly.toml` — `[http_service]` 만 존재. **`[checks]` / `[metrics]` 섹션 없음**. `auto_stop_machines = true`, `min_machines_running = 0` (§5 에 영향)
- `Dockerfile` L30 — `EXPOSE 8080` 단일 포트
- `.github/workflows/deploy.yml` — CI 성공 후 `flyctl deploy --remote-only`. 변경 불요
- `SecurityConfig.java` L57~111 — `anyRequest().authenticated()` 마감 → **`/actuator/**` 매처를 추가하지 않으면 모든 actuator 요청이 /login 302 redirect** (기존 static-path-pattern 사고와 동일 메커니즘)
- `web/HomeController` 계열의 `/api/ping` — SecurityConfig L94 에 permitAll 등록된 기존 수동 ping (Q6)

## 1-A. 자산 간 갭 (3자산 비교)

**N/A** — 디자인 자산 비교 대상 없음 (§1 참조).

## 1-B. 데이터 모델 gap 표

**N/A — 엔티티·스키마 변경 없음.** Actuator/Micrometer 는 인메모리 지표만 다루며 DB 테이블을 만들지 않음. (Flyway P0-1 과도 무관 — 마이그레이션 파일 불필요)

## 1-C. 데이터 소비 지점

**N/A** — 사용자 대면 화면에서 소비되는 데이터 없음. 지표 소비 지점은 Grafana 대시보드(§6)이며 repo 내 `docs/observability/` 로 재현 가능하게 커밋.

---

## 2. 노출 엔드포인트 정책 + 접근 제어

### 2-1. 엔드포인트별 노출 범위

Actuator 는 기본적으로 `/actuator/health` 만 웹 노출합니다. `management.endpoints.web.exposure.include` 로 명시적 opt-in 하는 구조 (기본 차단 = 안전한 기본값).

| 엔드포인트 | 노출 | 근거 |
|---|---|---|
| `health` | ✅ 노출 | Fly health check(§5) + 로컬 확인. `show-details: always` 는 **management 전용 포트에서만** (DB down 등 내부 상태 포함) — 공개 포트 노출 안이 채택되면 `when-authorized` 로 강등 |
| `prometheus` | ✅ 노출 (비공개 경로) | Prometheus scrape 대상. **공개 인터넷 노출 절대 금지** |
| `metrics` | ✅ 노출 (비공개 경로) | 단건 지표 디버깅용 (`/actuator/metrics/jvm.memory.used`). prometheus 와 동일 등급 |
| `info` | ⚪ Q7 | `springBoot { buildInfo() }` 로 버전·빌드시각 노출 — 포트폴리오 데모에 유용하나 커밋 SHA 공개 여부 결정 필요 |
| `env`, `beans`, `heapdump`, `threaddump`, `loggers` 등 | ❌ 미노출 | 시크릿·내부 구조 유출 위험. 학습 시 로컬에서 일시적으로 열어보는 것은 자유 (base yml 에는 넣지 않음) |

### 2-2. 접근 제어 방식 비교 (Q1)

| 방식 | 구현 | 장점 | 단점 | Fly.io 적합성 |
|---|---|---|---|---|
| **A. 별도 management 포트 (권장)** | `management.server.port: 9091`. fly.toml `[http_service]` 는 8080 만 공개하므로 9091 은 **외부에서 도달 자체가 불가능** | 보안 경계가 "네트워크 레벨"이라 설정 실수 여지 최소. Fly 내장 스크레이퍼는 호스트 내부에서 9091 직접 접근 가능 (§4-a). 업계 표준 패턴 | 포트 2개 관리. 로컬 검증 시 9091 기억 필요. `@SpringBootTest` 랜덤포트 테스트와 충돌 방지 조치 필요 (test 에서 `management.server.port=0`) | ★★★ — 공개 포트(`internal_port`)와 분리되어 무방비 노출이 구조적으로 불가능 |
| B. 동일 포트 + 경로 인증 | SecurityConfig 에 `/actuator/health` permitAll + `/actuator/**` hasRole("SYSTEM_ADMIN") | 파일 1개 수정으로 끝. 포트 단일 | scraper 가 form 로그인 불가 → **Fly 내장 스크레이퍼 사용 불가** (인증 헤더 설정 없음). HTTP Basic 별도 체인 추가 시 복잡도 급증. 매처 순서 실수 = 즉시 전체 공개 사고 | ★ — 이 방식을 쓰면 스크레이핑 경로가 막혀 (c) 로컬 전용이 됨 |
| C. IP 제한 | Fly proxy 뒤라 remote address 는 프록시 IP. `Fly-Client-IP` 헤더 기반 필터 자작 필요 | — | 헤더 신뢰 설정 실수 시 스푸핑 가능. Prometheus 호스팅 IP 가 유동적. 유지보수 최악 | ✗ 비권장 |

**권장: A.** 배포에서 `/actuator/**` 가 공개 URL 로 아예 라우팅되지 않는 구조가 "무방비 노출 금지" 요건을 가장 확실히 만족. SecurityConfig 에는 `.requestMatchers("/actuator/**").permitAll()` 을 추가하되(9091 의 별도 컨텍스트에도 security filter chain 이 적용되므로 필요), 공개 포트 8080 에서는 해당 경로가 404 라 permitAll 이어도 노출면이 없음 — 이 이중 근거를 impl 주석에 명시.

> ⚠️ ym-impl 검증 항목: Spring Boot 4.1 에서 `EndpointRequest.toAnyEndpoint()` 매처의 패키지 경로 (Boot 4 모듈 분리로 이동 가능성 — `@DataJpaTest` 이동 선례). 문자열 매처 `"/actuator/**"` 가 더 단순하면 그대로 채택 가능.

---

## 3. 호스팅 선택지 비교 (Q2)

| 선택지 | 비용 | 학습 가치 | 운영성 | 비고 |
|---|---|---|---|---|
| **(a) Fly.io 내장 metrics + Managed Grafana** | 무료 (Fly 계정 포함) | ★★ — Prometheus 설정 파일을 직접 안 만져서 pull 모델 체감 낮음 | ★★★ — fly.toml `[metrics]` 2줄이면 끝. Fly 가 호스트 내부에서 scrape → 관리형 Prometheus 저장 → `fly-metrics.net` Grafana 자동 제공 | `auto_stop_machines=true` 라 머신 정지 중엔 지표 공백 (절전과 트레이드오프 — 정상 동작임을 이해하는 것도 학습 포인트) |
| (b) Grafana Cloud Free tier | 무료 (시계열 1만 개·보존 14일 수준 제한) | ★★☆ — remote_write(push) 모델 추가 학습. 단 Grafana Cloud 는 외부 엔드포인트를 pull 하지 않으므로 **Grafana Alloy(agent) 사이드카**를 Fly 머신에 추가해야 함 → Dockerfile 복잡화 | ★★ — agent 프로세스 1개 추가 운영. 512MB 단일 VM 에 부담 | 대안: Fly 관리형 Prometheus 를 Grafana Cloud 의 **datasource 로 연결** (Fly token 인증) — agent 없이 (a) 저장 + (b) 시각화 결합 가능 |
| **(c) 로컬 docker-compose (학습용)** | 무료 | ★★★ — `prometheus.yml` scrape 설정·targets 페이지·PromQL 을 직접 다룸. **pull 모델을 몸으로 이해하는 유일한 경로** | 로컬 전용 (운영 아님) | prometheus + grafana 컨테이너 2개. 회사 PC Docker 사용 가능 실증됨 (2026-07-10) |

**권장 조합: (c) + (a) 병행.**
- **(c) 로컬 docker-compose** 로 개념 학습 + 대시보드 JSON 제작 → repo 커밋 (재현 가능)
- **(a) Fly 내장** 으로 배포 모니터링 — 앱 쪽 설정은 (c)(a) 완전 동일 (`/actuator/prometheus` 노출)이라 추가 코드 0
- (b) 는 Fly Prometheus→Grafana Cloud datasource 연결 방식으로 **후속 확장 여지**로만 문서화 (agent 사이드카는 512MB VM 에 과설계)

---

## 4. 커스텀 메트릭 (포트폴리오 차별화 포인트, Q3·Q4)

JVM/HTTP 기본 지표는 의존성만 넣으면 자동 수집됨. 차별화는 **도메인 메트릭** — "이 앱이 비즈니스적으로 뭘 하는지"를 지표로 보이는 것.

### 후보 (권장 2종 + 옵션 1종)

| 메트릭 (Micrometer 이름) | Prometheus 이름 | 타입 | 태그 | 계측 위치 |
|---|---|---|---|---|
| `youthmoa.application.submitted` | `youthmoa_application_submitted_total` | Counter | 없음 (프로그램 ID 태그는 **카디널리티 폭발 위험** — 태그 없이 시작, 필요 시 `category` 정도만) | `ApplicationService` 신청 성공 지점 |
| `youthmoa.login.failure` | `youthmoa_login_failure_total` | Counter | 없음 (username 태그 **절대 금지** — 카디널리티 + PII) | `AbstractAuthenticationFailureEvent` `@EventListener` 신규 컴포넌트 (Boot 이 `DefaultAuthenticationEventPublisher` 자동 구성 → SecurityConfig 의 failureHandler 수정 불필요) |
| (옵션) `youthmoa.sms.sent` | `youthmoa_sms_sent_total` | Counter | `mode` = `mock`/`real`, `result` = `success`/`fail` | `SmsSender` 구현체 (Mock/CoolSMS 양쪽) |

### 계측 방식 비교 (Q4)

| 방식 | 장점 | 단점 |
|---|---|---|
| **MeterRegistry 직접 주입 (권장)** | 동작이 코드에 그대로 보임 (학습 목적 부합). 추가 의존성 0. 태그·조건 분기 자유 | 서비스 코드에 계측 한 줄 추가 (관심사 혼입 — Counter 3개 수준에선 허용 범위) |
| `@Counted` / `@Timed` (AOP) | 선언적, 비침투 | `micrometer-core` 의 aspect + AspectJ 프록시 설정 필요. 프록시 경유 호출만 계측되는 함정 (self-invocation 미계측) — 디버깅 난이도가 학습 단계에 부적합 |

**권장: MeterRegistry 직접 주입.** `Counter.builder("youthmoa.application.submitted").register(registry)` 를 생성자에서 1회 등록 후 `.increment()`.

---

## 5. Fly.io health check 연동

현재 fly.toml 에 check 가 전혀 없음 → Fly 는 프로세스 생존만 봄. Actuator health (DB 커넥션 포함 판정) 를 check 로 연결.

Q1-A (별도 포트 9091) 채택 시 `[http_service]` 내부 check 는 8080 만 대상이라 사용 불가 → **top-level `[checks]` 섹션** 으로 포트 지정:

```toml
[metrics]
  port = 9091
  path = "/actuator/prometheus"   # Fly 관리형 Prometheus 가 호스트 내부에서 scrape

[checks]
  [checks.actuator_health]
    type = "http"
    port = 9091
    path = "/actuator/health"
    method = "GET"
    interval = "30s"
    timeout = "5s"
    grace_period = "60s"   # JVM 기동 + Hibernate 초기화 (512MB shared-cpu 감안 여유)
```

주의 사항 (spec 확정 근거):
- Fly 의 check·metrics scrape 는 **호스트 로컬 agent** 발신 — proxy 트래픽으로 안 잡혀 `auto_stop_machines` 절전을 깨우지 않음. 머신 정지 중 지표 공백은 정상.
- `grace_period` 부족 시 배포 직후 unhealthy 판정 → 배포 실패 루프. 60s 시작, 실측 후 조정.
- Dockerfile `EXPOSE 9091` 추가는 문서화 목적 (Fly 는 EXPOSE 미참조) — 명시 권장.
- health 기본 구성에 DataSource ping 포함 → Supabase 순단 시 check fail 로 재시작될 수 있음. 학습 단계에서는 기본값 유지, 운영 이슈 시 `management.health.db.enabled` 조정 여지 문서화.

---

## 6. 대시보드 구성안 (`docs/observability/`)

```
docs/observability/
├── README.md                      # 로컬 기동 절차 + Fly 대시보드 접근 경로 + 개념 요약
├── local/
│   ├── docker-compose.yml         # prometheus + grafana (grafana 는 provisioning 마운트)
│   ├── prometheus.yml             # scrape_configs: host.docker.internal:9091/actuator/prometheus (5s 간격)
│   └── grafana-provisioning/      # datasource + dashboard 자동 로드 (컨테이너 기동만으로 재현)
└── grafana/
    ├── jvm-dashboard.json         # 커뮤니티 "JVM (Micrometer)" (ID 4701) 기반 export
    └── youthmoa-dashboard.json    # 커스텀 대시보드 (아래 패널 구성)
```

`youthmoa-dashboard.json` 패널 구성:

| 행 | 패널 | 지표 (PromQL 소스) |
|---|---|---|
| JVM | Heap 사용량 / GC pause / 스레드 수 | `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live_threads` |
| HTTP | 요청 처리율(RPS) / p95 지연 / 5xx 비율 | `http_server_requests_seconds_count`, `..._seconds_bucket` (histogram — `management.metrics.distribution.percentiles-histogram.http.server.requests: true` 필요), `status=~"5.."` 필터 |
| DB | HikariCP active/idle/pending | `hikaricp_connections_active` 등 |
| 도메인 | 누적 신청 수 / 로그인 실패 추이 / (옵션) SMS 발송 | `youthmoa_application_submitted_total`, `youthmoa_login_failure_total` (increase/rate) |

Grafana provisioning 으로 **`docker compose up` 한 방에 datasource + 대시보드까지 뜨는 재현성**이 포트폴리오 포인트. Fly Managed Grafana 쪽에도 동일 JSON import 가능 (datasource UID 만 변수 처리).

---

## 7. 변경 범위 (파일 단위 — 전체 목록)

- [ ] `build.gradle.kts` — `implementation("org.springframework.boot:spring-boot-starter-actuator")` + `runtimeOnly("io.micrometer:micrometer-registry-prometheus")`. ⚠️ Boot 4.1 BOM 기준 좌표·starter 명 실검증 (springdoc TODO 선례)
- [ ] `src/main/resources/application.yml` — `management` 블록 신설 (exposure include, `server.port: ${MANAGEMENT_PORT:9091}`, health show-details, percentiles-histogram)
- [ ] `src/main/resources/application-e2e.properties` — e2e/CI 는 단일 인스턴스라 9091 유지 or 명시 고정 (bootrun-e2e 동적 검증 대상 포트)
- [ ] `src/test/resources/` 또는 `@SpringBootTest` 대상 — `management.server.port=0` (랜덤포트 테스트 간 9091 충돌 방지)
- [ ] `SecurityConfig.java` — `/actuator/**` permitAll 매처 추가 (근거 주석: 8080 에선 404, 9091 은 비공개 — §2-2)
- [ ] `application/ApplicationService.java` (해당 서비스) — 신청 성공 Counter 계측
- [ ] `common/` 또는 `user/` — `AuthenticationFailureMetrics` `@EventListener` 컴포넌트 신규
- [ ] (Q3 옵션) `MockSmsSender` / CoolSMS sender — 발송 Counter
- [ ] `src/test/java/...` — 커스텀 메트릭 단위 테스트 (`SimpleMeterRegistry` 주입 → 서비스 호출 → counter 값 assert), SecurityConfig 회귀 테스트
- [ ] `fly.toml` — `[metrics]` + `[checks]` (§5)
- [ ] `Dockerfile` — `EXPOSE 9091` (문서화 목적)
- [ ] `docs/observability/**` — §6 전체 (docker-compose, prometheus.yml, provisioning, dashboard JSON 2종, README)
- `.github/workflows/deploy.yml` — **변경 없음**
- 엔티티·템플릿·CSS — **변경 없음**

### PR 분할 제안 (Q5)

| PR | 브랜치 | 내용 | 검증 게이트 |
|---|---|---|---|
| ① | `chore/observability-actuator` | 의존성 + management 설정 + SecurityConfig + fly.toml checks/metrics + Dockerfile | 정적 + 동적 (health/prometheus curl) — **이 PR 머지 후 fly deploy 로 §8 배포 검증까지** |
| ② | `chore/observability-metrics` | 커스텀 메트릭 2종 + 단위 테스트 | 정적 + 동적 (신청 후 counter 증가 확인) |
| ③ | `docs/observability-dashboard` | docker-compose + provisioning + dashboard JSON + README | 로컬 docker compose 기동 + targets UP + 대시보드 렌더 (시각) |

3 PR 권장 — "단계적 진행" 원칙 + ① 이 배포 인프라를 건드리므로 단독 롤백 단위 확보. 2 PR (①+② 통합) 도 허용 범위.

---

## 8. 검증 시나리오 (ym-qa)

### 정적 검증
- `compileJava` 통과 (의존성 좌표가 Boot 4 BOM 에서 해석되는지 — 실패 시 좌표 재조사 후 spec 갱신)
- 신규 단위 테스트: `SimpleMeterRegistry` 기반 counter 증가 assert (신청 성공 시 +1, 실패 경로에선 미증가 / 로그인 실패 이벤트 발화 시 +1)
- 기존 전체 테스트 회귀 — 특히 `@SpringBootTest` 계열이 management 포트 충돌 없이 통과 (`management.server.port=0` 적용 확인)
- SecurityConfig 회귀: 기존 URL 매처 동작 불변 (`*RenderTest` 포함)

### 동적 검증 (bootrun-e2e, 8090 + management 9091)
```bash
# health — 200 + status UP
curl -s http://localhost:9091/actuator/health          # {"status":"UP",...}
# prometheus scrape 포맷 — 기본 JVM 지표 존재
curl -s http://localhost:9091/actuator/prometheus | grep -m1 "jvm_memory_used_bytes"
curl -s http://localhost:9091/actuator/prometheus | grep -m1 "http_server_requests_seconds"
# 공개 포트에서 actuator 미노출 (핵심 보안 요건 — 404 여야 함, 200 이면 FAIL)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/actuator/prometheus   # 404 기대
# 커스텀 메트릭 (PR ②): 로그인 실패 1회 유발 후
curl -s http://localhost:9091/actuator/prometheus | grep "youthmoa_login_failure_total"   # 1.0
# 미노출 엔드포인트 차단
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9091/actuator/env   # 404 기대
```

### 로컬 스택 검증 (PR ③)
- `docker compose up -d` → `http://localhost:9090/targets` 에서 youthmoa target **UP**
- Grafana(`localhost:3000`) 접속 시 provisioning 된 대시보드 2종 자동 존재 + 패널 데이터 표시

### 배포 검증 (PR ① 머지 후 — 사용자와 함께)
- `fly deploy` 성공 + `fly checks list` 로 actuator_health **passing**
- `curl https://youth-moa-java.fly.dev/actuator/prometheus` → **연결 실패 또는 404** (공개 노출 없음 — 무방비 노출 금지 최종 확인)
- fly-metrics.net (Managed Grafana) 에서 앱 지표 유입 확인

### 시각 확인 (사용자 영역)
1. Grafana 대시보드에서 신청 1건 수행 후 도메인 패널 증가 반영
2. Fly Managed Grafana 화면 캡처 (포트폴리오 자료)

---

## 9. 의존성 / 선행 작업
- 선행 없음. Flyway(P0-1)·admin 트랙과 독립 — 스키마 무변경이라 병렬 안전
- `fly.toml` 을 건드리는 다른 브랜치가 없는지만 머지 시점 확인
- CI (`deploy.yml`) 변경 없음 — FLY_DEPLOY_ENABLED 게이트 현행 유지

## 10. 작업 큐 메타
- 작업 ID: `chore-observability` / 우선순위: 포트폴리오 강화 트랙 / 추정: **3 PR** (§7 분할안) / 상태: **spec_done**

---

## 사용자 결정 필요 질문 (Q-리스트)

- **Q1. 접근 제어 방식** — **A안 (권장): 별도 management 포트 9091** (공개 라우팅 자체 차단 + Fly 내장 scrape 호환) / B안: 동일 포트 + 경로 인증 (Fly scrape 불가, 로컬 한정)
- **Q2. 호스팅 조합** — **권장: (c) 로컬 docker-compose 학습 + (a) Fly 내장 metrics/Managed Grafana 배포 병행**. (b) Grafana Cloud 는 Fly Prometheus datasource 연결 방식의 후속 확장으로만 문서화
- **Q3. 커스텀 메트릭 범위** — **권장: 신청 수 + 로그인 실패 2종**. SMS 발송(`mode`/`result` 태그) 포함 여부?
- **Q4. 계측 방식** — **권장: MeterRegistry 직접 주입** / 대안: @Counted/@Timed AOP (프록시 함정으로 비권장)
- **Q5. PR 분할** — **권장: 3 PR** (actuator·인프라 → 커스텀 메트릭 → 대시보드·문서) / 대안: ①+② 통합 2 PR
- **Q6. 기존 `/api/ping` 처리** — A안: 유지 (외부 uptime 체크용 공개 경로로 용도 분화) / B안: 제거 (actuator health 로 일원화 — 단 health 는 비공개 포트라 외부 uptime 모니터링 불가해짐). **권장: A (유지)** + 주석으로 용도 구분
- **Q7. `info` 엔드포인트 + buildInfo** — 포함 시 `build.gradle.kts` 에 `springBoot { buildInfo() }` 추가, 버전·빌드시각 노출 (비공개 포트라 위험 낮음). **권장: 포함** — 배포 버전 즉시 확인 가능
