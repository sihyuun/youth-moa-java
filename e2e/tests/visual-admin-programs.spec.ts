import { test } from '@playwright/test';
import {
    adminProgramDetailContract,
    adminProgramListContract,
} from '../contracts/admin-programs';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 프로그램 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminProgramListContract.viewport.width,
        height: adminProgramListContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminProgramListContract, 'anon');
    writeGapReport(adminProgramListContract, { anon });
});

test('관리자 프로그램 상세 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminProgramDetailContract.viewport.width,
        height: adminProgramDetailContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminProgramDetailContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminProgramDetailContract, 'anon');
    writeGapReport(adminProgramDetailContract, { anon });
});
