import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin, resetApplyQuestions } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test.afterAll(async ({ browser }) => {
    const page = await browser.newPage();
    try {
        await loginAdmin(page);
        await resetApplyQuestions(page);
    } finally {
        await page.close();
    }
});

test('TEXT 필드 신규 등록 → 편집 화면에 값 유지', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/7/dynamic-fields/new', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-dynamic-field-title')).toHaveText('동적 필드 등록');

    await page.locator('select#fieldType').selectOption('TEXT');
    await page.locator('input#label').fill('E2E TEXT 질문');
    await page.locator('input#sortOrder').fill('10');
    await page.locator('input#maxLength').fill('300');
    await page.locator('input#isRequired').check();

    await page.locator('button[type="submit"]:has-text("등록")').click();

    await page.waitForURL(/\/admin\/programs\/7\/dynamic-fields\/\d+/);
    await expect(page.locator('.admin-dynamic-field-title')).toHaveText('동적 필드 편집');
    await expect(page.locator('input#label')).toHaveValue('E2E TEXT 질문');
    await expect(page.locator('input#maxLength')).toHaveValue('300');
});

test('DROPDOWN 필드 신규 등록 → options 한 줄에 하나 저장', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/7/dynamic-fields/new', { waitUntil: 'domcontentloaded' });

    await page.locator('select#fieldType').selectOption('DROPDOWN');
    await page.locator('input#label').fill('E2E DROPDOWN 질문');
    await page.locator('input#sortOrder').fill('11');
    await page.locator('textarea#options').fill('사과\n배\n감');
    await page.locator('button[type="submit"]:has-text("등록")').click();

    await page.waitForURL(/\/admin\/programs\/7\/dynamic-fields\/\d+/);
    // 옵션이 개행으로 역직렬화되어 textarea 에 채워짐
    const optionsText = await page.locator('textarea#options').inputValue();
    expect(optionsText.split('\n').filter(l => l)).toEqual(['사과', '배', '감']);
});

test('Qn-8 C: 비활성 처리 후 목록에서 상태 pill 비활성으로 변경 · hard delete 버튼 없음', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/7/dynamic-fields/new', { waitUntil: 'domcontentloaded' });
    await page.locator('select#fieldType').selectOption('TEXT');
    await page.locator('input#label').fill('임시 필드');
    await page.locator('input#sortOrder').fill('20');
    await page.locator('input#maxLength').fill('100');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/programs\/7\/dynamic-fields\/\d+/);

    // hard delete 버튼이 존재하지 않아야 함 (Qn-8 C)
    await expect(page.locator('.admin-dynamic-field-form-actions .admin-btn--danger:not(.admin-btn--danger-outline)')).toHaveCount(0);

    // "비활성 처리" 버튼 → 모달 → 확정
    await page.locator('.admin-dynamic-field-form-actions button:has-text("비활성 처리")').click();
    await expect(page.locator('#dynamic-field-deactivate-modal')).toBeVisible();
    await page.locator('#dynamic-field-deactivate-modal form button[type="submit"]').click();

    await page.waitForURL('**/admin/programs/7/dynamic-fields');
    // 임시 필드가 비활성 상태로 리스트에 남아 있음
    const inactiveRow = page.locator('.admin-dynamic-field-row--inactive');
    await expect(inactiveRow).toContainText('임시 필드');
});
