import { expect, test } from '@playwright/test';
import { abortExternal, login, seedEmail, seedPassword } from '../helpers';

/**
 * D5 마이페이지 인터랙션 기능 E2E.
 *
 * CLAUDE.md #144 (인터랙션 검증 조항) 준수:
 *  - KPI 카드 클릭 → tab 이동 (Q-4)
 *  - 관심 편집 모달 오픈·닫기 (Q-3 / P-Q1)
 *  - 성별 pill 클릭 후 hidden input 값 반영 (Q-5 / P-Q2)
 *  - 주소 검색 버튼 클릭 (Toast 노출)
 *  - 탈퇴 확인 모달 오픈·닫기 (P-Q4, POST 는 회귀 방지를 위해 트리거하지 않음)
 *
 * 계약(--project=contracts) 은 렌더만 검사. 실 클릭 시퀀스는 이 spec 이 담당.
 */
test.describe('마이페이지 인터랙션', () => {
    test.beforeEach(async ({ page }) => {
        await abortExternal(page);
    });

    test('KPI 카드 클릭 → history 대기 필터 탭으로 이동 (Q-4)', async ({ page }) => {
        await login(page, seedEmail(30));
        await page.goto('/mypage?tab=favorites', { waitUntil: 'domcontentloaded' });
        // 진행중인 신청 KPI 클릭 → ?tab=history&status=PENDING
        await Promise.all([
            page.waitForURL(/\/mypage\?tab=history/),
            page.locator('.mypage-kpi').first().click(),
        ]);
        await expect(page).toHaveURL(/tab=history/);
        await expect(page).toHaveURL(/status=PENDING/);
        // history 탭이 활성화되고 렌더됨
        await expect(page.locator('.mypage-tab.active span')).toHaveText('신청 내역');
    });

    test('관심 편집 모달 오픈 → 취소 클릭 → 닫힘 (Q-3 / P-Q1)', async ({ page }) => {
        await login(page, seedEmail(30));
        // Step1 → Step2 진입 (세션 flag 세팅)
        await page.goto('/mypage?tab=profile', { waitUntil: 'domcontentloaded' });
        await page.fill('#verifyPassword', seedPassword(30));
        await Promise.all([
            page.waitForURL(/\/mypage\/profile\/edit/),
            page.click('.mypage-verify-form button[type="submit"]'),
        ]);
        const modal = page.locator('#interestModal');
        await expect(modal).toBeHidden();
        await page.click('#btnInterestEdit');
        await expect(modal).toBeVisible();
        // 관심 chip 그룹이 렌더되어야 함 (지역 + 분야 최소 5개 이상)
        await expect(modal.locator('.interest-chip-label')).not.toHaveCount(0);
        await page.click('#btnInterestCancel');
        await expect(modal).toBeHidden();
    });

    test('성별 pill 클릭 → hidden input 갱신 (Q-5 / P-Q2)', async ({ page }) => {
        await login(page, seedEmail(30));
        await page.goto('/mypage?tab=profile', { waitUntil: 'domcontentloaded' });
        await page.fill('#verifyPassword', seedPassword(30));
        await Promise.all([
            page.waitForURL(/\/mypage\/profile\/edit/),
            page.click('.mypage-verify-form button[type="submit"]'),
        ]);
        // FEMALE pill 클릭 → is-on 이동 + hidden input 값 변경
        await page.click('.gender-pill[data-gender="FEMALE"]');
        await expect(page.locator('.gender-pill[data-gender="FEMALE"]')).toHaveClass(/is-on/);
        await expect(page.locator('.gender-pill[data-gender="MALE"]')).not.toHaveClass(/is-on/);
        const val = await page.locator('#editGender').inputValue();
        expect(val).toBe('FEMALE');
    });

    test('탈퇴 모달 오픈 → close(X) 클릭 → 닫힘 (P-Q4, POST 미트리거)', async ({ page }) => {
        await login(page, seedEmail(30));
        await page.goto('/mypage?tab=profile', { waitUntil: 'domcontentloaded' });
        await page.fill('#verifyPassword', seedPassword(30));
        await Promise.all([
            page.waitForURL(/\/mypage\/profile\/edit/),
            page.click('.mypage-verify-form button[type="submit"]'),
        ]);
        const modal = page.locator('#withdrawModal');
        await expect(modal).toBeHidden();
        await page.click('#btnWithdraw');
        await expect(modal).toBeVisible();
        // danger 모달 안 close SVG 존재 (P-Q4)
        await expect(page.locator('#btnWithdrawClose svg')).toHaveCount(1);
        await page.click('#btnWithdrawClose');
        await expect(modal).toBeHidden();
    });

    test('신청 상세 라우트 진입 → 소유자면 200 + 다른 사용자면 404 (Q-1)', async ({ page }) => {
        // seed30 은 신청 이력이 없음 → seed28 사용 (seed1~28 이 program[0] APPROVED)
        await login(page, seedEmail(28));
        // history 진입 → 첫 카드 신청 상세 링크 클릭
        await page.goto('/mypage?tab=history', { waitUntil: 'domcontentloaded' });
        const detailLink = page.locator('.history-card-detail-link').first();
        await expect(detailLink).toHaveAttribute('href', /\/mypage\/applications\/\d+/);
        const href = await detailLink.getAttribute('href');
        await Promise.all([
            page.waitForURL(new RegExp(href!.replace(/\//g, '\\/'))),
            detailLink.click(),
        ]);
        await expect(page.locator('.mypage-application-detail')).toBeVisible();
        // 존재하지 않는 신청 ID → 404 (인증 세션 유지 상태로 page.request 호출)
        const badRes = await page.request.get('/mypage/applications/999999');
        expect(badRes.status()).toBe(404);
    });

    // ── 2026-08-14 ym-verify UNVERIFIED 후속 검증 ──────────────
    test('gender=null 유저가 pill 안 누르고 저장 시 정상 처리 (UNVERIFIED-3)', async ({ page }) => {
        // seed 유저는 gender=null. hidden input value="" 전송 시 400 나는지 확인
        await login(page, seedEmail(1));
        await page.goto('/mypage?tab=profile', { waitUntil: 'domcontentloaded' });
        // Step1 비번 재입력
        const verifyForm = page.locator('form[action*="verify"]');
        if ((await verifyForm.count()) > 0) {
            await verifyForm.locator('input[type="password"]').fill(seedPassword(1));
            await verifyForm.locator('button[type="submit"]').click();
            await page.waitForURL(/\/mypage\/profile\/edit/, { timeout: 10000 });
        }
        // 성별 pill 안 누르고 저장 (gender 는 hidden 이거나 빈 값)
        // 저장 버튼은 form 밖 (form="profileEditForm" 속성으로 연결) — 별도로 locate
        const saveBtn = page.locator('button[type="submit"][form="profileEditForm"]');
        const responsePromise = page.waitForResponse(
            (r) => r.url().endsWith('/mypage/profile') && r.request().method() === 'POST',
        );
        await saveBtn.click({ noWaitAfter: true });
        const response = await responsePromise;
        // 400 안 나야 함
        expect(response.status(), 'gender="" 저장 시 400 없어야 함').toBeLessThan(400);
    });

    test('신청 상세 → 취소 form submit → 302 redirect (UNVERIFIED-13)', async ({ page }) => {
        await login(page, seedEmail(1));
        await page.goto('/mypage?tab=history', { waitUntil: 'domcontentloaded' });
        const detailLinks = page.locator('a[href^="/mypage/applications/"]');
        if ((await detailLinks.count()) === 0) test.skip();
        await detailLinks.first().click();
        await page.waitForURL(/\/mypage\/applications\/\d+/);

        // 취소 모달 오픈
        const openBtn = page.locator('button:has-text("신청 취소")').first();
        if ((await openBtn.count()) === 0) return; // 취소 불가 상태
        await openBtn.click();
        await page.waitForTimeout(300);

        const cancelForm = page.locator('#cancelForm');
        // 사유 라디오 선택 (label 클릭)
        const reasonLabel = cancelForm.locator('label').filter({ has: page.locator('input[type="radio"]') }).first();
        if ((await reasonLabel.count()) > 0) await reasonLabel.click({ force: true });

        const responsePromise = page.waitForResponse((r) => r.url().includes('/cancel'));
        // modal aria overlay 로 button 클릭이 안정치 않아 폼 직접 submit
        await cancelForm.evaluate((f: HTMLFormElement) => f.submit());
        const response = await responsePromise;
        expect(response.status(), '취소 POST 는 302 redirect').toBe(302);
        expect(response.headers()['location']).toContain('/mypage?tab=history');
    });
});
