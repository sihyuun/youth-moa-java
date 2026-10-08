# QA Report — FOLLOW-waitlist-auto-approve (2026-10-08)

- 작업 ID: FOLLOW-waitlist-auto-approve
- 선행: ym-spec a946634eb664b6a5f / ym-impl a8154447b180db1f3
- 브랜치: `feature/FOLLOW-waitlist-auto-approve` (미커밋)
- QA 수행: ym-qa 에이전트
- 수행 환경: Windows / JDK 17 bootstrap + Gradle wrapper / bootRun(e2e 프로파일, port 8090)
- 변경 범위: 신설 9 + 수정 10 (총 19 파일)

---

## 종합 판정

| 영역 | 결과 |
|---|---|
| 정적 — compileJava | PASS |
| 정적 — 신규 테스트 4종 (17 TC) | PASS |
| 정적 — 전체 회귀 (761 TC) | **FAIL** — ApplicationServiceTest 10 TC 전부 context load 실패 |
| 동적 — bootRun 8090 상세 + 토글 round-trip | PASS |
| 동적 — 정적 리소스 + 신규 CSS 셀렉터 서빙 | PASS |
| E2E — admin-waitlist-auto.spec.ts | **FAIL** — 신규 spec 자체 결함 (프로그램 폼 탭 전환 누락) |
| E2E — 디자인 계약 (--project=contracts) | PASS (45/45, 갭 0건) |
| Side-effect 스캔 | PASS (의도치 않은 참조 없음) |

**최종 판정 — BLOCKER 2건 (아래 B1·B2) 존재. 머지 전 impl 피드백 필요.**

---

## 1. 정적 검증

### 1-1. compileJava — PASS

```
.\gradlew.bat compileJava
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 7s
```

### 1-2. 신규/수정 테스트 4종 — PASS (17 TC / 0 fail)

| 테스트 클래스 | TC 수 | 결과 | 소요 |
|---|---|---|---|
| `ApplicationServiceAutoApproveTest` | 4 | PASS | 0.27s |
| `AdminApplicationServiceAutoApproveTest` | 3 | PASS | 48.4s |
| `AdminProgramAutoApproveTest` | 3 | PASS | 11.5s |
| `AdminProgramDetailRenderTest` (waitlist 2건 포함) | 7 | PASS | 1.96s |

Impl 특별 체크포인트 커버 결과:
- Q2 승격 트리거 매트릭스 — `admin_reject_approved_promotes_pending`, `admin_forceCancel_approved_promotes_pending`, `toggleOn_full_cancel_promotes` 모두 PASS
- Prev==APPROVED 분기 — `admin_reject_pending_doesNotPromote` 로 PENDING→REJECTED 는 승격 트리거 X 확인
- Q3 sysadmin 재활용 + adminNote — `getProcessedBy().getEmail() == SYSTEM_ADMIN_EMAIL` + `adminNote == PROMOTION_NOTE` 모두 어서트 통과
- Q4 OFF→ON 전환 시 일괄 승급 금지 — `toggleOff_full_noPromotion` 로 토글 OFF 시 승격 X 확인
- Q5 배너 조건 (capacity != null && applied >= capacity) — `GET_admin_program_detail_정원_꽉참_배너_노출` + `GET_admin_program_detail_정원_여유_배너_미노출` 양방향 PASS
- capacity=null 가드 — `toggleOn_noCapacity_noPromotion` PASS (정원 제한 없는 프로그램은 "꽉참" 성립 안 함)

### 1-3. 전체 회귀 — **FAIL** (B1 BLOCKER)

```
.\gradlew.bat test
total=761 failures=10 errors=0 skipped=0 pass=751
```

**실패 집중 지점:** `io.github.sihyuuun.youthmoa.application.ApplicationServiceTest` (10/10 TC 전부 FAIL)

**근본 원인 (B1):**
```
UnsatisfiedDependencyException: Error creating bean with name
'io.github.sihyuuun.youthmoa.application.ApplicationService':
Unsatisfied dependency expressed through constructor parameter 6:
No qualifying bean of type
'io.github.sihyuuun.youthmoa.application.WaitlistPromotionService' available
```

`ApplicationServiceTest` 는 `@DataJpaTest` 슬라이스 + `@Import({..., ApplicationService.class, ...})` 로 ApplicationService 를 수동 임포트한다. 이번 변경에서 `ApplicationService` 가 `WaitlistPromotionService` 를 생성자 주입하도록 수정됐으나, 슬라이스 테스트 컨텍스트에는 `WaitlistPromotionService` 가 등록되지 않아 10 TC 전부 context load 단계에서 실패.

**CLAUDE.md 재발 패턴**: "`@ControllerAdvice` 가 추가된 경우 기존 `@WebMvcTest` 슬라이스 전부에 해당 의존성 `@MockitoBean` 필요" 와 동형. ApplicationService 가 신규 의존을 받으면 이를 임포트하는 모든 슬라이스 테스트에 `WaitlistPromotionService` 를 `@Import` 또는 `@MockitoBean` 으로 추가해야 한다.

