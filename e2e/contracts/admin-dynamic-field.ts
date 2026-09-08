/**
 * 관리자 동적 신청 필드 관리 (`/admin/programs/{programId}/dynamic-fields`) 디자인 계약.
 *
 * F0c-dynamic-fields (2026-09-08 · Qn-1~10 A · Qn-Δ A · Qn-11 B · Qn-8 C) 산출.
 * admin prototype 부재 → POLICY + admin-notice/term 계약 승계.
 */

import type { ScreenContract } from './types';

// V11 마이그레이션이 seed program #7 에 3필드 백필 → 목록은 정확히 3건.
const SEED_PROGRAM_ID = 7;

export const adminDynamicFieldListContract: ScreenContract = {
    screen: 'admin-dynamic-field-list',
    path: `/admin/programs/${SEED_PROGRAM_ID}/dynamic-fields`,
    source: 'admin POLICY + admin-notice/term 계약 승계 · 2026-09-08 F0c-dynamic-fields',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'page.title.exists',
            desc: '페이지 타이틀 "동적 신청 필드 관리"',
            selector: '.admin-dynamic-field-title',
            kind: 'text',
            expected: '동적 신청 필드 관리',
            proto: 'admin/program-dynamic-field/list.html',
            severity: 'P0',
        },
        {
            id: 'page.subtitle.exists',
            desc: '서브 카피 존재 (프로그램명 포함)',
            selector: '.admin-dynamic-field-sub',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'create.button.exists',
            desc: '"+ 신규 등록" 버튼',
            selector: '.admin-dynamic-field-header a.admin-btn.admin-btn--primary',
            kind: 'text',
            expected: '+ 신규 등록',
            severity: 'P0',
        },
        {
            id: 'list.head.row',
            desc: '테이블 헤더 존재',
            selector: '.admin-dynamic-field-row.admin-dynamic-field-row--head',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'list.head.col.type',
            desc: '헤더 "타입" 컬럼',
            selector: '.admin-dynamic-field-row--head .admin-dynamic-field-col-type',
            kind: 'text',
            expected: '타입',
            severity: 'P1',
        },
        {
            id: 'list.head.col.label',
            desc: '헤더 "라벨" 컬럼',
            selector: '.admin-dynamic-field-row--head .admin-dynamic-field-col-label',
            kind: 'text',
            expected: '라벨',
            severity: 'P1',
        },
        {
            id: 'list.head.col.required',
            desc: '헤더 "필수" 컬럼',
            selector: '.admin-dynamic-field-row--head .admin-dynamic-field-col-required',
            kind: 'text',
            expected: '필수',
            severity: 'P1',
        },
        {
            id: 'list.head.col.status',
            desc: '헤더 "상태" 컬럼',
            selector: '.admin-dynamic-field-row--head .admin-dynamic-field-col-status',
            kind: 'text',
            expected: '상태',
            severity: 'P1',
        },
        {
            id: 'list.rows.seeded',
            desc: 'seed program #7 에 3필드 (TEXT · DROPDOWN · ATTACHMENT)',
            selector: '.admin-dynamic-field-row:not(.admin-dynamic-field-row--head)',
            kind: 'count-min',
            expected: 3,
            severity: 'P0',
        },
        {
            id: 'list.type-pill.exists',
            desc: '타입 pill 3종 이상 렌더',
            selector: '.admin-dynamic-field-type-pill',
            kind: 'count-min',
            expected: 3,
            severity: 'P1',
        },
    ],
};

export const adminDynamicFieldFormContract: ScreenContract = {
    screen: 'admin-dynamic-field-form',
    path: `/admin/programs/${SEED_PROGRAM_ID}/dynamic-fields/new`,
    source: 'admin POLICY + admin-notice-form 승계 · 2026-09-08 F0c-dynamic-fields',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'page.title',
            desc: '페이지 타이틀 "동적 필드 등록"',
            selector: '.admin-dynamic-field-title',
            kind: 'text',
            expected: '동적 필드 등록',
            severity: 'P0',
        },
        {
            id: 'field.fieldType.exists',
            desc: '필드 타입 select 존재',
            selector: 'select#fieldType[name="fieldType"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.fieldType.options',
            desc: 'TEXT / DROPDOWN / ATTACHMENT 3옵션',
            selector: 'select#fieldType option',
            kind: 'count',
            expected: 3,
            severity: 'P0',
        },
        {
            id: 'field.label.exists',
            desc: '라벨 입력 필드',
            selector: 'input#label[name="label"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.sortOrder.exists',
            desc: '정렬 순서 필드',
            selector: 'input#sortOrder[name="sortOrder"]',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'field.isRequired.exists',
            desc: '필수 응답 체크박스',
            selector: 'input#isRequired[name="isRequired"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.maxLength.exists',
            desc: 'TEXT 전용 최대 글자수 필드',
            selector: 'input#maxLength[name="maxLength"]',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'field.options.exists',
            desc: 'DROPDOWN 전용 options textarea',
            selector: 'textarea#options[name="options"]',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'submit.button.exists',
            desc: '"등록" 버튼',
            selector: '.admin-dynamic-field-form-actions button[type="submit"]',
            kind: 'text',
            expected: '등록',
            severity: 'P0',
        },
    ],
};

export const adminDynamicFieldEditContract: ScreenContract = {
    screen: 'admin-dynamic-field-edit',
    // seed program #7 · V11 마이그레이션이 만든 첫 번째 필드 id=1 (TEXT · 지원 동기)
    path: `/admin/programs/${SEED_PROGRAM_ID}/dynamic-fields/1`,
    source: '2026-09-08 F0c-dynamic-fields · Qn-8 C soft delete',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'page.title',
            desc: '페이지 타이틀 "동적 필드 편집"',
            selector: '.admin-dynamic-field-title',
            kind: 'text',
            expected: '동적 필드 편집',
            severity: 'P0',
        },
        {
            id: 'field.label.prefilled',
            desc: 'label 필드 시드값 prefilled',
            selector: 'input#label',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'submit.button.update',
            desc: '"수정 저장" 버튼',
            selector: '.admin-dynamic-field-form-actions button[type="submit"]',
            kind: 'text',
            expected: '수정 저장',
            severity: 'P0',
        },
        {
            id: 'deactivate.button.exists',
            desc: 'Qn-8 C: 활성 필드에는 "비활성 처리" 버튼 (hard delete 금지)',
            selector: '.admin-dynamic-field-form-actions .admin-btn--danger-outline',
            kind: 'text',
            expected: '비활성 처리',
            severity: 'P0',
        },
        {
            id: 'deactivate.modal.exists',
            desc: '비활성 confirm 모달 마크업',
            selector: '#dynamic-field-deactivate-modal.admin-confirm-modal',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
    ],
};
