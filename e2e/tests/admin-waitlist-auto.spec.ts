/**
 * FOLLOW-waitlist-auto-approve (2026-10-08) — 대기자 자동 승인 토글 기능 E2E.
 *
 * 검증 포인트:
 *   1. 정원 꽉 참 상태 상세에서 노란 배너 + 토글 노출 (Q5)
 *   2. 토글 클릭 → POST /admin/programs/{id}/auto-approve → 302 → 상세 복귀
 *   3. 재진입 시 토글 상태 반영 (OFF → ON → OFF round-trip)
 *   4. CLAUDE.md 인터랙션 검증 필수 조항 (POST + redirect + DOM 변화)
 *
 * ym-impl 3차 (2026-10-08, B2' fix): QA BLOCKER — capacity=0 reject 수정.
 *   2차 수정에서 "capacity=0 으로 세팅하면 applied=0 과 (0 >= 0) 성립 → 배너 노출" 전략이었으나,
 *   서버 `AdminProgramService.validate()` 가 "모집 인원은 1명 이상이어야 합니다" 로 400 reject →
 *   폼이 /admin/programs/new 에 머물러 waitForURL 30s timeout. 자가모순이었음.
 *
 *   수정 방향 — capacity=1 로 세팅 + 생성 직후 seed 유저 1명이 신청 → applied=1 >= capacity=1 성립:
 *     a. capacity=1 로 생성 (서버 validate 통과)
 *     b. 생성된 programId 추출 후 별도 browser context 로 seed 유저 로그인 → `/programs/{id}/apply` 로 신청 1건
 *        (admin session 보존 위해 신규 context 사용)
 *     c. admin page 로 돌아와 상세 재진입 → 배너·토글 노출 확인
 */
import { expect, test } from '@playwright/test';
import {
    ADMIN_SYSTEM_EMAIL,
    SEED_PASS,
    abortExternal,
    applyProgram,
    loginAdmin,
    resetApplications,
    seedEmail,
} from '../helpers';

// 신청 fixture 로 쓸 seed 유저 — DataInitializer 규약상 seed29/30 은 어떤 프로그램에도 신청 없음 (fresh).
const APPLY_SEED_EMAIL = seedEmail(29);

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('정원 꽉 참 프로그램 상세: 배너 노출 + 토글 OFF→ON→OFF 라운드트립', async ({ page, browser }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);

    // ------------------------------------------------------------------
    // 1. 프로그램 신규 생성 (capacity=1 — 서버 validate >=1 통과)
    // ------------------------------------------------------------------
    await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });

    // (a) "프로그램 정보" 탭 명시 활성화 — 초기 default 지만 멱등 보장
    await page.locator('.admin-program-form-tab[data-tab-target="tab-info"]').click();
    await expect(page.locator('#tab-info')).not.toHaveAttribute('hidden', '');

    const uniqueTitle = `대기자 토글 E2E ${Date.now()}`;
    await page.locator('input[name="title"]').fill(uniqueTitle);

    // 센터 select — 첫 활성 센터 (option[0] 은 placeholder 가정)
    const centerSelect = page.locator('select[name="centerId"]');
    const centerOptionValue = await centerSelect.locator('option').nth(1).getAttribute('value');
    expect(centerOptionValue, '시드 센터가 1건 이상 있어야 함').not.toBeNull();
    await centerSelect.selectOption(centerOptionValue!);

    await page.locator('textarea[name="content"]').fill('e2e waitlist toggle fixture');

    // (b) "신청 정보" 탭 전환
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await expect(page.locator('#tab-apply')).not.toHaveAttribute('hidden', '');

    await page.locator('input[name="applyStartDate"]').fill('2026-01-01');
    await page.locator('input[name="applyEndDate"]').fill('2099-12-31');

    // capacity=1 — 서버 validate 통과 (최소값 1). applied=1 로 채워 조건 성립시킨다.
    await page.locator('input[name="capacity"]').fill('1');

    // (d) submit — redirect 302 + 상세 URL 이동 모두 대기
    const saveBtn = page.locator('.admin-program-form-actions button[type="submit"]');
    await Promise.all([
        page.waitForURL(/\/admin\/programs\/\d+$/, { timeout: 30_000 }),
        saveBtn.click(),
    ]);

    const detailUrl = page.url();
    const match = detailUrl.match(/\/admin\/programs\/(\d+)$/);
    expect(match, '생성 후 상세 URL 로 redirect').not.toBeNull();
    const programId = Number(match![1]);

    // ------------------------------------------------------------------
    // 1-b. seed 유저 1명이 신청 → applied=1 (capacity=1 과 동일 → 꽉참 조건 성립)
    //      admin session 보존을 위해 별도 browser context 로 격리.
    // ------------------------------------------------------------------
    const applyCtx = await browser.newContext();
    try {
        const applyPage = await applyCtx.newPage();
        await abortExternal(applyPage);
        // 재실행 시 중복 신청 방지 — fresh user 지만 reuseExistingServer 환경 대비.
        await resetApplications(applyPage, { userEmail: APPLY_SEED_EMAIL, programId });

        await applyPage.goto('/login', { waitUntil: 'commit' });
        await applyPage.locator('input[name="username"]').fill(APPLY_SEED_EMAIL);
        await applyPage.locator('input[name="password"]').fill(SEED_PASS);
        await applyPage.locator('form.auth-form-prototype button[type="submit"]').click();
        await applyPage.waitForURL('/');

        await applyProgram(applyPage, programId);
    } finally {
        await applyCtx.close();
    }

    // admin page 로 돌아와 상세 재진입 (fresh render → applied=1 반영)
    await page.goto(detailUrl, { waitUntil: 'domcontentloaded' });

    // ------------------------------------------------------------------
    // 2. 배너 + 토글 노출 확인 (capacity=0 + applied=0 → 조건 성립)
    // ------------------------------------------------------------------
    const banner = page.locator('[data-testid="waitlist-auto-banner"]');
    await expect(banner).toBeVisible();
    const toggle = page.locator('[data-testid="waitlist-auto-toggle"]');
    await expect(toggle).toBeVisible();
    // 초기 상태 OFF (aria-pressed=false)
    await expect(toggle).toHaveAttribute('aria-pressed', 'false');

    // ------------------------------------------------------------------
    // 3. 토글 OFF → ON (form submit → 302 → 상세 복귀)
    // ------------------------------------------------------------------
    const [toggleOnResp] = await Promise.all([
        page.waitForResponse(
            r => r.url().includes('/auto-approve') && r.request().method() === 'POST',
            { timeout: 15_000 },
        ),
        toggle.click(),
    ]);
    expect(toggleOnResp.status()).toBeGreaterThanOrEqual(300);
    await page.waitForURL(detailUrl);

    // 재진입 상태 ON 확인
    const toggleAfter = page.locator('[data-testid="waitlist-auto-toggle"]');
    await expect(toggleAfter).toHaveAttribute('aria-pressed', 'true');

    // ------------------------------------------------------------------
    // 4. 토글 ON → OFF
    // ------------------------------------------------------------------
    await Promise.all([
        page.waitForResponse(
            r => r.url().includes('/auto-approve') && r.request().method() === 'POST',
            { timeout: 15_000 },
        ),
        toggleAfter.click(),
    ]);
    await page.waitForURL(detailUrl);
    await expect(page.locator('[data-testid="waitlist-auto-toggle"]')).toHaveAttribute(
        'aria-pressed',
        'false',
    );
});
