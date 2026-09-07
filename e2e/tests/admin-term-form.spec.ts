import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin, resetTerms } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

// seed-pollution 방지: 이 spec 이 생성한 임시 약관을 정리해 signup 회귀와 admin-term-list count 오염을 차단.
test.afterAll(async ({ browser }) => {
    const page = await browser.newPage();
    try {
        await loginAdmin(page);
        await resetTerms(page);
    } finally {
        await page.close();
    }
});

test('신규 약관 등록 → 편집 폼에 값 유지', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-term-title')).toHaveText('약관 등록');

    // A-Z_ 만 허용 (validateCode). 유니크 code 생성.
    const uniqueSuffix = String(Date.now()).replace(/\D/g, '').slice(-6);
    const code = `E2E_${uniqueSuffix}`;
    await page.locator('input[name="code"]').fill(code);
    await page.locator('input[name="title"]').fill('E2E 테스트 약관');
    await page.locator('input[name="contentPath"]').fill('/terms');
    await page.locator('textarea[name="content"]').fill('<p>E2E 검증용 본문입니다.</p>');
    await page.locator('input[name="sortOrder"]').fill('10');
    await page.locator('input[name="isActive"]').check();

    await page.locator('button[type="submit"]:has-text("등록")').click();

    await page.waitForURL(/\/admin\/terms\/\d+/);
    await expect(page.locator('.admin-term-title')).toHaveText('약관 편집');
    await expect(page.locator('input[name="code"]')).toHaveValue(code);
    await expect(page.locator('input[name="title"]')).toHaveValue('E2E 테스트 약관');
});

test('편집에서 bumpVersion 체크 시 version 증가 (Qn-6 A)', async ({ page }) => {
    await loginAdmin(page);
    // 1) 새 약관 만들기 (버전=1)
    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const code = `BUMP_${String(Date.now()).replace(/\D/g, '').slice(-6)}`;
    await page.locator('input[name="code"]').fill(code);
    await page.locator('input[name="title"]').fill('버전 테스트');
    await page.locator('input[name="contentPath"]').fill('/terms');
    await page.locator('textarea[name="content"]').fill('<p>초안</p>');
    await page.locator('input[name="sortOrder"]').fill('20');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);

    // "현재 버전: v1" 렌더 확인
    await expect(page.locator('.admin-term-form-help').filter({ hasText: '현재 버전' })).toContainText('v1');

    // 2) bumpVersion 체크 후 저장
    await page.locator('input#bumpVersion').check();
    await page.locator('textarea[name="content"]').fill('<p>개정본</p>');
    await page.locator('button[type="submit"]:has-text("수정 저장")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);

    // 버전 v2 로 증가
    await expect(page.locator('.admin-term-form-help').filter({ hasText: '현재 버전' })).toContainText('v2');
});

test('편집에서 bumpVersion 미체크 시 version 유지', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const code = `KEEP_${String(Date.now()).replace(/\D/g, '').slice(-6)}`;
    await page.locator('input[name="code"]').fill(code);
    await page.locator('input[name="title"]').fill('버전 유지');
    await page.locator('input[name="contentPath"]').fill('/terms');
    await page.locator('textarea[name="content"]').fill('<p>x</p>');
    await page.locator('input[name="sortOrder"]').fill('30');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);

    // bumpVersion 미체크 상태로 저장
    await page.locator('input[name="title"]').fill('버전 유지 (오타수정)');
    await page.locator('button[type="submit"]:has-text("수정 저장")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);

    // 버전 v1 유지
    await expect(page.locator('.admin-term-form-help').filter({ hasText: '현재 버전' })).toContainText('v1');
});
