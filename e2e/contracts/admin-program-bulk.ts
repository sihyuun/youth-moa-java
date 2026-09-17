/**
 * A8 admin-bulk-csv (2026-09-17) — 관리자 프로그램 bulk action 디자인 계약.
 *
 * 결정 요약:
 *  - Qn-P1: 프로그램 bulk publish/unpublish 미도입 (prototype.tsx L507 명시 금지 · Program.getStatus() 는 런타임 파생).
 *    Bulk deactivate (soft) / reactivate 로 대체.
 *  - CENTER_ADMIN 도 자기 센터 프로그램만 조작 가능 (AdminScope organization 매칭).
 */

import type { ScreenContract } from './types';

export const adminProgramBulkContract: ScreenContract = {
    screen: 'admin-program-bulk',
    path: '/admin/programs',
    source: 'admin/prototype.html L907~945 · L2772 · docs/specs/A8-admin-bulk-csv.md §4-3',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'bulk.head.checkbox',
            desc: '헤더 전체선택 checkbox',
            selector: '.admin-program-row--head .admin-bulk-checkbox[data-bulk-select-all]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.row.checkbox',
            desc: '행별 checkbox 최소 1건',
            selector: '.admin-program-row:not(.admin-program-row--head) input[data-bulk-checkbox]',
            kind: 'count-min',
            expected: 1,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.exists',
            desc: '하단 다크 액션바',
            selector: '.admin-bulk-action-bar[data-bulk-domain="programs"]',
            kind: 'exists',
            expected: true,
            proto: 'prototype L2772',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.deactivate',
            desc: '"운영 중단" 버튼 (soft delete)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="programs"] [data-bulk-action="deactivate"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-1 · Q10 소프트 삭제',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.reactivate',
            desc: '"재활성화" 버튼',
            selector: '.admin-bulk-action-bar[data-bulk-domain="programs"] [data-bulk-action="reactivate"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-1',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.csv',
            desc: '"선택 건 CSV" 링크',
            selector: '.admin-bulk-action-bar[data-bulk-domain="programs"] [data-bulk-action="csv-selected"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 Qn-CSV1',
            severity: 'P0',
        },
        // Qn-P1: publish/unpublish bulk 는 도입하지 않는다 (deviation)
        {
            id: 'bulk.bar.action.publish',
            desc: '"일괄 게시" 버튼 (미도입)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="programs"] [data-bulk-action="publish"]',
            kind: 'exists',
            expected: false,
            proto: 'prototype.tsx L507 명시 금지 — Program.getStatus() 는 런타임 파생',
            severity: 'P0',
            deviation: 'A8 Qn-P1: 프로그램 status 는 신청기간·정원 파생 · UPDATE 불가',
        },
    ],
};
