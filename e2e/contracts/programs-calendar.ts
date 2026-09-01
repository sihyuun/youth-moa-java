/**
 * 프로그램 캘린더 뷰 화면 (`/programs?view=calendar`) 디자인 계약 — 기계 검사 항목.
 *
 * 정본: docs/00_assets/Program Calendar.dc.html (7 섹션 · 1183 라인)
 *   §1a (데스크톱 확정본) · §5a (chip 4종 매핑) · §6a (시작일 축) · §7a (빈 달 배너)
 * 서술 계약: docs/design-contracts/programs.md §5-A ~ §5-I
 * 스펙: docs/specs/F0f-calendar-view.md (spec_confirmed)
 *
 * PR-1 구현: #195 (2026-08-31). 이 계약은 그 구현을 기반으로 신설되었다.
 *
 * 디자인 토큰:
 *   primary #3F30E9 (모집중 셀·선택 셀 테두리·오늘 셀 배경)
 *   secondary #F97316 (진행예정 셀·UPCOMING chip)
 *   error rgb(239, 68, 68) (D-3 이하 urgent chip)
 *   textTri oklch(0.7 0.02 280) (종료 셀·범례)
 *   radius: cell 8 / panel 12 / chip 20 (pill)
 */

import type { ScreenContract } from './types';

