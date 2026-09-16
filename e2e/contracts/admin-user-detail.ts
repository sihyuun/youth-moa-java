/**
 * A5 admin-users (2026-09-15) — 관리자 사용자 상세 (`/admin/users/{id}`) 디자인 계약.
 *
 * 원본: `docs/00_assets/admin/prototype.html` L1774~1943.
 *
 * Qn-B (spec §12) 페이지 결정 반영. `admin-user-detail` 화면.
 *
 * 이탈(deviation) — prototype 미명시 신설:
 *  - Danger zone (회원 차단/재활성화 CTA)
 *  - 관리자 메모 (admin-note)
 *
 * 이월(deferred):
 *  - 회원 정보 편집 form (name/phone/birth/address 저장) → Qn-11 이월
 *  - 새 비밀번호 필드 → A7 이월
 *  - 신청 상태 변경 dropdown → A4 화면에서 수행
 */

import type { ScreenContract } from './types';

/**
 * seed1@youth-moa.test 의 상세 페이지. E2E 러너는 로그인 후 `/admin/users/{seed1.id}` 로 이동한 상태여야 함.
 * 계약 검사에서는 spec 상 첫 seed 유저의 ID 를 미리 알기 어려우니 admin-users-detail spec 안에서 리다이렉트 후 검사.
 */
export const adminUserDetailContract: ScreenContract = {
    screen: 'admin-user-detail',
    path: '/admin/users/{id}', // 실제 이동은 spec 파일 안에서 수행
    source: 'admin/prototype.html L1774~1943 · docs/specs/A5-admin-users.md',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ─── 헤더 / 뒤로가기 ───
        {
            id: 'header.back.link',
            desc: '"← 사용자 목록" 뒤로가기 링크',
            selector: 'a.admin-user-back',
            kind: 'text',
            expected: '← 사용자 목록',
            proto: 'admin/user/detail.html L27 (prototype 는 "선택된 사용자 상세" 진입 시 nav back)',
            severity: 'P1',
        },
        {
            id: 'header.status.badge',
            desc: '상태 badge 노출',
            selector: '.admin-program-header .admin-user-status-badge',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L35 · prototype L1242',
            severity: 'P1',
        },

        // ─── 2컬럼 그리드 ───
        {
            id: 'layout.detail.grid',
            desc: '2컬럼 grid 컨테이너',
            selector: '.admin-user-detail-grid',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1776 grid-template-columns:1fr 1fr',
            severity: 'P0',
        },
        {
            id: 'layout.cards.2',
            desc: '카드 2개 (좌 프로필 · 우 신청 이력)',
            selector: '.admin-user-detail-grid .admin-user-card',
            kind: 'count',
            expected: 2,
            proto: 'admin/prototype.html L1780 + L1870',
            severity: 'P0',
        },

        // ─── 좌: 회원 정보 카드 ───
        {
            id: 'left.card.title',
            desc: '좌 카드 타이틀 "회원 정보"',
            selector: '.admin-user-detail-grid .admin-user-card:first-child .admin-user-card-title',
            kind: 'text',
            expected: '회원 정보',
            proto: 'admin/prototype.html L1783',
            severity: 'P0',
        },
        {
            id: 'left.field.count',
            desc: '프로필 필드 6개 (이메일·이름·성별·생년월일·핸드폰·주소)',
            selector: '.admin-user-profile-list .admin-user-field',
            kind: 'count',
            expected: 6,
            proto: 'admin/prototype.html L1788~1867',
            severity: 'P0',
        },

        // ─── 권한 radio ───
        {
            id: 'role.form.exists',
            desc: '권한 변경 form',
            selector: 'form.admin-user-role-form',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1822~1839',
            severity: 'P0',
        },
        {
            id: 'role.radios.3',
            desc: '권한 radio 3옵션 (사용자·관리자·시스템 관리자)',
            selector: 'form.admin-user-role-form input[name="role"]',
            kind: 'count',
            expected: 3,
            proto: 'admin/prototype.html L1826~1836',
            severity: 'P0',
        },

        // ─── admin-note ───
        {
            id: 'note.form.exists',
            desc: '관리자 메모 form',
            selector: 'form.admin-user-note-form',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L115 · Qn-2 A 신설',
            severity: 'P0',
        },
        {
            id: 'note.textarea.maxlength',
            desc: '관리자 메모 textarea maxlength 1000',
            selector: 'textarea[name="adminNote"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L120 maxlength=1000',
            severity: 'P1',
        },

        // ─── Danger zone (deviation) ───
        {
            id: 'danger.zone.exists',
            desc: 'Danger zone 카드',
            selector: '.admin-user-danger-zone',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L131 · prototype 미명시 신설 (deviation)',
            severity: 'P0',
        },
        {
            id: 'danger.deactivate.modal',
            desc: '차단 확인 모달 markup',
            selector: '#deactivateModal',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L226~255 · Qn-3 사유 필수',
            severity: 'P0',
        },
        {
            id: 'danger.deactivate.reason.required',
            desc: '모달 사유 textarea (required)',
            selector: '#deactivateModal textarea[name="reason"][required]',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/detail.html L247 · Qn-3 A 필수',
            severity: 'P0',
        },

        // ─── 우: 신청 이력 카드 ───
        {
            id: 'right.card.title',
            desc: '우 카드 타이틀 "프로그램 신청 현황"',
            selector: '.admin-user-detail-grid .admin-user-card:last-child .admin-user-card-title',
            kind: 'text',
            expected: '프로그램 신청 현황',
            proto: 'admin/prototype.html L1872',
            severity: 'P0',
        },
        {
            id: 'right.tabs.4',
            desc: '탭 4옵션 (전체·승인·반려·취소)',
            selector: 'nav.admin-user-tabs a.admin-user-tab',
            kind: 'count',
            expected: 4,
            proto: 'admin/prototype.html L1889~1894',
            severity: 'P0',
        },
        {
            id: 'right.tabs.all.default.active',
            desc: '기본 진입 시 "전체" 탭 활성',
            selector: 'nav.admin-user-tabs a.admin-user-tab--active',
            kind: 'text',
            expected: '전체',
            proto: 'admin/prototype.html L1890 default all',
            severity: 'P0',
        },

        // ─── 이월 항목 (검사 제외) ───
        {
            id: 'left.password.new.input',
            desc: '새 비밀번호 입력',
            selector: 'input[type="password"][name="newPassword"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1789~1801',
            severity: 'P2',
            deferred: 'A7-admin-header-live · Qn-11 B (임시 비밀번호 발급 flow)',
        },
        {
            id: 'left.profile.save.button',
            desc: '프로필 저장 버튼',
            selector: 'form.admin-user-profile-form button[type="submit"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1866',
            severity: 'P2',
            deferred: 'A5-1 · Qn-11 이월 (관리자 프로필 편집)',
        },
        {
            id: 'right.status.dropdown',
            desc: '신청 카드 내 상태 dropdown',
            selector: '.admin-user-app-card select[name="status"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1903~1907',
            severity: 'P2',
            deferred: 'A4 admin-program-detail 에서 상태 변경 수행',
        },
    ],
};
