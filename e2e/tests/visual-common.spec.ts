/**
 * 공통 헤더·푸터 디자인 계약 검사.
 *
 * 왜 별도 스펙인가: 헤더·푸터는 13개 사용자 화면 전부에 렌더된다.
 * 여기 갭 1건 = 전 화면 갭 1건이므로 화면별 계약보다 우선순위가 높다.
 *
 * 계약:
 *   - e2e/contracts/common.ts  (수치)
 *   - docs/design-contracts/common.md (전환 애니메이션·드롭다운 열림·hover 등 판단 영역)
 *
 * 실행:
 *   cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-common --reporter=list
 *
 * 결과 갭 리포트:
 *   e2e/gap-reports/gap-common.md       (`/programs` — 일반 헤더 + 푸터)
 *   e2e/gap-reports/gap-common-home.md  (`/` — transparent 헤더)
 *
 * 4개 조합(경로 2 × 로그인 2)을 **한 테스트 안에서** 돌린다.
 * (Playwright 는 테스트 실패 시 워커를 재시작하므로, 나누면 로그인 세션과 결과 수집이 끊긴다.
 *  soft assertion 은 테스트 종료 시점에 한 번에 던져지므로 리포트 쓰기는 항상 선행된다.)
 */

import { test } from '@playwright/test';
import { commonContract, commonHomeContract } from '../contracts/common';
import { runContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

test('공통 헤더·푸터 디자인 계약 — 비로그인·로그인 / 일반·transparent', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(commonContract.viewport);

    const base: Record<string, CheckResult[]> = { anon: [], auth: [] };
    const home: Record<string, CheckResult[]> = { anon: [], auth: [] };

    // ── 비로그인 ────────────────────────────────────────────
    await page.goto(commonContract.path);
    await page.waitForLoadState('domcontentloaded');
    base.anon = await runContract(page, commonContract, 'anon');

    await page.goto(commonHomeContract.path);
    await page.waitForLoadState('domcontentloaded');
    home.anon = await runContract(page, commonHomeContract, 'anon');

    // ── 로그인 ──────────────────────────────────────────────
    await login(page, seedEmail(1));

    await page.goto(commonContract.path);
    await page.waitForLoadState('domcontentloaded');
    base.auth = await runContract(page, commonContract, 'auth');

    // transparent 헤더는 스크롤 0 에서만 성립 (header-scroll.js THRESHOLD 60)
    await page.goto(commonHomeContract.path);
    await page.waitForLoadState('domcontentloaded');
    await page.evaluate(() => window.scrollTo(0, 0));
    home.auth = await runContract(page, commonHomeContract, 'auth');

    console.log('\n' + writeGapReport(commonContract, base));
    console.log('\n' + writeGapReport(commonHomeContract, home));
});
