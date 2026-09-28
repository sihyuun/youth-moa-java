/**
 * A7-e2e-suite (2026-09-28) — NEW_APPLICATION 알림 3축 fan-out E2E.
 *
 * 검증 축:
 *   - B-2 organization  : 프로그램 소속 센터 CENTER_ADMIN 이 배지 +1
 *   - B-3-A createdBy   : 프로그램 작성자 admin 이 배지 +1
 *   - B-3-B watcher     : 프로그램을 지켜보기 등록한 admin 이 배지 +1
 *
 * seed 매핑 (DataInitializer 실측):
 *   centers[0] = 내일꿈제작소(고양시) → CENTER_ADMIN: center1@youth-moa.test
 *   centers[1] = 28청춘창업소(고양시)  → CENTER_ADMIN: center2@youth-moa.test
 *   centers[2] = 과천시 청년공간 비행지구 → CENTER_ADMIN: admin_center2@youth-moa.test
 *   centers[3] = 청춘곳간(광명시)     → CENTER_ADMIN: admin_inactive@youth-moa.test (isActive=false)
 *
 *   programId=11 = "청년 크리에이터 프로그램" (center=내일꿈제작소, centers[0], createdBy=sysadmin)
 *   programId=5  = "AI 활용 실무 교육" (center=과천시 청년공간 비행지구, centers[2], createdBy=sysadmin)
 *   programId=17 = "청춘곳간" 소속 프로그램 (centers[3])
 *
 * fan-out 로직 자체 (distinct union · axis order · inactive filter) 는 JVM 유닛에서 검증됨:
 *   - CompositeApplicationCreatedResolverTest — b3b_only, distinct_union_across_three_axes 등
 *   - WatcherRecipientResolverTest — inactive admin skip
 * 본 E2E 는 배지 관측 관점에서 로직이 실제 HTTP + DB 사이클로도 성립하는지 통합 검증.
 *
 * seed pollution 방지:
 *   test.describe.serial 로 순차. beforeEach 에서 대상 admin 을 mark-all-read 로 배지 0 기준선 확보.
 *   watcher 등록은 시나리오별로 다르므로 spec 종료 시 정리하지 않는다 (다음 spec 이 다시 mark-all-read 하므로 무해).
 *
 * 사용 유저:
 *   - seed30 : 어떤 프로그램에도 미신청 (helpers 규약)
 *   - seed29 : 어떤 프로그램에도 미신청
 *   - 각 TC 는 apply 전 resetApplications 로 fresh state 강제
 */
import { expect, test, type Browser } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_CENTER2_EMAIL,
    ADMIN_INACTIVE_EMAIL,
    ADMIN_SEED_PASS,
    ADMIN_SYSTEM_EMAIL,
    abortExternal,
    applyProgram,
    ensureAdminWatches,
    login,
    loginAdmin,
    resetAdminNotifications,
    resetApplications,
    seedEmail,
} from '../helpers';

test.describe.configure({ mode: 'serial' });

// centers[0] 소속 · createdBy=sysadmin 프로그램 (organization + createdBy 축 겹침 검증에 사용).
const PROG_CENTER0 = 11;
// centers[2] 소속 · createdBy=sysadmin (watcher 축 순수 격리 검증에 사용).
const PROG_CENTER2 = 5;

const USER_30 = seedEmail(30);
const USER_29 = seedEmail(29);

