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
        // ── 드롭다운 오픈 후 구조 ─────────────────────────────
        // A7-e2e-suite (2026-09-28): 벨 클릭 후 dropdown 이 채워진 상태에서 재검사되는 계약.
        // `deferredOpen: true` 마킹된 항목은 러너가 조건부 skip — visual spec 은 초기 hidden 상태만 검사하고,
        // tests/visual-admin-header-notifications.spec.ts 이 __openDropdown 헬퍼 후 재실행한다.
        //
        // 이 계약이 활성화된 이유:
        //   - 초기 로드 시엔 .admin-notif-panel 이 DOM 에 없어 (dropdown wrapper 는 hidden 이라도 innerHTML 비어있음)
        //     러너가 자동 skip 하는 방식으로 안전 (count 검사도 0 이 나올 뿐 fail 안 함).
        {
            id: 'dropdown.width',
            desc: '드롭다운 300px 폭 (열린 상태에서 검사)',
            selector: '.admin-notif-panel',
            kind: 'box',
            prop: 'width',
            expected: 300,
            tolerance: 1,
            proto: 'admin/prototype.html L455 width:300',
            severity: 'P2',
        },
        {
            id: 'dropdown.panel.shadow',
            desc: '드롭다운 패널 shadow (prototype box-shadow:0 8px 30px rgba(0,0,0,0.15))',
            selector: '.admin-notif-panel',
            kind: 'css',
            prop: 'box-shadow',
            // 브라우저는 rgba(0,0,0,0.15) → 'rgba(0, 0, 0, 0.15) 0px 8px 30px 0px' 형태로 정규화
            expected: 'rgba(0, 0, 0, 0.15) 0px 8px 30px 0px',
            proto: 'admin/prototype.html L456 box-shadow',
            severity: 'P2',
        },
        // ── admin-notif-item 개별 DOM 계약 ────────────────────
        // Qn-5 (2026-09-28 A7-e2e-suite): 항목 마크업 계약 신설.
        // seed 알림 (AdminNotificationEventListener 가 만든 NEW_APPLICATION 계열) 이 최소 1건 있다는 전제.
        {
            id: 'item.count.max',
            desc: '드롭다운 항목 최대 표시 5건 (recentForHeader top-5 · prototype mock 3건 근거로 상한 5)',
            selector: '.admin-notif-item',
            kind: 'count-min',
            expected: 1, // 최소 1건은 있어야 함 (승인 seed). 상한 5 는 서비스 계층에서 이미 보장.
            proto: 'admin/prototype.html L2836 mock 3건 · AdminNotificationService.recentForHeader',
            severity: 'P1',
        },
        {
            id: 'item.unread.dot.exists',
            desc: '미읽음 항목에는 좌측/우측 unread dot 표시 (.admin-notif-unread-dot)',
            selector: '.admin-notif-item--unread .admin-notif-unread-dot',
            kind: 'count-min',
            expected: 1,
            proto: 'admin/prototype.html L484 width:7px;height:7px background:#3F30E9',
            severity: 'P1',
        },
        {
            id: 'item.time.exists',
            desc: '항목별 시간 표기 (<time class="admin-notif-time">) 존재',
            selector: '.admin-notif-item .admin-notif-time',
            kind: 'count-min',
            expected: 1,
            proto: 'admin/prototype.html L478 font-size:10px;color:#A6A3B3',
            severity: 'P2',
        },
        {
            id: 'item.delete.button',
            desc: '항목별 삭제 버튼 (.admin-notif-delete)',
            selector: '.admin-notif-item .admin-notif-delete',
            kind: 'count-min',
            expected: 1,
            proto: 'admin/prototype.html L480~482 삭제 버튼',
            severity: 'P1',
        },
    ],
};
