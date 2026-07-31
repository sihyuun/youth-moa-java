/**
 * 회원가입 화면 (`/signup`) 디자인 계약 — MVP 시연용.
 *
 * 도입 목적 (2026-07-31): input 폭·높이 통일 여부를 계약으로 강제. 지금까지 시각 검사가 커버하지 않던 폼 필드 크기 정합성.
 *
 * 향후: 시각 갭 발견 시 항목 확장 (placeholder 색 · 라벨 크기 · 헬프 텍스트 폰트 등).
 */

import type { ScreenContract } from './types';

export const signupContract: ScreenContract = {
    screen: 'signup',
    path: '/signup',
    source: 'prototype.tsx SignupScreen L1414~1978 · 2026-07-31 최초 신설 (input 폭 통일 검증)',
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
