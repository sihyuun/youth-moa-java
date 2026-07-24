import { test, expect, type Page } from '@playwright/test';
import { abortExternal, SEED_PASS, seedEmail } from '../helpers';

/**
 * 로그인·로그아웃·미인증 리다이렉트 여정.
 *
 * 이 spec 은 실패 경로 (잘못된 비밀번호) 도 다루므로 helpers.login() (waitForURL '/' 포함) 대신
 * waitForURL 없이 폼만 채우고 제출하는 로컬 helper 를 유지한다.
 */

const SEED_USER = seedEmail(30);   // 어디에도 신청 안 한 깨끗한 유저

async function fillLogin(page: Page, email: string, password: string) {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('input[name="username"]').fill(email);
    await page.locator('input[name="password"]').fill(password);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('로그인 성공 시 홈으로 리다이렉트되고 헤더에 사용자 이름이 표시된다', async ({ page }) => {
    await fillLogin(page, SEED_USER, SEED_PASS);
    await page.waitForURL('/');
    // seed30 의 name = "시드유저30" (DataInitializer 참조)
    await expect(page.locator('.header-user-name')).toContainText('시드유저30');
});

test('잘못된 비밀번호 로그인 시 /login?error 로 이동하고 username 이 보존된다', async ({ page }) => {
    await fillLogin(page, SEED_USER, 'WrongPass!');
    await page.waitForURL(/\/login\?error/);
    // savedUsername 세션 attribute → th:value 로 재렌더
    await expect(page.locator('input[name="username"]')).toHaveValue(SEED_USER);
    // Spring Security 기본 에러는 flash 로 안 실려서 alert-error 대신 URL 파라미터로만 판단
    await expect(page).toHaveURL(/error/);
});

test('로그아웃 시 /login?logout 로 이동하고 성공 alert 문구가 뜬다', async ({ page }) => {
    await fillLogin(page, SEED_USER, SEED_PASS);
    await page.waitForURL('/');
    // U-COMMON-02 (2026-07-16): hover → click 토글로 변경. 트리거 클릭 후 form 버튼 click.
    await page.locator('.header-user-trigger').click();
    await page.locator('form.header-dropdown-logout button[type="submit"]').click();
    await page.waitForURL(/\/login\?logout/);
    await expect(page.locator('.alert.alert-success')).toContainText('로그아웃되었습니다');
});

test('비인증 상태로 /programs/{id}/apply 직접 접근 시 /login 으로 리다이렉트된다', async ({ page }) => {
    // SecurityConfig: /programs/*/apply → authenticated()
    await page.goto('/programs/1/apply', { waitUntil: 'commit' });
    // Spring Security 기본은 /login 이지만 쿼리스트링(saved request) 이 붙을 수 있음
    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('form.auth-form-prototype')).toBeVisible();
});
