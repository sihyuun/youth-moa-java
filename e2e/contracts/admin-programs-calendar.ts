/**
 * 관리자 프로그램 캘린더 뷰 (`/admin/programs?view=calendar`) 디자인 계약.
 *
 * D-admin-calendar (2026-10-12) 산출. 사용자 F0f 캘린더(`programs-calendar.ts`) 패턴을
 * admin 네임스페이스로 포팅. POLICY P-6 (42셀 6행 고정) + Q1~Q10 결정 반영.
 *
 * FOLLOW-admin-calendar-prototype-match (2026-10-14):
 *   우측 패널 카드를 admin prototype L1125~1157 매칭으로 전면 재작성.
 *   - 썸네일 100%×100px 보라 bg + 텍스트 (Q1=b)
 *   - 제목 anchor + 센터명 (Q2=a)
 *   - 진행 기간 / 신청 기간 / 신청 현황 3라인 (Q3·Q4)
 *   - 상세보기 버튼 primary bg + chevron
 *   - capacity bar / chip 제거 (negative check 로 regression 방지)
 */

import type { ScreenContract } from './types';

export const adminProgramsCalendarContract: ScreenContract = {
    screen: 'admin-programs-calendar',
    path: '/admin/programs?view=calendar',
    source: 'D-admin-calendar 2026-10-12 · FOLLOW-admin-calendar-prototype-match 2026-10-14 · POLICY P-6',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ─── 뷰 토글 세그먼트 ───
        {
            id: 'view.toggle.exists',
            desc: '뷰 토글 세그먼트 (목록·카드·캘린더 3버튼)',
            selector: '.admin-program-view-toggle',
            kind: 'exists',
            expected: true,
            proto: 'D-admin-calendar spec §6',
            severity: 'P0',
        },
        {
            id: 'view.toggle.calendar.active',
            desc: '캘린더 뷰 진입 시 calendar 버튼이 active',
            selector: '.admin-program-view-toggle .admin-program-view-btn--active',
            kind: 'text',
            expected: '캘린더',
            proto: 'D-admin-calendar spec §6',
            severity: 'P0',
        },

        // ─── 42셀 grid (POLICY P-6) ───
        {
            id: 'grid.42cells',
            desc: 'POLICY P-6: grid 셀 42개 (6행 × 7열)',
            selector: '.program-calendar-grid .program-calendar-cell',
            kind: 'count',
            expected: 42,
            proto: 'POLICY P-6',
            severity: 'P0',
        },
        {
            id: 'grid.dow.header',
            desc: '요일 헤더 7열 (일~토)',
            selector: '.program-calendar-dow .program-calendar-dow-cell',
            kind: 'count',
            expected: 7,
            proto: 'F0f 공용 CSS',
            severity: 'P0',
        },

        // ─── 월 이동 nav + 오늘 ───
        {
            id: 'toolbar.today.btn',
            desc: '[오늘] 버튼 존재',
            selector: '.program-calendar-today-btn',
            kind: 'exists',
            expected: true,
            proto: 'F0f 공용',
            severity: 'P0',
        },
        {
            id: 'toolbar.nav.title',
            desc: '월 타이틀 "YYYY년 M월" 포맷',
            selector: '[data-testid="admin-calendar-title"]',
            kind: 'exists',
            expected: true,
            proto: 'F0f 공용',
            severity: 'P0',
        },
        {
            id: 'toolbar.nav.prev',
            desc: '이전 달 arrow 링크',
            selector: '.program-calendar-nav a[aria-label="이전 달"]',
            kind: 'exists',
            expected: true,
            proto: 'F0f 공용',
            severity: 'P0',
        },
        {
            id: 'toolbar.nav.next',
            desc: '다음 달 arrow 링크',
            selector: '.program-calendar-nav a[aria-label="다음 달"]',
            kind: 'exists',
            expected: true,
            proto: 'F0f 공용',
            severity: 'P0',
        },

        // ─── 우측 slide 패널 ───
        {
            id: 'panel.exists',
            desc: 'Q3: 우측 300px slide 패널 (초기 hidden)',
            selector: '.admin-program-calendar-panel',
            kind: 'exists',
            expected: true,
            proto: 'HANDOFF L232 Google Calendar · spec Q3',
            severity: 'P0',
        },

        // ─── 범례 ───
        {
            id: 'legend.items',
            desc: '범례 3종 (진행예정·모집중·종료·운영중단)',
            selector: '.program-calendar-legend .program-calendar-legend-item',
            kind: 'count',
            expected: 3,
            proto: 'spec §7',
            severity: 'P1',
        },

        // ─── 카드 뷰 placeholder 금지 (Q8) ───
        {
            id: 'calendar.scope.no_card_impl',
            desc: 'Q8: 카드 뷰 실제 UI 미구현 (placeholder flash 만, calendar 뷰에서는 표시 X)',
            selector: '.admin-program-card-grid',
            kind: 'not-exists',
            expected: true,
            proto: 'spec Q8',
            severity: 'P1',
        },

        // ─── FOLLOW-admin-calendar-prototype-match: 우측 패널 카드 구조 ───
        // 패널은 초기 hidden 이지만 panelData 는 사전 렌더되어 DOM 에 존재한다.
        // panel-group[hidden] 안에 카드가 들어가므로 count 는 그대로 세어진다.
        {
            id: 'panel.card.wrapper.exists',
            desc: 'FOLLOW: 패널 카드 wrapper 는 <article> (anchor 중첩 회피)',
            selector: '.admin-program-calendar-panel-card',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1125 · spec Q2=a',
            severity: 'P0',
        },
        {
            id: 'panel.card.thumb_text.exists',
            desc: 'FOLLOW: 텍스트 전용 썸네일 (제목 첫 글자 32px 보라)',
            selector: '.admin-program-calendar-panel-card-thumb-text',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1130 · spec Q1=b',
            severity: 'P0',
        },
        {
            id: 'panel.card.title.a.exists',
            desc: 'FOLLOW: 제목 anchor (제목만 상세 링크)',
            selector: '.admin-program-calendar-panel-card-title a[href^="/admin/programs/"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1134 · spec Q2=a',
            severity: 'P0',
        },
        {
            id: 'panel.card.center.exists',
            desc: 'FOLLOW: 센터명 라인 (11px #6E6B82)',
            selector: '.admin-program-calendar-panel-card-center',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1135',
            severity: 'P1',
        },
        {
            id: 'panel.card.divider.exists',
            desc: 'FOLLOW: 구분선 (1px #F0EFF3)',
            selector: '.admin-program-calendar-panel-card-divider',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1137',
            severity: 'P2',
        },
        {
            id: 'panel.card.info.rows.count',
            desc: 'FOLLOW: 정보 3라인 (진행 기간 / 신청 기간 / 신청 현황)',
            selector: '.admin-program-calendar-panel-card-info-row',
            kind: 'count-min',
            expected: 3,
            proto: 'admin/prototype.html L1139~1150',
            severity: 'P0',
        },
        {
            id: 'panel.card.applied.exists',
            desc: 'FOLLOW: 신청 현황 값 (primary bold 12px)',
            selector: '.admin-program-calendar-panel-card-applied',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1148~1149 · spec Q5=a',
            severity: 'P0',
        },
        {
            id: 'panel.card.detail_btn.exists',
            desc: 'FOLLOW: 상세보기 버튼 (primary bg + white)',
            selector: '.admin-program-calendar-panel-card-detail-btn[href^="/admin/programs/"]',
            kind: 'exists',
            expected: true,
            proto: 'admin/prototype.html L1152~1155',
            severity: 'P0',
        },

        // ─── negative checks: regression 방지 ───
        {
            id: 'panel.card.capacity_bar.absent',
            desc: 'FOLLOW: capacity bar 는 admin 캘린더 패널 카드에 없다 (prototype 매칭)',
            selector: '.admin-program-calendar-panel-card .program-calendar-panel-card-bar',
            kind: 'not-exists',
            expected: true,
            proto: 'admin/prototype.html L1125~1157 (bar 없음) · spec Q5 연쇄',
            severity: 'P0',
        },
        {
            id: 'panel.card.chip.absent',
            desc: 'FOLLOW: chip (D-3 / 마감 / 종료) 는 admin 캘린더 패널 카드에 없다 (prototype 매칭)',
            selector: '.admin-program-calendar-panel-card .program-calendar-panel-card-chip',
            kind: 'not-exists',
            expected: true,
            proto: 'admin/prototype.html L1125~1157 (chip 없음) · spec Q5 연쇄',
            severity: 'P0',
        },
    ],
};
