import { test, expect } from '@playwright/test';
import { abortExternal, applyNextStep, login, seedEmail } from '../helpers';

/**
 * 프로그램 신청 여정 — 3단계 위저드 (PR #75 F0c 개편 반영).
 *
 * apply.html 구조:
 *  - step 1 신청자 정보(readonly) / step 2 지원 동기 / step 3 개인정보 동의 + 제출
 *  - 비활성 스텝 카드는 display:none → applyNextStep() 으로 이동 후 입력
 *  - 동의 미체크 시 제출 버튼(#applyNavSubmit) disabled — UI 가 미체크 제출을 차단
 *  - 서버 검증 실패 시 window.__applyErrors 로 에러 있는 스텝을 초기 표시
 *
 * ApplyRequest 검증 (F0c-remainder Q2 결정 반영):
 *  - applyReason: 선택 입력, @Size(max=1000) 만 유지 (@NotBlank·@Size(min=10) 제거됨)
 *  - privacyAgreed: @AssertTrue "개인정보 수집 동의가 필요합니다."
 * ApplicationService:
 *  - 중복 신청 시 IllegalStateException("이미 신청한 프로그램입니다.") → flash applyError
 */

const FRESH_USER = seedEmail(30);          // 어느 프로그램에도 미신청
const DUPLICATE_USER = seedEmail(1);       // programs[0] (id=1) 에 이미 APPROVED
const FRESH_PROGRAM_ID = 3;   // 마음건강 힐링 캠프 (ACTIVE, 6/20)
const DUP_PROGRAM_ID = 1;     // 취업역량 강화 워크숍 (seed1 이미 신청)

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('로그인 후 신청 폼 진입 시 신청자 정보(이름·이메일)가 자동 채워진다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });
    await expect(page).toHaveTitle(/신청/);

    // apply.html: 이름/이메일 은 <div class="apply-applicant-value"> 로 readonly 렌더 (step 1)
    const applicant = page.locator('.apply-applicant-fields');
    await expect(applicant).toContainText('시드유저30');
    await expect(applicant).toContainText(FRESH_USER);
});

test('개인정보 동의 미체크 시 제출 버튼 비활성 + 서버 @AssertTrue 방어가 동작한다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await applyNextStep(page, 2);
    await page.locator('#applyReason').fill('진로 탐색을 위해 참여하고 싶어요.');
    await applyNextStep(page, 3);

    // 위저드 UI: 미체크 상태에서는 제출 버튼이 disabled → 클릭 자체가 차단됨
    await expect(page.locator('#applyNavSubmit')).toBeDisabled();

    // 서버 측 @AssertTrue 는 UI 우회(form 직접 제출) 로 검증 — 클라이언트 차단만 믿지 않는다
    await page.evaluate(() =>
        (document.getElementById('applyForm') as HTMLFormElement).submit());

    // 검증 실패 rerender → __applyErrors.privacyAgreed 로 step 3 이 초기 표시됨
    await expect(page.locator('[data-step-card="3"]')).toBeVisible();
    await expect(page.locator('.apply-field-error')).toContainText('개인정보 수집 동의가 필요합니다.');
});

test('지원 동기 1000자 초과 제출 시 @Size(max) 메시지가 노출된다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await applyNextStep(page, 2);
    await page.locator('#applyReason').fill('가'.repeat(1001));   // @Size(max=1000) 위반
    await applyNextStep(page, 3);
    // apply.html: privacyAgreed input 은 label wrapper 안에서 opacity:0 + pointer-events:none (custom UI)
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();

    // 검증 실패 rerender → __applyErrors.applyReason 으로 step 2 가 초기 표시됨
    await expect(page.locator('[data-step-card="2"]')).toBeVisible();
    await expect(page.locator('.apply-field-error')).toContainText('1000자 이하');
});

test('이미 신청한 프로그램에 재신청 시 "이미 신청한 프로그램입니다." 에러 alert', async ({ page }) => {
    await login(page, DUPLICATE_USER);
    await page.goto(`/programs/${DUP_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await applyNextStep(page, 2);
    await page.locator('#applyReason').fill('중복 신청 테스트용 지원 동기 문장입니다.');
    await applyNextStep(page, 3);
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('#applyNavSubmit').click();

    // ApplicationService.IllegalStateException → redirect /programs/{id}/apply + flash applyError
    await page.waitForURL(`**/programs/${DUP_PROGRAM_ID}/apply`);
    await expect(page.locator('.alert.alert-error')).toContainText('이미 신청한 프로그램입니다.');
});
