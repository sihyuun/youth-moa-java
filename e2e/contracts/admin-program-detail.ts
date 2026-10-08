/**
 * 관리자 프로그램 상세 (`/admin/programs/{id}`) 디자인 계약.
 *
 * FOLLOW-admin-program-detail-readonly (2026-10-07 · Q1~Q8 모두 권장안):
 *   - Q1 편집 URL = `/admin/programs/{id}/edit` 서브경로로 분리
 *   - Q2 저장 후 redirect = `/admin/programs/{id}` (상세로 복귀)
 *   - Q4 대기자/자동승인 토글 = 이월 (배너 미포함)
 *   - Q5 복제 = 이월, ⋯ 더보기에 삭제만
 *   - Q6 신청 현황 = 상세 내 테이블 없음. "신청 현황 N건 보기" 버튼으로 /applications 이동
 *   - Q7 content 렌더 = `th:text` plain (사용자 /programs/{id} 템플릿과 동일, 저장 시 sanitize 미도입)
 *   - Q8 watch-button = 상세 헤더 전용 (form 에서 제거됨)
 *
 * 원본: docs/00_assets/admin/HANDOFF.md L234-238 + admin/prototype.html L1446~1619
 */

import type { ScreenContract } from './types';

export const adminProgramDetailContract: ScreenContract = {
    screen: 'admin-program-detail',
    path: '/admin/programs/1',
    source: 'admin HANDOFF L234-238 + prototype.html L1446~1619 · 2026-10-07 FOLLOW-admin-program-detail-readonly',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'header.gnb.programs.active',
            desc: '관리자 GNB "프로그램 관리" 활성 (상세에서도)',
            selector: 'nav.admin-header-nav a.admin-nav-link.active',
            kind: 'text',
            expected: '프로그램 관리',
            severity: 'P0',
        },
        {
            id: 'breadcrumb.back.to.list',
            desc: '브레드크럼 "← 프로그램 목록" 링크',
            selector: '.admin-program-detail-breadcrumb a',
            kind: 'exists',
            expected: true,
            severity: 'P1',
        },
        {
            id: 'header.title',
            desc: '프로그램 제목 (헤더)',
            selector: '.admin-program-detail-title',
            kind: 'exists',
            expected: true,
            proto: 'prototype.html L1453',
            severity: 'P0',
        },
        {
            id: 'header.status.badge',
            desc: '상태 뱃지 (헤더)',
            selector: '.admin-program-detail-header .admin-status-badge',
            kind: 'exists',
            expected: true,
            proto: 'prototype.html L1454',
            severity: 'P0',
        },
        {
            id: 'header.applied.count',
            desc: '신청수 표기',
            selector: '.admin-program-detail-applied',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF L235 "신청 N명"',
            severity: 'P1',
        },
        {
            id: 'header.watch.button',
            desc: '지켜보기 버튼 (상세 헤더 전용 · Q8)',
            selector: '.admin-program-detail-header-actions [data-testid="watch-button"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'header.cta.edit',
            desc: '수정 버튼 → /edit (Q1)',
            selector: '.admin-program-detail-header-actions a[href$="/edit"]',
            kind: 'exists',
            expected: true,
            proto: 'prototype.html L1457~1460',
            severity: 'P0',
        },
        {
            id: 'header.cta.edit.label',
            desc: '수정 버튼 라벨',
            selector: '.admin-program-detail-header-actions a.admin-btn--primary',
            kind: 'text',
            expected: '수정',
            severity: 'P0',
        },
        {
            id: 'header.more.menu.trigger',
            desc: '⋯ 더보기 메뉴 트리거',
            selector: '.admin-program-detail-header-actions [data-detail-menu-trigger]',
            kind: 'exists',
            expected: true,
            proto: 'prototype.html L1463~1465',
            severity: 'P1',
        },
        {
            id: 'header.more.menu.delete',
            desc: '⋯ 더보기 메뉴 삭제 항목 (Q5: 복제 이월, 삭제만)',
            selector: '[data-testid="detail-menu-delete"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'header.more.menu.clone.missing',
            desc: '복제 항목 미노출 (Q5 이월)',
            selector: '[data-testid="detail-menu-clone"]',
            kind: 'count',
            expected: 0,
            severity: 'P1',
        },
        {
            id: 'layout.info.column',
            desc: '좌측 정보 카드 (썸네일·청년센터·진행기간·신청기간·모집인원·장소·문의처·첨부)',
            selector: '.admin-program-detail-info',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF L236 "좌측 정보 카드"',
            severity: 'P0',
        },
        {
            id: 'layout.desc.column',
            desc: '우측 설명 카드',
            selector: '.admin-program-detail-desc',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF L236 "우측 설명 카드"',
            severity: 'P0',
        },
        {
            id: 'info.center.label',
            desc: '청년센터 라벨',
            selector: '.admin-program-detail-info [data-field="center"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '청년센터',
            severity: 'P1',
        },
        {
            id: 'info.period.label',
            desc: '진행기간 라벨',
            selector: '.admin-program-detail-info [data-field="period"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '진행 기간',
            severity: 'P1',
        },
        {
            id: 'info.applyperiod.label',
            desc: '신청기간 라벨',
            selector: '.admin-program-detail-info [data-field="apply-period"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '신청 기간',
            severity: 'P1',
        },
        {
            id: 'info.capacity.label',
            desc: '모집인원 라벨 (applied/capacity 포맷)',
            selector: '.admin-program-detail-info [data-field="capacity"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '모집 인원',
            severity: 'P1',
        },
        {
            id: 'info.venue.label',
            desc: '장소 라벨',
            selector: '.admin-program-detail-info [data-field="venue"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '진행 장소',
            severity: 'P1',
        },
        {
            id: 'info.contact.label',
            desc: '문의처 라벨',
            selector: '.admin-program-detail-info [data-field="contact"] .admin-program-detail-field-label',
            kind: 'text',
            expected: '문의처',
            severity: 'P1',
        },
        {
            id: 'applications.view.button',
            desc: '"신청 현황 N건 보기" 버튼 → /applications (Q6)',
            selector: 'a[data-testid="link-applications"]',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'applications.inline.table.missing',
            desc: '상세 내 신청 현황 테이블 없음 (Q6: /applications 유지)',
            selector: '.admin-program-detail-applications-table',
            kind: 'count',
            expected: 0,
            severity: 'P1',
        },
        {
            id: 'waitlist.banner.missing',
            desc: '대기자/자동승인 배너 없음 (Q4 이월)',
            selector: '.admin-program-detail-waitlist-banner',
            kind: 'count',
            expected: 0,
            severity: 'P1',
        },
        {
            id: 'delete.modal.markup',
            desc: '삭제 confirm 모달 markup (⋯ 메뉴에서 호출)',
            selector: '#program-delete-modal',
            kind: 'exists',
            expected: true,
            severity: 'P0',
        },
        {
            id: 'form.inline.missing',
            desc: '상세는 form 폼이 아님 (편집은 /edit)',
            selector: 'form.admin-program-form',
            kind: 'count',
            expected: 0,
            severity: 'P0',
        },
    ],
};
