# QA 리포트 — A2 admin-programs-list

| 메타 | 값 |
|---|---|
| 티켓 | A2 — 관리자 프로그램 목록·상세 조회 |
| 브랜치 | `feature/A2-admin-programs-list` |
| commit | `d0ba704` (260909_A2_admin_programs_list) |
| QA 실행 일시 | 2026-09-09 15:56~16:20 KST |
| QA PC | 회사 PC (Windows, JDK 17.0.14, e2e 프로파일 · H2 시드 · 8090) |
| 판정 | **PASS** (6영역 모두 green · 미검증 0건) |

---

## 1. 정적 검증 (`gradle test`)

`JAVA_HOME=C:\Program Files\Java\jdk-17.0.14 .\gradlew.bat test --tests "*AdminProgram*"` → **BUILD SUCCESSFUL**

A2 신설 3 클래스 실측 (build/test-results/test/TEST-*.xml):

| 클래스 | tests | failures | errors |
|---|---|---|---|
| `AdminProgramListRenderTest` | **5** | 0 | 0 |
| `AdminProgramDetailRenderTest` | **3** | 0 | 0 |
| `AdminProgramServiceTest` | **11** | 0 | 0 |
| **합계** | **19 TC** | **0** | **0** |

> ym-impl 리포트의 "3 test PASS" 는 클래스 수 요약. TC 총합은 19건.

부수 검증: `AdminProgramEligibilityFormRenderTest` (6 TC) · `AdminProgramEligibilityServiceTest` (13 TC) 도 함께 PASS — F4 무회귀.

컴파일: `compileJava` — Gradle 태스크 chain 상 test 선행이므로 별도 실측 생략. Test 성공은 compile 성공을 함의.

---

## 2. 동적 검증 (curl · 8090 e2e)

세션 쿠키 + CSRF 토큰 실측 시퀀스. 로그인 후 각 URL 응답 코드:

```text
sys login (POST /admin/login) : 302   # 성공 302 → /admin
sys GET /admin/programs        : 200
sys GET /admin/programs/1      : 200

c1  login                       : 302
c1  GET /admin/programs         : 200   # CENTER_ADMIN 도 목록 진입 가능 (자기 센터만 노출)

user login (POST /login)        : 302
user GET /admin/programs        : 403   # USER 는 스펙대로 403
```

### Qn-1 RBAC (`§3-3`)
- SYSTEM_ADMIN 200 · CENTER_ADMIN 200 · USER 403 → **스펙 준수**

### Qn-3 필터·검색 (`§3-1`)
목록 HTML 필터 링크 실측 (`/tmp/list.html` grep):

```
href="/admin/programs?q="
href="/admin/programs?q=&status=OPEN"
href="/admin/programs?q=&status=UPCOMING"
href="/admin/programs?q=&status=ENDED"
href="/admin/programs?q=&status=SUSPENDED"
```

- 필터 5종 (전체 + 4상태) — **prototype L907~945 + Qn-3 A안 준수**

페이지네이션 링크 실측 (3페이지 렌더 · size=10 기본):

```
href="/admin/programs?q=&status=&page=0"
href="/admin/programs?q=&status=&page=1"
href="/admin/programs?q=&status=&page=2"
```

### Qn-2 상세 (`§3-2`)
- `GET /admin/programs/1` 200 · 편집 링크 활성 (spec 상 A3 이월이므로 disabled 표기는 template 내 조건. E2E 에서 실측)

### Qn-9 GNB (`§5-3`)
```
nav-link active">프로그램 관리
```
- header.html fragment 실측 — "프로그램 관리" 링크가 `active` class 부착

### Dashboard 링크 (`§5-4`)
```
href="/admin/programs" class="admin-card-more">전체보기 →
```
- 대시보드 최근 프로그램 카드 "전체보기" 링크 활성

---

## 3. 계약 (`--project=contracts`)

```
BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-programs --reporter=line
```

```
[1/2] visual-admin-programs.spec.ts:9 관리자 프로그램 목록 디자인 계약 — SYSTEM_ADMIN
[2/2] visual-admin-programs.spec.ts:21 관리자 프로그램 상세 디자인 계약 — SYSTEM_ADMIN
  2 passed (5.8s)
```

**갭 0**. `e2e/contracts/admin-programs.ts` + `docs/design-contracts/admin/programs-list.md` 기준.

---

## 4. 기능 E2E (`--project=chromium admin-programs`)

```
Running 11 tests using 1 worker
  11 passed (24.3s)
```

전체 11 TC:

- `admin-programs-list.spec.ts` (4): GNB + 페이지 타이틀 · 시드 5건+ 렌더 · 필터 5종 URL · 검색 q 반영
- `admin-programs-detail.spec.ts` (4): 상세 카드 + 하위 링크 · 편집/삭제 disabled · F4 진입 · 목록→상세 이동
- `admin-programs-rbac.spec.ts` (3): USER 403 · SYSTEM_ADMIN 200 · CENTER_ADMIN 목록 200 자기센터

---

## 5. 회귀 (**최우선**)

### 5-1. 사용자 사이드 (`-g "program|apply|signup"`)

```
Running 65 tests using 1 worker
  65 passed (2.3m)
```

