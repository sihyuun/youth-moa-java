import { expect, test } from '@playwright/test';
import { ADMIN_SYSTEM_EMAIL, abortExternal, loginAdmin } from '../helpers';

/**
 * A5 admin-users (2026-09-15) — 사용자 상세 액션 E2E.
 *
 * spec §6 요구: deactivate · reactivate · role 변경 · admin-note (Safeguard 3종 포함).
 *
 * 격리: 각 테스트 전에 `/__test__/reset-users` 로 is_active · deactivated 컬럼 · admin_note 초기화
 * (TestFixtureController · e2e profile only). 서로 다른 seed 유저를 고르지만 반복 실행 안전성 확보 목적.
 */

async function resetUsers(page: import('@playwright/test').Page) {
    const res = await page.request.post('/__test__/reset-users');
    if (res.status() !== 204) {
        throw new Error(`reset-users failed status=${res.status()}`);
    }
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    await resetUsers(page);
});

test.afterEach(async ({ page }) => {
    await resetUsers(page);
});

/** 시드 사용자 (seed30@youth-moa.test) 의 id 를 `/admin/users?q=` 로 조회. */
async function findSeedUserId(page: import('@playwright/test').Page, email: string): Promise<number> {
    await page.goto(`/admin/users?q=${encodeURIComponent(email)}`, { waitUntil: 'domcontentloaded' });
    const href = await page.locator('a.admin-user-row:not(.admin-user-row--head)').first().getAttribute('href');
    const match = href?.match(/\/admin\/users\/(\d+)/);
    if (!match) throw new Error(`could not find user id for ${email}`);
    return Number(match[1]);
}

test('차단 사유 필수 — 모달 열고 제출 → 302 redirect + 상태 변경', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed30@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    // 활성 상태 확인
    await expect(page.locator('.admin-program-header .admin-user-status-badge')).toHaveText('활성');

    // 차단 버튼 → 모달 열기
    await page.locator('.admin-user-danger-zone button.admin-btn--danger').click();
    await expect(page.locator('#deactivateModal')).toBeVisible();

    // 사유 입력 + 제출
    await page.locator('#deactivateReason').fill('e2e 자동화 테스트 차단');
    await page.locator('#deactivateModal form.admin-modal-form button[type="submit"]').click();

    await expect(page).toHaveURL(new RegExp(`/admin/users/${seedId}`));
    await expect(page.locator('.admin-program-header .admin-user-status-badge')).toHaveText('차단');
    await expect(page.locator('.admin-flash--success')).toBeVisible();
});

test('재활성화 — 차단 상태에서 재활성화 버튼 클릭 → 활성 복귀', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed30@youth-moa.test');

    // 사전 차단
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });
    await page.locator('.admin-user-danger-zone button.admin-btn--danger').click();
    await page.locator('#deactivateReason').fill('사전 차단');
    await page.locator('#deactivateModal form.admin-modal-form button[type="submit"]').click();
    await expect(page.locator('.admin-program-header .admin-user-status-badge')).toHaveText('차단');

    // 재활성화 버튼 노출 · 클릭
    // CI 회귀 방어 (2026-09-15 · Recovery #2): reload 는 CI 에서 60s timeout · 명시적 goto 로 재진입
    // (302 응답 후 form action 렌더 순간 vs Playwright 폴링 race 를 결정적으로 해소).
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });
    const reactivateForm = page.locator('.admin-user-danger-zone form[action*="/reactivate"]');
    await expect(reactivateForm).toBeVisible({ timeout: 10_000 });
    await reactivateForm.locator('button[type="submit"]').click();

    await expect(page.locator('.admin-program-header .admin-user-status-badge')).toHaveText('활성');
});

test('role 변경 — USER → CENTER_ADMIN 저장', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed30@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    // 초기 role: USER
    await expect(
        page.locator('form.admin-user-role-form input[name="role"][value="USER"]'),
    ).toBeChecked();

    // CENTER_ADMIN radio 클릭 + 저장
    await page.locator('form.admin-user-role-form input[name="role"][value="CENTER_ADMIN"]').check();
    await page.locator('form.admin-user-role-form button[type="submit"]').click();

    await expect(page).toHaveURL(new RegExp(`/admin/users/${seedId}`));
    await expect(page.locator('.admin-flash--success')).toBeVisible();
    await expect(
        page.locator('form.admin-user-role-form input[name="role"][value="CENTER_ADMIN"]'),
    ).toBeChecked();
});

test('admin-note 저장 — 인라인 textarea 저장 후 flash', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed30@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    await page.locator('textarea[name="adminNote"]').fill('E2E 로 저장한 메모');
    await page.locator('form.admin-user-note-form button[type="submit"]').click();

    await expect(page.locator('.admin-flash--success')).toContainText('관리자 메모');
    await expect(page.locator('textarea[name="adminNote"]')).toHaveValue('E2E 로 저장한 메모');
});

// ─── Safeguard 3종 (spec §2) ───

test('Safeguard 1: 자기 자신 상세에서 role radio · 차단 버튼 disabled', async ({ page }) => {
    await loginAdmin(page);
    const selfId = await findSeedUserId(page, ADMIN_SYSTEM_EMAIL);
    await page.goto(`/admin/users/${selfId}`, { waitUntil: 'domcontentloaded' });

    // role radio 모두 disabled + 저장 버튼 disabled
    const radios = page.locator('form.admin-user-role-form input[name="role"]');
    const count = await radios.count();
    for (let i = 0; i < count; i++) {
        await expect(radios.nth(i)).toBeDisabled();
    }
    await expect(page.locator('form.admin-user-role-form button[type="submit"]')).toBeDisabled();

    // 차단 버튼 disabled + hint 노출
    await expect(page.locator('.admin-user-danger-zone button.admin-btn--danger')).toBeDisabled();
    await expect(page.locator('.admin-user-danger-zone').getByText('본인 계정은 차단할 수 없어요.'))
        .toBeVisible();
});

test('Safeguard 2: 마지막 SYSTEM_ADMIN 강등 시도 — 실제 시나리오는 서비스 계층 커버', async ({ page }) => {
    // Safeguard 2 는 다른 관리자가 시도해야만 self-check 를 우회하고 실증됨.
    // E2E 에서는 sysadmin 로그인 상태에서 자기 자신 상세만 접근 가능하므로
    // Safeguard 1(자기 자신 X) 에 먼저 걸린다. Safeguard 2 정밀 검증은
    // `AdminUserServiceTest.deactivate_last_system_admin_rejected_safeguard2` /
    // `changeRole_last_system_admin_demote_rejected_safeguard2` 가 커버.
    //
    // 여기서는 차단·강등 UI 가 자기 자신 상세에서 노출되지 않음(=1차 차단) 만 재확인.
    await loginAdmin(page);
    const selfId = await findSeedUserId(page, ADMIN_SYSTEM_EMAIL);
    await page.goto(`/admin/users/${selfId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-user-danger-zone button.admin-btn--danger')).toBeDisabled();
});

test('Safeguard 3: CENTER_ADMIN 은 사용자 관리 화면 접근 403', async ({ page }) => {
    // Qn-A 이월 결정 반영: /admin/users 는 SYSTEM_ADMIN 만.
    await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill('center1@youth-moa.test');
    await page.locator('input[name="password"]').fill('Admin!234');
    await page.locator('#adminLoginForm button[type="submit"]').click();
    await page.waitForURL('**/admin');

    const res = await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    // 403 page 또는 상태
    expect([403, 401]).toContain(res?.status() ?? 0);
});
