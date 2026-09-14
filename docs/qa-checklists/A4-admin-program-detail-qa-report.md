# QA 리포트 — A4 admin-program-detail

| 메타 | 값 |
|---|---|
| 브랜치 | `feature/A4-admin-program-detail` |
| commit | `e5e86d9` (ym-impl 산출물) |
| spec | [`docs/specs/A4-admin-program-detail.md`](../specs/A4-admin-program-detail.md) `spec_confirmed` · Qn 24건 모두 A |
| QA 실행 | 2026-09-15 (회사 PC · JDK 17 부트스트랩 · e2e 프로파일 H2) |
| **판정** | **⛔ 재반려 (BLOCKING) — 목록 화면 LazyInitializationException 재현** |

---

## 요약

- 정적 회귀: **✅ 기존 468/469 유지** (사전 실행분과 동일 · Testcontainers 1건은 Docker 부재 · A4 무관)
- 신규 Service Test: **✅ 22/22 PASS** (A3-2 verify FAIL 학습 반영해 이번 QA 세션에서 신설)
- 신규 Modal Render Test: **✅ 3/3 PASS**
- 신규 List Render Test: **❌ 1/4 FAIL** — 목록 렌더 시 `app.user.name` 접근에서 `LazyInitializationException` 재현 (**P0 · BLOCKING**)
- 계약: `docs/design-contracts/admin/program-applications.md` + `e2e/contracts/admin-program-applications.ts` 신설

**결론**: 신설된 목록 화면(`/admin/programs/{id}/applications`)이 **실제 브라우저 접근 시 500 에러**를 낸다. 정적 검증만으로도 재현되며 사용자 회귀 이전에 자체 기능 결함이 확인되어 머지 불가.

---

## 1. 정적 검증

### 1-A. 전체 회귀 (`./gradlew test`)

| 항목 | 결과 |
|---|---|
| 전체 TC | 469 |
| 성공 | 468 |
| 실패 | 1 (`YouthMoaApplicationTests.contextLoads` — Testcontainers Docker unavailable · A4 무관) |
| 성공률 | 99.8% |
| duration | 3m 33s |

**A4 관련 회귀 없음** — 기존 회귀 baseline 과 동일 (Testcontainers 1건은 회사 PC Docker 미기동 상태에서 상수적으로 발생하는 인프라 이슈).

### 1-B. 신설 테스트 (A3-2 verify FAIL 학습 반영)

| 클래스 | TC | 결과 |
|---|---|---|
| `AdminApplicationServiceTest` | 22 | **✅ 22 PASS** |
| `AdminApplicationDetailModalRenderTest` | 3 | **✅ 3 PASS** |
| `AdminApplicationListRenderTest` | 4 | **❌ 1 FAIL / 3 PASS** |

**신규 커버리지 요약** (Service Test 22건):

- `assertProgramInScope` — sysadmin 허용 · 존재하지 않는 ID → IllegalArgumentException · CENTER_ADMIN 타 센터 → IllegalAccessError
- 목록 `list()` — sysadmin 전체 / PAGE_SIZE 매칭 / status 필터 격리 / 잘못된 값 무시 / appliedAt DESC 정렬
- `summaryCounts` — 4개 상태 모두 반환
- `totalCount` — Repository 직접 호출과 일치
- `visitsByUserIds` — 빈 목록 처리 / APPROVED 만 카운트
- `approve` — PENDING→APPROVED · processedBy·processedAt 세팅 · idempotent
- `reject` — reason 필수 (null/blank 각각 IllegalArgumentException) · 정상 케이스 상태 전이
- `forceCancel` — reason 필수 · `"관리자 취소: "` 접두 · processedBy 세팅 (Qn-C A)
- `updateNote` — 저장 / blank 시 clear / 1000자 초과 거절 · 상태 미변경
- `labelOf` — 4개 상태 한글 매핑 + null → "" 처리
- `statusOptions` — 4개 상태 순서 유지

---

## 2. 동적 검증 ⛔ BLOCKING

### 2-A. 재현된 결함

`AdminApplicationListRenderTest.GET_applications_목록_렌더_기본()` 이 실제 SpringBoot MockMvc 로 렌더링을 시도했을 때 아래 스택 트레이스 재현:

```
jakarta.servlet.ServletException: Request processing failed:
  org.thymeleaf.exceptions.TemplateInputException:
    An error happened during template parsing (template: "templates/admin/program/applications.html")

Caused by: org.thymeleaf.exceptions.TemplateProcessingException:
  Exception evaluating SpringEL expression: "app.user.name"
  (template: "admin/program/applications" - line 130, col 23)

Caused by: org.hibernate.LazyInitializationException:
  Could not initialize proxy [io.github.sihyuuun.youthmoa.user.User#31] - no session
```

