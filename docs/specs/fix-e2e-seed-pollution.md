# 작업 명세: fix-e2e-seed-pollution — E2E flaky B형 (seed self-pollution) 해소

> 상태: **impl_done** (branch `fix/e2e-seed-pollution`) — 사용자 결정 Q1=C / Q2=권장안 / Q3=(a) / Q4=조사 후 결정
> 결정 확정: 4항 조사 결과 apply() 시점 이벤트 발행 없음 → Notification 삭제 스코프 밖 확정
> 스코프: E2E 테스트 안정성 개선만. 실서비스 로직(`ApplicationService.apply`) 은 test-only 프로파일에서만 우회. 프로덕션 코드 오염 금지.
> read-only 명세. 코드 변경 없음.

---

## 1. 배경 · 문제

### 확정된 사실

- PR #199 머지로 flaky **A형 (JS 초기화 경합)** 은 완전 해소 — `#applyNavNext` addEventListener 등록 이전 클릭 문제는 `window.__applyReady` 플래그와 `applyNextStep()` helper 로 차단됨
- **B형 (seed self-pollution)** 은 이월:
  - `visual-apply-complete.spec.ts:35` — `const seedIdx = 29 + (Math.floor(Date.now() / 1000) % 10)` → seed pool **29~38 (10명)** 을 초 단위로 rotation
  - `apply-complete.spec.ts:28` — 동일 패턴, pool **39~48 (10명)**
  - `--repeat-each=N` 반복 실행 시 같은 초에 여러 iteration 진입 → 같은 seedIdx → 첫 iteration 이 program 7 에 대해 신청 row 를 만들고, 후속 iteration 이 `ApplicationService.apply()` L90 에서 `IllegalStateException("이미 신청한 프로그램입니다.")` 로 실패
- `playwright.config.ts:41` — `webServer.reuseExistingServer: true`. 서버 재사용 상태에서는 이전 실행의 신청 row 가 그대로 남아 self-pollution 지속
- CI (fresh H2, 매 job 마다 새 서버 부팅) 에서는 미발생 — `--repeat-each=1` 이고 job 이 단발
- 실측: `visual-apply-complete.spec.ts --repeat-each=10` → **1 passed / 9 failed** (9/9 실패가 B형 grep 매칭)

### 왜 지금 해결하는가

- 회사 PC 로컬에서 계약 검사 반복 실행 시 계약 갭 판정과 flaky 실패가 섞여 리포트 신뢰도 하락
- `--repeat-each` 는 로컬에서 계약 안정성을 정량 측정하는 표준 수단 → 이걸 못 쓰면 후속 계약·기능 E2E 확장 시 회귀 감지 지연
- 근본 원인 (POST /apply 후 rollback 경로가 없음) 은 test-scoped 로 격리하지 않으면 다른 신청 계열 spec (`apply.spec.ts`, `apply-complete.spec.ts`, `visual-apply.spec.ts`) 로 전염 가능

### 관련 자산 요약

| 경로 | 역할 | 라인 |
|---|---|---|
| `DataInitializer.java` | seed 유저 1~50 생성 (seed29~48 = 신청 없음 fresh) | 1005~1036 |
| `ApplicationService.java` | 중복 신청 차단 (B형 트리거 지점) | 86~98 |
| `visual-apply-complete.spec.ts` | seed 29~38 rotation | 35 |
| `apply-complete.spec.ts` | seed 39~48 rotation | 28 |
| `apply.spec.ts` | seed 30 고정 | 20 |
| `visual-apply.spec.ts` | seed 30 고정 (신청 제출 없음 — 렌더 계약만) | 26 |
| `helpers.ts` | login, applyNextStep, seedEmail | 전체 |
| `playwright.config.ts` | webServer.reuseExistingServer=true, workers=1, fullyParallel=false | 22~45 |

**참고**: seed30 을 신청 제출 spec 이 아닌 것 (`apply.spec.ts` — 첫 진입/필드 검증, `visual-apply.spec.ts` — 3단계 위저드 렌더 계약) 에서만 쓰고 있는지 확인 필요. `apply.spec.ts` L20 은 `FRESH_USER = seedEmail(30)` 이지만 실제로 신청 POST 를 호출하는지는 spec 상세 재확인 필요 (Q3 참조).