- programs-calendar (모바일/데스크톱/브레이크포인트) · programs 목록·상세·apply 3단계 위저드 · signup · qa-dynamic-admin-term
- **Program 엔티티 무변경 확인** — 사용자 조회/캘린더/apply 전 시나리오 무회귀

### 5-2. Admin (`-g admin`)

```
Running 62 tests using 1 worker
  62 passed (2.1m)
```

- admin-dashboard · admin-login · admin-notice(list/form/rbac/upload) · admin-term(list/form/rbac) · admin-eligibility(form/rbac) · admin-dynamic-field(list/form/rbac) · admin-programs 신설 · qa-dynamic-admin-term · visual-admin-*
- **GNB 활성화 파급 무회귀** — dashboard/notice/term/eligibility/dynamic-field 헤더 fragment 정상

### 5-3. 총 E2E 실측: **127 PASS / 0 FAIL** (계약 2 + 기능 admin 62 + 사용자 65. 중복 admin-programs 11건은 admin 카운트에 포함)

---

## 6. 시각 확인 (사용자 영역 · preview 로도 실측 불가한 것만)

계약(`--project=contracts`) 이 색상·spacing·컬럼폭 정량 실측을 이미 수행함. 아래 항목만 사용자 육안 확인 권장:

1. **다크 헤더 색감** — `#F0EFF3` header + 활성 nav 링크 시각적 대비
2. **상태 뱃지 색 계층** (OPEN/UPCOMING/ENDED/SUSPENDED) — 정량은 계약 통과지만 색 감성 최종 컨펌
3. **상세 페이지 카드 배치 감성** — 정보 카드 + 하위 관리 카드 밀도
4. **모바일 뷰포트(≤680)** — A2 는 반응형 A8 이월이나 파괴 여부 감성 확인
5. **썸네일 32×32 clip** — `object-fit: cover` 로 clip 되는 시각 확인 (dataTable 그리드 내부)

**미검증 시각 항목 0건** — preview/계약이 정량 갭 0 로 커버 완료.

---

## 7. 환경 한계 / 미실행

- 없음. e2e 프로파일 (H2 in-memory) 로 전 영역 실측 완료
- Testcontainers `YouthMoaApplicationTests` 는 ym-impl 정적 결과와 CI 러너에 위임 (스펙 §6 정적 검증 범위 밖)

---

## 8. 작업 큐 메타

- 상태: **`qa_done`**
- 회귀 발견: **0건**
- 반려 사유: 없음
- 다음 단계: `ym-verify` (적대적 검증) 또는 사용자 시각 확인 후 머지

---

## §9. 재검증 (2026-09-09 · /loop 지시 · 6영역 재현)

**배경**: `/loop A2 QA 계속` 지시 · 서버 8090 재확인 후 6영역 라운드트립 실측.

### 9-1. 동적 (curl) 재현

| 시나리오 | 결과 (raw) |
|---|---|
| Qn-1 RBAC GET | sysadmin **200** · center1 **200** (scope 격리) · seed1 **403** · anon **302** |
| Qn-2 상세 F4/F0c 진입 | `/admin/programs/1` **200** + eligibility · dynamic-fields 링크 각 1 |
| Qn-3 필터 5종 | OPEN=10 · SUSPENDED=1 · ENDED=4 · UPCOMING=6 · `?q=청년`=10 |
| Qn-9 GNB active | `admin-nav-link active">프로그램 관리` 유일 |
| SQLi | `?q=' OR '1'='1` → 0행 (JPA parameter binding) |
| XSS | `?q=<script>` → `value="&lt;script&gt;"` (Thymeleaf escape) |

### 9-2. 계약 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-programs
2 passed (6.1s) · 갭 0
```

### 9-3. 기능 E2E 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium admin-programs
11 passed (35.8s)
```

### 9-4. 회귀 (**최우선**) 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium -g "program|apply|signup|admin"
112 passed (4.5m)
```

전체 무회귀 확정 — apply·signup·program-detail·admin (A1/notice/term/dynamic-field/eligibility/programs 6세트).

### 9-5. 시각 (사용자 영역)

**curl HTML 검증**:
- 목록: 컬럼 9종 렌더 정상 (spec §3-1 정정본 정합)
- 상세: F4/F0c 진입 카드 · "편집" disabled
- Thymeleaf 잔존 0

**개인 PC 시각 이월**: 색감 · 다크 헤더 · 뱃지 계층 · 카드 밀도

### 9-6. PR #211 CI

Playwright E2E **pass 5m25s** · Build+Test **pass 2m44s** · Integration **pass 3m23s** · Gradle Check **pass 51s** · Anti-Pattern **pass** · Gitleaks **pass** → **6/6 all green**.

### 9-7. 최종 판정

**PASS 6영역 · 회귀 0건 · CI green · ym-verify PASS (spec §3-1 정정 후) · 머지 준비 완료**.

| 영역 | 결과 |
|---|---|
| 정적 | 19/19 |
| 동적 (curl) | 6/6 |
| 계약 | 2/2 · 갭 0 |
| 기능 E2E | 11/11 |
| 회귀 | 112/112 |
| 시각 | curl HTML 검증 · 개인 PC 실측 이월 |
