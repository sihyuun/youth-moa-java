/**
 * FOLLOW-program-duplicate (2026-10-08 · Q1~Q10 모두 권장안) — 상세 → 복제 → 편집폼 redirect.
 *
 * 핵심 검증:
 *   1. ⋯ 더보기 → "복제" 클릭 시 POST /admin/programs/{id}/duplicate
 *   2. 302 → /admin/programs/{newId}/edit (Q5)
 *   3. 편집 폼에서 title 에 "(복제본)" 접미사가 붙어 있음 (Q2)
 *   4. 복제본 isActive=false (Q3) — "활성 상태" 체크박스 off
 */
import { expect, test } from '@playwright/test';
import { ADMIN_SYSTEM_EMAIL, abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('상세 ⋯ 더보기 → 복제 → 복제본 편집폼 redirect + 제목 "(복제본)" + isActive=false', async ({
    page,
}) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });

    // 원본 제목 확보
    const originalTitle = await page.locator('.admin-program-detail-title').innerText();
    expect(originalTitle.length).toBeGreaterThan(0);

    // 더보기 메뉴 열기
    const trigger = page.locator('[data-detail-menu-trigger]');
    await trigger.click();

    const cloneBtn = page.locator('[data-testid="detail-menu-clone"]');
    await expect(cloneBtn).toBeVisible();

    // 복제 클릭 → /edit redirect (Q5)
    await Promise.all([
        page.waitForURL(/\/admin\/programs\/\d+\/edit$/),
        cloneBtn.click(),
    ]);

    // 편집 폼 렌더 확인
    await expect(page.locator('form.admin-program-form')).toBeVisible();

    // Q2: 제목에 접미사 포함
    const titleInput = page.locator('input[name="title"]');
    const copyTitle = await titleInput.inputValue();
    expect(copyTitle).toBe(`${originalTitle} (복제본)`);

    // Q3: isActive=false — 활성 상태 체크박스가 off
    //    AdminProgram form 은 "활성 상태" checkbox name="active"
    const activeCheckbox = page.locator('input[type="checkbox"][name="active"]');
    await expect(activeCheckbox).toHaveCount(1);
    await expect(activeCheckbox).not.toBeChecked();
});
