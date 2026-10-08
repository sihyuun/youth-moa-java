/**
 * A3-1 admin-program-form (2026-09-10) 기능 E2E.
 *
 * 시나리오:
 *   - create: 신규 프로그램 3탭 왕복 → 저장 → 편집 폼 prefilled 확인
 *   - edit: 편집 폼에서 제목 수정 → 저장 → 반영
 *   - delete-fk-soft: FK 있는 시드 프로그램(#1) 삭제 → 302 목록 · isActive=false (ADMIN-00 §Q10 소프트 삭제)
 *   - delete-clean: FK 없는 신규 프로그램 삭제 → 302 목록
 *   - rbac.center: CENTER_ADMIN 은 /new / POST 시 403 (Qn-1 A: SYSTEM only)
 *   - rbac.anon: 로그인 없이 접근 시 302 login
 *
 * seed-pollution 방지: afterAll 에서 resetPrograms.
 */
import { expect, test } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    abortExternal,
    loginAdmin,
    resetPrograms,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test.afterAll(async ({ browser }) => {
    const page = await browser.newPage();
    try {
        await loginAdmin(page);
        await resetPrograms(page);
    } finally {
        await page.close();
    }
});

test('신규 등록 → 편집 폼 prefilled 확인 (3탭 왕복)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-program-form-title')).toHaveText('프로그램 신규 등록');

    const uniqueTitle = `e2e-prog-${Date.now()}`;
    // 탭 1
    await page.locator('input[name="title"]').fill(uniqueTitle);
    // A9-a: organization text → centerId select. 첫 활성 센터 옵션 선택 (index 0 은 placeholder)
    await page.locator('select[name="centerId"]').selectOption({ index: 1 });
    const selectedCenterId = await page.locator('select[name="centerId"]').inputValue();
    await page.locator('textarea[name="content"]').fill('상세 내용 e2e');
    await page.locator('input[name="description"]').fill('짧은 설명 e2e');

    // 탭 2 이동 후 필수값 입력
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await expect(page.locator('#tab-apply')).toBeVisible();
    await page.locator('input[name="applyStartDate"]').fill('2026-09-01');
    await page.locator('input[name="applyEndDate"]').fill('2026-09-30');
    await page.locator('input[name="venue"]').fill('e2e 장소');
    await page.locator('input[name="contact"]').fill('02-000-0000');
    await page.locator('input[name="capacity"]').fill('50');
    // approvalMode MANUAL 은 기본값 라디오

    // 탭 3
    await page.locator('.admin-program-form-tab[data-tab-target="tab-terms"]').click();
    await expect(page.locator('#tab-terms')).toBeVisible();
    await page.locator('textarea[name="termsService"]').fill('약관 내용 e2e');

    // 저장 → FOLLOW-admin-program-detail-readonly: 저장 후 상세 화면으로 redirect.
    await page.locator('.admin-program-form-actions button[type="submit"]').click();
    await page.waitForURL(/\/admin\/programs\/\d+$/);
    await expect(page.locator('.admin-program-detail-page')).toBeVisible();
    await expect(page.locator('.admin-program-detail-title')).toHaveText(uniqueTitle);

    // 수정 CTA → /edit 이동해 prefilled 확인
    const editCta = page.locator('.admin-program-detail-header-actions a.admin-btn--primary', {
        hasText: '수정',
    });
    await Promise.all([page.waitForURL(/\/admin\/programs\/\d+\/edit$/), editCta.click()]);

    // 편집 폼 prefill 확인
    await expect(page.locator('.admin-program-form-title')).toHaveText('프로그램 편집');
    await expect(page.locator('input[name="title"]')).toHaveValue(uniqueTitle);
    // A9-a: select 는 저장된 centerId 값 유지 (option:checked 로 재확인)
    await expect(page.locator('select[name="centerId"]')).toHaveValue(selectedCenterId);
    await expect(page.locator('input[name="description"]')).toHaveValue('짧은 설명 e2e');

    // 탭 2 값 유지
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await expect(page.locator('input[name="applyStartDate"]')).toHaveValue('2026-09-01');
    await expect(page.locator('input[name="venue"]')).toHaveValue('e2e 장소');
    await expect(page.locator('input[name="capacity"]')).toHaveValue('50');

    // F4/F0c 진입 링크 노출 확인 (sub-links 는 탭1 안에 위치)
    await page.locator('.admin-program-form-tab[data-tab-target="tab-info"]').click();
    await expect(page.locator('#tab-info')).toBeVisible();
    await expect(page.locator('.admin-program-form-sub-links a[href*="/eligibility"]')).toBeVisible();
    await expect(page.locator('.admin-program-form-sub-links a[href*="/dynamic-fields"]')).toBeVisible();
});

test('편집 → 제목 수정 → 저장 → 반영', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });
    const initial = `edit-src-${Date.now()}`;
    await page.locator('input[name="title"]').fill(initial);
    // A9-a: organization → centerId select
    await page.locator('select[name="centerId"]').selectOption({ index: 1 });
    await page.locator('textarea[name="content"]').fill('본문');
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await page.locator('input[name="applyStartDate"]').fill('2026-09-01');
    await page.locator('input[name="applyEndDate"]').fill('2026-09-30');
    await page.locator('.admin-program-form-actions button[type="submit"]').click();
    // FOLLOW-admin-program-detail-readonly: 저장 후 상세로 redirect
    await page.waitForURL(/\/admin\/programs\/\d+$/);
    await expect(page.locator('.admin-program-detail-page')).toBeVisible();

    // 상세 → 수정 CTA → /edit 재진입
    const editCta = page.locator('.admin-program-detail-header-actions a.admin-btn--primary', {
        hasText: '수정',
    });
    await Promise.all([page.waitForURL(/\/admin\/programs\/\d+\/edit$/), editCta.click()]);

    const updated = `${initial}-mod`;
    await page.locator('input[name="title"]').fill(updated);
    await page.locator('.admin-program-form-actions button[type="submit"]').click();
    // 저장 후 다시 상세 → title 반영 확인
    await page.waitForURL(/\/admin\/programs\/\d+$/);
    await expect(page.locator('.admin-program-detail-title')).toHaveText(updated);
});

