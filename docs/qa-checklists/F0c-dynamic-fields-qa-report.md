# F0c-dynamic-fields QA Report (재재검증 최종)

- 작업 브랜치 · commit: `feature/F0c-dynamic-fields` @ `b1d4d48` (P0-3 fix)
- 이전 반려: `a5c7725` (초회) → `678c799` (1차 재검증) → `b1d4d48` (2차 재검증 = 본 리포트)
- 재재검증 수행: 2026-09-08
- 서버: `.claude/scripts/bootrun-e2e.cmd` 재기동 (fresh H2), profile `e2e`, port 8090
- **최종 판정: PASS ✅** — P0-1 · P0-2 · P0-3 전부 RESOLVED. 회귀 0건. ym-verify 인계 가능

---

## 판정 요약 (재재검증)

| 영역 | 1차 재검증 (678c799) | 2차 재재검증 (b1d4d48) | 비고 |
|---|---|---|---|
| 1. 정적 | ✅ | ✅ | compileJava BUILD SUCCESSFUL |
| 2. 동적 (curl) | ✅ | ✅ | admin 세션 dynamic-fields list = **3 seeded rows** (정확 카운트) |
| 3. 계약 (`--project=contracts`) | ❌ FAIL 2 (24 assertion 중) | ✅ **3/3 spec · 24/24 assertion PASS · 갭 0** | count-min runner 지원 추가 (test-only) |
| 4. 기능 E2E (`--project=chromium`) | ❌ FAIL 4 | ✅ **15/15 PASS** | apply-complete spec dynamic 필드 채움 (test-only) |
| 5. 회귀 apply | ❌ 3건 | ✅ **11/11 PASS** (apply.spec.ts 4/4 · apply-dynamic-response 4/4 · apply-complete 2/2 · login 1/1) | reset-applications FK 회귀 fix |
| 5. 회귀 signup | ✅ | ✅ | 5/5 PASS |
| 5. 회귀 admin 전체 | 43/44 | ✅ **44/44 PASS** | notice · term · A1 · rbac · list · dynamic-field 무회귀 |
| 6. Qn-8 C soft delete | 검증 못함 | ✅ | admin-dynamic-field-form:53 PASS |

**최종 상태**: P0-1 · P0-2 · P0-3 모두 RESOLVED. 회귀 0건. 계약 갭 0. **ym-verify 인계 조건 충족**.

---

## 1. P0 fix 재검증 (실측 원본)

### P0-1 · ApplyAnswer.value SQL 예약어 → **RESOLVED ✅**

**재현 (bootRun 재기동 후 fresh 세션 · admin.jar):**

```
POST /__test__/reset-apply-questions  =  204   (이전 500)
GET  /admin/programs/7/dynamic-fields/3  =  200   (편집 폼, 이전 500)
GET  /admin/programs/7/dynamic-fields/4  =  200
GET  /admin/programs/7/dynamic-fields/5  =  200
```

`ApplyAnswer.java` @Column(name="answer_value") 매핑 + V11 `answer_value TEXT` 컬럼명 rewrite 로 H2(MODE=PostgreSQL) DDL 파싱 오류 해소. Java 필드명 `value` 는 유지되어 도메인 표현력 손실 없음.

### P0-2 · DROPDOWN options JSON literal 파싱 → **RESOLVED ✅**

**재현 (admin 세션 · DROPDOWN 신설):**

```
POST /admin/programs/7/dynamic-fields
  label=RE-DROP-Field  fieldType=DROPDOWN  options=["A","B","C"]
=> 302 Location: /admin/programs/7/dynamic-fields/4
```

**편집 폼 옵션 textarea 정규화 확인:**
```html
<textarea id="options" name="options" rows="6" ...>A
B
C</textarea>
```

**사용자 apply 폼 옵션 렌더 확인 (seed40 · programs/7/apply):**
```html
<select name="dynamicAnswers[4]" class="apply-dynamic-select">
    <option value="">선택해주세요</option>
    <option value="A">A</option>
    <option value="B">B</option>
    <option value="C">C</option>
</select>
```

Jackson TypeReference 파싱 + `IllegalArgumentException("옵션은 JSON 배열 형식이어야 해요...")` fallback 이 정상 작동. 사용자 apply POST 시 whitelist 검증도 통과 (아래 참조).

### Qn-Δ 사용자 신청 POST → **RESOLVED ✅**

**seed40 (fresh 유저) 세션:**

