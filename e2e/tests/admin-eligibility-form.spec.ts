import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * F4-admin-eligibility (2026-09-09) 기능 E2E.
 * seed program #7 = "청년 문화예술 스쿨" — 3필드 모두 채워진 상태 시작.
 * 각 테스트는 마지막에 자기 상태를 원복 → 다음 spec 격리.
 */

const PROGRAM_ID = 7;
const SEED_AGE = '만 19세 ~ 39세 청년';
const SEED_REGION = '의왕시 거주 또는 활동';
const SEED_ETC = '문화예술 입문자 대상';

async function submitForm(
    page: import('@playwright/test').Page,
    age: string,
    region: string,
    etc: string,
): Promise<void> {
    await page.locator('input#age').fill(age);
    await page.locator('input#region').fill(region);
    await page.locator('textarea#etc').fill(etc);
    await page.locator('.admin-eligibility-form-actions button[type="submit"]').click();
    await page.waitForURL(`**/admin/programs/${PROGRAM_ID}/eligibility`);
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test.afterEach(async ({ page }) => {
    // 원복 — 다음 spec 오염 방지
    await loginAdmin(page);
    await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, { waitUntil: 'domcontentloaded' });
    await submitForm(page, SEED_AGE, SEED_REGION, SEED_ETC);
});

test('편집 화면에 seed 값이 prefilled', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-eligibility-title')).toHaveText('자격요건 편집');
    await expect(page.locator('input#age')).toHaveValue(SEED_AGE);
    await expect(page.locator('input#region')).toHaveValue(SEED_REGION);
    await expect(page.locator('textarea#etc')).toHaveValue(SEED_ETC);
});

test('편집 저장 → 사용자 프로그램 상세에 반영', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, { waitUntil: 'domcontentloaded' });
    const newAge = '만 25세 ~ 34세 (E2E 편집)';
    await submitForm(page, newAge, SEED_REGION, SEED_ETC);

    // Qn-8 A PRG: flash 배너 노출
    await expect(page.locator('.admin-eligibility-flash')).toContainText('저장했어요');
    await expect(page.locator('input#age')).toHaveValue(newAge);

    // 사용자 사이드 회귀 방지: 변경된 값이 상세 페이지에 노출
    await page.goto(`/programs/${PROGRAM_ID}`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.detail-requirement-value').first()).toContainText('만 25세 ~ 34세 (E2E 편집)');
});

test('Qn-2 A: 3필드 모두 공란 저장 → 사용자 화면 기본 문구 노출', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, { waitUntil: 'domcontentloaded' });
    await submitForm(page, '', '', '');
    await expect(page.locator('.admin-eligibility-flash')).toContainText('저장했어요');

    await page.goto(`/programs/${PROGRAM_ID}`, { waitUntil: 'domcontentloaded' });
    // detail.html L184~236: 3-grid 기본 문구
    const values = page.locator('.detail-requirement-value');
    await expect(values.nth(0)).toContainText('연령 제한 없음');
    await expect(values.nth(1)).toContainText('거주지 제한 없음');
    await expect(values.nth(2)).toContainText('별도 조건 없음');
});

test('Qn-4 A: 101자 age 입력 → 400 (검증 실패)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfHeader = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';

    const overflowAge = 'A'.repeat(101);
    const response = await page.request.post(`/admin/programs/${PROGRAM_ID}/eligibility`, {
        form: { age: overflowAge, region: SEED_REGION, etc: SEED_ETC },
        headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    });
    expect(response.status()).toBe(400);
});
