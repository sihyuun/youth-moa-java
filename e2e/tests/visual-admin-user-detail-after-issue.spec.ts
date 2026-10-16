import { expect, test } from '@playwright/test';
import {
    adminUserDetailAfterIssueMailFallbackContract,
    adminUserDetailAfterIssueMailSentContract,
} from '../contracts/admin-user-detail-after-issue';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * A5-1-e2e Part 1 Q3 (2026-10-16) — 관리자 사용자 상세 **발급 직후 상태** 계약 검사 러너.
 *
 * `--project=contracts` 로 실행되며 `testMatch: /visual-.*\.spec\.ts$/`.
 * 다른 계약 검사와 달리 "임의 페이지 이동 후 셀렉터 매치" 가 아니라 **발급 flow 를 완주한 뒤 리다이렉트된 상세 상태** 에서 러너를 호출한다.
 *
 *  - mail-sent variant: forceFail OFF 상태에서 발급 → 성공 배너·password 카드 비노출 확인.
 *  - mail-fallback variant: `/__test__/mail/force-fail?enabled=true` 로 강제 실패 후 발급 → 실패 배너·fallback
 *    password 카드 노출 확인. finally 로 반드시 OFF 복구.
 *
 * 반복 실행 안전성: 발급 이메일은 `Date.now()` 로 유니크화 (중복 발급 실패 회피 — H2 create-drop 내 단일 부팅 누적).
 */

function uniqueEmail(tag: string): string {
    return `visual-${tag}-${Date.now()}@youth-moa.test`;
}

const NEW_FORM = 'form[data-testid="admin-user-new-form"]';
const EMAIL_INPUT = 'input[data-testid="admin-user-new-email"]';
const NAME_INPUT = 'input[data-testid="admin-user-new-name"]';
const SUBMIT = 'button[data-testid="admin-user-new-submit"]';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('관리자 사용자 상세 — 메일 발송 성공 직후 상태 계약', async ({ page }) => {
    const email = uniqueEmail('mail-sent');
    await page.setViewportSize({
        width: adminUserDetailAfterIssueMailSentContract.viewport.width,
        height: adminUserDetailAfterIssueMailSentContract.viewport.height,
    });

    await loginAdmin(page);
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    await expect(page.locator(NEW_FORM)).toBeVisible();
    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('계약검사 성공');
    await page.locator(SUBMIT).click();

    // 302 → /admin/users/{id} 리다이렉트 완료 후 계약 검사
    await page.waitForURL(/\/admin\/users\/\d+/);

    const anon = await runContract(page, adminUserDetailAfterIssueMailSentContract, 'anon');
    writeGapReport(adminUserDetailAfterIssueMailSentContract, { anon });
});

test('관리자 사용자 상세 — 메일 발송 실패 fallback 직후 상태 계약', async ({ page, request }) => {
    const email = uniqueEmail('mail-fallback');
    await page.setViewportSize({
        width: adminUserDetailAfterIssueMailFallbackContract.viewport.width,
        height: adminUserDetailAfterIssueMailFallbackContract.viewport.height,
    });

    // 1) 강제 실패 mode ON
    let res = await request.post('/__test__/mail/force-fail?enabled=true');
    expect(res.status()).toBe(204);

    try {
        await loginAdmin(page);
        await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
        await expect(page.locator(NEW_FORM)).toBeVisible();
        await page.locator(EMAIL_INPUT).fill(email);
        await page.locator(NAME_INPUT).fill('계약검사 fallback');
        await page.locator(SUBMIT).click();

        await page.waitForURL(/\/admin\/users\/\d+/);

        const anon = await runContract(page, adminUserDetailAfterIssueMailFallbackContract, 'anon');
        writeGapReport(adminUserDetailAfterIssueMailFallbackContract, { anon });
    } finally {
        // 2) 강제 실패 mode OFF (복구) — 반드시 수행
        res = await request.post('/__test__/mail/force-fail?enabled=false');
        expect(res.status()).toBe(204);
    }
});
