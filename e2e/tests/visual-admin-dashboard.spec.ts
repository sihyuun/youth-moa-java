import { test } from '@playwright/test';
import { adminDashboardContract } from '../contracts/admin-dashboard';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 대시보드 콘텐츠 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminDashboardContract.viewport.width,
        height: adminDashboardContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminDashboardContract, 'anon');
    writeGapReport(adminDashboardContract, { anon });
});
