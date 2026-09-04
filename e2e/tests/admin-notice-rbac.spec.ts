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

test('USER 계정으로 /admin/notices 접근 시 403', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(seedEmail(1));
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    const response = await page.goto('/admin/notices', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('CENTER_ADMIN 이 SYSTEM_ADMIN 소유 공지 편집 POST → 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    // 시드 공지 id=1 은 sysadmin 소유. CENTER_ADMIN 은 read 가능하지만 update 는 불가.
    await page.goto('/admin/notices/1', { waitUntil: 'domcontentloaded' });
    // 편집 폼 자체는 render (canEdit=false 배너)
    await expect(page.locator('.admin-notice-forbidden-banner')).toBeVisible();

    // 폼을 강제 submit — CSRF 는 이미 페이지에 부착됨. 서버는 서비스 계층 assertCanEdit 에서 403 반환.
    const csrf = await page.locator('input[name="_csrf"]').first().inputValue();
    const response = await page.request.post('/admin/notices/1', {
        form: {
            title: 'force-update',
            content: 'body',
            _csrf: csrf,
        },
    });
    expect(response.status()).toBe(403);
});

test('CENTER_ADMIN 이 본인 작성 공지 편집 → 성공', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    await page.goto('/admin/notices/new', { waitUntil: 'domcontentloaded' });
    const t = `center-own-${Date.now()}`;
    await page.locator('input[name="title"]').fill(t);
    await page.locator('textarea[name="content"]').fill('본인 공지');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/notices\/\d+/);
    // 편집 폼에 forbidden-banner 없어야 함
    await expect(page.locator('.admin-notice-forbidden-banner')).toHaveCount(0);
    // 저장 버튼도 활성 — 수정 성공
    const updated = `${t}-upd`;
    await page.locator('input[name="title"]').fill(updated);
    await page.locator('button[type="submit"]:has-text("수정 저장")').click();
    await page.waitForURL(/\/admin\/notices\/\d+/);
    await expect(page.locator('input[name="title"]')).toHaveValue(updated);
});

test('SYSTEM_ADMIN 은 목록·신규·편집 모두 접근 200', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL, ADMIN_SEED_PASS);
    let response = await page.goto('/admin/notices/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
    response = await page.goto('/admin/notices/1', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
    // NOTE: 목록 (/admin/notices) 은 현재 LazyInitializationException 회귀로 500 발생 —
    //       이 spec 은 회귀 fix 후 제거해야 할 임시 skip 이다. 회귀 사실을 박제하기 위해 여기서
    //       toBe(200) 로 두면 fix 전까지 실패한다 (의도된 red).
    response = await page.goto('/admin/notices', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
});
