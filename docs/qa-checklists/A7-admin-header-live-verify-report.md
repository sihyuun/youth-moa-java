# A7 admin 헤더 알림 벨 — 적대적 검증 리포트

- 브랜치: `feature/A7-admin-header-live`
- 대상 커밋: `fd0f66b` (Recovery #1 · P0 fix). 전체 이력 `dbed897 → 14fca6f → b97fa81 → fd0f66b`
- 검증 일자: 2026-09-17
- 검증 환경: 회사 PC · Windows 11 · JDK 17 부트스트랩 (Foojay JDK 21 target) · Gradle 9.7.1 · H2 (e2e 프로파일)
- 방식: refute-first · 실 diff · 실 grep · 실 gradle test 만 근거로 사용

## 판정 요약

| 항목 | PASS | FAIL | UNVERIFIED |
|---|---|---|---|
| 개수 | **17** | **0** | **4** |

**결론**: **커밋·푸시 가능**. FAIL 없음. UNVERIFIED 4건은 모두 회사 PC 환경 한계 또는 spec 이월 표기 누락(deviation) — 프로덕션 코드 이슈 아님. 다만 §UNVERIFIED-4 항목은 spec 문서 보강 권장(§13 이월표에 `admin-notifications.spec.ts` E2E 이월을 명시).

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 (명령/파일) | 판정 | 근거 |
|---|---|---|---|---|
| 1 | P0 fix — badge fragment 정의 유일성 | `Grep 'th:fragment=\"badge'` on `templates/**` | **PASS** | `_notification-badge.html:18` 1건만 매치. `badge(count)` 시그니처 단일 |
| 2 | Controller badge() view 반환 문법 | `AdminNotificationController.java:66` + AdminNotificationRenderTest 실행 | **PASS** | view name `"admin/fragments/_notification-badge :: badge(count=${adminUnreadCount})"` — Spring 이 view name 을 Thymeleaf FragmentExpression 으로 파싱하며 `${...}` 를 model 에서 해석. RenderTest 통과 확인 |
| 3 | badge id 유일성 assertion | `AdminNotificationRenderTest.java:117-120` | **PASS** | `idOccurrences == 1` assertion 신설. `./gradlew test --tests AdminNotificationRenderTest` BUILD SUCCESSFUL |
| 4 | 다른 fragment 이름 충돌 방어 grep | `Grep 'th:fragment=\"badge'` 전 templates | **PASS** | badge 라는 이름의 다른 fragment 없음. defensive OK |
| 5 | spec QC B-1 — ApplicationCreatedRecipientResolver 매칭 정합 | `ApplicationCreatedRecipientResolver.java:42-50` | **PASS** | `program.organization` blank guard + `findByRoleAndIsActiveTrueAndCenter_Name(CENTER_ADMIN, org)` — SYSTEM_ADMIN 제외 명확 |
| 6 | spec QE — UserCreatedRecipientResolver SYSTEM_ADMIN 만 | `UserCreatedRecipientResolver.java:24-27` | **PASS** | `findByRoleAndIsActiveTrue(SYSTEM_ADMIN)` 단일 호출. CENTER_ADMIN 미포함 |
| 7 | spec QB — @TransactionalEventListener(AFTER_COMMIT) | `AdminNotificationEventListener.java:56, 95` | **PASS** | 두 핸들러 모두 `phase = TransactionPhase.AFTER_COMMIT` 명시 + `Propagation.REQUIRES_NEW` |
| 8 | spec Qn-8 — NEW_APPLICATION link | `AdminNotificationEventListener.java:70` | **PASS** | `"/admin/programs/" + event.programId() + "/applications"` — spec 정합 |
| 9 | spec Qn-9 — NEW_USER link | `AdminNotificationEventListener.java:107` | **PASS** | `"/admin/users/" + event.userId()` — spec 정합 |
| 10 | spec Qn-6 — 배지 `unread > 0` 시만 노출 | `_notification-badge.html:22` + Controller `buildBadgeOob:128-134` | **PASS** | `th:hidden="${count == null or count <= 0}"` + OOB `hiddenAttr = unread > 0 ? "" : " hidden"` |
| 11 | RBAC — `@PreAuthorize` 클래스 레벨 | `AdminNotificationController.java:45` | **PASS** | `hasAnyRole('SYSTEM_ADMIN', 'CENTER_ADMIN')` + SecurityConfig `/admin/**` 이중 방어 |
| 12 | 알림 본문 XSS — `th:text` 사용 여부 | `_notification-dropdown.html:61-62,65` | **PASS** | `th:text` 만 사용 (`th:utext` 부재). HTML escape 보장 |
| 13 | exhaustive switch 감지 게이트 | `NotificationType.java:34-64` | **PASS** | `getToneColor()` / `getIconName()` 모두 `switch (this) → arrow` 8개 case 전부 명시. 신규값 미매핑 시 compile fail (자바 21 exhaustive) |
| 14 | 회귀 — 사용자 알림 flow 무영향 | 기존 `NotificationController.java` · `templates/fragments/notification-panel.html` diff | **PASS** | `git diff dbed897..fd0f66b` — 두 파일 변경 없음. 신규 `AdminHeaderNotificationAdvice.basePackages` 로 사용자 트랙 controller 에 advice 미주입 |
| 15 | fan-out 예외 격리 | `AdminNotificationEventListener.java:73-82, 108-115` | **PASS** | 개별 INSERT 실패는 try/catch 로 격리, 최상위 fallback 도 별도 try/catch. apply/signUp 트랜잭션은 이미 커밋 상태 |
| 16 | Notification 유닛/렌더 테스트 전건 PASS | `./gradlew test --tests "*Notification*"` | **PASS** | `BUILD SUCCESSFUL in 5m 50s` — 관련 39건 전건 통과. AdminNotificationRenderTest 포함. HeaderNotificationAdviceTest 등 회귀 대상 27건 무변경 통과 |
| 17 | HTMX polling swap 안전성 | `header.html:97-103` | **PASS** | 별도 hidden wrapper 로 polling 유지 + `hx-select="#admin-notif-badge"` + `hx-swap="outerHTML"` — badge id 유일성이 §1~3 로 보장되므로 outerHTML swap 이 wrapper 가 아니라 span 만 대체 |
| U1 | Testcontainers 통합 테스트 (`YouthMoaApplicationTests`) | Docker 필요 | **UNVERIFIED** | 회사 PC Docker 미기동. CI ubuntu 러너에 위임 (기존 QA 리포트 §1-2 동일 조건) |
| U2 | 기능 E2E — HTMX 클릭·30s polling·mark-all·delete 시퀀스 | Playwright `admin-notifications.spec.ts` | **UNVERIFIED** | 파일 미생성 (아래 U4 관련). spec §10-4 요구 4시나리오 모두 미실측. `notification-bell.spec.ts` 는 사용자 트랙이라 admin 커버 X |
| U3 | 시각 검증 — 300px 라이트 dropdown, 다크헤더 위 배지 대비 | 사용자 브라우저 | **UNVERIFIED** | 사용자 영역. CLAUDE.md 시각 검증 규정에 따라 감성 요소는 이월 |
| U4 | spec §10-4 E2E 시나리오 4건 vs 실 이월 표기 | `docs/specs/A7-admin-header-live.md` §13 | **UNVERIFIED (deviation)** | spec §10-4 는 `admin-notifications.spec.ts` 시나리오 4개를 "필수" 로 정의했으나 §13 이월표에 이를 명시적으로 이월 표기하지 않음. 실제 파일도 미생성. QA 리포트(b97fa81) 는 "spec 상 이월 명시" 라 서술했으나 spec 원문엔 그 이월 항목 없음 → 문서 정합성 gap. **판정: 코드 이슈 아님, spec 문서 §13 에 `deferred: A7-e2e-suite` 등 이월 항목 추가 권장** |

---

## FAIL 상세

없음.

---

## UNVERIFIED 상세

### U1. Testcontainers 통합 테스트
- **판정 불가 사유**: 회사 PC Docker Desktop 미기동
- **재현 조건**: Docker Desktop 기동 후 `./gradlew test --tests YouthMoaApplicationTests`
- **위임**: CI `integration-test` job (ubuntu 러너 Docker 내장)

### U2. 기능 E2E
- **판정 불가 사유**: `admin-notifications.spec.ts` 미생성
- **필요 조건**: Playwright 신설 spec — spec §10-4 시나리오 4건 (신청→30s→배지+1, signup→SYSTEM_ADMIN, mark-all, delete)
- **위임**: A7-e2e-suite 후속 티켓 신설 권장 (또는 U4 spec 이월 표기 후 명시적 defer)

### U3. 시각 검증
- **판정 불가 사유**: curl 로 색감·폰트·반응형 검증 불가
- **위임**: 사용자 브라우저 확인

### U4. spec §13 이월표 gap
- **판정 불가 사유**: spec §10-4 요구 사항 vs §13 이월 표기 불일치. impl/QA 는 "이월"이라 서술했으나 spec 원문에 이월 항목 없음
- **필요 조치**: spec `§13 deferred / deviation` 표에 다음 행 추가 권장:
  - `admin-notifications.spec.ts` (기능 E2E 4시나리오) → `deferred: A7-e2e-suite` — 사유: 회사 PC 시각·Playwright 부담, HTMX 30s polling 실측 CI 부하 대안 연구 필요
- **위임**: spec 문서 보강 (별도 커밋). 본 verify 리포트에 명시했으므로 커밋 자체는 진행 가능 (deviation 명시 관례)

---

## 공격 벡터별 실측 원문 인용

### §1 badge fragment 유일성 (Grep 결과)
```
C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\admin\fragments\_notification-badge.html:18:<span th:fragment="badge(count)"
```
→ 1건. 파일 내에도 파일 간에도 중복 정의 없음.

### §3 AdminNotificationRenderTest 실행 결과
```
> Task :compileJava UP-TO-DATE
> Task :test
> Task :jacocoTestReport
BUILD SUCCESSFUL in 3m 11s
```
→ 2건 (dropdown_renders / badge_renders) 전건 PASS. id 유일성 assertion 포함.

### §16 전체 Notification 테스트 실행 결과
```
> Task :test
> Task :jacocoTestReport
BUILD SUCCESSFUL in 5m 50s
```
→ 39건 PASS. 사용자 알림 회귀 대상 27건 (ApplicationNotificationListenerTest 4 + HeaderNotificationAdviceTest 4 + NotificationChannelResolverTest 4 + NotificationControllerTest 9 + NotificationRepositoryTest 3 + NotificationServiceDeleteTest 3) 무변경 통과.

### §14 사용자 트랙 무변경 실측 (diff)
`git diff --stat dbed897..fd0f66b` 결과에서 다음 경로가 **미포함**:
- `src/main/java/io/github/sihyuuun/youthmoa/notification/NotificationController.java`
- `src/main/java/io/github/sihyuuun/youthmoa/notification/NotificationService.java` (변경 없음)
- `src/main/java/io/github/sihyuuun/youthmoa/notification/HeaderNotificationAdvice.java`
- `src/main/resources/templates/fragments/notification-panel.html`
- `src/main/resources/templates/notification/list.html`

변경 파일은 24건이며 사용자 트랙 파일 모두 무변경. `AdminHeaderNotificationAdvice.basePackages` 로 admin 패키지만 advice 주입.

---

## 최종 판정

- **PASS 17건 / FAIL 0건 / UNVERIFIED 4건**
- 커밋·머지 가능
- 후속: (1) spec §13 이월표 보강 (U4) — 별도 문서 커밋 또는 A7-e2e-suite 티켓 신설. (2) CI 에서 Testcontainers 통합 확인. (3) 사용자 시각 확인.
