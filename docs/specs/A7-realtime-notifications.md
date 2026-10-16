# A7-realtime — 알림 벨 실시간 push (polling → SSE 전환)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-10-15 산출, Q0~Q7 사용자 일괄 승인) |

## 결정 요약 (사용자 확정 Q0~Q7)

- **Q0 = SSE + HTMX sse-ext** — WebSocket 대비 ~150~200 LOC, sticky session 선택, Fly.io 그대로 동작, HTMX `sse-swap` 자연 통합
- **Q1 = admin 벨만** — 사용자 트랙은 polling 자체가 없는 SSR 이라 전환 효과 극소, 이월
- **Q2 = 리스너 분기** — merge → badge 만 broadcast, 신규 → badge + item broadcast
- **Q3 = 전체 refetch** — 재연결 시 unread count 전체 refetch 로 단순화
- **Q4 = 0.6s bg flash 추가** — POLICY P-5 로 문서화 (prototype 미명시 deviation 표기)
- **Q5 = sse-ext 내장 재연결만** — polling fallback 코드 중복 회피
- **Q6 = heartbeat 25s** — nginx 60s · Fly.io idle 안전 범위
- **Q7 = HTML fragment** — HTMX swap + Thymeleaf fragment 재사용

| 항목 | 값 |
|---|---|
| 브랜치 (예정) | `feature/A7-realtime` |
| 선행 | A7-admin-header-live (PR #217) · A7-createdBy-recipient (#229) · A7-watcher-ui (#230) · A7-e2e-suite (#232) · A7-rate-limit (#235) · A7-SMTP-mail (#243) — **모두 머지 완료** |
| 성격 | 알림 벨 트랜스포트 교체 (HTMX polling 30s → **SSE**). fan-out · rate-limit · 수신자 Resolver · mail 는 모두 재사용 |
| 추정 규모 | **소~중** — 파일 10~14 · +400~600 LOC · V 파일 **없음** (스키마 변경 X) · Boot 4 SseEmitter + HTMX sse-ext |
| 리스크 | admin 트랙만 전환 vs 사용자 트랙 동시 전환 — Q1 결정 사항. 회귀 영향도 사용자 트랙 선택 시 급증 |

---

## 0. 디자인 계약 선독 (CLAUDE.md 0단계)

- 계약 존재: [`docs/design-contracts/admin/header-notifications.md`](../design-contracts/admin/header-notifications.md) + [`e2e/contracts/admin-header-notifications.ts`](../../e2e/contracts/admin-header-notifications.ts)
- 사용자 트랙 계약: [`docs/design-contracts/notifications.md`](../design-contracts/notifications.md) (`/notifications` 페이지 — 헤더 벨은 panel SSR fragment 하나로만 표현되므로 별도 계약 없음)
- **갱신 사항** (본 티켓이 계약을 손봐야 하는 범위):
  - `admin/header-notifications.md` §상태 머신 — `30s tick | polling | ...` 항목을 **`sse event "notification" | 서버 push | ...`** 로 교체
  - `admin/header-notifications.md` §deferred — `SSE / WebSocket 실시간 — deferred: A7-realtime` 를 **삭제** (본 티켓이 해소)
  - `e2e/contracts/admin-header-notifications.ts` — polling wrapper selector (`[hx-trigger="every 30s"]`) 를 **`[hx-ext="sse"]` + `[sse-connect]`** 로 교체. prototype 라인 인용 (`proto:L443~494`) 은 그대로 유지 (디자인은 무변경)
  - 신설 계약: `docs/design-contracts/admin/notification-sse.md` + `e2e/contracts/admin-notification-sse.ts` — SSE 전송 포맷·이벤트 타입·heartbeat 간격을 **서술 + 기계 검사** 로 고정

**0단계 결론**: 디자인 외형(배지·드롭다운·아이콘·링크)은 **완전 무변경**. 본 티켓은 **트랜스포트 교체** 작업이며 UI 는 벨 flash 애니메이션 1건만 추가 제안(Q4). 계약 변경은 "polling → SSE" 치환이 전부.

---

## 1. 배경 · 스코프

### 왜 전환하나
A7 축 알림 축이 2026-10-10 기준 완결됐지만 트랜스포트는 **HTMX polling 30s** 로 남아 있다. 결과:
- 사용자 신청 → 관리자 벨 반영까지 **최대 30초 지연**
- 관리자 다수(≥10) 접속 시 **30초마다 `GET /admin/notifications/badge` N건** 발생 (대부분 unread count 변화 없음)
- `A7-rate-limit` 5분 window 병합은 polling 전제가 아니라 서버 측 `createOrMerge` 로직이므로 **SSE 전환 시에도 그대로 재사용**

### 포함 (이번 티켓)
- Spring Boot 4 `SseEmitter` 기반 서버 push endpoint 신설 (`/admin/notifications/stream`)
- HTMX `sse-ext` (htmx 2.x 공식 확장) 도입 — 벨 fragment 를 `hx-ext="sse" sse-connect="/admin/notifications/stream"` 로 전환
- 이벤트 타입 2종 — `notification-badge` (unread count 변화) + `notification-item` (신규 알림 1건 메타)
- heartbeat (keep-alive) — 25s 주기 comment 발신 (프록시 timeout 방지)
- 연결 수명 관리 — `SseConnectionRegistry` (`ConcurrentHashMap<userId, List<SseEmitter>>`)
- 재연결 — SSE 표준 `retry: 3000` + `Last-Event-ID` 기반 유실 보정 (옵션, Q3)
- polling fallback — SSE 미지원·장애 시 자동 fallback (HTMX `sse-ext` 내장 or 수동, Q5)
- 벨 flash 애니메이션 — 신규 알림 수신 시 벨 배지 1회 flash (디자인 확장, Q4)

### 제외 (deferred 표기 필수)
- 사용자 트랙 (`/notifications` fragment) SSE 전환 — Q1 에서 **admin 만** 결정 시 `deferred: A7-realtime-user`
- WebSocket (양방향) 도입 — 이번 범위 X. 알림 벨은 서버→클라 단방향이라 SSE 로 충분
- Push Notification (브라우저 Web Push API) — `deferred: A7-webpush`
- 이메일 발송 (별도 알림 축) — SMTP 는 PR #243 로 완결 (A7-SMTP-mail)
- 전역 알림 수신 UI 재설계 (toast · 알림 센터) — `deferred: A7-notification-center`

---

## 2. 디자인 출처 (3자산 정독)

| 자산 | 위치 |
|---|---|
| admin prototype 벨 영역 | `docs/00_assets/admin/prototype.html` L443~494 (bell + dropdown + delete btn) |
| admin prototype mock 데이터 | 같은 파일 L2832~2836 (`notifItems` 3건) |
| 사용자 prototype 벨 | `docs/00_assets/prototype.html` 사용자 영역 (벨 자체는 정적 panel SSR) |
| 기존 계약 (admin) | `docs/design-contracts/admin/header-notifications.md` + `e2e/contracts/admin-header-notifications.ts` |
| 기존 계약 (사용자 `/notifications`) | `docs/design-contracts/notifications.md` + `e2e/contracts/notifications.ts` |
| HANDOFF | `docs/00_assets/HANDOFF.md` — 알림 벨 토큰 (`admin-notif-badge` `admin-notif-panel`) |

**prototype 영향도**: prototype 은 **React state** 로 알림을 관리하므로 트랜스포트 구분이 없다. "서버 push" 라는 개념도 prototype 에 없다 → **디자인 영향 0**. 본 티켓은 디자인 계약 신설 없이 기존 계약을 "polling → SSE" 로 **치환**하는 것이 전부.

**예외**: Q4 에서 벨 flash 애니메이션을 추가하면 prototype 에 없는 개선 → POLICY `P-5` 에 따라 화면 문서(계약 md)에 기록, prototype 는 수정하지 않는다.

---

## 3. 자산 간 갭 표 (3자산 비교)

| 항목 | prototype (admin L443~494) | prototype (user) | 현재 구현 | 채택 |
|---|---|---|---|---|
| 벨 아이콘 · 배지 · 드롭다운 외형 | 명시 (300px · #E72D0F · 1.5px border #111827) | 유사 (380px) | 계약 정합 | 변경 없음 |
| 트랜스포트 | 없음 (React state) | 없음 (SSR) | admin: polling 30s · user: SSR panel 그대로 | **admin → SSE**. user Q1 결정 |
| 신규 알림 즉시 표시 | React re-render | — | polling 30s 이후 | **SSE 로 즉시** |
| 재연결 처리 | React 자동 | — | HTMX polling 자동 재시도 | SSE 자동 재연결 (`EventSource` 표준) |
| 유실 방지 | prototype 없음 | — | polling 다음 tick 에서 catch-up | **`Last-Event-ID`** or 재연결 시 최신 unread count fetch |
| 벨 flash | prototype 없음 | 없음 | 없음 | **Q4 — 추가 제안** (prototype 미명시) |
| rate-limit (5분 병합) | prototype 없음 | 없음 | 서버 측 `createOrMerge` | **무변경 재사용** (SSE 트리거는 병합된 알림 1건만 push) |

**갭 결론**: prototype 과 "실시간성" 사이의 축이 다르므로 비교 축이 없다. 본 티켓은 prototype 외부 결정 사항(엔지니어링) 이고 디자인 책임은 Q4 벨 flash 1건으로 제한된다.

---

## 4. 데이터 모델 gap 표

**SSE 전환은 스키마 변경이 없다**. 아래 표는 "확인했고 변경 없음" 을 명시한다.

| 필드 | 현재 엔티티 (`Notification.java`) | 조치 |
|---|---|---|
| `id` / `user` / `type` / `title` / `message` / `link` / `isRead` | ✅ 존재 | 무변경 |
| `createdAt` / `lastOccurredAt` | ✅ 존재 (A7-rate-limit) | 무변경 — SSE 이벤트 id 로 `lastOccurredAt.toEpochMilli()` 사용 가능 (Q3) |
| `dedupKey` / `occurrenceCount` | ✅ 존재 (A7-rate-limit) | 무변경 — SSE push 는 병합 **후** 트리거 |

**신규 저장 테이블** (`sse_connection_log` 등): **불필요**. `SseEmitter` 는 메모리 내 관리로 충분. 연결 끊김 시 자동 GC. 운영 관측은 `/actuator/metrics` 로 Micrometer gauge 노출(§6-C) 로 커버.

### V 파일
**없음**. CLAUDE.md "스키마 변경 없는 PR 은 V 파일 금지" 원칙 준수.

---

## 5. 데이터 소비 지점 리스트

| 소비 지점 | 현재 트랜스포트 | 본 티켓 전환 |
|---|---|---|
| admin 헤더 배지 (`#admin-notif-badge`) | HTMX polling 30s (`GET /admin/notifications/badge`) | **SSE event `notification-badge` 로 교체** |
| admin 드롭다운 (`#admin-notif-dropdown`) | 벨 클릭 시 `GET /admin/notifications/dropdown` (유지) | **클릭 fetch 유지** — SSE 는 드롭다운이 열려 있을 때 신규 항목 OOB prepend 만 담당 |
| admin 전체 목록 페이지 | 없음 (deferred) | 범위 밖 |
| 사용자 헤더 벨 (`fragments/notification-panel.html`) | **SSR fragment · polling 없음** | **Q1 결정 필요** — admin 만 전환 vs 사용자도 전환 |
| 사용자 `/notifications` 페이지 | SSR | 변경 없음 (페이지 재로드가 자연스러운 UX) |

### write→read 왕복 시나리오 (각 소비 지점별)

1. **admin 신규 신청 → 벨 flash + 배지 +1**: 사용자 A `POST /programs/{id}/apply` 성공 → `ApplicationCreatedEvent` → `AdminNotificationEventListener.onApplicationCreated` → `createOrMerge` INSERT → **신규 이벤트 `NotificationBroadcastEvent` 발행** → `SseConnectionRegistry` 가 수신자 관리자 각각의 emitter 로 `event: notification-badge` + `event: notification-item` 전송 → 브라우저 `EventSource` 가 즉시 수신 → HTMX `sse-ext` 가 배지 OOB swap + 벨 flash 트리거

2. **admin rate-limit 병합 이벤트**: 5분 window 안 2번째 신청 → `createOrMerge` 가 기존 row `mergeOccurrence(count++, lastOccurredAt=now, isRead=false)` → **같은 SSE 이벤트 발행** (dedup 완료 상태의 1건) → 배지 재증가 (0→1 재알림) + 드롭다운 열려 있으면 상단 항목 title 이 `[n건] ...` 로 교체

3. **드롭다운 열린 상태에서 SSE 수신**: 사용자가 벨 클릭해 드롭다운 열어둠 → 새 신청 도착 → SSE 가 **`event: notification-item`** 로 fragment HTML 1건 전송 → `sse-ext` 가 `#admin-notif-list` 상단 prepend → 배지 OOB swap

4. **연결 끊김 후 재연결**: 네트워크 끊김 → `EventSource` 자동 재연결 (브라우저 기본 3s) → `Last-Event-ID` 헤더로 재전송 → 서버는 그 이후 발행된 알림을 catch-up (Q3 결정 필요)

---

## 6. 기술 선택지 비교 (Q0 핵심)

### 6-A. SSE vs WebSocket vs HTMX SSE ext

| 축 | WebSocket (STOMP) | **SSE (Server-Sent Events)** | HTMX sse-ext (SSE + fragment swap) |
|---|---|---|---|
| 방향 | 양방향 | **단방향 (서버→클라)** | 단방향 (SSE 위에 fragment swap) |
| Spring Boot 4 지원 | `spring-boot-starter-websocket` + STOMP | **`SseEmitter` · Boot 2.x 부터 안정 · Boot 4 무변경** | SSE 그대로 사용 (클라이언트만 HTMX 확장) |
| 프로토콜 | WS (상위 TCP) | **HTTP/1.1 chunked · HTTP/2 stream** (표준) | HTTP (SSE 와 동일) |
| 자동 재연결 | STOMP 플러그인 or 수동 | **표준 (EventSource 내장)** | 표준 + HTMX 로그 통합 |
| 유실 방지 | STOMP ACK or 수동 | **`id:` 필드 + `Last-Event-ID` 헤더** | 같음 |
| 프록시 호환 | 일부 (sticky session 필수 · nginx 설정 복잡) | **HTTP 그대로 통과** (단 buffering off 필요) | 같음 |
| 서버 비용 | 커넥션 1개당 JVM thread or NIO channel | **Boot 4 async: NIO channel 1개 · thread 미점유** | 같음 |
| LOC (예상) | ~400 (STOMP config · broker · subscribe) | **~200** (SseEmitter + Registry + event listener) | **~150** (서버 LOC 는 SSE 와 같고 클라이언트 템플릿 LOC 가 ~30 로 축소) |
| 복잡도 | 高 (ARG WS endpoint · broker · subscription) | 中 (SseEmitter · Registry · heartbeat) | **低~中** (HTMX 가 fragment swap · OOB 자동) |
| HTMX 통합 | 수동 (JS 로 WS 수신 후 htmx.swap API 호출) | 수동 (JS 로 EventSource + swap) | **자연 통합** (`sse-swap` 속성 하나) |
| 테스트 복잡도 | 高 (STOMP 클라이언트 필요) | 中 (`EventSource` 폴리필 or httpx stream) | 중 |
| Playwright 테스트 | 가능하나 복잡 | **`page.evaluate(() => new EventSource(...))`** | **직접 fragment swap 결과 검증** |

**권장**: **HTMX sse-ext 기반 SSE** — 서버는 표준 `SseEmitter`, 클라는 `hx-ext="sse"` 로 fragment swap 재사용. Fly.io · 일반 HTTP 프록시와 호환되며 LOC 최소.

### 6-B. 각 선택지의 운영성

| 항목 | WebSocket | SSE | HTMX sse-ext |
|---|---|---|---|
| sticky session | **필수** (STOMP broker 상태) | 선택적 (stateless 가능 — 단 Registry 는 인스턴스 별) | 같음 |
| Fly.io 호환 | 가능 (WS 지원) · sticky session 설정 필요 | **그대로 동작** · `Idle Timeout` 만 heartbeat 보다 길게 | 같음 |
| Supabase 영향 | 없음 (DB 는 그대로) | 없음 | 없음 |
| 수평 확장 | STOMP broker (Redis Pub/Sub 등) | **인스턴스 간 pub/sub 필요** (옵션: 현재 단일 인스턴스라 미도입) | 같음 |
| 연결 수 상한 | JVM thread pool (STOMP) 또는 NIO | **async request 당 thread 미점유** (Tomcat NIO) | 같음 |
| 메시지 유실 방지 | STOMP ACK | **`id` + `Last-Event-ID`** or 재연결 시 재조회 | 같음 |

### 6-C. 관측·운영 — Micrometer

- `youthmoa.notification.sse.connections.active` (gauge) — 활성 SSE emitter 수
- `youthmoa.notification.sse.events.sent` (counter · tag: `event_type`) — 발신 이벤트 수
- `youthmoa.notification.sse.disconnect.reason` (counter · tag: `reason=complete|timeout|error`) — 끊김 사유 분포
- `/actuator/health` 에 **readiness contributor 없음** (SSE 미지원이라고 전체 unhealthy 처리하면 안 됨) — 자체 `/actuator/metrics/youthmoa.notification.sse...` 로 관측만

---

## 7. 변경 범위 (파일 단위)

### 신규

| # | 파일 | 역할 |
|---|---|---|
| 1 | `notification/sse/SseConnectionRegistry.java` | `ConcurrentHashMap<Long userId, List<SseEmitter>>` · add/remove/broadcast |
| 2 | `notification/sse/NotificationSseController.java` | `GET /admin/notifications/stream` · `SseEmitter` 반환 · heartbeat scheduler · `Last-Event-ID` 처리 |
| 3 | `notification/sse/NotificationBroadcastEvent.java` | 신규 Spring event record (recipientUserId, Notification, eventKind) |
| 4 | `notification/sse/NotificationSseEventListener.java` | `@EventListener` NotificationBroadcastEvent → Registry.broadcast 호출 |
| 5 | `notification/sse/SseConfig.java` | async task executor + heartbeat scheduler bean + 설정 property binding |
| 6 | `templates/admin/fragments/_notification-sse-item.html` | SSE push 시 리스트에 prepend 될 단일 item fragment (기존 `_notification-dropdown.html` 의 `<li>` 추출) |
| 7 | `docs/design-contracts/admin/notification-sse.md` | SSE 전송 포맷 · 이벤트 종류 · heartbeat · 재연결 서술 계약 |
| 8 | `e2e/contracts/admin-notification-sse.ts` | SSE endpoint 응답 Content-Type · `event:` 라인 포맷 기계 검사 |
| 9 | `e2e/tests/admin-notifications-sse.spec.ts` | Playwright 기능 E2E (연결·수신·재연결·heartbeat) |

### 수정

| # | 파일 | 변경 |
|---|---|---|
| 10 | `notification/admin/AdminNotificationEventListener.java` | `createOrMerge` 반환 Notification 을 `NotificationBroadcastEvent` 로 **재발행** (fan-out loop 안쪽) |
| 11 | `templates/admin/fragments/header.html` | polling wrapper `<div hx-get="...badge" hx-trigger="every 30s">` **삭제** → `<div hx-ext="sse" sse-connect="/admin/notifications/stream" sse-swap="notification-badge">` 로 교체. 드롭다운 wrapper 에 `sse-swap="notification-item"` + `hx-swap="afterbegin"` 속성 추가 (드롭다운 열려 있을 때만 유의미) |
| 12 | `templates/fragments/_head-htmx.html` 또는 각 admin layout | HTMX **sse 확장** script 추가 (`htmx-ext-sse` CDN 또는 webjar) |
| 13 | `build.gradle.kts` | **htmx-ext-sse webjar 추가** (`org.webjars.npm:htmx.org__sse` 가 공식 좌표인지 실측 필요 — 없으면 `/static/js/htmx-ext-sse.js` 로 self-host) |
| 14 | `common/config/SecurityConfig.java` | `/admin/notifications/stream` 접근 role 확인 (이미 `/admin/**` chain 상속되나 **CSRF ignore** 등록 필요 — SSE 는 Idempotent GET 이라 기본적으로 CSRF 영향 없음, 그러나 async timeout filter 와 상호작용 검증) |
| 15 | `docs/design-contracts/admin/header-notifications.md` | §상태 머신 polling → SSE 로 치환 · §deferred 에서 SSE 항목 제거 · §selector 에 `[sse-connect]` 추가 |
| 16 | `e2e/contracts/admin-header-notifications.ts` | polling selector 체크 삭제 → SSE 속성 selector 체크 추가 |
| 17 | `static/css/admin.css` | **Q4 승인 시** `.admin-notif-badge.is-flash` 애니메이션 (keyframes bg flash 0.6s) — prototype 에 없음 (POLICY P-5 로 화면 문서에 기록) |
| 18 | `application.yml` | `spring.mvc.async.request-timeout` 를 heartbeat 보다 길게 (예: 60분). `youthmoa.notification.sse.heartbeat-seconds=25` · `youthmoa.notification.sse.timeout-seconds=1800` property |

---

## 8. SSE 이벤트 포맷 (서술 계약 핵심)

### 이벤트 종류

| event | 데이터 포맷 | 트리거 | 클라 동작 |
|---|---|---|---|
| `notification-badge` | `<span id="admin-notif-badge" ...>N</span>` (fragment HTML) | 매 신규/병합 알림 발행 + 수신자별 unread count 재계산 | HTMX sse-swap="notification-badge" + `hx-target="#admin-notif-badge"` + `hx-swap="outerHTML"` 로 배지 교체 |
| `notification-item` | `<li class="admin-notif-item admin-notif-item--unread" ...>...</li>` | 매 신규 알림 발행 (병합은 제외) | 드롭다운 열린 경우만 상단 prepend (`sse-swap="notification-item"` + `hx-swap="afterbegin"` + `hx-target="#admin-notif-list"`) |
| `heartbeat` | 빈 comment (`: heartbeat\n\n`) | 서버 25s 주기 | 클라 무시 (프록시 timeout 방지만) |
| `ready` | `connected` | 연결 수립 직후 1회 | 클라 로그만 |

### 와이어 포맷 예

```
: heartbeat

id: 1697365200000
event: notification-badge
data: <span id="admin-notif-badge" class="admin-notif-badge" data-notif-badge="3">3</span>

id: 1697365200001
event: notification-item
data: <li class="admin-notif-item admin-notif-item--unread" ...>...</li>

retry: 3000
```

### id 전략 (Q3)

- `id:` = `Notification.lastOccurredAt.atZone(SEOUL).toInstant().toEpochMilli()` + 수신자 접미사
- 재연결 시 `Last-Event-ID` 헤더 수신 → 그 시점 이후 생성된 알림을 재발행 (`findByUserAndLastOccurredAtAfter(user, since)`)
- **단순화 옵션**: id 를 생략하고 재연결 시 unread count 전체 재조회 (유실 알림 제목 복구 불가하나 배지 숫자만 맞추면 충분)

---

## 9. 결정 질문 (Q0~Q7)

| # | 질문 | 옵션 | 권장 | 근거 |
|---|---|---|---|---|
| **Q0** | 기술 스택 | (1) SSE + HTMX sse-ext (2) 순수 SSE + 수동 JS (3) WebSocket STOMP | **(1) SSE + HTMX sse-ext** | 서버 LOC 최소 · HTMX 와 OOB swap 자연 통합 · Fly.io · 사내 프록시 호환 · 양방향 불필요 |
| **Q1** | 전환 범위 | (1) admin 벨만 (2) admin + 사용자 벨 | **(1) admin 만** | 사용자 벨은 polling 없는 SSR fragment · 전환 효과 극소 · 회귀 영향 확산. 사용자 트랙은 `A7-realtime-user` 로 이월 |
| **Q2** | rate-limit 호환 | (1) `createOrMerge` 반환값으로 broadcast 트리거 (2) 리스너 분기 (merge 는 badge 만, 신규는 badge + item) | **(2) 분기** | merge 된 경우 "같은 항목 재강조" 는 item prepend 필요 X. badge 만 재알림. 신규는 item 도 push |
| **Q3** | 재연결 유실 보정 | (A) `id` + `Last-Event-ID` 재조회 (B) 재연결 시 unread count 전체 refetch (C) 유실 허용 | **(B)** | 알림 벨은 "숫자 맞음" 이 핵심. 유실된 1건은 다음 신규 수신 또는 드롭다운 클릭 시 catch-up. (A) 는 복잡도↑ · 이번 스코프 불필요 |
| **Q4** | 벨 flash 애니메이션 | (1) 추가 (0.6s bg flash) (2) prototype 미명시라 유지 | **(1) 추가** | SSE 로 즉시 수신하면 "뭔가 변했다" 신호가 필요. POLICY P-5 로 화면 문서 기록. 과도한 모션 아닌 1회성 flash |
| **Q5** | fallback 전략 | (1) HTMX sse-ext 내장 재연결만 (2) `EventSource` 미지원 환경에 polling fallback (3) 둘 다 | **(1)** | 지원 브라우저 Chrome/Safari/Firefox 모두 EventSource 지원. IE 포기 (A1 기준 미지원). polling fallback 유지 시 두 코드경로 유지비용 과도 |
| **Q6** | heartbeat 간격 | 15s / **25s** / 50s | **25s** | nginx 기본 `proxy_read_timeout=60s` 보다 넉넉히 짧게 · Fly.io idle timeout 안전 범위 |
| **Q7** | 이벤트 포맷 | (1) HTML fragment (HTMX swap 재사용) (2) JSON (클라 JS 직접 처리) | **(1) HTML fragment** | HTMX sse-ext 가 HTML 그대로 swap · Thymeleaf fragment 재사용으로 템플릿 중복 제거. JSON 은 클라 JS 템플릿 중복 |

---

## 10. 회귀 방지 (최우선)

| 항목 | 검증 방법 |
|---|---|
| 사용자 트랙 알림 flow (PR #143) 무영향 | `notification/*Test`, `ApplicationNotificationListenerTest`, `NotificationControllerTest` 전건 PASS 유지 · `/notifications` · `/notifications/{id}/read` · `/notifications/read-all` 응답 스펙 무변경 |
| admin 트랙 기존 endpoint 무영향 | `/admin/notifications/dropdown` · `/badge` · `/{id}/read` · `/mark-all-read` · `/{id}/delete` 응답 포맷 그대로. `AdminNotificationControllerTest` 전건 PASS · 계약 검사 (`admin-header-notifications`) 갭 0 유지 |
| A7-rate-limit 병합 무영향 | `createOrMerge` 로직 무수정 · `Notification` 엔티티 필드 무변경 · `NotificationServiceTest` 병합 시나리오 PASS 유지. **SSE broadcast 는 `createOrMerge` 반환 **후** 발행** |
| fan-out Resolver (B-1/B-2/B-3) 무영향 | `CompositeApplicationCreatedResolver` 등 Resolver 체인 무수정 |
| SMTP 메일 발송 (PR #243) 무영향 | `SmtpAdminInvitationMailSender` 무수정. SSE 와 메일은 별개 축 |
| HTMX 다른 영역 (admin 검색 · 유저 드롭다운 · 알림 drop 클릭) 무영향 | `_head-htmx.html` 에 `htmx-ext-sse` script 추가 시 기존 HTMX 2.0.4 동작 변화 없음 실측 — admin 트랙 전체 Playwright E2E green 유지 |
| SecurityConfig CSRF | SSE 는 GET 이라 영향 없음. 그러나 async request timeout filter 와 `SseEmitter` 상호작용 검증 (Boot 4 신규 async filter chain) |

---

## 11. 검증 시나리오

### 11-A. 정적 검증
- `./gradlew compileJava`
- `SseConnectionRegistryTest` — add/remove · broadcast 격리 · concurrent add 검증
- `NotificationSseControllerTest` — `GET /admin/notifications/stream` 응답 Content-Type `text/event-stream` · ready 이벤트 1회 전송 · heartbeat 발신
- `NotificationSseEventListenerTest` — `NotificationBroadcastEvent` 수신 시 Registry.broadcast 호출 검증 (Mockito)
- `AdminNotificationEventListenerBroadcastTest` — 기존 fan-out 후 `NotificationBroadcastEvent` 발행 횟수 (수신자 수 만큼)
- 기존 `AdminNotificationControllerTest` · `AdminNotificationServiceTest` · `NotificationServiceTest` 전건 PASS 유지

### 11-B. 동적 검증 (curl / bootRun)

> **CLAUDE.md 동적 검증 필수**. bootRun (`.claude/scripts/bootrun-e2e.cmd`, port 8090) 상태에서 수행.

- `GET /admin/notifications/stream` (관리자 세션 쿠키 포함) →
  ```
  HTTP/1.1 200 OK
  Content-Type: text/event-stream;charset=UTF-8
  Cache-Control: no-cache
  Connection: keep-alive
  X-Accel-Buffering: no
  ```
  첫 응답 바디에 `event: ready` + `: heartbeat` 가 25s 이내 등장 확인
- `GET /admin/notifications/stream` without auth → 302 /admin/login (admin chain 상속)
- USER role → 403
- `curl` 로 SSE 연결 유지 상태에서 다른 터미널에서 `POST /programs/{id}/apply` → 첫 터미널 SSE 스트림에 `event: notification-badge` + `event: notification-item` 등장
- 60초 유지 테스트 — 끊김 없이 heartbeat 2회 이상 수신

### 11-C. 계약 검사
- `npx playwright test --project=contracts admin-header-notifications` — polling selector 제거 후에도 갭 0
- `npx playwright test --project=contracts admin-notification-sse` — 신규 계약 갭 0

### 11-D. 인터랙션 E2E (`admin-notifications-sse.spec.ts`)

Playwright 로 SSE 검증 시나리오:

```ts
// 시나리오 1: SSE 연결 수립
await page.goto('/admin');
const sseConnected = await page.waitForFunction(() => {
  // htmx:sseOpen 이벤트가 발생했는지 확인
  return window.__sseOpenFired === true;
}, { timeout: 5000 });

// 시나리오 2: 신규 알림 수신 즉시 반영 (polling 지연 X)
const t0 = Date.now();
await apiPostAsUser('/programs/1/apply', { ... });
await expect(page.locator('#admin-notif-badge')).toHaveText('1', { timeout: 2000 });
expect(Date.now() - t0).toBeLessThan(2000); // 30s polling 지연 아님을 증명

// 시나리오 3: 드롭다운 열린 상태에서 신규 수신 → prepend
await page.click('.admin-header-bell');
await apiPostAsUser('/programs/1/apply', { ... });
await expect(page.locator('.admin-notif-item').first()).toContainText('새 신청이 접수됐어요');

// 시나리오 4: 재연결 — 서버 재시작 후 자동 재연결
await restartBootRunServer();
await expect(page.locator('[data-sse-state="connected"]')).toBeVisible({ timeout: 10000 });
```

**Playwright SSE 테스트 고려사항**:
- `page.evaluate(() => new EventSource('/admin/notifications/stream'))` 직접 평가 가능
- 또는 `page.route('/admin/notifications/stream', ...)` 로 서버 응답 가로채 테스트 — 이번 티켓은 **실제 SSE 왕복** 검증 우선 (bootRun e2e profile)
- CI (Testcontainers) 환경에서 SSE 안정성 — 응답 timeout 길게 설정 필요. `page.waitForFunction` 패턴 추천

### 11-E. 시각 검증 (사용자 영역)
- 배지 flash 애니메이션 — SSE 수신 시 벨 배지 bg 0.6s flash
- 드롭다운 prepend 애니메이션 (옵션) — slide-down 등 (prototype 미명시 → 무조건 추가 X. Q4 범위 내만)
- 다크 헤더 위 벨 외형 변경 없음 재확인

---

## 12. 갭 리스트 (현재 vs 목표)

| # | 항목 | 현재 | 목표 | 우선순위 |
|---|---|---|---|---|
| 1 | admin 벨 반영 지연 | 최대 30s | **즉시 (< 1s)** | 높음 |
| 2 | polling 트래픽 | 관리자 N명 × 30s · 하루 2,880 req/admin | **연결당 1회 + heartbeat** | 중 |
| 3 | 드롭다운 열린 상태에서 신규 알림 | 다음 polling tick 까지 미노출 | **즉시 prepend** | 중 |
| 4 | 연결 끊김 처리 | polling 특성상 암묵적 재시도 | **표준 EventSource 자동 재연결 + Last-Event-ID 복구 (Q3)** | 낮음 |
| 5 | 벨 flash 피드백 | 없음 | **0.6s bg flash (Q4)** | 낮음 |
| 6 | 관측성 | polling 로그만 | **Micrometer gauge/counter** | 중 |

---

## 13. 리스크 · 비용

### 리스크
- **Fly.io · 프록시 buffering** — nginx 등이 응답 flush 를 지연시키면 SSE 가 큰 chunk 단위로 몰려 전달됨. 대응: `X-Accel-Buffering: no` 응답 헤더 + `response.flushBuffer()`
- **Boot 4 async request timeout** — Spring MVC async request timeout (`spring.mvc.async.request-timeout`) 기본값(30s) 보다 SSE 연결 수명이 길어야 함. `-1` (무제한) 또는 30분 명시
- **Tomcat NIO connector thread** — 동시 SSE 연결 수가 Tomcat `max-connections` 를 초과하면 신규 HTTP 요청 blocked. 현재 관리자 ≤10 규모라 영향 없음. 100명 이상 확장 시 `server.tomcat.max-connections` 조정
- **HTMX sse-ext 호환성** — HTMX 2.0.4 는 htmx-ext-sse 와 호환되나 **webjar 좌표 확정 필요**. 없으면 `/static/js/htmx-ext-sse.js` 로 self-host
- **인스턴스 수평 확장** — Registry 는 인스턴스 내부 메모리라 다중 인스턴스 배포 시 발행 노드와 수신 노드가 다르면 유실. 대응: Redis pub/sub (이번 범위 X · 단일 인스턴스 전제)

### 비용 (LOC 추정)

| 영역 | LOC |
|---|---|
| `SseConnectionRegistry` + 테스트 | ~80 |
| `NotificationSseController` + 테스트 | ~100 |
| `NotificationSseEventListener` + 이벤트 record + 테스트 | ~60 |
| `AdminNotificationEventListener` 수정 (broadcast 발행) | ~20 |
| `SseConfig` (executor · heartbeat scheduler) | ~40 |
| 헤더 fragment 수정 (polling → SSE 속성) | ~15 |
| `_notification-sse-item.html` 추출 | ~40 |
| CSS flash 애니메이션 (Q4) | ~15 |
| 계약 2건 (설명 .md + .ts) | ~100 |
| Playwright E2E spec | ~120 |
| **합계** | **~590** |

---

## 14. deferred / deviation

| 항목 | 처리 | 사유 |
|---|---|---|
| 사용자 트랙 `/notifications` fragment SSE | `deferred: A7-realtime-user` | 사용자 벨은 polling 없는 SSR · 전환 효과 극소 |
| WebSocket 양방향 | `deferred: 없음` (영구 미도입) | 알림은 서버→클라 단방향이라 SSE 로 충분 |
| Web Push (브라우저 Push API) | `deferred: A7-webpush` | 백그라운드 알림 필요 시 별도 티켓 |
| 다중 인스턴스 Redis pub/sub | `deferred: A7-realtime-scaling` | 현재 단일 인스턴스 · 수평 확장 시 티켓 |
| 벨 flash 애니메이션 | 본 티켓 Q4 승인 시 포함 (POLICY P-5 로 화면 문서 기록) | prototype 미명시 개선. 과도한 모션 아닌 1회성 |
| 상대 시간 표기 ("방금 전") | 기존 deviation 유지 (A7 spec 참조) | 클라 JS 필요 · SSR 정합 |

---

## 15. 작업 큐 메타

| 필드 | 값 |
|---|---|
| 작업 ID | **A7-realtime** |
| 우선순위 | admin 트랙 A7 축 마감 (알림 축 최종 PR) |
| 추정 단위 | **1 PR** (계약·Registry·Controller·Listener·fragment·테스트 일괄) |
| 상태 | `spec_draft` → 사용자 Q0~Q7 결정 후 `spec_confirmed` |
| 후속 티켓 | A7-realtime-user (사용자 트랙) · A7-realtime-scaling (Redis pub/sub) · A7-webpush |

---

## 16. 다음 단계

### 사용자 결정 대기 (핵심 8)
- **Q0** 기술 스택 = SSE + HTMX sse-ext (권장) / 순수 SSE / WebSocket STOMP
- **Q1** 전환 범위 = admin 벨만 (권장) / admin + 사용자
- **Q2** rate-limit 호환 = 리스너 분기 (merge 는 badge 만) (권장) / `createOrMerge` 반환으로 통일 broadcast
- **Q3** 재연결 유실 보정 = unread count 전체 refetch (권장) / Last-Event-ID 재조회 / 유실 허용
- **Q4** 벨 flash 애니메이션 = 추가 (권장) / 추가 안함
- **Q5** fallback = HTMX sse-ext 내장 재연결만 (권장) / polling fallback 병행
- **Q6** heartbeat 간격 = 25s (권장) / 15s / 50s
- **Q7** 이벤트 포맷 = HTML fragment (권장) / JSON

### 결정 후
- `spec_confirmed` 승격 → ym-impl 인계
- 디자인 계약 치환 (polling → SSE) + 신규 SSE 계약 신설은 **구현 PR 안에서** 같은 단위로 처리 (CLAUDE.md "계약 신설·치환은 구현과 같은 PR" 원칙)
