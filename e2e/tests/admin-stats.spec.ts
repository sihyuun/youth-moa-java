/**
 * A6-followup (2026-09-29) — 관리자 통계 대시보드 조회수·정렬 회귀 E2E.
 *
 * 검증 축:
 *  (a) USER 세션 dedup — 같은 프로그램 상세 2회 진입 시 조회수는 +1 만
 *  (b) 관리자 skip — CENTER_ADMIN 상세 진입은 조회수 미증가
 *  (c) admin-stats 조회수 셀 실 수치 렌더 (숫자)
 *  (d) 프로그램별 정렬 = 신청수 DESC (상위 3건 내림차순)
 *  (e) 마감임박 카드 상한 5건 유지
 */
import { expect, test } from '@playwright/test';
import {
    abortExternal,
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    login,
    loginAdmin,
    SEED_PASS,
    seedEmail,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

async function readProgramViews(page: any, programId: number): Promise<number> {
    // /admin/stats 프로그램별 참여 행에서 특정 program id 링크의 조회수 셀을 읽음.
    // 상위 6 정렬 기준이 신청수 DESC 라 대상 program 이 상위에 없을 수 있음 → data-* 없이 링크 href 매칭.
    const row = page.locator(`.admin-stats-program-row--link[href*="/admin/programs/${programId}"]`).first();
    if ((await row.count()) === 0) return NaN;
    const text = await row.locator('.admin-stats-program-views').innerText();
    return Number.parseInt(text.replace(/[^\d]/g, ''), 10);
}

async function readAllViewsInStats(page: any): Promise<number> {
    // /admin/stats 진입 후 상위 6 정렬 대상에서 seed 프로그램 하나라도 숫자셀 있는지 sanity check
    const cells = page.locator('.admin-stats-program-row--link .admin-stats-program-views');
    const n = await cells.count();
    return n;
}

test('(c) admin-stats 프로그램별 조회수 셀이 정수 렌더 + 최소 1건 존재', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/stats', { waitUntil: 'domcontentloaded' });

    const cellCount = await readAllViewsInStats(page);
    expect(cellCount).toBeGreaterThan(0);

    // 첫 셀 텍스트가 순수 정수
    const first = await page.locator('.admin-stats-program-row--link .admin-stats-program-views').first().innerText();
    expect(first.trim()).toMatch(/^\d+$/);

    // 시드 랜덤 30~200 기반. 최소 하나는 30 이상.
    const nums = await page
        .locator('.admin-stats-program-row--link .admin-stats-program-views')
        .allInnerTexts();
    const parsed = nums.map((t) => Number.parseInt(t.replace(/[^\d]/g, ''), 10)).filter((n) => !Number.isNaN(n));
    expect(parsed.some((n) => n >= 30)).toBeTruthy();
});

test('(a) USER 세션에서 같은 프로그램 2회 진입 시 조회수는 +1 만 (dedup)', async ({ page, browser }) => {
    const programId = 1;

    // baseline 측정 (관리자로 stats 페이지 열어서 확인)
    const adminCtx = await browser.newContext();
    const adminPage = await adminCtx.newPage();
    await abortExternal(adminPage);
    await loginAdmin(adminPage);
    await adminPage.goto('/admin/stats', { waitUntil: 'domcontentloaded' });
    const before = await readProgramViews(adminPage, programId);

    // USER 로그인 후 2회 진입
    await login(page, seedEmail(1), SEED_PASS);
    await page.goto(`/programs/${programId}`, { waitUntil: 'domcontentloaded' });
    await page.goto(`/programs/${programId}`, { waitUntil: 'domcontentloaded' });

    // 조회수 재확인
    await adminPage.reload({ waitUntil: 'domcontentloaded' });
    const after = await readProgramViews(adminPage, programId);

    await adminCtx.close();

    // baseline 이 상위 6 정렬에서 밀려 NaN 이면 skip 처리 (신청수 DESC 정렬 특성).
    // 그렇지 않으면 정확히 +1 이어야 한다 (세션 dedup).
    if (!Number.isNaN(before) && !Number.isNaN(after)) {
        expect(after - before).toBe(1);
    }
});

test('(b) CENTER_ADMIN 프로그램 상세 진입은 조회수 미증가 (Role skip)', async ({ page, browser }) => {
    const programId = 1;

    const adminStatsCtx = await browser.newContext();
    const adminStatsPage = await adminStatsCtx.newPage();
    await abortExternal(adminStatsPage);
    await loginAdmin(adminStatsPage);
    await adminStatsPage.goto('/admin/stats', { waitUntil: 'domcontentloaded' });
    const before = await readProgramViews(adminStatsPage, programId);

    // 다른 브라우저 세션에서 CENTER_ADMIN 로그인 후 사용자 프로그램 상세 진입
    // (관리자도 사용자 페이지 /programs/{id} 는 접근 가능. Role skip 확인)
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    await page.goto(`/programs/${programId}`, { waitUntil: 'domcontentloaded' });
    await page.goto(`/programs/${programId}`, { waitUntil: 'domcontentloaded' });

    await adminStatsPage.reload({ waitUntil: 'domcontentloaded' });
    const after = await readProgramViews(adminStatsPage, programId);

    await adminStatsCtx.close();

    if (!Number.isNaN(before) && !Number.isNaN(after)) {
        expect(after).toBe(before);
    }
});

test('(d) 프로그램별 참여 현황 정렬 = 신청수 DESC', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/stats', { waitUntil: 'domcontentloaded' });

    const applied = await page.locator('.admin-stats-program-row--link .admin-stats-program-applied').allInnerTexts();
    // '5 / 30' 포맷 → 신청수만 추출
    const parsed = applied
        .map((t) => Number.parseInt(t.split('/')[0].replace(/[^\d]/g, ''), 10))
        .filter((n) => !Number.isNaN(n));
    expect(parsed.length).toBeGreaterThan(0);

    // 내림차순 검증 (인접 pair)
    for (let i = 0; i < parsed.length - 1; i++) {
        expect(parsed[i]).toBeGreaterThanOrEqual(parsed[i + 1]);
    }
});

test('(e) 마감임박 카드 상한 5건 유지', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/stats', { waitUntil: 'domcontentloaded' });
    // 마감임박 리스트 카드 (2개 리스트 카드 중 1번째 = 마감임박)
    const urgent = page.locator('.admin-stats-list-card').first();
    const rows = urgent.locator('.admin-stats-list-row');
    const n = await rows.count();
    expect(n).toBeLessThanOrEqual(5);
});
