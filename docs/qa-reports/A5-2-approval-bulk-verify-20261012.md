# 적대적 검증 리포트: A5-2 승인 처리 페이지

- 작성: 2026-10-12 (ym-verify)
- 브랜치: `feature/A5-2-approval-bulk` (미커밋 · 13 modified + 3 untracked test/spec)
- 선행 체인: ym-spec a7f391c5 → ym-impl1 afc3d5fd → ym-qa1 ad765998 (BLOCK) → ym-impl2 a767e58f → ym-qa2 a585badd (PASS)
- 범위: qa 2차 "PASS" 판정 반박. refute-first 로 공격 벡터 14개 전수 검증

---

## 판정 요약

- **PASS 10 / FAIL 0 / UNVERIFIED 4**
- **결론: 머지 가능 (조건부)** — 핵심 요구 전부 충족. UNVERIFIED 4건은 실제 결함이 아니라 "verify 환경에서 재확인 불가" 항목. qa 2차의 E2E 4/4 + 302 round-trip + 소스 리뷰로 커버됨
- 단, **spec 서술 오타 1건** 과 **테스트 커버리지 공백 2건** 은 머지 후 즉시 보강 권고 (MEDIUM, 비차단)

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | Q1 공통 사유 복제 — 운영 리스크? | `AdminApplicationBulkService.java:159` `app.reject(admin, reason)` | PASS (설계상 수용) | 사용자 Q1 결정 명시(spec L41). 서비스가 단일 reason 을 전 row 에 적용. "일괄 반려 사유는 공통으로 적용" 모달 안내도 명시(`admin-bulk.js:45`). 운영 리스크는 UX 결정 사항이며 기술 결함 아님 |
| 2 | Q2 200자 경계 (@Size vs 서버 validate) | `AdminApplicationController.java:258-265` | PASS | Controller 가 blank(258) + length>200(262) 수동 검증. @Size 어노테이션 미사용(BindingResult 없음) → DTO 바인딩이 아닌 raw `@RequestParam String reason` 이므로 수동 체크가 정확. 201자 → `flashError` + 302. 단, **201/200 경계 자동 테스트 부재** (MEDIUM) |
| 3 | Q3 500건 cap 처리 | `AdminApplicationBulkService.java:40,212-217` | PASS | `MAX_BULK_SIZE=500` 상수 + `assertSizeCap` 가 bulkApprove/bulkReject/bulkApproveAllPending(위임) 전부에 적용. 501건 → IllegalArgumentException → Controller catch → flashError (approve `AdminApplicationController.java:216-220`, reject 270-275). 단, **501건 경계 자동 테스트 부재** (MEDIUM) |
| 4 | Q4 CENTER_ADMIN scope 격리 (bulk 전수) | `AdminApplicationBulkService.java:191-204` `loadProgramInScope` | PASS | bulkApprove/bulkReject/bulkApproveAllPending 전부 `loadProgramInScope` 선행 호출. scope 불일치 시 IllegalStateException → Controller가 AccessDeniedException 전환(App:218,273). bulkApproveAllPending(L112)도 loadProgramInScope 호출 후 ids 조회 → 위임 bulkApprove 가 재검증 (중복 가드) |
| 5 | Q5 성별 열 — CSV 테스트가 gender 적극 검증? | `admin-bulk-csv.spec.ts:240` `expect(content).toContain('applicantEmail')` | **UNVERIFIED (MEDIUM)** | 기존 CSV E2E 는 applicantEmail 헤더만 확인 — gender 헤더/값 적극 검증 없음. 코드 리뷰로만 11열(gender 4번째) 확인됨 (`AdminCsvController.java:238,253-256`). 성별 E2E (`admin-application-bulk-reject.spec.ts:124-137`)는 **화면 셀만** 검증, CSV 는 미커버 |
| 6 | Q6 302 PRG + flash 전달 | Controller 전수 `return "redirect:..."` + `RedirectAttributes.addFlashAttribute` | PASS | 모든 POST (bulkReject · bulkApproveAllPending · bulkApprove) 가 303(Spring 기본 302 처리)로 PRG. E2E (`admin-application-bulk-reject.spec.ts:61,109`) `page.waitForURL` + `flash-success` 가시성 확인 |
| 7 | Q7 cross-page select 미지원 | spec L46 명시 | PASS (현상 유지) | 신규 리스크 추가 없음 |
| 8 | Q8 반려 이메일 — 리스너 실제 bulk 이벤트 수신? | `notification/ApplicationNotificationListener.java:51` `onRejected(ApplicationRejectedEvent)` | PASS | 리스너 존재. bulkReject (`AdminApplicationBulkService.java:169-175`) 가 성공 건마다 ApplicationRejectedEvent 발행. 테스트 (`AdminApplicationBulkRejectServiceTest:65-81`) 가 eventCount==2 로 2건 발행 실측 |
| 9 | Q9 액션바 다크 유지 | diff 전수 — admin-bulk CSS 미변경 | PASS | admin.css 변경은 application-row grid·반응형·toolbar inline-flex 3개뿐. 액션바 색상 토큰 미변경 |
| 10 | Q10 "명" 라벨 — 다른 도메인 영향? | `_bulk-action-bar.html:30-35` `th:if domain=='applications'` / `th:unless` 분기 | PASS | 분기 2개로 격리. users/programs 는 "건" 유지. admin-bulk.js 는 count 라벨 하드코딩 없음 (DOM 템플릿이 소스) |
| 11 | BUG-1 재발 방지 — enum 확장 자동 커버? | `AdminApplicationController.java:76-80` `for enum entry → put(key.name(), value)` | PASS | HashMap 전수 재패킹이므로 ApplicationStatus enum 신규 상수 추가 시 자동 포함. 수동 유지 지점 없음. 단, 템플릿은 `summary['PENDING']` 등 하드코딩 키 조회 — 신규 상태 UI 반영은 템플릿 수정 필요 (설계 범위) |
| 12 | APPROVED→REJECTED 공석 승격 중복 가드 | `WaitlistPromotionService.java:66-73` `approvedCount >= capacity` 가드 | PASS | bulkReject 가 N 건 APPROVED→REJECTED 시 N 번 promoteIfEligible 호출. 각 호출마다 capacity 재확인 → 공석 1개당 1명 승격. 과승격 불가. 각 호출은 default @Transactional (REQUIRED, 호출측 REQUIRES_NEW 외부에서 새 트랜잭션) |
| 13 | 500건 bulk 트랜잭션 범위 | `AdminApplicationBulkService.java:51-55,65` `TransactionTemplate PROPAGATION_REQUIRES_NEW` | PASS | row 단위 REQUIRES_NEW. 중간 실패해도 성공 row 는 커밋 유지. BulkResult 로 집계. 설계 일치 (spec L17) |
| 14 | 15+ 파일 cascade drift | `grep "일괄 반려|approve-all-pending|admin-application-row|row-gender"` + fragment arity | PASS | 3 호출부 모두 bar(…, rejectPath) 5인자 — arity 일치. drift 없음 |
| 15 | spec 매핑 표 vs 실제 코드 line | spec L72-87 전수 대조 | PASS (오타 1건 제외) | 매핑 대부분 일치. 단, **spec L31/L85 "9열" 서술은 실제 10열 구조(44·44·100·1fr·44·120·150·80·90·90)와 불일치** — 토큰 리스트는 9개를 쓴 뒤 뒤이어 "관리 열 90px"을 추가하므로 실제 10개 (LOW 서술 오타, UI는 정합) |
| 16 | compileJava + compileTestJava | `.\gradlew.bat compileJava compileTestJava` | PASS | 워닝/에러 없음 (7초) |