---

## 1-A. 자산 간 갭

디자인 자산이 개입하지 않는 **테스트 인프라 스펙** 이라 3자산 비교 표는 해당 없음. prototype·UX 변경 없음.

## 1-B. 데이터 모델 gap 표

신설 필드 없음. `Application` 엔티티·`ApplicationStatus` enum 변경 없음.

단, **선택지 C 채택 시** test-only endpoint 가 추가로 요구하는 것:
- `ApplicationRepository` 에 `deleteAllByUser_EmailAndProgram_Id(...)` 또는 `deleteAllByUser_EmailIn(...)` 파생 메서드 — 삭제 계열이라 신설 리스크 낮음
- 신규 컨트롤러 `TestFixtureController` (프로파일 가드) — 프로덕션 빌드에 포함되지 않도록 격리

## 1-C. 데이터 소비 지점

`Application` row 삭제 시 영향 받는 UI:

| 소비 지점 | 영향 | 조치 |
|---|---|---|
| mypage 내 신청 현황 카드 | seed29~48 유저의 신청 목록에서 사라짐 | 문제 없음 — 이 유저들은 rotation pool 전용 |
| ProgramService capacity 계산 | program 7 의 신청 수 감소 | program 7 은 시드 신청 0건 전제라 무해 |
| ApplicationApprovedEvent 등 이벤트 | 발행 전 삭제라 무해 (신청 후 곧바로 삭제) | 무해 |
| Notification | 신청 성공 시 알림 발행 여부? → **Q4** 로 확인 필요 |  |

## write→read 왕복 시나리오

이 티켓은 신규 저장 로직이 아니라 **정리 로직** 신설이라 write→read 대신 **write→delete→write 시나리오**:

1. seed29 로 program 7 신청 → 성공
2. test-only cleanup 호출
3. 같은 seed29 로 program 7 재신청 → 성공 (첫 신청과 동일한 flow 로 성공해야 함)

---

## 2. 변경 범위 (선택지에 따라 상이 — Q1 답변 후 확정)

### 공통 (모든 선택지)

- `docs/specs/fix-e2e-seed-pollution.md` — 본 문서 (spec_done → spec_confirmed)

### 선택지별 예상 변경

| 선택지 | 변경 파일 | 라인 추정 | 리스크 |
|---|---|---|---|
| A (pool 확대) | `DataInitializer.java` (50→100+), `visual-apply-complete.spec.ts`, `apply-complete.spec.ts` | ~20 | 낮음 — 임시방편, 결국 재발 |
| B (rotation 세밀도) | 2개 spec 파일의 `seedIdx` 계산식 | ~4 | 낮음 — 근본 해결 아님 |
| **C (test-only endpoint)** | `TestFixtureController.java` 신설, `ApplicationRepository.java`, 2개 spec 의 `test.beforeEach`, `application-e2e.yml` (프로파일 노출 조건) | ~60 | 중 — 프로파일 가드 실수 시 프로덕션 노출 |
| D (spec 단 cancel) | 2개 spec 파일 `test.beforeEach` (기존 `applicationService.cancel` 호출 경로 이용) | ~15 | 중 — `cancel` 은 CANCELLED 로 보내지만 그 후 재신청은 `case CANCELLED -> reapply` 경로로 성공. 근본 해결 |
| E (reuseExistingServer=false) | `playwright.config.ts:41` | 1 | 낮음 (설정) — 개발자 경험 저하 크게 나쁨 |
| F (uuid 유저) | `helpers.ts` (createTestUser 신설), `UserRepository` 확인 | ~30 | 중 — 매 test 마다 회원가입 flow 필요, 실행 시간 증가 |

---

## 3. 선택지 Trade-off 표

침습도·재현성·유지비용을 5점 척도로 정량화 (1=좋음, 5=나쁨).

