# 작업 명세: F2c-sse — SSE 실시간 알림 (폴링 대체)

> 산출: ym-spec, 2026-07-20. 상태: **spec_done** (Q1~Q7 사용자 결정 대기)
> 브랜치 후보: `feature/F2c-sse-notifications`
> 포트폴리오 강화 트랙 — 백로그 F2c 원안 "HTMX 30s 폴링 unread 갱신" 을 **SSE(Server-Sent Events) + HTMX sse extension** 으로 대체 구현.

---

## 0. 개념 설명 — 왜 이 use-case 에 SSE 인가 (학습 규칙)

### 실시간 갱신 3가지 방식 비교

| 항목 | 폴링 (원안) | **SSE (본 spec)** | WebSocket |
|---|---|---|---|
| 방향 | 클라이언트 → 서버 반복 요청 | **서버 → 클라이언트 단방향 push** | 양방향 |
| 프로토콜 | 일반 HTTP 요청 반복 | **일반 HTTP** (`Content-Type: text/event-stream` 인 열린 응답 1개) | HTTP Upgrade → 별도 ws:// 프로토콜 |
| 지연 | 최대 폴링 주기 (30s) | 이벤트 즉시 (ms 단위) | 이벤트 즉시 |
| 서버 부하 | 알림 없어도 유저 수 × (요청/30s) — DB count 쿼리 반복 | 이벤트 발생 시에만 write. 유휴 시 부하 0 | 이벤트 시에만. 단 핸드셰이크·프레이밍 비용 |
| 재접속 | 불필요 (매번 새 요청) | **브라우저 `EventSource` 가 자동 재접속** (내장) + `Last-Event-ID` 지원 | 직접 구현 필요 (라이브러리 의존) |
| 인프라 | 아무거나 | HTTP 그대로 — 프록시·인증(세션 쿠키)·CSRF 체계 재사용 | 프록시 Upgrade 설정·인증 별도 고려 |
| Spring WebMVC 지원 | `@GetMapping` | **`SseEmitter` 내장** (Servlet async 기반, WebFlux 불필요) | `spring-websocket` 별도 모듈 + STOMP 등 |
| HTMX 지원 | `hx-trigger="every 30s"` | **공식 `sse` extension** (`hx-ext="sse"` 선언만으로 연결·재접속·swap) | `ws` extension 있으나 양방향 전제 |

### 결론 — 알림 뱃지 갱신은 SSE 가 최적

1. **단방향으로 충분** — 클라이언트가 서버로 보낼 실시간 데이터가 없다 (읽음 처리는 기존 HTMX POST 유지). WebSocket 의 양방향성은 오버스펙.
2. **HTTP 기반** — 세션 쿠키 인증·SecurityConfig 매처·기존 배포 체계가 그대로 통한다. WebSocket 은 인증 전파를 따로 설계해야 함.
3. **HTMX 친화** — 공식 sse extension 이 연결 수립·자동 재접속·이벤트별 swap 을 선언형 attribute 로 제공. JS 를 거의 쓰지 않는 이 프로젝트의 SSR+HTMX 방향과 일치.
4. **폴링 대비**: 지연 30s → 즉시, 유휴 시 DB 쿼리 0회. 포트폴리오 관점에서 "폴링을 SSE 로 개선 + 스레드 모델 이해" 서사가 성립.

### SSE 와이어 포맷 (참고)

```
HTTP/1.1 200
Content-Type: text/event-stream

event: notification        ← 이벤트 이름 (HTMX sse-swap / hx-trigger 매칭 키)
data: {...}                 ← 페이로드 (텍스트)
                            ← 빈 줄 = 이벤트 종료
: heartbeat                 ← ':' 시작 줄 = 주석 (연결 유지 ping 용)
```

---

## 1. 디자인 출처

본 티켓은 **전송 계층(transport) 교체**로, 화면 시각 변경이 없다. 알림 종·dot·드롭다운 UI 는 PR #60 에서 prototype 정합 구현 완료 상태이며 본 spec 은 그 갱신 트리거만 폴링 예정안 → SSE 로 바꾼다.

| 자산 | 위치 | 내용 |
|---|---|---|
| `docs/00_assets/prototype.tsx` | **line 289~337 `NotifPanel`** | 알림 패널 UI — 기 구현 (`fragments/notification-panel.html` 주석에 동일 라인 인용) |
| 〃 | **line 337~400 `Header`** | 종 아이콘 + unread dot — 기 구현 (`fragments/header.html`) |
| `docs/00_assets/HANDOFF.md` | 4.4 Header | 헤더 우측 액션 구성 — 변경 없음 |
| `docs/00_assets/wireframe.png` | — | 실시간 갱신 정책 언급 없음 (충돌 없음) |
| 비교 대상 | `fragments/header.html` (bell 블록), `fragments/notification-panel.html`, `NotificationController`, `HeaderNotificationAdvice`, `ApplicationNotificationListener` | |

