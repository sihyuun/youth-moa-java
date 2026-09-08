import { expect, test } from '@playwright/test';
import { abortExternal, applyNextStep, login, resetApplications, seedEmail } from '../helpers';

/**
 * F0c-dynamic-fields (2026-09-08): 사용자 신청 폼에 관리자 동적 필드가 노출되고 응답이 저장되는지 검증.
 *
 * 시나리오:
 *  - seed30 (fresh) → seed program #7 (친환경 도시농부 프로젝트, V11 시드 3필드)
 *  - Step 2 에 지원 동기 · 관심 강좌 · 포트폴리오 (파일 optional) 3필드 노출 확인
 *  - 값 입력 후 제출 → complete 페이지 도달
 *  - required 미입력 시 서버 400 → flash 에러
 */

const USER = seedEmail(30);
const PROGRAM_ID = 7;

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    // fresh state 보장 — seed30 이 program #7 재신청 시 중복 방지
    await page.request.post('/__test__/reset-applications', {
        data: { userEmail: USER, programId: PROGRAM_ID },
    });
});

test('seed program #7 apply 화면 Step 2 에 동적 필드 3건 렌더', async ({ page }) => {
    await login(page, USER);
    await page.goto(`/programs/${PROGRAM_ID}/apply`, { waitUntil: 'domcontentloaded' });
    await applyNextStep(page, 2);

    const dynFields = page.locator('.apply-dynamic-field');
    await expect(dynFields).toHaveCount(3);
    await expect(page.locator('.apply-dynamic-field').first()).toContainText('지원 동기');
    // 두 번째는 DROPDOWN
    await expect(page.locator('.apply-dynamic-select')).toBeVisible();
});

test('동적 필드 응답 포함하여 신청 성공 → 완료 페이지 도달', async ({ page }) => {
    await login(page, USER);
    await page.goto(`/programs/${PROGRAM_ID}/apply`, { waitUntil: 'domcontentloaded' });

    await applyNextStep(page, 2);
    // 지원 동기 (기본 apply reason)
    await page.locator('#applyReason').fill('도시농업에 관심이 많습니다.');
    // 동적 TEXT (지원 동기 - 관리자 정의)
    await page.locator('textarea[name="dynamicAnswers[1]"]').fill('농업 실습 경험을 넓히고 싶어요.');
    // 동적 DROPDOWN (관심 강좌)
    await page.locator('select[name="dynamicAnswers[2]"]').selectOption('농작물 재배');
    // ATTACHMENT (id=3) 는 optional 이라 미첨부

    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();

    await page.waitForURL(/\/apply\/complete\?applicationId=\d+/);
    await expect(page).toHaveTitle(/신청/);
});

test('동적 DROPDOWN 필수 미선택 시 서버 400 → flash 에러 alert', async ({ page }) => {
    await login(page, USER);
    await page.goto(`/programs/${PROGRAM_ID}/apply`, { waitUntil: 'domcontentloaded' });

    await applyNextStep(page, 2);
    // 관리자 정의 TEXT/DROPDOWN 필드 모두 미입력 (required 위반)
    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();

    await page.waitForURL(`**/programs/${PROGRAM_ID}/apply`);
    await expect(page.locator('.alert.alert-error')).toContainText('필수 항목');
});

test('회귀 방지: seed program #3 (dynamic 0건) 신청 흐름 무회귀', async ({ page }) => {
    // seed program #3 = 마음건강 힐링 캠프 (dynamic field 없음)
    const otherProgramId = 3;
    await page.request.post('/__test__/reset-applications', {
        data: { userEmail: USER, programId: otherProgramId },
    });
    await login(page, USER);
    await page.goto(`/programs/${otherProgramId}/apply`, { waitUntil: 'domcontentloaded' });
    await applyNextStep(page, 2);
    // dynamic 필드 0건 확인
    await expect(page.locator('.apply-dynamic-field')).toHaveCount(0);
    await page.locator('#applyReason').fill('회귀 확인용 지원 동기.');
    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();
    await page.waitForURL(/\/apply\/complete\?applicationId=\d+/);
});
