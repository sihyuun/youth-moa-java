/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 인터랙션 E2E.
 *
 * 5개 TC:
 *   (a) 입력 후 200ms debounce → HTMX 호출 → 드롭다운 is-open · 섹션 노출
 *   (b) 프로그램 항목 클릭 → /admin/programs/{id} 로 이동
 *   (c) × 버튼 클릭 → 입력박스 비워짐 · 드롭다운 닫힘(is-open 미부여)
 *   (d) 결과 없는 쿼리 → "검색 결과가 없어요" 안내
 *   (e) 2-round — 입력 → × → 재입력 (FAIL #1 outerHTML swap 후 id 유지 회귀 방어)
 *
 * 사용 계정: sysadmin — DataInitializer 시드 프로그램이 전역 노출됨.
 *
 * 입력 방식 주의 (FAIL #3 fix):
 *   locator.fill() 은 "input" 이벤트만 발생시키고 "keyup" 은 발생시키지 않는다.
 *   현재 header.html 의 hx-trigger="keyup changed delay:200ms" 는 keyup 에 바인딩되므로
 *   fill() 만으로는 HTMX 가 호출되지 않는다. 각 입력 뒤에 명시적으로 dispatchEvent('keyup') 를 호출한다.
 */
import { expect, test, type Page } from '@playwright/test';
import { abortExternal, loginAdmin, waitForHtmx } from '../helpers';

async function typeSearch(page: Page, text: string) {
    const input = page.locator('.admin-header-search-input');
    await input.fill(text);
    // fill 은 input 이벤트만 발생 → hx-trigger="keyup" 이 걸리지 않는다. keyup 명시.
    await input.dispatchEvent('keyup');
}

test.describe('A8-search · 관리자 헤더 글로벌 검색', () => {
    test.beforeEach(async ({ page }) => {
        await abortExternal(page);
        await loginAdmin(page);
        await waitForHtmx(page);
    });

    test('입력 debounce 후 드롭다운 열림 · 섹션 노출', async ({ page }) => {
        const input = page.locator('.admin-header-search-input');
        await expect(input).toBeVisible();

        await typeSearch(page, '청년');

        // debounce 200ms + HTMX round-trip. 드롭다운이 열림 상태로 바뀌는 것을 기다린다.
        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        // 프로그램 섹션 또는 사용자 섹션 중 하나는 노출됨
        const sections = page.locator('#admin-search-dropdown .admin-search-section');
        await expect(sections.first()).toBeVisible();
    });

    test('프로그램 항목 클릭 시 /admin/programs/{id} 로 이동', async ({ page }) => {
        await typeSearch(page, '청년');

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
        await typeSearch(page, '청년');

        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        await page.locator('.admin-header-search-clear').click();

        // 입력 비워짐 (JS onclick) + HTMX 응답 적용되어 is-open 사라짐
        const input = page.locator('.admin-header-search-input');
        await expect(input).toHaveValue('');
        await expect(dropdown).not.toHaveClass(/is-open/, { timeout: 3_000 });
    });

    test('결과 없는 쿼리는 "검색 결과가 없어요" 안내 노출', async ({ page }) => {
        await typeSearch(page, 'zzzxyznonexistquery12345');

        const dropdown = page.locator('#admin-search-dropdown');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });
        await expect(dropdown.locator('.admin-search-empty')).toHaveText('검색 결과가 없어요');
    });

    /**
     * 2-round 시나리오 — FAIL #1 회귀 방어.
     *
     * 재현 조건: hx-swap="outerHTML" 로 wrapper 가 fragment 로 교체된 후에도
     * fragment root 에 동일 id 가 유지돼야 다음 입력의 hx-target 이 계속 매치된다.
     * fragment root 에 id 가 없으면 2번째 입력부터 HTMX 가 target not found 로 실패하고
     * 드롭다운이 더 이상 갱신되지 않는다.
     */
    test('입력 → × → 재입력 — hx-swap=outerHTML 후에도 id 유지', async ({ page }) => {
        const dropdown = page.locator('#admin-search-dropdown');

        // round 1: 입력 → 열림
        await typeSearch(page, '청년');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        // × 클릭 → 닫힘
        await page.locator('.admin-header-search-clear').click();
        await expect(dropdown).not.toHaveClass(/is-open/, { timeout: 3_000 });

        // round 2: 다른 값으로 재입력 → 다시 열림.
        // (hx-trigger="changed" 는 이전 값과 다를 때만 발화하므로 다른 쿼리를 사용한다.
        //  fragment root 에 id 가 없다면 outerHTML swap 으로 wrapper 가 사라져 hx-target 매치 실패)
        await typeSearch(page, '프로그램');
        await expect(dropdown).toHaveClass(/is-open/, { timeout: 3_000 });

        // 2번째 round 에서도 섹션이 다시 노출돼야 함
        const sections = page.locator('#admin-search-dropdown .admin-search-section');
        await expect(sections.first()).toBeVisible();
    });
});