prototype 은 mock state 로 동작해 실시간 push 개념 자체가 없음 → **자산 간 갭 없음, 데이터 모델 gap 없음** (Notification 엔티티 필드 변경 0건 — §1-B).

## 1-B. 데이터 모델 gap 표

| prototype 필드 (NotifPanel mock, tsx L289~) | 현재 엔티티 (`Notification`) | 조치 |
|---|---|---|
| title / message / time / tone | `title` / `message` / `createdAt` / `type.getToneColor()` | **변경 없음** (F2b 완료) |
| unread 여부 | `isRead` | 변경 없음 |
| (실시간 push) | — 엔티티 무관 (전송 계층) | 컬럼 추가 없음. `Last-Event-ID` 재전송용 id 는 `Notification.id` 재사용 가능 (후속, §6) |

## 1-C. 데이터 소비 지점

| 소비 지점 | 현재 갱신 경로 | 본 티켓 후 |
|---|---|---|
| 헤더 종 dot (`#header-bell-dot`) | 페이지 로드 시 `HeaderNotificationAdvice` 주입 + read-all/read 시 OOB swap | **+ SSE 이벤트 수신 시 re-fetch 로 갱신** |
| 헤더 드롭다운 패널 (`notification-panel :: panel`) | 페이지 로드 시 advice + read-all/read 시 swap | Q5 — 기본안: dot 과 함께 갱신 (panel fragment 재사용) |
| `/notifications` 전체 목록 | 페이지 로드 시 | **변경 없음** (진입 시 최신) |
| `/mypage` 알림 설정 | 채널 설정 (SSE 무관) | 변경 없음 |

---

## 2. 아키텍처 설계 (WebMVC + SseEmitter)

### 2-1. 컴포넌트 구성

```
[admin approve/reject | user cancel]
        │ ApplicationService.@Transactional — publishEvent(스칼라 snapshot record)
        ▼ (커밋 성공 후에만)
ApplicationNotificationListener  @TransactionalEventListener(AFTER_COMMIT)
        │ ① notificationService.create(...)   ← 알림 저장 (자체 트랜잭션, 즉시 커밋)
        │ ② sseEmitterRegistry.push(userId)    ← ①커밋 이후에 실행 (§2-4 트랜잭션 재구성)
        ▼
NotificationSseRegistry (신규, in-memory)
        │ Map<Long userId, List<SseEmitter>> — 탭별 다중 연결 지원
        ▼ event: notification (data 는 신호만 — Q1)
브라우저 EventSource (htmx sse ext 가 관리)
        │ hx-trigger="sse:notification" → hx-get
        ▼
GET /notifications/badge  → dot(+panel) fragment 재렌더 (기존 advice 조회 경로 재사용)
```

### 2-2. 신규 클래스 (도메인 flat 패키지 규칙 — `notification/` 안에 배치)

**`NotificationSseRegistry`** (`@Component`)

| 멤버 | 설계 |
|---|---|
| `Map<Long, List<SseEmitter>> emitters` | `ConcurrentHashMap` + value 는 `CopyOnWriteArrayList` (연결 수 적고 순회 >> 변경이므로 적합) |
| `SseEmitter subscribe(Long userId)` | emitter 생성(타임아웃 §2-3) → 리스트 추가 → `onCompletion`/`onTimeout`/`onError` 콜백에서 자기 제거 등록 → **연결 직후 초기 sync 이벤트 1건 send** (재접속 중 놓친 이벤트 보정, §2-5) → 반환 |
| `void push(Long userId)` | 해당 유저의 모든 emitter 에 `event: notification` send. `IOException`/`IllegalStateException` 발생 emitter 는 즉시 제거 (죽은 연결 정리) |
| per-user 연결 상한 | 초과 시 가장 오래된 emitter `complete()` 후 제거 — 탭 다수·좀비 연결 누수 방어 (상한값 Q6) |
| `int connectionCount()` (패키지 private) | 테스트·모니터링용 |

**`NotificationSseController`** (`@Controller` — 기존 `NotificationController` 와 분리해 SSE 관심사 격리)

```java
@GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
    return registry.subscribe(principal.getId());
}
```

