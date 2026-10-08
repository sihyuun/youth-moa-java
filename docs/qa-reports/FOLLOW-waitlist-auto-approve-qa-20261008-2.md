# QA Report 2차 — FOLLOW-waitlist-auto-approve (2026-10-08)

- 작업 ID: FOLLOW-waitlist-auto-approve
- 1차 QA: `FOLLOW-waitlist-auto-approve-qa-20261008.md` (a9b1790ccd7f7f9fb) — BLOCKER 2건 (B1 ApplicationServiceTest 10 TC context load fail / B2 신규 spec 폼 탭 전환 누락)
- ym-impl 2차: ac85c6e85c3f7f9fb — 2개 파일 수정
  - `src/test/java/.../ApplicationServiceTest.java` (@MockitoBean WaitlistPromotionService 주입)
  - `e2e/tests/admin-waitlist-auto.spec.ts` (116 줄로 전면 재작성)
- 브랜치: `feature/FOLLOW-waitlist-auto-approve` (여전히 미커밋)
- QA 수행: ym-qa 에이전트 (범위: 재검증만, 코드/테스트 수정 금지)
- 환경: Windows / JDK 17 bootstrap + Gradle wrapper / bootRun(e2e 프로파일, port 8090)

---

## 종합 판정 — 1차 vs 2차 비교

