import { test } from '@playwright/test';
import { adminSearchContract } from '../contracts/admin-search';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 헤더 글로벌 검색 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminSearchContract.viewport.width,
        height: adminSearchContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminSearchContract, 'anon');
    writeGapReport(adminSearchContract, { anon });
});