### 2-B. 원인 분석 (소스 실측)

- `application.yml:20` — `spring.jpa.open-in-view: false` (전 프로젝트 규칙)
- `AdminApplicationService.list()` — JpaSpecificationExecutor + `Specification` 만 사용. `@EntityGraph` 없음 · fetch join 없음
- 결과: `applicationRepository.findAll(spec, pageable)` 로 돌아온 Application 의 `user` 는 lazy proxy. 서비스 트랜잭션 종료 후 템플릿 렌더 시점에 세션이 없어 `app.user.name` 접근 → `LazyInitializationException`

**spec §7 (307라인) 이 명시했던 규칙 위반**:
> `ApplicationRepository` 쿼리 추가 — `findByProgramWithUser(programId, filter, pageable)` `@EntityGraph`

구현이 `JpaSpecificationExecutor` 로 대체되었으나 `@EntityGraph` 를 적용하지 못했음. `Specification` 에서 fetch join 은 `root.fetch("user")` 로 명시해야 함.

### 2-C. 재현 조건

| 케이스 | 결과 |
|---|---|
| 목록 진입 (프로그램 1 = 28 APPROVED) | 500 · `LazyInitializationException` |
| 목록 진입 (빈 프로그램 · 신청 0건) | 200 (렌더할 row 가 없어 우연히 통과) |
| 필터 `?status=APPROVED` | 500 (동일 원인) |
| 상세 페이지 (`/{aid}`) | 200 · `findWithProgramAndUserById(@EntityGraph)` 사용 중이라 무관 |

### 2-D. 수정 방향 (제안 · 구현은 ym-impl 재실행 몫)

**옵션 A (권장 · spec §7 준수)**: `Specification` 안에서 fetch join

```java
Specification<Application> fetchUser =
    (root, query, cb) -> {
      if (query != null && Long.class != query.getResultType()) {
        root.fetch("user");
      }
      return cb.conjunction();
    };
spec = spec.and(fetchUser);
```

**옵션 B**: Service list 로직을 전용 Repository 메서드 (`findByProgramWithUser` `@EntityGraph`) 로 이관 후 `Specification` 대신 쿼리 파라미터 조합. spec §7 원안.

**옵션 C**: `@Transactional(readOnly = true)` 를 Service 클래스에 이미 부여했지만 트랜잭션이 Service.list() 종료와 함께 닫히므로 무의미. OSIV=true 로 되돌리는 것은 프로젝트 전역 정책 위반 → 제외.

---

## 3. 계약 신설

| 파일 | 내용 |
|---|---|
| `docs/design-contracts/admin/program-applications.md` | 아키텍처 · 상태 머신 · RBAC · CTA · 데이터 계약 · deviation 4건 · deferred 8건 |
| `e2e/contracts/admin-program-applications.ts` | 목록 20 checks + 상세 12 checks · Playwright `runContract` 호환 · deferred marking 3건 (Bulk / CSV / approve-all-pending) + 1건 (HTMX modal overlay) |

**계약 검증**은 별도 Playwright 실행 필요 — 이번 QA 세션에서는 bootRun 환경 미기동이라 실행 미완. contracts 코드 자체는 TS 컴파일 기준 문법 검증 완료.

---

## 4. 회귀 검증

| 트랙 | 결과 |
|---|---|
| 사용자 apply flow | 정적 회귀 468/469 안에 포함 · A4 관련 회귀 0건 |
| mypage cancel flow | 동 |
| notification listener | 동 |
| admin 전체 (dashboard·programs·notice·term·eligibility·dynamic-fields) | 동 |
| V15 마이그레이션 | e2e 프로파일 H2 재기동 시 Application 엔티티 매핑 검증 통과 (테스트 22건 실 DB 접근 성공) |

**V15 안전성**: `admin_note VARCHAR(1000) NULL` 컬럼 신설. 기존 시드 애플리케이션 admin_note 는 NULL 초기화 (테스트 확인). Application 도메인의 `updateAdminNote(null|blank)` 시 컬럼 clear 확인 (Service Test `updateNote_blank_clearsNote`).

---

## 5. 시각 확인 (사용자 영역)

**본 QA 세션에서는 bootRun 미기동으로 시각 확인 미실행**. 이번 재반려로 인해 시각 확인 이전에 구현 재실행이 필요하므로 시각 확인은 다음 사이클에서 수행 예정.

---

## 6. Δ deviation 로그

spec 대비 구현 이탈 항목 (구현이 재실행되어 목록 결함이 수정된 뒤에도 남는 이탈):

