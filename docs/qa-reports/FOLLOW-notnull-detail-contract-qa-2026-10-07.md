# QA 리포트 — FOLLOW XS 2건 (notnull-valid + detail-contract)

- 브랜치: `feature/FOLLOW-notnull-detail-contract` (7f20201 위 미커밋)
- 검증일: 2026-10-07
- 검증 담당: ym-qa
- 결과: **GO (커밋 가능)**

---

## 1. 옵션 B 선택 사유 재검증 — PASS

**@Valid 패턴 전수 결과**: `src/main/java` 전체 9건 매치. **admin 패키지는 0건** (ProgramFormRequest.java:47 주석 1건만 매치 — "`@Valid 를 쓰지 않으며`" 문구). 외 8건은 application/user Controller 소관으로 admin 트랙과 격리됨.

- `admin-notice`/`admin-term`/`admin-user` Controller 전수 확인 결과 `@Valid` 사용 없음 — ym-impl 보고 "admin 13개 Controller 전수 0건" 사실 재확인
- `ProgramFormRequest.java` L15~18 javadoc + L46~48 데드코드 사유 주석 **일관성 확보**
- 옵션 B(수동 validate) 선택이 admin 트랙 전역 패턴과 **100% 일치**. "admin-notice/term 패턴 승계"라는 명분은 실체 있음

**판정**: 커뮤니티 best practice 와 상충하긴 해도 **프로젝트 내부 일관성**을 우선한 결정으로 명확히 정당화됨. 추후 이 결정이 바뀔 때는 admin 트랙 **일괄 전환** (13개 Controller + ProgramFormRequest 리팩토링) 이 전제.

---

## 2. @NotNull 제거 사이드이펙트 — PASS

- `jakarta.validation` import 제거 확인 (grep 결과 admin 패키지에서 `jakarta.validation` 참조 0건, `@NotNull`/`@NotBlank`/`@Valid` 모두 0건)
- `ProgramFormRequest` 참조 전수 (12 테스트 + 3 src 파일) 중 **애너테이션 존재 전제 코드 없음** — 전부 setter/getter 로 필드 접근
- `AdminProgramFormServiceTest.create_신청_기간_누락_400` (L103~109) 가 `setApplyStartDate(null)` 직접 주입 → service.create() → `IllegalArgumentException("신청 기간을 입력해주세요.")` 검증. **수동 validate() L388~390 커버됨**
- 회귀 테스트 `AdminProgramFormRenderTest` (L60~61) 가 폼 렌더에 `name="applyStartDate"`/`name="applyEndDate"` 존재만 체크 — 애너테이션과 독립

**판정**: 제거 안전.

---

## 3. design-contract §5 서술 품질 — PASS

`docs/design-contracts/program-detail.md:115` 수정 내용:

> 신청 기간 값 포맷 (L995) — prototype `2026-07-01 ~ 07-31`, 구현 `YYYY.MM.DD ~ YYYY.MM.DD` (양측 노출). **D5-Q1d (2026-10-06) 로 `applyStartDate` 가 NOT NULL 승격되면서 "~ YYYY.MM.DD 까지" (마감 단독) 폴백 포맷을 제거하고 양측 날짜 노출로 확정**. 구분자 `.` 유지 (prototype 의 `-` 와는 정책상 다름 — 전 화면 날짜 표기 공통 토큰). 기계 계약 `e2e/contracts/program-detail.ts` `meta.apply.period.text` 로 자동 검사됨

- D5-Q1d 변경 근거 (NOT NULL 승격 → 폴백 제거) 명확 서술 ✓
- 이전/신규 포맷 명시 ✓
- 기계 계약 참조 링크 ✓
- prototype `-` vs 구현 `.` 구분자 결정 재확인 (POLICY 수준 결정 명시) ✓

**판정**: 서술 완결성 OK.

---

## 4. contract text-match selector 정확성 — PASS

**selector**: `.detail-meta-item:first-of-type .detail-meta-value`

- `templates/program/detail.html:70~78` — 최초 `.detail-meta-item` 이 **신청 기간** 셀 (`<div class="detail-meta-key">신청 기간</div>` + `<div class="detail-meta-value">`)
- 진행 기간 (L80~89) 은 `:nth-of-type(2)` 이자 `th:if="${program.startDate != null and program.endDate != null}"` 조건부. **first-of-type 과 겹칠 수 없음**
- `runner.ts` L65~67, L90~95 — text-match 는 innerText + RegExp.test() 로 판정. **구현 OK**
- `#temporals.format(...)` 결과 "2026.09.23 ~ 2026.10.09" → 정규식 `^\d{4}\.\d{2}\.\d{2}\s*~\s*\d{4}\.\d{2}\.\d{2}$` **매치됨** (아래 동적 검증 결과로 실측 확인)