```
POST /programs/7/apply (multipart)
  applyReason=(35자)  privacyAgreed=true
  dynamicAnswers[1]=A  dynamicAnswers[3]=answer-reverify-seed40  dynamicAnswers[4]=B
=> 302 Location: http://localhost:8090/apply/complete?applicationId=55
```

apply_answer INSERT 성공 (P0-1 fix 후 테이블 존재), DROPDOWN whitelist 통과 (P0-2 fix 후 저장 정규화).

### Qn-8 C soft delete → **PASS ✅**

**admin 세션:**
```
POST /admin/programs/7/dynamic-fields/3/deactivate  =  302 Location: .../dynamic-fields
```

**목록 재조회:**
```html
<a ... href="/admin/programs/7/dynamic-fields/3">RE-TEXT-Field</a>
<span class="admin-dynamic-field-status-pill admin-dynamic-field-status-pill--inactive">비활성</span>
```

**사용자 apply 폼 미노출 (seed42 세션):**
```
동적 필드 렌더 카운트:
  1 P0-2-JSON-Literal
  2 RE-ATTACH-Field
  1 RE-DROP-Field
  (RE-TEXT-Field 미노출 — deactivated)
```

### Required 미입력 검증 → **PASS ✅**

seed41 세션 · dynamicAnswers[3] 누락 POST:
```
=> 302 Location: /programs/7/apply
=> flash: <div class="alert alert-error">필수 항목이에요: RE-TEXT-Field</div>
```

---

## 2. 신규 P0-3 · e2e 프로파일 seed apply_question 부재

### 발견 경위

bootRun e2e 재기동 후 admin 세션으로 `/admin/programs/7/dynamic-fields` 접근:

```
GET /admin/programs/7/dynamic-fields  =  200
grep "admin-dynamic-field-row" seed_list.html  → count=1 (헤더만)
grep "필드가 없\|empty"  → "empty state" 렌더
```

즉 seed 프로그램 #7 에 dynamic apply_question 0건.

### 근본 원인

- `V11__apply_questions_and_answers.sql:56~72` — INSERT 3건 (TEXT/DROPDOWN/ATTACHMENT) 를 program #7 에 삽입
- 그러나 e2e 프로파일: `application-e2e.yml` (또는 상응 설정) 에서 Flyway off + H2 `create-drop` 사용 → V11 INSERT 미실행
- `DataInitializer.java` 에 `ApplyQuestion` seed 로직 부재 (grep 결과 apply_question 을 저장하는 코드 없음)
- 결과: e2e 프로파일 부팅 시 apply_question 테이블 empty

### 영향

**계약 검사 FAIL (visual-admin-dynamic-field.spec.ts:10):**
```
Error: [P0] list.rows.seeded — seed program #7 에 3필드 (TEXT · DROPDOWN · ATTACHMENT)
  Expected: "3"
  Received: ""
Error: [P1] list.type-pill.exists — 타입 pill 3종 이상 렌더
  Expected: "3"
  Received: ""
```

**기능 E2E FAIL 3건 (chromium):**
- `apply-complete.spec.ts:32` — program #7 신청 시 dynamicAnswers 미주입 → 400
- `apply-dynamic-response.spec.ts:25` — seed program #7 apply 화면 dynamic 3건 렌더 못 함
- `apply-dynamic-response.spec.ts:37` — dropdown selectOption('농작물 재배') 실패 (seed DROPDOWN 자체 부재)

**admin-dynamic-field-form.spec.ts:53 FAIL (Qn-8 C):**
- 이전 spec 실행 잔재 "임시 필드" 2건 → strict mode 위반. seed 부재로 spec 간 상태 오염 확대

### ym-impl 조치 요구

옵션 A (권장): **`DataInitializer` 에 apply_question seed 3건 추가**
- V11 은 PG 전용, DataInitializer 는 프로파일 무관 → e2e/dev/prod 모두 idempotent 시드
- 기존 규칙 `SEED_APPLY_QUESTION_COUNT=3` 상수와 정합
- `seedApplyQuestions()` 메서드 신설, seedPrograms() 뒤 실행. `if (!applyQuestionRepository.existsByProgramIdAndFieldType(...))` 로 멱등화

옵션 B: **e2e 프로파일에서 Flyway 활성화** — 큰 변경 (H2 방언·검증 로직 재작성 필요) 이라 비추

---

## 3. 회귀 검증

### signup 회귀 → PASS ✅
```
BASE_URL=http://localhost:8090 npx playwright test --project=chromium -g signup
7 passed (27.4s)
```

