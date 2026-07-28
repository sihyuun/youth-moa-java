/**
 * 홈 화면 디자인 계약 검사 (파일럿).
 *
 * 목적: prototype 대비 **정량 갭** 을 사람 눈이 아니라 CI 가 잡는다.
 * 계약: e2e/contracts/home.ts (수치) + docs/design-contracts/home.md (구조·상태)
 *
 * 실행:
 *   cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-home
 *
 * 결과 갭 리포트: e2e/gap-reports/gap-home.md
 *
 * 비로그인·로그인 두 상태를 **한 테스트 안에서** 검사한다.
 * (Playwright 는 테스트 실패 시 워커를 재시작하므로, 테스트를 나누면 모듈 변수로
 *  결과를 모을 수 없어 리포트가 마지막 상태로 덮어써진다.)
 */

import { test } from '@playwright/test';
import { homeContract } from '../contracts/home';
import { runContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

test('홈 디자인 계약 — 비로그인·로그인', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(homeContract.viewport);

    const byState: Record<string, CheckResult[]> = { anon: [], auth: [] };

    await page.goto(homeContract.path);
    await page.waitForLoadState('domcontentloaded');
    byState.anon = await runContract(page, homeContract, 'anon');

    await login(page, seedEmail(1));
    await page.goto(homeContract.path);
    await page.waitForLoadState('domcontentloaded');
    byState.auth = await runContract(page, homeContract, 'auth');

    console.log('\n' + writeGapReport(homeContract, byState));
});
