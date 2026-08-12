import { expect, test } from '@playwright/test';
import { applyCompleteContract } from '../contracts/apply-complete';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, applyNextStep, login, seedEmail } from '../helpers';

/**
 * 신청 완료 화면 디자인 계약 실행.
 *
 * 진입 방식 특기: 완료 페이지는 직접 URL 접근이 불가능하진 않지만, 유효한 applicationId 가
 * 있어야 하고 그 신청의 소유자가 로그인 사용자여야 한다 (다른 유저 신청 접근 시 404).
 *
 * 계약 실행 절차:
 *   1) seed 29~48 풀에서 rotation idx 로 로그인 (bootRun 재기동 없이 반복 실행 가능하도록)
 *   2) /programs/7/apply → 3단계 위저드 통과 → 제출
 *   3) redirect:/apply/complete?applicationId={saved.id} URL 을 capture
 *   4) 그 URL 에서 계약 실행 (contract path 는 placeholder 라 실제 URL 로 override)
 *
 * program 7 = 청년 문화예술 스쿨 (today-7 ~ today+30, ACTIVE, capacity 40, 시드 신청 없음)
 * program 4 는 CLOSED 라 신청 거부 → 다른 program 사용 불가.
 *
 * seed rotation (2026-08-12): 로컬 반복 실행 시 seed29 가 이미 신청 상태라 실패하던 이슈 해소.
 * DataInitializer 50 유저 확장 + 초(second) 단위 rotation 으로 seed29~48 순환 사용.
 * CI 는 매번 fresh H2 라 어차피 신선하지만, 로컬 다중 실행 방어 목적.
 * 같은 초에 반복 실행하면 여전히 충돌하지만 인간이 그렇게 빠르게 재실행할 일은 없다.
 */
test('신청 완료 화면 디자인 계약 — 신청 제출 후 실 URL 에서 검증', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: applyCompleteContract.viewport.width,
        height: applyCompleteContract.viewport.height,
    });

    // 로그인 + 신청 폼 제출로 실 applicationId 확보 (seed 29~48 second-rotation)
    const seedIdx = 29 + (Math.floor(Date.now() / 1000) % 20);
    await login(page, seedEmail(seedIdx));
    await page.goto('/programs/7/apply', { waitUntil: 'commit' });
    await applyNextStep(page, 2);
    await page.locator('#applyReason').fill('디자인 계약 실행용 지원 동기.');
    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();
    await page.waitForURL(/\/apply\/complete\?applicationId=\d+/);

    // 진입한 완료 URL 에서 계약 검사
    await expect(page).toHaveURL(/\/apply\/complete\?applicationId=\d+/);
    const auth = await runContract(page, applyCompleteContract, 'auth');
    writeGapReport(applyCompleteContract, { auth });
});
