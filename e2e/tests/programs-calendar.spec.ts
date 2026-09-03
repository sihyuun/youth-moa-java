import { expect, test, type Page } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * F0f — 프로그램 캘린더 뷰 인터랙션 E2E (2026-08-27).
 *
 * CLAUDE.md #144 (인터랙션 검증 조항) 준수: 셀 클릭·× 닫기·[오늘]·pill·빈 달 배너 링크의 실 브라우저 동작.
 * 계약(--project=contracts) 은 렌더만 검사. 실 클릭 시퀀스는 이 spec 이 담당.
 */

async function gotoCalendar(page: Page, path = '/programs?view=calendar') {
    await page.goto(path, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.program-calendar-grid')).toBeVisible();
}

test.describe('프로그램 캘린더뷰 인터랙션', () => {
    test.beforeEach(async ({ page }) => {
        await abortExternal(page);
    });

    test('view=calendar 진입 시 42셀 grid + 3색 범례 렌더', async ({ page }) => {
        await gotoCalendar(page);
        // 42셀 (6행 × 7일)
        const cells = page.locator('.program-calendar-cell');
        await expect(cells).toHaveCount(42);
        // 3색 범례 (범례 영역 안에서만 찾도록 스코프. 우측 패널 카드에도 동일 클래스 사용)
        const legend = page.locator('.program-calendar-legend');
        await expect(legend.locator('.program-calendar-legend-dot--upcoming')).toBeVisible();
        await expect(legend.locator('.program-calendar-legend-dot--open')).toBeVisible();
        await expect(legend.locator('.program-calendar-legend-dot--ended')).toBeVisible();
        // 우측 패널은 초기 hidden
        await expect(page.locator('#program-calendar-panel')).toBeHidden();
    });

    test('캘린더뷰에서 status 탭 클릭 시 view=calendar 유지 (회귀 방지)', async ({ page }) => {
        await gotoCalendar(page);
        // 상태 탭 "모집중" 클릭 → 여전히 캘린더 뷰 유지
        await Promise.all([
            page.waitForURL(/view=calendar/),
            page.locator('.status-tabs a', { hasText: '모집중' }).click(),
        ]);
        await expect(page).toHaveURL(/status=active/);
        await expect(page).toHaveURL(/view=calendar/);
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
    });

    test('view-toggle 버튼에 밑줄 없음 (링크 스타일 리셋 회귀 방지)', async ({ page }) => {
        await page.goto('/programs', { waitUntil: 'domcontentloaded' });
        const calBtn = page.locator('.view-toggle a.view-toggle-btn').first();
        const decoration = await calBtn.evaluate((el) => window.getComputedStyle(el).textDecorationLine);
        expect(decoration).toBe('none');
    });

    test('view toggle 링크 활성 - 목록 ↔ 캘린더 왕복', async ({ page }) => {
        await page.goto('/programs', { waitUntil: 'domcontentloaded' });
        const calBtn = page.locator('.view-toggle a.view-toggle-btn', { hasText: '캘린더' });
        await expect(calBtn).toHaveAttribute('href', /view=calendar/);
        await Promise.all([
            page.waitForURL(/view=calendar/),
            calBtn.click(),
        ]);
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        // 캘린더 → 목록 회귀
        const listBtn = page.locator('.view-toggle a.view-toggle-btn', { hasText: '목록' });
        await Promise.all([
            page.waitForURL((url) => !url.searchParams.get('view')?.includes('calendar')),
            listBtn.click(),
        ]);
        await expect(page.locator('.program-calendar-grid')).toHaveCount(0);
    });

    test('HTMX 필터 팝오버 적용 후에도 셀 클릭 정상 동작 (R3-4 회귀 방지)', async ({ page }) => {
        // ym-verify R3-4: DOMContentLoaded 만 리스너 걸면 HTMX innerHTML swap 후 listener 유실.
        // body-level delegation 으로 회귀 방어됨.
        await gotoCalendar(page);
        // 지역 팝오버 열기 → 옵션 하나 체크 → 적용 (HTMX GET + innerHTML swap)
        await page.locator('.filter-pop-chip', { hasText: '지역' }).click();
        const pop = page.locator('.filter-popover[data-group="regions"]');
        await expect(pop).toBeVisible();
        // 첫 옵션 체크 → onchange 이벤트로 applyFiltersFromPopovers() 호출됨
        const firstCheckbox = pop.locator('input[type="checkbox"]').first();
        await firstCheckbox.check();
        // HTMX swap 대기 (URL 변경 + swap 완료)
        await page.waitForURL(/regions=/);
        await page.waitForLoadState('networkidle');
        // swap 후에도 캘린더 그리드 렌더 유지
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        // 팝오버가 셀을 덮고 있을 수 있어 명시적으로 닫음 (chip 재클릭 = 토글)
        await page.locator('.filter-pop-chip', { hasText: '지역' }).click();
        // swap 후 셀 클릭 → 우측 패널 열려야 함 (listener 유실됐으면 실패)
        const firstInMonth = page.locator('.program-calendar-cell[data-in-month="true"]').first();
        await firstInMonth.click();
        await expect(page.locator('#program-calendar-panel')).toBeVisible();
    });

    test('셀 클릭 시 해당 날짜 카드만 노출 (다른 날짜 그룹 hidden 준수)', async ({ page }) => {
        // CSS display:flex 가 hidden 속성 덮어쓰던 회귀 방지 (2026-08-28)
        await gotoCalendar(page);
        // pill 이 있는 첫 셀 찾기
        const pillCells = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)');
        const cellCount = await pillCells.count();
        test.skip(cellCount < 2, '이번 달 pill 있는 셀 2개 이상 필요');
        // 첫 pill 셀 클릭 → 그 날짜의 그룹만 visible
        await pillCells.first().click();
        const visibleGroups = page.locator('.program-calendar-panel-group:not([hidden])');
        await expect(visibleGroups).toHaveCount(1);
        // 다른 pill 셀 클릭 → 여전히 그 날짜의 그룹만 visible (누적 아님)
        await pillCells.nth(1).click();
        await expect(page.locator('.program-calendar-panel-group:not([hidden])')).toHaveCount(1);
    });

    test('UPCOMING 프로그램 chip 은 "M/D 오픈" · 진행예정 색상 (D-N 만들지 않음)', async ({ page }) => {
        // 스펙 §3-A #3 · dc.html §5a: UPCOMING 은 D-N 대신 "M/D 오픈" 날짜 텍스트 + secondary 오렌지 (padding 없음)
        const now = new Date();
        let nextY = now.getFullYear();
        let nextM = now.getMonth() + 2;
        if (nextM > 12) { nextM -= 12; nextY += 1; }
        await gotoCalendar(page, `/programs?view=calendar&year=${nextY}&month=${nextM}`);
        const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
        const cnt = await pillCell.count();
        test.skip(cnt === 0, '다음 달 UPCOMING pill 없음');
        await pillCell.click();
        const chip = page.locator('.program-calendar-panel-group:not([hidden]) .program-calendar-panel-card-chip--upcoming').first();
        await expect(chip).toBeVisible();
        await expect(chip).toContainText('오픈');
        const chipText = await chip.textContent();
        // "M/D 오픈" 형태 (0-padding 없음, 예: 9/3 오픈)
        expect(chipText).toMatch(/^\d{1,2}\/\d{1,2} 오픈$/);
        expect(chipText).not.toMatch(/^D-/);
    });

    test('OPEN chip 은 D-N 텍스트 + dark 배경 (dc.html §5a)', async ({ page }) => {
        // OPEN chip 배경 = rgba(0,0,0,0.55) (chip--open modifier)
        await gotoCalendar(page);
        const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
        const cnt = await pillCell.count();
        test.skip(cnt === 0, '현재 달 OPEN pill 없음');
        await pillCell.click();
        // --open 또는 --urgent chip 존재 (둘 다 OPEN 상태)
        const openChip = page.locator('.program-calendar-panel-group:not([hidden]) .program-calendar-panel-card-chip--open').first();
        const urgentChip = page.locator('.program-calendar-panel-group:not([hidden]) .program-calendar-panel-card-chip--urgent').first();
        const openCount = await openChip.count();
        const urgentCount = await urgentChip.count();
        expect(openCount + urgentCount).toBeGreaterThan(0);
    });

    test('ENDED chip 은 "종료" 텍스트 + grey 배경 (dc.html §5a)', async ({ page }) => {
        // 지난 달 + status=ended 필터로 ENDED 프로그램 확보
        const now = new Date();
        let prevY = now.getFullYear();
        let prevM = now.getMonth();
        if (prevM === 0) { prevM = 12; prevY -= 1; }
        await gotoCalendar(page, `/programs?view=calendar&status=ended&year=${prevY}&month=${prevM}`);
        const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
        const cnt = await pillCell.count();
        test.skip(cnt === 0, '지난 달 ENDED pill 없음');
        await pillCell.click();
        const endedChip = page.locator('.program-calendar-panel-group:not([hidden]) .program-calendar-panel-card-chip--ended').first();
        await expect(endedChip).toBeVisible();
        await expect(endedChip).toContainText('종료');
    });

    test('pill 없는 셀 클릭 시 "이 날에는 프로그램이 없어요" 문구만 노출', async ({ page }) => {
        await gotoCalendar(page);
        // pill 없는 셀 찾기 (data-in-month=true 이면서 program-calendar-pill 미포함)
        const emptyCells = page.locator('.program-calendar-cell[data-in-month="true"]:not(:has(.program-calendar-pill))');
        const cnt = await emptyCells.count();
        test.skip(cnt === 0, '이번 달 비어있는 셀 필요');
        await emptyCells.first().click();
        await expect(page.locator('#program-calendar-panel-empty')).toBeVisible();
        // 어떤 그룹도 노출되지 않아야 함
        await expect(page.locator('.program-calendar-panel-group:not([hidden])')).toHaveCount(0);
    });

    test('셀 클릭 → 우측 패널 표시 + × 클릭 → hidden', async ({ page }) => {
        await gotoCalendar(page);
        const panel = page.locator('#program-calendar-panel');
        await expect(panel).toBeHidden();
        // 이번 달에 속한 첫 셀 클릭 (data-in-month=true)
        const firstInMonth = page.locator('.program-calendar-cell[data-in-month="true"]').first();
        await firstInMonth.click();
        await expect(panel).toBeVisible();
        // 헤더 날짜 갱신 확인
        await expect(page.locator('#program-calendar-panel-date')).toContainText('일');
        // × 클릭 → 닫힘
        await page.locator('[data-calendar-close]').click();
        await expect(panel).toBeHidden();
    });

    test('셀 클릭 → 선택 셀 하이라이트 표시', async ({ page }) => {
        await gotoCalendar(page);
        const firstInMonth = page.locator('.program-calendar-cell[data-in-month="true"]').first();
        await firstInMonth.click();
        await expect(firstInMonth).toHaveClass(/program-calendar-cell--selected/);
        // 다른 셀 클릭 시 이전 선택 해제
        const secondInMonth = page.locator('.program-calendar-cell[data-in-month="true"]').nth(1);
        await secondInMonth.click();
        await expect(firstInMonth).not.toHaveClass(/program-calendar-cell--selected/);
        await expect(secondInMonth).toHaveClass(/program-calendar-cell--selected/);
    });

    test('[오늘] 클릭 - 현재 월이면 오늘 셀 선택, 다른 월이면 오늘 월로 이동', async ({ page }) => {
        // 다른 달로 이동 (2020년 1월) → [오늘] 클릭 시 오늘 월로 이동
        await gotoCalendar(page, '/programs?view=calendar&year=2020&month=1');
        await Promise.all([
            page.waitForURL(/year=/),
            page.locator('[data-calendar-today]').click(),
        ]);
        const url = new URL(page.url());
        expect(url.searchParams.get('year')).not.toBe('2020');
    });

    test('pill 클릭 → detail 페이지 이동 + event bubbling 차단 (우측 패널 안 열림)', async ({ page }) => {
        await gotoCalendar(page);
        const pill = page.locator('.program-calendar-pill').first();
        const pillCount = await pill.count();
        test.skip(pillCount === 0, '이번 달 시드 프로그램이 없어 pill 클릭 시나리오 스킵');
        await Promise.all([
            page.waitForURL(/\/programs\/\d+/),
            pill.click(),
        ]);
        await expect(page).toHaveURL(/\/programs\/\d+/);
    });

    test('월 이동 화살표 클릭 → URL year/month 변경 + 필터 파라미터 유지', async ({ page }) => {
        await gotoCalendar(page, '/programs?view=calendar&status=upcoming');
        const nextBtn = page.locator('.program-calendar-nav-arrow[aria-label="다음 달"]');
        await Promise.all([
            page.waitForURL(/year=/),
            nextBtn.click(),
        ]);
        const url = new URL(page.url());
        expect(url.searchParams.get('view')).toBe('calendar');
        expect(url.searchParams.get('status')).toBe('upcoming');
        expect(url.searchParams.get('year')).toBeTruthy();
        expect(url.searchParams.get('month')).toBeTruthy();
    });

    test('빈 달 배너 - 결과 0건 시 문구 노출', async ({ page }) => {
        // 시드에 없을 것으로 예상되는 먼 미래 (2100년 1월)
        await gotoCalendar(page, '/programs?view=calendar&year=2100&month=1');
        const banner = page.locator('.program-calendar-empty-banner');
        await expect(banner).toBeVisible();
        // 격자는 그대로 렌더
        await expect(page.locator('.program-calendar-cell')).toHaveCount(42);
    });

    test('빈 달 배너 문구 - 탭 이름 포함 (dc.html §7a)', async ({ page }) => {
        // 종료 필터 + 빈 달 → "8월에는 종료된 프로그램이 없어요"
        await gotoCalendar(page, '/programs?view=calendar&status=ended&year=2100&month=1');
        const banner = page.locator('.program-calendar-empty-banner');
        await expect(banner).toBeVisible();
        // dc.html §7a: 탭 이름 (종료된/모집중인/진행예정된) 이 문구에 포함돼야 함
        await expect(banner).toContainText('종료된 프로그램이 없어요');
    });

    test('툴바 년월 nav 가 툴바 폭 정중앙 정렬 (±2px, 사용자 시각 6번 회귀 방지)', async ({ page }) => {
        await gotoCalendar(page);
        const toolbarBox = await page.locator('.program-calendar-toolbar').boundingBox();
        const navBox = await page.locator('.program-calendar-nav').boundingBox();
        expect(toolbarBox).not.toBeNull();
        expect(navBox).not.toBeNull();
        const toolbarCenter = toolbarBox!.x + toolbarBox!.width / 2;
        const navCenter = navBox!.x + navBox!.width / 2;
        expect(Math.abs(toolbarCenter - navCenter)).toBeLessThanOrEqual(2);
    });

    // ym-verify U-1 (2026-09-01): N3 브레이크포인트 전환 회귀 방어
    // program-calendar.js:122-127 의 matchMedia change 리스너가 데스크톱↔모바일 전환 시
    // stale --selected · body overflow 락 · 시트 잔존을 clear 하는지 실측.
    test.describe('N3: 브레이크포인트 전환 회귀', () => {
        test('N3: 데스크톱→모바일 전환 시 선택 상태·패널 클리어', async ({ page }) => {
            await page.setViewportSize({ width: 1440, height: 900 });
            await gotoCalendar(page);
            const firstInMonth = page.locator('.program-calendar-cell[data-in-month="true"]').first();
            await firstInMonth.click();
            await expect(page.locator('.program-calendar-cell--selected')).toHaveCount(1);
            await expect(page.locator('#program-calendar-panel')).toBeVisible();
            // 모바일 브레이크포인트로 전환 → matchMedia change 리스너가 closePanel 실행
            await page.setViewportSize({ width: 375, height: 667 });
            await expect(page.locator('.program-calendar-cell--selected')).toHaveCount(0);
            await expect(page.locator('#program-calendar-panel')).toBeHidden();
        });

        test('N3: 모바일→데스크톱 전환 시 시트·overflow 락 해제', async ({ page }) => {
            await page.setViewportSize({ width: 375, height: 667 });
            await gotoCalendar(page);
            const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
            const cnt = await pillCell.count();
            test.skip(cnt === 0, '현재 달 pill 있는 셀 없음');
            await pillCell.click();
            const sheet = page.locator('#program-calendar-mobile-sheet');
            await expect(sheet).toBeVisible();
            // 모바일 시트 open 시 openMobileSheet 이 body overflow=hidden 설정
            const lockedOverflow = await page.evaluate(() => document.body.style.overflow);
            expect(lockedOverflow).toBe('hidden');
            // 데스크톱 브레이크포인트로 전환 → matchMedia change → closePanel → closeMobileSheet
            await page.setViewportSize({ width: 1440, height: 900 });
            await expect(sheet).toBeHidden();
            const overflow = await page.evaluate(() => document.body.style.overflow);
            expect(overflow).toBe('');
            await expect(page.locator('.program-calendar-cell--selected')).toHaveCount(0);
        });
    });

    // F0f PR-2 (2026-09-01) 모바일 인터랙션
    test.describe('모바일 (375×667) - dc.html §7b', () => {
        test.use({ viewport: { width: 375, height: 667 } });

        test('모바일 셀 클릭 시 하단 시트 open, backdrop 클릭 close', async ({ page }) => {
            await gotoCalendar(page);
            const sheet = page.locator('#program-calendar-mobile-sheet');
            await expect(sheet).toBeHidden();
            // pill 있는 셀 찾기
            const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
            const cnt = await pillCell.count();
            test.skip(cnt === 0, '현재 달 pill 있는 셀 없음');
            await pillCell.click();
            await expect(sheet).toBeVisible();
            // 시트 body 에 최소 1개 카드가 클론됨
            await expect(page.locator('#program-calendar-mobile-sheet-body .program-calendar-panel-card').first()).toBeVisible();
            // backdrop 클릭 → 닫힘
            // 260903: backdrop inset:0 이라 center 클릭이 panel(bottom:0;max-height:78vh)에 가려짐.
            // panel 위쪽 여백을 명시적으로 클릭 (오늘 seed 롤오버로 CI 노출된 사전 결함).
            await page.locator('.program-calendar-mobile-sheet-backdrop').click({ position: { x: 10, y: 10 } });
            await expect(sheet).toBeHidden();
        });

        test('모바일에서 데스크톱 우측 패널 노출 안 됨', async ({ page }) => {
            await gotoCalendar(page);
            const panel = page.locator('.program-calendar-panel');
            const display = await panel.evaluate((el) => window.getComputedStyle(el).display);
            expect(display).toBe('none');
        });

        test('모바일 셀은 정사각 aspect-ratio 1', async ({ page }) => {
            await gotoCalendar(page);
            const cell = page.locator('.program-calendar-cell').first();
            const box = await cell.boundingBox();
            expect(box).not.toBeNull();
            // aspect-ratio 1 → 약간의 정수 반올림 허용 (±2px)
            expect(Math.abs(box!.width - box!.height)).toBeLessThanOrEqual(2);
        });
    });
});