export const programsCalendarContract: ScreenContract = {
    screen: 'programs-calendar',
    path: '/programs?view=calendar',
    source: 'dc.html §1a/§5a/§7a + spec F0f, 2026-08-31 PR-1 (#195)',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ── 진입 · 기본 구조 ─────────────────────────────────────
        {
            id: 'grid.exists',
            desc: '캘린더 격자 존재 (view=calendar 진입 시)',
            selector: '.program-calendar-grid',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L735',
            severity: 'P0',
        },
        {
            id: 'grid.cells.count',
            desc: '42셀 (6행 × 7일) 고정 — 5행은 8월 등에서 잘림 (dc.html §1a L799)',
            selector: '.program-calendar-cell',
            kind: 'count',
            expected: 42,
            proto: 'dc.html §1a 각주 L799 "6행 고정(42칸) 으로 정정 필요"',
            severity: 'P0',
        },
        {
            id: 'cell.height',
            desc: '셀 높이 104px',
            selector: '.program-calendar-cell',
            kind: 'css',
            prop: 'height',
            expected: '104px',
            proto: 'dc.html §1a L738',
            severity: 'P2',
        },

        // ── 툴바 ─────────────────────────────────────────────
        {
            id: 'toolbar.exists',
            desc: '툴바 존재 ([오늘] · nav · 우측 여백)',
            selector: '.program-calendar-toolbar',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L713',
            severity: 'P1',
        },
        {
            id: 'toolbar.display',
            desc: 'grid 3-column (1fr auto 1fr) 로 nav 정중앙 정렬',
            selector: '.program-calendar-toolbar',
            kind: 'css',
            prop: 'display',
            expected: 'grid',
            proto: '2026-08-31 사용자 시각 fix (dc.html §1a 정본 반영)',
            severity: 'P1',
        },
        {
            id: 'toolbar.nav.centered',
            desc: 'nav 정중앙 정렬 (justify-self: center) — grid 3-column 정합',
            selector: '.program-calendar-nav',
            kind: 'css',
            prop: 'justify-self',
            expected: 'center',
            proto: 'dc.html §1a L718 · programs.md §5-A "±2px 정량 assert"',
            severity: 'P1',
        },
        {
            id: 'toolbar.todayBtn.leftAligned',
            desc: '[오늘] 버튼 좌측 정렬 (justify-self: start)',
            selector: '.program-calendar-today-btn',
            kind: 'css',
            prop: 'justify-self',
            expected: 'start',
            proto: 'dc.html §1a L714 · programs.md §5-A "[오늘] 좌측"',
            severity: 'P1',
        },
        {
            id: 'toolbar.axisLabel.absent',
            desc: '"시작일 기준" 라벨 없음 (2026-08-31 사용자 시각 fix 로 제거)',
            selector: '.program-calendar-axis-label',
            kind: 'count',
            expected: 0,
            proto: 'spec §3-A #2 취소 (dc.html §1a L723 초회 채택 → 제거)',
            severity: 'P1',
        },
        {
            id: 'todayBtn.exists',
            desc: '[오늘] 버튼 존재',
            selector: '.program-calendar-today-btn',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L714',
            severity: 'P1',
        },
        {
            id: 'nav.exists',
            desc: '월 이동 nav (‹ YYYY년 M월 ›)',
            selector: '.program-calendar-nav',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L718',
            severity: 'P1',
        },
        {
            id: 'nav.arrows.htmx',
            desc: '월 이동 화살표 2개 모두 HTMX 부분 스왑 (전체 페이지 리로드 방지 · 사용자 관측 "day 16 컷오프" hotfix)',
            selector: '.program-calendar-nav-arrow[hx-get][hx-target=".program-calendar-layout"]',
            kind: 'count',
            expected: 2,
            proto: '_calendar-fragment.html:26-38 (2026-09-01) — HTMX 스왑 평균 111ms vs 전체 리로드 1750ms',
            severity: 'P1',
        },

        // ── 요일 헤더 ────────────────────────────────────────
        {
            id: 'dow.count',
            desc: '요일 셀 7개',
            selector: '.program-calendar-dow-cell',
            kind: 'count',
            expected: 7,
            proto: 'dc.html §1a L726~734',
            severity: 'P1',
        },

        // ── 오늘 셀 (dc.html §1a L738·programs.md §5-B) ────────
        {
            id: 'cell.today.count',
            desc: '오늘 셀 1개 (현재 월 진입 시)',
            selector: '.program-calendar-cell--today',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L738 · programs.md §5-B',
            severity: 'P1',
        },
        {
            id: 'cell.today.badge',
            desc: '오늘 셀에 "오늘" 뱃지',
            selector: '.program-calendar-cell-today-badge',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L738 · programs.md §5-B',
            severity: 'P2',
        },
        {
            id: 'cell.today.num.primaryBg',
            desc: '오늘 셀 날짜 원형 primary(#3F30E9) 배경',
            selector: '.program-calendar-cell-num--today',
            kind: 'css',
            prop: 'background-color',
            expected: 'rgb(63, 48, 233)',
            proto: 'dc.html §1a "원형 primary 배경" · programs.md §5-B',
            severity: 'P1',
        },
        {
            id: 'cell.today.num.circle',
            desc: '오늘 셀 날짜 원형 (border-radius 50%)',
            selector: '.program-calendar-cell-num--today',
            kind: 'css',
            prop: 'border-radius',
            expected: '50%',
            proto: 'dc.html §1a "원형" · programs.md §5-B',
            severity: 'P2',
        },

        // ── 범례 3종 (dc.html §5a) ───────────────────────────
        {
            id: 'legend.count',
            desc: '범례 3종 (진행예정 · 모집중 · 종료)',
            selector: '.program-calendar-legend .program-calendar-legend-item',
            kind: 'count',
            expected: 3,
            proto: 'dc.html §5a 매핑 표 (셀 pill 3색)',
            severity: 'P1',
        },
        {
            id: 'legend.upcoming.exists',
            desc: '범례: 진행예정 dot (secondary orange)',
            selector: '.program-calendar-legend .program-calendar-legend-dot--upcoming',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §5a UPCOMING → secondary #F97316',
            severity: 'P1',
        },
        {
            id: 'legend.open.exists',
            desc: '범례: 모집중 dot (primary)',
            selector: '.program-calendar-legend .program-calendar-legend-dot--open',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §5a OPEN → primary #3F30E9',
            severity: 'P1',
        },
        {
            id: 'legend.ended.exists',
            desc: '범례: 종료 dot (textTri)',
            selector: '.program-calendar-legend .program-calendar-legend-dot--ended',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §5a ENDED → textTri',
            severity: 'P1',
        },

        // ── 우측 패널 ────────────────────────────────────────
        {
            id: 'panel.width',
            desc: '우측 패널 폭 320px',
            selector: '.program-calendar-panel',
            kind: 'css',
            prop: 'width',
            expected: '320px',
            proto: 'dc.html §1a L766',
            severity: 'P1',
        },
        {
            id: 'panel.initialHidden',
            desc: '우측 패널 초기 hidden (셀 클릭 전까지 노출 안 됨)',
            selector: '.program-calendar-panel[hidden]',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L783 "{selDay &&" — selDay=null 초기 상태',
            severity: 'P1',
        },
        {
            id: 'panel.empty.initialHidden',
            desc: '"이 날에는 프로그램이 없어요" 초기 hidden',
            selector: '.program-calendar-panel-empty[hidden]',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §1a L793 "daySel.length===0 ? ..."',
            severity: 'P2',
        },
        {
            id: 'panel.group.exists',
            desc: '날짜 그룹 최소 1개 이상 존재 (vacuous truth 방지 · panel.group.allInitialHidden 하한)',
            selector: '.program-calendar-panel-group',
            kind: 'exists',
            expected: true,
            proto: 'programs.md §5-F "JS 로 선택 날짜만 노출" — 그룹 자체는 서버 렌더 · 개수는 시드 의존',
            severity: 'P1',
        },
        {
            id: 'panel.card.thumb.notEagerLoaded',
            desc: '우측 패널 카드 썸네일에 loading="lazy" 누락 없음 (hidden 이어도 브라우저는 eager fetch — 월 넘김 지연 원인)',
            selector: '.program-calendar-panel-card-thumb img:not([loading="lazy"])',
            kind: 'count',
            expected: 0,
            proto: '_calendar-fragment.html:140 hotfix (2026-08-31) — 사용자 관측: 월 넘김 시 day 16 이후 지연 페인트',
            severity: 'P2',
        },
        {
            id: 'panel.group.allInitialHidden',
            desc: '모든 날짜 그룹 초기 hidden — JS 로 선택 날짜만 노출 (2026-08-31 사고 방지)',
            selector: '.program-calendar-panel-group:not([hidden])',
            kind: 'count',
            expected: 0,
            proto: 'programs.md §5-F 마지막 항목 (CSS :not([hidden]) 스코핑)',
            severity: 'P1',
        },

        // ── view-toggle 링크화 (2026-08-31 회귀 방지) ──────────
        {
            id: 'viewtoggle.linkOnly',
            desc: '뷰 전환 버튼은 링크(<a>) 형태 — disabled 버튼 0',
            selector: '.view-toggle button, .view-toggle .view-toggle-btn[disabled]',
            kind: 'count',
            expected: 0,
            proto: 'PR-1 (2026-08-31) — 초회 disabled placeholder 제거',
            severity: 'P1',
        },
        {
            id: 'viewtoggle.textDecoration',
            desc: '뷰 전환 버튼 밑줄 없음 (링크 style 리셋)',
            selector: '.view-toggle-btn',
            kind: 'css',
            prop: 'text-decoration-line',
            expected: 'none',
            proto: 'PR-1 사용자 시각 fix #1 (2026-08-31)',
            severity: 'P1',
        },

        // ── 빈 달 배너 존재 안함 검증 (현재 월엔 프로그램 있음) ──
        {
            id: 'emptyBanner.absent.whenNonEmpty',
            desc: '현재 월에 프로그램이 있을 때 빈 달 배너 미노출',
            selector: '.program-calendar-empty-banner',
            kind: 'count',
            expected: 0,
            proto: 'dc.html §7a "배너는 프로그램 없을 때만"',
            severity: 'P1',
        },
    ],
};

