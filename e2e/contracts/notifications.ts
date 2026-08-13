/**
 * 알림 목록 화면 (`/notifications`) 디자인 계약 — 사용자 트랙 계약 시리즈 5번째.
 *
 * 도입 (2026-08-12): login #138 · signup #139 · notices #140 · apply #141 이후.
 * prototype `NotificationsScreen` (tsx L1252~1314) 기준.
 *
 * 인증: 로그인 필수 (SecurityConfig `/notifications/**` authenticated).
 *   → visual-notifications.spec.ts 는 helpers.login 으로 seed 계정 진입.
 *
 * side-effect: 렌더 자체는 안전 (GET). 계약 검사 단계에서는 `POST /notifications/read-all`
 *   또는 개별 `read` 를 발생시키지 않는다 → rotation pool 불필요.
 *   단, seed 사용자가 미읽음 알림을 최소 1건 보유해야 unread-badge · mark-all-btn 이 노출된다.
 *   (DataInitializer L182~203 에서 4건 시드 · 일부 unread=true)
 *
 * 갭 이력 (2026-08-12 impl 로 청산 완료):
 *  - notif-icon SVG 이식 (Q-1 A) — fragments/icons.html fragment 로 이식 완료
 *  - notif-page max-width 720 → 680 (Q-4 P2) 정합 완료
 *  - filter pill padding 6/14 → 7/15 (Q-4 P2) 정합 완료
 *  - filter pill active 색: prototype 옅은 primary-bg (Q-2) — notifications 전용 조정 완료.
 *    notices `.notice-tab.active` 는 prototype 이 solid primary 이므로 유지 (각 화면 실측 정합)
 *  - close(x) 삭제 버튼 (Q-3) — POST /notifications/{id}/delete + hx-swap="delete" 로 신설 완료
 *  - "안 읽음" 빈 상태 문구: prototype "안 읽은 알림이 없어요" vs 구현 "읽지 않은 알림이 없어요".
 *    POLICY P-1 카피 현행 유지 → deviation
 *
 * 함께: `docs/design-contracts/notifications.md` — 아키텍처·상태머신·CTA·POLICY 매핑·§8 결정 Q.
 */

import type { ScreenContract } from './types';