- `SseEmitter` 반환 = Spring 이 Servlet 3.x async (`request.startAsync()`) 로 전환 — 메서드는 즉시 리턴하고 응답은 열린 채 유지.
- badge re-fetch 엔드포인트는 기존 `NotificationController` 에 추가 (§3-2).

### 2-3. 타임아웃·정리·재접속

| 항목 | 설계 | 이유 |
|---|---|---|
| emitter 타임아웃 | **30분** (`new SseEmitter(30 * 60 * 1000L)`). `0L`(무제한) 금지 | 무제한이면 죽은 연결이 heartbeat 실패 전까지 영구 잔존. 유한 타임아웃 + 클라이언트 자동 재접속 조합이 안전 |
| `spring.mvc.async.request-timeout` | **설정하지 않음** (emitter 생성자 타임아웃이 우선) | 전역 async 타임아웃을 두면 다른 async 기능까지 영향 |
| 정리 콜백 | `onCompletion` / `onTimeout` / `onError` 3종 모두에서 registry 제거. onTimeout 안에서 `complete()` 호출 | 콜백 누락 시 Map 누수 → OOM 경로 |
| heartbeat | `@Scheduled(fixedDelay = 25_000)` 로 전체 emitter 에 SSE 주석(`:` comment) send — 실패 emitter 제거 | ① 프록시·브라우저 유휴 연결 절단 방지 ② **죽은 연결의 유일한 감지 수단** (TCP 절단은 write 시점에만 IOException 으로 드러남). `@EnableScheduling` 신설 필요 (Q4) |
| 재접속 | **HTMX sse ext 가 내장 처리** — 연결 끊김/타임아웃 시 `EventSource` 표준 재접속 (기본 backoff). 서버 측 추가 코드 불필요 | |
| 재접속 중 놓친 이벤트 | `subscribe()` 가 연결 직후 초기 `notification` 이벤트 1건 send → 클라이언트가 badge re-fetch → 항상 DB 기준 최신으로 수렴 | `Last-Event-ID` 기반 이벤트 재전송은 오버스펙 — 페이로드가 "신호" 뿐이라 최신 re-fetch 1회로 동등 (후속 §6) |
| 설정 외부화 | `youthmoa.notification.sse.timeout-minutes` / `heartbeat-seconds` / `max-connections-per-user` — `application.yml` + `@ConfigurationProperties` | 학습 목적 + e2e 프로파일에서 짧은 타임아웃으로 테스트 용이 |

### 2-4. 이벤트 흐름 — 트랜잭션 경계 (본 spec 의 핵심 설계 결정)

**⚠️ 기존 `AFTER_COMMIT` 만으로는 부족하다 — 2단계 유령 알림 창이 있다.**

1단계 (기 해결): `ApplicationService` 트랜잭션이 롤백되면 알림 자체가 생기면 안 됨 → 기존 `@TransactionalEventListener(phase = AFTER_COMMIT)` 이 이미 보장. **커밋 전에 push 하면**: 클라이언트가 SSE 를 받아 badge 를 re-fetch 하는 시점에 ① 원본 트랜잭션이 롤백되어 알림이 영영 없거나 ② 아직 커밋 전이라 count 가 stale — 두 경우 모두 "dot 은 켜졌는데 알림이 없는" 유령 알림.

2단계 (**본 티켓에서 해결 필요**): 현재 listener 메서드는 `@Transactional(REQUIRES_NEW)` 이고 `notificationService.create()` 가 이 트랜잭션에 참여한다. **push 를 listener 메서드 끝에 그냥 추가하면 REQUIRES_NEW 트랜잭션이 아직 커밋되기 전에 SSE 가 나간다** → 클라이언트 re-fetch 가 별도 커넥션에서 count 를 읽으므로 새 알림 row 를 못 볼 수 있다 (push 가 저장보다 빠른 역전).

**해결 — listener 트랜잭션 재구성 (권장안)**:

```java
// Before (F2b): 메서드 자체가 REQUIRES_NEW 트랜잭션
@TransactionalEventListener(phase = AFTER_COMMIT)
@Transactional(propagation = REQUIRES_NEW)
public void onApproved(...) { notificationService.create(...); }

// After (F2c-sse): 메서드는 비트랜잭션.
// create() 는 서비스 자신의 @Transactional 로 독립 트랜잭션 개시·"리턴 시점에 커밋 완료".
// push 는 그 이후 실행 → 순서 보장.
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onApproved(ApplicationApprovedEvent event) {
    try {
        notificationService.create(...);          // ① 저장 + 커밋 (리턴 시 완료)
        sseRegistry.push(event.userId());          // ② 커밋 이후 push
    } catch (RuntimeException e) { log.error(...); }
}
```

