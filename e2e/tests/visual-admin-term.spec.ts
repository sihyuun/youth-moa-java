import { test } from '@playwright/test';
import {
    adminTermEditContract,
    adminTermFormContract,
    adminTermListContract,
} from '../contracts/admin-term';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 약관 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminTermListContract.viewport.width,
        height: adminTermListContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminTermListContract, 'anon');
    writeGapReport(adminTermListContract, { anon });
});

test('관리자 약관 신규 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminTermFormContract.viewport.width,
        height: adminTermFormContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminTermFormContract, 'anon');
    writeGapReport(adminTermFormContract, { anon });
});

test('관리자 약관 편집 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminTermEditContract.viewport.width,
        height: adminTermEditContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminTermEditContract, 'anon');
    writeGapReport(adminTermEditContract, { anon });
});
