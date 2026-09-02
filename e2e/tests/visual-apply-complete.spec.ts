import { expect, test } from '@playwright/test';
import { applyCompleteContract } from '../contracts/apply-complete';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, applyNextStep, login, resetApplications, seedEmail } from '../helpers';

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
 * seed self-pollution 해소 (fix-e2e-seed-pollution, 2026-09-02):
 * 이전에는 seed 29~38 second-rotation 으로 collision 을 피했지만 `--repeat-each` 반복 실행 시 여전히
 * 같은 초에 여러 iteration 이 같은 seed 를 잡아 B형 flaky (`이미 신청한 프로그램입니다.`) 재발했음.
 * 이제는 test-only endpoint `resetApplications` 로 beforeEach 에서 신청 row 를 지우고 시작하므로
 * rotation 없이 고정 seed 를 사용해도 안전하다.
 */
const SEED_USER = seedEmail(29);

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    // /__test__/reset-applications 는 인증 없이 호출 가능 (e2e 프로파일 SecurityConfig).
    await resetApplications(page, { userEmail: SEED_USER, programId: 7 });
});

test('신청 완료 화면 디자인 계약 — 신청 제출 후 실 URL 에서 검증', async ({ page }) => {
    await page.setViewportSize({
        width: applyCompleteContract.viewport.width,
        height: applyCompleteContract.viewport.height,
    });

    await login(page, SEED_USER);
    await page.goto('/programs/7/apply', { waitUntil: 'domcontentloaded' });
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