- `AFTER_COMMIT` 시점에는 원본 트랜잭션이 닫혀 있으므로, 비트랜잭션 listener 에서 `create()` 를 호출하면 서비스의 `@Transactional`(REQUIRED) 이 **새 트랜잭션을 열고 리턴 시점에 커밋**한다 — 기존 REQUIRES_NEW 와 동일한 격리 효과 + push 순서 보장을 동시에 얻는다. 3개 메서드 (`onApproved`/`onRejected`/`onCancelled`) 모두 동일 적용.
- push 실패(느린 클라이언트 등)해도 알림은 이미 저장됨 → 다음 페이지 로드 시 advice 가 노출. push 는 best-effort, 예외는 registry 내부에서 소화 (listener 로 전파 금지).
- **push 는 이벤트 발생 요청 스레드에서 실행됨** (승인한 관리자의 요청 스레드). emitter write 가 오래 걸리면 관리자 응답이 늦어질 수 있으나, `SseEmitter.send` 는 servlet 출력 버퍼 write 라 통상 즉발. 학습 규모에서는 동기 push 로 충분 — `@Async` 분리는 §6 후속.

### 2-5. 스레드 모델 — SseEmitter 와 톰캣 스레드풀 (학습 규칙)

| 질문 | 답 |
|---|---|
| 열린 SSE 연결 1개 = 톰캣 스레드 1개 점유? | **아니다.** `SseEmitter` 는 Servlet async 기반 — 컨트롤러 메서드가 리턴하면 요청 처리 스레드는 풀로 반환되고, 연결은 NIO 커넥터가 소켓 레벨로만 유지한다. **유휴 연결의 비용은 스레드가 아니라 커넥션·메모리** (톰캣 `maxConnections` 기본 8192 vs `maxThreads` 기본 200 — 200 유저 동시 접속해도 스레드풀 고갈 없음) |
| 스레드를 쓰는 순간은? | ① 최초 구독 요청 처리 (즉시 반환) ② `push()`/heartbeat 의 `send()` 호출 순간 — 호출한 스레드(이벤트 발생 요청 스레드·스케줄러 스레드)가 write 를 수행 |
| 그럼 병목은? | 다수 유저에게 동시 push 하는 fan-out 순간, 그리고 느린 클라이언트로의 write. 현 설계는 유저 단건 push 라 fan-out 없음 |
| **가상 스레드 시너지** | Java 21 + `spring.threads.virtual.enabled=true` 시 요청 처리·`@Scheduled` 가 가상 스레드에서 실행 → blocking write 가 캐리어 스레드를 점유하지 않아 push/heartbeat 확장성이 개선된다. **단, 이 프로퍼티 활성화는 별도 티켓 `chore/caching-loadtest` 범위** — 본 티켓은 프로퍼티를 건드리지 않고, SseEmitter 설계가 가상 스레드 on/off 어느 쪽에서도 동작하도록만 담보한다 (의존 관계: 없음 — 순서 무관, 시너지만 존재) |

### 2-6. 다중 인스턴스 한계 (명시)

emitter 레지스트리는 **인스턴스 로컬 in-memory** 다. 인스턴스 2대 이상 스케일아웃 시, 승인 이벤트가 A 인스턴스에서 발생하면 B 인스턴스에 연결된 유저는 push 를 못 받는다 (다음 페이지 로드까지 지연 — 기능 파손은 아님, 실시간성만 상실).

- 현 단계 판단: 단일 인스턴스 학습 프로젝트 — **의도적으로 수용**하고 spec·클래스 Javadoc 에 한계를 명시.
- 확장 경로 (후속 백로그 `feature/sse-redis-pubsub`): 알림 생성 시 Redis pub/sub 채널로 `userId` publish → 각 인스턴스가 구독해 자기 레지스트리의 해당 유저에게 push. 레지스트리 인터페이스를 `push(userId)` 단일 진입점으로 잡아두면 구현 교체만으로 확장 가능.

---

## 3. HTMX 연동 설계

### 3-1. sse extension 로딩

- htmx 2.x 는 코어에 extension 을 **내장하지 않음** — `htmx.org:2.0.4` webjar 에는 sse ext 가 없다. 별도 npm 패키지 `htmx-ext-sse` webjar 추가 필요 (Q2):

```kotlin
// build.gradle.kts — htmx 2.x 계열 호환 (코어 2.0.4 는 E2E 이슈로 고정 — bump 금지)
implementation("org.webjars.npm:htmx-ext-sse:2.2.2")
```

