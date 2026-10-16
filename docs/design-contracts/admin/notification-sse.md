# admin 알림 SSE 트랜스포트 (A7-realtime)

> 계약 파일: [`e2e/contracts/admin-notification-sse.ts`](../../../e2e/contracts/admin-notification-sse.ts)
> spec: [`docs/specs/A7-realtime-notifications.md`](../../specs/A7-realtime-notifications.md)
> 선행 계약: [`admin/header-notifications.md`](header-notifications.md) — 벨 외형 · 상태 머신

## 범위

관리자 알림 벨의 **실시간 push 트랜스포트** 를 다룬다. 2026-10-15 이전엔 HTMX polling 30s 였고 본 계약 신설과 함께 **SSE** 로 교체됐다. 벨 외형(배지·드롭다운·항목 마크업)은 선행 계약이 담당하므로 여기선 **트랜스포트 선언만** 검사한다.

## 엔드포인트

```
GET /admin/notifications/stream
Accept: text/event-stream
Role: SYSTEM_ADMIN 또는 CENTER_ADMIN
```

응답:

```
Content-Type: text/event-stream;charset=UTF-8
Cache-Control: no-cache
X-Accel-Buffering: no
```

연결 수립 즉시 `event: ready` 1건 송신. 이후:

| event | 데이터 | 트리거 | 클라 동작 |
|---|---|---|---|
| `notification-badge` | `<span id="admin-notif-badge" ...>N</span>` fragment HTML | 매 신규/병합 알림 발행 | `sse-swap="notification-badge"` + `hx-swap="outerHTML"` 로 배지 교체 + 0.6s flash |
| `notification-item` | `<li class="admin-notif-item ...">...</li>` fragment HTML | 신규 알림 발행만 (병합 제외, Q2) | 드롭다운 열려 있을 때 `.admin-notif-list` 상단 prepend |
| `heartbeat` | `ping` | 서버 25s 주기 (Q6) | 클라 무시 — 프록시 keep-alive 용 |
| `ready` | `connected` | 연결 수립 직후 1회 | 클라 로그만 |

## HTML 선언 (계약 검사 대상)

```html
<div class="admin-header-notif"
     hx-ext="sse"
     sse-connect="/admin/notifications/stream">
  <button class="admin-header-bell" ...>
    <span sse-swap="notification-badge" hx-swap="outerHTML" id="admin-notif-badge">...</span>
  </button>
  <div id="admin-notif-dropdown"
       sse-swap="notification-item"
       hx-target=".admin-notif-list"
       hx-swap="afterbegin"
       hidden></div>
</div>
```

## 결정 사항 (spec §Q 반영)

| Q | 결정 |
|---|---|
| Q0 | SSE + HTMX sse-ext |
| Q1 | admin 벨만 (사용자 트랙은 `A7-realtime-user` 로 이월) |
| Q2 | 리스너 분기 — merge → badge만, 신규 → badge + item |
| Q3 | 재연결 시 unread count 전체 refetch (Last-Event-ID 생략) |
| Q4 | 0.6s bg flash — POLICY P-5 적용 (prototype 미명시) |
| Q5 | sse-ext 내장 재연결만 (polling fallback 없음) |
| Q6 | heartbeat 25s |
| Q7 | HTML fragment (JSON 아님) |

## 재연결 동작

브라우저 `EventSource` 는 끊김 시 기본 ~3s 뒤 자동 재연결. 재연결 성공 시 `htmx:sseReconnected` 이벤트 발생. Q3 결정으로 Last-Event-ID 기반 유실 보정은 하지 않는다 — 재연결 즉시 새로운 알림 발생 시 badge count 가 최신값으로 반영되고, 드롭다운 열어 보면 전체 목록이 재조회된다.

## deviation / deferred

| 항목 | 사유 |
|---|---|
| 사용자 트랙 SSE | **deferred: A7-realtime-user** — 사용자 벨은 polling 자체가 없는 SSR fragment |
| Last-Event-ID 재조회 | **deviation (Q3)** — 재연결 시 unread count 전체 refetch 로 단순화 |
| polling fallback | **deviation (Q5)** — HTMX sse-ext 내장 재연결로 충분 |
| 다중 인스턴스 pub/sub | **deferred: A7-realtime-scaling** — Redis pub/sub 는 수평 확장 시 티켓 분리 |
| Web Push | **deferred: A7-webpush** |
| 0.6s bg flash | 본 티켓 포함 (Q4). POLICY P-5 — prototype 미명시 개선 |

## 회귀 방어

1. 사용자 트랙 (`/notifications`) 무영향 — 사용자 알림 flow 는 SSE 미사용
2. 기존 admin endpoint (`/admin/notifications/{dropdown,badge,{id}/read,mark-all-read,{id}/delete}`) 응답 포맷 무변경 — 벨 클릭 fetch 는 HTMX 그대로 재사용
3. A7-rate-limit `createOrMerge` 로직 무수정 — SSE broadcast 는 병합 **후** 트리거 (Q2)
4. HTMX 다른 영역 (admin 검색·유저 드롭다운·알림 drop 클릭) 무영향 — sse-ext script 는 htmx 코어 뒤에 defer 로드
