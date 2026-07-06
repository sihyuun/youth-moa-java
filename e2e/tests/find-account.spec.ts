import { test, expect } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * F0i 아이디/비밀번호 찾기 여정.
 *
 * 시드 유저 규약 (DataInitializer.seedUsers, F0i 이후):
 *  - seed1~seed30, phone = 010-0000-000N (N = seed 번호, N ≥ 10 은 010-0000-00NN)
 *  - name = "시드유저N", email = seedN@youth-moa.test
 *  - password (초기) = "Test1234!"
 *
 * 다른 spec 과 seed 유저 충돌 최소화를 위해 비밀번호 재설정 대상은 seed15 사용 (다른 spec 이 로그인 안 함).
 */

const FIND_TARGET_NAME = '시드유저15';
const FIND_TARGET_PHONE = '010-0000-0015';
const FIND_TARGET_EMAIL = 'seed15@youth-moa.test';
const NEW_PASSWORD = 'Reset1234!';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

// ─────────────── 아이디 찾기 (5 TC) ───────────────

test('로그인 페이지에서 "아이디 찾기" 링크 클릭 시 /find-id 로 이동한다', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('a[href="/find-id"]').first().click();
    await expect(page).toHaveURL(/\/find-id$/);
    // Stepper: 1단계 active, 2단계 비활성
    const activeSteps = page.locator('.auth-step--active');
    await expect(activeSteps).toHaveCount(1);
    await expect(activeSteps.first()).toContainText('가입정보 입력');
});

test('아이디 찾기 — 이름+휴대폰 매칭 시 마스킹된 이메일 노출', async ({ page }) => {
    await page.goto('/find-id', { waitUntil: 'commit' });
    await page.locator('input[name="name"]').fill(FIND_TARGET_NAME);
    await page.locator('input[name="phone"]').fill(FIND_TARGET_PHONE);
    await page.locator('button.btn-auth--primary').click();

    // 결과 화면: Step 2 active + 앞 3자 마스킹 이메일
    const activeSteps = page.locator('.auth-step--active');
    await expect(activeSteps.first()).toContainText('아이디 찾기');
    await expect(page.locator('.auth-result-email')).toContainText('see***@youth-moa.test');
    // 하단 CTA
    await expect(page.locator('a.btn-auth--primary')).toContainText('로그인하기');
});

test('아이디 찾기 — 미매칭 시 "계정 없음" 에러 메시지 노출', async ({ page }) => {
    await page.goto('/find-id', { waitUntil: 'commit' });
    await page.locator('input[name="name"]').fill('존재하지않는이름_xyz');
    await page.locator('input[name="phone"]').fill('01099999999');
    await page.locator('button.btn-auth--primary').click();

    // 같은 페이지 재렌더 (URL 유지) + errorMsg
    await expect(page).toHaveURL(/\/find-id$/);
    await expect(page.locator('.alert.alert-error')).toContainText('계정');
});

// ─────────────── 비밀번호 찾기 (5 TC) ───────────────

test('로그인 페이지에서 "비밀번호 찾기" 링크 클릭 시 /find-password 로 이동한다', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('a[href="/find-password"]').first().click();
    await expect(page).toHaveURL(/\/find-password$/);
    // Stepper: 1단계 "본인 확인" active
    await expect(page.locator('.auth-step--active').first()).toContainText('본인 확인');
});

test('비밀번호 찾기 — 이메일+이름+휴대폰 매칭 시 새 비밀번호 화면 진입', async ({ page }) => {
    await page.goto('/find-password', { waitUntil: 'commit' });
    await page.locator('input[name="email"]').fill(FIND_TARGET_EMAIL);
    await page.locator('input[name="name"]').fill(FIND_TARGET_NAME);
    await page.locator('input[name="phone"]').fill(FIND_TARGET_PHONE);
    await page.locator('button.btn-auth--primary').click();

    // Step 2 active
    await expect(page.locator('.auth-step--active').first()).toContainText('새 비밀번호 설정');
    // 새 비밀번호 input 2개 노출
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('input[name="passwordConfirm"]')).toBeVisible();
});

test('비밀번호 찾기 — 새 비밀번호 8자 미만 제출 시 검증 에러 노출', async ({ page }) => {
    // Step 1 통과 → Step 2 진입
    await page.goto('/find-password', { waitUntil: 'commit' });
    await page.locator('input[name="email"]').fill(FIND_TARGET_EMAIL);
    await page.locator('input[name="name"]').fill(FIND_TARGET_NAME);
    await page.locator('input[name="phone"]').fill(FIND_TARGET_PHONE);
    await page.locator('button.btn-auth--primary').click();
    await expect(page.locator('.auth-step--active').first()).toContainText('새 비밀번호');

    // 짧은 비밀번호 제출
    await page.locator('input[name="password"]').fill('Ab1');
    await page.locator('input[name="passwordConfirm"]').fill('Ab1');
    await page.locator('button.btn-auth--primary').click();

    // 필드 에러 렌더
    const body = await page.locator('body').innerText();
    expect(body).toMatch(/8자 이상|비밀번호/);
});

test('비밀번호 찾기 — 새 비밀번호 정상 재설정 후 /login?reset 성공 알림', async ({ page }) => {
    // Step 1 → Step 2 진입
    await page.goto('/find-password', { waitUntil: 'commit' });
    await page.locator('input[name="email"]').fill(FIND_TARGET_EMAIL);
    await page.locator('input[name="name"]').fill(FIND_TARGET_NAME);
    await page.locator('input[name="phone"]').fill(FIND_TARGET_PHONE);
    await page.locator('button.btn-auth--primary').click();

    // 새 비밀번호 (영문+숫자 포함 8자 이상)
    await page.locator('input[name="password"]').fill(NEW_PASSWORD);
    await page.locator('input[name="passwordConfirm"]').fill(NEW_PASSWORD);
    await page.locator('button.btn-auth--primary').click();

    // /login?reset 리다이렉트 + 성공 알림 톤 (login.html 의 `?reset` 파라미터 감지)
    await expect(page).toHaveURL(/\/login\?reset/);
});

test('변경된 비밀번호로 로그인 성공', async ({ page }) => {
    // 위 재설정 test 가 seed15 비밀번호를 NEW_PASSWORD 로 이미 바꿈 (같은 spec 파일 순차 실행)
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('input[name="username"]').fill(FIND_TARGET_EMAIL);
    await page.locator('input[name="password"]').fill(NEW_PASSWORD);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    // 헤더 사용자 이름 렌더 (인증 완료)
    await expect(page.locator('.header-user-name')).toContainText('시드유저15');
});