```html
<script th:src="@{/webjars/htmx.org/2.0.4/dist/htmx.min.js}" defer></script>
<script th:src="@{/webjars/htmx-ext-sse/2.2.2/dist/sse.min.js}" defer></script> <!-- htmx 뒤 -->
```

- ⚠️ impl 첫 단계에서 **webjar 실제 경로 검증 필수**: `jar tf ~/.gradle/.../htmx-ext-sse-2.2.2.jar | grep -i sse` — dist 경로·min 파일명이 다르면 (예: `dist/sse.js` 만 존재) 템플릿 경로를 실측값으로. 검증 후 `curl /webjars/htmx-ext-sse/2.2.2/dist/sse.min.js` 200 확인.
- ⚠️ **htmx 로딩 범위 갭**: 헤더 fragment 는 16개 템플릿에 포함되지만 htmx script 는 7개 템플릿에만 로드됨 (`mypage/*` 4종, `notice/detail`, `search/result`, `application/apply`, `user/*` 등 미로드). 헤더에 `sse-connect` 를 넣으면 htmx 없는 페이지에선 그냥 무시되어 **페이지별로 실시간 여부가 달라지는 비일관** 발생 → Q3.

### 3-2. 마크업 — 헤더 종 블록 (`fragments/header.html`)

기존 인증 블록(`sec:authorize="isAuthenticated()"`) 내부의 `.header-bell-menu` 를 SSE 루트로 지정:

```html
<div sec:authorize="isAuthenticated()" class="header-bell-menu"
     hx-ext="sse" sse-connect="/notifications/stream">
    <button ... class="header-bell-trigger"
            hx-get="/notifications/badge"
            hx-trigger="sse:notification"
            hx-target="this"
            hx-swap="none">          <!-- 응답은 OOB 로만 반영 (아래) -->
        ... 종 svg + #header-bell-dot span 2종 (기존 유지) ...
    </button>
    ...
</div>
```

- `sse-connect` — ext 가 이 요소 생성 시 `EventSource('/notifications/stream')` 를 열고, 요소 제거·페이지 이탈 시 자동 close. **비인증 사용자는 블록 자체가 렌더되지 않아 연결 시도 없음** (SecurityConfig 수정 불필요 — §5).
- `hx-trigger="sse:notification"` — 신호 수신 시 re-fetch (권장 A안, Q1). `sse-swap` 방식(B안)과의 비교는 Q1 표 참조.
- `hx-swap="none"` + 응답의 `hx-swap-oob` 조합: badge 응답이 dot span (id=`header-bell-dot`) 과 패널을 OOB 로 교체 → 버튼 내부 구조를 건드리지 않아 열려 있는 드롭다운·aria 상태 비파괴.

### 3-3. re-fetch 엔드포인트 (기존 `NotificationController` 에 추가)

```java
/** SSE 신호 수신 후 dot(+panel) 재렌더. 렌더 데이터는 Advice 가 이미 주입 — 본문은 뷰명만. */
@GetMapping("/notifications/badge")
public String badge() {
    return "fragments/notification-panel :: badge";
}
```

- **`HeaderNotificationAdvice` 와의 공존 원칙**: 초기 렌더 = advice (`headerUnreadCount`/`headerRecentNotifications` 전 페이지 자동 주입), 이후 갱신 = SSE 신호 → 이 GET → **같은 advice 가 이 요청에도 실행되어 최신값 주입** → 렌더 경로가 단일화된다 (SSE 페이로드에 HTML 을 싣는 B안이 필요로 하는 "요청 컨텍스트 밖 Thymeleaf 수동 렌더" 회피 — Q1 근거).
- `:: badge` fragment (신규, `notification-panel.html` 에 추가): `hx-swap-oob` 붙은 dot span 2종 + (Q5 채택 시) 패널 OOB. 기존 read-all/read 응답의 OOB dot 패턴과 동일 문법 재사용.
- ⚠️ **기존 중복 id 리스크**: `header.html` 의 래퍼 div 와 `notification-panel :: panel` 루트가 **둘 다 `id="header-notif-dropdown"`** — 현재 read-all 의 `hx-target="#header-notif-dropdown"` 이 래퍼를 outerHTML 교체해 `hidden` 래퍼 구조가 사라지는 잠재 버그. 본 티켓에서 패널 OOB 갱신을 넣으려면 id 충돌 정리가 선행되어야 함 → Q7.

### 3-4. CSRF / 캐싱

- `/notifications/stream`·`/notifications/badge` 모두 GET — `htmx-csrf.js` 가 skip (수정 불필요).
- SSE 응답은 `text/event-stream` 이라 중간 캐시 대상 아님. 운영 리버스 프록시(nginx) 도입 시 `X-Accel-Buffering: no` 헤더 필요 — 로컬·현 배포에서는 불필요, Javadoc 메모만.

