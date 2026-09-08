import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin, resetApplyQuestions } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

// seed-pollution 방지 (id > SEED_APPLY_QUESTION_COUNT 정리)
test.afterAll(async ({ browser }) => {
    const page = await browser.newPage();
    try {
        await loginAdmin(page);
        await resetApplyQuestions(page);
    } finally {
        await page.close();
    }
});

test('seed program #7 목록에 3필드 (TEXT · DROPDOWN · ATTACHMENT) 렌더', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/7/dynamic-fields', { waitUntil: 'domcontentloaded' });

    await expect(page.locator('.admin-dynamic-field-title')).toHaveText('동적 신청 필드 관리');
    await expect(page.locator('.admin-dynamic-field-header a.admin-btn--primary')).toContainText('신규 등록');

    const rows = page.locator('.admin-dynamic-field-row:not(.admin-dynamic-field-row--head)');
    await expect(rows).toHaveCount(3);

    // V11 시드: 지원 동기(TEXT) · 관심 강좌(DROPDOWN) · 포트폴리오(ATTACHMENT)
    await expect(page.locator('.admin-dynamic-field-type-pill').first()).toHaveText('TEXT');
});

test('seed program #7 이외 프로그램은 필드 0건 empty state', async ({ page }) => {
    await loginAdmin(page);
    // program #1 (취업역량 강화 워크숍) — dynamic field 없음
    await page.goto('/admin/programs/1/dynamic-fields', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-dynamic-field-empty')).toContainText('등록된 동적 필드가 없어요');
});
