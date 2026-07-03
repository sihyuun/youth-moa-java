import { test, expect, type Page } from '@playwright/test';
import { abortExternal, waitForHtmx } from '../helpers';

/**
 * F0f — 프로그램 목록 필터 재설계 E2E.
 *
 * 사이드바 체크박스 (지역·청년센터) / 활성 칩 / 팝오버 / 정렬 / 캘린더 토글 / 빈 상태.
 * 데이터 시드: DataInitializer 가 8개 프로그램 + 30 region + 48 center 를 매 부팅마다 재생성.
 */

async function gotoPrograms(page: Page, path: string = '/programs') {
    await page.goto(path, { waitUntil: 'commit' });
    await waitForHtmx(page);
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    await gotoPrograms(page);
    await expect(page).toHaveTitle(/프로그램/);
});

test('사이드바에 featured 5개 지역 + 청년센터 + 전체보기 버튼이 보인다', async ({ page }) => {
    const regionItems = page.locator('#sidebar-region-list .sidebar-check-item');
    await expect(regionItems).toHaveCount(5);

    const centerItems = page.locator('#sidebar-center-list .sidebar-check-item');
    await expect(centerItems).toHaveCount(5);

    // 전체보기 → 버튼 2개 (지역 + 청년센터)
    await expect(page.locator('.sidebar-show-all')).toHaveCount(2);
});

test('지역 체크박스 1개를 체크하면 URL 이 갱신되고 칩이 나타난다', async ({ page }) => {
    const firstRegion = page.locator('#sidebar-region-list input[name="regions"]').first();
    const value = await firstRegion.getAttribute('value');
    await firstRegion.check();

    // htmx ajax 가 끝날 때까지 잠시 대기
    await page.waitForTimeout(500);

    await expect(page).toHaveURL(new RegExp(`regions=${encodeURIComponent(value!)}`));
    await expect(page.locator('.active-filter-chip')).toContainText(value!);
});

test('칩 × 버튼을 클릭하면 해당 값만 제거된다', async ({ page }) => {
    const firstRegion = page.locator('#sidebar-region-list input[name="regions"]').first();
    const value = await firstRegion.getAttribute('value');
    await firstRegion.check();
    await page.waitForTimeout(500);

    await page.locator('.active-filter-chip-x').first().click();
    await page.waitForTimeout(500);

    await expect(page.locator('.active-filter-chip')).toHaveCount(0);
    await expect(page).not.toHaveURL(new RegExp(`regions=${encodeURIComponent(value!)}`));
});

test('전체 초기화 버튼은 regions/centers 만 제거하고 status/sort 는 유지한다', async ({ page }) => {
    // status=active + sort=deadline + regions 1개 조합
    await gotoPrograms(page, '/programs?status=active&sort=deadline');
    const firstRegion = page.locator('#sidebar-region-list input[name="regions"]').first();
    await firstRegion.check();
    await page.waitForTimeout(500);

    await page.locator('.active-filter-reset').click();
    await page.waitForTimeout(500);

    const url = page.url();
    expect(url).toContain('status=active');
    expect(url).toContain('sort=deadline');
    expect(url).not.toContain('regions=');
});

test('전체보기 버튼을 누르면 팝오버가 열린다', async ({ page }) => {
    await page.locator('.sidebar-show-all').first().click();
    await expect(page.locator('.filter-popover[data-group="regions"]')).toBeVisible();
    // 30개 region 이 시드되므로 8개 초과 → 검색 박스 노출
    await expect(page.locator('.filter-popover[data-group="regions"] .filter-popover-search')).toBeVisible();
});

test('인기순 정렬 클릭 시 URL 에 sort=popular 가 들어간다', async ({ page }) => {
    await page.locator('.sort-link', { hasText: '인기순' }).click();
    await page.waitForLoadState('domcontentloaded');
    await expect(page).toHaveURL(/sort=popular/);
});

test('캘린더 토글 버튼은 disabled 이고 tooltip 이 있다', async ({ page }) => {
    const cal = page.locator('.view-toggle-btn--disabled');
    await expect(cal).toBeVisible();
    await expect(cal).toBeDisabled();
    await expect(cal).toHaveAttribute('title', /캘린더/);
});

test('불가능한 region 조합은 rich 빈 상태 카피 + 2 버튼을 노출한다', async ({ page }) => {
    // 시드 데이터에 절대 매칭되지 않을 region 값
    await gotoPrograms(page, '/programs?regions=존재하지않는지역_xyz');
    await expect(page.locator('.empty-state-rich .empty-title'))
        .toContainText('조건에 맞는 프로그램이 아직 없어요');
    await expect(page.locator('.empty-state-rich .empty-sub'))
        .toContainText('선택하신 필터를 줄여보거나');
    await expect(page.locator('.empty-actions button, .empty-actions a')).toHaveCount(2);
});
