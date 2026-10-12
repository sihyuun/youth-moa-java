import { expect, test, type Page } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * A5-2 admin-approval-bulk (2026-10-12) — bulk reject + 대기 전체 승인 기능 E2E.
 *
 *  - PENDING 2건 선택 → "일괄 반려" → 사유 모달 → confirm → 302 + flash + REJECTED 반영
 *  - "대기 전체 승인" 버튼 → confirm 다이얼로그 → 302 + flash + PENDING 0 건
 *
 * 시드 오염 방지: programs[2] (id=3) 만 사용. afterEach `/__test__/reset-application-status` 로 원복.
 */

test.describe.configure({ mode: 'serial' });

const APPROVE_PROGRAM_ID = 3;

const BAR = '.admin-bulk-action-bar';
const MODAL = '[data-bulk-modal]';

async function resetApplicationStatus(page: Page, programId: number): Promise<void> {
    const res = await page.request.post('/__test__/reset-application-status', {
        data: { programId },
    });
    if (res.status() !== 204) {
        throw new Error(`reset-application-status failed status=${res.status()}`);
    }
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test.afterEach(async ({ page }) => {
    await resetApplicationStatus(page, APPROVE_PROGRAM_ID);
});

test('신청 일괄 반려 — PENDING 2건 선택 → 사유 모달 → confirm → 302 + REJECTED 반영', async ({
    page,
}) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications?status=PENDING`, {
        waitUntil: 'domcontentloaded',
    });

    const boxes = page.locator(
        '.admin-application-row:not(.admin-application-row--head) input[data-bulk-checkbox]',
    );
    await boxes.nth(0).check();
    await boxes.nth(1).check();
    await expect(page.locator(`${BAR} [data-bulk-count]`)).toHaveText('2');

    await page.locator(`${BAR} [data-bulk-action="reject"]`).click();

    const modal = page.locator(MODAL);
    await expect(modal).toHaveAttribute('data-open', 'true');
    const reason = modal.locator('[data-bulk-modal-input]');
    await expect(reason).toBeVisible();
    await reason.fill('A5-2 E2E 공통 반려 사유');
    await modal.locator('[data-bulk-modal-confirm]').click();

    await page.waitForURL(new RegExp(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`));
    await expect(page.locator('[data-testid="flash-success"]')).toBeVisible();

    // REJECTED 필터에서 반려 건 반영
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications?status=REJECTED`, {
        waitUntil: 'domcontentloaded',
    });
    const rejectedRows = page.locator(
        '.admin-application-row:not(.admin-application-row--head)',
    );
    expect(await rejectedRows.count()).toBeGreaterThanOrEqual(2);
});

test('신청 일괄 반려 — 사유 공백 → alert → 요청 미발송', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications?status=PENDING`, {
        waitUntil: 'domcontentloaded',
    });

    const boxes = page.locator(
        '.admin-application-row:not(.admin-application-row--head) input[data-bulk-checkbox]',
    );
    await boxes.nth(0).check();

    // alert 창은 자동 수락 처리
    page.on('dialog', (dialog) => dialog.accept());

    await page.locator(`${BAR} [data-bulk-action="reject"]`).click();
    const modal = page.locator(MODAL);
    await expect(modal).toHaveAttribute('data-open', 'true');
    // 공백만 입력하면 confirm 불가 (alert 발생 · 모달 유지)
    await modal.locator('[data-bulk-modal-input]').fill('   ');
    await modal.locator('[data-bulk-modal-confirm]').click();
    await expect(modal).toHaveAttribute('data-open', 'true');
});

test('대기 전체 승인 — 버튼 → confirm → 302 + PENDING 0', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`, {
        waitUntil: 'domcontentloaded',
    });

    const btn = page.locator('[data-testid="approve-all-pending-btn"]');
    await expect(btn).toBeVisible();

    page.on('dialog', (dialog) => dialog.accept());
    await btn.click();

    await page.waitForURL(new RegExp(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`));
    await expect(page.locator('[data-testid="flash-success"]')).toBeVisible();

    // PENDING 필터 결과 비어 있음 (대기 신청 0)
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications?status=PENDING`, {
        waitUntil: 'domcontentloaded',
    });
    const pendingRows = page.locator(
        '.admin-application-row:not(.admin-application-row--head)',
    );
    expect(await pendingRows.count()).toBe(0);
    // 승인 처리 후 PENDING=0 이므로 버튼도 숨겨짐
    await expect(page.locator('[data-testid="approve-all-pending-btn"]')).toHaveCount(0);
});

test('성별 열 — 각 신청 행에 성별 셀 노출 (남/여/-)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`, {
        waitUntil: 'domcontentloaded',
    });

    const genderCells = page.locator('[data-testid="row-gender"]');
    const count = await genderCells.count();
    expect(count).toBeGreaterThan(0);
    for (let i = 0; i < count; i++) {
        const t = (await genderCells.nth(i).textContent())?.trim() ?? '';
        expect(['남', '여', '-']).toContain(t);
    }
});
