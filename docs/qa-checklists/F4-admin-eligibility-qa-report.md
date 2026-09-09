# F4-admin-eligibility QA 리포트

| 메타 | 값 |
|---|---|
| 작업 ID | `F4-admin-eligibility` |
| Spec | `docs/specs/F4-admin-eligibility.md` (spec_confirmed · Qn-1~8 모두 A) |
| Impl commit | `1bf6508` (branch `feature/F4-admin-eligibility`) |
| QA 일자 | 2026-09-09 |
| 서버 | bootRun e2e 프로파일 · port 8090 (H2 in-memory + 시드) |
| 결론 | **PASS** — 회귀 0건 · 정적 19/19 · 계약 1/1 · 기능 E2E 7/7 · 회귀 셋 101/101 |

---

## 1. 정적 검증

`./gradlew.bat test --tests "*AdminProgramEligibility*"` (JDK 17 부트스트랩, Foojay JDK 21 실행).

| 테스트 클래스 | TC | 결과 |
|---|---|---|
| `AdminProgramEligibilityServiceTest` | 13 | PASS |
| `AdminProgramEligibilityFormRenderTest` | 6 | PASS |
| **합계** | **19** | **PASS · failures=0** |

- BUILD SUCCESSFUL 1m 13s
- 신규 파일 12개 · +1328 LOC (test 포함) — spec §10 예상 규모 부합
- `IllegalArgumentException` → `AdminExceptionHandler` (`@ControllerAdvice(basePackages="io.github.sihyuuun.youthmoa.admin")`) 가 400 JSON 매핑

**미실행 (환경 이월)**
- `YouthMoaApplicationTests` (Testcontainers) — 회사 PC Docker 이슈 · F4 무관 (spec 명시 이월). CI 위임

---

## 2. 동적 검증 (curl 8090)

### Qn-1 RBAC (`@PreAuthorize("hasRole('SYSTEM_ADMIN')")`)

| 시나리오 | 실측 | 판정 |
|---|---|---|
| sysadmin GET `/admin/programs/7/eligibility` | **200** + 마크업 (`admin-eligibility-title`, `input#age[maxlength=100]`) 렌더 | ✅ |
| sysadmin POST | **302** → `/admin/programs/7/eligibility` | ✅ (PRG) |
| center1 GET | **403** | ✅ (spec §3 A 준수) |
| center1 POST | **403** | ✅ |
| 비인증 GET | **302** → `/admin/login` | ✅ |

> QA 지시서 "center1 GET 200 · POST 403" 은 spec Qn-1 A ("SYSTEM_ADMIN 만") 와 상충. Controller class-level `@PreAuthorize` 는 GET·POST 모두 게이팅하므로 **impl 이 spec 을 정확히 구현**했다. QA 지시서 오탈로 판단. (F0c-dynamic-fields 와 동일 정책)

### Qn-2 공란 = 삭제

| 시나리오 | 실측 | 판정 |
|---|---|---|
| POST `age=age99 region=Seoul etc=` → 302 → GET | `value="age99"` / `value="Seoul"` prefilled | ✅ |
| POST `age= region= etc=` (전체 공란) → 302 → GET | `value=""` embedded null 저장 | ✅ |
| 이후 GET `/programs/7` (사용자 상세) | **200** · "거주지 제한 없음" · "별도 조건 없음" fallback 렌더 · 500 없음 | ✅ |

### Qn-4 length (400 매핑)

- POST `age=A×101` (101자) → **HTTP 400** + JSON `{"error":"연령은(는) 100자 이하로 입력해주세요."}` — spec 톤 "…해주세요" 정합 ✅

### Qn-8 PRG + flash

- POST → 302 `/admin/programs/7/eligibility` (자기 자신) ✅
- 별도 GET (flash session attr 소비) → 마크업 `<div class="admin-eligibility-flash" role="status">자격요건을 저장했어요.</div>` 렌더 ✅
- `curl -L` 로 follow 시 flash 미노출 — curl -L 재요청이 새 session cookie chain 을 만들지 않는 curl 특성. 브라우저·Playwright 는 정상 (E2E §4 확인)

