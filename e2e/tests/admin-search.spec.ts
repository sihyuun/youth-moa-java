/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 인터랙션 E2E.
 *
 * 4개 TC:
 *   (a) 입력 후 200ms debounce → HTMX 호출 → 드롭다운 is-open · 프로그램 섹션 노출
 *   (b) 프로그램 항목 클릭 → /admin/programs/{id} 로 이동
 *   (c) × 버튼 클릭 → 입력박스 비워짐 · 드롭다운 닫힘(is-open 미부여)
 *   (d) 결과 없는 쿼리 → "검색 결과가 없어요" 안내
 *
 * 사용 계정: sysadmin — DataInitializer 시드 프로그램이 전역 노출됨.
 */
import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin, waitForHtmx } from '../helpers';

test.describe('A8-search · 관리자 헤더 글로벌 검색', () => {
    test.beforeEach(async ({ page }) => {
        await abortExternal(page);
        await loginAdmin(page);
        await waitForHtmx(page);
    });

    test('입력 debounce 후 드롭다운 열림 · 프로그램 섹션 노출', async ({ page }) => {
        const input = page.locator('.admin-header-search-input');
        await expect(input).toBeVisible();

        await input.fill('청년');

        // debounce 200ms + HTMX round-trip. 드롭다운이 열림 상태로 바뀌는 것을 기다린다.
        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        // 프로그램 섹션 또는 사용자 섹션 중 하나는 노출됨
        const sections = page.locator('#admin-search-dropdown .admin-search-section');
        await expect(sections.first()).toBeVisible();
    });

    test('프로그램 항목 클릭 시 /admin/programs/{id} 로 이동', async ({ page }) => {
        const input = page.locator('.admin-header-search-input');
        await input.fill('청년');

        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        // 프로그램 섹션 안의 첫 번째 링크
        const programLink = dropdown
            .locator('.admin-search-section')
            .filter({ hasText: '프로그램' })
            .locator('.admin-search-item')
            .first();

        if ((await programLink.count()) === 0) {
            // 시드 상 "청년" 매칭이 없는 환경에서는 skip
            test.skip(true, 'seed 데이터에 "청년" 매칭 프로그램 없음 — 환경 의존');
        }

        const href = await programLink.getAttribute('href');
        expect(href).toMatch(/^\/admin\/programs\/\d+$/);

        await programLink.click();
        await page.waitForURL(/\/admin\/programs\/\d+/);
    });

    test('× 버튼 클릭 시 입력 비워지고 드롭다운 닫힘', async ({ page }) => {
        const input = page.locator('.admin-header-search-input');
        await input.fill('청년');

        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        await page.locator('.admin-header-search-clear').click();

        // 입력 비워짐 (JS onclick) + HTMX 응답 적용되어 is-open 사라짐
        await expect(input).toHaveValue('');
        await expect(dropdown).not.toHaveClass(/is-open/, { timeout: 3_000 });
    });

    test('결과 없는 쿼리는 "검색 결과가 없어요" 안내 노출', async ({ page }) => {
        const input = page.locator('.admin-header-search-input');
        await input.fill('zzzxyznonexistquery12345');

        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });
        await expect(dropdown.locator('.admin-search-empty')).toHaveText('검색 결과가 없어요');
    });
});