### admin 회귀 → 43/44 PASS
```
44 tests, 1 failed (admin-dynamic-field-form.spec.ts:53 Qn-8 C, 상세 §2)
```
notice / term / A1 / rbac / list 세트 무회귀. 실패는 seed 부재 파생.

### apply 회귀 → 8/11 PASS
```
apply.spec.ts (4/4 PASS): 자동 채움, privacyAgreed 방어, @Size 방어, 중복 신청 방어
login.spec.ts:50 PASS: /apply 비인증 리다이렉트
apply-dynamic-response.spec.ts (2/4 FAIL): seed 부재 파생
apply-complete.spec.ts:32 FAIL: seed 부재 파생 (Program #7 dynamic 필수)
```

**multipart 전환 영향으로 인한 회귀는 없음** — apply.spec.ts 4건 전부 PASS 로 확인.

### reset 엔드포인트 → PASS ✅
```
POST /__test__/reset-notices          204
POST /__test__/reset-terms            204
POST /__test__/reset-apply-questions  204   (fix 후 회복)
```

---

## 4. 재QA 조건 (ym-impl 인계)

**필수 조치**:
1. `DataInitializer` 에 `seedApplyQuestions()` 추가. seed program #7 에 3필드 (TEXT '지원 동기' required · DROPDOWN '관심 강좌' options `["농작물 재배","유기농 요리","도시양봉"]` required · ATTACHMENT '포트폴리오' optional). id 1~3 확보되도록 seedPrograms 완료 후·첫 실행 시점에 배치
2. (선택) `admin-dynamic-field-form.spec.ts:53` 은 spec 자체를 beforeEach 로 reset 하도록 test-only 수정 필요 (재검증 흐름에서 오염됨). 다만 root cause 는 seed 부재이므로 우선 옵션 A 로 해결

**재QA 통과 기준**:
- bootRun e2e 재기동 후 fresh 상태에서:
  - `/admin/programs/7/dynamic-fields` → 필드 3건 (TEXT/DROPDOWN/ATTACHMENT) 렌더
  - `--project=contracts admin-dynamic-field` → 24/24 assertion PASS
  - `--project=chromium apply-dynamic-response admin-dynamic-field-* apply-complete` → 전부 PASS
  - apply.spec.ts 4건 회귀 PASS 유지
  - signup / admin (term · notice · A1) 무회귀

---

---

## 5. 재재검증 (2026-09-08 · commit b1d4d48) — 최종

### 5-1. P0-3 · e2e seed apply_question 부재 → **RESOLVED ✅**

**서버 fresh 재기동 후 admin (sysadmin@youth-moa.test) 세션 실측:**

```
GET /admin/programs/7/dynamic-fields  =  200
grep 'class="admin-dynamic-field-row"' → 3   (헤더 제외 실 데이터 rows)
```

정확히 3필드 렌더 (`지원 동기` TEXT / `관심 강좌` DROPDOWN / `포트폴리오` ATTACHMENT).

**DROPDOWN 편집 폼 옵션 (`/admin/programs/7/dynamic-fields/2`)**:
```html
<textarea id="options" name="options" rows="6" ...>농작물 재배
유기농 요리
도시양봉</textarea>
```
옵션 3개 (P0-2 회복 유지 재확인).

### 5-2. 계약 검사 (`--project=contracts admin-dynamic-field`)

```
[1/3] visual-admin-dynamic-field.spec.ts:10 (관리자 동적 필드 목록 계약)
[2/3] visual-admin-dynamic-field.spec.ts:22 (신규 폼 계약)
[3/3] visual-admin-dynamic-field.spec.ts:34 (편집 계약)
  3 passed (5.6s)
```

24 assertion **갭 0 재현**. `list.rows.seeded 3/3` + `list.type-pill.exists 3/3` 통과.

**test-only 수정 (스킬 정책상 허용)**: `e2e/contracts/runner.ts` + `types.ts` 에 `count-min` kind 지원 추가. 계약 정의는 이미 `count-min` 을 쓰고 있었으나 runner 가 지원 안 해 항상 fail 하던 정합성 결함이었음 (`Received: ""` = css fallback path). 세 파일(admin-dynamic-field · admin-notice · admin-term)의 기존 count-min 계약이 이 수정으로 함께 정상화.

### 5-3. 기능 E2E (`--project=chromium`)

```
admin-dynamic-field(-form|-rbac|-list) + apply-dynamic-response + apply-complete
  15 passed (30.7s)
```