| 영역 | 1차 (a9b1790) | 2차 (ac85c6e) |
|---|---|---|
| 정적 — compileJava | PASS | PASS |
| 정적 — 신규 테스트 4종 (17 TC) | PASS | PASS |
| **정적 — 전체 회귀** | **FAIL (751/761, 10 TC fail)** | **PASS (761/761, 0 fail)** ✅ **B1 해소** |
| 동적 — bootRun 상세 토글 round-trip (1차 확인됨) | PASS | PASS (bootRun 재가동 + 정적 리소스 재확인) |
| 동적 — 정적 리소스 + waitlist CSS 셀렉터 서빙 | PASS | PASS (13 매치) |
| **E2E — `admin-waitlist-auto.spec.ts`** | **FAIL (B2 — spec 결함)** | **FAIL (B2' — spec 결함, 양상 변경)** ❌ **B2 미해소** |
| E2E — 디자인 계약 (`--project=contracts`, 45건) | PASS | PASS (45/45, 갭 0건) |
| E2E — `admin-program-detail.spec.ts` regression | — | PASS (3/3) ✅ |
| Side-effect 스캔 | PASS | 변경 없음, 유지 |

**최종 판정 — BLOCKER 1건 (B2' — 신규 spec 자체 결함) 존재. 머지 전 impl 3차 피드백 필요.**

- 핵심 기능 자체는 1차·2차 모두 **단위/통합/계약 테스트로 충분히 검증됨** (JUnit 17 TC + 디자인 계약 45건 + regression 3건).
- 신규 E2E spec 만 작성 결함으로 FAIL. 기능 품질 문제 아님.

---

## 1. 정적 검증

### 1-1. compileJava — PASS

`compileJava UP-TO-DATE` (BUILD SUCCESSFUL in 7m 31s 전체).

### 1-2. 전체 회귀 — **PASS** (B1 해소)

```
.\gradlew.bat test
BUILD SUCCESSFUL in 7m 31s
```

JUnit XML aggregation (117 testsuite xml):

```
files=117  tests=761  fail=0  err=0  skip=0  pass=761
```

- 1차 에서 fail 10건 (ApplicationServiceTest 전부) → 2차 **fail 0건**
- 신규 테스트 4종 (17 TC: ApplicationServiceAutoApproveTest 4 / AdminApplicationServiceAutoApproveTest 3 / AdminProgramAutoApproveTest 3 / AdminProgramDetailRenderTest 7) 모두 유지 PASS
- `ApplicationServiceTest` 10 TC 전부 복구 — `@MockitoBean WaitlistPromotionService waitlistPromotionService;` (line 92) 주입으로 context load 성공 확인

### 1-3. B1 fix 상세 확인

`ApplicationServiceTest.java` 변경 diff:
```
8 insertions
```
- line 88: `/* FOLLOW-waitlist-auto-approve (2026-10-08): ApplicationService 가 WaitlistPromotionService 를 ... */` 주석
- line 92: `@MockitoBean WaitlistPromotionService waitlistPromotionService;`

CLAUDE.md 재발 패턴 (`@WebMvcTest` 슬라이스에 신규 의존성 `@MockitoBean` 필요) 과 **동일 처방**. ApplicationService 신규 의존 해소 완료.

---

## 2. 동적 검증 (bootRun · e2e 프로파일 · port 8090)

### 2-1. bootRun 기동 (background) + smoke check — PASS

```
GET /                      → 200
GET /css/admin.css         → 200
```

`admin.css` 안에 waitlist/토글 신규 셀렉터 서빙: `admin-waitlist-banner|admin-toggle-switch` grep **13 매치** (1차와 동일 수치 확인).

### 2-2. 상세 응답·토글 round-trip 재확인

1차 QA 가 상세 GET 200 + POST /auto-approve 302 round-trip + 배너 미노출 조건 검증 완료. 2차에서 impl 변경이 서버·템플릿·CSS 를 건드리지 않았으므로 **1차 결과 그대로 승계**. bootRun 신규 기동 후에도 CSS·루트 응답 정상.

---

## 3. E2E 검증

### 3-1. 신규 spec `admin-waitlist-auto.spec.ts` — **FAIL (B2' — 1차와 다른 양상)**

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium
→ 1 failed (약 60s)
```

**실패 지점**: `tests/admin-waitlist-auto.spec.ts:66` — `page.waitForURL(/\/admin\/programs\/\d+$/, { timeout: 30_000 })` 가 30s timeout.

**근본 원인 (B2' — 신규)**:

- impl 2차 spec 은 1차의 "탭 전환 누락" 을 해소하기 위해 "프로그램 정보" 탭 활성화 + title/center/content 입력 → "신청 정보" 탭 전환 + 날짜·capacity 입력 흐름으로 재작성됨 (주석 line 10~21 명시).
- capacity 는 **`0`** 으로 설정 (주석 line 20~21: "capacity=0 + applied=0 → 0 >= 0 성립 → 배너 노출" 가정).
- 그러나 서버 측 `AdminProgramService.validate()` **line 414** 에서:
  ```
  "모집 인원은 1명 이상이어야 합니다."  (IllegalArgumentException → 400)
  ```
  → capacity=0 은 폼 submit 시 400 으로 reject 되어 **상세 URL redirect 불발**. 결과: 브라우저가 계속 `/admin/programs/new` 에 머무름 → `waitForURL(/admin/programs/\d+$/)` timeout.
- Playwright snapshot 로 확인: 실패 시점에 "신청 정보" 탭이 selected, `spinbutton "모집 인원"` 값이 `0` (서버 reject 후 같은 폼 재렌더). spec 재작성 과정에서 "0 보다 1 이 안정적" 이라는 주석(line 18)을 적어놓고도 실제 `.fill('0')` 를 사용한 자가모순.

**이 역시 "기능 FAIL" 아니고 "spec 작성 결함"**:
- `AdminProgramAutoApproveTest` (JUnit) 는 capacity 가 확보된 Program 에 대해 toggle on/off/full-cancel 승격 흐름 전부 PASS.
- `AdminProgramDetailRenderTest` 꽉참/여유 양방향 markup 노출 PASS.
- `admin-program-detail` E2E regression 3/3 PASS.
- 디자인 계약 45/45 PASS.

**impl 3차 피드백 요구 (B2')**: spec `line 61` 에서 `await page.locator('input[name="capacity"]').fill('0')` 를 **`'1'`** 로 변경한 뒤, 생성된 program 상세 진입 후 **신청 1건을 추가**해서 (applied=1 >= capacity=1) 꽉참 조건을 만들거나, 더 간단히 **DataInitializer 시드에 꽉찬 프로그램을 미리 추가**하고 spec 는 그 ID 로 바로 진입하는 방식으로 단순화 권장. 1차 피드백 (3번 안) 과 같은 결론.

### 3-2. 디자인 계약 (`--project=contracts`) — PASS

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts
→ 45 passed (2.3m)
```

- 전 화면 45/45 통과 (**갭 0건** 유지 — gap-reports 상세 요약 섹션 log 발췌 포함: "합계: 76/76 통과 · 갭 0건 · 의도적 이탈 2건").
- `admin-program-detail` 계약 (waitlist 배너 조건부 포함) 재집계 통과.

### 3-3. `admin-program-detail.spec.ts` regression — PASS

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-program-detail --project=chromium
→ 3/3 passed (11.0s)
```

| TC | 결과 |
|---|---|
| 상세 → 수정 CTA → 편집 폼 → 저장 → 상세 복귀 | PASS |
| 상세 ⋯ 더보기 → 삭제 메뉴 노출 + 복제 미노출 (Q5 이월) | PASS |
| 상세 하단 "신청 현황 N건 보기" → /applications 이동 (Q6) | PASS |

waitlist 배너 추가가 기존 상세 화면 핵심 흐름에 **regression 0건**.

---

## 4. 1차 → 2차 변동 요약

| ID | 1차 상태 | 2차 조치 | 2차 결과 |
|---|---|---|---|
| B1 (회귀 10 TC fail) | FAIL | `@MockitoBean WaitlistPromotionService` 주입 | **해소 (PASS)** ✅ |
| B2 (신규 spec 폼 탭 전환 누락) | FAIL | spec 전면 재작성 (탭 활성화 + 2 탭 전환 + capacity 입력) | **미해소 (FAIL — 양상 변경)** ❌ |

**새로 포착된 BLOCKER**:
- **B2'**: spec 가 capacity=0 으로 submit 하여 서버 validation `모집 인원은 1명 이상이어야 합니다.` 로 400 reject → 상세 redirect 불발 → `waitForURL` timeout.

---

## 5. 미실행·시각 확인 (사용자 영역, 대기)

1차 QA 5-1·5-2·5-3 과 동일 (변경 없음):

- 실 브라우저에서 꽉찬 프로그램 배너 UX·토글 질감·flashMessage ("대기자 자동 승인을 켰어요/껐어요") 사용자 확인 대기.
- Testcontainers 통합 테스트는 e2e 프로파일 (H2) 로 등가 커버됨 — Flyway V28 실 적용은 머지 후 Deployment 단계에서 Supabase 환경 검증.
- `gap-reports/*.md` 는 untracked 로컬 아티팩트 (매 실행 덮어쓰기).

---

## 6. BLOCKER / 피드백 정리

| ID | 유형 | 설명 | 조치 책임 |
|---|---|---|---|
| ~~B1~~ | ~~회귀 FAIL~~ | ~~`ApplicationServiceTest` 10 TC context load 실패~~ | **해소 ✅ (impl 2차 ac85c6e)** |
| ~~B2~~ | ~~E2E FAIL (spec 결함)~~ | ~~`admin-waitlist-auto.spec.ts` 폼 탭 전환 누락~~ | **해소 완료, 새 결함 B2' 로 교체** |
| **B2'** | E2E FAIL (spec 결함 — 2차 신규) | spec line 61 `capacity.fill('0')` 가 서버 validation `모집 인원 1명 이상` 로 400 reject → `/admin/programs/new` 머물러 `waitForURL` 30s timeout | ym-impl 3차 — spec 수정 (`fill('1')` + applied=1 생성, 또는 시드 꽉찬 program 참조) |

---

## 7. 결론

- **B1 (회귀 10 TC fail) 완전 해소.** 전체 761/761 PASS, 기존 ApplicationService 회귀 방어망 복구.
- **B2 는 해소되었으나 재작성 과정에서 B2' 가 신규 발생.** capacity=0 가정이 서버 validate() 규칙과 충돌.
- 핵심 기능 품질은 1차·2차 공통으로 **단위 17 TC + 디자인 계약 45건 + regression 3건으로 충분히 입증됨**. 신규 E2E spec 자체 결함은 **기능 품질과 분리 판정** 가능.
- 그러나 CLAUDE.md "인터랙션 검증 — 클릭·상태 전환은 기능 E2E 필수 (2026-08-13 신설)" 조항 상 신규 POST+redirect 흐름은 **기능 E2E 1건 통과 필수**. 머지 전 B2' 해소 요구.
- 사용자 시각 확인 대기 항목은 1차와 동일.

**권장 순서**: impl 3차 (spec `fill('0')` → `fill('1')` + 신청 생성 또는 시드 활용) → ym-qa 3차 (admin-waitlist-auto spec 만 재돌림, ~1분) → 머지.

---

## 부록 — 명령 로그

```powershell
# 정적
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat test                                                             # 761/761 PASS, 7m 31s

# 동적 smoke
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/                   # 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/css/admin.css      # 200
curl -s http://localhost:8090/css/admin.css | grep -c "admin-waitlist-banner|admin-toggle-switch"  # 13

# E2E
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium  # 1 FAIL (B2')
cd e2e && BASE_URL=http://localhost:8090 npx playwright test admin-program-detail --project=chromium # 3/3 PASS
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts                      # 45/45 PASS
```
