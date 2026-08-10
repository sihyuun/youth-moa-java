/**
 * 회원가입 화면 (`/signup`) 디자인 계약 — MVP 시연용.
 *
 * 도입 목적 (2026-07-31): input 폭·높이 통일 여부를 계약으로 강제. 지금까지 시각 검사가 커버하지 않던 폼 필드 크기 정합성.
 * 2026-08-10 확장 (U1): filled/readonly 상태 커버리지 추가. login-contract-fix ym-verify UNVERIFIED U1 대응.
 *   - 규격 정합(radius·border) 정적 검증
 *   - 기본(empty) 상태 흰 배경 정합 (레퍼런스 패턴 A)
 *   - readonly 상태 회색 배경 정합
 *   - filled 상태는 login 처럼 별도 spec 블록 (visual-signup.spec.ts) 에서 인터랙티브 검증
 */

import type { ScreenContract } from './types';

export const signupContract: ScreenContract = {
    screen: 'signup',
    path: '/signup',
    source: 'prototype.tsx SignupScreen L1414~1978 · 2026-07-31 최초 신설 · 2026-08-10 확장',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ── form input 크기 통일 (모든 form-input 은 42px 높이 표준) ─────
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
        // ── U1 확장: form-input 규격 정합 (radius/border) ─────
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
        // ── U1 확장: 패턴 A — empty 상태 흰 배경 정합 ─────
        // (email input 은 초기 로드 시 값 없음 → base .form-input 배경이 그대로 노출됨)
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
        // ── U1 확장: readonly 상태 회색 배경 정합 ─────
        // zipcode 는 .form-input--readonly 클래스 → #F3F4F6 (진한 회색). :read-only 폴백 규칙(#F7F8FA) 과 구분.
        {
            id: 'form.input.readonly-bg',
            desc: 'readonly (우편번호) 배경 #F3F4F6 (form-input--readonly 클래스)',
            selector: '#zipcode',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(243, 244, 246)',
            proto: 'main.css .form-input--readonly background:#F3F4F6',
            severity: 'P2',
        },
        // ── 약관 컴포넌트 — data-term-code 안정 셀렉터 존재 확인 (회귀 방어) ──
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
    ],
};
