import { expect, test } from '@playwright/test';
import { ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS, abortExternal, loginAdmin } from '../helpers';

/**
 * A5-1 admin staff management + 강제 비밀번호 변경 (2026-09-18) — 기능 E2E.
 *
 * 배경: A5-1 은 계약(admin-user*.ts)만 있고 클릭→응답→DOM 시퀀스를 검증하는 기능 E2E 가
 * 없었다 (STATE.md A5-1 이월 "A5-1-e2e"). 계정 발급·임시 비밀번호 1회 노출·재발급·강제
 * 비밀번호 변경은 전부 인터랙션 영역이라 계약 검사로는 커버 못 한다 (CLAUDE.md 인터랙션 검증 조항).
 *
 * 반복 실행 안전성: 발급된 스태프 계정을 지우는 TestFixtureController reset 엔드포인트가 없다.
 * 따라서 매 테스트가 `Date.now()` 로 유니크 이메일을 만들어 중복 발급 실패를 회피한다
 * (e2e 프로파일은 H2 in-memory create-drop 이라 부팅마다 초기화되고, 누적은 단일 부팅 내에서만 발생).
 *
 * 격리: 스태프 발급은 seed 유저를 건드리지 않으므로 reset-users 불필요.
 */

/** 유니크 스태프 이메일 (반복 실행 시 이메일 중복 발급 회피). */
function uniqueEmail(tag: string): string {
    return `e2e-${tag}-${Date.now()}@youth-moa.test`;
}

const NEW_FORM = 'form[data-testid="admin-user-new-form"]';
const EMAIL_INPUT = 'input[data-testid="admin-user-new-email"]';
const NAME_INPUT = 'input[data-testid="admin-user-new-name"]';
const ACCOUNT_TYPE_ADMIN = 'input[data-testid="admin-user-new-account-type-admin"]';
const ROLE_CENTER = 'input[data-testid="admin-user-new-role-center"]';
const CENTER_SELECT = 'select[data-testid="admin-user-new-center"]';
const SUBMIT = 'button[data-testid="admin-user-new-submit"]';
const INITIAL_PW_CARD = '[data-testid="admin-user-initial-password"]';
const INITIAL_PW_VALUE = '[data-testid="admin-user-initial-password-value"]';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('신규 발급 폼 렌더 — SYSTEM_ADMIN 이 /admin/users/new 진입 → 폼·핵심 필드 노출', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });

    await expect(page.locator(NEW_FORM)).toBeVisible();
    await expect(page.locator(EMAIL_INPUT)).toBeVisible();
    await expect(page.locator(NAME_INPUT)).toBeVisible();
    await expect(page.locator(SUBMIT)).toBeVisible();
});

test('동적 섹션 — 계정타입 ADMIN 선택 시 관리자 종류·센터 섹션이 순차 노출', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });

    // 기본(USER): 관리자 종류 섹션 숨김 (new.html 초기 style="display:none")
    await expect(page.locator('#adminRoleSection')).toBeHidden();

    // ADMIN 선택 → 관리자 종류 섹션 노출
    await page.locator(ACCOUNT_TYPE_ADMIN).check();
    await expect(page.locator('#adminRoleSection')).toBeVisible();

    // CENTER_ADMIN 선택 → 소속 센터 선택 섹션 노출
    await page.locator(ROLE_CENTER).check();
    await expect(page.locator('#centerSection')).toBeVisible();
});

test('USER 계정 발급 — 제출 → 상세로 redirect + 임시 비밀번호 1회 노출', async ({ page }) => {
    const email = uniqueEmail('issue');
    await loginAdmin(page);
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });

    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('E2E 발급대상');
    // accountType 은 기본 USER(checked). 그대로 제출.
    await page.locator(SUBMIT).click();

    // 302 → /admin/users/{id}
    await page.waitForURL(/\/admin\/users\/\d+/);

    // 초기 비밀번호 카드 + 값 노출 (flash — 1회성)
    await expect(page.locator(INITIAL_PW_CARD)).toBeVisible();
    const tempPw = (await page.locator(INITIAL_PW_VALUE).textContent())?.trim();
    expect(tempPw && tempPw.length >= 8).toBeTruthy();

    // 발급 대상 이메일이 화면에 표기되는지
    await expect(page.locator(INITIAL_PW_CARD)).toContainText(email);
});

test('이메일 중복 발급 — 같은 이메일 재발급 시 에러 메시지 노출', async ({ page }) => {
    const email = uniqueEmail('dup');
    await loginAdmin(page);

    // 1차 발급 (성공)
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('중복 테스트');
    await page.locator(SUBMIT).click();
    await page.waitForURL(/\/admin\/users\/\d+/);

    // 2차 발급 (같은 이메일 → 실패)
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('중복 테스트2');
    await page.locator(SUBMIT).click();

    // 중복 이메일 에러 메시지 (정확 문구는 서버 검증 메시지 — "이미 사용" 부분 매칭으로 견고화)
    await expect(page.getByText(/이미 사용/)).toBeVisible();
});

