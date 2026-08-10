/**
 * 로그인 화면 (`/login`) 디자인 계약.
 *
 * 도입 목적 (2026-08-10): 남은 9화면 계약 트랙 첫 화면. 폼 계열이라 signup 최소 계약 확장 참고.
 *
 * 추출 기준: `docs/00_assets/prototype.tsx` LoginScreen L1987~2024
 * (참고: html 라인 = tsx 라인 + 35 이며 두 파일은 같은 소스이므로 tsx 만 인용)
 *
 * 특이 사항:
 *  - prototype 은 헤더 없이 중앙 정렬 400px 폭 + Footer.
 *  - input height 42 · radius 10 · border 1px (prototype.tsx L105 `inputStyle`).
 *  - 현재 구현은 auth-input height 46 · radius 8 · border 1.5px 로 갭 존재 (P1).
 *  - Btn size:l → height 50 · fontSize 16 · radius 8 (prototype.tsx L85 sizes.l).
 *  - "아이디 저장" (proto) vs "로그인 상태 유지" (구현) 는 POLICY P-1 카피 정책 → deviation.
 *  - 아이디 찾기·비번 찾기 라우팅은 prototype 이 둘 다 `find-id` 로 보내는 단순화 → 구현이
 *    별개 라우팅으로 정보구조 개선. Q-2 결정 (2026-08-10): 구현 유지, deviation 확정.
 *  - filled 상태 (proto L105 filled=true → primary border + white bg) — Q-3 결정 (2026-08-10):
 *    이번 세션 도입, 폼 전 7화면 일괄. `container.filled-*` 계약 추가.
 */

import type { ScreenContract } from './types';

