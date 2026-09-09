import { test } from '@playwright/test';
import { adminEligibilityFormContract } from '../contracts/admin-eligibility';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('관리자 자격요건 편집 폼 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminEligibilityFormContract.viewport.width,
        height: adminEligibilityFormContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto(adminEligibilityFormContract.path);
    const anon = await runContract(page, adminEligibilityFormContract, 'anon');
    writeGapReport(adminEligibilityFormContract, { anon });
});