**판정**: selector 정확, 정규식 적합.

---

## 5. 정적 재검증 — PASS

```
.\gradlew.bat clean test   →   BUILD SUCCESSFUL in 17m 31s
PASSED: 746  /  FAILED: 0  /  SKIPPED: 0
```

- compileJava SUCCESS (deprecated API 경고 `AdminUserSafeguard` 1건은 기존 상태 유지)
- Testcontainers 통합 테스트 (`YouthMoaApplicationTests.contextLoads()`) PASSED
- ProgramFormRequest 관련 단위 테스트 8+ 전부 PASS (`AdminProgramFormServiceTest`, `AdminProgramCourseUpsertTest`, `AdminProgramAttachmentServiceTest`, `AdminProgramImageServiceTest`)
- `create_신청_기간_누락_400` 포함 역전 케이스 PASS

**판정**: 회귀 0.

---

## 6. 동적 재검증 — PASS

bootRun e2e 프로파일 포트 8090 기동 (약 70초 소요)

### 6-1. 신청기간 포맷 실측

```bash
GET http://localhost:8090/programs/1  →  200 OK
```

추출된 셀 값: **`2026.09.23 ~ 2026.10.09`**

- 포맷: `YYYY.MM.DD ~ YYYY.MM.DD` ✓ (contract 정규식 매치)
- "~ YYYY.MM.DD 까지" 폴백 포맷 사라짐 재확인

### 6-2. Playwright `--project=contracts` (45/45)

```
45 passed (4.8m)
```

- `visual-program-detail.spec.ts` 포함 전 계약 통과
- `meta.apply.period.text` 신규 체크 통과 (program-detail.ts L348~355)
- 기존 program-detail 101개 체크 모두 유지 (갭 0)

### 6-3. POST /admin/programs — 403

로그인 세션 없이 호출 → **403 (Spring Security CSRF/인증 거부)**. 서비스 레이어 null 가드까지 도달 안 함. **대안**: 단위 테스트 `AdminProgramFormServiceTest.create_신청_기간_누락_400` 가 service.create(null) 직접 호출로 400 매핑 메시지("신청 기간을 입력해주세요.") 검증 완료. 동적 400 재현은 세션 확보 필요한데, 수동 검증 로직은 테스트가 커버하므로 **커밋 blocker 아님**.

### 6-4. bootRun 종료 확인

PID 114836/106012 강제 종료 → 8090 LISTENING 사라짐. TIME_WAIT 소켓만 잔존 (정상).

---

## 7. 담합 재공격 — PASS

| grep 패턴 | 결과 | 판정 |
|---|---|---|
| `신청\s?기간을?\s?입력` | `AdminProgramService.java:389` 1건 (`throw new IllegalArgumentException("신청 기간을 입력해주세요.");`) | 메시지 유일 소스 유지 — null 가드 담당 |
| `까지` in `templates/program/` | **0 매치** | "~ YYYY.MM.DD 까지" 폴백 포맷 완전 제거 확인 |
| `@NotNull`/`@Valid` in admin | **0 매치** (주석 포함 1건은 사유 설명) | 패턴 일관성 유지 |
| `format.*apply` in `templates/program/` | `detail.html:77` 1건 (`YYYY.MM.DD ~ YYYY.MM.DD`) | 포맷 지점 단일화 |

**판정**: 숨은 지점 없음. 다른 템플릿 (`admin/dashboard.html`, `admin/stats.html`) 의 `YYYY-MM-DD 마감` 포맷은 **admin 전용**이라 사용자 화면 D5-Q1d 결정과 무관.

---

## 종합 판정 — **GO**

| 영역 | 결과 |
|---|---|
| 옵션 B 선택 사유 재검증 | PASS |
| @NotNull 제거 사이드이펙트 | PASS |
| design-contract §5 서술 품질 | PASS |
| contract text-match selector 정확성 | PASS |
| 정적 재검증 (746 TC) | PASS |
| 동적 재검증 (curl + Playwright 45/45) | PASS |
| 담합 재공격 | PASS |

**커밋 가능 상태**. 추가 수정 불필요.

### 참고: 미처리 사항

- POST /admin/programs 세션 통한 400 응답 체인 실측은 미실시 (단위 테스트 커버로 대체)
- 소요 시간: gradle test 17m 31s + Playwright 4.8m = 약 22분. clean 선행 영향 큼
