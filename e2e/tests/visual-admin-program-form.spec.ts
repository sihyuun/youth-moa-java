import { test } from '@playwright/test';
import {
    adminProgramEditContract,
    adminProgramFormNewContract,
} from '../contracts/admin-program-form';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 프로그램 신규 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminProgramFormNewContract.viewport.width,
        height: adminProgramFormNewContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminProgramFormNewContract.path);
    const anon = await runContract(page, adminProgramFormNewContract, 'anon');
    writeGapReport(adminProgramFormNewContract, { anon });
});

test('관리자 프로그램 편집 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminProgramEditContract.viewport.width,
        height: adminProgramEditContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminProgramEditContract.path);
    const anon = await runContract(page, adminProgramEditContract, 'anon');
    writeGapReport(adminProgramEditContract, { anon });
});