이전 fail 4건 전부 회복:
- ✅ `apply-complete:32` — 신청 → complete 페이지
- ✅ `apply-dynamic-response:25` — Step 2 에 dynamic 3필드
- ✅ `apply-dynamic-response:37` — dynamic 응답 포함 신청 성공
- ✅ `admin-dynamic-field-form:53` — Qn-8 C soft delete + hard delete 버튼 부재

**test-only 수정 1건 (spec 갱신 · 정책 허용)**: `apply-complete.spec.ts:32` 는 program #7 을 쓰는데 P0-3 fix 후 dynamic 필드가 실체화되어 required 검증에 걸림 (실 flash: "필수 항목이에요: 지원 동기"). spec 에 `dynamicAnswers[1]` (TEXT 지원 동기) + `dynamicAnswers[2]` (DROPDOWN 관심 강좌 = '농작물 재배') 채움 추가. 이 spec 은 완료 페이지 렌더 검증이 목적이므로 dynamic 검증은 `apply-dynamic-response` 가 담당.

### 5-4. 회귀 검증

**apply 계열 (`-g apply`)** — 11/11 PASS:
```
apply.spec.ts        4/4  (자동채움 · privacyAgreed 방어 · @Size · 중복 신청)
apply-dynamic-response 4/4
apply-complete       2/2
login.spec.ts:50     1/1  (/apply 비인증 리다이렉트)
```
multipart 전환 후 apply.spec.ts 4/4 유지 확인.

**signup** — 5/5 PASS (`signup.spec.ts` 총 5건, 이전 리포트의 "7/7" 표기 정정. 필수·정책·중복확인 5시나리오).

**admin 전체 (`-g admin`)** — **44/44 PASS**. dynamic-field · notice · term · A1 · rbac 무회귀. 이전 1건 (admin-dynamic-field-form:53) 회복 포함.

**신규 발견 회귀 → 즉시 fix (test-fixture 결함)**:

- `resetApplications` (test-only endpoint) 가 `apply_answer.application_id` FK 를 미고려하고 `applicationRepository.deleteAllInBatch` 를 직접 호출 → P0-3 fix 로 apply_answer 가 실제 생성되면서 500 DataIntegrityViolationException 노출
- 조치: `TestFixtureController.resetApplications` 에 `DELETE FROM apply_answer WHERE application_id IN (:ids)` 선행 실행 추가. `@Profile("e2e")` scope 이므로 운영 소스 영향 없음
- 서버 재기동 후 이 spec (`apply-complete.spec.ts:68` 존재하지 않는 applicationId 로 접근 시 404) PASS 확인

### 5-5. 정적 검증

```
./gradlew.bat compileJava  →  BUILD SUCCESSFUL in 37s
```

---

## 6. 최종 판정 · ym-verify 인계

**판정: PASS ✅** — F0c-dynamic-fields 커밋 `b1d4d48` 는 6영역 검증 모두 통과.

- ✅ 정적 · 동적 · 계약 (24 assertion 갭 0) · 기능 E2E (15/15) · 회귀 (apply 11/11 · signup 5/5 · admin 44/44) · Qn-8 C soft delete
- ✅ 회귀 0건 (신규 발견 test-fixture 회귀 1건은 본 QA 사이클에서 즉시 fix 후 재검증 PASS)
- ✅ prototype-check 대상 아님 (관리자 스킴)

**test-only 수정 산출물** (본 QA 사이클에서 스킬 정책 범위 내 반영):
1. `e2e/contracts/runner.ts` · `types.ts` — count-min kind 지원
2. `e2e/tests/apply-complete.spec.ts` — dynamic 필드 채우기 추가
3. `src/main/java/.../test/TestFixtureController.java` — resetApplications FK 처리 (@Profile("e2e") scope)

**다음 관문 (ym-verify)**:
> 커밋 `b1d4d48` + 본 QA 리포트 · 위 test-only 수정 3건 기반으로 적대적 검증 진행. spec 구현 매핑 행 단위 재대조 + PASS/FAIL/UNVERIFIED 3단 판정 요망.

---

## Appendix — 실측 원본

- fix 커밋 로그: `git show 7edd13b`
- reverify 로그: `logs/bootrun-reverify.log`
- 재검증 세션 스크래치: `/tmp/f0c-reverify/` (admin.jar · user*.jar · edit_*.html · apply*.html · seed_list.html)
- test-results FAIL 3건: `e2e/test-results/`
  - `visual-admin-dynamic-field-4c255-...-contracts/` — 계약 seed 부재
  - `apply-dynamic-response-...-chromium/` — dropdown selectOption 실패
  - `admin-dynamic-field-form-Q-8ec6b-...-chromium/` — 임시 필드 strict mode 위반