/**
 * 빈 달 배너 시나리오 계약 (`?view=calendar&year=2030&month=6` — nearestMonth 존재 case).
 *
 * dc.html §7a · programs.md §5-E.
 * 프로그램 없는 달로 진입 시:
 *   - 배너 노출 ("{M}월에는 프로그램이 없어요. {nearestMonth}월에 N건 있어요.")
 *   - [N월 보기] 버튼 노출 (nearestMonth 필터 유지)
 *   - 격자 42셀은 그대로 렌더 (빈 달이라도 날짜 표시)
 *
 * **URL slot 선정 근거**: 2030-06 은 시드 데이터 기준 미래 빈 달 고정 slot.
 *   시드는 2026-08 기준으로 프로그램을 생성하고 매년 자동 추가되지 않으므로,
 *   4년 후 미래 달은 안정적으로 빈 달로 유지된다. 시드 확장 시 이 slot 재검토 필요.
 *
 * **커버 범위 제한**:
 *   - 이 계약은 banner/btn/grid **존재 여부** 만 검사한다.
 *   - nearestMonth == null 케이스 (모든 필터 조합에서 0건): 시드가 항상 프로그램을 갖고 있어
 *     계약 검사로 커버 불가 → 별도 통합 테스트 스코프.
 *   - tie-break "미래 우선" 정책 (spec §3-A #9): 2030-06 실측 nearestMonth=2026-09 는
 *     미래 후보 부재 케이스이므로 tie-break 발동 없음. 정책 자체 회귀 검증은 별도 통합 테스트 스코프.
 */
