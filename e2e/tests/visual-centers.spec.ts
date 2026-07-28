/**
 * 청년센터 화면 (`/centers`) 디자인 계약 검사.
 *
 * 목적: prototype 대비 **정량 갭** 을 사람 눈이 아니라 CI 가 잡는다.
 * 계약: e2e/contracts/centers.ts (수치) + docs/design-contracts/centers.md (구조·상태머신)
 *
 * 실행:
 *   cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-centers
 *
 * 결과 갭 리포트: e2e/gap-reports/gap-centers.md
 *
 * ── 상태 배정 (홈 스펙과 다른 점) ──
 * 이 화면의 핵심 축은 로그인 여부가 아니라 **상세 패널 열림 여부** 다 (리스트 360↔240 전환,
 * 카드 full↔compact 전환, 정렬 pill 노출/숨김이 모두 여기에 걸린다).
 * runner 는 'anon'/'auth' 두 상태만 지원하므로 다음과 같이 매핑한다.
 *
 *   anon → `/centers`         비로그인 · 상세 닫힘
 *   auth → `/centers/{id}`    로그인   · 상세 열림   ← 리포트의 "로그인" 섹션
 *
 * 계약 항목 desc 에 `[상세 열림]` 접두어가 붙은 것이 auth 패스 항목이다.
 *
 * 지도 내부(마커·클러스터·인포윈도우)는 계약 대상이 아니다 — `abortExternal()` 이
 * 카카오맵 SDK 도메인을 차단해 렌더 자체가 되지 않는다. 컨테이너 존재·크기만 본다.
 *
 * 비로그인·로그인 두 상태를 **한 테스트 안에서** 검사한다.
 * (Playwright 는 테스트 실패 시 워커를 재시작하므로, 테스트를 나누면 모듈 변수로
 *  결과를 모을 수 없어 리포트가 마지막 상태로 덮어써진다.)
 */

import { expect, test } from '@playwright/test';
import { centersContract } from '../contracts/centers';
import { runContract, writeGapReport, type CheckResult } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

test('청년센터 디자인 계약 — 상세 닫힘·상세 열림', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize(centersContract.viewport);

    const byState: Record<string, CheckResult[]> = { anon: [], auth: [] };

    // ── 1차: 비로그인 + 상세 닫힘 ────────────────────────────
    await page.goto(centersContract.path);
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('.centers-layout')).toBeVisible();
    byState.anon = await runContract(page, centersContract, 'anon');

    // 상세 대상 센터: 운영시간 배지가 렌더되는 (hasSchedule=true) 첫 카드.
    // hasSchedule=false 센터를 고르면 `.centers-detail-image-badge` 가 없어 허위 갭이 난다.
    const detailHref = await page
        .locator('.center-card[data-center-has-schedule="true"]')
        .first()
        .getAttribute('href');
    expect(detailHref, '상세 검사 대상 센터 카드를 찾지 못했습니다').toBeTruthy();

    // ── 2차: 로그인 + 상세 열림 ──────────────────────────────
    await login(page, seedEmail(1));
    await page.goto(detailHref!);
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('.centers-detail-col')).toBeVisible();
    byState.auth = await runContract(page, centersContract, 'auth');

    console.log('\n' + writeGapReport(centersContract, byState));
});
