/**
 * 프로그램 캘린더 뷰 (`/programs?view=calendar`) 디자인 계약 검사.
 *
 * 정본: docs/00_assets/Program Calendar.dc.html
 * 계약: e2e/contracts/programs-calendar.ts + docs/design-contracts/programs.md §5-A ~ §5-I
 *
 * 실행:
 *   cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-programs-calendar
 *
 * 결과 갭 리포트: e2e/gap-reports/gap-programs-calendar.md
 *
 * 캘린더 뷰는 로그인 상태와 무관하게 동일 렌더 (즐겨찾기 인디케이터 없음) → anon 상태만 검사.
 */

import { test } from '@playwright/test';
import {
    programsCalendarContract,
    programsCalendarEmptyContract,
} from '../contracts/programs-calendar';
import { runContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal } from '../helpers';

test('프로그램 캘린더 뷰 디자인 계약', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(programsCalendarContract.viewport);

    const byState: Record<string, CheckResult[]> = { anon: [] };

    await page.goto(programsCalendarContract.path);
    await page.waitForLoadState('domcontentloaded');
    byState.anon = await runContract(page, programsCalendarContract, 'anon');

    console.log('\n' + writeGapReport(programsCalendarContract, byState));
});

test('프로그램 캘린더 뷰 — 빈 달 배너 (§5-E)', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(programsCalendarEmptyContract.viewport);

    const byState: Record<string, CheckResult[]> = { anon: [] };

    await page.goto(programsCalendarEmptyContract.path);
    await page.waitForLoadState('domcontentloaded');
    byState.anon = await runContract(page, programsCalendarEmptyContract, 'anon');

    console.log('\n' + writeGapReport(programsCalendarEmptyContract, byState));
});
