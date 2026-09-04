import { test } from '@playwright/test';
import {
    adminNoticeEditContract,
    adminNoticeFormContract,
    adminNoticeListContract,
} from '../contracts/admin-notice';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 공지 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminNoticeListContract.viewport.width,
        height: adminNoticeListContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminNoticeListContract, 'anon');
    writeGapReport(adminNoticeListContract, { anon });
});

test('관리자 공지 신규 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminNoticeFormContract.viewport.width,
        height: adminNoticeFormContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminNoticeFormContract, 'anon');
    writeGapReport(adminNoticeFormContract, { anon });
});

test('관리자 공지 편집 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminNoticeEditContract.viewport.width,
        height: adminNoticeEditContract.viewport.height,
    });
    await loginAdmin(page);
    const anon = await runContract(page, adminNoticeEditContract, 'anon');
    writeGapReport(adminNoticeEditContract, { anon });
});
