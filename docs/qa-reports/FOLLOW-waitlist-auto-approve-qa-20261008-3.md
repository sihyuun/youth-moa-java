# QA Report 3차 — FOLLOW-waitlist-auto-approve (2026-10-08)

- 작업 ID: FOLLOW-waitlist-auto-approve
- 1차 QA: `FOLLOW-waitlist-auto-approve-qa-20261008.md` — BLOCKER 2건 (B1 회귀 10 TC fail / B2 신규 spec 폼 탭 전환 누락)
- 2차 QA: `FOLLOW-waitlist-auto-approve-qa-20261008-2.md` — B1 해소 ✅ / B2' 재발 (capacity=0 서버 reject) ❌
- ym-impl 3차: spec `admin-waitlist-auto.spec.ts` 수정
  - capacity `'0'` → `'1'` (서버 validate ≥1 통과)
  - 별도 browser context 로 seed 유저(seed29) 로그인 → `applyProgram(programId)` 로 신청 1건 추가 → applied=1 >= capacity=1 조건 성립
  - admin page 재진입 → 배너·토글 노출·OFF→ON→OFF round-trip 검증
- 브랜치: `feature/FOLLOW-waitlist-auto-approve` (여전히 미커밋)
- QA 수행: ym-qa 에이전트 (범위: 재검증만. 기존 bootRun(8090) 재사용, 코드 수정 없음)
- 환경: Windows / JDK 17 bootstrap + Gradle wrapper / bootRun(e2e 프로파일, port 8090) · 이미 기동 상태 재사용

---

## 종합 판정 — 1차 → 2차 → 3차 추이