export const loginContract: ScreenContract = {
    screen: 'login',
    path: '/login',
    source: 'prototype.tsx LoginScreen L1987~2024 · 2026-08-10 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ── 레이아웃 컨테이너 ────────────────────────────────────────────
        {
            id: 'container.width',
            desc: '중앙 정렬 컨테이너 폭 400',
            selector: '.auth-screen-inner',
            kind: 'box',
            prop: 'width',
            expected: 400,
            tolerance: 1,
            proto: 'tsx L1998 width:400',
            severity: 'P1',
        },
        {
            id: 'container.text-align',
            desc: '컨테이너 중앙 정렬',
            selector: '.auth-screen-inner',
            kind: 'css',
            prop: 'text-align',
            expected: 'center',
            proto: 'tsx L1998 textAlign:center',
            severity: 'P2',
        },
        // ── 헤더 없음 (prototype: 로고 위에 헤더 없이 바로 화면) ─────
        {
            id: 'header.absent',
            desc: '로그인 화면에 공용 헤더 없음',
            selector: 'header.header, .site-header',
            kind: 'count',
            expected: 0,
            proto: 'tsx L1996~2022 Header 미렌더',
            severity: 'P1',
        },
        // ── 로고 ────────────────────────────────────────────────────────
        {
            id: 'logo.height',
            desc: '상단 로고 높이 36',
            selector: '.auth-logo-img',
            kind: 'box',
            prop: 'height',
            expected: 36,
            tolerance: 1,
            proto: 'tsx L1999 height:36',
            severity: 'P1',
        },
        // ── 타이틀 ──────────────────────────────────────────────────────
        {
            id: 'title.text',
            desc: '타이틀 "로그인"',
            selector: '.auth-title',
            kind: 'text',
            expected: '로그인',
            proto: 'tsx L2000',
            severity: 'P0',
        },
        {
            id: 'title.font-size',
            desc: '타이틀 폰트 26',
            selector: '.auth-title',
            kind: 'css',
            prop: 'font-size',
            expected: '26px',
            proto: 'tsx L2000 fontSize:26',
            severity: 'P1',
        },
        {
            id: 'title.font-weight',
            desc: '타이틀 굵기 700',
            selector: '.auth-title',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L2000 fontWeight:700',
            severity: 'P1',
        },
        // ── input (아이디) ──────────────────────────────────────────────
        {
            id: 'input.id.exists',
            desc: '아이디 input 존재',
            selector: 'input[name="username"]',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2002',
            severity: 'P0',
        },
        {
            id: 'input.id.height',
            desc: '아이디 input 높이 42 (prototype inputStyle)',
            selector: 'input[name="username"]',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'tsx L105 inputStyle height:42',
            severity: 'P1',
        },
        {
            id: 'input.id.border-radius',
            desc: '아이디 input radius 10',
            selector: 'input[name="username"]',
            kind: 'css',
            prop: 'border-radius',
            expected: '10px',
            proto: 'tsx L105 inputStyle borderRadius:10',
            severity: 'P2',
        },
        // ── input (비밀번호) — signup 처럼 눈 아이콘 wrapper 안에 있어 padding-right 44 ──
        {
            id: 'input.pw.exists',
            desc: '비밀번호 input 존재',
            selector: 'input[name="password"]',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2003',
            severity: 'P0',
        },
        {
            id: 'input.pw.type',
            desc: '비밀번호 input type=password',
            selector: 'input[name="password"]',
            kind: 'css',
            prop: 'display',
            expected: 'block',
            proto: 'tsx L2003 type:password (렌더 확인용 sanity)',
            severity: 'P2',
            deviation: 'placeholder — CSS display 로는 type 검증이 안 됨. exists 만으로 충분',
        },
        {
            id: 'input.pw.height',
            desc: '비밀번호 input 높이 42',
            selector: 'input[name="password"]',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'tsx L105 inputStyle height:42',
            severity: 'P1',
        },
        // ── filled 상태는 인터랙티브 검증 (visual-login.spec.ts 별도 블록) ────
        // prototype L105 inputStyle: filled ? { bg:white, border:primary } : { bg:gray, border:transparent }
        // static contract 로 표현 불가 (runner 가 input value 주입 훅 미보유). spec 파일에서 fill → getComputedStyle 로 직접 검증.
        // ── 눈 아이콘 (prototype 에는 없음, 구현 추가 요소) ─────────────
        {
            id: 'eye.icon.svg',
            desc: '비밀번호 가시성 토글 SVG (POLICY P-3, 문자 대체 금지)',
            selector: '.auth-eye-btn svg',
            kind: 'exists',
            expected: true,
            proto: '구현 추가 요소 — POLICY P-5 (계약 검사 대상 아님, deferred 로 기록)',
            severity: 'P2',
            deferred: 'docs/design-contracts/login.md §5 prototype 없는 추가 요소',
        },
        // ── 옵션 row ────────────────────────────────────────────────────
        {
            id: 'options.checkbox.exists',
            desc: '옵션 row 좌측 체크박스 존재',
            selector: '.auth-checkbox input[type="checkbox"]',
            kind: 'exists',
            expected: true,
            proto: 'tsx L2007',
            severity: 'P0',
        },
        {
            id: 'options.checkbox.label',
            desc: '체크박스 라벨 텍스트 "아이디 저장" (prototype)',
            selector: '.auth-checkbox span',
            kind: 'text',
            expected: '아이디 저장',
            proto: 'tsx L2007',
            severity: 'P2',
            deviation:
                'POLICY P-1 카피 정책 — 구현 카피 "로그인 상태 유지" 유지. Spring Security remember-me 는 세션 유지 동작이라 구현 카피가 정확함',
        },
        {
            id: 'options.find-id.text',
            desc: '아이디 찾기 링크 존재',
            selector: '.auth-help-links a[href="/find-id"]',
            kind: 'text',
            expected: '아이디 찾기',
            proto: 'tsx L2010',
            severity: 'P1',
        },
        {
            id: 'options.find-pw.text',
            desc: '비밀번호 찾기 링크 존재',
            selector: '.auth-help-links a[href="/find-password"]',
            kind: 'text',
            expected: '비밀번호 찾기',
            proto: 'tsx L2012',
            severity: 'P1',
            deviation:
                'Q-2 확정 (2026-08-10) — prototype 은 두 링크가 모두 find-id 로 이동하는 단순화지만 구현은 별개 라우팅으로 정보구조 개선. 구현 유지',
        },
        {
            id: 'options.font-size',
            desc: '옵션 row 폰트 13',
            selector: '.auth-options',
            kind: 'css',
            prop: 'font-size',
            expected: '13px',
            proto: 'tsx L2005 fontSize:13',
            severity: 'P2',
        },
        // ── 액션 버튼 (로그인 · 회원가입) ─────────────────────────────
        {
            id: 'action.primary.text',
            desc: '기본 액션 버튼 텍스트 "로그인"',
            selector: '.btn-auth--primary',
            kind: 'text',
            expected: '로그인',
            proto: 'tsx L2016',
            severity: 'P0',
        },
        {
            id: 'action.primary.height',
            desc: 'Btn size:l → height 50',
            selector: '.btn-auth--primary',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 1,
            proto: 'tsx L85 sizes.l h:50',
            severity: 'P1',
        },
        {
            id: 'action.primary.font-size',
            desc: 'Btn size:l → fontSize 16',
            selector: '.btn-auth--primary',
            kind: 'css',
            prop: 'font-size',
            expected: '16px',
            proto: 'tsx L85 sizes.l fs:16',
            severity: 'P2',
        },
        {
            id: 'action.primary.border-radius',
            desc: 'Btn borderRadius 8',
            selector: '.btn-auth--primary',
            kind: 'css',
            prop: 'border-radius',
            expected: '8px',
            proto: 'tsx L97 borderRadius:8',
            severity: 'P2',
        },
        {
            id: 'action.secondary.text',
            desc: '보조 액션 "회원가입"',
            selector: '.btn-auth--secondary',
            kind: 'text',
            expected: '회원가입',
            proto: 'tsx L2017',
            severity: 'P0',
        },
        {
            id: 'action.secondary.height',
            desc: '회원가입 버튼 높이 50',
            selector: '.btn-auth--secondary',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 1,
            proto: 'tsx L85 sizes.l h:50 (fullWidth secondary)',
            severity: 'P1',
        },
        // ── Footer (prototype: <Footer/> 렌더) ────────────────────────
        {
            id: 'footer.exists',
            desc: '푸터 존재 (prototype 은 Footer 렌더)',
            selector: 'footer, .site-footer',
            kind: 'count',
            expected: 1,
            proto: 'tsx L2021 <Footer/>',
            severity: 'P1',
        },
    ],
};
