/**
 * A5 admin-users (2026-09-15) — 관리자 사용자 관리 목록 (`/admin/users`) 디자인 계약.
 *
 * 원본: `docs/00_assets/admin/prototype.html` L1166~1272.
 * 결정 요약 (spec §12):
 *  - Qn-A 이월 → `/admin/staff` 별도 화면 없음. `/admin/users?role=…` 필터로 통합.
 *  - Qn-9 이월 → bulk selection · CSV · checkbox 컬럼 모두 제거 (A8 재도입 대기).
 *  - Qn-4 이월 → "등록하기" 버튼 미포함 (기존 사용자 승격 flow 만).
 */

import type { ScreenContract } from './types';

export const adminUsersListContract: ScreenContract = {
    screen: 'admin-users',
    path: '/admin/users',
    source: 'admin/prototype.html L1166~1272 · docs/specs/A5-admin-users.md',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ─── GNB / 페이지 헤더 ───
        {
            id: 'header.gnb.users.active',
            desc: '관리자 GNB "사용자 관리" 링크 활성',
            selector: 'nav.admin-header-nav a.admin-nav-link.active',
            kind: 'text',
            expected: '사용자 관리',
            proto: 'admin/fragments/header.html currentPage=users',
            severity: 'P0',
        },
        {
            id: 'page.title',
            desc: '페이지 타이틀 "사용자 관리"',
            selector: '.admin-program-title',
            kind: 'text',
            expected: '사용자 관리',
            proto: 'admin/prototype.html L1166',
            severity: 'P0',
        },
        {
            id: 'page.subtitle.exists',
            desc: '서브 카피 존재',
            selector: '.admin-program-sub',
            kind: 'exists',
            expected: true,
            proto: 'admin/user/list.html L28',
            severity: 'P1',
        },

        // ─── 액션 바 (검색 · role filter) ───
        {
            id: 'action.search.input',
            desc: '검색 입력 placeholder',
            selector: 'input.admin-program-search-input',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1178',
            severity: 'P0',
        },
        {
            id: 'action.role.filter.tab.count',
            desc: 'role filter 탭 4옵션 (전체 · 시스템 관리자 · 관리자 · 사용자)',
            selector: 'nav.admin-program-tabs a.admin-program-tab',
            kind: 'count',
            expected: 4,
            proto: 'admin/prototype.html L1181~1185',
            severity: 'P0',
        },
        {
            id: 'action.role.filter.tab.all.default.active',
            desc: '기본 진입 시 "전체" 탭 활성',
            selector: 'nav.admin-program-tabs a.admin-program-tab--active',
            kind: 'text',
            expected: '전체',
            proto: 'admin/prototype.html L1181 default all',
            severity: 'P0',
        },

        // ─── 총 개수 라벨 ───
        {
            id: 'count.label',
            desc: '총 N명 카운트 라벨',
            selector: '.admin-user-count',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1200',
            severity: 'P1',
        },

        // ─── 테이블 헤더 ───
        {
            id: 'list.head.exists',
            desc: '테이블 헤더 존재',
            selector: '.admin-user-row--head',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1214~1228',
            severity: 'P0',
        },
        {
            id: 'list.head.col.no',
            desc: '헤더 컬럼 "No."',
            selector: '.admin-user-row--head .admin-user-col-no',
            kind: 'text',
            expected: 'No.',
            proto: 'admin/prototype.html L1221',
            severity: 'P1',
        },
        {
            id: 'list.head.col.name',
            desc: '헤더 컬럼 "이름"',
            selector: '.admin-user-row--head .admin-user-col-name',
            kind: 'text',
            expected: '이름',
            proto: 'admin/prototype.html L1222',
            severity: 'P0',
        },
        {
            id: 'list.head.col.email',
            desc: '헤더 컬럼 "이메일"',
            selector: '.admin-user-row--head .admin-user-col-email',
            kind: 'text',
            expected: '이메일',
            proto: 'admin/prototype.html L1223',
            severity: 'P0',
        },
        {
            id: 'list.head.col.gender',
            desc: '헤더 컬럼 "성별"',
            selector: '.admin-user-row--head .admin-user-col-gender',
            kind: 'text',
            expected: '성별',
            proto: 'admin/prototype.html L1224',
            severity: 'P1',
        },
        {
            id: 'list.head.col.role',
            desc: '헤더 컬럼 "권한"',
            selector: '.admin-user-row--head .admin-user-col-role',
            kind: 'text',
            expected: '권한',
            proto: 'admin/prototype.html L1225',
            severity: 'P0',
        },
        {
            id: 'list.head.col.phone',
            desc: '헤더 컬럼 "핸드폰"',
            selector: '.admin-user-row--head .admin-user-col-phone',
            kind: 'text',
            expected: '핸드폰',
            proto: 'admin/prototype.html L1226',
            severity: 'P1',
        },
        {
            id: 'list.head.col.join',
            desc: '헤더 컬럼 "가입일"',
            selector: '.admin-user-row--head .admin-user-col-join',
            kind: 'text',
            expected: '가입일',
            proto: 'admin/prototype.html L1227',
            severity: 'P1',
        },
        {
            id: 'list.head.col.last',
            desc: '헤더 컬럼 "최근접속"',
            selector: '.admin-user-row--head .admin-user-col-last',
            kind: 'text',
            expected: '최근접속',
            proto: 'admin/prototype.html L1228 lastAccess',
            severity: 'P1',
        },
        {
            id: 'list.head.col.status',
            desc: '헤더 컬럼 "상태"',
            selector: '.admin-user-row--head .admin-user-col-status',
            kind: 'text',
            expected: '상태',
            proto: 'admin/prototype.html L1229 status',
            severity: 'P0',
        },

        // ─── 데이터 행 ───
        {
            id: 'list.rows.seeded',
            desc: '시드 사용자 최소 10건 (페이지 사이즈)',
            selector: 'a.admin-user-row:not(.admin-user-row--head)',
            kind: 'count-min',
            expected: 10,
            proto: 'DataInitializer 시드 · admin/prototype.html L1230',
            severity: 'P0',
        },
        {
            id: 'list.row.role.badge',
            desc: 'role badge 존재',
            selector: 'a.admin-user-row .admin-role-badge',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1237 roleCfg',
            severity: 'P0',
        },
        {
            id: 'list.row.status.badge',
            desc: 'status badge 존재',
            selector: 'a.admin-user-row .admin-user-status-badge',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1242 statusBadge',
            severity: 'P0',
        },

        // ─── 페이지네이션 ───
        {
            id: 'pagination.exists',
            desc: '페이지네이션 노출 (시드 30+명 · 10건 페이지)',
            selector: 'nav.admin-notice-paging',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1259',
            severity: 'P0',
        },
        {
            id: 'pagination.active.page.1',
            desc: '기본 진입 시 1페이지 활성',
            selector: '.admin-page-btn.admin-page-btn--active',
            kind: 'text',
            expected: '1',
            proto: 'admin/prototype.html L1263',
            severity: 'P1',
        },

        // ─── 이월 항목 (검사 제외) ───
        {
            id: 'action.bulk.checkbox',
            desc: 'checkbox 컬럼',
            selector: '.admin-user-col-check',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1220',
            severity: 'P2',
            deferred: 'A8-admin-bulk-actions · Qn-9 이월 결정 (spec §12)',
        },
        {
            id: 'action.csv.export.button',
            desc: 'CSV 내보내기 버튼',
            selector: 'button.admin-btn-csv',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1189',
            severity: 'P2',
            deferred: 'A8-admin-bulk-actions · Qn-9 이월 결정 (spec §12)',
        },
        {
            id: 'action.register.button',
            desc: '"등록하기" 버튼',
            selector: 'a.admin-btn--primary[href*="/admin/users/new"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1192',
            severity: 'P2',
            deferred: 'A5-1 · Qn-4 이월 (기존 사용자 승격 flow 만)',
        },
        {
            id: 'action.bulk.selection.bar',
            desc: '다크 bulk selection bar',
            selector: '.admin-user-bulk-bar',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1201~1210',
            severity: 'P2',
            deferred: 'A8-admin-bulk-actions · Qn-9 이월 결정 (spec §12)',
        },
    ],
};