| 영역 | 1차 | 2차 | 3차 |
|---|---|---|---|
| 정적 — compileJava | PASS | PASS | PASS (변경 없음, 승계) |
| 정적 — 신규 테스트 4종 (17 TC) | PASS | PASS | PASS (변경 없음, 승계) |
| 정적 — 전체 회귀 (761 TC) | FAIL (10 fail) | **PASS** ✅ | PASS (JUnit 비수정, 승계) |
| 동적 — bootRun 응답 + 정적 리소스 | PASS | PASS | PASS (기동 유지, GET / 200) |
| **E2E — `admin-waitlist-auto.spec.ts`** | **FAIL (B2)** | **FAIL (B2')** | **PASS ✅ B2' 해소** |
| E2E — `admin-program-detail.spec.ts` regression (3 TC) | — | PASS | **PASS ✅ (3/3, 10.6s)** |
| E2E — 디자인 계약 (`--project=contracts`, 45건) | PASS | PASS | 2차 결과 승계 (impl 변경이 spec 1개 파일로 국한, 계약·구현 비변경) |
| Side-effect 스캔 | PASS | 유지 | 유지 |

**최종 판정 — BLOCKER 0건. 머지 가능.**

- 1차·2차 모두 핵심 기능 품질은 단위/통합/계약으로 입증됐으나 신규 E2E spec 자체 결함으로 블록. 3차 impl 수정으로 spec 통과 → CLAUDE.md "인터랙션 검증 — 클릭·상태 전환은 기능 E2E 필수" 조항 충족.
- impl 3차 변경 범위가 `e2e/tests/admin-waitlist-auto.spec.ts` 1개 파일 (JUnit·Controller·Template·CSS 비변경) 이므로 2차에서 PASS 한 회귀 761 TC + 계약 45건 결과를 승계. 추가 전수 재실행은 리소스 낭비로 생략.

---

## 1. E2E 재검증 (B2' 해소 확인)

### 1-1. `admin-waitlist-auto.spec.ts` — PASS ✅

```powershell
cd e2e
BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium --reporter=line
```

```
Running 1 test using 1 worker
[1/1] [chromium] › tests\admin-waitlist-auto.spec.ts:39:5 › 정원 꽉 참 프로그램 상세: 배너 노출 + 토글 OFF→ON→OFF 라운드트립
  1 passed (19.6s)
```

**검증 포인트 전수 통과**:
1. ✅ capacity=1 로 program 생성 → 상세 URL redirect
2. ✅ 별도 browser context 로 seed29 로그인 → applyProgram(programId) → applied=1
3. ✅ admin page 재진입 → `[data-testid="waitlist-auto-banner"]` visible
4. ✅ `[data-testid="waitlist-auto-toggle"]` 초기 `aria-pressed="false"`
5. ✅ 토글 클릭 → POST `/admin/programs/{id}/auto-approve` → status ≥300 (302) → 상세 복귀
6. ✅ 재진입 상태 `aria-pressed="true"` (ON)
7. ✅ 재토글 → `aria-pressed="false"` (OFF) 복귀 — round-trip 완결

CLAUDE.md 인터랙션 검증 체크리스트 4항 전부 충족 (status 코드 / DOM 변화 / URL 변화 / 응답 동기화).

### 1-2. `admin-program-detail.spec.ts` regression — PASS ✅

waitlist 배너 추가가 상세 화면 핵심 흐름에 regression 없음 확인:

```powershell
cd e2e
BASE_URL=http://localhost:8090 npx playwright test admin-program-detail --project=chromium --reporter=line
```

```
Running 3 tests using 1 worker
[1/3] 상세 → 수정 CTA → 편집 폼 → 저장 → 상세 복귀
[2/3] 상세 ⋯ 더보기 → 삭제 메뉴 노출 + 복제 미노출 (Q5 이월)
[3/3] 상세 하단 "신청 현황 N건 보기" → /applications 이동 (Q6)
  3 passed (10.6s)
```

---

## 2. 정적 검증 — 2차 결과 승계

- 3차 impl 변경 파일: `e2e/tests/admin-waitlist-auto.spec.ts` (TypeScript spec) 1건.
- JUnit · Controller · Service · Template · CSS 모두 비변경.
- 2차 `.\gradlew.bat test` 전체 761/761 PASS 결과를 승계. 추가 전수 재실행은 변경 없는 소스에 대해 7분 반복 → 리소스 낭비.

---

## 3. 동적 검증 — bootRun 유지 확인

```powershell
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/   # 200
```

bootRun(e2e 프로파일, port 8090) 2차 QA 세션부터 continuous 기동 상태 유지. CSS/정적 리소스 2차 결과 승계.

---

## 4. 미실행·시각 확인 (사용자 영역, 대기)

1차·2차 QA 와 동일. 변경 없음.

- 실 브라우저에서 꽉찬 프로그램 배너 UX·토글 질감·flashMessage ("대기자 자동 승인을 켰어요/껐어요") 사용자 확인 대기 (감성·시각 영역)
- Testcontainers 통합 테스트는 e2e 프로파일 (H2) 로 등가 커버됨 — Flyway V28 실 적용은 머지 후 Deployment 단계에서 Supabase 환경 검증
- `gap-reports/*.md` 는 untracked 로컬 아티팩트 (매 실행 덮어쓰기)

---

## 5. BLOCKER 현황

| ID | 유형 | 상태 |
|---|---|---|
| ~~B1~~ | ~~회귀 10 TC fail~~ | **해소 ✅** (2차 impl `@MockitoBean WaitlistPromotionService`) |
| ~~B2~~ | ~~E2E spec 폼 탭 전환 누락~~ | **해소 ✅** (2차 impl 전면 재작성) |
| ~~B2'~~ | ~~E2E spec `capacity.fill('0')` 서버 validation reject~~ | **해소 ✅** (3차 impl `fill('1')` + seed29 신청 fixture) |

**현재 BLOCKER: 0건**

---

## 6. 결론 — 머지 가능 판정

### 머지 조건 체크

| 조건 | 상태 |
|---|---|
| 전체 회귀 PASS (761/761) | ✅ (2차 승계) |
| 신규 단위/통합 테스트 PASS (17 TC) | ✅ (2차 승계) |
| 디자인 계약 PASS (45/45) | ✅ (2차 승계, 구현·계약 비변경) |
| 신규 기능 E2E PASS (admin-waitlist-auto) | ✅ (3차 신규 통과) |
| 핵심 화면 regression PASS (admin-program-detail 3 TC) | ✅ |
| CLAUDE.md 인터랙션 검증 조항 (POST+redirect+DOM) | ✅ |
| BLOCKER 0건 | ✅ |

### 판정: **머지 가능** ✅

**잔여 사용자 확인 영역** (머지와 무관, 시각·감성 영역):
- 실 브라우저에서 배너 UX·토글 애니메이션·flashMessage 톤 사용자 확인
- Supabase 환경에서 Flyway V28 실 적용 (Deployment 시점 자동 검증)

### 권장 다음 단계
1. 사용자가 시각·감성 영역 승인 (선택)
2. `/wrap-up` 또는 수동 커밋·PR 생성 → self-PR → squash merge
3. Flyway V28 는 머지 후 다음 부팅 시 Supabase 에 자동 적용 (validate 모드가 schema 일치 확인)

---

## 부록 — 3차 QA 명령 로그

```powershell
# bootRun 생존 확인 (2차에서 continuous 기동)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/
# → 200

# E2E — 재검증 대상 (B2' fix 검증)
cd e2e
BASE_URL=http://localhost:8090 npx playwright test admin-waitlist-auto --project=chromium --reporter=line
# → 1 passed (19.6s)

# E2E — regression 확인 (waitlist 배너 추가가 기존 흐름 미파손)
BASE_URL=http://localhost:8090 npx playwright test admin-program-detail --project=chromium --reporter=line
# → 3 passed (10.6s)
```

**승계된 2차 결과** (impl 변경 파일이 TS spec 1건으로 국한되어 재실행 생략):
- `.\gradlew.bat test` — 761/761 PASS (7m 31s)
- `BASE_URL=http://localhost:8090 npx playwright test --project=contracts` — 45/45 PASS (2.3m, 갭 0건)
