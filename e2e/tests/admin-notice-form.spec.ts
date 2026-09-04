import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('신규 공지 작성 → 목록 이동 → 편집 폼에 값 유지 확인', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices/new', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-notice-title')).toHaveText('공지 등록');

    const uniqueTitle = `e2e-notice-${Date.now()}`;
    await page.locator('input[name="title"]').fill(uniqueTitle);
    await page.locator('textarea[name="content"]').fill('e2e 검증 본문입니다.');
    await page.locator('select[name="category"]').selectOption('NOTICE');
    await page.locator('button[type="submit"]:has-text("등록")').click();

    await page.waitForURL(/\/admin\/notices\/\d+/);
    await expect(page.locator('.admin-notice-title')).toHaveText('공지 편집');
    await expect(page.locator('input[name="title"]')).toHaveValue(uniqueTitle);
    await expect(page.locator('textarea[name="content"]')).toHaveValue('e2e 검증 본문입니다.');
});

test('편집 화면에서 제목 수정 → 저장 → 반영 확인', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices/new', { waitUntil: 'domcontentloaded' });
    const initial = `edit-target-${Date.now()}`;
    await page.locator('input[name="title"]').fill(initial);
    await page.locator('textarea[name="content"]').fill('본문');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/notices\/\d+/);

    const updated = `${initial}-updated`;
    await page.locator('input[name="title"]').fill(updated);
    await page.locator('button[type="submit"]:has-text("수정 저장")').click();
    await page.waitForURL(/\/admin\/notices\/\d+/);
    await expect(page.locator('input[name="title"]')).toHaveValue(updated);
});
