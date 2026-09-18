import { test } from '@playwright/test';
import { adminUsersListContract } from '../contracts/admin-users';
import { adminUserDetailContract } from '../contracts/admin-user-detail';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * A5 admin-users (2026-09-15) — 계약 검사 스펙 (visual · project=contracts).
 *
 * `--project=contracts` 로 실행되며 `testMatch: /visual-.*\.spec\.ts$/`.
 * 갭 리포트는 `e2e/gap-reports/gap-admin-users.md` · `gap-admin-user-detail.md` 로 생성.
 */

test('관리자 사용자 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminUsersListContract.viewport.width,
        height: adminUsersListContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminUsersListContract, 'anon');
    writeGapReport(adminUsersListContract, { anon });
});

test('관리자 사용자 상세 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminUserDetailContract.viewport.width,
        height: adminUserDetailContract.viewport.height,
    });
    await loginAdmin(page);
    // seed1 유저 상세로 이동 (id 조회)
    await page.goto('/admin/users?q=seed1', { waitUntil: 'domcontentloaded' });
    // A8 (2026-09-17): row 는 <div> 로 변경 (checkbox 추가). 내부 링크로 이동
    const href = await page
        .locator('.admin-user-row:not(.admin-user-row--head) a.admin-user-row-link')
        .first()
        .getAttribute('href');
    if (!href) throw new Error('seed1 not found for admin-user-detail contract');
    await page.goto(href, { waitUntil: 'domcontentloaded' });

    const anon = await runContract(page, adminUserDetailContract, 'anon');
    writeGapReport(adminUserDetailContract, { anon });
});