**영향**: 신규 기능과 무관한 10 TC 가 깨짐 — ApplicationService 본진의 신청/승인/반려/취소 플로우 회귀 방어망이 전부 사라진 상태.

**impl 피드백 요구**: `src/test/java/.../ApplicationServiceTest.java` `@Import` 블록에 `WaitlistPromotionService.class` 추가 (또는 `@MockitoBean WaitlistPromotionService waitlistPromotionService`). 수정 후 전체 회귀 재실행 필수.

---

## 2. 동적 검증 (bootRun · e2e 프로파일 · port 8090)

### 2-1. 상세 응답 + 배너 조건 반영 — PASS

```
GET /admin/programs/1    → 200
```

- Program 1 (capacity=30, applied < capacity) → `waitlist-auto-banner` markup **미노출** (0 매치) ✅
- 응답 HTML 에 미처리 Thymeleaf 표현식 없음 (grep 매치 3건은 전부 HTML 주석 안의 설명 문구 — 실제 `th:` / `${}` 미처리 아님)
- `admin-program-detail-title`, `admin-status-badge` 등 상세 전용 markup 정상 렌더

### 2-2. `POST /admin/programs/{id}/auto-approve` round-trip — PASS

```
POST /admin/programs/1/auto-approve  enabled=true
  → 302 redirect to http://localhost:8090/admin/programs/1 ✅
POST /admin/programs/1/auto-approve  enabled=false
  → 302 redirect to http://localhost:8090/admin/programs/1 ✅
```

PRG 패턴 + Location 헤더 정확히 상세 복귀.

### 2-3. 정적 리소스 — PASS

```
GET /css/admin.css   → 200
GET /css/main.css    → 200
```

`admin.css` 안에 신규 waitlist 셀렉터 서빙 확인:
- `admin-waitlist-banner`, `admin-waitlist-banner-text`, `admin-waitlist-banner-title`, `admin-waitlist-banner-desc`, `admin-waitlist-banner-form`
- `admin-toggle-switch`, `admin-toggle-switch--on`, `admin-toggle-switch-label`, `admin-toggle-switch-button`, `admin-toggle-switch-thumb`

---

## 3. E2E 검증

### 3-1. 신규 spec `admin-waitlist-auto.spec.ts` — **FAIL** (B2 BLOCKER)

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium
→ 1 failed (60s timeout)
```

**실패 지점**: `tests/admin-waitlist-auto.spec.ts:48` — "꽉 찬 프로그램 신규 생성" 단계에서 저장 버튼 클릭 후 `page.waitForResponse(POST /admin/programs)` 가 60s timeout.

**근본 원인**: spec 코드가 폼 입력 전 **"프로그램 정보" 탭으로 전환하지 않음**. 현재 폼은 탭 UI (`프로그램 정보` / `신청 정보` / `약관 정보`) 로 분리됐고 title/center/content 는 "프로그램 정보" 탭 안에 있다. spec 는 바로 `input[name="title"]` 를 fill 하고 "신청 정보" 탭으로 이동만 하므로 필수 필드가 비어 submit 시 서버 400 또는 클라이언트 validation 블록 (실제 Playwright snapshot 상 "신청 정보" 탭만 활성 상태로 멈춰 있음).

**이 spec 자체의 결함** — 핵심 기능(토글 round-trip·배너 노출)은 render test + bootRun curl 로 모두 PASS 입증됐으나, 신규 E2E 는 작성 단계에서 폼 UI 흐름을 반영하지 못함.

**impl 피드백 요구**: spec 를 다음과 같이 수정 필요:
1. `/admin/programs/new` 진입 후 **"프로그램 정보" 탭에서** title/centerId/content 입력
2. "신청 정보" 탭 전환 후 applyStart/End/capacity 입력
3. 또는: spec 를 "폼 생성 경유" 대신 **DataInitializer 가 미리 생성한 꽉 찬 시드 program** 을 참조하는 방식으로 단순화 (더 안정적)

**검증 보완**: 신규 spec FAIL 은 "기능이 깨져서 FAIL" 이 아니라 "spec 작성이 폼 UI 와 불일치". 기능 자체는 JUnit (`AdminProgramAutoApproveTest`, `AdminProgramDetailRenderTest` 꽉참/여유 2 TC) + 동적 curl (round-trip 302) 로 동치 검증 완료.

### 3-2. 디자인 계약 (`--project=contracts`) — PASS

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts
→ 45 passed (2.7m)
```

- `admin-program-detail` 계약 재집계 완료 (`waitlist.banner.conditional` 체크 포함 — program 1 은 조건 미충족이므로 count=0 기대, 실측 count=0 일치)
- 전 화면 갭 0건 유지 — 이번 변경이 다른 화면에 영향 없음

