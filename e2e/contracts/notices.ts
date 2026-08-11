/**
 * 공지사항 목록 화면 (`/notices`) 디자인 계약 — MVP 시연용.
 *
 * 도입 (2026-08-11): 사용자 트랙 남은 9화면 계약 시리즈 3번째 (login #138 · signup #139 이후).
 * prototype `NoticesScreen` (tsx L2027~2088) 기준. 5-column 목록 그리드, pill 카테고리 탭, 3버튼 페이지네이션.
 *
 * 함께: `docs/design-contracts/notices.md` — 아키텍처·상태머신·CTA·POLICY.
 * 상세 화면: `notice-detail.ts` / `notice-detail.md`.
 */

import type { ScreenContract } from './types';

export const noticesContract: ScreenContract = {
    screen: 'notices',
    path: '/notices',
    source: 'prototype.tsx NoticesScreen L2027~2088 · 2026-08-11 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 페이지 제목
        // ══════════════════════════════════════════════════════
        {
            id: 'title.text',
            desc: '페이지 제목 "공지사항" (prototype L2040)',
            selector: '.page-title-bar h2',
            kind: 'text',
            expected: '공지사항',
            proto: 'tsx L2040 h2 공지사항',
            severity: 'P1',
        },
        {
            id: 'title.font-size',
            desc: '페이지 제목 font-size 28 (prototype L2040)',
            selector: '.page-title-bar h2',
            kind: 'css',
            prop: 'font-size',
            expected: '28px',
            proto: 'tsx L2040 fontSize:28',
            severity: 'P2',
        },
        {
            id: 'title.font-weight',
            desc: '페이지 제목 font-weight 700',
            selector: '.page-title-bar h2',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L2040 fontWeight:700',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 카테고리 pill 탭
        // ══════════════════════════════════════════════════════
        {
            id: 'tabs.count',
            desc: '카테고리 탭 5개 (전체·행사·공지·운영·기타)',
            selector: '.notice-tab',
            kind: 'count',
            expected: 5,
            proto: 'tsx L2031 cats=[\'전체\',\'행사\',\'공지\',\'운영\',\'기타\']',
            severity: 'P0',
        },
        {
            id: 'tabs.gap',
            desc: '카테고리 탭 gap 8 (prototype L2044)',
            selector: '.notice-tabs',
            kind: 'css',
            prop: 'gap',
            expected: '8px',
            proto: 'tsx L2044 gap:8',
            severity: 'P2',
        },
        {
            id: 'tab.padding',
            desc: '탭 pill padding 8/18 (prototype L2046)',
            selector: '.notice-tab',
            kind: 'css',
            prop: 'padding-top',
            expected: '8px',
            proto: 'tsx L2046 padding:8px 18px',
            severity: 'P2',
        },
        {
            id: 'tab.radius',
            desc: '탭 pill radius (prototype L2046 T.tagR=pill)',
            selector: '.notice-tab',
            kind: 'css',
            prop: 'border-radius',
            expected: '20px',
            proto: 'tsx L2046 borderRadius:T.tagR · --radius-pill=20',
            severity: 'P2',
        },
        {
            id: 'tab.font-size',
            desc: '탭 font-size 13.5 (prototype L2046)',
            selector: '.notice-tab',
            kind: 'css',
            prop: 'font-size',
            expected: '13.5px',
            proto: 'tsx L2046 fontSize:13.5',
            severity: 'P2',
        },
        {
            id: 'tab.active-bg',
            desc: '전체 탭 active 시 primary 배경 (prototype L2046)',
            selector: '.notice-tab.active',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(63, 48, 233)',
            proto: 'tsx L2046 cat===c ? T.primary',
            severity: 'P1',
        },
        {
            id: 'tab.active-color',
            desc: '전체 탭 active 시 글자 흰색',
            selector: '.notice-tab.active',
            kind: 'css',
            prop: 'color',
            expected: 'rgb(255, 255, 255)',
            proto: 'tsx L2046 color:\'#fff\'',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 목록 테이블 헤더
        // ══════════════════════════════════════════════════════
        {
            id: 'table.border-top',
            desc: '테이블 border-top 1px solid text (prototype L2049)',
            selector: '.notice-table',
            kind: 'css',
            prop: 'border-top-width',
            expected: '1px',
            proto: 'tsx L2049 borderTop:1px solid T.text',
            severity: 'P2',
        },
        {
            id: 'thead.grid',
            desc: '헤더 display:grid sanity (prototype L2050 grid 5칸)',
            selector: '.notice-thead',
            kind: 'css',
            prop: 'display',
            expected: 'grid',
            proto: 'tsx L2050 display:grid gridTemplateColumns:\'80px 80px 1fr 120px 80px\'',
            severity: 'P2',
        },
        {
            id: 'thead.padding',
            desc: '헤더 padding 10/16 (prototype L2050)',
            selector: '.notice-thead',
            kind: 'css',
            prop: 'padding-top',
            expected: '10px',
            proto: 'tsx L2050 padding:10px 16px',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 목록 행 (row)
        // ══════════════════════════════════════════════════════
        {
            id: 'row.exists',
            desc: '목록 행 최소 1건 이상 렌더 (seed 데이터 검증)',
            selector: '.notice-row',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2056 rows.map',
            severity: 'P0',
        },
        {
            id: 'row.padding',
            desc: '행 padding 14/16 (prototype L2056)',
            selector: '.notice-row',
            kind: 'css',
            prop: 'padding-top',
            expected: '14px',
            proto: 'tsx L2056 padding:14px 16px',
            severity: 'P2',
        },
        {
            id: 'row.grid',
            desc: '행 grid-template-columns 5칸 (prototype L2056)',
            selector: '.notice-row',
            kind: 'css',
            prop: 'display',
            expected: 'grid',
            proto: 'tsx L2056 display:grid',
            severity: 'P1',
        },
        {
            id: 'row.title-font-size',
            desc: '제목 텍스트 font-size 14 (prototype L2061)',
            selector: '.notice-row .notice-title-text',
            kind: 'css',
            prop: 'font-size',
            expected: '14px',
            proto: 'tsx L2061 fontSize:14',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 카테고리 배지 (badge)
        // ══════════════════════════════════════════════════════
        {
            id: 'badge.exists',
            desc: '카테고리 배지 최소 1개 (prototype L2058)',
            selector: '.category-badge',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2058 span badge',
            severity: 'P1',
        },
        {
            id: 'badge.font-size',
            desc: '배지 font-size 11 (prototype L2058)',
            selector: '.notice-row .category-badge',
            kind: 'css',
            prop: 'font-size',
            expected: '11px',
            proto: 'tsx L2058 fontSize:11',
            severity: 'P2',
        },
        {
            id: 'badge.font-weight',
            desc: '배지 font-weight 600 (prototype L2058)',
            selector: '.notice-row .category-badge',
            kind: 'css',
            prop: 'font-weight',
            expected: '600',
            proto: 'tsx L2058 fontWeight:600',
            severity: 'P2',
        },
        {
            id: 'badge.radius',
            desc: '배지 pill radius (prototype L2058 T.tagR)',
            selector: '.notice-row .category-badge',
            kind: 'css',
            prop: 'border-radius',
            expected: '20px',
            proto: 'tsx L2058 borderRadius:T.tagR',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 페이지네이션 (seed 20개+ 있어야 유효 — 없으면 count=0)
        // ══════════════════════════════════════════════════════
        {
            id: 'pagination.page-btn.size',
            desc: '페이지 버튼 32×32 (prototype L2071)',
            selector: '.notice-pagination .page-btn',
            kind: 'box',
            prop: 'height',
            expected: 32,
            tolerance: 1,
            proto: 'tsx L2071 width:32 height:32',
            severity: 'P2',
            deferred: 'seed 데이터가 페이지네이션 노출 임계(> pageSize) 이상일 때만 유효. 현재 seed 정책 미확정 → docs/specs/F-notices-seed-volume.md',
        },
        {
            id: 'pagination.page-btn.radius',
            desc: '페이지 버튼 radius 7 (prototype L2071)',
            selector: '.notice-pagination .page-btn',
            kind: 'css',
            prop: 'border-radius',
            expected: '7px',
            proto: 'tsx L2071 borderRadius:7',
            severity: 'P2',
            deferred: 'seed 데이터 임계 확정 후 활성화',
        },
    ],
};
