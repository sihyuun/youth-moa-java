import { test } from '@playwright/test';
import {
    adminNoticeEditContract,
    adminNoticeFormContract,
    adminNoticeListContract,
} from '../contracts/admin-notice';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

// A9-b verify #9 사후 fix (2026-09-23): loginAdmin 후 contract.path 로 명시적 이동 필수.
// runContract 는 goto 를 수행하지 않으므로 (runner 주석 명시), 호출자가 목표 경로로 이동시켜 둔 상태여야 한다.
// 기존에는 loginAdmin 이 남긴 /admin 대시보드 상태에서 contract 실행 → 모든 checks "(요소 없음)" 반환.

test('관리자 공지 목록 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminNoticeListContract.viewport.width,
        height: adminNoticeListContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminNoticeListContract.path, { waitUntil: 'domcontentloaded' });
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
    await page.goto(adminNoticeFormContract.path, { waitUntil: 'domcontentloaded' });
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
    await page.goto(adminNoticeEditContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminNoticeEditContract, 'anon');
    writeGapReport(adminNoticeEditContract, { anon });
});
