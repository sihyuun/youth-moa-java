/**
 * A7-realtime (2026-10-15) — admin 알림 SSE 트랜스포트 계약.
 *
 * admin 헤더 알림 벨이 SSE 로 구동됨을 보장. 외형(배지·드롭다운) 은 admin-header-notifications 가 커버하고,
 * 이 계약은 "실시간 push 트랜스포트" 의 존재·속성·엔드포인트 지정만 집중 검사한다.
 *
 * HTMX sse-ext 가 EventSource 를 자동 생성하므로 selector 로 wire-level SSE 는 직접 못 잡지만,
 * `hx-ext="sse"` + `sse-connect="/admin/notifications/stream"` + `sse-swap` 조합이 DOM 에 선언되었는지
 * 는 정확히 검사 가능.
 *
 * source: A7-realtime-notifications.md §7 변경 범위
 */

import type { ScreenContract } from './types';

export const adminNotificationSseContract: ScreenContract = {
    screen: 'admin-notification-sse',
    path: '/admin',
    source: 'docs/specs/A7-realtime-notifications.md Q0/Q7 · 2026-10-15 A7-realtime',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'sse.extension.declared',
            desc: 'HTMX sse 확장이 벨 wrapper 에 선언 (hx-ext~="sse")',
            selector: '.admin-header-notif[hx-ext~="sse"]',
            kind: 'count',
            expected: 1,
            proto: 'spec Q0 — SSE + HTMX sse-ext',
            severity: 'P0',
        },
        {
            id: 'sse.endpoint.exact',
            desc: 'sse-connect 가 정확히 /admin/notifications/stream',
            selector: '[sse-connect="/admin/notifications/stream"]',
            kind: 'count',
            expected: 1,
            proto: 'spec §7 신규 #2 — NotificationSseController endpoint',
            severity: 'P0',
        },
        {
            id: 'sse.swap.badge',
            desc: 'notification-badge 이벤트가 배지 span 에 바인딩',
            selector: '[sse-swap~="notification-badge"]',
            kind: 'count',
            expected: 1,
            proto: 'spec §8 이벤트 종류 — notification-badge',
            severity: 'P0',
        },
        {
            id: 'sse.swap.item',
            desc: 'notification-item 이벤트가 드롭다운 리스트에 바인딩',
            selector: '[sse-swap~="notification-item"]',
            kind: 'count',
            expected: 1,
            proto: 'spec §8 이벤트 종류 — notification-item',
            severity: 'P0',
        },
        {
            id: 'sse.swap.item.target',
            desc: 'notification-item swap 대상이 .admin-notif-list (열렸을 때 리스트 상단 prepend)',
            selector: '[sse-swap~="notification-item"][hx-target=".admin-notif-list"]',
            kind: 'count',
            expected: 1,
            proto: 'spec §8 — hx-swap="afterbegin" + hx-target=".admin-notif-list"',
            severity: 'P1',
        },
        {
            id: 'sse.swap.item.strategy',
            desc: 'notification-item swap 전략이 afterbegin (prepend)',
            selector: '[sse-swap~="notification-item"][hx-swap="afterbegin"]',
            kind: 'count',
            expected: 1,
            proto: 'spec §8 — 신규 항목은 리스트 상단에 prepend',
            severity: 'P1',
        },
        {
            id: 'polling.absent',
            desc: '기존 30s polling 이 완전히 제거됨 (SSE 로 치환됨을 보장)',
            selector: 'div[hx-trigger="every 30s"]',
            kind: 'count',
            expected: 0,
            proto: 'spec §1 포함 범위 — polling → SSE 교체',
            severity: 'P0',
        },
    ],
};
