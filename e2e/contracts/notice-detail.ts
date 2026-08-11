/**
 * 공지사항 상세 화면 (`/notices/{id}`) 디자인 계약 — MVP 시연용.
 *
 * 도입 (2026-08-11): 사용자 트랙 남은 9화면 계약 시리즈. `/notices/1` (seed 첫 공지) 를 path 로 지정한다.
 * prototype `NoticeDetail` (tsx L2091~2144) 기준.
 *
 * F-notice-attachment (PR #130 · 2026-07-31) 로 실 첨부 다운로드 UI 도입됨 → 계약에도 반영.
 * 목록 계약: `notices.ts` / `notices.md`.
 */

import type { ScreenContract } from './types';

export const noticeDetailContract: ScreenContract = {
    screen: 'notice-detail',
    path: '/notices/1',
    source: 'prototype.tsx NoticeDetail L2091~2144 · 2026-08-11 신설 (F-notice-attachment PR #130 반영)',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 컨테이너
        // ══════════════════════════════════════════════════════
        {
            id: 'container.max-width',
            desc: 'notice-detail max-width 900 (prototype L2097)',
            selector: '.notice-detail',
            kind: 'css',
            prop: 'max-width',
            expected: '900px',
            proto: 'tsx L2097 maxWidth:900',
            severity: 'P1',
        },
        {
            id: 'container.padding-top',
            desc: 'notice-detail padding-top 32 (prototype L2097)',
            selector: '.notice-detail',
            kind: 'css',
            prop: 'padding-top',
            expected: '32px',
            proto: 'tsx L2097 padding:32px 24px 56px',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 목록으로 돌아가기 버튼 (뒤로가기 아이콘)
        // ══════════════════════════════════════════════════════
        {
            id: 'back-btn.size',
            desc: '뒤로가기 버튼 38×38 (prototype L2098)',
            selector: '.notice-back-btn',
            kind: 'box',
            prop: 'height',
            expected: 38,
            tolerance: 1,
            proto: 'tsx L2098 width:38 height:38',
            severity: 'P2',
        },
        {
            id: 'back-btn.radius',
            desc: '뒤로가기 버튼 radius 9 (prototype L2098)',
            selector: '.notice-back-btn',
            kind: 'css',
            prop: 'border-radius',
            expected: '9px',
            proto: 'tsx L2098 borderRadius:9',
            severity: 'P2',
        },
        {
            id: 'back-btn.svg',
            desc: '뒤로가기 아이콘 SVG (POLICY P-3, prototype L2099 Icon arrowL)',
            selector: '.notice-back-btn svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2099 Icon n=arrowL · tsx L76 arrowL is SVG path',
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // 배지 · 제목 · 메타
        // ══════════════════════════════════════════════════════
        {
            id: 'badges.exists',
            desc: '카테고리 배지 존재 (prototype L2102)',
            selector: '.notice-detail-badges .category-badge',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2102 span category badge',
            severity: 'P1',
        },
        {
            id: 'title.font-size',
            desc: '상세 제목 font-size 26 (prototype L2105)',
            selector: '.notice-detail-title',
            kind: 'css',
            prop: 'font-size',
            expected: '26px',
            proto: 'tsx L2105 fontSize:26',
            severity: 'P1',
        },
        {
            id: 'title.font-weight',
            desc: '상세 제목 font-weight 700',
            selector: '.notice-detail-title',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L2105 fontWeight:700',
            severity: 'P2',
        },
        {
            id: 'meta.font-size',
            desc: '작성일·조회 메타 font-size 13 (prototype L2106)',
            selector: '.notice-detail-meta',
            kind: 'css',
            prop: 'font-size',
            expected: '13px',
            proto: 'tsx L2106 fontSize:13',
            severity: 'P2',
        },
        {
            id: 'meta.gap',
            desc: '메타 gap 16 (prototype L2106)',
            selector: '.notice-detail-meta',
            kind: 'css',
            prop: 'gap',
            expected: '16px',
            proto: 'tsx L2106 gap:16',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 본문
        // ══════════════════════════════════════════════════════
        {
            id: 'body.font-size',
            desc: '본문 font-size 15 (prototype L2111)',
            selector: '.notice-detail-body',
            kind: 'css',
            prop: 'font-size',
            expected: '15px',
            proto: 'tsx L2111 fontSize:15',
            severity: 'P2',
        },
        {
            id: 'body.line-height',
            desc: '본문 line-height 1.85 (prototype L2111)',
            selector: '.notice-detail-body',
            kind: 'css',
            prop: 'line-height',
            expected: '27.75px',
            proto: 'tsx L2111 lineHeight:1.85 · 15 * 1.85',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 첨부파일 UI (F-notice-attachment PR #130)
        // ══════════════════════════════════════════════════════
        {
            id: 'attachment.exists',
            desc: '첨부파일 링크 존재 (seed 첫 공지에 첨부 최소 1건)',
            selector: '.notice-attachment',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2123 첨부파일 block · PR #130 실제 다운로드 링크',
            severity: 'P0',
        },
        {
            id: 'attachment.padding',
            desc: '첨부 항목 padding 14/16 (prototype L2123)',
            selector: '.notice-attachment',
            kind: 'css',
            prop: 'padding-top',
            expected: '14px',
            proto: 'tsx L2123 padding:14px 16px',
            severity: 'P2',
        },
        {
            id: 'attachment.radius',
            desc: '첨부 항목 radius (prototype L2123 T.radius = --radius-md 8)',
            selector: '.notice-attachment',
            kind: 'css',
            prop: 'border-radius',
            expected: '8px',
            proto: 'tsx L2123 borderRadius:T.radius · --radius-md=8',
            severity: 'P2',
        },
        {
            id: 'attachment.icon.svg',
            desc: '첨부 다운로드 아이콘 SVG (POLICY P-3, prototype L2124 Icon download)',
            selector: '.notice-attachment svg, .notice-attachment-icon svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2124 Icon n=download · tsx L73 download is SVG path',
            severity: 'P0',
        },
        {
            id: 'attachment.name.font-size',
            desc: '첨부 파일명 font-size 14 (prototype L2125)',
            selector: '.notice-attachment-name',
            kind: 'css',
            prop: 'font-size',
            expected: '14px',
            proto: 'tsx L2125 fontSize:14',
            severity: 'P2',
        },
        {
            id: 'attachment.size.font-size',
            desc: '첨부 크기 font-size 12 (prototype L2126)',
            selector: '.notice-attachment-size',
            kind: 'css',
            prop: 'font-size',
            expected: '12px',
            proto: 'tsx L2126 fontSize:12',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 이전글 / 다음글
        // ══════════════════════════════════════════════════════
        {
            id: 'adjacent.exists',
            desc: '이전글·다음글 네비게이션 영역 존재 (prototype L2129~2136)',
            selector: '.notice-adjacent',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2129 이전/다음 블록',
            severity: 'P1',
        },
        {
            id: 'adjacent-row.padding',
            desc: '인접 글 row padding 14/8 (prototype L2131)',
            selector: '.notice-adjacent-row',
            kind: 'css',
            prop: 'padding-top',
            expected: '14px',
            proto: 'tsx L2131 padding:14px 8px',
            severity: 'P2',
        },
        {
            id: 'adjacent-label.font-size',
            desc: '이전글/다음글 라벨 font-size 13 (prototype L2132)',
            selector: '.notice-adjacent-label',
            kind: 'css',
            prop: 'font-size',
            expected: '13px',
            proto: 'tsx L2132 fontSize:13',
            severity: 'P2',
        },
        {
            id: 'adjacent-label.font-weight',
            desc: '이전글/다음글 라벨 font-weight 600',
            selector: '.notice-adjacent-label',
            kind: 'css',
            prop: 'font-weight',
            expected: '600',
            proto: 'tsx L2132 fontWeight:600',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 하단 "목록으로" 버튼
        // ══════════════════════════════════════════════════════
        {
            id: 'list-btn.exists',
            desc: '하단 "목록으로" 버튼 존재 (prototype L2138)',
            selector: '.notice-detail-actions a',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2138 Btn variant=secondary onClick=>go(\'notices\')',
            severity: 'P1',
        },
        {
            id: 'list-btn.text',
            desc: '하단 목록 버튼 텍스트 "목록으로"',
            selector: '.notice-detail-actions a',
            kind: 'text',
            expected: '목록으로',
            proto: 'tsx L2138 목록으로',
            severity: 'P2',
        },
    ],
};
