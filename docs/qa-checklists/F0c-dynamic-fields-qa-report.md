# F0c-dynamic-fields QA Report

- 작업 브랜치 · commit: `feature/F0c-dynamic-fields` @ `792687c`
- 명세: [docs/specs/F0c-dynamic-fields.md](../specs/F0c-dynamic-fields.md) (spec_confirmed)
- QA 수행: 2026-09-08
- 서버: `.claude/scripts/bootrun-e2e.cmd` 재기동 (PID 33468, 2026-09-08 13:01:08 KST 시작), profile `e2e`, port 8090
- 판정: **REJECT (반려)** — 프로덕션 결함 3건 (전부 동일 근본 원인)

---

## 판정 요약

| 영역 | 결과 |
|---|---|
| 1. 동적 (curl) | **FAIL** — Qn-3/4 편집 폼 500, Qn-4 DROPDOWN 옵션 파싱 결함, Qn-7 reset 500, Qn-Δ apply POST 실질 실패 |
| 2. 계약 (Playwright `--project=contracts`) | **미실행** — 편집 폼 500 이라 실행해도 갭 확정 발생. 프로덕션 fix 후 재검증 |
| 3. 기능 E2E (`--project=chromium`) | **미실행** — 동일 사유 |
| 4. 회귀 (apply / signup / admin / reset) | **부분 실행** — reset-notices / reset-terms 무회귀 ✅ · reset-apply-questions **FAIL** |
| 5. 시각 (사용자 영역) | 미진행 (반려로 시각 확인 요청 보류) |
| 6. RBAC | **PASS** — sysadmin / center1 / USER 정합 확인 |

**Blocking issue**: `ApplyAnswer.value` 컬럼 `columnDefinition = "TEXT"` 이 H2 (MODE=PostgreSQL) 스키마 생성 시 예약어 `VALUE` 로 파싱되어 `apply_answer` 테이블 자체가 생성되지 않음. 그 결과 (a) reset 500, (b) 편집 폼 500 (`countByQuestionId` 쿼리), (c) apply POST 는 302 리다이렉트만 반환하고 실 INSERT 는 fail (실측한 다음 시나리오 참조).

---

## 1. 동적 검증 (curl · port 8090)

### Qn-1 RBAC ✅ (PASS)

3개 세션 확보 후 실측:

```
== sysadmin ==
GET /admin/programs/7/dynamic-fields          = 200
GET /admin/programs/7/dynamic-fields/new      = 200

== center1 ==
GET /admin/programs/7/dynamic-fields          = 403
POST /admin/programs/7/dynamic-fields         = 403

== USER (seed1) ==
GET /admin/programs/7/dynamic-fields          = 403
```

`@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 정확히 작동. center1 은 CENTER_ADMIN 인데 dynamic-fields 는 SYSTEM_ADMIN 전용으로 정합. 스펙 Qn-1 A 부합.

### Qn-3/4 필드 타입 CRUD

**Create** (302 redirect to edit form):
```
POST label=QA-TEXT-Field   fieldType=TEXT       → 302 Location: /admin/programs/7/dynamic-fields/1
POST label=QA-DROP-Field   fieldType=DROPDOWN   → 302
POST label=QA-ATTACH-Field fieldType=ATTACHMENT → 302
```
3건 모두 목록 페이지에 정상 노출됨:
```
$ grep -oE 'QA-(TEXT|DROP|ATTACH)-Field' dyn_list.html | sort | uniq -c
      1 QA-ATTACH-Field
      1 QA-DROP-Field
      1 QA-TEXT-Field
```

**Edit** — **FAIL**:
```
GET /admin/programs/7/dynamic-fields/1 = 500 Internal Server Error
```
응답 body:
```
"message":"Could not prepare statement [Table \"APPLY_ANSWER\" not found; SQL statement:
select count(aa1_0.id) from apply_answer aa1_0 where aa1_0.question_id=? [42102-240]"
```

`AdminApplyQuestionController.editForm()` 에서 사용 응답 건수를 계산하려고 `applyAnswerRepository.countByQuestionId()` 호출 → `apply_answer` 테이블 미존재로 500.

**DROPDOWN 옵션 저장·복원** — **FAIL (별건 결함)**:
사용자 apply 폼에서 옵션이 잘못 파싱되어 렌더링됨.

입력한 options: `["A","B","C"]` (JSON array literal)

실 apply 페이지 HTML 실측:
```html
<select name="dynamicAnswers[2]" class="apply-dynamic-select">
    <option value="">선택해주세요</option>
    <option value="[&quot;A&quot;">[&quot;A&quot;</option>
    <option value="&quot;B&quot;">&quot;B&quot;</option>
    <option value="&quot;C&quot;]">&quot;C&quot;]</option>