export const notificationsContract: ScreenContract = {
    screen: 'notifications',
    path: '/notifications',
    source: 'prototype.tsx NotificationsScreen L1252~1314 · 2026-08-12 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 컨테이너 폭
        // ══════════════════════════════════════════════════════
        {
            id: 'container.max-width',
            desc: 'notif-page max-width 680 (prototype L1264)',
            selector: '.notif-page',
            kind: 'css',
            prop: 'max-width',
            expected: '680px',
            proto: 'tsx L1264 maxWidth:680',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 뒤로가기 버튼 (독립 행)
        // ══════════════════════════════════════════════════════
        {
            id: 'back-btn.exists',
            desc: '뒤로가기 버튼 존재 (prototype L1265)',
            selector: '.notif-back-btn',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1265 arrowL 버튼',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'back-btn.width',
            desc: '뒤로가기 버튼 width 38 (prototype L1265)',
            selector: '.notif-back-btn',
            kind: 'box',
            prop: 'width',
            expected: 38,
            tolerance: 1,
            proto: 'tsx L1265 width:38',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'back-btn.height',
            desc: '뒤로가기 버튼 height 38 (prototype L1265)',
            selector: '.notif-back-btn',
            kind: 'box',
            prop: 'height',
            expected: 38,
            tolerance: 1,
            proto: 'tsx L1265 height:38',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'back-btn.radius',
            desc: '뒤로가기 버튼 radius 9 (prototype L1265)',
            selector: '.notif-back-btn',
            kind: 'css',
            prop: 'border-radius',
            expected: '9px',
            proto: 'tsx L1265 borderRadius:9',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'back-btn.svg',
            desc: '뒤로가기 버튼 내부 SVG (POLICY P-3)',
            selector: '.notif-back-btn svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1266 Icon n="arrowL"',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 페이지 타이틀 + unread 뱃지
        // ══════════════════════════════════════════════════════
        {
            id: 'title.text',
            desc: '페이지 제목 "알림" (prototype L1270)',
            selector: '.notif-page-title',
            kind: 'text',
            expected: '알림',
            proto: 'tsx L1270 h2 알림',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'title.font-size',
            desc: '페이지 제목 font-size 24 (prototype L1270)',
            selector: '.notif-page-title',
            kind: 'css',
            prop: 'font-size',
            expected: '24px',
            proto: 'tsx L1270 fontSize:24',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'title.font-weight',
            desc: '페이지 제목 font-weight 700',
            selector: '.notif-page-title',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L1270 fontWeight:700',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'unread-badge.exists',
            desc: '미읽음 뱃지 존재 (seed 알림 unread ≥ 1 전제, prototype L1271)',
            selector: '.notif-unread-badge',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1271 unread>0 span badge',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'unread-badge.bg',
            desc: '미읽음 뱃지 배경 error 컬러 (prototype L1271)',
            selector: '.notif-unread-badge',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(239, 68, 68)',
            proto: 'tsx L1271 background:T.error',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'unread-badge.font-size',
            desc: '미읽음 뱃지 font-size 12 (prototype L1271)',
            selector: '.notif-unread-badge',
            kind: 'css',
            prop: 'font-size',
            expected: '12px',
            proto: 'tsx L1271 fontSize:12',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // "모두 읽음" 버튼
        // ══════════════════════════════════════════════════════
        {
            id: 'mark-all.exists',
            desc: '모두 읽음 버튼 존재 (unread>0 전제, prototype L1273)',
            selector: '.notif-mark-all-btn',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1273 모두 읽음',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'mark-all.text',
            desc: '모두 읽음 버튼 라벨',
            selector: '.notif-mark-all-btn',
            kind: 'text',
            expected: '모두 읽음',
            proto: 'tsx L1273 모두 읽음',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'mark-all.svg',
            desc: '모두 읽음 버튼 내부 check SVG (POLICY P-3, prototype L1273)',
            selector: '.notif-mark-all-btn svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1273 Icon n="check"',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 필터 pill (전체 / 안 읽음)
        // ══════════════════════════════════════════════════════
        {
            id: 'filter.count',
            desc: '필터 pill 2개 (전체 · 안 읽음, prototype L1277)',
            selector: '.notif-filter-pill',
            kind: 'count',
            expected: 2,
            proto: 'tsx L1277 [전체, 안 읽음]',
            severity: 'P0',
        },
        {
            id: 'filter.bar.gap',
            desc: '필터 바 gap 6 (prototype L1276)',
            selector: '.notif-filter-bar',
            kind: 'css',
            prop: 'gap',
            expected: '6px',
            proto: 'tsx L1276 gap:6',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'filter.pill.radius',
            desc: '필터 pill radius pill=999 (prototype L1278)',
            selector: '.notif-filter-pill',
            kind: 'css',
            prop: 'border-radius',
            expected: '20px',
            proto: 'tsx L1278 borderRadius:999 · --radius-pill=20',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'filter.pill.font-size',
            desc: '필터 pill font-size 13.5 (prototype L1278)',
            selector: '.notif-filter-pill',
            kind: 'css',
            prop: 'font-size',
            expected: '13.5px',
            proto: 'tsx L1278 fontSize:13.5',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'filter.active.bg',
            desc: '활성 필터 배경 primary-bg (prototype L1278 filter===f ? T.primaryBg)',
            selector: '.notif-filter-pill--active',
            kind: 'css',
            prop: 'background-color',
            expected: 'oklch(0.96 0.0255 280)',
            proto: 'tsx L1278 background:filter===f?T.primaryBg:T.surface',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'filter.count-badge.exists',
            desc: '필터 pill 안 카운트 span (prototype L1279)',
            selector: '.notif-filter-pill .notif-filter-count',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1279 <span>{n}</span>',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 그룹 헤더 (오늘 / 지난 7일 / 이전)
        // ══════════════════════════════════════════════════════
        {
            id: 'group.header.exists',
            desc: '그룹 헤더 최소 1개 (prototype L1290)',
            selector: '.notif-group-header',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1290 오늘/지난 7일/이전',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'group.header.font-size',
            desc: '그룹 헤더 font-size 13 (prototype L1290)',
            selector: '.notif-group-header',
            kind: 'css',
            prop: 'font-size',
            expected: '13px',
            proto: 'tsx L1290 fontSize:13',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'group.header.font-weight',
            desc: '그룹 헤더 font-weight 700 (prototype L1290)',
            selector: '.notif-group-header',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L1290 fontWeight:700',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 리스트 아이템
        // ══════════════════════════════════════════════════════
        {
            id: 'item.exists',
            desc: '알림 행 최소 1건 (seed 4건 전제)',
            selector: '.notif-page-item',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1293 arr.map',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'item.padding',
            desc: '알림 행 padding 16/18 (prototype L1293)',
            selector: '.notif-page-item',
            kind: 'css',
            prop: 'padding-top',
            expected: '16px',
            proto: 'tsx L1293 padding:16px 18px',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'item.gap',
            desc: '알림 행 gap 12 (prototype L1293)',
            selector: '.notif-page-item',
            kind: 'css',
            prop: 'gap',
            expected: '12px',
            proto: 'tsx L1293 gap:12',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'item.border-left',
            desc: '알림 행 border-left 3px (prototype L1293)',
            selector: '.notif-page-item',
            kind: 'css',
            prop: 'border-left-width',
            expected: '3px',
            proto: 'tsx L1293 borderLeft:3px solid',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'item.unread.border-color',
            desc: '미읽음 행 border-left primary 컬러 (prototype L1293)',
            selector: '.notif-page-item.notif-item--unread',
            kind: 'css',
            prop: 'border-left-color',
            expected: 'rgb(63, 48, 233)',
            proto: 'tsx L1293 item.unread ? 3px solid T.primary',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'item.unread.bg',
            desc: '미읽음 행 배경 primary-bg (prototype L1293)',
            selector: '.notif-page-item.notif-item--unread',
            kind: 'css',
            prop: 'background-color',
            expected: 'oklch(0.96 0.0255 280)',
            proto: 'tsx L1293 background:T.primaryBg',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 아이콘 원형 (POLICY P-3 강제 대상 — 현재 빈 div)
        // ══════════════════════════════════════════════════════
        {
            id: 'icon.width',
            desc: '아이콘 원형 width 36 (prototype L1294)',
            selector: '.notif-page-item .notif-icon',
            kind: 'box',
            prop: 'width',
            expected: 36,
            tolerance: 1,
            proto: 'tsx L1294 width:36',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'icon.height',
            desc: '아이콘 원형 height 36 (prototype L1294)',
            selector: '.notif-page-item .notif-icon',
            kind: 'box',
            prop: 'height',
            expected: 36,
            tolerance: 1,
            proto: 'tsx L1294 height:36',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'icon.radius',
            desc: '아이콘 원형 radius 50% (prototype L1294)',
            selector: '.notif-page-item .notif-icon',
            kind: 'css',
            prop: 'border-radius',
            expected: '50%',
            proto: 'tsx L1294 borderRadius:50%',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'icon.svg',
            desc: '아이콘 원형 안 SVG 렌더 (POLICY P-3 — prototype L1295 Icon n={item.icon})',
            selector: '.notif-page-item .notif-icon svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1295 Icon n={item.icon||"bell"} size:16',
            states: ['auth'],
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // 텍스트 (제목 · 본문 · 시각)
        // ══════════════════════════════════════════════════════
        {
            id: 'title.font-size-item',
            desc: '알림 제목 font-size 14.5 (prototype L1300)',
            selector: '.notif-page-item .notif-item-title',
            kind: 'css',
            prop: 'font-size',
            expected: '14.5px',
            proto: 'tsx L1300 fontSize:14.5',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'message.font-size',
            desc: '알림 본문 font-size 13.5 (prototype L1302)',
            selector: '.notif-page-item .notif-item-message',
            kind: 'css',
            prop: 'font-size',
            expected: '13.5px',
            proto: 'tsx L1302 fontSize:13.5',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'time.font-size',
            desc: '알림 시각 font-size 12 (prototype L1303)',
            selector: '.notif-page-item .notif-item-time',
            kind: 'css',
            prop: 'font-size',
            expected: '12px',
            proto: 'tsx L1303 fontSize:12',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 닫기(X) 삭제 버튼 — prototype L1305 · 현재 구현에 없음
        // ══════════════════════════════════════════════════════
        {
            id: 'item.close-btn',
            desc: '개별 삭제(X) 버튼 존재 (prototype L1305)',
            selector: '.notif-page-item .notif-item-close',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1305 close 아이콘 클릭 → remove(id)',
            states: ['auth'],
            severity: 'P2',
        },
    ],
};
