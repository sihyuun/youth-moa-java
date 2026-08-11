import { test } from '@playwright/test';
import { noticeDetailContract } from '../contracts/notice-detail';
import { runContract, writeGapReport } from '../contracts/runner';

test('공지사항 상세 화면 디자인 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({
        width: noticeDetailContract.viewport.width,
        height: noticeDetailContract.viewport.height,
    });
    await page.goto(noticeDetailContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, noticeDetailContract, 'anon');
    writeGapReport(noticeDetailContract, { anon });
});
