/**
 * 프로그램 상세 화면 디자인 계약 검사.
 *
 * 목적: prototype 대비 **정량 갭** 을 사람 눈이 아니라 CI 가 잡는다.
 * 계약: e2e/contracts/program-detail.ts (수치) + docs/design-contracts/program-detail.md (구조·상태)
 *
 * 실행:
 *   cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-program-detail
 *
 * 결과 갭 리포트: e2e/gap-reports/gap-program-detail.md
 *
 * ── 상태 슬롯 매핑 ────────────────────────────────────────────────
 * 이 화면의 시각 분기는 로그인 여부가 아니라 **프로그램 상태** 다
 * (prototype L1013·L1029 의 isLoggedIn 은 클릭 동작만 바꾼다).
 * 따라서 runner 의 2개 상태 슬롯을 아래처럼 쓴다.
 *
 *   anon → 모집중(OPEN) 프로그램, 비로그인
 *   auth → 종료(ENDED) 프로그램, 로그인
 *
 * 두 상태를 **한 테스트 안에서** 검사한다. (Playwright 는 테스트 실패 시 워커를
 *  재시작하므로, 테스트를 나누면 모듈 변수로 결과를 모을 수 없어 리포트가
 *  마지막 상태로 덮어써진다 — visual-home.spec.ts 와 동일한 이유.)
 */

import { test } from '@playwright/test';
import { programDetailContract } from '../contracts/program-detail';
import { runContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

/** 시드 기준 상태별 대표 프로그램 (DataInitializer) */
const OPEN_PATH = '/programs/1';
const ENDED_PATH = '/programs/4';

/** 목표 경로로 이동해 상세 화면이 실제로 그려졌는지 확인한다 */
async function gotoDetail(page: import('@playwright/test').Page, path: string): Promise<void> {
    await page.goto(path);
    await page.waitForLoadState('domcontentloaded');
    await page.locator('.detail-container').first().waitFor({ state: 'attached' });
}

test('프로그램 상세 디자인 계약 — 모집중(비로그인)·종료(로그인)', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(programDetailContract.viewport);

    const byState: Record<string, CheckResult[]> = { anon: [], auth: [] };

    // ① 모집중(OPEN) 프로그램 · 비로그인
    await gotoDetail(page, OPEN_PATH);
    byState.anon = await runContract(page, programDetailContract, 'anon');

    // ② 종료(ENDED) 프로그램 · 로그인
    await login(page, seedEmail(1));
    await gotoDetail(page, ENDED_PATH);
    byState.auth = await runContract(page, programDetailContract, 'auth');

    console.log('\n' + writeGapReport(programDetailContract, byState));
});