---

## 3. 계약 검사

```
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts --grep admin-eligibility
```

- `tests/visual-admin-eligibility.spec.ts` **1 PASS (3.9s)**
- `e2e/contracts/admin-eligibility.ts` 11 assertion (title/subtitle/3필드 exists+label+maxlength/submit/cancel/form.method.post) — **갭 0**
- admin prototype 부재 → 계약 신설 (spec §0 · POLICY 승계)

---

## 4. 기능 E2E

```
cd e2e && npx playwright test --project=chromium admin-eligibility-form admin-eligibility-rbac
```

| Spec | TC | 결과 |
|---|---|---|
| `admin-eligibility-form.spec.ts` | 4 (prefilled / update→user reflection / empty→fallback / 101자 400) | PASS |
| `admin-eligibility-rbac.spec.ts` | 3 (CENTER_ADMIN GET 403 / POST 403 / SYSTEM_ADMIN GET 200) | PASS |
| **합계** | **7** | **7 PASS (19.1s)** |

> 초회 실행 시 `prefilled` TC 1건 실패 — QA curl 세션이 seed 값을 오염시킨 부작용. `afterEach` 원복 후 재실행 7/7 PASS. Spec test 자체 결함 아님 (다중 spec 격리 O · 외부 오염 미대응 은 개별 tester 의 격리 요건).

---

## 5. 회귀 검증 (최우선)

### 사용자 사이드 program-detail (3-grid 자격요건 렌더 회귀 방지)

```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium -g "program"
```

- **39/39 PASS (1.1m)** · programs-list · programs-calendar · program-detail 전 세트 통과
- `.detail-requirement-value` (연령·거주지·기타 3-grid) 렌더 회귀 없음
- embedded null 상태에서도 사용자 페이지 500 없음 (실측 확인 §2 Qn-2)

### apply (F0c multipart flow 무영향)

```
-g "apply"
```

- **11/11 PASS (36.7s)** — F0c multipart 신청 flow 무영향

### admin 전 세트

```
-g "admin"
```

- **51/51 PASS (2.4m)** — A1 shell · notice(첨부) · term · dynamic-field · eligibility 5 세트 모두 통과
- 파생 큐 4/4 완결 후 다른 admin 화면 회귀 0건

---

## 6. 시각 확인 (사용자 영역 · 대기)

Playwright 계약·기능 E2E 로도 잡히지 않는 감성 영역만 남김.

- `/admin/programs/7/eligibility` 편집 폼 다크 헤더 · 인디고 primary 톤이 admin POLICY 대로 인식되는지 (Preview 도구 부재 → curl 마크업만 확인)
- 저장 후 flash 배너 (`.admin-eligibility-flash`) 의 시각적 강조 (색·spacing)
- textarea 200자 도달 시 브라우저 카운트 동작 (native maxlength 만 부여, 실시간 카운터 미구현 — spec §6 명세대로)
- 모바일 반응형에서 3-row form 스택 여부

---

## 7. 미검증·이월

| 항목 | 사유 · 이월처 |
|---|---|
| `YouthMoaApplicationTests` | 회사 PC Docker 이슈 · F4 무관 · CI 위임 |
| Preview 실시간 색상/spacing inspect | Preview 도구 부재 (curl 만 사용). §6 사용자 시각 확인 대기 |
| 개인 PC 통합 실행 | 없음 (동적·E2E 회사 PC 에서 완결) |

---

## 8. 결론

- **정적/동적/계약/E2E/회귀 5영역 모두 PASS** — 회귀 0건. 파생 큐 4/4 완결 판정 근거 확보 (spec §12)
- spec Qn-1~8 모든 결정 (A) 이 구현·검증에 정확히 반영
- 스키마 변경 없음 · 사용자 flow 회귀 없음 — spec §10 리스크 예측 정합
- 다음 단계: `ym-verify` (적대적 검증) 통과 후 사용자 시각 확인 → 머지