test('임시 비밀번호 재발급 — 상세에서 재발급 버튼 → 새 임시 비밀번호 노출', async ({ page }) => {
    const email = uniqueEmail('reset');
    await loginAdmin(page);

    // 대상 유저 발급
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('재발급 대상');
    await page.locator(SUBMIT).click();
    await page.waitForURL(/\/admin\/users\/\d+/);
    const firstPw = (await page.locator(INITIAL_PW_VALUE).textContent())?.trim();

    // 재발급 버튼 — form onsubmit=confirm(...) 이므로 dialog accept
    page.once('dialog', dialog => dialog.accept());
    await page.locator('button[data-testid="admin-user-reset-password-btn"]').click();
    await page.waitForURL(/\/admin\/users\/\d+/);

    // 새 임시 비밀번호 카드 재노출
    await expect(page.locator(INITIAL_PW_CARD)).toBeVisible();
    const secondPw = (await page.locator(INITIAL_PW_VALUE).textContent())?.trim();
    expect(secondPw && secondPw.length >= 8).toBeTruthy();
    // 재발급이면 이전 비밀번호와 달라야 한다 (SecureRandom 12자)
    expect(secondPw).not.toBe(firstPw);
});

test('강제 비밀번호 변경 — 신규 관리자 첫 로그인 시 /password/change 강제 → 변경 후 정상 진입', async ({
    page,
    browser,
}) => {
    const email = uniqueEmail('force');
    const newPw = 'NewPass123';

    // 1) SYSTEM_ADMIN 이 CENTER_ADMIN 스태프 발급 (강제 변경 flag 확정 대상)
    await loginAdmin(page);
    await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    await page.locator(EMAIL_INPUT).fill(email);
    await page.locator(NAME_INPUT).fill('강제변경 대상');
    await page.locator(ACCOUNT_TYPE_ADMIN).check();
    await page.locator(ROLE_CENTER).check();
    await page.locator(CENTER_SELECT).selectOption({ index: 1 });
    await page.locator(SUBMIT).click();
    await page.waitForURL(/\/admin\/users\/\d+/);
    const tempPw = (await page.locator(INITIAL_PW_VALUE).textContent())?.trim();
    expect(tempPw).toBeTruthy();

    // 2) 발급된 스태프로 별도 컨텍스트에서 로그인 → 인터셉터가 /password/change 로 강제 이동
    const ctx = await browser.newContext();
    const staff = await ctx.newPage();
    await abortExternal(staff);
    await staff.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await staff.locator('input[name="username"]').fill(email);
    await staff.locator('input[name="password"]').fill(tempPw!);
    await staff.locator('#adminLoginForm button[type="submit"]').click();

    await staff.waitForURL('**/password/change');
    await expect(staff.locator('[data-testid="password-change-force-notice"]')).toBeVisible();

    // 3) 비밀번호 변경
    await staff.locator('input[data-testid="password-change-current"]').fill(tempPw!);
    await staff.locator('input[data-testid="password-change-new"]').fill(newPw);
    await staff.locator('input[data-testid="password-change-confirm"]').fill(newPw);
    await staff.locator('button[data-testid="password-change-submit"]').click();

    // 성공 → /logout 경유. /password/change 를 벗어나면 성공.
    await staff.waitForURL(url => !url.pathname.endsWith('/password/change'));

    // 4) 새 비밀번호로 재로그인 → /admin 정상 진입 (강제 리다이렉트 없음 = flag 해제 확인)
    await staff.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await staff.locator('input[name="username"]').fill(email);
    await staff.locator('input[name="password"]').fill(newPw);
    await staff.locator('#adminLoginForm button[type="submit"]').click();
    await staff.waitForURL('**/admin');
    expect(new URL(staff.url()).pathname).toMatch(/\/admin$/);

    await ctx.close();
});

test('RBAC — CENTER_ADMIN 은 스태프 발급 화면 접근 불가 (403)', async ({ page }) => {
    // /admin/users/** 는 SYSTEM_ADMIN 전용 (@PreAuthorize hasRole SYSTEM_ADMIN)
    await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(ADMIN_CENTER1_EMAIL);
    await page.locator('input[name="password"]').fill(ADMIN_SEED_PASS);
    await page.locator('#adminLoginForm button[type="submit"]').click();
    await page.waitForURL('**/admin');

    const res = await page.goto('/admin/users/new', { waitUntil: 'domcontentloaded' });
    expect([401, 403]).toContain(res?.status() ?? 0);
});
