/**
 * A8 admin-bulk-csv (2026-09-17) — 관리자 사용자 bulk action 디자인 계약.
 *
 * 결정 요약 (docs/specs/A8-admin-bulk-csv.md):
 *  - QA: floating bottom action bar
 *  - QB: 페이지 이동 시 selection state 초기화
 *  - Qn-1: per-row 트랜잭션 (POLICY.md P-BULK-1)
 *  - Qn-8: AdminUserSafeguard 컴포넌트 재사용 (개별+bulk)
 *
 * 화면 UI 검증 (checkbox 컬럼 · action bar · CSV 링크 · 확인 모달). endpoint 계약(302 redirect · flash)
 * 은 별도 기능 E2E (`tests/admin-bulk-users.spec.ts`) 에서 다룬다.
 */

import type { ScreenContract } from './types';

export const adminUserBulkContract: ScreenContract = {
    screen: 'admin-user-bulk',
    path: '/admin/users',
    source: 'admin/prototype.html L1201~1210 · L2772 · docs/specs/A8-admin-bulk-csv.md §4-3',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'bulk.head.checkbox',
            desc: '헤더 전체선택 checkbox',
            selector: '.admin-user-row--head .admin-bulk-checkbox[data-bulk-select-all]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.row.checkbox',
            desc: '행별 checkbox 최소 1건',
            selector: '.admin-user-row:not(.admin-user-row--head) input[data-bulk-checkbox]',
            kind: 'count-min',
            expected: 1,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.exists',
            desc: '하단 다크 액션바 (초기 hidden)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"]',
            kind: 'exists',
            expected: true,
            proto: 'prototype L2772 · POLICY QA',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.count.exists',
            desc: '선택 개수 표시 요소',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-count]',
            kind: 'exists',
            expected: true,
            proto: 'spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.deactivate',
            desc: '"차단" 버튼',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-action="deactivate"]',
            kind: 'exists',
            expected: true,
            proto: 'A5 §9 + A8 spec §4-1',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.reactivate',
            desc: '"재활성화" 버튼',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-action="reactivate"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-1',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.role',
            desc: '"권한 변경" 버튼 (SYSTEM_ADMIN 전용)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-action="role"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-1 · Qn-8 safeguard 재적용',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.csv',
            desc: '"선택 건 CSV" 링크',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-action="csv-selected"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 Qn-CSV1 하단 = 선택 건',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.clear',
            desc: '"선택 해제" 버튼',
            selector: '.admin-bulk-action-bar[data-bulk-domain="users"] [data-bulk-clear]',
            kind: 'exists',
            expected: true,
            proto: 'prototype L4108 clearSelection',
            severity: 'P1',
        },
        {
            id: 'bulk.modal.exists',
            desc: '확인/입력 모달 (초기 hidden)',
            selector: '.admin-bulk-modal-backdrop[data-bulk-modal]',
            kind: 'exists',
            expected: true,
            proto: 'spec §4-3 _bulk-confirm-dialog',
            severity: 'P1',
        },
        {
            id: 'bulk.form.csrf',
            desc: 'hidden form 에 CSRF token 포함',
            selector: 'form[data-bulk-form] input[name="_csrf"]',
            kind: 'exists',
            expected: true,
            proto: 'R5 회귀 방지',
            severity: 'P0',
        },
    ],
};
