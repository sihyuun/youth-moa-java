import { test, expect, type Page } from '@playwright/test';

/**
 * 프로그램 신청 여정 (D1 완료분).
 *
 * D1b (`/apply/complete` 신청 완료 화면) 은 병렬 세션에서 진행 중이라 여기서 커버 안 함.
 * 현재 baseline (origin/main) 은 신청 성공 시 /programs/{id} 로 flash "applySuccess" 리다이렉트.
 *
 * ApplyRequest 검증:
 *  - applyReason: @NotBlank + @Size(10~1000)
 *  - privacyAgreed: @AssertTrue "개인정보 수집 동의가 필요합니다."
 * ApplicationService:
 *  - 중복 신청 시 IllegalStateException("이미 신청한 프로그램입니다.") → flash applyError
 */

const FRESH_USER = 'seed30@youth-moa.test';        // 어느 프로그램에도 미신청
const DUPLICATE_USER = 'seed1@youth-moa.test';     // programs[0] (id=1) 에 이미 APPROVED
const SEED_PASS = 'Test1234!';
const FRESH_PROGRAM_ID = 3;   // 마음건강 힐링 캠프 (ACTIVE, 6/20)
const DUP_PROGRAM_ID = 1;     // 취업역량 강화 워크숍 (seed1 이미 신청)

async function login(page: Page, email: string) {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('input[name="username"]').fill(email);
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
}

test.beforeEach(async ({ page }) => {
    await page.route(/^https?:\/\/(?!localhost)/, route => route.abort());
});

test('로그인 후 신청 폼 진입 시 신청자 정보(이름·이메일)가 자동 채워진다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });
    await expect(page).toHaveTitle(/신청/);

    // apply.html: 이름/이메일 은 <div class="apply-applicant-value"> 로 readonly 렌더
    const applicant = page.locator('.apply-applicant-fields');
    await expect(applicant).toContainText('시드유저30');
    await expect(applicant).toContainText(FRESH_USER);
});

test('개인정보 동의 미체크 상태로 제출 시 필드 에러가 노출된다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await page.locator('#applyReason').fill('진로 탐색을 위해 참여하고 싶어요.');   // 10자 이상
    // privacyAgreed 체크박스는 그대로 두고 제출
    await page.locator('button.apply-submit-btn').click();

    // @AssertTrue 메시지 (ApplyRequest.isPrivacyAccepted)
    // hasErrors('privacyAgreed') 로 필드 에러 표시 안 되고 global 로 뜰 수 있으니 둘 다 커버
    const body = await page.locator('body').innerText();
    expect(body).toContain('개인정보 수집 동의가 필요합니다.');
});

test('지원 동기 10자 미만 제출 시 @Size 메시지가 노출된다', async ({ page }) => {
    await login(page, FRESH_USER);
    await page.goto(`/programs/${FRESH_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await page.locator('#applyReason').fill('짧음');   // 2자 → @Size(min=10) 위반
    // apply.html: privacyAgreed input 은 label wrapper 안에서 opacity:0 + pointer-events:none (custom UI)
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('button.apply-submit-btn').click();

    await expect(page.locator('.apply-field-error')).toContainText('10자 이상');
});

test('이미 신청한 프로그램에 재신청 시 "이미 신청한 프로그램입니다." 에러 alert', async ({ page }) => {
    await login(page, DUPLICATE_USER);
    await page.goto(`/programs/${DUP_PROGRAM_ID}/apply`, { waitUntil: 'commit' });

    await page.locator('#applyReason').fill('중복 신청 테스트용 지원 동기 문장입니다.');
    // apply.html: privacyAgreed input 은 label wrapper 안에서 opacity:0 + pointer-events:none (custom UI)
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    await page.locator('button.apply-submit-btn').click();

    // ApplicationService.IllegalStateException → redirect /programs/{id}/apply + flash applyError
    await page.waitForURL(`**/programs/${DUP_PROGRAM_ID}/apply`);
    await expect(page.locator('.alert.alert-error')).toContainText('이미 신청한 프로그램입니다.');
});
