/**
 * 관리자 프로그램 캘린더 뷰 (`/admin/programs?view=calendar`) 디자인 계약.
 *
 * D-admin-calendar (2026-10-12) 산출. 사용자 F0f 캘린더(`programs-calendar.ts`) 패턴을
 * admin 네임스페이스로 포팅. POLICY P-6 (42셀 6행 고정) + Q1~Q10 결정 반영.
 *
 * 범위: 뷰 토글 세그먼트 · 42셀 grid · 월 이동 nav · 우측 300px slide 패널 · 범례.
 *   - 카드 뷰 placeholder 는 별도 계약 신설 안 함 (별 티켓에서 실 UI 신설 시 포함)
 *   - 주/리스트 뷰 없음 (Q4 MVP = 월간만)
 */

import type { ScreenContract } from './types';

export const adminProgramsCalendarContract: ScreenContract = {
    screen: 'admin-programs-calendar',
    path: '/admin/programs?view=calendar',
    source: 'D-admin-calendar 2026-10-12 · POLICY P-6 · F0f CSS 재사용 (Q9)',
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
    ],
};