| 항목 | spec | impl | 판단 |
|---|---|---|---|
| `PAGE_SIZE` | Qn-6 A = 20 | 10 (`AdminApplicationService.PAGE_SIZE`) | **deviation** — 계약 md 에 명시. A2 admin-programs `ADMIN_PAGE_SIZE=10` 과 통일된 값 → 기능적으론 문제 없음 · spec 갱신 or 구현 수정 중 선택 필요 |
| 상세 UI | Qn-B A = 480px HTMX 모달 | 별도 페이지 렌더 (`_application-detail-modal.html` fragment 를 host page 로 감쌈) | **deviation** — 계약 md 에 명시 |
| 상태 변경 원자성 | Qn-Δ9 A = 원자적 (하나의 POST) | approve/reject/cancel/note 4 endpoint 분리 | **deviation** — 계약 md 에 명시 · UX 상 큰 차이 없음 |

Qn-6 PAGE_SIZE 는 spec_confirmed 24건 A 원칙과 명시적 충돌. 사용자 컨펌 필요.

---

## 7. 신설 자산 목록 (본 QA 세션 산출)

| 파일 | 종류 | 목적 |
|---|---|---|
| `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminApplicationServiceTest.java` | Service Test | Qn-A/C/1/2/3/6 로직 22 TC 커버 |
| `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminApplicationListRenderTest.java` | Render Test | 목록 화면 렌더 + 필터 활성 + 404 + 익명 리다이렉트 4 TC — **1 FAIL 로 P0 결함 감지** |
| `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminApplicationDetailModalRenderTest.java` | Render Test | 상세 모달 form 3개·담당자 의견·CSRF·F0c 답변 3 TC |
| `docs/design-contracts/admin/program-applications.md` | 디자인 계약 | 아키텍처·상태머신·CTA·데이터·deviation·deferred |
| `e2e/contracts/admin-program-applications.ts` | Playwright 계약 | 32 checks (20 목록 + 12 상세) — 4건 deferred marking |
| `docs/qa-checklists/A4-admin-program-detail-qa-report.md` | 본 리포트 | |

---

## 8. 판정 및 인계

- **판정: 재반려 (BLOCKING)**
- **차단 사유**: 목록 화면 `LazyInitializationException` — 실제 브라우저로 진입하면 500 에러 · spec §7 명시한 `@EntityGraph` fetch join 미이행
- **후속 처리**:
  1. ym-impl 재실행 → `AdminApplicationService.list()` 에 fetch join 추가 (options A/B 중 택 1)
  2. `AdminApplicationListRenderTest.GET_applications_목록_렌더_기본()` 재실행 PASS 확인
  3. bootRun 기동 후 실제 GET `/admin/programs/1/applications` 응답 200 실측
  4. PAGE_SIZE = 10 vs 20 사용자 최종 컨펌
  5. ym-verify 재실행 → 계약 갭 0 확인
- **머지 조건**: 위 4개 항목 모두 완료 시

---

## 부록 A · 실측 스택 (LazyInitializationException)

```
Exception evaluating SpringEL expression: "app.user.name"
  (template: "admin/program/applications" - line 130, col 23)
...
Caused by: org.hibernate.LazyInitializationException:
  Could not initialize proxy [io.github.sihyuuun.youthmoa.user.User#31] - no session
```

- 파일: `src/main/resources/templates/admin/program/applications.html:130`
- 대응 소스: `AdminApplicationService.java:82~102` (Specification list)
- OSIV 설정: `application.yml:20` (open-in-view: false)

## 부록 B · Service Test 22 TC 상세

- `assertProgramInScope_sysadmin_allowsAny`
- `assertProgramInScope_missingId_throwsIllegalArgument`
- `assertProgramInScope_centerAdmin_otherCenterProgram_denied`
- `list_sysadmin_returnsAppliedApplications`
- `list_pageSize_matches_constant`
- `list_statusFilter_APPROVED_isolatesRows`
- `list_statusFilter_invalidValue_ignored`
- `list_sortedByAppliedAt_desc`
- `summaryCounts_returnsAllFourStatuses`
- `totalCount_matchesRepositoryDirect`
- `visitsByUserIds_empty_returnsEmptyMap`
- `visitsByUserIds_bulkQueryOnlyCountsApproved`
- `approve_pending_transitionsToApproved`
- `approve_alreadyApproved_idempotentNoop`
- `reject_withReason_transitionsToRejected`
- `reject_blankReason_throwsIllegalArgument`
- `reject_nullReason_throwsIllegalArgument`
- `forceCancel_withReason_prefixesReason_andSetsProcessedBy`
- `forceCancel_blankReason_throwsIllegalArgument`
- `updateNote_savesNote_statusUnchanged`
- `updateNote_blank_clearsNote`
- `updateNote_over1000chars_rejected`
- `labelOf_koreanMapping`
- `statusOptions_containsAllFour`
