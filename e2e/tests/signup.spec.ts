import { test, expect, Page } from '@playwright/test';

/**
 * 회원가입 (/signup) 시각·동작 자동화 검증.
 * Java 단위 테스트로 잡기 어려운 form validation 메시지·실시간 JS·렌더링을 커버.
 */

/** form 의 모든 visible signup-field-error / alert-error 텍스트를 수집 */
async function collectErrors(page: Page): Promise<string[]> {
    // submit 후 페이지 응답 완전 로딩 대기 (race condition 회피)
    await page.waitForLoadState('domcontentloaded');
    // 적어도 첫 에러 element 가 attach 될 때까지 대기
    await page.locator('.signup-field-error').first().waitFor({ state: 'attached', timeout: 5000 }).catch(() => {});
    return await page.locator('.signup-field-error:visible, .alert-error:visible').allInnerTexts();
}

test.beforeEach(async ({ page }) => {
    await page.goto('/signup');
    await expect(page).toHaveTitle(/회원가입/);
});

test('빈 폼 제출 시 1단계(RequiredCheck) 메시지만 노출', async ({ page }) => {
    await page.locator('button.signup-submit-btn').click();
    const errs = await collectErrors(page);

    // 입력 안 한 9개 필드의 NotBlank/NotNull 메시지
    expect(errs).toContain('이메일을 입력해주세요.');
    expect(errs).toContain('비밀번호를 입력해주세요.');
    expect(errs).toContain('비밀번호 확인을 입력해주세요.');
    expect(errs).toContain('이름을 입력해주세요.');
    expect(errs).toContain('핸드폰 번호를 입력해주세요.');
    expect(errs).toContain('성별을 선택해주세요.');
    expect(errs).toContain('생년월일을 입력해주세요.');
    expect(errs).toContain('우편번호를 입력해주세요.');
    expect(errs).toContain('주소를 입력해주세요.');

    // 2단계(FormatCheck) 메시지는 안 보여야 함
    const formatMsgs = [
        '아이디 중복확인을 진행해주세요.',
        '이용약관과 개인정보처리방침에 모두 동의해주세요.',
        '비밀번호와 비밀번호 확인이 일치하지 않습니다.',
        '비밀번호는 8자 이상이어야 합니다.',
        '비밀번호는 영문과 숫자를 모두 포함해야 합니다.',
    ];
    for (const m of formatMsgs) {
        expect(errs).not.toContain(m);
    }
});

test('비밀번호 실시간 정책 검증 — 누락 조건별 한 문장', async ({ page }) => {
    const pw = page.locator('#password');
    const msg = page.locator('#pw-policy-msg');

    await pw.fill('abc');
    await expect(msg).toBeVisible();
    await expect(msg).toContainText('비밀번호는 8자 이상이어야 하고, 숫자를 포함해야 합니다.');

    await pw.fill('abcdefgh');
    await expect(msg).toContainText('비밀번호는 숫자를 포함해야 합니다.');

    await pw.fill('12345678');
    await expect(msg).toContainText('비밀번호는 영문을 포함해야 합니다.');

    await pw.fill('Test1234');
    // 정책 충족 — 메시지가 ✓ 초록색 피드백으로 전환 (컬리 패턴)
    await expect(msg).toBeVisible();
    await expect(msg).toContainText('✓');
    await expect(msg).toHaveClass(/signup-field-ok/);
});

test('서버 측 비밀번호 정책 위반 — 한 문장 통합', async ({ page }) => {
    // 빈 폼 → 1단계 통과 위해 필수 필드만 채움 + 비밀번호는 정책 위반
    await page.locator('#email').fill('t@t.com');
    await page.locator('#password').fill('abc');
    await page.locator('#passwordConfirm').fill('abc');
    await page.locator('#name').fill('홍길동');
    await page.locator('#phone').fill('01012345678');
    await page.locator('input[name="gender"][value="MALE"]').check({ force: true });
    await page.locator('#birthDateText').fill('1990-01-01');
    // 우편번호 / 주소는 readonly 라 dummy 검색
    await page.locator('button.signup-search-btn').click();
    await page.locator('input[name="termsAgreed"]').check({ force: true });
    await page.locator('input[name="privacyAgreed"]').check({ force: true });
    // emailChecked 는 hidden 으로 default false → 위반 케이스 만들기 위해 그대로 두면
    // "아이디 중복확인" 메시지도 같이 나옴. 통합 메시지만 검증 위해 true 로 강제.
    await page.evaluate(() => {
        const el = document.getElementById('emailCheckedHidden') as HTMLInputElement;
        if (el) el.value = 'true';
    });

    await page.locator('button.signup-submit-btn').click();

    const errs = await collectErrors(page);
    expect(errs).toContain('비밀번호는 8자 이상이어야 하고, 숫자를 포함해야 합니다.');
    // 분리된 메시지가 같이 나오면 안 됨
    expect(errs).not.toContain('비밀번호는 8자 이상이어야 합니다.');
    expect(errs).not.toContain('비밀번호는 영문과 숫자를 모두 포함해야 합니다.');
});

test('FormatCheck 그룹 내 다중 @AssertTrue 위반 모두 노출 (회귀)', async ({ page }) => {
    // 1단계 모두 통과 (모든 필수 필드 입력)
    // 2단계에서 3개 @AssertTrue 동시 위반:
    //   - isPasswordMatched (불일치)
    //   - isAllTermsAccepted (약관 미동의)
    //   - isEmailChecked (중복확인 미실행)
    await page.locator('#email').fill('multi@example.com');
    await page.locator('#password').fill('Test1234');
    await page.locator('#passwordConfirm').fill('Other999');   // 불일치
    await page.locator('#name').fill('홍길동');
    await page.locator('#phone').fill('01012345678');
    await page.locator('input[name="gender"][value="MALE"]').check({ force: true });
    await page.locator('#birthDateText').fill('1990-01-01');
    await page.locator('button.signup-search-btn').click();
    // 약관 미체크 / 중복확인 미실행

    await page.locator('button.signup-submit-btn').click();

    const errs = await collectErrors(page);
    // 3개 모두 노출되어야 함
    expect(errs).toContain('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
    expect(errs).toContain('이용약관과 개인정보처리방침에 모두 동의해주세요.');
    expect(errs).toContain('아이디 중복확인을 진행해주세요.');
});

test('중복확인 안 누르고 제출 — 안내 메시지 노출', async ({ page }) => {
    // 1단계 모두 통과 + 정책 통과 + 약관 동의 + 중복확인 미실행
    await page.locator('#email').fill('new@example.com');
    await page.locator('#password').fill('Test1234');
    await page.locator('#passwordConfirm').fill('Test1234');
    await page.locator('#name').fill('홍길동');
    await page.locator('#phone').fill('01012345678');
    await page.locator('input[name="gender"][value="MALE"]').check({ force: true });
    await page.locator('#birthDateText').fill('1990-01-01');
    await page.locator('button.signup-search-btn').click();
    await page.locator('input[name="termsAgreed"]').check({ force: true });
    await page.locator('input[name="privacyAgreed"]').check({ force: true });

    await page.locator('button.signup-submit-btn').click();

    const errs = await collectErrors(page);
    expect(errs).toContain('아이디 중복확인을 진행해주세요.');
});
