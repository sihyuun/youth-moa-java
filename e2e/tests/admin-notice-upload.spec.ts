import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

/**
 * 첨부 업로드 왕복: 공지 신규 생성 → 편집 화면 → PDF 업로드 → 목록 노출 →
 * HTMX 삭제 → 목록 미노출 확인.
 *
 * A-admin-notice-attachment (2026-09-03) 기능 E2E.
 */
test('multipart PDF 업로드 → 목록 노출 → 삭제 → 미노출', async ({ page }) => {
    await loginAdmin(page);
    // 새 공지부터 생성 (기존 시드는 sysadmin 소유이나 격리를 위해 매 실행 신규 생성)
    await page.goto('/admin/notices/new', { waitUntil: 'domcontentloaded' });
    const title = `upload-target-${Date.now()}`;
    await page.locator('input[name="title"]').fill(title);
    await page.locator('textarea[name="content"]').fill('첨부 업로드 회귀 검증용');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/notices\/\d+/);

    // 편집 화면 도달. 첨부 섹션·업로드 폼 노출 확인
    await expect(page.locator('#admin-notice-attachments')).toBeVisible();
    await expect(page.locator('form.admin-notice-upload-form')).toBeVisible();

    // 1KB 더미 PDF 업로드
    const pdfBytes = Buffer.from('%PDF-1.4\n%%EOF\n', 'utf-8');
    await page.locator('input[name="file"]').setInputFiles({
        name: 'e2e-dummy.pdf',
        mimeType: 'application/pdf',
        buffer: pdfBytes,
    });
    await page.locator('form.admin-notice-upload-form button[type="submit"]').click();

    // fragment outerHTML swap 후 파일명 노출
    const item = page.locator('.admin-notice-attachment-item', { hasText: 'e2e-dummy.pdf' });
    await expect(item).toBeVisible({ timeout: 10_000 });
    await expect(item.locator('.admin-notice-attachment-name')).toHaveText('e2e-dummy.pdf');

    // 삭제 (hx-confirm 을 window.confirm 자동 승인)
    page.on('dialog', dialog => dialog.accept());
    await item.locator('button:has-text("삭제")').click();
    await expect(page.locator('.admin-notice-attachment-item', { hasText: 'e2e-dummy.pdf' })).toHaveCount(0, { timeout: 10_000 });
    await expect(page.locator('.admin-notice-attachments-empty')).toBeVisible();
});
