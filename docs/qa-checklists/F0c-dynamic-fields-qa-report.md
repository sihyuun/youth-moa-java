# F0c-dynamic-fields QA Report (재검증 갱신)

- 작업 브랜치 · commit: `feature/F0c-dynamic-fields` @ `7edd13b` (P0 fix)
- 이전 반려: `a5c7725` (2026-09-08 초회 QA)
- 재검증 수행: 2026-09-08
- 서버: `.claude/scripts/bootrun-e2e.cmd` 재기동 (PID 3872 → 재기동), profile `e2e`, port 8090
- 판정: **REJECT (재반려)** — P0 2건은 완전 회복. 신규 P0 결함 1건 발견 (e2e 프로파일 seed 부재)

---

## 판정 요약

| 영역 | 이전 QA | 재검증 | 비고 |
|---|---|---|---|
| 1. 정적 | ✅ | ✅ | compileJava · AdminApplyQuestionServiceTest 16/16 · JpaMappingTest 3/3 |
| 2. 동적 (curl) | ❌ 500 3건 | ✅ | P0-1 / P0-2 fix 확인 (실측 원본 아래) |
| 3. 계약 (`--project=contracts`) | 미실행 | ❌ FAIL 1건 | **신규 결함**: seed 3필드 부재 → list.rows.seeded 0/3 |
| 4. 기능 E2E (`--project=chromium`) | 미실행 | ❌ FAIL 3건 | seed 3필드 부재 파생 |
| 5. 회귀 | reset-apply 500 | ✅ 부분 · ❌ apply 회귀 3건 | signup 7/7 PASS, admin 43/44 (1 FAIL) |
| 6. 시각 | - | 미진행 | 재반려로 보류 |

**Blocking issue (신규 P0-3)**: e2e 프로파일에서 seed 프로그램 #7 에 dynamic apply_question 3필드 (TEXT · DROPDOWN · ATTACHMENT) 가 실체 없음. V11 Flyway 마이그레이션이 seed 를 담당하지만 e2e 프로파일은 Flyway off (H2 create-drop). DataInitializer 에도 apply_question 시드 로직 없음. 결과: 계약·기능·apply flow 전반 fail.

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

## Appendix — 실측 원본

- fix 커밋 로그: `git show 7edd13b`
- reverify 로그: `logs/bootrun-reverify.log`
- 재검증 세션 스크래치: `/tmp/f0c-reverify/` (admin.jar · user*.jar · edit_*.html · apply*.html · seed_list.html)
- test-results FAIL 3건: `e2e/test-results/`
  - `visual-admin-dynamic-field-4c255-...-contracts/` — 계약 seed 부재
  - `apply-dynamic-response-...-chromium/` — dropdown selectOption 실패
  - `admin-dynamic-field-form-Q-8ec6b-...-chromium/` — 임시 필드 strict mode 위반