---

## 4. 로그아웃·세션 만료 시 연결 정리

| 시나리오 | 동작 | 조치 |
|---|---|---|
| 페이지 이동·닫기 | htmx sse ext 가 `EventSource.close()` → 서버는 다음 heartbeat write 실패로 감지·제거 | heartbeat 가 정리 트리거 (§2-3) — 추가 코드 불필요 |
| 로그아웃 | POST /logout → 페이지 전환 → 위와 동일. 서버 세션 무효화와 무관하게 **이미 열린 SSE 연결은 즉시 끊기지 않음** — 잔존 push 는 다음 heartbeat 까지 최대 25s 도달 가능. 알림 데이터는 본인 것이므로 보안 문제 아님 (연결 수립 시점에 인증 완료) | 수용. 즉시 강제 종료가 필요해지면 후속: `SessionDestroyedEvent` 리스너에서 `registry.completeAll(userId)` |
| 세션 만료 후 재접속 | EventSource 재접속 → Security 가 302 `/login` → text/html 응답 = EventSource error → **표준 backoff 로 무한 재시도** (수 초 간격의 302 반복) | 수용 (해당 탭이 어차피 만료 상태 — 다음 상호작용에서 로그인 이동). 완화 옵션: stream 요청에 한해 401 반환하는 EntryPoint 분기 — 오버스펙으로 후속 |
| 서버 재기동 | 모든 연결 절단 → 클라이언트 자동 재접속 → 연결 직후 초기 sync 이벤트로 상태 수렴 (§2-3) | 설계로 흡수 |

**SecurityConfig 변경: 없음.** `/notifications/stream`·`/notifications/badge` 는 기존 `"/notifications/**"` authenticated 매처에 이미 포함 — impl 시 grep 재확인만.

---

## 5. 변경 범위 (파일 단위 전체 목록)

### Java (main)
- [ ] `notification/NotificationSseRegistry.java` — **신규** (§2-2)
- [ ] `notification/NotificationSseController.java` — **신규**, `GET /notifications/stream`
- [ ] `notification/NotificationSseProperties.java` — **신규**, `@ConfigurationProperties("youthmoa.notification.sse")`
- [ ] `notification/NotificationController.java` — `GET /notifications/badge` 추가
- [ ] `notification/ApplicationNotificationListener.java` — `REQUIRES_NEW` 제거 + push 연결 (§2-4). Javadoc 의 트랜잭션 설명 갱신 필수
- [ ] `YouthMoaApplication.java` 또는 config — `@EnableScheduling` (Q4 채택 시. `@ConfigurationPropertiesScan` 도 미존재 시 함께)

### 템플릿 / 정적
- [ ] `fragments/header.html` — bell 블록에 `hx-ext="sse"` / `sse-connect` / re-fetch 트리거 (§3-2)
- [ ] `fragments/notification-panel.html` — `:: badge` fragment 신설 (+ Q7 채택 시 id 충돌 정리)
- [ ] htmx script 로딩 대상 템플릿 — Q3 결정에 따름 (A안: 16개 템플릿 head 에 htmx+sse+csrf script 3종 일괄 — 공통 head fragment 신설 여부는 impl 재량, 단 기존 7개 페이지 회귀 없어야 함)

### 빌드 / 설정
- [ ] `build.gradle.kts` — `org.webjars.npm:htmx-ext-sse` 추가 (htmx 코어 2.0.4 는 **절대 bump 금지** — 2026-07-03 E2E revert 이력)
- [ ] `application.yml` — `youthmoa.notification.sse.*` 3키 (+ `application-e2e.properties` 에 짧은 타임아웃 override 검토)

### 테스트 (§7)
- [ ] `notification/NotificationSseRegistryTest.java` — 신규
- [ ] `notification/NotificationSseControllerTest.java` — 신규
- [ ] `notification/ApplicationNotificationListenerTest.java` — push 호출·순서 검증 보강
- [ ] `render/HeaderDropdownRenderTest.java` (또는 신규 RenderTest) — `sse-connect` 마크업 렌더 검증

### PR 분할 제안 (2 PR)

| PR | 내용 | 검증 게이트 |
|---|---|---|
| **PR-1 `feature/F2c-sse-backend`** | Registry + Properties + `/stream` + listener 재구성 + 단위 테스트. UI 무변경 | 정적 (테스트) + 동적 `curl -N` 스트림 실측 — 프론트 없이 독립 검증 가능 |
| **PR-2 `feature/F2c-sse-frontend`** | webjar + header 마크업 + `:: badge` + `/badge` 엔드포인트 + script 로딩 정리(Q3) + RenderTest + E2E | 동적 2-세션 시나리오 + Playwright |

