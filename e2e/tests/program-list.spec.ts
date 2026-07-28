import { test, expect, type Page } from '@playwright/test';
import { abortExternal, waitForHtmx } from '../helpers';

/**
 * F0f — 프로그램 목록 필터 (2026-07-27 batch3 재설계 반영).
 *
 * 좌측 사이드바 제거, 상단 dropdown chip 방식.
 *  - 지역·청년센터 chip (.filter-pop-chip) 클릭 → 하단 dropdown (.filter-popover) open
 *  - dropdown 내부 체크박스 체크 → [적용] 버튼 → HTMX partial swap + URL pushState
 *  - active-filter-chip / active-filter-reset 은 결과 영역 (_list-fragment) 에서 렌더
 *  - filter-reset-link 는 필터 바 좌측에 노출 (regions/centers 선택 시)
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

test('상단 필터 바에 지역 · 청년센터 dropdown chip 이 노출된다', async ({ page }) => {
    const chips = page.locator('.filter-pop-chip');
    await expect(chips).toHaveCount(2);
    await expect(chips.nth(0)).toContainText('지역');
    await expect(chips.nth(1)).toContainText('청년센터');
});

test('지역 chip 클릭 시 dropdown 팝오버가 chip 하단에 열린다', async ({ page }) => {
    await page.locator('.filter-pop-chip', { hasText: '지역' }).click();
    const pop = page.locator('.filter-popover[data-group="regions"]');
    await expect(pop).toBeVisible();
    // 8개 초과 (30개 region) → 검색 박스 노출
    await expect(pop.locator('.filter-popover-search')).toBeVisible();
});

test('지역 dropdown 에서 옵션 체크 후 적용 시 URL 이 갱신되고 chip 활성화된다', async ({ page }) => {
    await page.locator('.filter-pop-chip', { hasText: '지역' }).click();
    const pop = page.locator('.filter-popover[data-group="regions"]');
    const firstOption = pop.locator('input[name="regions"]').first();
    const value = await firstOption.getAttribute('value');
    await firstOption.check();
    await pop.locator('.btn-primary', { hasText: '적용' }).click();

    await page.waitForTimeout(500);
    await expect(page).toHaveURL(new RegExp(`regions=${encodeURIComponent(value!)}`));
    // active-filter-chip 하단 결과 영역
    await expect(page.locator('.active-filter-chip')).toContainText(value!);
    // dropdown chip 자체 is-active 표기 (재렌더 시)
    await expect(page.locator('.filter-pop-chip', { hasText: '지역' })).toHaveClass(/is-active/);
});

test('active-filter-chip × 버튼을 클릭하면 해당 값만 제거된다', async ({ page }) => {
    await page.locator('.filter-pop-chip', { hasText: '지역' }).click();
    const pop = page.locator('.filter-popover[data-group="regions"]');
    const firstOption = pop.locator('input[name="regions"]').first();
    const value = await firstOption.getAttribute('value');
    await firstOption.check();
    await pop.locator('.btn-primary', { hasText: '적용' }).click();
    await page.waitForTimeout(500);

    await page.locator('.active-filter-chip-x').first().click();
    await page.waitForTimeout(500);

    await expect(page.locator('.active-filter-chip')).toHaveCount(0);
    await expect(page).not.toHaveURL(new RegExp(`regions=${encodeURIComponent(value!)}`));
});

test('필터 바 초기화 링크는 regions/centers 만 제거하고 status/sort 는 유지한다', async ({ page }) => {
    // status=active + sort=deadline + regions 1개 조합 (URL 직접 접근)
    await gotoPrograms(page, '/programs?status=active&sort=deadline&regions=%EC%88%98%EC%9B%90%EC%8B%9C');

    const resetLink = page.locator('.filter-reset-link');
    await expect(resetLink).toBeVisible();
    await resetLink.click();
    await page.waitForLoadState('domcontentloaded');

    const url = page.url();
    expect(url).toContain('status=active');
    expect(url).toContain('sort=deadline');
    expect(url).not.toContain('regions=');
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
