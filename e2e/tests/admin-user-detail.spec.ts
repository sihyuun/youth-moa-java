import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * A5 admin-users (2026-09-15) — 사용자 상세 화면 (`/admin/users/{id}`) 렌더 · 네비게이션 E2E.
 *
 * 계약(visual-admin-users detail)이 정적 렌더 검증. 여기서는 실제 브라우저에서 클릭·URL 변화 실측.
 * 액션(deactivate/reactivate/role/note) 은 별도 spec `admin-users-actions.spec.ts` 에서 수행.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

async function findSeedUserId(page: import('@playwright/test').Page, email: string): Promise<number> {
    await page.goto(`/admin/users?q=${encodeURIComponent(email)}`, { waitUntil: 'domcontentloaded' });
    const href = await page.locator('a.admin-user-row:not(.admin-user-row--head)').first().getAttribute('href');
    const match = href?.match(/\/admin\/users\/(\d+)/);
    if (!match) throw new Error(`could not find user id for ${email}`);
    return Number(match[1]);
}

test('상세 진입 → 2컬럼 그리드 · 좌 프로필 · 우 신청 이력', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed1@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    await expect(page.locator('.admin-user-detail-grid')).toBeVisible();
    await expect(page.locator('.admin-user-card')).toHaveCount(2);
    await expect(
        page.locator('.admin-user-card').first().locator('.admin-user-card-title'),
    ).toHaveText('회원 정보');
    await expect(
        page.locator('.admin-user-card').last().locator('.admin-user-card-title'),
    ).toHaveText('프로그램 신청 현황');
});

test('좌 프로필 카드 — 6개 필드 렌더 (이메일·이름·성별·생년월일·핸드폰·주소)', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed1@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-user-profile-list .admin-user-field')).toHaveCount(6);
});

test('권한 radio 3옵션 · admin-note textarea · danger zone 모두 렌더', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed1@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    await expect(page.locator('form.admin-user-role-form input[name="role"]')).toHaveCount(3);
    await expect(page.locator('form.admin-user-note-form textarea[name="adminNote"]')).toBeVisible();
    await expect(page.locator('.admin-user-danger-zone')).toBeVisible();
});

test('신청 이력 탭 4옵션 — 전체/승인/반려/취소 클릭 시 URL 반영', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed1@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    await expect(page.locator('nav.admin-user-tabs a.admin-user-tab')).toHaveCount(4);

    // "승인" 탭 클릭
    await page.locator('nav.admin-user-tabs a').filter({ hasText: '승인' }).click();
    await expect(page).toHaveURL(/tab=APPROVED/);
    await expect(page.locator('nav.admin-user-tabs a.admin-user-tab--active')).toHaveText('승인');
});

test('뒤로가기 링크 → 목록 화면 복귀', async ({ page }) => {
    await loginAdmin(page);
    const seedId = await findSeedUserId(page, 'seed1@youth-moa.test');
    await page.goto(`/admin/users/${seedId}`, { waitUntil: 'domcontentloaded' });

    await page.locator('a.admin-user-back').click();
    await expect(page).toHaveURL(/\/admin\/users(\?|$)/);
    await expect(page.locator('.admin-program-title')).toHaveText('사용자 관리');
});

test('존재하지 않는 사용자 ID → 404', async ({ page }) => {
    await loginAdmin(page);
    const res = await page.goto('/admin/users/999999', { waitUntil: 'domcontentloaded' });
    expect(res?.status()).toBe(404);
});