1-PR 통합도 가능하나, 백엔드만으로 curl 검증이 완결되는 절단면이 있어 2 PR 권장 (단계적 진행 원칙).

---

## 6. 의존성 / 선행 작업 / 후속 백로그

- 선행: 없음 (F2b 알림 도메인 완성 상태 확인 완료).
- 병행 주의: `fragments/header.html` 을 만지는 다른 브랜치와 충돌 가능 — 머지 순서 조율.
- `chore/caching-loadtest` (별도 티켓): `spring.threads.virtual.enabled=true` 는 그쪽 범위. 본 티켓과 순서 무관, 시너지만 존재 (§2-5).
- 후속 백로그 등재 제안: ① `feature/sse-redis-pubsub` (다중 인스턴스, §2-6) ② `Last-Event-ID` 기반 이벤트 재전송 (Notification.id 재사용) ③ push `@Async` 분리 ④ 세션 만료 시 stream 401 EntryPoint.

---

## 7. 검증 시나리오 (ym-qa 실행 항목)

### 정적 검증 (단위 테스트)

- `NotificationSseRegistryTest` (Spring 컨텍스트 불필요, 순수 단위):
  - subscribe → connectionCount 1 / 동일 유저 2회 → 2 (다중 탭)
  - push 후 emitter 수신 데이터에 `event:notification` 포함 (`SseEmitter` 대신 수신 검증 가능한 테스트 더블 또는 MockMvc 경유)
  - **죽은 emitter 정리**: send 시 IOException 던지는 mock emitter → push 후 registry 에서 제거됨
  - onCompletion/onTimeout 콜백 실행 시 registry 에서 제거됨
  - per-user 상한 초과 시 oldest complete + 제거
- `NotificationSseControllerTest` (`@WebMvcTest` + security): 인증 시 200 + `Content-Type: text/event-stream`, 비인증 시 302 (기존 매처 회귀 확인). ⚠️ async 응답이므로 `mvc.perform(...).andExpect(request().asyncStarted())` 패턴 사용
- `ApplicationNotificationListenerTest` 보강: ① create 성공 시 push 1회 호출 (InOrder 로 **create → push 순서** 검증 — §2-4 회귀 방어) ② create 예외 시 push 미호출 ③ push 예외가 listener 밖으로 전파 안 됨
- RenderTest: 인증 렌더 시 `hx-ext="sse"` + `sse-connect="/notifications/stream"` 존재, 비인증 렌더 시 부재. `th:*`/`${...}` 잔존 없음
- `.\gradlew.bat compileJava` + notification 패키지 전체 + 기존 RenderTest 회귀

### 동적 검증 (curl — bootrun-e2e 8090, 회사 PC 필수 수행)

```bash
# 1. 로그인 세션 확보 (e2e 시드 유저)
curl -s -c /tmp/cj -o /dev/null http://localhost:8090/login   # CSRF 토큰 파싱 후
curl -s -b /tmp/cj -c /tmp/cj -d "username=...&password=...&_csrf=..." http://localhost:8090/login

# 2. SSE 스트림 실측 — 열린 채 유지되고 즉시 초기 sync 이벤트 + 25s 주기 heartbeat 주석 수신
curl -N -b /tmp/cj http://localhost:8090/notifications/stream
#   기대: HTTP 200, Content-Type: text/event-stream, "event: notification" 초기 1건, ": heartbeat" 반복

# 3. (별도 터미널) 같은 유저로 신청 취소 POST → 2번 터미널에 event: notification 즉시 수신
# 4. badge re-fetch: curl -b /tmp/cj http://localhost:8090/notifications/badge
#   기대: 200 + hx-swap-oob dot 마크업, unread>0 시 --hidden 클래스 부재
# 5. 비인증: curl -N http://localhost:8090/notifications/stream → 302 /login
# 6. 정적 리소스: curl -o /dev/null -w "%{http_code}" /webjars/htmx-ext-sse/<ver>/dist/sse.min.js → 200
```

**2-브라우저 세션 시나리오** (Claude Preview 또는 수동): 브라우저 A = 일반 유저 홈 대기(dot 꺼짐), 브라우저 B = 관리자 로그인 후 해당 유저 신청 승인 → **A 를 새로고침하지 않고** dot 점등 + (Q5 채택 시) 드롭다운 열면 새 알림 표시.

### E2E (Playwright — 대기 전략 명시)

