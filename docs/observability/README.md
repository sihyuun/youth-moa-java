# Observability — 로컬 Prometheus + Grafana

`youth-moa-java` 앱의 Actuator 지표를 로컬에서 시각화하는 스택입니다. 앱 자체 설정은 **PR-1**(`chore/observability-actuator`) 에서 완료되어 있고, **PR-2**(`chore/observability-metrics`) 에서 도메인 커스텀 메트릭 2종(`youthmoa_application_submitted_total`, `youthmoa_login_failure_total`) 이 등록되어 있습니다. 본 문서는 **PR-3** (`docs/observability-dashboard`) — 로컬 재현 스택 + 대시보드입니다.

---

## 개념 요약

| 구성요소 | 역할 |
|---|---|
| **Spring Boot Actuator** | 앱 내부 상태를 `/actuator/*` HTTP 엔드포인트로 노출. 본 앱은 별도 포트 **9091** 로 운영 (공개 포트 8080 과 격리) |
| **Micrometer** | 메트릭 파사드. 코드에서는 `MeterRegistry` 만 사용. Prometheus 포맷 export 는 `micrometer-registry-prometheus` 가 담당 |
| **Prometheus** | 시계열 DB. **pull 모델** — 5초마다 `/actuator/prometheus` 를 긁어감 (scrape) |
| **Grafana** | 시각화. Prometheus datasource 를 연결해 차트만 그림 |

Micrometer → Prometheus 이름 변환: `youthmoa.application.submitted` (Counter) → `youthmoa_application_submitted_total`.

---

## 사전 준비

1. 앱을 로컬 기동 (management 포트 9091 노출 필수)
   ```powershell
   # Windows (회사 PC)
   .\.claude\scripts\bootrun-e2e.cmd
   # → 앱 8090, actuator 9091
   ```
   또는 개인 PC 에서:
   ```bash
   ./gradlew bootRun
   # → 앱 8080, actuator 9091 (기본)
   ```

2. Actuator 엔드포인트 접근 확인
   ```bash
   curl -s http://localhost:9091/actuator/health
   # {"status":"UP",...}
   curl -s http://localhost:9091/actuator/prometheus | grep -m1 jvm_memory_used_bytes
   ```

3. Docker Desktop 기동 (Windows/Mac). Linux 는 `docker` 데몬만 있으면 됨.

---

## 로컬 스택 기동

```bash
cd docs/observability/local
docker compose up -d
```

기동 후 접근:

| URL | 계정 | 용도 |
|---|---|---|
| http://localhost:9090 | — | Prometheus UI. **Status → Targets** 에서 `youthmoa` job 이 **UP** 인지 확인 |
| http://localhost:9090/graph | — | PromQL 임의 질의 (예: `up`, `youthmoa_application_submitted_total`) |
| http://localhost:3000 | admin/admin (또는 anonymous Viewer) | Grafana. 좌측 Dashboards → `youth-moa-java` 폴더 → **youth-moa-java — 앱 지표** |

### targets 가 DOWN 이면

- `scrape_configs.static_configs.targets` 는 `host.docker.internal:9091` — Docker Desktop 은 자동 매핑, Linux 는 `docker-compose.yml` 의 `extra_hosts` (`host-gateway`) 로 처리
- 앱이 실제 9091 을 열고 있는지 호스트에서 curl 로 확인 (`curl http://localhost:9091/actuator/prometheus`)
- 방화벽이 컨테이너 → 호스트 loopback 을 막고 있지 않은지 확인 (회사망에서 흔함)

---

## 대시보드 구성

`grafana/youthmoa-dashboard.json` (provisioning 자동 등록)

| 행 | 패널 | 지표 |
|---|---|---|
| **JVM** | Heap 사용량 / GC pause / 스레드 수 | `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live_threads`, `jvm_threads_daemon_threads` |
| **HTTP** | RPS / p95 지연 / 5xx 비율 | `http_server_requests_seconds_count`, `..._seconds_bucket` (`percentiles-histogram` 활성 필요 — PR-1 에서 설정됨), `status=~"5.."` 필터 |
| **DB** | HikariCP active/idle/pending / total·max | `hikaricp_connections_*` |
| **도메인** | 누적 신청 수 · 신청 rate · 로그인 실패 increase | `youthmoa_application_submitted_total`, `youthmoa_login_failure_total` |

### 커뮤니티 JVM 대시보드 (선택)

Grafana 커뮤니티 대시보드 [**JVM (Micrometer) — ID 4701**](https://grafana.com/grafana/dashboards/4701) 을 추가 import 하면 스레드/클래스 로더/버퍼 풀 등 상세 뷰가 확보됩니다.

절차:
1. Grafana 좌측 메뉴 → Dashboards → New → **Import**
2. `4701` 입력 → Load
3. Prometheus datasource 로 이 스택의 `Prometheus` 선택 → Import

라이선스 상 JSON 을 repo 에 임베드하지 않고, 필요 시 위 절차로 각자 import 하는 방식을 채택합니다.

---

## 검증 시나리오

### 스택 자체
- `http://localhost:9090/targets` — `youthmoa` job **UP**
- Grafana → 대시보드 → 모든 패널 데이터 표시 (JVM/HTTP 는 몇 초 내, DB 는 최초 커넥션 이후, 도메인은 액션 발생 후)

### 커스텀 메트릭 반영
```bash
# 로그인 실패 1회 유발 (아무 잘못된 크레덴셜)
curl -c cookies -b cookies -s -o /dev/null \
  -X POST http://localhost:8090/login \
  --data-urlencode "username=wrong@example.com" \
  --data-urlencode "password=wrong"

# 5~10초 후 Prometheus scrape → Grafana 도메인 행에 반영
curl -s http://localhost:9091/actuator/prometheus | grep youthmoa_login_failure_total
# youthmoa_login_failure_total 1.0
```

신청 카운터는 실 로그인 + POST /applications 필요. e2e 프로파일 시드 계정으로 재현 가능.

---

## 배포 환경 (Fly.io)

앱 쪽 설정은 로컬과 완전 동일 (`/actuator/prometheus` 9091 노출) → Fly 내장 스크레이퍼가 `fly.toml` `[metrics]` 설정으로 자동 수집, Managed Grafana (`fly-metrics.net`) 에서 확인. 본 로컬 스택은 학습·개발용이며 배포 환경은 별도 관리.

---

## 정리

```bash
cd docs/observability/local
docker compose down          # 컨테이너 정지
docker compose down -v       # 볼륨까지 삭제 (Grafana 임시 상태 초기화)
```

## 파일 구조

```
docs/observability/
├── README.md
├── local/
│   ├── docker-compose.yml               # prometheus + grafana (host.docker.internal:9091 scrape)
│   └── prometheus.yml                   # 5s scrape_interval, job=youthmoa
├── grafana-provisioning/
│   ├── datasources/prometheus.yml       # datasource 자동 등록 (uid: youthmoa-prometheus)
│   └── dashboards/dashboards.yml        # /var/lib/grafana/dashboards 폴더 로드 설정
└── grafana/
    └── youthmoa-dashboard.json          # JVM + HTTP + DB + 도메인 4행 대시보드
```