| 선택지 | 침습도 | 재현성 해소 | 유지비용 | 실행 시간 영향 | 프로덕션 오염 위험 | 종합 |
|---|---|---|---|---|---|---|
| **A** pool 30명 이상으로 확대 | 1 | 3 (`--repeat-each=30+` 이면 재발) | 3 (rotation 인원 늘 때마다 시드·spec 동기화) | 1 (기동 시 시드 시간 소폭 증가) | 1 | 임시방편 — **비추천** |
| **B** rotation 세밀도 밀리초 | 1 | 4 (병렬 iteration 이 같은 ms 히트 가능) | 2 | 1 | 1 | 실효성 낮음 — **비추천** |
| **C** test-only cleanup endpoint | 3 | **1** | 2 | 1 (beforeEach + DELETE 1회) | 3 (프로파일 가드 실수 시 위험) | **1순위 후보** |
| **D** spec 단 cancel → reapply 경로 | 1 | 2 (CANCELLED 유저는 재사용 가능하나 pool 여전히 유한) | 2 (`case CANCELLED -> reapply` 경로 의존) | 2 (매 test 취소 API 1회 추가) | 1 | **2순위 후보** — 로직 재활용 |
| **E** `reuseExistingServer: false` | 1 | 1 | 1 | **5** (매 실행 bootRun 2~3분 재기동) | 1 | 개발자 경험 파괴 — **비추천** |
| **F** uuid 유저 매 실행 생성 | 4 | 1 | 4 (회원가입 flow 유지·검증 부담) | 4 (매 test 회원가입 + 로그인) | 2 | **비추천** — 과잉 대응 |

### 1·2순위 상세

**C (test-only cleanup endpoint) 상세 설계 초안**

- Endpoint: `POST /__test__/reset-applications` (또는 `DELETE /__test__/applications`)
- 요청 파라미터: `userEmail` (string) + `programId` (long, optional — 없으면 해당 유저의 모든 신청 삭제)
- 응답: 204 No Content + 삭제 count 헤더
- 프로파일 가드: `@Profile("e2e")` — bootrun-e2e.cmd 는 `SPRING_PROFILES_ACTIVE=e2e` 로 기동. 프로덕션 (`local`/`prod`) 에서는 Bean 등록 자체 안 됨
- 보안: `SecurityConfig` 는 `/__test__/**` 를 `e2e` 프로파일에서만 permitAll. 그 외 프로파일에서는 endpoint 존재 X → 접근 불가
- 호출 시점: 각 신청 계열 spec 의 `test.beforeEach` 에서 로그인 직전 (또는 로그인 후)
- 실행 예:
  ```ts
  await page.request.post('/__test__/reset-applications', {
      data: { userEmail: seedEmail(seedIdx), programId: 7 },
  });
  ```

**D (spec 단 cancel) 상세 설계 초안**

- 각 spec `test.beforeEach` 에서:
  1. 로그인
  2. `page.goto('/mypage')` → 해당 프로그램 신청 카드 검사
  3. 존재 시 취소 버튼 클릭 (또는 `POST /applications/{id}/cancel` 직접 호출) → CANCELLED 로 전이
  4. 본 시나리오 진입
- 근거: `ApplicationService.apply` L92~95 이미 `case CANCELLED -> app.reapply(...)` 로 재활용 경로 존재
- 리스크: mypage UI 를 통해 취소 시 UI 셀렉터 변경에 취약. `POST` 직접 호출이 안전
- **미묘한 이슈**: 첫 실행에는 신청 row 가 없으므로 취소 대상도 없음 → 조건부 로직 필요

---

## 4. 결정 필요 사항 (Q1~Q4)

**Q1. 선택지 채택** — C (test-only endpoint) 와 D (spec 단 cancel) 중 어느 쪽?
- C 장점: 명시적, 근본 해결, 재사용 쉬움 (다른 신청 계열 spec 로 즉시 확장)
- C 단점: 프로덕션 코드에 test 전용 컴포넌트 추가 (프로파일 격리) — 이 프로젝트 최초 사례
- D 장점: 프로덕션 코드 변경 0, `case CANCELLED -> reapply` 이미 존재하는 로직 재활용
- D 단점: 각 spec 이 취소 로직을 알아야 함 (helper 로 추상화 가능하나 여전히 spec 편에서 처리)

