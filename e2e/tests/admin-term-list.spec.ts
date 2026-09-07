import { expect, test } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    abortExternal,
    loginAdmin,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('약관 목록 렌더 — SYSTEM_ADMIN 은 시드 2건 + 신규 등록 버튼', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });

    await expect(page.locator('.admin-term-title')).toHaveText('약관 관리');
    await expect(page.locator('.admin-term-header a.admin-btn--primary')).toContainText('신규 등록');

    const rows = page.locator('.admin-term-row:not(.admin-term-row--head)');
    await expect(rows).toHaveCount(2);

    // SERVICE / PRIVACY 시드 (DataInitializer L112~131)
    await expect(page.locator('.admin-term-code-pill').first()).toHaveText('SERVICE');
});

test('약관 상세 진입 — SYSTEM_ADMIN 편집 폼', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms/1', { waitUntil: 'domcontentloaded' });

    await expect(page.locator('.admin-term-title')).toHaveText('약관 편집');
    await expect(page.locator('input#code')).toHaveValue('SERVICE');
    await expect(page.locator('input#code')).toHaveAttribute('readonly', /.*/);
    await expect(page.locator('input#bumpVersion')).toBeAttached();
});

test('CENTER_ADMIN 조회 가능 (Qn-1 B) — 신규 등록 버튼 숨김', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });

    // 목록은 200 · 렌더 성공
    await expect(page.locator('.admin-term-title')).toHaveText('약관 관리');
    // Qn-1 B: SYSTEM_ADMIN 만 신규 등록. CENTER_ADMIN 에는 버튼 없음
    await expect(page.locator('.admin-term-header a.admin-btn--primary')).toHaveCount(0);
});
