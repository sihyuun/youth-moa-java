import { test, expect } from '@playwright/test';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * D1b: 신청 완료 페이지 (/apply/complete?applicationId=...)
 *
 * 백로그 상단 항목 — D1b 머지 (PR #39) 이후 착수.
 *
 * 커버 시나리오:
 *  1) 신청 제출 → redirect → 완료 페이지 렌더링 (성공 아이콘·요약 카드·CTA)
 *  2) 존재하지 않는 applicationId → 404
 *
 * 다른 유저의 applicationId 접근 → 404 는 ApplicationCompleteControllerTest (WebMvcTest) 가 이미 커버.
 * 여기서는 E2E 관점의 사용자 여정 (실 폼 제출 → 실 redirect → 실 페이지 렌더) 을 검증한다.
 */

// seed29: 어떤 프로그램에도 신청 안 함 (DataInitializer seed loop 상 seed1~seed28만 신청)
const FRESH_USER = seedEmail(29);
// programs[0..2] 만 신청 시드 있음. programs[3] (id=4) 은 아무 유저도 신청 안 함 → 중복 신청 걱정 없음
const FRESH_PROGRAM_ID = 4;

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('신청 제출 후 완료 페이지가 렌더된다 (성공 아이콘·요약 카드·CTA)', async ({ page }) => {
    await login(page, FRESH_USER);

    // 신청 폼 → 성공 제출
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });
    await page.locator('#applyReason').fill('E2E 완료 페이지 검증용 지원 동기 문장입니다.');
    // apply.html: 실제 input 은 opacity:0 + pointer-events:none (custom UI). Playwright actionable 대기 우회.
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('button.apply-submit-btn').click();

    // ApplicationController.apply() → "redirect:/apply/complete?applicationId=" + saved.getId()
    await page.waitForURL(/\/apply\/complete\?applicationId=\d+/);

    // h1 (autofocus 앵커) — 완료 문구
    await expect(page.locator('h1.apply-complete-title')).toContainText('프로그램 신청이 완료되었습니다');

    // 성공 아이콘 aria-label
    await expect(page.locator('.apply-complete-icon')).toHaveAttribute('aria-label', '신청 완료');

    // 요약 카드: 승인 대기 뱃지 + 프로그램 제목 + #A{id} + 신청일시
    const card = page.locator('.apply-complete-card');
    await expect(card).toContainText('승인 대기');
    await expect(card.locator('.apply-complete-program-title')).not.toBeEmpty();
    await expect(card.locator('.apply-complete-appno')).toHaveText(/^#A\d+$/);
    await expect(card.locator('.apply-complete-applied-at')).toContainText('신청일시');

    // CTA 2개 (ghost = 홈, primary = 마이페이지)
    await expect(page.locator('.apply-complete-btn--ghost')).toContainText('홈으로');
    await expect(page.locator('.apply-complete-btn--primary')).toContainText('내 신청 현황 보기');
});

test('존재하지 않는 applicationId 로 접근 시 404', async ({ page }) => {
    await login(page, FRESH_USER);
    // ResponseStatusException(NOT_FOUND) — 실 응답 상태 코드 검증
    const resp = await page.goto('/apply/complete?applicationId=999999');
    expect(resp?.status()).toBe(404);
});
