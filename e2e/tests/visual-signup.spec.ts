import { test } from '@playwright/test';
import { signupContract } from '../contracts/signup';
import { runContract, writeGapReport } from '../contracts/runner';

test('회원가입 화면 디자인 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({
        width: signupContract.viewport.width,
        height: signupContract.viewport.height,
    });
    await page.goto(signupContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, signupContract, 'anon');
    writeGapReport(signupContract, { anon });
});
