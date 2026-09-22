# 작업 명세: A9-a — Program-Center FK 도입 (1/2 · 병행 유지 단계)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed_post_hoc`** (2026-09-21 · 착수 시점 대화형 결정만 있었고 spec 파일 부재 → ym-verify #1 UNVERIFIED 지적으로 사후 문서화. Q-A9-2/4/5/7 결정 이력 포함) |
| 브랜치 | `feature/A9-a-program-center-fk` |
| 스코프 | `Program.organization`(String, nullable=false, length=100) → `Program.center`(Center FK) 전환. Center 엔티티는 이미 완비(145 LOC)·User.center FK 존재. 순수 Program 축 확장 + A7 event.centerId 원자적 전환. **A9-a 는 병행 유지 스텝 (V20 nullable 컬럼 추가 · Backfill · organization 컬럼 @Deprecated 유지)**. NOT NULL 승격 · DROP 은 A9-b (다음 릴리즈) |
| 선행 | ✅ Center 엔티티(2026-08-XX 완비) · ✅ User.center FK(A5-1 이전 존재) · ✅ AdminScope.effectiveCenterName(A5) |
| 후행 | **A9-b** (V21 NOT NULL · V22 DROP · 사용자 템플릿 15+ 곳 `.organization` → `.center.name` 리네임 · UserRepository dead code 삭제 · Program.organization 필드/builder 동기화 제거) · **A7-createdBy-recipient B-3 확장** (Program.createdBy + watcher chain) |
| 마스터 지시서 | ADMIN-00 §Program 확장 갭 리스트 (Program-Center FK 미도입 항목) |
| main 기준 | `1d38030` (A5-1 완결 상태) |
| 예상 규모 | **중** — 파일 41 · 순증 +579/-184 LOC · V20 마이그레이션 · Backfill Runner 신설 · 계약 갱신 (`field.organization` → `field.centerId` deviation 해제) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: `docs/design-contracts/admin-program-form.md` + `e2e/contracts/admin-program-form.ts`
  - A3-1 시점 원본은 prototype select 9종 요구를 "text 로 축약 (deviation)" 표기
  - **A9-a 로 deviation 해제** — `field.centerId` (select) 로 승격, prototype L2495~2501 정합 회복
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` 승계
- **재활용 계약**: `admin-programs-list.ts` (A2) · `admin-program-form.ts` (A3-1/2)
- **prototype 우선순위**:
  - `admin/prototype.html` L2495~2502 — 프로그램 등록/수정 폼의 청년센터 `<select value="pfCenter">` (필수 `*`)
  - L2867 — `centerList = ['고천센터','더누림플랫폼', ..., '오름']` 드롭다운 옵션 소스
  - L2876 — programs mock `{ id:7, center:'양주 청년센터', region:'양주시', ... }` — center 문자열이나 UI 는 select

---

## 1. 디자인 출처 · 자산 간 갭

| 항목 | wireframe | prototype.html (admin) | 현재 구현 (A9-a 이전) | 채택 |
|---|---|---|---|---|
| 청년센터 입력 방식 | 명시 없음 | select 드롭다운 (필수 `*`) | text input (자유입력) | **prototype — select FK 필수** |
| 폐업/비활성 센터 선택 | 명시 없음 | `isActive` 필터 없음 (mock) | 필터 없음 | 신규: `Center.isActive=true` 만 옵션 노출 + 서버 방어 |
| 센터명 변경 시 프로그램 표기 | 명시 없음 | 문자열 스냅샷 | 문자열 스냅샷 | **FK 실시간 반영** (스냅샷 폐기 · 데이터 무결성 우선) |

---

## 2. 사용자 결정 (Q-A9)

| # | 질문 | 결정 | 이유 |
|---|---|---|---|
| **Q-A9-1** | Center 신설 vs 재사용? | **재사용 확정** | 실측: `Center.java` 145 LOC · `User.center` FK 이미 존재 · centers.csv 49건 시드 완비 |
| **Q-A9-2** | organization 컬럼 처리 | **병행 → 다음 릴리즈 DROP** (V20 nullable · V21 NOT NULL · V22 DROP · 3 스텝) | Backfill 실패 시 rollback 여유. FK NOT NULL 즉시 도입 시 부팅 실패 리스크 |
| **Q-A9-3** | Center CRUD 화면 A9 포함? | **분리 A9-1** | prototype admin 에 Center 관리 화면 없음. site config 만 존재 |
| **Q-A9-4** | A7 resolver B-1→B-3 A9 포함? | **A9-a 에 포함** | event.programOrganization → event.centerId 는 organization 컬럼 폐기와 원자적. 분리 불가능 |
| **Q-A9-5** | Stats DTO 필드명 리네임? | **centerName 리네임** | 의미 정합성. Stats DTO 는 admin 내부 소비만 |
| **Q-A9-6** | 센터명 변경 시 표기 정책 | **FK 실시간 반영** | 데이터 무결성 우선. 스냅샷 필요 시 별도 필드 신설 (권장 X) |
| **Q-A9-7** | Backfill 매칭 실패 fallback | **fail-fast (B안 · 부팅 실패)** | 학습 프로젝트 · 조기 감지 원칙 · seed 24건 100% 매칭 예상 |

## 2-A. QA 라운드 추가 결정 (2026-09-21)

| # | 질문 | 결정 | 이유 |
|---|---|---|---|
| **Q-A9-QA-1** | loadCenter 에 isActive 검사 추가? | **추가 (5 LOC + 단위 TC 2건)** | Admin form select 는 활성 센터만 노출하나, URL/폼 조작 방어 필요. QA 발견 P1 |
| **Q-A9-QA-2** | spotless 정정을 A9-a 안에 포함? | **포함 (7 파일 자동 정정)** | ym-impl 원본 commit 이 spotless 위반 → CI Gradle Check FAIL → A9-a 원자성 위해 같은 PR 에서 정정 |

---

## 3. 변경 범위

### 신규 파일 (3)
- `src/main/resources/db/migration/V20__add_program_center_id.sql` — `program.center_id BIGINT REFERENCES center(id)` nullable + 인덱스
- `src/main/java/.../program/ProgramCenterBackfill.java` — `ApplicationRunner`. `center_id IS NULL` 프로그램에 대해 `Center.name = program.organization` 매칭 → UPDATE. **매칭 실패 시 IllegalStateException 던져 부팅 중단** (Q-A9-7 fail-fast)
- `src/test/java/.../program/ProgramCenterBackfillTest.java` — 3 TC (success · fail-fast · idempotent skip)

### 엔티티 변경
- `Program.java:36` — organization 유지(@Deprecated 주석) + `@ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name="center_id") Center center;` 추가 (**EAGER** — open-in-view=false 하 템플릿 접근 시 LazyInitializationException 방어 · Program.java:51~53 주석). Builder/`updateFromAdminForm` 시그니처에 `center` 파라미터 추가. **organization 자동 동기화** — `this.organization = center != null ? center.getName() : organization` (3개 mutation 경로 모두)

### Repository 변경
- `CenterRepository.java` — `findByIsActiveTrueOrderByNameAsc()` 신설 (form select 옵션 소스)
- `ProgramRepository.java:34` — JPQL group-by `p.organization` → `p.center.id, p.center.name`
- `UserRepository.java` — `findByRoleAndIsActiveTrueAndCenter_Id` 신설 (`_Name` 은 dead code · A9-b 삭제 대상)

### Service 변경
- `AdminScope.java` — `effectiveCenterId(): Long` 신설. 기존 `effectiveCenterName()` 은 라벨용으로 유지
- `AdminProgramService.java` — `create/update` 에서 `req.getCenterId()` 로 Center 조회 세팅 · `find()` 격리 `center_id` 매칭 · `loadCenter()` isActive guard + scope guard 이중 방어
- `AdminProgramBulkService/AdminApplicationService/AdminApplicationBulkService` — 동일 전환
- `AdminDashboardService/AdminStatsService/AdminCsvController` — organization 매칭·표시 → center_id / center.name
- `AdminStatsService` DTO — `.organization(...)` → `.centerName(...)` (**Q-A9-5** 리네임)
- `ProgramSpec.java:28,94` — `p.organization` → `p.center.name` (join)
- `ProgramController.java:164` — organization → center.phone 우회 조회 제거, 직접 `program.center.phone`
- `CenterService.java`, `CenterController.java` — 신규 폼 옵션 API 제공

### A7 알림 원자적 전환 (**Q-A9-4**)
- `ApplicationCreatedEvent.java` — `programOrganization: String` → `centerId: Long`
- `ApplicationCreatedRecipientResolver.java:48` — `findByRoleAndIsActiveTrueAndCenter_Name(CENTER_ADMIN, organization)` → `findByRoleAndIsActiveTrueAndCenter_Id(CENTER_ADMIN, centerId)`
- `AdminNotificationEventListener.java`, `ApplicationService.java` — 이벤트 발행부 인자 변경 (`app.program.getCenter()?.getId()` 안전 참조)

### Admin form/list 템플릿
- `templates/admin/program/form.html:126~129` — `<input type="text">` → `<select th:field="*{centerId}">` + `th:each` 로 활성 센터 옵션 렌더
- `templates/admin/program/list.html:123` — `${p.organization}` → `${p.center.name}`
- `templates/admin/stats.html` — organization 표시 → centerName
- `ProgramFormRequest.java:29` — `String organization` → `@NotNull Long centerId`
- `AdminProgramController.java` — 폼 모델에 `activeCenters` 추가

### 시드 데이터
- `DataInitializer.java:632~961` — 24개 Program seed `.organization("...")` → `.center(centerRepo.findByName("...").orElseThrow(...))` 로 전환. 매칭 실패 시 부팅 실패 (fail-fast)

### QA 라운드 (7b4c51a 커밋)
- `AdminProgramService.loadCenter()` — `!center.isActive()` 검사 추가 (Q-A9-QA-1)
- `AdminProgramFormServiceTest` — 신규 TC 2건 (`create_inactive_centerId_거부_400`, `update_inactive_centerId_거부_400`)
- Spotless 자동 정정 7 파일 (Q-A9-QA-2)
- `e2e/tests/admin-program-form.spec.ts` — `input[organization]` 4곳 → `select[centerId].selectOption({index:1})`
- `e2e/contracts/admin-program-form.ts` — `field.organization` / `edit.organization.prefilled` → `field.centerId` / `edit.centerId.prefilled` (prototype 정합 회복)
- `e2e/tests/admin-program-center-fk.spec.ts` — QA 신설 5 TC (centerId select · CENTER_ADMIN 스코프 · 문의처 center.phone)

---

## 4. 갭 리스트

| # | 항목 | 현재 (A9-a 이전) | prototype (admin) | A9-a 조치 |
|---|---|---|---|---|
| 1 | Program 입력 형식 | text 자유입력 | select 필수 | ✅ select 도입 |
| 2 | Program.center FK | 없음 (문자열) | 개념적 (mock 필드) | ✅ FK 추가 (병행) |
| 3 | 폐업 센터 노출 방지 | 항상 노출 | (mock 이므로 없음) | ✅ `isActive=true` 만 select · loadCenter 서버 방어 |
| 4 | 알림 수신자 매칭 | organization 문자열 | center 개념 | ✅ event.centerId · resolver 전환 |
| 5 | 통계 group-by | per-row organization | 미지정 | ✅ center.id/name · DTO 리네임 |
| 6 | CSV export 컬럼명 | "organization" | 없음 | ⏸ 유지 (호환) · 값은 center.name |
| 7 | 사용자 템플릿 15+ 곳 | organization 참조 | (사용자 화면 무관) | ⏸ **A9-b 이월** — DROP 전 리네임 필수 |

---

## 5. 검증 시나리오

### 정적 검증
- `./gradlew compileJava` · `./gradlew test`
- `ProgramCenterBackfillTest` 3 TC (success · fail-fast · idempotent)
- `AdminProgramFormServiceTest` 17 TC (신규 inactive guard 2건 포함)
- `AdminNotificationEventListenerTest` 4 TC (event.centerId 기반 fan-out)
- 회귀 감시: `AdminProgramFormRenderTest` · `AdminProgramListRenderTest` · `ProgramSearchTest` · `ProgramServiceFilterTest`
- `./gradlew spotlessCheck` PASS

### 동적 검증 (bootRun e2e 8090)
- `GET /admin/programs/new` — select 옵션에 활성 센터 렌더
- `POST /admin/programs` (centerId 유효) 200 → 목록에 반영
- `POST /admin/programs` (centerId 누락) → validation 오류 재표시
- `GET /admin/programs` (CENTER_ADMIN 세션) → 자기 센터 프로그램만 노출
- `GET /programs/{id}` → 문의처 = center.phone

### 인터랙션 E2E (Playwright)
- `e2e/tests/admin-program-form.spec.ts` — organization → centerId 갱신 후 6/6 PASS
- `e2e/tests/admin-program-center-fk.spec.ts` — 신규 5 TC PASS
- backfill 검증: 부팅 후 `SELECT COUNT(*) FROM program WHERE center_id IS NULL` = 0

### 계약 검사
- `--project=contracts` `admin-program-form` — `field.centerId` 승격 · deviation 0건

---

## 6. 의존성 · 후속

- 선행: A5-1 staff 완료 · User.center FK 이미 사용 중 · AdminScope 전환 안전
- 병렬 금지: A6 admin-stats 는 organization 표시 참조 → A9-a 와 순차 진행 (이번 통합)
- 후행 트리거: **A9-b** (V21/V22 · 사용자 템플릿 리네임 · dead code 삭제) · **A7-createdBy-recipient** (Program.createdBy + watcher · A9-b 정합 이후)

---

## 7. 작업 큐 메타

- 작업 ID: **A9-a**
- 실 커밋:
  - `8b760fe` — ym-impl 원본 구현
  - `7b4c51a` — QA fix 라운드 (spotless + loadCenter guard + spec/contract 갱신)
- PR: #223
- 검증 체인: ym-spec → ym-impl → ym-qa → ym-verify (11 PASS · 0 FAIL · 2 UNVERIFIED · 커밋 가능 판정)
- 상태: **`verify_passed`** — 사용자 최종 승인 대기

---

## 8. verify UNVERIFIED 후속 처리

| # | 항목 | 처리 |
|---|---|---|
| #1 | spec 파일 부재 | **RESOLVED** — 본 문서로 사후 정합 확보. A9-b 부터 spec 파일 필수화 (사용자 결정 대기) |
| #9 | visual-admin-notice 3 FAIL | **RESOLVED (A9-a 인과 없음)** — 원인: `visual-admin-notice.spec.ts` 가 `loginAdmin(page)` 후 `page.goto(contract.path)` 호출 누락 → 대시보드 상태로 contract 실행. main 2026-09-18 부터 존재하는 사전 스펙 버그. CI `continue-on-error: true` 로 masking 됨. **별도 티켓 `fix/visual-admin-notice-goto` 이월 권장** |

---

## 9. A9-b 이월 확정 (verify 지적)

1. 사용자 템플릿 15+ 곳 `.organization` → `.center.name` 리네임 (V22 DROP 전 필수)
2. `UserRepository.findByRoleAndIsActiveTrueAndCenter_Name` dead code 삭제
3. `Program.organization` 필드 + `@Deprecated` 제거 + `Program.builder()` organization 동기화 로직 제거
4. V21 (center_id NOT NULL 승격) · V22 (organization DROP)
5. A9-a spec 문서 이력을 A9-b spec 서두에 포함 (spec 연속성)
