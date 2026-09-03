import { expect, test } from '@playwright/test';
import {
    abortExternal,
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    ADMIN_SYSTEM_EMAIL,
    loginAdmin,
    SEED_PASS,
    seedEmail,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('비인증 상태로 /admin 진입 시 /admin/login 으로 리다이렉트', async ({ page }) => {
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveURL(/\/admin\/login/);
    await expect(page.locator('.admin-auth-title')).toHaveText('관리자 로그인');
});

test('잘못된 자격증명 → /admin/login?error + username 보존', async ({ page }) => {
    await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(ADMIN_SYSTEM_EMAIL);
    await page.locator('input[name="password"]').fill('WrongPassword!');
    await page.locator('#adminLoginForm button[type="submit"]').click();
    await page.waitForURL(/\/admin\/login\?error/);
    await expect(page.locator('input[name="username"]')).toHaveValue(ADMIN_SYSTEM_EMAIL);
    await expect(page.locator('.admin-auth-alert--error')).toContainText('아이디 또는 비밀번호가 올바르지 않습니다.');
});

test('SYSTEM_ADMIN 로그인 성공 → /admin 도달 + 다크 헤더 렌더', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL, ADMIN_SEED_PASS);
    await expect(page.locator('.admin-header')).toBeVisible();
    await expect(page.locator('.admin-header-badge')).toHaveText('ADMIN');
    await expect(page.locator('.admin-header-scope-label')).toHaveText('전체');
});

test('CENTER_ADMIN 로그인 → 자기 센터명 노출', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    // 자기 센터명 (전체 아님). 정확한 센터명은 시드에 따라 달라 "전체" 만 부재로 검증.
    await expect(page.locator('.admin-header-scope-label')).not.toHaveText('전체');
    await expect(page.locator('.admin-header-scope--center')).toBeVisible();
});

test('USER 로그인 상태로 /admin 접근 시 403', async ({ page }) => {
    // 사용자 페이지로 먼저 로그인
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(seedEmail(1));
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    // 이제 /admin 접근
    const response = await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});
