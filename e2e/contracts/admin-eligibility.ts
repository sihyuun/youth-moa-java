/**
 * 관리자 프로그램 자격요건 편집 (`/admin/programs/{programId}/eligibility`) 디자인 계약.
 *
 * F4-admin-eligibility (2026-09-09 · Qn-1~8 모두 A) 산출.
 * admin prototype 부재 → POLICY + admin-notice/term/dynamic-field 계약 승계.
 */

import type { ScreenContract } from './types';

// seed program #7 = F0c-dynamic-fields 와 동일하게 편집 진입 대상.
const SEED_PROGRAM_ID = 7;

export const adminEligibilityFormContract: ScreenContract = {
    screen: 'admin-eligibility-form',
    path: `/admin/programs/${SEED_PROGRAM_ID}/eligibility`,
    source: 'admin POLICY + admin-notice/term/dynamic-field 계약 승계 · 2026-09-09 F4-admin-eligibility',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'page.title.exists',
            desc: '페이지 타이틀 "자격요건 편집"',
            selector: '.admin-eligibility-title',
            kind: 'text',
            expected: '자격요건 편집',
            proto: 'admin/program-eligibility/form.html',
            severity: 'P0',
        },
        {
            id: 'page.subtitle.exists',
            desc: '서브 카피 존재 (프로그램명 + 3필드 안내)',
            selector: '.admin-eligibility-sub',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'field.age.exists',
            desc: '연령 입력 필드 존재 (maxlength=100)',
            selector: 'input#age[name="age"][maxlength="100"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.region.exists',
            desc: '거주지 입력 필드 존재 (maxlength=100)',
            selector: 'input#region[name="region"][maxlength="100"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.etc.exists',
            desc: '기타 조건 textarea 존재 (maxlength=200)',
            selector: 'textarea#etc[name="etc"][maxlength="200"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'field.age.label',
            desc: '연령 라벨 "연령"',
            selector: 'label[for="age"]',
            kind: 'text',
            expected: '연령',
            severity: 'P1',
        },
        {
            id: 'field.region.label',
            desc: '거주지 라벨 "거주지"',
            selector: 'label[for="region"]',
            kind: 'text',
            expected: '거주지',
            severity: 'P1',
        },
        {
            id: 'field.etc.label',
            desc: '기타 조건 라벨 "기타 조건"',
            selector: 'label[for="etc"]',
            kind: 'text',
            expected: '기타 조건',
            severity: 'P1',
        },
        {
            id: 'submit.button.exists',
            desc: '"저장" 버튼',
            selector: '.admin-eligibility-form-actions button[type="submit"]',
            kind: 'text',
            expected: '저장',
            severity: 'P0',
        },
        {
            id: 'cancel.link.exists',
            desc: '"취소" 링크 존재 (프로그램 상세로 이동)',
            selector: '.admin-eligibility-form-actions a.admin-btn--outline',
            kind: 'text',
            expected: '취소',
            severity: 'P1',
        },
        {
            id: 'form.method.post',
            desc: 'form 은 POST 로 자격요건 endpoint 로 제출',
            selector: `form.admin-eligibility-form[method="post"][action="/admin/programs/${SEED_PROGRAM_ID}/eligibility"]`,
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
    ],
};
