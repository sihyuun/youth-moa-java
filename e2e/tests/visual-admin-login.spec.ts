import { test } from '@playwright/test';
import { adminLoginContract } from '../contracts/admin-login';
import { runContract, writeGapReport } from '../contracts/runner';

test('관리자 로그인 화면 디자인 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({
        width: adminLoginContract.viewport.width,
        height: adminLoginContract.viewport.height,
    });
    await page.goto(adminLoginContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminLoginContract, 'anon');
    writeGapReport(adminLoginContract, { anon });
});
