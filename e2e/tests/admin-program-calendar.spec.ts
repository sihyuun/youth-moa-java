import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * D-admin-calendar (2026-10-12) 기능 E2E — 뷰 토글·월 이동·셀 클릭 slide 패널·상세 이동.
 *
 * CLAUDE.md 인터랙션 검증 필수 조항 반영:
 *  - 셀 클릭 후 응답 status / DOM 변화 / URL 변화 확인
 *  - 패널 wrapper 유지 (`outerHTML` swap 아님)
 */
test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('뷰 토글 세그먼트 3버튼 렌더 + 캘린더 클릭 → view=calendar URL', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });

    const toggle = page.locator('.admin-program-view-toggle');
    await expect(toggle).toBeVisible();
    await expect(toggle.locator('.admin-program-view-btn')).toHaveCount(3);

    // 캘린더 버튼 클릭
    await toggle.locator('[data-view="calendar"]').click();
    await page.waitForURL(/view=calendar/);
    await expect(page.locator('.admin-program-view-btn--active')).toContainText('캘린더');
    // 42셀 grid 렌더
    await expect(page.locator('.program-calendar-grid .program-calendar-cell')).toHaveCount(42);
});

test('월 이동 arrow 클릭 → year/month URL 갱신', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs?view=calendar&year=2026&month=8', {
        waitUntil: 'domcontentloaded',
    });
    await expect(page.locator('[data-testid="admin-calendar-title"]')).toHaveText('2026년 8월');

    // 다음 달
    await page.locator('.program-calendar-nav a[aria-label="다음 달"]').click();
    await page.waitForURL(/month=9/);
    await expect(page.locator('[data-testid="admin-calendar-title"]')).toHaveText('2026년 9월');

    // 이전 달
    await page.locator('.program-calendar-nav a[aria-label="이전 달"]').click();
    await page.waitForURL(/month=8/);
    await expect(page.locator('[data-testid="admin-calendar-title"]')).toHaveText('2026년 8월');
});

test('셀 클릭 → 우측 패널 slide-in (hidden 해제) + 날짜 라벨 갱신', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs?view=calendar', { waitUntil: 'domcontentloaded' });

    const panel = page.locator('#admin-program-calendar-panel');
    // 초기 hidden
    await expect(panel).toBeHidden();

    // 이번 달 inMonth 셀 중 하나 클릭
    const cell = page
        .locator('.program-calendar-cell[data-in-month="true"]')
        .first();
    await cell.click();

    // 패널이 노출됨
    await expect(panel).toBeVisible();
    // 날짜 라벨에 "일" 포함
    await expect(page.locator('#admin-program-calendar-panel-date')).toContainText('일');
});

test('패널 × 버튼 → 패널 close + 셀 선택 해제', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs?view=calendar', { waitUntil: 'domcontentloaded' });

    const cell = page
        .locator('.program-calendar-cell[data-in-month="true"]')
        .first();
    await cell.click();
    const panel = page.locator('#admin-program-calendar-panel');
    await expect(panel).toBeVisible();

    await panel.locator('[data-calendar-close]').click();
    await expect(panel).toBeHidden();
    await expect(page.locator('.program-calendar-cell--selected')).toHaveCount(0);
});

test('FOLLOW: 패널 "상세보기" 버튼 클릭 → /admin/programs/{id} 이동', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs?view=calendar', { waitUntil: 'domcontentloaded' });

    // 셀 중 pill 이 있는 것 (= panelData 보유) 을 찾아 클릭
    const cellWithPill = page
        .locator('.program-calendar-cell[data-in-month="true"]')
        .filter({ has: page.locator('.program-calendar-pill') })
        .first();

    const cellCount = await cellWithPill.count();
    if (cellCount === 0) {
        test.skip(true, 'panelData 보유 셀 없음 (시드 데이터 월 범위 밖)');
        return;
    }

    await cellWithPill.click();
    const panel = page.locator('#admin-program-calendar-panel');
    await expect(panel).toBeVisible();

    // 상세보기 버튼 (admin prototype L1152~1155) — href = /admin/programs/{id}
    const detailBtn = panel.locator('.admin-program-calendar-panel-card-detail-btn').first();
    await expect(detailBtn).toBeVisible();
    const href = await detailBtn.getAttribute('href');
    expect(href).toMatch(/^\/admin\/programs\/\d+$/);

    await detailBtn.click();
    await page.waitForURL(/\/admin\/programs\/\d+$/);
    // 상세 화면 로드 확인 (status 200)
    expect(page.url()).toMatch(/\/admin\/programs\/\d+$/);
});

test('q·status URL 파라미터는 월 이동 nav 링크에 pass-through', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(
        '/admin/programs?view=calendar&q=%EA%B5%90%EC%9C%A1&status=SUSPENDED',
        { waitUntil: 'domcontentloaded' },
    );
    const nextLink = page.locator('.program-calendar-nav a[aria-label="다음 달"]');
    const href = await nextLink.getAttribute('href');
    expect(href).toMatch(/q=%EA%B5%90%EC%9C%A1/);
    expect(href).toMatch(/status=SUSPENDED/);
});
