/**
 * 관리자 공지 관리 (`/admin/notices`, `/admin/notices/new`, `/admin/notices/{id}`) 디자인 계약.
 *
 * 참고: admin prototype.html 에 공지 관리 화면이 부재 (spec §0). 사용자 사이드 notice/list.detail 톤 +
 * admin POLICY (다크 헤더 · 인디고 primary · 존댓말) 를 근거로 구현·계약 신설.
 *
 * A-admin-notice-attachment (2026-09-03) 산출.
 */

import type { ScreenContract } from './types';

export const adminNoticeListContract: ScreenContract = {
    screen: 'admin-notice-list',
    path: '/admin/notices',
    source: 'admin POLICY + 사용자 notice/list · 2026-09-03 A-admin-notice-attachment',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'header.gnb.notices.active',
            desc: '관리자 GNB "공지 관리" 링크 활성',
            selector: 'nav.admin-header-nav a.admin-nav-link.active',
            kind: 'text',
            expected: '공지 관리',
            proto: 'admin/fragments/header.html currentPage=notices',
            severity: 'P0',
        },
        {
            id: 'page.title.exists',
            desc: '페이지 타이틀 "공지 관리"',
            selector: '.admin-notice-title',
            kind: 'text',
            expected: '공지 관리',
            proto: 'admin/notice/list.html',
            severity: 'P0',
        },
        {
            id: 'page.subtitle.exists',
            desc: '서브 카피 존재',
            selector: '.admin-notice-sub',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'create.button.exists',
            desc: '"신규 등록" 버튼',
            selector: '.admin-notice-header a.admin-btn.admin-btn--primary',
            kind: 'text',
            expected: '+ 신규 등록',
            severity: 'P0',
        },
        {
            id: 'list.head.row',
            desc: '테이블 헤더 존재',
            selector: '.admin-notice-row.admin-notice-row--head',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'list.head.col.title',
            desc: '헤더 "제목" 컬럼',
            selector: '.admin-notice-row--head .admin-notice-col-title',
            kind: 'text',
            expected: '제목',
            severity: 'P1',
        },
        {
            id: 'list.rows.seeded',
            desc: '시드된 공지 12건 이상 렌더 (첫 페이지 20건)',
            selector: '.admin-notice-row:not(.admin-notice-row--head)',
            kind: 'count-min',
            expected: 12,
            severity: 'P0',
        },
    ],
};

export const adminNoticeFormContract: ScreenContract = {
    screen: 'admin-notice-form',
    path: '/admin/notices/new',
    source: 'admin POLICY · A-admin-notice-attachment',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'form.title.new',
            desc: '신규 등록 모드 타이틀',
            selector: '.admin-notice-title',
            kind: 'text',
            expected: '공지 등록',
            severity: 'P0',
        },
        {
            id: 'form.field.title',
            desc: '제목 입력',
            selector: 'form.admin-notice-form input[name="title"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'form.field.content',
            desc: '내용 textarea',
            selector: 'form.admin-notice-form textarea[name="content"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'form.field.category',
            desc: '분류 select',
            selector: 'form.admin-notice-form select[name="category"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'form.field.pinned',
            desc: '상단 고정 체크박스',
            selector: 'form.admin-notice-form input[name="isPinned"]',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'form.submit',
            desc: '등록 버튼',
            selector: 'form.admin-notice-form button[type="submit"]',
            kind: 'text',
            expected: '등록',
            severity: 'P0',
        },
        {
            id: 'form.attachments.hidden.on.new',
            desc: '신규 모드에서 첨부 섹션 미노출',
            selector: '.admin-notice-attachments-wrapper',
            kind: 'not-exists',
            expected: true,
            severity: 'P1',
        },
    ],
};

export const adminNoticeEditContract: ScreenContract = {
    screen: 'admin-notice-edit',
    path: '/admin/notices/1',
    source: 'admin POLICY · A-admin-notice-attachment',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'edit.title',
            desc: '편집 모드 타이틀',
            selector: '.admin-notice-title',
            kind: 'text',
            expected: '공지 편집',
            severity: 'P0',
        },
        {
            id: 'edit.title.prefilled',
            desc: '제목 인풋 prefilled',
            selector: 'form.admin-notice-form input[name="title"]',
            kind: 'attr-not-empty',
            attr: 'value',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'edit.attachments.section',
            desc: '첨부 관리 섹션 노출',
            selector: '#admin-notice-attachments',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'edit.upload.form',
            desc: '업로드 폼 노출',
            selector: 'form.admin-notice-upload-form',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'edit.delete.button',
            desc: '삭제 버튼 (canEdit true 시)',
            selector: '.admin-btn.admin-btn--danger',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'edit.confirm.modal',
            desc: '커스텀 confirm 모달 markup',
            selector: '#notice-delete-modal',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
    ],
};
