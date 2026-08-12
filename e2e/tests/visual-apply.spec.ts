import { test } from '@playwright/test';
import { applyContract } from '../contracts/apply';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * 프로그램 신청 폼 화면 디자인 계약 실행 (인증 필요).
 *
 * seed30 = 어떤 프로그램에도 신청 없음 (helpers 주석) → 중복 신청 방어에 걸리지 않음.
 * program id 3 = 마음건강 힐링 캠프 (ACTIVE, seed 신청 없음, apply.spec.ts 와 동일 대상).
 *
 * 진입 순서:
 *   1) helpers.login → "/" 로 이동 완료 대기
 *   2) /programs/3/apply 로 이동
 *   3) 초기 렌더 (step=1) 상태로 계약 검사
 *
 * step2·3 카드는 display:none 이라 계약(anon) 상태에서 측정 불가.
 * 필요 시 인터랙션 블록을 추가해 step 전환 후 확인 (현재 미포함, 향후 확장).
 */
test('신청 폼 화면 디자인 계약 — 로그인 상태 · step=1 초기 렌더', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: applyContract.viewport.width,
        height: applyContract.viewport.height,
    });
    await login(page, seedEmail(30));
    await page.goto(applyContract.path, { waitUntil: 'domcontentloaded' });
    const auth = await runContract(page, applyContract, 'auth');
    writeGapReport(applyContract, { auth });
});