test('FK 참조가 있는 시드 프로그램(#1) 삭제 → 소프트 삭제 302 (ADMIN-00 §Q10)', async ({ page }) => {
    await loginAdmin(page);
    // 시드 프로그램 #1 은 다수 seed 신청이 있어 FK 참조 존재. 소프트 삭제이므로 FK 무관하게 성공.
    // CSRF 토큰 확보 후 POST /admin/programs/1/delete
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content');
    const response = await page.request.post('/admin/programs/1/delete', {
        headers: { 'X-CSRF-TOKEN': csrfToken ?? '' },
        form: { _csrf: csrfToken ?? '' },
        maxRedirects: 0,
    });
    expect(response.status()).toBe(302);
    expect(response.headers()['location']).toContain('/admin/programs');
    // 재조회 시 SUSPENDED 상태로 남아 있어야 함 (row 유지).
    // FOLLOW-admin-program-detail-readonly: /admin/programs/1 은 이제 read-only 상세이므로
    // active 체크박스는 편집 폼(/edit)에서만 확인 가능.
    await page.goto('/admin/programs/1/edit', { waitUntil: 'domcontentloaded' });
    // 편집 폼에서 활성 체크박스 (isActive) 가 해제되어 있어야 함
    const isActive = await page.locator('input[name="active"]').isChecked();
    expect(isActive).toBe(false);

    // Restore: 다른 spec (apply.spec.ts DUP_PROGRAM_ID=1) 오염 방지.
    // PR #206 seed-pollution 학습 재발 방지 — 편집 폼에서 active 재체크 후 저장.
    // active 체크박스는 탭2(신청정보)에 위치하므로 탭 전환 후 체크. force:true 로
    // Thymeleaf 가 생성한 id 변조(active1)에 관계없이 name 셀렉터로 체크 강제.
    // 시드 program #1 은 applyStartDate/EndDate 가 비어 있어 HTML5 required 로 submit 이 막힘 →
    // wide-range 날짜를 채워 넣어야 제출 가능. ApplicationService.apply() 는 apply 기간을 검사하지
    // 않으므로 (active 상태·중복 여부만) 이 값이 apply.spec.ts 시나리오에 영향 없음.
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await page.locator('input[name="applyStartDate"]').fill('2026-01-01');
    await page.locator('input[name="applyEndDate"]').fill('2099-12-31');
    await page.locator('input[name="active"]').check({ force: true });
    await page.locator('.admin-program-form-actions button[type="submit"]').click();
    await page.waitForURL(/\/admin\/programs\/\d+$/);
    // 편집 폼 재진입해 실제 저장 상태 확인 (redirect 후 GET)
    await page.goto('/admin/programs/1/edit', { waitUntil: 'domcontentloaded' });
    const isActiveRestored = await page.locator('input[name="active"]').isChecked();
    expect(isActiveRestored).toBe(true);
});

test('FK 없는 신규 프로그램 삭제 → 목록 리다이렉트', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });
    const t = `del-target-${Date.now()}`;
    await page.locator('input[name="title"]').fill(t);
    // A9-a: organization → centerId select
    await page.locator('select[name="centerId"]').selectOption({ index: 1 });
    await page.locator('textarea[name="content"]').fill('본문');
    await page.locator('.admin-program-form-tab[data-tab-target="tab-apply"]').click();
    await page.locator('input[name="applyStartDate"]').fill('2026-09-01');
    await page.locator('input[name="applyEndDate"]').fill('2026-09-30');
    await page.locator('.admin-program-form-actions button[type="submit"]').click();
    // FOLLOW-admin-program-detail-readonly: 저장 후 상세로 redirect
    await page.waitForURL(/\/admin\/programs\/(\d+)$/);
    await expect(page.locator('.admin-program-detail-page')).toBeVisible();
    const url = page.url();
    const id = url.match(/\/admin\/programs\/(\d+)$/)?.[1];
    expect(id).toBeTruthy();

    // 상세 ⋯ 더보기 → 삭제 메뉴 → 모달 → 확인
    await page.locator('[data-detail-menu-trigger]').click();
    await page.locator('[data-testid="detail-menu-delete"]').click();
    await page.locator('#program-delete-modal form button[type="submit"]').click();
    await page.waitForURL('**/admin/programs');
    // ADMIN-00 §Q10 소프트 삭제: row 유지 · isActive=false. 편집 폼 재진입 시 200 · 활성 체크박스 해제 상태.
    await page.goto(`/admin/programs/${id}/edit`, { waitUntil: 'domcontentloaded' });
    const isActive = await page.locator('input[name="active"]').isChecked();
    expect(isActive).toBe(false);
});

test('CENTER_ADMIN 이 /new GET 시 403 (Qn-1 A: SYSTEM only)', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('비로그인 시 등록 폼 접근 → 로그인 페이지로', async ({ page }) => {
    const response = await page.goto('/admin/programs/new', { waitUntil: 'domcontentloaded' });
    // Spring Security 는 302 → /admin/login. Playwright 는 최종 페이지 URL 로 이동
    expect(page.url()).toMatch(/\/admin\/login/);
    // final 200 (login page) 또는 302 (still forwarding)
    expect([200, 302]).toContain(response?.status() ?? 0);
});
