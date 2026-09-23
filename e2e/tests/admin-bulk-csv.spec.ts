import { readFileSync } from 'node:fs';
import { expect, test, type Page } from '@playwright/test';
import { ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS, abortExternal, loginAdmin } from '../helpers';

/**
 * A8 admin-bulk-csv (2026-09-17) — 대량 처리 + CSV 내보내기 기능 E2E.
 *
 * 배경: A8 은 계약(admin-*-bulk.ts / admin-csv.ts)만 있고 체크박스 대량선택 → 액션바 → 모달 →
 * form submit → 302 시퀀스를 검증하는 기능 E2E 가 없었다 (STATE.md A8 이월 "A8-e2e-suite").
 * 모든 bulk 액션은 admin-bulk.js 가 hidden form(`[data-bulk-form]`)을 채워 순수 form submit 으로
 * 302 redirect 한다 (HTMX 미사용 · 204 이슈 없음).
 *
 * 시드 오염 방지 (이 프로젝트 #1 사고 유형):
 *  - programs bulk: 운영중단 → 재활성화로 테스트 내 자기복원 (application 미변경)
 *  - users bulk: afterEach `/__test__/reset-users` (users 테이블만 원복 · application 무관)
 *  - applications bulk approve: reset-bulk-fixtures 는 programs[0](id=1) 의 APPROVED 시드까지
 *    PENDING 으로 오염시켜 apply/mypage spec 을 깨뜨린다. 따라서 시드 규약상 전부 PENDING 인
 *    programs[2](id=3) 만 대상으로 승인하고, afterEach `/__test__/reset-application-status`(programId=3)
 *    로 그 프로그램만 정밀 원복한다.
 *
 * serial: 여러 테스트가 seed 상태를 변경하므로 순차 실행으로 상호 간섭 차단.
 */
test.describe.configure({ mode: 'serial' });

/** programs[2] — 시드 규약상 6건 전부 PENDING (bulk approve 대상). helpers.ts 시드 규약 참고. */
const APPROVE_PROGRAM_ID = 3;

const BAR = '.admin-bulk-action-bar';
const MODAL = '[data-bulk-modal]';

async function resetUsers(page: Page): Promise<void> {
    const res = await page.request.post('/__test__/reset-users');
    if (res.status() !== 204) throw new Error(`reset-users failed status=${res.status()}`);
}

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
    // users 원복 (application 무관) + approve 대상 프로그램만 정밀 원복
    await resetUsers(page);
    await resetApplicationStatus(page, APPROVE_PROGRAM_ID);
});

test('선택 인터랙션 — 체크박스 선택 시 액션바 활성 + 개수 갱신 + 해제', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });

    const bar = page.locator(BAR);
    await expect(bar).toHaveAttribute('data-active', 'false');

    const boxes = page.locator(
        '.admin-program-row:not(.admin-program-row--head) input[data-bulk-checkbox]',
    );
    await boxes.nth(0).check();
    await boxes.nth(1).check();
    await expect(bar).toHaveAttribute('data-active', 'true');
    await expect(bar.locator('[data-bulk-count]')).toHaveText('2');

    // 전체 선택 (현재 페이지)
    const total = await boxes.count();
    await page.locator('[data-bulk-select-all]').check();
    await expect(bar.locator('[data-bulk-count]')).toHaveText(String(total));

    // 해제
    await bar.locator('[data-bulk-clear]').click();
    await expect(bar).toHaveAttribute('data-active', 'false');
});

test('프로그램 일괄 운영중단 → 재활성화 — 모달 확인 후 302 + 상태 반영 (자기복원)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });

    const boxes = page.locator(
        '.admin-program-row:not(.admin-program-row--head) input[data-bulk-checkbox]',
    );
    const id0 = await boxes.nth(0).getAttribute('value');
    const id1 = await boxes.nth(1).getAttribute('value');
    expect(id0 && id1).toBeTruthy();

    await boxes.nth(0).check();
    await boxes.nth(1).check();

    // 운영중단 → 모달 확인
    await page.locator(`${BAR} [data-bulk-action="deactivate"]`).click();
    await expect(page.locator(MODAL)).toHaveAttribute('data-open', 'true');
    await page.locator(`${MODAL} [data-bulk-modal-confirm]`).click();

    await page.waitForURL(/\/admin\/programs/);
    await expect(page.locator('.admin-flash--success')).toBeVisible();

    // 운영중단(SUSPENDED) 탭에 두 프로그램 반영
    await page.goto('/admin/programs?status=SUSPENDED', { waitUntil: 'domcontentloaded' });
    await expect(page.locator(`[data-program-id="${id0}"]`)).toBeVisible();
    await expect(page.locator(`[data-program-id="${id1}"]`)).toBeVisible();

    // 재활성화로 원복
    await page.locator(`input[data-bulk-checkbox][value="${id0}"]`).check();
    await page.locator(`input[data-bulk-checkbox][value="${id1}"]`).check();
    await page.locator(`${BAR} [data-bulk-action="reactivate"]`).click();
    await expect(page.locator(MODAL)).toHaveAttribute('data-open', 'true');
    await page.locator(`${MODAL} [data-bulk-modal-confirm]`).click();
    await page.waitForURL(/\/admin\/programs/);
    await expect(page.locator('.admin-flash--success')).toBeVisible();
});