/** admin 로그인 후 헤더 배지 카운트 조회. */
async function readAdminBadge(browser: Browser, adminEmail: string): Promise<number> {
    const ctx = await browser.newContext();
    try {
        const page = await ctx.newPage();
        await abortExternal(page);
        await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
        await page.locator('input[name="username"]').fill(adminEmail);
        await page.locator('input[name="password"]').fill(ADMIN_SEED_PASS);
        await page.locator('#adminLoginForm button[type="submit"]').click();
        await page.waitForURL('**/admin');
        // 헤더 상단 렌더된 초기 배지값. 명시적으로 GET /badge 호출로 최신값 확보 (30s polling 대기 X).
        const resp = await page.request.get('/admin/notifications/badge');
        expect(resp.status()).toBe(200);
        const body = await resp.text();
        const match = body.match(/data-notif-badge="(\d+)"/);
        return match == null ? 0 : Number.parseInt(match[1], 10);
    } finally {
        await ctx.close();
    }
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('(a) organization 축 — seed30 이 centers[0] 프로그램 신청 → center1 배지 +1', async ({
    browser,
    page,
}) => {
    // 기준선: center1 배지 클리어. USER 신청 정리.
    await resetAdminNotifications(browser, ADMIN_CENTER1_EMAIL);
    await login(page, USER_30);
    await resetApplications(page, { userEmail: USER_30, programId: PROG_CENTER0 });

    const before = await readAdminBadge(browser, ADMIN_CENTER1_EMAIL);

    await applyProgram(page, PROG_CENTER0, 'organization 축 fan-out 검증 사유');

    const after = await readAdminBadge(browser, ADMIN_CENTER1_EMAIL);
    expect(after).toBe(before + 1);
});

test('(b) createdBy 축 — 같은 시나리오에서 sysadmin 배지 +1 (프로그램 작성자)', async ({
    browser,
    page,
}) => {
    // seed 프로그램 전량이 sysadmin createdBy. 신청 시 sysadmin 도 배지 +1.
    // organization 축(center1) 과 createdBy 축(sysadmin) 을 하나의 신청으로 병행 검증.
    await resetAdminNotifications(browser, ADMIN_SYSTEM_EMAIL);
    await login(page, USER_30);
    await resetApplications(page, { userEmail: USER_30, programId: PROG_CENTER0 });

    const before = await readAdminBadge(browser, ADMIN_SYSTEM_EMAIL);

    await applyProgram(page, PROG_CENTER0, 'createdBy 축 fan-out 검증 사유');

    const after = await readAdminBadge(browser, ADMIN_SYSTEM_EMAIL);
    expect(after).toBe(before + 1);
});

test('(c) watcher 축 — admin_center2 가 centers[0] 프로그램 watch → seed29 신청 → admin_center2 배지 +1', async ({
    browser,
    page,
}) => {
    // admin_center2 는 centers[2] 소속. centers[0] 프로그램은 organization 축 매칭 X.
    // createdBy=sysadmin 이라 createdBy 축 매칭 X. 오직 watcher 축으로만 배지 +1 발생.
    await ensureAdminWatches(browser, ADMIN_CENTER2_EMAIL, PROG_CENTER0);
    await resetAdminNotifications(browser, ADMIN_CENTER2_EMAIL);
    await login(page, USER_29);
    await resetApplications(page, { userEmail: USER_29, programId: PROG_CENTER0 });

    const before = await readAdminBadge(browser, ADMIN_CENTER2_EMAIL);

    await applyProgram(page, PROG_CENTER0, 'watcher 축 fan-out 검증 사유');

    const after = await readAdminBadge(browser, ADMIN_CENTER2_EMAIL);
    expect(after).toBe(before + 1);
});

test('(d) 3축 distinct — sysadmin 이 자기 createdBy 프로그램을 watch 해도 배지는 1건만', async ({
    browser,
    page,
}) => {
    // sysadmin 은 createdBy 축으로 이미 매칭. 여기에 watcher 축 추가로 등록 → distinct union 결과 알림 1건.
    // 배지 관측: before → after 는 정확히 +1 이어야 함 (+2 아님).
    // organization 축은 sysadmin 이 CENTER_ADMIN 이 아니므로 관련 없음.
    await ensureAdminWatches(browser, ADMIN_SYSTEM_EMAIL, PROG_CENTER2);
    await resetAdminNotifications(browser, ADMIN_SYSTEM_EMAIL);
    await login(page, USER_30);
    await resetApplications(page, { userEmail: USER_30, programId: PROG_CENTER2 });

    const before = await readAdminBadge(browser, ADMIN_SYSTEM_EMAIL);

    await applyProgram(page, PROG_CENTER2, 'distinct union 검증 사유');

    const after = await readAdminBadge(browser, ADMIN_SYSTEM_EMAIL);
    // createdBy + watcher 두 축 매칭이지만 distinct union 으로 알림 1건 → 배지 +1
    expect(after).toBe(before + 1);
});

test('(e) 비활성 admin skip — admin_inactive 는 로그인 불가 · 존재 자체만 검증', async ({
    browser,
}) => {
    // WatcherRecipientResolver JPQL 필터 (w.admin.isActive = true) 로 자동 제외됨은 JVM 유닛에서 검증됨.
    // E2E 관점에서는 admin_inactive 로 로그인이 실패하는지 (isActive=false → 인증 차단) 를 통해 시드 정합만 검증.
    const ctx = await browser.newContext();
    try {
        const page = await ctx.newPage();
        await abortExternal(page);
        await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
        await page.locator('input[name="username"]').fill(ADMIN_INACTIVE_EMAIL);
        await page.locator('input[name="password"]').fill(ADMIN_SEED_PASS);
        await page.locator('#adminLoginForm button[type="submit"]').click();

        // 로그인 실패 — /admin/login?error 또는 /admin/login 자리에 머묾. /admin 이동은 없어야 함.
        await page.waitForLoadState('domcontentloaded');
        expect(page.url()).not.toMatch(/\/admin$/);
        expect(page.url()).toMatch(/\/admin\/login/);
    } finally {
        await ctx.close();
    }
});
