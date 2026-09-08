import { test } from '@playwright/test';
import {
    adminDynamicFieldEditContract,
    adminDynamicFieldFormContract,
    adminDynamicFieldListContract,
} from '../contracts/admin-dynamic-field';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 동적 필드 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminDynamicFieldListContract.viewport.width,
        height: adminDynamicFieldListContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminDynamicFieldListContract.path);
    const anon = await runContract(page, adminDynamicFieldListContract, 'anon');
    writeGapReport(adminDynamicFieldListContract, { anon });
});

test('관리자 동적 필드 신규 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminDynamicFieldFormContract.viewport.width,
        height: adminDynamicFieldFormContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminDynamicFieldFormContract.path);
    const anon = await runContract(page, adminDynamicFieldFormContract, 'anon');
    writeGapReport(adminDynamicFieldFormContract, { anon });
});

test('관리자 동적 필드 편집 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminDynamicFieldEditContract.viewport.width,
        height: adminDynamicFieldEditContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminDynamicFieldEditContract.path);
    const anon = await runContract(page, adminDynamicFieldEditContract, 'anon');
    writeGapReport(adminDynamicFieldEditContract, { anon });
});
