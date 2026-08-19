import { test } from '@playwright/test';
import { privacyContract, termsContract, emailPolicyContract } from '../contracts/policy';
import { runContract, writeGapReport } from '../contracts/runner';

for (const contract of [privacyContract, termsContract, emailPolicyContract]) {
    test(`${contract.screen} 디자인 계약 — 비로그인`, async ({ page }) => {
        await page.setViewportSize({
            width: contract.viewport.width,
            height: contract.viewport.height,
        });
        await page.goto(contract.path, { waitUntil: 'domcontentloaded' });
        const anon = await runContract(page, contract, 'anon');
        writeGapReport(contract, { anon });
    });
}