test('사용자 일괄 차단 — 사유 모달 입력 후 확인 → 302 + flash', async ({ page }) => {
    await loginAdmin(page);
    // seed 유저만 노출 (관리자/self 제외 → Safeguard 회피)
    await page.goto('/admin/users?q=seed', { waitUntil: 'domcontentloaded' });

    const boxes = page.locator(
        '.admin-user-row:not(.admin-user-row--head) input[data-bulk-checkbox]',
    );
    await boxes.nth(0).check();
    await boxes.nth(1).check();
    await expect(page.locator(`${BAR} [data-bulk-count]`)).toHaveText('2');

    await page.locator(`${BAR} [data-bulk-action="deactivate"]`).click();

    // 사유 입력 필수 → 모달 input 노출
    const modal = page.locator(MODAL);
    await expect(modal).toHaveAttribute('data-open', 'true');
    const reason = modal.locator('[data-bulk-modal-input]');
    await expect(reason).toBeVisible();
    await reason.fill('E2E 대량 차단 사유');
    await modal.locator('[data-bulk-modal-confirm]').click();

    await page.waitForURL(/\/admin\/users/);
    await expect(page.locator('.admin-flash--success')).toBeVisible();
});

test('사용자 권한 변경 모달 — 역할 select 노출 + 취소 (무변경 확인)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users?q=seed', { waitUntil: 'domcontentloaded' });

    const boxes = page.locator(
        '.admin-user-row:not(.admin-user-row--head) input[data-bulk-checkbox]',
    );
    await boxes.nth(0).check();

    await page.locator(`${BAR} [data-bulk-action="role"]`).click();
    const modal = page.locator(MODAL);
    await expect(modal).toHaveAttribute('data-open', 'true');
    // requiresRole → 역할 select 노출
    await expect(modal.locator('[data-bulk-modal-select]')).toBeVisible();
    // 취소 → 모달 닫힘 · 변경 없음
    await modal.locator('[data-bulk-modal-cancel]').click();
    await expect(modal).toHaveAttribute('data-open', 'false');
    await expect(page).toHaveURL(/\/admin\/users/);
});

test('신청 일괄 승인 — PENDING 2건 선택 → 모달 확인 → 302 + APPROVED 반영 (정밀 복원)', async ({
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

    await page.locator(`${BAR} [data-bulk-action="approve"]`).click();
    await expect(page.locator(MODAL)).toHaveAttribute('data-open', 'true');
    await page.locator(`${MODAL} [data-bulk-modal-confirm]`).click();

    await page.waitForURL(new RegExp(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`));
    await expect(page.locator('[data-testid="flash-success"]')).toBeVisible();

    // APPROVED 필터에 승인 건 반영
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications?status=APPROVED`, {
        waitUntil: 'domcontentloaded',
    });
    const approvedRows = page.locator(
        '.admin-application-row:not(.admin-application-row--head)',
    );
    expect(await approvedRows.count()).toBeGreaterThanOrEqual(2);
});

test('CSV 내보내기 — 프로그램 목록 다운로드 + 파일명·헤더 검증', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });

    const [download] = await Promise.all([
        page.waitForEvent('download'),
        page.locator('a[data-testid="csv-export-btn"]').click(),
    ]);
    expect(download.suggestedFilename()).toMatch(/^programs_\d{8}_\d{6}\.csv$/);

    const content = readFileSync(await download.path(), 'utf8');
    expect(content).toContain('title');
    // A9-b: CSV 헤더 organization → centerName 리네임
    expect(content).toContain('centerName');
});

test('CSV 내보내기 — 사용자 목록 (SYSTEM_ADMIN) 다운로드 + 헤더 검증', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });

    const [download] = await Promise.all([
        page.waitForEvent('download'),
        page.locator('a[data-testid="csv-export-btn"]').click(),
    ]);
    expect(download.suggestedFilename()).toMatch(/^users_\d{8}_\d{6}\.csv$/);

    const content = readFileSync(await download.path(), 'utf8');
    expect(content).toContain('email');
    expect(content).toContain('role');
});

test('CSV 내보내기 — 신청 목록 다운로드 + 파일명 프로그램 스코프 검증', async ({ page }) => {
    await loginAdmin(page);
    await page.goto(`/admin/programs/${APPROVE_PROGRAM_ID}/applications`, {
        waitUntil: 'domcontentloaded',
    });

    const [download] = await Promise.all([
        page.waitForEvent('download'),
        page.locator('a[data-testid="csv-export-btn"]').click(),
    ]);
    expect(download.suggestedFilename()).toMatch(
        new RegExp(`^applications_p${APPROVE_PROGRAM_ID}_\\d{8}_\\d{6}\\.csv$`),
    );

    const content = readFileSync(await download.path(), 'utf8');
    expect(content).toContain('applicantEmail');
});

test('CSV RBAC — CENTER_ADMIN 은 사용자 CSV 내보내기 불가 (403)', async ({ page }) => {
    await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(ADMIN_CENTER1_EMAIL);
    await page.locator('input[name="password"]').fill(ADMIN_SEED_PASS);
    await page.locator('#adminLoginForm button[type="submit"]').click();
    await page.waitForURL('**/admin');

    const res = await page.goto('/admin/users/export.csv', { waitUntil: 'domcontentloaded' });
    expect([401, 403]).toContain(res?.status() ?? 0);
});
