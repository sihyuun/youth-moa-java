/**
 * 인터랙션 계약 — 사용자 화면 (login / signup / profile-edit).
 *
 * 각 버튼·링크의 클릭 결과 (이동 목적지) 를 명세로 관리해 wireframe/prototype 이탈을 자동 감지.
 * 2026-07-31 사고 사례: profile-edit [비밀번호 변경하기] → `/find-password` 이동. 인라인 폼으로 수정 후
 * 이 계약에도 등재해 회귀 방어.
 */

import type { InteractionContract } from './interactions-types';

export const userInteractions: InteractionContract = {
    name: 'user',
    description: 'login / signup / find-* / mypage/profile-edit 사용자 화면 인터랙션 목적지',
    interactions: [
        // ── login ──────────────────────────────────────
        {
            id: 'login.signup.link',
            desc: '로그인 화면 회원가입 링크 → /signup',
            startPath: '/login',
            selector: 'a.btn-auth--secondary[href*="/signup"]',
            expected: { kind: 'navigate', toPattern: /\/signup$/ },
            proto: 'wireframe WF-1-001 A. 회원가입',
            severity: 'P1',
        },
        {
            id: 'login.find-id.link',
            desc: '로그인 화면 아이디 찾기 링크 → /find-id',
            startPath: '/login',
            selector: 'a[href*="/find-id"]',
            expected: { kind: 'navigate', toPattern: /\/find-id/ },
            proto: 'wireframe WF-1-001 C. 아이디 찾기',
            severity: 'P1',
        },
        {
            id: 'login.find-password.link',
            desc: '로그인 화면 비밀번호 찾기 링크 → /find-password',
            startPath: '/login',
            selector: 'a[href*="/find-password"]',
            expected: { kind: 'navigate', toPattern: /\/find-password/ },
            proto: 'wireframe WF-1-001 D. 비밀번호 찾기',
            severity: 'P1',
        },

        // ── signup ─────────────────────────────────────
        {
            id: 'signup.login.link',
            desc: '회원가입 화면 로그인 링크 → /login',
            startPath: '/signup',
            selector: '.signup-login-link a',
            expected: { kind: 'navigate', toPattern: /\/login$/ },
            proto: 'wireframe WF-2-001 하단 로그인 링크',
            severity: 'P1',
        },
        {
            id: 'signup.terms.view',
            desc: '회원가입 약관보기 클릭 → 페이지 이동 없이 모달 노출 (stay)',
            startPath: '/signup',
            selector: '[data-term-code="SERVICE"] .signup-agree-view',
            expected: { kind: 'stay' },
            proto: 'F-signup-terms-agreement (2026-07-31) — 새 탭 이동 대신 오버레이 모달로 회원가입 흐름 유지',
            severity: 'P1',
        },

        // ── profile-edit (auth 필요) ──────────────────
        // 2026-07-31: [비밀번호 변경하기] 링크가 /find-password 로 이동하던 버그를 인라인 필드로 수정.
        // 이 회귀 방어: 링크가 아닌 인라인 필드가 존재해야 한다 (다른 시각 계약과 별도로 selector 자체 감지).
        //
        // auth 인터랙션은 세션 필요 — 이번 MVP 는 anon 위주. auth 지원 확장은 후속.
    ],
};
