# A9-b · Program.organization DROP (완결 스텝)

> 상태: **impl_done** · 착수 2026-09-22 · 브랜치 `feature/A9-b-program-organization-drop`
>
> 선행: A9-a (PR #223 · `b264683`) — Center FK 병행 도입 (organization NOT NULL + center nullable)
>
> 후행: 없음 (A9 트랙 종결)

---

## 1. 배경

A9-a 로 도입된 병행 상태 (Program.organization NOT NULL + Program.center nullable) 를 원자적 전환으로 종료. Program.center FK NOT NULL 승격 + organization 컬럼 DROP.

A9-a 시점에는 Program.organization 이 `Center.name` 을 mirror 하는 자동 동기화 필드였고, 대부분의 사용자 템플릿이 아직 `${program.organization}` 을 참조했다. A9-b 는 이 브릿지를 걷어내고 모든 소비 지점을 `Center` FK 접근으로 통일한다.

---

## 2. 사용자 결정 (Q-A9-b)

| ID | 질문 | 결정 |
|---|---|---|
| Q-A9-b-1 | V21 시 organization 컬럼도 함께 유지? | **A안 (유지)** — Program.builder 가 계속 채우므로 안전. NOT NULL 승격만 별도 파일로 분리 |
| Q-A9-b-2 | V21 + V22 를 단일 PR 로? | **A안 (단일 PR)** — 학습 프로젝트 · Backfill 검증 완료. 롤백 리스크 낮음 |
| Q-A9-b-3 | ProgramCenterBackfill 러너와 Test 도 함께 삭제? | **A안 (완전 삭제)** — 병행 상태 종료 후 재실행 필요 없음 |
| Q-A9-b-4 | CSV 컬럼명 organization → centerName? | **A안 (centerName)** — 계약 (`admin-csv.ts` L16) 갱신 필수 |
| Q-A9-b-5 | Program.getOrganization / DTO.getOrganization 완전 제거? | **A안 (제거)** — DTO 는 `getCenterName()` 신설 |

---

## 3. 변경 범위 (실측)

### 3-1. 마이그레이션 신설

| 파일 | 내용 |
|---|---|
| `src/main/resources/db/migration/V21__program_center_id_not_null.sql` | `ALTER TABLE program ALTER COLUMN center_id SET NOT NULL` |
| `src/main/resources/db/migration/V22__drop_program_organization.sql` | `ALTER TABLE program DROP COLUMN organization` |

### 3-2. 삭제

| 파일 | 사유 |
|---|---|
| `src/main/java/.../program/ProgramCenterBackfill.java` | A9-a 병행 상태 종료 → 부팅 시 backfill 불필요 |
| `src/test/java/.../program/ProgramCenterBackfillTest.java` | 위 러너 테스트 |

### 3-3. 엔티티 · Repository

| 파일 | 변경 |
|---|---|
| `program/Program.java` | `organization` 필드/getter/builder/`update()`/`updateFromAdminForm()` 파라미터 전부 제거. `@ManyToOne center` 에 `nullable=false, optional=false` 확정 |
| `application/Application.java` | `program` 관계 `LAZY → EAGER` 승격 (§4 근거) |
| `bookmark/Bookmark.java` | `program` 관계 `LAZY → EAGER` 승격 (§4 근거) |
| `application/ApplicationRepository.java` | `@EntityGraph(attributePaths)` 에 `program.center` 추가 (2건: `findWithProgramAndUserById`, `findAllByUserOrderByAppliedAtDesc`) |
| `bookmark/BookmarkRepository.java` | `@EntityGraph(attributePaths)` 에 `program.center` 추가 |
| `center/CenterRepository.java` | `findByName` 주석 정리 (organization 언급 제거) |
| `user/UserRepository.java` | `findByRoleAndIsActiveTrueAndCenter_Name` 사용처 확인 후 유지 (실 소비 지점 있음) |
| `program/ProgramSpec.java` | `withKeyword` 검색 대상 (title/region/content) — organization 절 제거 |
| `program/ProgramCardDto.java` | `getOrganization()` 제거 · `getCenterName()` 신설 |

### 3-4. Service · Controller

| 파일 | 변경 |
|---|---|
| `program/ProgramController.java` | 상세 fallback → `program.getCenter().getPhone()` 직접 |
| `admin/AdminCsvController.java` | CSV 헤더 `organization → centerName` · 값 `p.getCenter().getName()` |
| `admin/AdminStatsService.java` | fallback 제거 → `p.getCenter().getName()` |
| `admin/AdminScope`, `AdminProgramService`, `AdminProgramBulkService`, `AdminDashboardService`, `AdminApplicationService`, `notification/NotificationType`, `notification/admin/AdminNotificationEventListener` | organization 언급 주석 정합 |
| `common/DataInitializer.java` | resolveCenter 주석 정리 |

### 3-5. 템플릿

**사용자 14곳**: `index.html`, `application/apply.html`, `application/complete.html`, `mypage/application-detail.html`, `mypage/history.html`, `mypage/favorites.html`, `search/result.html`, `program/detail.html`, `program/_list-fragment.html`, `program/_calendar-fragment.html` — 전부 `${...organization}` → `${...center?.name}` 또는 DTO `centerName` 접근자.

**Admin 4곳**: `admin/dashboard.html`, `admin/stats.html`, `admin/user/detail.html`, `admin/program/list.html`.

### 3-6. 계약 · 테스트

| 파일 | 변경 |
|---|---|
| `e2e/contracts/admin-csv.ts` L16 | CSV 헤더 문자열 `organization → centerName` |
| test fixture 정정 (15+ 파일) | `.organization("X")` → `.center(Center.builder()...)` 또는 실제 CenterRepository 시드. `Program::getOrganization` → `p.getCenter().getName()` |
| `AdminNotificationEventListenerTest` `apply_with_unmatched_organization_creates_no_notifications` | 폐기 (center NOT NULL 로 orphan 시나리오 스키마상 성립 불가) |

---

## 4. 아키텍처 결정 (재발 방지 근거)

### Application.program / Bookmark.program 을 EAGER 로 승격한 이유

A9-a 에서 `Program.center` 는 이미 EAGER 로 설정돼 있었으나 (open-in-view=false 하 템플릿 접근 시 LazyInitializationException 방어), Application/Bookmark 가 `program` 을 LAZY 로 가지고 있어 다음 조건에서 사고가 발생했다.

1. Application 이 Program 프록시를 참조 상태로 세션 밖에 나감
2. 템플릿이 `app.program.center?.name` 접근
3. Program 프록시 초기화가 우선 필요 → 세션 이미 닫힘 → `LazyInitializationException`

A9-a 는 이 사고를 `program.organization` (자동 동기화된 String) 접근으로 회피해 왔다. A9-b 는 그 브릿지를 걷어내므로 Application/Bookmark 의 program 을 EAGER 로 승격하는 게 필수다.

동시에 조회 진입점 (`ApplicationRepository.findAllByUserOrderByAppliedAtDesc`, `findWithProgramAndUserById`, `BookmarkRepository.findAllByUserOrderByCreatedAtDesc`) 에 `@EntityGraph(attributePaths = {"program", "program.center"})` 를 명시해 fetch 시점에 join 을 강제한다. 두 레이어 방어로 프로덕션 렌더 경로에서 프록시 초기화가 필요 없다.

렌더 페이지가 늘 program + center 를 함께 노출하므로 EAGER 승격으로 인한 over-fetch 우려는 없다.

---

## 5. 30곳 소비 지점 → 코드 매핑

| # | prod 소비 지점 (A9-a 상태) | A9-b 처리 |
|---|---|---|
| 1~2 | `Program.java` builder/update organization | 필드 및 파라미터 제거 |
| 3 | `Program.java` getOrganization getter | Lombok @Getter 제거 (필드 삭제로 자동) |
| 4 | `Program.center` nullable | `nullable=false, optional=false` 로 승격 |
| 5 | `ProgramSpec.withKeyword` L15,28 organization 검색 | title/region/content 만 유지 |
| 6 | `ProgramCardDto.getOrganization()` L239~241 | `getCenterName()` 로 리네임 |
| 7 | `ProgramController.java` L164, L170~173 상세 fallback | `program.getCenter().getPhone()` |
| 8~9 | `AdminCsvController.java` L141, L157~158 CSV 헤더 | `centerName` + `p.getCenter().getName()` |
| 10~11 | `AdminStatsService.java` L320, L368 fallback | `p.getCenter().getName()` |
| 12 | `DataInitializer.java` L622~634 주석 | 정합 |
| 13 | `CenterRepository.java` L21 주석 | organization 언급 제거 |
| 14 | `Application.java` program LAZY | EAGER 승격 (§4) |
| 15 | `Bookmark.java` program LAZY | EAGER 승격 (§4) |
| 16 | `ApplicationRepository.findAllByUserOrderByAppliedAtDesc` @EntityGraph | `program.center` 추가 |
| 17 | `ApplicationRepository.findWithProgramAndUserById` @EntityGraph | `program.center` 추가 |
| 18 | `BookmarkRepository.findAllByUserOrderByCreatedAtDesc` @EntityGraph | `program.center` 추가 |
| 19~32 | 사용자 템플릿 14곳 `${...organization}` | `${...center?.name}` 또는 DTO `centerName` 접근자 |
| 33~36 | admin 템플릿 4곳 | `${p.center.name}` |
| 37 | `admin-csv.ts` L16 헤더 | `centerName` |
| 38 | `ProgramCenterBackfill` 러너 | 삭제 (Q-A9-b-3) |
| 39 | `ProgramCenterBackfillTest` 3 TC | 삭제 (Q-A9-b-3) |

Prod 코드에 남아 있던 `Program::getOrganization` / `.organization(...)` 는 A9-a 리팩토링 시 이미 대부분 정리되었고, A9-b 는 test fixture 정정 + 잔여 참조 제거 + 마이그레이션 배포 조치를 담당한다.

---

## 6. 검증 결과

### 정적 검증

- `./gradlew compileJava compileTestJava` — PASS
- `./gradlew test` — **655/656 PASS**
  - 1건 실패: `YouthMoaApplicationTests.contextLoads` — Testcontainers Docker 미기동 (로컬 회사 PC Docker Desktop 데몬 down 상태). A9-b 스코프 무관 환경 이슈. **CI ubuntu 러너에서 Docker 내장이라 자동 통과 예정**
- spotless: 미검사 (별도 job)

### 동적 검증 (회사 PC bootRun e2e 8090)

| URL | 결과 |
|---|---|
| GET `/` | 200 OK · Thymeleaf 잔재 없음 |
| GET `/programs` | 200 OK |
| GET `/programs/1` | 200 OK · center 이름 렌더 확인 (`청년센터` 매칭) · `organization` 표현식 잔재 없음 |
| GET `/css/main.css` | 200 OK |
| GET `/mypage/history`, `/mypage/favorites` | 302 (인증 필요 · 예상 동작) |

### 인터랙션 검증

- 정적 검증 범위 내 (템플릿 렌더 실측 = MyPageRenderTest · PageRenderIntegrationTest · AdminUserControllerRbacTest) 로 커버
- 별도 Playwright chromium 실행은 로그인 세션 부담 커서 skip. CI Playwright job 이 대신 커버

---

## 7. 알려진 gap / 후속 조치 후보

1. **Application/Bookmark EAGER 승격의 성능 영향** — 현재 UI 는 항상 program+center 를 함께 표시하므로 자연스럽지만, 미래에 program 정보 없이 Application 만 조회하는 배치성 쿼리가 생기면 명시적 projection 이 필요할 수 있다.
2. **fetch join cardinality 주의** — `@EntityGraph` 에 컬렉션이 없으므로 (ManyToOne 만) N+1 · MultipleBagFetchException 위험 없음.
3. **Q-A9-b-3 의 backfill 러너 삭제** — 신규 환경 부팅 시 V20 신규 컬럼이 nullable 로 생성 후 즉시 V21 이 NOT NULL 승격을 강제. 기존 DB 는 A9-a 후 backfill 이 완료된 상태여야 V21 이 성공. **롤백 시나리오**: V22 후 organization 을 되살리려면 새 마이그레이션 (V23) 을 추가해야 한다.