---

## 4. Side-Effect Scan — PASS

### 4-1. `grep -rn "waitlist|promoteIfEligible|autoApproveWhenFull"` (src 전역)

매칭 파일 26건 중:
- 신규 파일 7건 (WaitlistPromotionService, _waitlist-banner.html, V28 SQL, 테스트 3종, spec 1종)
- 수정 파일 10건 (ApplicationService, AdminApplicationService, AdminProgramController, AdminProgramService, Program, ApplicationRepository, detail.html, admin.css, AdminProgramDetailRenderTest, admin-program-detail.ts)
- 매칭된 나머지 9건은 **"notification wait" / "await" 등 단어 겹침**이며 waitlist 기능과 무관 (notifications.html, index.html, program/_list-fragment.html, V1_baseline.sql 등 전부 사전 확인)

→ 의도치 않은 참조·호출 없음.

### 4-2. Spring Event — 영향 없음

impl 는 신규 Spring Event 를 도입하지 않음. 승격 성공 시 `ApplicationApprovedEvent` 를 재발행하여 사용자 알림 (ApplicationNotificationListener) 만 재활용. 관리자 알림 fan-out 흐름에 끼어드는 변경 없음.

### 4-3. `ApprovalMode` enum 교차 영향 — 없음 (Q1 A 준수)

`autoApproveWhenFull` 은 `approvalMode` 와 독립 boolean. enum 확장 없음. apply() 자동 승인 경로 (approvalMode=AUTO) 와 무관.

---

## 5. 미실행·시각 확인 (사용자 영역, 대기)

### 5-1. 꽉 참 조건에서 실 브라우저 배너·토글 UX
- JUnit render test 로 markup 포함 확인 완료, CSS 서빙 확인 완료, 하지만 **실제 노란 배경 + 토글 스위치 질감·클릭 피드백**은 사용자 브라우저 확인 영역
- 확인 URL: 꽉 찬 프로그램 (capacity ≤ applied) 상세 페이지. seed 에 포함된 꽉 참 program 이 없다면 신규 생성 (capacity=0) 후 확인

### 5-2. flashMessage ("대기자 자동 승인을 켰어요/껐어요")
- 302 round-trip 후 session 1회 소비 — curl 분리 호출로는 재현 불가 (정상)
- 실 브라우저 토글 클릭 시 flash 문구 노출 확인 영역

### 5-3. Testcontainers 통합 테스트
- 회사 PC 는 Docker 가용하므로 실행 가능. 단, 본 QA 는 e2e 프로파일 (H2) 으로 충분히 커버됨 (JPA 매핑 + Flyway V28 적용 포함 — 단, Flyway 는 PG 전용이라 e2e 프로파일에선 Hibernate create-drop 로 대체)
- Supabase 환경 V28 실 적용은 머지 후 Deployment 단계에서 검증

---

## 6. BLOCKER / 피드백 정리

| ID | 유형 | 설명 | 조치 책임 |
|---|---|---|---|
| **B1** | 회귀 FAIL | `ApplicationServiceTest` 10 TC context load 실패 — `@Import` 에 `WaitlistPromotionService.class` 추가 필요 | ym-impl |
| **B2** | E2E FAIL (spec 결함) | `admin-waitlist-auto.spec.ts` 폼 탭 전환 누락 — "프로그램 정보" 탭에서 title/center/content 입력 후 "신청 정보" 탭으로 이동하도록 수정 | ym-impl |

### 7. 결론

- 핵심 기능(승격·배너 조건·토글 round-trip·sysadmin 재활용·FIFO·capacity 가드·멱등 가드·3경로 커버)은 신규 테스트 17 TC + 동적 curl + 디자인 계약 재집계로 **기능 레벨 검증 전부 통과**
- 하지만 **(1) 기존 회귀망에 BLOCKER (B1)** 와 **(2) 신규 E2E 자체 결함 (B2)** 가 남아 있어 **머지 보류**
- B1·B2 모두 impl 측 **테스트 자산** 수정이면 해결 (프로덕션 코드 수정 불필요). 수정 후 재 QA 요청 바랍니다.

---

## 부록 — 명령 로그

```
# 정적
.\gradlew.bat compileJava                           # UP-TO-DATE
.\gradlew.bat test --tests ApplicationServiceAutoApproveTest ...  # 17/17 PASS
.\gradlew.bat test                                  # 751/761 PASS, FAIL 10 (ApplicationServiceTest)

# 동적
curl -b cookies.txt http://localhost:8090/admin/programs/1          # 200, 배너 미노출
curl -X POST http://localhost:8090/admin/programs/1/auto-approve ...  # 302 redirect
curl http://localhost:8090/css/admin.css                              # 200, waitlist 셀렉터 서빙

# E2E
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium  # FAIL (B2)
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts                      # 45/45 PASS
```