- **금지**: `waitForTimeout` 고정 sleep. **사용**: 이벤트 기반 대기 2단 —
  1. `page.waitForResponse(r => r.url().includes('/notifications/badge'))` — SSE 신호 → re-fetch 발생 자체를 대기
  2. `expect(page.locator('#header-bell-dot:not(.header-bell-dot--hidden)')).toBeVisible({ timeout: 10_000 })` — swap 결과 대기
- 시나리오: userA 로그인 → 홈 진입 (스트림 수립은 `page.waitForResponse('/notifications/stream' 응답 헤더)` 또는 짧은 안정화 대기) → API/별도 컨텍스트로 승인 트리거 → 위 2단 대기 → dot 검증. 재접속 케이스: `page.reload()` 후 dot 상태 유지 확인 (advice 경로 회귀).
- e2e 프로파일에서 heartbeat·타임아웃 짧게 override 해 타임아웃-재접속 경로도 1케이스 커버 검토.

### 시각 확인 (사용자 영역)
1. 두 브라우저 시나리오에서 dot 점등이 체감 즉시(1s 내)인지
2. 로그아웃 → 재로그인 반복 시 콘솔에 EventSource 에러 루프가 남지 않는지 (DevTools Network 의 stream 항목 상태)
3. 장시간(30분+) 탭 방치 후 승인 이벤트 → 재접속 경유로 dot 정상 점등

---

## 8. 작업 큐 메타
- 작업 ID: F2c-sse / 우선순위: 중 (포트폴리오 강화 트랙) / 추정: 2 PR / 상태: **spec_done**

---

## 사용자 결정 필요 질문 (Q 리스트)

- **Q1. SSE push 페이로드 방식**
  - **A안 (권장): 신호만 push + 클라이언트 re-fetch** — `event: notification` / data 는 빈 신호. 클라이언트가 `GET /notifications/badge` 로 재조회. 장점: 렌더 경로가 기존 advice + Thymeleaf 요청 컨텍스트로 단일화, 놓친 이벤트도 최신 수렴, 페이로드 보안 걱정 無. 단점: 이벤트당 HTTP 1회 추가 (알림 빈도상 무시 가능).
  - B안: 서버가 HTML fragment 를 SSE data 에 실어 `sse-swap="notification"` 직접 swap — 왕복 1회 절약이나, 요청 컨텍스트 밖 `SpringTemplateEngine` 수동 렌더 코드 필요 + advice 와 렌더 경로 이원화.
  - C안: unread count 숫자만 push + 커스텀 JS — HTMX 선언형 방향과 불일치.
- **Q2. sse extension 조달** — A안 (권장): webjar `org.webjars.npm:htmx-ext-sse` (버전은 impl 시 htmx 2.0.x 호환 최신 확인, jar 경로 실측 §3-1). B안: `static/js/` 에 파일 vendoring (webjar 미존재·경로 문제 시 fallback).
- **Q3. htmx script 로딩 범위** — 현재 16개 헤더 페이지 중 7개만 htmx 로드. A안 (권장): htmx+sse+csrf 3종 script 를 전 헤더 페이지에 일괄 로드 (공통 head/scripts fragment 신설 포함) → 어느 페이지서든 실시간 일관. B안: 기존 7개 페이지만 SSE 동작 (변경 최소, 비일관 수용).
- **Q4. heartbeat + `@EnableScheduling` 도입** — 권장: 도입 (25s). 미도입 시 죽은 연결이 push 시점까지 잔존하고 프록시 절단에 취약. 프로젝트 첫 `@Scheduled` 사용이라 명시 결정 요청.
- **Q5. 갱신 범위 — dot 만 vs dot+드롭다운 패널** — 권장: **dot + 패널 동시** (badge fragment 가 패널 OOB 포함 — 종을 열어둔 채 알림 수신 시에도 목록 최신). 단 Q7 (id 충돌 정리) 선행 필요. 최소안: dot 만 (Q7 회피 가능).
- **Q6. per-user 동시 연결 상한** — 권장: 5 (탭 5개 초과 시 oldest 종료). 근거: 누수 방어 + 일반 사용 패턴 커버.
- **Q7. 기존 중복 id 정리 포함 여부** — `header.html` 래퍼와 panel fragment 가 동일 id `header-notif-dropdown` (§3-3 ⚠️). A안 (권장): 본 티켓 PR-2 에서 래퍼 id 를 `header-notif-dropdown-wrap` 으로 분리 + read-all/read 의 hx-target 재지정 + 기존 렌더/E2E 회귀 확인. B안: 별도 fix 티켓 분리 (이 경우 Q5 는 최소안 강제).
