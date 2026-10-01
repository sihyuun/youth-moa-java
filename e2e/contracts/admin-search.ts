/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 디자인 계약.
 *
 * 로그인 후 /admin 상태에서 검사한다 (admin-shell 과 동일 base).
 * 드롭다운 열림 상태의 치수·아이콘 색을 검증하려면 시나리오 E2E(별도 spec) 가 필요하므로
 * 본 계약은 **헤더 입력박스 자체의 토큰** 과 **드롭다운 placeholder** 만 다룬다.
 */

import type { ScreenContract } from './types';

export const adminSearchContract: ScreenContract = {
    screen: 'admin-search',
    path: '/admin',
    source: 'A8-search docs/design-contracts/admin/search.md',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'search.input.width',
            desc: '검색 입력박스 폭 180',
            selector: '.admin-header-search',
            kind: 'box',
            prop: 'width',
            expected: 180,
            tolerance: 1,
            proto: 'admin.css .admin-header-search width:180',
            severity: 'P2',
        },
        {
            id: 'search.input.height',
            desc: '검색 입력박스 높이 32',
            selector: '.admin-header-search',
            kind: 'box',
            prop: 'height',
            expected: 32,
            tolerance: 1,
            proto: 'admin.css .admin-header-search height:32',
            severity: 'P2',
        },
        {
            id: 'search.input.background',
            desc: '검색 입력박스 배경 #1E293B',
            selector: '.admin-header-search',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(30, 41, 59)',
            proto: 'admin.css .admin-header-search background:#1E293B',
            severity: 'P2',
        },
        {
            id: 'search.input.border-radius',
            desc: '검색 입력박스 pill (border-radius 20)',
            selector: '.admin-header-search',
            kind: 'css',
            prop: 'border-radius',
            expected: '20px',
            proto: 'admin.css .admin-header-search border-radius:20',
            severity: 'P2',
        },
        {
            id: 'search.dropdown.exists-closed',
            desc: '드롭다운 컨테이너 존재 (초기 닫힘 — is-open 없음)',
            selector: '#admin-search-dropdown:not(.is-open)',
            kind: 'exists',
            expected: true,
            proto: 'A8-search 2026-10-01 초기 상태',
            severity: 'P1',
        },
        {
            id: 'search.clear.exists',
            desc: '검색 지우기 버튼 존재',
            selector: '.admin-header-search-clear',
            kind: 'exists',
            expected: true,
            proto: 'A8-search 2026-10-01',
            severity: 'P2',
        },
    ],
};
