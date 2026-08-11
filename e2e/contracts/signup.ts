/**
 * 회원가입 화면 (`/signup`) 디자인 계약 — MVP 시연용.
 *
 * 도입 목적 (2026-07-31): input 폭·높이 통일 여부를 계약으로 강제. 지금까지 시각 검사가 커버하지 않던 폼 필드 크기 정합성.
 * 2026-08-10 확장 (U1): filled/readonly 상태 커버리지 추가. login-contract-fix ym-verify UNVERIFIED U1 대응.
 *   - 규격 정합(radius·border) 정적 검증
 *   - 기본(empty) 상태 흰 배경 정합 (레퍼런스 패턴 A)
 *   - readonly 상태 회색 배경 정합
 *   - filled 상태는 login 처럼 별도 spec 블록 (visual-signup.spec.ts) 에서 인터랙티브 검증
 * 2026-08-10 확장 (U2 · signup.md 신설 동반): 서술 계약 매핑. 컨테이너 폭, 성별 pill 규격, 2컬럼 gap,
 *   btn-outline-sm primary 색, 약관 강조 박스, 회원가입 submit height, 로그인 링크 존재 — 정량 검증 가능 항목 편입.
 */

import type { ScreenContract } from './types';

export const signupContract: ScreenContract = {
    screen: 'signup',
    path: '/signup',
    source: 'prototype.tsx SignupScreen L1842~1983 · 2026-07-31 신설 · 2026-08-10 U1·U2 확장',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 컨테이너 · 레이아웃
        // ══════════════════════════════════════════════════════
        {
            id: 'inner.width',
            desc: 'signup-inner 폭 560 (prototype L1867)',
            selector: '.signup-inner',
            kind: 'box',
            prop: 'width',
            expected: 560,
            tolerance: 1,
            proto: 'tsx L1867 width:560',
            severity: 'P1',
        },
        {
            id: 'field-row-2col.gap',
            desc: '성별·생년월일 2컬럼 gap 14 (prototype L1924)',
            selector: '.signup-field-row-2col',
            kind: 'css',
            prop: 'gap',
            expected: '14px',
            proto: 'tsx L1924 flex gap:14',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // form input 크기 통일 (기존)
        // ══════════════════════════════════════════════════════
        {
            id: 'form.input.height',
            desc: '회원가입 form-input 높이 42 (통일)',
            selector: '#email',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'main.css .form-input height:42px (전 화면 통일)',
            severity: 'P1',
        },
        {
            id: 'form.input.password.height',
            desc: '회원가입 비밀번호 input 도 42 (form-input--with-eye)',
            selector: '#password',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'main.css .form-input height:42px',
            severity: 'P1',
        },
        {
            id: 'form.input.name.height',
            desc: '회원가입 이름 input 42 통일',
            selector: '#name',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'main.css .form-input height:42px',
            severity: 'P2',
        },
        // ── U1: form-input 규격 정합 (radius/border) ─────
        {
            id: 'form.input.radius',
            desc: 'form-input radius 10 (prototype L105 inputStyle)',
            selector: '#email',
            kind: 'css',
            prop: 'border-radius',
            expected: '10px',
            proto: 'main.css .form-input border-radius:10px',
            severity: 'P2',
        },
        {
            id: 'form.input.border-width',
            desc: 'form-input border 1px',
            selector: '#email',
            kind: 'css',
            prop: 'border-top-width',
            expected: '1px',
            proto: 'main.css .form-input border:1px',
            severity: 'P2',
        },
        // ── U1: 패턴 A — empty 상태 흰 배경 정합 ─────
        {
            id: 'form.input.empty-bg',
            desc: 'empty state 배경 흰색 (레퍼런스 패턴 A, 2026-08-10)',
            selector: '#email',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(255, 255, 255)',
            proto: 'main.css .form-input background:var(--color-surface)',
            severity: 'P1',
        },
        // ── U1: readonly 상태 회색 배경 정합 ─────
        {
            id: 'form.input.readonly-bg',
            desc: 'readonly (우편번호) 배경 #F3F4F6 (form-input--readonly 클래스)',
            selector: '#zipcode',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(243, 244, 246)',
            proto: 'main.css .form-input--readonly background:#F3F4F6',
            severity: 'P2',
            deviation: 'signup.md §7 — phone.readonly-bg 규칙과 동일. verify.done phone lock 정책 통일',
        },

        // ══════════════════════════════════════════════════════
        // U2: 생년월일 native picker (deviation 명시)
        // ══════════════════════════════════════════════════════
        {
            id: 'birthdate.native',
            desc: '생년월일 native date picker 사용 (prototype 은 type=text) — 접근성·모바일 UX 우선',
            selector: 'input#birthDateText[type="date"]',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1938 type=text placeholder="YYYY / MM / DD"',
            severity: 'P2',
            deviation: 'signup.md Q-2 — native date picker 채택. 접근성·모바일 UX·유효성 자동검증 이점 (2026-08-10 확정)',
        },

        // ══════════════════════════════════════════════════════
        // U2: 성별 pill 규격
        // ══════════════════════════════════════════════════════
        {
            id: 'gender.pill.height',
            desc: '성별 pill 높이 42 (prototype L1929)',
            selector: '.signup-gender-pill[data-value="MALE"]',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'tsx L1929 height:42',
            severity: 'P2',
        },
        {
            id: 'gender.pill.radius',
            desc: '성별 pill radius 10 (prototype L1929)',
            selector: '.signup-gender-pill[data-value="MALE"]',
            kind: 'css',
            prop: 'border-radius',
            expected: '10px',
            proto: 'tsx L1929 borderRadius:10',
            severity: 'P2',
        },
        {
            id: 'gender.pill.empty-bg',
            desc: '성별 pill 미선택 배경 흰색 (레퍼런스 패턴 A 확장)',
            selector: '.signup-gender-pill[data-value="MALE"]',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(255, 255, 255)',
            proto: 'tsx L1929 background:#F7F8FA',
            severity: 'P2',
            deviation: 'signup.md Q-1 — input filled 정책과 통일. 편집 가능 상태 = 흰색, 회색 = 편집불가 단일 시그널 (login.md Q-3 동일 논리, 2026-08-10 확정)',
        },
        {
            id: 'gender.pill.count',
            desc: '성별 pill 2개 (남·여)',
            selector: '.signup-gender-pill',
            kind: 'count',
            expected: 2,
            proto: 'tsx L1928 [\'남\',\'여\'].map',
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // U2: btn-outline-sm primary 색 정합 (중복확인·인증요청·검색)
        // ══════════════════════════════════════════════════════
        {
            id: 'side-btn.color',
            desc: '중복확인 버튼 primary 색 (prototype L90 variant=outline fg=primary)',
            selector: '#emailCheckBtn',
            kind: 'css',
            prop: 'color',
            expected: 'rgb(63, 48, 233)',
            proto: 'tsx L90 outline fg:T.primary',
            severity: 'P2',
        },
        {
            id: 'side-btn.height',
            desc: '사이드 버튼 height 42 (prototype L1856 btnSide)',
            selector: '#emailCheckBtn',
            kind: 'box',
            prop: 'height',
            expected: 42,
            tolerance: 1,
            proto: 'tsx L1856 height:42',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // U2: 약관 UI 규격
        // ══════════════════════════════════════════════════════
        {
            id: 'agree-all.radius',
            desc: '약관 전체 동의 강조 박스 radius 10 (prototype L1958)',
            selector: '.signup-agree-all',
            kind: 'css',
            prop: 'border-radius',
            expected: '10px',
            proto: 'tsx L1958 borderRadius:10',
            severity: 'P2',
        },
        {
            id: 'agree-all.padding',
            desc: '약관 전체 동의 padding 12/14 (prototype L1958)',
            selector: '.signup-agree-all',
            kind: 'css',
            prop: 'padding-top',
            expected: '12px',
            proto: 'tsx L1958 padding:12px 14px',
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 약관 체크박스 존재 (기존)
        // ══════════════════════════════════════════════════════
        {
            id: 'terms.service.exists',
            desc: 'SERVICE 약관 체크박스 존재 (F-signup-terms-agreement 회귀 방어)',
            selector: 'input[data-term-code="SERVICE"]',
            kind: 'exists',
            expected: true,
            proto: 'F-signup-terms-agreement PR #125',
            severity: 'P0',
        },
        {
            id: 'terms.privacy.exists',
            desc: 'PRIVACY 약관 체크박스 존재',
            selector: 'input[data-term-code="PRIVACY"]',
            kind: 'exists',
            expected: true,
            proto: 'F-signup-terms-agreement PR #125',
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // U2 (Q-3 대기): P-3 SVG 아이콘 규칙 — 약관 체크
        //   현재 구현: content:'✓' 문자 (main.css L2736, L2778) → P-3 위반
        //   결정 대기 (signup.md §8 Q-3):
        //     (A) 이번 impl 범위 포함 → 이 check 를 활성화
        //     (B) 별도 티켓 deferred
        //   지금은 (B) 로 기록 — Q-3 확정 시 deferred 제거 or (A) 로 전환
        // ══════════════════════════════════════════════════════
        {
            id: 'terms.check.svg',
            desc: '약관 체크박스 아이콘은 SVG (POLICY P-3, prototype L1959·L1967)',
            selector: '.signup-agree-all svg, .signup-agree-item svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1959·L1967 svg path stroke check',
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // U2: submit 버튼 · 로그인 링크
        // ══════════════════════════════════════════════════════
        {
            id: 'submit.height',
            desc: '회원가입 submit 버튼 fullWidth · height 50 (prototype L1975 size=l)',
            selector: '.signup-submit-btn',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 2,
            proto: 'tsx L85 size:l h:50 · L1975 <Btn size="l" fullWidth>',
            severity: 'P1',
        },
        {
            id: 'login-link.exists',
            desc: '"이미 계정이 있으신가요? 로그인" 링크 (prototype L1976~1978)',
            selector: '.signup-login-link a[href="/login"]',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1977 onClick=>go(\'login\')',
            severity: 'P1',
        },
    ],
};
