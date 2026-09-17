/**
 * A8 admin-bulk-csv (2026-09-17) — 관리자 신청 bulk action 디자인 계약.
 *
 * 결정 요약:
 *  - Qn-App1 A안: **Approve bulk 만 편입** (deviation) — 반려 사유 개별성·감사 정합 위해 Reject 는 A4 개별 처리 유지.
 *    후속 티켓 `A8-reject-bulk` 로 이월.
 *  - Qn-9: 성공 건마다 ApplicationApprovedEvent 발송 (사용자 알림 자동 · A4 정합)
 */

import type { ScreenContract } from './types';

export const adminApplicationBulkContract: ScreenContract = {
    screen: 'admin-application-bulk',
    path: '/admin/programs/1/applications',
    source: 'admin/prototype.html L1397~1412 · docs/specs/A8-admin-bulk-csv.md §후속',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'bulk.head.checkbox',
            desc: '헤더 전체선택 checkbox',
            selector: '.admin-application-row--head .admin-bulk-checkbox[data-bulk-select-all]',
            kind: 'exists',
            expected: true,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.row.checkbox',
            desc: '행별 checkbox 최소 1건',
            selector: '.admin-application-row:not(.admin-application-row--head) input[data-bulk-checkbox]',
            kind: 'count-min',
            expected: 1,
            proto: 'A8 spec §4-3',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.exists',
            desc: '하단 다크 액션바',
            selector: '.admin-bulk-action-bar[data-bulk-domain="applications"]',
            kind: 'exists',
            expected: true,
            proto: 'prototype L2772 · A8 QA',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.approve',
            desc: '"일괄 승인" 버튼 (deviation: prototype 미명시 · 사용자 요구)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="applications"] [data-bulk-action="approve"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 Qn-App1 A · deviation',
            severity: 'P0',
        },
        {
            id: 'bulk.bar.action.reject.absent',
            desc: '"일괄 반려" 버튼 미노출 (A8-reject-bulk 로 이월)',
            selector: '.admin-bulk-action-bar[data-bulk-domain="applications"] [data-bulk-action="reject"]',
            kind: 'exists',
            expected: false,
            proto: 'A8 Qn-App1 A · Reject 는 A4 개별 처리 유지',
            severity: 'P0',
            deferred: 'A8-reject-bulk (반려 사유 개별성)',
        },
        {
            id: 'bulk.bar.action.csv',
            desc: '"선택 건 CSV" 링크',
            selector: '.admin-bulk-action-bar[data-bulk-domain="applications"] [data-bulk-action="csv-selected"]',
            kind: 'exists',
            expected: true,
            proto: 'A8 Qn-CSV1',
            severity: 'P0',
        },
    ],
};