권장: **C** (프로파일 가드가 확실하다면 명시성·재사용성이 우월. `Testcontainers` 기반 통합 테스트에서도 필요해질 여지)

**Q2. Endpoint 경로 및 프로파일** — C 채택 시:
- 경로: `/__test__/reset-applications` (권장, 명시적) vs `/test/reset-applications` (짧음, 프로덕션 오해 위험)
- 프로파일: `e2e` 만 vs `e2e,test` 모두
- 응답 형태: 삭제 count 반환 vs 204 No Content

권장: **경로 `/__test__/reset-applications`, 프로파일 `e2e` 만, 응답 204 (삭제 count 는 로그로)**

**Q3. 적용 범위** — 이번 티켓에서 손댈 spec 은?
- (a) `visual-apply-complete.spec.ts` + `apply-complete.spec.ts` 2개만 (B형 실측 재현 파일)
- (b) 신청 제출을 실행하는 모든 spec — 위 2개 + `apply.spec.ts` (신청 제출 여부 재확인 필요) + 향후 추가될 신청 계열 spec 에도 template 확산

권장: **(a) 로 시작하되, helper (`resetApplications(page, email, programId)`) 로 캡슐화해서 다른 spec 이 필요할 때 즉시 적용 가능하게**

**Q4. Notification 부수효과** — 신청 성공 시 Notification 이 발행되는가? 신청 row 삭제 시 관련 Notification row 도 함께 지워야 하는가?
- `ApplicationService.apply()` L100~108 은 이벤트 발행 없음 (승인·반려·취소만 이벤트 발행)
- 그러나 `NotificationListener` 등이 `apply` 성공에 반응하는 코드가 별도 존재할 수 있음 (미확인)
- 만약 존재하고 남으면 Notification 목록 계약이 오염될 수 있음

권장: **소스 확인 필요 — `Grep -r "ApplicationApprovedEvent\|apply" src/main/java/.../notification/`**. 있으면 cleanup endpoint 가 관련 Notification 도 삭제하도록 확장. 없으면 이번 티켓 스코프 밖.

---

## 5. 검증 시나리오

### 정적 검증

- `.\gradlew compileJava` 통과
- 신설된 `TestFixtureController` (C 채택 시) 는 `@Profile("e2e")` 로 로드 → `local`/`prod` 컨텍스트 로드 테스트 (`YouthMoaApplicationTests`) 에서 Bean 미등록 확인
- `SecurityConfig` 프로파일 분기 단위 테스트

### 동적 검증 (curl)

- `SPRING_PROFILES_ACTIVE=e2e` 기동 시:
  - `POST /__test__/reset-applications` with body `{userEmail:"seed29@youth-moa.test", programId:7}` → 204
  - 재호출 → 204 (idempotent)
- `SPRING_PROFILES_ACTIVE=local` 기동 시:
  - `POST /__test__/reset-applications` → 404 (endpoint 미존재)
  - GET `/actuator/mappings` (또는 Spring Boot logs) 에서 해당 endpoint 미등록 확인

### E2E 검증 (재현성)

- **핵심**: `cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-apply-complete.spec.ts --repeat-each=10 --project=contracts`
  - **Before**: 1 passed / 9 failed
  - **After (Expected)**: 10 passed / 0 failed
- `apply-complete.spec.ts --repeat-each=10 --project=chromium` 동일 통과
- CI 회귀 없음: `--project=chromium` 24/24 유지 (신청 계열 외 spec 도 무회귀)
- 병행 확인: `visual-apply.spec.ts`, `apply.spec.ts` 같은 신청 계열 spec 도 회귀 없음

### 프로파일 가드 회귀 방지

- 별도 통합 테스트 `TestFixtureProfileGuardTest` — Spring context 를 `local` 프로파일로 로드했을 때 `TestFixtureController` Bean 이 존재하지 않음을 assert

---

## 6. 의존성 / 선행 작업

