/**
 * 법적 문서 3종 (`/privacy`, `/terms`, `/email-policy`) 디자인 계약.
 *
 * 추출 기준: HANDOFF.md L629~L652 (wireframe 스펙).
 * prototype.tsx 에는 페이지 구현 없이 링크 이름 배열만 존재 (L468).
 *
 * 도입 목적 (260819): 푸터 3개 링크 연결 + 사이드바 레이아웃 신설과 함께,
 * 세 페이지의 시각 정합성을 정량 갭으로 감지할 수 있게 한다.
 */

import type { ScreenContract, Check } from './types';

/** 공통 사이드바 검사 헬퍼 (3개 페이지 공통). */
function sidebarChecks(activeText: string): Check[] {
    return [
        {
            id: 'sidebar.width',
            desc: '사이드바 폭 220',
            selector: '.policy-sidebar',
            kind: 'box',
            prop: 'width',
            expected: 220,
            tolerance: 1,
            proto: 'HANDOFF.md L633 사이드바 220px',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'sidebar.label.text',
            desc: '사이드바 레이블 "법적 문서"',
            selector: '.policy-sidebar-label',
            kind: 'text',
            expected: '법적 문서',
            proto: 'HANDOFF.md L633',
            states: ['anon'],
            severity: 'P2',
        },
        {
            id: 'sidebar.nav.count',
            desc: '사이드바 링크 3개',
            selector: '.policy-nav-item',
            kind: 'count',
            expected: 3,
            proto: 'HANDOFF.md L634~L636 3개 항목',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'sidebar.active.text',
            desc: `활성 링크 = "${activeText}"`,
            selector: '.policy-nav-item.is-active',
            kind: 'text',
            expected: activeText,
            proto: 'HANDOFF.md L634',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'sidebar.active.bg',
            desc: '활성 링크 배경 primary-bg',
            selector: '.policy-nav-item.is-active',
            kind: 'css',
            prop: 'background-color',
            expected: 'oklch(0.96 0.0255 280)',
            proto: 'HANDOFF.md L634 primaryBg (--color-primary-bg)',
            states: ['anon'],
            severity: 'P2',
        },
        {
            id: 'sidebar.active.border-color',
            desc: '활성 링크 테두리 primary 색',
            selector: '.policy-nav-item.is-active',
            kind: 'css',
            prop: 'border-top-color',
            expected: 'rgb(63, 48, 233)',
            proto: 'HANDOFF.md L634 primary 테두리',
            states: ['anon'],
            severity: 'P2',
        },
        {
            id: 'title.font-size',
            desc: 'h1 크기 26px',
            selector: '.policy-page-title',
            kind: 'css',
            prop: 'font-size',
            expected: '26px',
            proto: 'HANDOFF.md L638 h1 제목',
            states: ['anon'],
            severity: 'P2',
        },
    ];
}

export const privacyContract: ScreenContract = {
    screen: 'privacy',
    path: '/privacy',
    source: 'HANDOFF.md L629~L642 · 260819 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        ...sidebarChecks('개인정보처리방침'),
        {
            id: 'privacy.section.h2',
            desc: '장 제목 h2 크기 16px',
            selector: '.policy-section h2',
            kind: 'css',
            prop: 'font-size',
            expected: '16px',
            proto: 'HANDOFF.md L639 장 제목 16/700',
            states: ['anon'],
            severity: 'P2',
        },
        {
            id: 'privacy.table.exists',
            desc: '수집 항목 3열 테이블',
            selector: '.policy-table',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF.md L640 수집 항목 테이블 (3열)',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'privacy.table.columns',
            desc: '테이블 헤더 3개',
            selector: '.policy-table thead th',
            kind: 'count',
            expected: 3,
            proto: 'HANDOFF.md L640',
            states: ['anon'],
            severity: 'P1',
        },
    ],
};

export const termsContract: ScreenContract = {
    screen: 'terms',
    path: '/terms',
    source: 'HANDOFF.md L629~L642 · 260819 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        ...sidebarChecks('이용약관'),
        {
            id: 'terms.section.h2',
            desc: '조 제목 h2 크기 16px',
            selector: '.policy-section h2',
            kind: 'css',
            prop: 'font-size',
            expected: '16px',
            proto: 'HANDOFF.md L639',
            states: ['anon'],
            severity: 'P2',
        },
    ],
};

export const emailPolicyContract: ScreenContract = {
    screen: 'email-policy',
    path: '/email-policy',
    source: 'HANDOFF.md L646~L652 · 260819 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        ...sidebarChecks('이메일 무단 수집거부'),
        {
            id: 'email.hero.icon',
            desc: 'primary 원 이메일 아이콘',
            selector: '.policy-email-icon',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF.md L649 이메일 아이콘(primary 원)',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'email.hero.icon.size',
            desc: '아이콘 원 72×72',
            selector: '.policy-email-icon',
            kind: 'box',
            prop: 'width',
            expected: 72,
            tolerance: 1,
            proto: 'HANDOFF.md L649 아이콘 원',
            states: ['anon'],
            severity: 'P2',
        },
        {
            id: 'email.hero.icon.x',
            desc: '빨간 X 뱃지',
            selector: '.policy-email-icon-x',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF.md L649 빨간 X',
            states: ['anon'],
            severity: 'P1',
        },
        {
            id: 'email.hero.icon.x.bg',
            // 260819: wireframe L649 "빨간 X" 였으나 브랜드 정합성 위해 primary 로 변경 (사용자 지시).
            desc: 'X 뱃지 배경 primary 색',
            selector: '.policy-email-icon-x',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(63, 48, 233)',
            proto: 'HANDOFF.md L649 (원문 "빨간 X" · 260819 primary 로 변경)',
            states: ['anon'],
            severity: 'P2',
        },
        {
            // 260819 통일성 수정: privacy/terms 와 layout 구조 통일 (왼쪽 정렬 + .policy-section 재사용).
            // .policy-email-card 와 .policy-content--centered 는 제거 → 관련 계약 항목 삭제.
            id: 'email.section.h2-or-p',
            desc: '본문 .policy-section 컨테이너 존재',
            selector: '.policy-section',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF.md L651 안내 텍스트 (260819 카드 → .policy-section 통일)',
            states: ['anon'],
            severity: 'P1',
        },
    ],
};
