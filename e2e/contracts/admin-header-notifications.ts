/**
 * A7 admin 헤더 알림 벨 (2026-09-17) 디자인 계약.
 *
 * 관리자 로그인 후 `/admin` 진입 상태에서 검사. 계약은 벨 존재·배지·드롭다운(클릭 후)·항목 구조 4계층으로 나눈다.
 * source: prototype admin `L443~494` (bell + dropdown) · `L2832~2836` (mock notifItems).
 *
 * 인터랙션 형태이므로 검사 spec (`tests/visual-admin-header-notifications.spec.ts`) 은 벨 클릭 후 dropdown 이 채워진 상태에서
 * runContract 를 호출해야 한다. seed 로 admin 계정에 미읽음 알림 최소 1건 필요 — E2E 픽스처에서 사전 삽입.
 *
 * 갭 이력: (초기 신설이라 없음)
 */

import type { ScreenContract } from './types';

export const adminHeaderNotificationsContract: ScreenContract = {
    screen: 'admin-header-notifications',
    path: '/admin',
    source: 'admin/prototype.html L443~494 · L2832~2836 · 2026-09-17 A7',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ── 벨 트리거 존재 ────────────────────────────────────
        {
            id: 'bell.exists',
            desc: '알림 벨 트리거 존재 (disabled 해제)',
            selector: '.admin-header-bell',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L444~453',
            severity: 'P0',
        },
        {
            id: 'bell.enabled',
            desc: '알림 벨 disabled 미부착 (A7 실동작)',
            selector: '.admin-header-bell[disabled]',
            kind: 'count',
            expected: 0,
            proto: 'A7: disabled 해제',
            severity: 'P0',
        },
        // ── 배지 (초기 렌더) ──────────────────────────────────
        {
            id: 'badge.selector',
            desc: '배지 span 존재 (data-notif-badge)',
            selector: '[data-notif-badge]',
            kind: 'exists',
            expected: true,
            proto: 'Qn-6: 배지 span DOM 상 존재 (unread=0 이면 hidden)',
            severity: 'P1',
        },
        {
            id: 'badge.id',
            desc: '#admin-notif-badge 정확한 id (polling swap 대상)',
            selector: '#admin-notif-badge',
            kind: 'exists',
            expected: true,
            proto: 'polling hx-target',
            severity: 'P0',
        },
        {
            // 2026-09-17 P0 재반려 회귀 방어: fragment 정의 중복(badge, badge(count) 두 개)으로 인해
            // <span id="admin-notif-badge"> 가 DOM 에 2회 렌더되던 사고. count===1 검사로 재발 차단.
            id: 'badge.id.unique',
            desc: '#admin-notif-badge id 중복 금지 (fragment 이름 충돌 회귀 방어)',
            selector: '#admin-notif-badge',
            kind: 'count',
            expected: 1,
            proto: 'HTML id 유일성 (polling outerHTML swap 은 첫 매치만 대체하므로 중복 시 잔존 span 이 data-notif-badge 를 잃음)',
            severity: 'P0',
        },
        // ── 드롭다운 (초기 hidden) ────────────────────────────
        {
            id: 'dropdown.hidden-initial',
            desc: '드롭다운 wrapper 초기 hidden',
            selector: '#admin-notif-dropdown[hidden]',
            kind: 'count',
            expected: 1,
            proto: 'A1 표준 — 트리거 클릭 시 열림',
            severity: 'P1',
        },
        {
            id: 'dropdown.wrapper.exists',
            desc: '드롭다운 wrapper 자체는 존재',
            selector: '#admin-notif-dropdown',
            kind: 'exists',
            expected: true,
            proto: 'hx-target 대상',
            severity: 'P0',
        },
        // ── 벨 배지 스타일 (Qn-Δ 실측) ────────────────────────
        {
            id: 'badge.background',
            desc: '배지 배경 #E72D0F (unread>0 상태 검사는 seed 로 별도 spec 필요)',
            selector: '[data-notif-badge]',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(231, 45, 15)',
            proto: 'prototype 배지 색 · admin.css .admin-notif-badge',
            severity: 'P2',
        },
        {
            id: 'badge.border-color',
            desc: '배지 다크 헤더 위 1.5px border #111827',
            selector: '[data-notif-badge]',
            kind: 'css',
            prop: 'border-top-color',
            expected: 'rgb(17, 24, 39)',
            proto: 'prototype 다크 헤더 대비',
            severity: 'P2',
        },
        // ── 폴링 wrapper 존재 확인 ────────────────────────────
        {
            id: 'polling.wrapper.exists',
            desc: '30s polling wrapper 존재 (hx-trigger=every 30s)',
            selector: 'div[hx-trigger="every 30s"]',
            kind: 'count-min',
            expected: 1,
            proto: 'Qn-1: 30s polling',
            severity: 'P1',
        },
        // ── 드롭다운 오픈 후 구조 (deferred: 인터랙션 spec 에서 재검증) ──
        {
            id: 'dropdown.width',
            desc: '드롭다운 300px 폭 (열린 상태에서 검사, deferred)',
            selector: '.admin-notif-panel',
            kind: 'box',
            prop: 'width',
            expected: 300,
            tolerance: 1,
            proto: 'admin/prototype.html L455 width:300',
            severity: 'P2',
            deferred: 'tests/admin-header-notifications.spec.ts — 클릭 후 상태 검증 (이번 티켓 이월)',
        },
    ],
};
