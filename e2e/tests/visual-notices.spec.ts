import { test } from '@playwright/test';
import { noticesContract } from '../contracts/notices';
import { runContract, writeGapReport } from '../contracts/runner';

test('공지사항 목록 화면 디자인 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({
        width: noticesContract.viewport.width,
        height: noticesContract.viewport.height,
    });
    await page.goto(noticesContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, noticesContract, 'anon');
    writeGapReport(noticesContract, { anon });
});
