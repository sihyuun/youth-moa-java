# ADR A9-c: N+1 baseline 정책 (코드 상수 + JSON artifact 병행)

- 상태: Accepted
- 결정일: 2026-09-29
- 관련 트랙: A6-followup (2026-09-29 머지, N+1 실증 회귀 55→32 queries) · A9-b verify UNVERIFIED #16
- 관련 파일:
  - `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminEagerFetchN1Test.java`
  - `src/test/java/io/github/sihyuuun/youthmoa/user/UserEagerFetchN1Test.java`
  - `src/test/java/io/github/sihyuuun/youthmoa/common/N1BaselineReporter.java`
  - `.github/workflows/ci.yml`

## 컨텍스트

A9-b 에서 `Application.program` · `Bookmark.program` 을 LAZY → EAGER 로 승격하면서 다음이 실증됐다.

- open-in-view=false 하 MyPageRenderTest 실패로 EAGER 승격이 불가피
- admin/사용자 진입점 여러 곳에서 N+1 발생 위험이 함께 유입됨
- A6-followup 에서 실제로 회귀 감지: admin 대시보드 진입 시 55 queries → @EntityGraph 조정으로 32 queries 로 복구

즉 EAGER 승격 자체가 나쁜 판단은 아니지만, 그 다음의 회귀는 **감지 지점이 없으면 조용히 누적**된다. Hibernate 통계 기반 회귀 감시 테스트가 필요하다.

### 대안 검토

| 안 | 감지 시점 | 트렌드 분석 | 유지 비용 |
|---|---|---|---|
| A. 코드 상수 (테스트만) | 회귀 즉시 CI FAIL | 없음 | 낮음 (테스트 파일만 수정) |
| B. JSON export (artifact 만) | 사후 분석 | 가능 | 낮음 (자동 수집) |
| C. 코드 상수 + JSON 병행 | 즉시 감지 + 트렌드 | 가능 | 중간 |
| D. Prometheus/Grafana | 상시 관측 | 강력 | 높음 (인프라 추가) |

## 결정

**안 C 채택**: 코드 상수 + JSON artifact 병행.

- **코드 상수 (`isLessThanOrEqualTo(N)`)**: 회귀 즉시 CI FAIL 로 감지. baseline 실측값에 **+30% 여유율** 적용.
  - 30% 여유율 근거: seed 데이터가 늘거나(신규 시나리오·재실행 순서 차이), Hibernate 마이너 버전 업그레이드로 준비 statement 가 미세 증가하는 상황을 허용. 그보다 큰 변화는 회귀로 판정.
  - baseline 은 로컬 실행 후 impl 이 명시적으로 코드에 하드코딩 (자동 갱신 금지).
- **JSON artifact (`build/reports/n1-baseline.json`)**: `N1BaselineReporter` 가 매 TC 실측값 + 상한을 append. CI 는 `n1-baseline-<run_number>` artifact 로 14 일 보존 → 시계열 트렌드 분석에 사용.

### baseline 갱신 정책

- 실측값이 상한을 초과하면 → **먼저 회귀 여부부터 조사**. 상한을 올려 통과시키지 말 것.
- 정당한 사유 (신규 시드·의도적 스키마 변경 등) 로 baseline 자체가 이동해야 하면 → **PR 로 상한 수정 + 사유 명시**. 자동 갱신 스크립트는 도입하지 않는다 (감지 장치가 무의미해짐).

### 커버리지

| 트랙 | 파일 | TC 수 |
|---|---|---|
| Admin | `AdminEagerFetchN1Test` | 7 (기존 5 + A6/A7 확장 2) |
| User | `UserEagerFetchN1Test` | 6 |

사용자 트랙 TC 목록 (2026-09-29 e2e 프로파일 실측):

| TC | 진입점 | 실측 | 상한 (+30%, ceil) |
|---|---|---|---|
| U1 | `ProgramService.search` | 11 | 15 |
| U2 | `ProgramService.findById` | 1 | 2 |
| U3 | `MyPageController` history 탭 | 10 | 13 |
| U4 | `MyPageController` favorites 탭 | 11 | 15 |
| U5 | `NotificationService.recentForHeader` | 2 | 3 |
| U6 | `SearchService.search` | 16 | 21 |

Admin 확장 TC (A9-c 신설분):

| TC | 진입점 | 실측 | 상한 (+30%, ceil) |
|---|---|---|---|
| A6 | `AdminProgramService.list("청년","OPEN",0)` | 11 | 15 |
| A7 | `AdminProgramService.find(1)` | 2 | 3 |

기존 A9-b 유산 5 TC (`AdminApplicationService.list` 등) 는 100% 여유로 tight 유지 — A9-b 회고에서 확정된 값.

## 귀결

- 회사 PC / CI ubuntu 러너 모두 H2 e2e 프로파일로 실행 가능 → Testcontainers 불필요.
- 새 화면·서비스 진입점 추가 시 N+1 감시 TC 를 함께 추가하는 습관이 강제됨.
- baseline JSON 이 축적되면 EAGER/@EntityGraph 최적화 효과를 정량으로 회고할 수 있다.

## 폐기 조건

Hibernate 통계 API 가 향후 deprecated 되거나 Boot 5+ 에서 계약이 크게 바뀌면, Datasource proxy (p6spy) 기반 감시 또는 전용 관측 스택 (안 D) 으로 이관 검토.
