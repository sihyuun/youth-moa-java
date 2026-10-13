import { test } from '@playwright/test';
import { adminProgramsCalendarContract } from '../contracts/admin-programs-calendar';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * D-admin-calendar (2026-10-12): 관리자 프로그램 캘린더 뷰 디자인 계약 실행.
 * POLICY P-6 (42셀 6행 고정) + Q1~Q10 결정 반영.
 */
test('관리자 프로그램 캘린더 뷰 디자인 계약 — SYSTEM_ADMIN', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminProgramsCalendarContract.viewport.width,
        height: adminProgramsCalendarContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto('/admin/programs?view=calendar', { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, adminProgramsCalendarContract, 'anon');
    writeGapReport(adminProgramsCalendarContract, { anon });
});