---

## UNVERIFIED 상세

### U1. CSV gender 컬럼 자동 검증 공백 (MEDIUM)

- 사유: 기존 `admin-bulk-csv.spec.ts` 가 `applicantEmail` 헤더만 체크 (L240). gender 헤더·값 체크 없음
- 영향: CSV 11열 구조는 코드 리뷰로만 확보. 미래 리팩토링 시 gender 순서/존재 회귀 가능
- 보강안 (후속 커밋 권고): `admin-bulk-csv.spec.ts` 에 `expect(content).toContain('gender')` + 첫 데이터 행 4번째 필드 ("남"/"여"/"") 확인 1줄 추가

### U2. 201자 경계 자동 테스트 부재 (MEDIUM)

- 사유: `AdminApplicationBulkRejectServiceTest` 는 blank/null 만 검증. Controller 레벨 200/201 경계 테스트 없음
- 영향: Controller `length > 200` 조건 회귀 시 감지 지연
- 보강안: Controller MockMvc 테스트 1건 — 201자 reason → flashError. 또는 Service bulkReject 에 길이 검증을 밀어넣고 단위 테스트

### U3. 501건 경계 자동 테스트 부재 (MEDIUM)

- 사유: `MAX_BULK_SIZE=500` 상수 존재 + assertSizeCap 로직 존재. 하지만 경계 자동 테스트 없음
- 영향: cap 상수 변경 또는 분기 로직 회귀 시 감지 지연
- 보강안: BulkRejectServiceTest 에 501건 ids → IllegalArgumentException assertion 1건 추가

### U4. 다중 센터 교차 AccessDenied 자동 E2E 부재

- 사유: Q4 CENTER_ADMIN 분기는 소스 레벨 검증만. 타 센터 program 에 bulk/reject 요청 → AccessDenied 403 round-trip 자동 테스트 없음
- 영향: scope 분기 로직 회귀 시 감지 지연 (실제 배포 시 403 노출 가능성은 낮음 — 화면에 진입 자체가 차단되므로)
- 보강안: 기존 A4 scope 테스트 패턴 재사용하여 bulk 엔드포인트 추가

---

## FAIL 상세

없음.

---

## 머지 가능 여부

**조건부 PASS → 머지 가능.**

- 모든 핵심 요구 (Q1~Q10, BUG-1, 공석 승격, cascade drift) 통과
- 발견된 공백은 자동 테스트 커버리지 보강 범주 (MEDIUM, 비차단). qa 2차의 수동 E2E + 소스 리뷰로 커버됨
- spec L31 "9열" 서술 오타는 UI 자체는 정합이므로 문서 패치로 충분 (LOW)

### 후속 보강 권고 (별도 PR 또는 커밋)

1. `admin-bulk-csv.spec.ts` 에 gender 헤더/값 검증 추가 (U1)
2. BulkRejectServiceTest 에 201자 + 501건 경계 테스트 2건 추가 (U2, U3)
3. spec L31/L85 "9열" → "10열" 서술 정정 + CSS 주석과 통일

### 커밋 전 확인

- `git status --short` 로 cookies.txt (?? 유물) 제외 확인
- 테스트 파일 3개 (BulkRejectServiceTest · BulkApproveAllPendingTest · admin-application-bulk-reject.spec.ts) 포함 스테이징
- qa 리포트 3건 (qa-20261012.md · qa-20261012-2.md · verify-20261012.md) 포함
