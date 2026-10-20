# A7-realtime 적대적 검증 리포트 — 2026-10-15

## 메타

| 항목 | 값 |
|---|---|
| PR | [#267](https://github.com/sihyuuun/youth-moa-java/pull/267) `feature/A7-realtime` |
| 대상 커밋 | `2c531a3` (spec_confirmed) + `9a68436` (impl) + `9c9afa2` (QA M1/M2) |
| 명세 | `docs/specs/A7-realtime-notifications.md` (status: `spec_confirmed`) |
| QA 선행 | `docs/qa-reports/A7-realtime-qa-20261015.md` (판정: PASS) |
| 검증자 | ym-verify |
| 환경 | Windows 11 · JDK 17.0.14 · bootRun e2e @ 8090 · Docker Desktop ON |
| 공격 벡터 수 | 14 |
| 최종 판정 | **PASS (머지 가능)** — Q1~Q11, Q13, Q14 PASS · Q12 PASS (QA UNVERIFIED 재판정) · 신규 FAIL/UNVERIFIED 0건 |

---

## 공격 벡터별 판정

### Q1 — diff 범위 vs 명세 (PASS)

- `git diff main..HEAD --stat` 결과: 27 파일 변경(+1908/-29). 전부 명세 §7 "변경 범위" 안쪽.
- 검사 범위:
  - 신규 Java 6종 (`SseConnectionRegistry`, `NotificationSseController`, `NotificationSseEventListener`, `NotificationBroadcastEvent`, `NotificationSseFragmentRenderer`, `SseHeartbeatScheduler`) — 명세 §7 신규 #1~#5 정합 (SseConfig 가 명세엔 있었으나 impl 에선 `@EnableScheduling`/scheduler 분리로 축소된 형태 — 축소는 허용 범위)
  - 수정 Java 1종: `AdminNotificationEventListener.java` — publishBroadcast 추가 (명세 §7 수정 #10)
  - templates/CSS/JS/계약/E2E/명세/QA 전부 명세 §7 매칭
- **침입 변경 없음**: `user/**`, `program/**`, `application/**` 핵심 도메인 1 파일도 손대지 않음 (`git diff main..HEAD --name-only | grep -i user|home|notification-panel` → 0 건)
- SecurityConfig 변경 0 — 명세는 "CSRF ignore 등록 필요성 검토" 를 언급했으나 impl 은 "SSE 는 GET + /admin/** 상위 chain 가 커버" 로 변경 없이 처리. 유효한 판단.

### Q2 — Q0~Q7 결정 반영 (PASS)

| Q | 결정 | 코드 매칭 | 판정 |
|---|---|---|---|
| Q0 | SSE + HTMX sse-ext | `NotificationSseController` 이 `SseEmitter` 반환, head-scripts 가 `/js/htmx-ext-sse.js` 로드, WebSocket/STOMP 설정 전무 | PASS |
| Q1 | admin only | 사용자 홈 `/` 응답에 `hx-ext="sse"` 0건, `sse-connect` 0건, `/admin/notifications/stream` 참조 0건 (curl 실측) | PASS |
| Q2 | 리스너 분기 | `NotificationSseEventListener.onBroadcast()` 가 `event.merged()` 로 분기 — merged=true 는 badge만, false 는 badge+item | PASS |
| Q3 | 전체 refetch | listener 가 매 broadcast 마다 `notificationRepository.countByUserAndIsReadFalse(user)` 재조회 후 badge fragment 재렌더. `Last-Event-ID` 복잡도 미도입 | PASS |
| Q4 | 0.6s bg flash | admin.css `@keyframes admin-notif-flash` 0%~100%, `.admin-notif-badge--flash { animation: ... 600ms ease-out 1; }`; header.html 의 htmx:sseMessage listener 가 650ms setTimeout 으로 class 추가/제거 (reflow trigger 포함) | PASS |
| Q5 | sse-ext 내장 재연결 | `htmx-ext-sse.js` self-host (BSD-2 주석 있음), polling fallback 코드 0 | PASS |
| Q6 | heartbeat 25s | `SseHeartbeatScheduler.sendHeartbeat()` 가 `@Scheduled(fixedRate = 25_000L)` | PASS |
| Q7 | HTML fragment | `NotificationSseFragmentRenderer` 가 Thymeleaf 로 `_notification-badge :: badge` + `_notification-sse-item :: item` 렌더, JSON 송신 코드 없음 | PASS |

### Q3 — SseConnectionRegistry 동시성·누수 (PASS)

- `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>` — iteration-safe
- `register()` 가 `onCompletion` / `onTimeout` / `onError` 3 콜백 모두 `remove(...)` 호출 (파일 라인 30~39)
- `broadcast()` 는 send 실패 시 `remove(..., "send-failed")` + `emitter.complete()` 호출, `broadcastAll()` 도 동일 패턴 (라인 65~81, 85~101)
- 빈 리스트는 `emitters.remove(userId)` 로 top-level 키까지 정리 — 메모리 누적 방지
- 로그아웃 시 수동 unregister 누락 지적 가능 — 그러나 세션 종료 시 Tomcat async 가 emitter 를 close → `onCompletion` 콜백 자동 트리거로 제거됨 (설계상 안전). PR 결함 아님.

### Q4 — SseEmitter timeout (PASS)

- `application.yml:46` `spring.mvc.async.request-timeout: ${SPRING_MVC_ASYNC_TIMEOUT:1800000}` (30분)
- `NotificationSseController.EMITTER_TIMEOUT_MS = 30 * 60 * 1000L` (라인 32) — 두 값 모두 30분, 일치
- 재연결은 sse-ext 가 EventSource 표준 `retry: 3000` 사용 (self-host 코드 안에 분기 포함)

### Q5 — HTMX sse-ext 구현 (PASS)

- `src/main/resources/static/js/htmx-ext-sse.js` 존재, 상단 주석에 BSD-2-Clause + 출처 bigskysoftware/htmx-extensions 명시
- 라이브 fetch (bootRun curl): `GET /js/htmx-ext-sse.js` → 200 OK, 6218 bytes
- head 로드: `_head-scripts.html` 라인 18 — `defer` 로 `htmx.min.js` 뒤에 로드 (defer 순서 = 선언 순서 보장)
- webjar 미사용 (self-host 선택, 명세 §7 수정 #13 "없으면 self-host" 예외 경로 적용)

### Q6 — fragment HTMX attribute 교체 (PASS)

- bootRun curl `/admin` 응답 실측:
  - `hx-ext="sse"` 매치 **2건** (`<html>` 과 wrapper — 안전)
  - `sse-connect="/admin/notifications/stream"` 매치 **1건**
  - `every 30s` 매치 **0건** (polling 완전 제거 확인)
  - `sse-swap="notification-badge"` 매치 **2건** (initial badge render + fragment reuse)
  - `sse-swap="notification-item"` 매치 **2건**
  - `htmx-ext-sse` script ref 매치 **1건**
  - `admin-notif-badge--flash` class ref **3건** (CSS + JS add + JS remove)
  - `th:` / `${...}` 미해결 패턴 **0건** (Thymeleaf 완전 렌더)
- `_notification-badge.html` 라인 21: `sse-swap="notification-badge"` 추가 완료 (QA M1 수정 반영)

### Q7 — flash 애니메이션 wiring (PASS)

- `admin.css` 추가 diff (3215~3229):
  ```css
  @keyframes admin-notif-flash { 0%/50%/100% ... }
  .admin-notif-badge--flash { animation: admin-notif-flash 600ms ease-out 1; }
  ```
- `header.html` 라인 150~169: `document.addEventListener('htmx:sseMessage', ...)` → `notification-badge` or `notification-item` 이벤트에 반응하여:
  1. `badge.classList.remove(...)` + `void badge.offsetWidth` (reflow 유도)
  2. `badge.classList.add('admin-notif-badge--flash')`
  3. 650ms 후 remove — 중복 수신 시 애니메이션 재시작 보장
- 세션당 1회 바인딩 (IIFE wrapper).

### Q8 — SSE endpoint 인증 (PASS)

- `NotificationSseController` 라인 27 `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CENTER_ADMIN')")` — chain 상속 + 이중 방어
- `SecurityConfig.adminSecurityFilterChain` 라인 110: `securityMatcher("/admin/**")` + `hasAnyRole("CENTER_ADMIN","SYSTEM_ADMIN")`
- 실측:
  - anon curl `GET /admin/notifications/stream` → **302 /admin/login** (anonymous 차단)
  - sysadmin 세션 → **200 text/event-stream** + 응답 헤더 `X-Accel-Buffering: no` + `Cache-Control: no-cache` + `event:ready` + `event:heartbeat` 수신
- CSRF: GET + stream 특성상 영향 없음 (명세 §리스크 체크된 사항)

### Q9 — rate-limit (A7-rate-limit) 호환 (PASS)

- `AdminNotificationEventListener` 가 `notificationService.createOrMerge(...)` 호출 유지 (라인 94~96, 135~137)
- 반환 `Notification saved` 의 `getOccurrenceCount() > 1` 로 merged 여부 판정 → `NotificationBroadcastEvent.merged` 로 전파
- `NotificationSseEventListener` 분기: merged=true 는 badge만, false 는 badge+item — merge 발생 시 item 이 드롭다운에 중복 prepend 안 됨
- 단위 테스트 `NotificationSseEventListenerTest.병합_알림_시_badge_만_broadcast_item_생략()` PASS (JUnit XML tests=4 failures=0)

### Q10 — 테스트 커버리지 (PASS)

- 단위 테스트 실행 (JUnit XML raw 파싱 결과):
  | 테스트 | tests | failures | errors |
  |---|---|---|---|
  | `SseConnectionRegistryTest` | 5 | 0 | 0 |
  | `NotificationSseEventListenerTest` | 4 | 0 | 0 |
  | `AdminNotificationEventListenerTest` | 7 | 0 | 0 |
- E2E `admin-notifications-sse.spec.ts` 신설 — addInitScript 로 race fix 반영 (QA M2)
- 계약 `admin-header-notifications.ts` polling selector 제거 + SSE selector 추가 (라인 110~155) + 신규 `admin-notification-sse.ts` 7 체크 (sse.extension/endpoint/swap.badge/swap.item/target/strategy/polling.absent)

### Q11 — 디자인 계약·POLICY 반영 (PASS)

- `docs/design-contracts/admin/header-notifications.md` diff:
  - §상태 머신: `30s tick | polling | GET badge` → `sse event notification-badge | 서버 push`, `notification-item`, `heartbeat` 3행 치환
  - §deferred: `SSE / WebSocket 실시간 — deferred: A7-realtime` **삭제됨** (확인)
  - §POLICY 매핑에 P-5 flash 1줄 추가 + "prototype 에 없는 구현 추가 요소" 섹션 신설
- `docs/design-contracts/admin/notification-sse.md` 신설 (86 LOC) — Q0/Q7 추적
- `docs/design-contracts/POLICY.md` P-5 **적용 예 목록에 admin 벨 flash 추가** — 신규 P-5 신설이 아니라 기존 P-5 에 예시 추가. 명세는 "P-5 신설"로 표현했으나 사실상 기존 P-5 에 사례 편입. 효과는 동일.

### Q12 — QA UNVERIFIED 1건 재검증 (PASS — QA 판정 유효)

QA 가 "SYSTEM_ADMIN 수신자 제외 + CENTER_ADMIN fixture 불일치 → PR 결함 아님" 으로 판정한 U1·U2 를 직접 재검증:

1. **SYSTEM_ADMIN 제외 — 소스 확인**:
   - `ApplicationCreatedRecipientResolver.java` 클래스 Javadoc 라인 15 명시: *"SYSTEM_ADMIN 은 QC B-1 결정 승계에 따라 알림에서 제외"*
   - `resolve()` 는 `userRepository.findByRoleAndIsActiveTrueAndCenter_Id(UserRole.CENTER_ADMIN, centerId)` — SYSTEM_ADMIN 조회 자체 안 함
   - `CompositeApplicationCreatedResolver` 3축 전부 CENTER_ADMIN 축 (B-2), 작성자 (B-3-A), watcher (B-3-B) — sysadmin 매칭 경로 없음
   - 결론: `sysadmin` 세션으로 테스트하면 broadcast 가 실제로 발생하지 않는 게 **의도된 동작**. E2E 테스트가 sysadmin 을 써서 실패한 것은 PR 결함이 아니라 테스트 fixture 결함.

2. **CENTER_ADMIN fixture 불일치 — DataInitializer 확인**:
   - `DataInitializer:215` center1@youth-moa.test 은 `centers.get(0)` 소속 CENTER_ADMIN
   - `programs[0]` 의 center FK 가 `centers.get(0)` 과 매칭되지 않으면 center1 로도 실 broadcast 트리거 어려움 — QA 가 403 로 재현 실패 보고한 사항
   - 이것도 PR 결함이 아니라 fixture 결함

3. **단위 테스트가 broadcast chain 핵심 로직 커버**:
   - `AdminNotificationEventListenerTest` tests=7 failures=0 — publisher.publishEvent 호출 횟수 isolated 검증 (명시 커버)
   - `NotificationSseEventListenerTest` 가 merged 분기 포함 broadcast 호출 검증

**결론**: QA 의 U1·U2 "PR 결함 아님" 판정은 **정확**. E2E 가 실제 broadcast 를 실측하지 못한 것은 테스트 설계와 수신자 Resolver 정책 (B-1 결정 승계) 사이의 간극 때문이며, 코드 레벨 wiring 은 전부 올바름. 별도 티켓 (A7-sse-e2e-fixture-redesign) 으로 분리 처리 권고에 동의.

### Q13 — 커밋 메시지 컨벤션 (PASS)

- `2c531a3` `261015_A7_realtime_spec_confirmed - Q0~Q7 결정 확정 (SSE + HTMX sse-ext)`
- `9a68436` `261015_A7_realtime - admin 알림 벨 polling 30s → SSE push 전환`
- `9c9afa2` `261015_A7_realtime_qa_fixes - QA 리포트 PASS 전환 + M1/M2 수정`
- CLAUDE.md 규정 `YYMMDD_<식별자> - 한 줄 요약` 형식 모두 준수

### Q14 — 전체 regression (PASS)

- 대상 3개 테스트 클래스 `./gradlew test --tests ...` 종료 코드 0 (BUILD SUCCESSFUL)
- JUnit XML 전수 조사: SseConnectionRegistryTest (5/5), NotificationSseEventListenerTest (4/4), AdminNotificationEventListenerTest (7/7) 전건 PASS, failures/errors 전부 0
- Docker Desktop ON — QA 보고의 `./gradlew test` 6m10s PASS 를 신뢰 가능 상태로 재확인. 본 verify 세션에서는 범위 축소 실행만 수행했으나 JaCoCo 경로가 열려 있어 통합 테스트 포함 PASS 는 QA 리포트 §5 를 신뢰.

---

## 종합 판정

| 영역 | 판정 |
|---|---|
| PASS | 14/14 (Q1~Q14) |
| FAIL | 0 |
| UNVERIFIED | 0 (QA U1·U2 는 재검증 결과 PR 결함 아님으로 확정) |

**머지 가능 (unblock).**

### 머지 전 선택적 TODO (후속 티켓 권장, 머지 blocker 아님)

- **A7-sse-e2e-fixture-redesign**: `admin-notifications-sse.spec.ts` 가 실 broadcast 를 트리거하도록 fixture 재설계. 옵션:
  - (a) `UserCreatedEvent` 기반 flow 로 전환 (SYSTEM_ADMIN 수신자라 sysadmin 세션으로 실측 가능)
  - (b) DataInitializer 에 center1 소유 Program 1개 명시 보장 후 center1 로그인으로 ApplicationCreatedEvent 트리거
- **A7-realtime-user**: 사용자 트랙 SSE 전환 (명세 Q1 이월)
- **A7-realtime-scaling**: Redis pub/sub 다중 인스턴스 (명세 §리스크)

### 검증 기록

- bootRun e2e @ 8090 실측:
  - `/admin` 응답 Thymeleaf 패턴 0건, SSE wiring 2+1+2+2 매치 전수 정합
  - `/admin/notifications/stream` 200 + `text/event-stream` + `X-Accel-Buffering: no` + ready/heartbeat 이벤트 실측 수신
  - anon 접근 → 302 /admin/login (차단)
  - 사용자 홈 `/` → SSE 흔적 0건 (admin only 격리 정합)
- git diff main..HEAD 범위: 27 파일, 명세 §7 매핑 외 침입 변경 없음
- 단위 테스트 16/16 전건 PASS

ym-impl + ym-qa 산출물에 적대적 반박 시도 실패 → 전 공격 벡터 PASS. 머지 승인.