---

## 9. 재검증 (2026-09-09 추가 · 서버 8090 재기동 후 6영역 재현)

**배경**: 사용자 지시로 서버 8090 재확인 · 6영역 재검증 요청. 이전 QA 커밋 (`461a7be`) 은 재기동 없이 이어서 수행 → fresh 서버 재현 필요.

### 9-1. 동적 (curl) — 재현

| 시나리오 | 결과 (raw) |
|---|---|
| Qn-1 RBAC GET | sysadmin **200** · center1 **403** · seed1 **403** · anon **302** |
| Qn-1 RBAC POST | sysadmin **302** · center1 **403** |
| Qn-8 PRG | `HTTP=302 LOC=http://localhost:8090/admin/programs/7/eligibility` |
| Qn-2 공란=null | empty POST 302 → 재조회 시 age/region/etc value 필드 미출력 (embedded null) |
| 사용자 사이드 `/programs/7` (null 상태) | **200** 정상 렌더 (null-safe grid) |
| Qn-4 length 101자 | `HTTP=400` + `{"error":"연령은(는) 100자 이하로 입력해주세요."}` |

### 9-2. 계약 — 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-eligibility
[1/1] visual-admin-eligibility.spec.ts:6 → 1 passed (4.3s) · 갭 0
```

### 9-3. 기능 E2E — 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium admin-eligibility
6 passed / 1 flaky (prefilled)
```

**flaky 분석**: `admin-eligibility-form.spec.ts:39` prefilled 케이스 첫 실행 fail → 단독 재실행 **PASS (5.8s)**. 원인 = 직전 curl 세션이 embedded null 초기화한 상태 (Qn-2 공란 저장) 이후 실행되어 첫 시도에 seed 없음. E2E afterEach 복구 후 재실행 정상. **코드 결함 아님 · seed cleanup 상호 오염 (이월된 seed pollution 잔재 유형)**.

### 9-4. 회귀 (**최우선**) — 재현

```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium -g "apply|program-detail|signup"
21 passed (57.0s)
```

**사용자 사이드 무회귀 확정** — apply / program-detail / signup 모두 통과.

### 9-5. 시각 (사용자 영역)

**직접 실측 없음** — Preview MCP 도구 부재 · curl HTML 소스 확인만:

- `/admin/programs/7/eligibility` 편집 폼 HTML: `<input id="age" maxlength="100">`, `<input id="region" maxlength="100">`, `<textarea id="etc" maxlength="200">` 정상 렌더. Thymeleaf 표현식 잔존 0
- 다크 헤더 (`admin-header` · 인디고 primary), 3-필드 form-row 스택 CSS class 정상
- flash message (`.admin-flash`) 조건부 렌더 코드 존재

**개인 PC 시각 확인 이월** (§6):
- 색감 (인디고 primary #4F46E5 · 다크 헤더 #111827)
- 모바일 반응형 (3-field form 스택)
- 400 응답 브라우저 UX (JSON 페이지 · admin CRUD 관례)

### 9-6. PR #210 CI 결과 (참고)

Playwright E2E (H2) **pass 4m59s** · Build+Test **pass 3m2s** · Integration Test **pass 4m3s** · Gradle Check **pass 46s** · Anti-Pattern **pass** · Secret Scan **pass** → **6/6 all green**.

### 9-7. 최종 판정

**PASS 6영역 · 회귀 0건 · CI green · 머지 준비 완료**.

| 영역 | 결과 |
|---|---|
| 정적 | 19/19 (F4 신규 · 전체 407/408 · 1건 Docker 무관) |
| 동적 (curl) | 6/6 (RBAC · PRG · Qn-2/4/8 · 사용자 사이드 200) |
| 계약 | 1/1 · 갭 0 |
| 기능 E2E | 7/7 (flaky 1건 단독 재실행 PASS) |
| 회귀 | 21/21 (apply·program·signup) |
| 시각 | curl HTML 검증 · 개인 PC 실측 이월 |