</select>
```

= JSON 배열을 단순 콤마 split 로 파싱해서 브래킷·따옴표까지 옵션 문자열 자체에 포함됨. JSON parse 로직 부재. 사용자는 정상 옵션 하나도 선택 불가능.

### Qn-Δ 사용자 신청 폼 통합

**Step 2 렌더** — ✅ PASS (읽기 SELECT 만 사용하는 경로):
```
GET /programs/7/apply (seed1 로그인)  = 200
동적 필드 렌더 카운트:
  1 QA-ATTACH-Field
  1 QA-DROP-Field
  1 QA-TEXT-Field
```
필드 name 속성:
```
name="dynamicAnswers[1]"       (TEXT)
name="dynamicAnswers[2]"       (DROPDOWN)
name="dynamicAttachments_3"    (ATTACHMENT)
```

**POST** — FAIL (multipart, 정상 값 주입):
```
POST /programs/7/apply (multipart)
  applyReason, privacyAgreed=true, dynamicAnswers[1]=answer-text-1, dynamicAnswers[2]=A
→ 302 Location: /programs/7/apply    ← complete 로 안 가고 apply 로 돌아옴
```
apply 페이지에 남은 에러:
```
alert alert-error: QA-DROP-Field 항목은 허용되지 않은 옵션이에요: A
```
= DROPDOWN 옵션이 `[\"A\"` `\"B\"` `\"C\"]` 로 저장돼 있어서 사용자가 어떤 값을 골라도 whitelist 검증 실패.

Required 필드 미입력 시 안내 메시지는 폼 자체가 필수 표시 (`✓`) + `is-required` CSS class 로 표기됨을 확인 (`grep -c "is-required" apply7.html` = 5).

### Qn-10 회귀 (프로그램 3 = 동적 필드 없음) ✅ PASS
```
GET /programs/3/apply (seed1) = 200
```
multipart 전환 후에도 정상 렌더.

### Qn-7 reset — **FAIL**
```
POST /__test__/reset-apply-questions = 500
```
Body:
```
"message":"Could not prepare statement [Table \"APPLY_ANSWER\" not found; SQL statement:
DELETE FROM apply_answer WHERE question_id IN (SELECT id FROM apply_question WHERE id > ?)"
```
CSRF 예외 처리 정상 (SecurityConfig 확인) — 순수 스키마 결함.

### Qn-8 C soft delete
- 편집 폼 500 이라 deactivate 액션 도달 불가 → **미검증**

---

## 근본 원인 (모든 500 의 공통 원인)

`src/main/java/io/github/sihyuuun/youthmoa/application/ApplyAnswer.java:49-50`:
```java
@Column(columnDefinition = "TEXT")
private String value;
```

부팅 시 스키마 생성 로그 실측 (`logs/bootrun-qa.log:87~168`):
```
WARN GenerationTarget encountered exception accepting command : Error executing DDL "
    create table apply_answer (
        ...
        value TEXT,       ← 여기서 파싱 실패
        primary key (id)
    )" via JDBC [Syntax error in SQL statement "
    ... [*]value TEXT ...
    "; expected "identifier";]
```

H2 (v2.4.240, MODE=PostgreSQL) 파서는 `VALUE` 를 예약 키워드 (`VALUES` 축약 함수) 로 인식하기 때문에 컬럼명 위치의 bare `value` 를 identifier 로 못 받음. 결과적으로 `apply_answer` 테이블 자체가 생성되지 않고, 이 테이블에 의존하는 SELECT·DELETE·INSERT 전부 42102 (Table not found).

**e2e 프로파일 (H2 MODE=PostgreSQL) 에서 F0c 의 write 경로 전체가 fail** = CI Playwright 사이클도 반드시 fail 이 남.

### 프로덕션 결함 판정 근거
- 스펙 상 e2e 프로파일이 CI Playwright 검증의 게이트 (`docs/specs/F0c-dynamic-fields.md` Qn-Δ · Qn-11 · CI 조건 포함)
- 프로덕션 코드 `ApplyAnswer.java` 가 H2 뿐 아니라 다른 SQL 엔진에서도 잠재 결함 (Postgres 는 unquoted `value` 도 예약어라 컬럼명으로 쓰기 위험 — 관례상 quote 필요)
- ym-qa 는 test-only 수정만 허용 → 반려하여 ym-impl 이 프로덕션 수정 필요

---

## 회귀 검증 결과

### reset 엔드포인트 무회귀
| 엔드포인트 | 결과 |
|---|---|
| POST /__test__/reset-notices | ✅ 204 |
| POST /__test__/reset-terms | ✅ 204 |
| POST /__test__/reset-apply-questions | ❌ 500 (근본 원인 상동) |

### apply 회귀 (multipart 전환 영향)
- GET /programs/7/apply = 200, GET /programs/3/apply = 200 → 렌더 회귀 없음 ✅
- POST apply flow — 프로그램 #7 은 위 결함으로 실측 불가. 프로그램 #3 (동적 필드 없음) 은 프로덕션 결함 fix 후 재실측 필요

### 신규 spec 회귀 방어
- `apply.spec.ts` (4 케이스): 프로덕션 결함으로 실행 보류
- `admin-dynamic-field-list.spec.ts`, `-form.spec.ts`, `-rbac.spec.ts`: 편집 폼 500 으로 상당수 실행 시 실패 예상 → 프로덕션 fix 후 재검증
- `apply-dynamic-response.spec.ts`: DROPDOWN 옵션 결함 + insert fail 로 통과 불가 → 프로덕션 fix 후 재검증

---

## 반려 사유 요약 (ym-impl 인계)

**필수 조치 (프로덕션 코드)**:

1. `ApplyAnswer.value` 컬럼명 예약어 회피 — 두 가지 옵션:
   - (선호) `columnDefinition = "TEXT"` 제거 후 `@Column(name = "answer_value", ...)` 로 컬럼명 자체를 예약어 회피. H2 · Postgres · MySQL 전부에서 안전
   - (차선) `columnDefinition = "TEXT"` 유지하되 컬럼명을 `"\"value\""` 로 quote — Postgres·H2 는 인용되면 예약어여도 사용 가능. 다만 케이스 sensitivity 이슈 발생 가능
   - Java 필드명 `value` 는 유지 가능 (`@Column(name = ...)` 로 매핑만 변경)
2. DROPDOWN 옵션 JSON 파싱 로직 도입:
   - 저장: `options` (JSON array literal `["A","B","C"]`) 를 파싱해서 `List<String>` 로 검증 후 저장 문자열도 정규화
   - 로드/렌더: `apply.html` 렌더 시 콤마 split 이 아니라 JSON 배열 파싱 → `<option value="A">A</option>` 형태로 렌더
   - 파일: `templates/apply.html` (dropdown 렌더 fragment) + `AdminApplyQuestionService` (파싱·검증) 두 지점 다 확인 필요

**재QA 조건**: 위 2건 fix 후 아래 전부 통과되어야 승인:
- `/admin/programs/7/dynamic-fields/{id}` (edit) 200
- `/programs/7/apply` POST → `/programs/7/apply/complete` 302
- `POST /__test__/reset-apply-questions` 204
- `--project=contracts` admin-dynamic-field 3화면 갭 0
- `--project=chromium` apply-dynamic-response + admin-dynamic-field-* 전부 PASS
- 기존 `apply.spec.ts` 4 케이스 회귀 통과

---

## Appendix — 실측 원본

- 서버 기동 로그: `logs/bootrun-qa.log` (line 87~168 = DDL 파싱 fail 원본 · line 3630~3635 = 첫 reset 500 · line 6000~6045 = edit form 500)
- 이전 서버 프로세스 종료: 2026-09-07 13:21 기동 PID 34228 (F0c 커밋 이전) 강제 종료 후 신규 기동 (PID 33468 · 2026-09-08 13:01)
- 계약 파일: `e2e/contracts/admin-dynamic-field.ts` — 24 assertion (list 9 · new form 8 · edit 7)