- PR #199 (A형 해소) 이미 머지됨 — 선행 조건 충족
- 별도 선행 작업 없음
- 후속 티켓 가능성: **다른 신청 계열 spec 에 rotation 대신 cleanup 확산** (Q3 (b) 채택 시 이번 티켓에 포함, (a) 채택 시 후속)

---

## 7. 작업 큐 메타

- 작업 ID: `fix-e2e-seed-pollution`
- 우선순위: **높음** — 로컬 반복 검증 신뢰도 회복. 후속 계약·기능 E2E 확장의 전제
- 추정 단위: **1 PR** (C 채택 기준, ~60 라인 + 2 spec beforeEach)
- 상태: **spec_done** (Q1~Q4 답변 후 `spec_confirmed` 로 전환하여 ym-impl 인계)

---

## 8. 결정 요청 (사용자 원샷 답변용)

아래 4개 항목에 대해 한 번에 답해 주세요:

1. **Q1** 선택지: `C (test-only endpoint)` / `D (spec 단 cancel)` / 기타
2. **Q2** (C 채택 시) endpoint 경로·프로파일·응답: `/__test__/reset-applications, profile=e2e, 204` / 커스텀
3. **Q3** 적용 범위: `(a) 2개 spec만` / `(b) 신청 제출 spec 전체 확산`
4. **Q4** Notification 삭제 포함: `조사 후 결정` / `이번 스코프 밖` / `포함`

권장 기본값: **Q1=C, Q2=권장안, Q3=(a), Q4=조사 후 결정**

---

## 9. 구현 매핑 (impl_done)

### Q4 조사 결과

`Grep "Application" src/main/java/.../notification/` → `ApplicationNotificationListener.java` 확인.
`@TransactionalEventListener` 로 반응하는 이벤트는 `ApplicationApprovedEvent` / `ApplicationRejectedEvent` /
`ApplicationCancelledEvent` 3종뿐. `ApplicationService.apply()` 성공 시점엔 이벤트 발행이 없어 Notification row 가
만들어지지 않음 → **Notification 삭제는 스코프 밖**으로 확정.

### 파일별 매핑

| 명세 항목 | 파일:라인 | 비고 |
|---|---|---|
| Q1 C — test-only endpoint | `src/main/java/io/github/sihyuuun/youthmoa/test/TestFixtureController.java` (신규) | `@Profile("e2e")` `@RequestMapping("/__test__")` |
| Q2 경로 `/__test__/reset-applications` | 위 파일 `@PostMapping("/reset-applications")` | |
| Q2 요청 바디 `{userEmail, programId?}` | `ResetApplicationsRequest` record | `programId` null → 유저 전체 |
| Q2 응답 204 | `ResponseEntity.noContent().build()` | |
| e2e 프로파일 SecurityConfig 분기 | `SecurityConfig.java` `environment.matchesProfiles("e2e")` if 블록 | `/__test__/**` permitAll + CSRF ignoring |
| Q3 (a) helper 캡슐화 | `e2e/helpers.ts` `resetApplications()` | 실패 시 명시적 throw |
| Q3 (a) visual-apply-complete beforeEach | `e2e/tests/visual-apply-complete.spec.ts` | rotation 제거, seed29 고정 |
| Q3 (a) apply-complete beforeEach | `e2e/tests/apply-complete.spec.ts` | rotation 제거, seed39 고정 |
| Q3 (a) apply.spec.ts 스코프 밖 확인 | `apply.spec.ts` L20 seed30 신청 성공 케이스 없음 | 검증 실패 rerender 만 있음 |
| 프로파일 가드 회귀 방지 | `src/test/java/.../test/TestFixtureProfileGuardTest.java` (신규) | `@ActiveProfiles("test-guard")` |

### 실측

```
compileJava — BUILD SUCCESSFUL
test --tests *TestFixtureProfileGuardTest* — BUILD SUCCESSFUL

playwright --project=contracts visual-apply-complete.spec.ts --repeat-each=10
  10 passed (1.6m)

playwright --project=contracts (전체)
  24 passed (1.8m)  · 합계: 76/76 통과 · 갭 0건

playwright --project=chromium -g "apply"
  7 passed (26.0s)
```