export const programsCalendarEmptyContract: ScreenContract = {
    screen: 'programs-calendar-empty',
    path: '/programs?view=calendar&year=2030&month=6',
    source: 'dc.html §7a + spec F0f §3-A #9, 2026-08-31 PR-1 (#195)',
    viewport: { width: 1440, height: 900 },
    checks: [
        {
            id: 'emptyBanner.exists',
            desc: '프로그램 없는 달 진입 시 빈 달 배너 노출',
            selector: '.program-calendar-empty-banner',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §7a · programs.md §5-E',
            severity: 'P0',
        },
        {
            id: 'emptyBanner.text.exists',
            desc: '빈 달 배너 텍스트 span 노출',
            selector: '.program-calendar-empty-banner-text',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §7a "문구 규칙"',
            severity: 'P1',
        },
        {
            id: 'emptyBanner.nearestBtn.exists',
            desc: '[N월 보기] 버튼 노출 (nearestMonth 있는 케이스)',
            selector: '.program-calendar-empty-banner-btn',
            kind: 'count',
            expected: 1,
            proto: 'dc.html §7a "[N월 보기] 버튼" · programs.md §5-E',
            severity: 'P1',
        },
        {
            id: 'emptyBanner.gridStillRendered',
            desc: '배너 있어도 격자 42셀 그대로 렌더',
            selector: '.program-calendar-cell',
            kind: 'count',
            expected: 42,
            proto: 'programs.md §5-E "배너 있어도 격자는 그대로 렌더"',
            severity: 'P1',
        },
        {
            id: 'emptyBanner.nearestBtn.sortPreserved',
            desc: '[N월 보기] 버튼 href 에 sort 파라미터 유지 (다른 nav 링크와 정합)',
            selector: '.program-calendar-empty-banner-btn[href*="sort="]',
            kind: 'count',
            expected: 1,
            proto: '_calendar-fragment.html:52 hotfix (2026-08-31) · dc.html §7a "필터 유지"',
            severity: 'P1',
        },
        {
            id: 'emptyBanner.nearestBtn.htmx',
            desc: '[N월 보기] 버튼도 HTMX 부분 스왑 (nav 화살표와 일관)',
            selector: '.program-calendar-empty-banner-btn[hx-get][hx-target=".program-calendar-layout"]',
            kind: 'count',
            expected: 1,
            proto: '_calendar-fragment.html:52 (2026-09-01) · 전체 페이지 리로드 회피',
            severity: 'P1',
        },
    ],
};
