/**
 * FOLLOW-admin-program-detail-readonly (2026-10-07) — 상세→편집→저장→상세 복귀 흐름.
 *
 * 핵심 검증 포인트:
 *   1. GET /admin/programs/{id} 가 read-only 상세로 렌더됨 (form 없음)
 *   2. "수정" CTA 클릭 → /admin/programs/{id}/edit 로 이동, 편집 폼 렌더
 *   3. 저장 → 302 → /admin/programs/{id} (상세로 복귀)
 *   4. ⋯ 더보기 → 삭제만 노출 (복제 미노출, Q5 이월)
 */
import { expect, test } from '@playwright/test';
import { ADMIN_SYSTEM_EMAIL, abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('상세 → 수정 CTA → 편집 폼 → 저장 → 상세 복귀', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);

    // 1. 상세 진입
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-program-detail-page')).toBeVisible();
    await expect(page.locator('.admin-program-detail-title')).toBeVisible();
    // 상세는 form 이 아님
    await expect(page.locator('form.admin-program-form')).toHaveCount(0);

    // 2. 수정 CTA 클릭 → /edit
    const editCta = page.locator('.admin-program-detail-header-actions a.admin-btn--primary', {
        hasText: '수정',
    });
    await expect(editCta).toBeVisible();
    await Promise.all([page.waitForURL(/\/admin\/programs\/1\/edit$/), editCta.click()]);
    await expect(page.locator('form.admin-program-form')).toBeVisible();
    await expect(page.locator('.admin-program-form-title')).toHaveText('프로그램 편집');

    // 브레드크럼이 "← 프로그램 상세" 로 바뀌었는지
    await expect(page.locator('.admin-program-form-breadcrumb a')).toHaveText('← 프로그램 상세');

    // 3. 저장 → 302 → /admin/programs/1 (상세 복귀)
    // seed program #1 은 applyStartDate/EndDate 가 비어 있음 → HTML5 required 로 submit 이 블록됨.
    // PR #206 learning: wide-range 날짜를 선주입해 submit 통과. ApplicationService.apply() 는
    // apply 기간을 검사하지 않으므로 다른 spec 에 영향 없음 (admin-program-form.spec.ts:138~141 전례).
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await page.locator('input[name="applyStartDate"]').fill('2026-01-01');
    await page.locator('input[name="applyEndDate"]').fill('2099-12-31');

    const saveBtn = page.locator('.admin-program-form-actions button[type="submit"]');
    await expect(saveBtn).toHaveText('저장');
    await Promise.all([page.waitForURL(/\/admin\/programs\/1$/), saveBtn.click()]);
    await expect(page.locator('.admin-program-detail-page')).toBeVisible();
});

test('상세 ⋯ 더보기 → 삭제 메뉴 노출 + 복제 미노출 (Q5 이월)', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });

    const trigger = page.locator('[data-detail-menu-trigger]');
    await expect(trigger).toBeVisible();
    await trigger.click();

    // 삭제 메뉴 하나만 노출
    await expect(page.locator('[data-testid="detail-menu-delete"]')).toBeVisible();
    await expect(page.locator('[data-testid="detail-menu-clone"]')).toHaveCount(0);
});

test('상세 하단 "신청 현황 N건 보기" → /applications 이동 (Q6)', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });

    const link = page.locator('a[data-testid="link-applications"]');
    await expect(link).toBeVisible();
    await Promise.all([
        page.waitForURL(/\/admin\/programs\/1\/applications$/),
        link.click(),
    ]);
});
