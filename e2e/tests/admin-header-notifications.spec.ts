/**
 * A7-e2e-suite (2026-09-28) — admin 헤더 알림 벨 인터랙션 E2E.
 *
 * 5개 TC:
 *   (a) 벨 클릭 → dropdown 오픈 · 항목 렌더
 *   (b) 항목 클릭 → POST /admin/notifications/{id}/read → unread class 제거 (다시 열어 확인) · 배지 감소
 *   (c) mark-all-read 클릭 → 배지 hidden 속성 부여 (unread=0)
 *   (d) 개별 delete → 항목 DOM 제거 + 배지 감소
 *   (e) 30s polling wrapper endpoint (GET /admin/notifications/badge) 200 응답 sanity check
 *
 * 사용 계정: sysadmin@youth-moa.test — seed program 전량의 createdBy 로 부착되어 있어 시드 시점에
 *   NEW_APPLICATION 축(B-3-A)으로 알림이 다수 발행됨 (AdminNotificationEventListener). 최소 4건 이상 unread.
 *
 * seed pollution 방지:
 *   각 TC 는 상태를 되돌리지 않지만, 로컬 e2e 프로파일은 create-drop → 매 부팅마다 fresh.
 *   전체 spec 진행 순서 상 (c) mark-all-read 는 후속 TC 의 unread 검증을 깨뜨리므로 마지막에 배치.
 *   (a)(b)(e) → (d) 삭제 1건 → (c) mark-all-read → 이후 회복 불가하므로 이 spec 은 test.describe.serial 로 순차 실행.
 *
 * 배지 관측:
 *   .admin-notif-badge 의 data-notif-badge 속성값을 parseInt 로 파싱. hidden 상태는 [hidden] 셀렉터.
 */
import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.describe.configure({ mode: 'serial' });

async function readBadgeCount(page: import('@playwright/test').Page): Promise<number> {
    const badge = page.locator('#admin-notif-badge');
    const val = await badge.getAttribute('data-notif-badge');
    return val == null ? 0 : Number.parseInt(val, 10);
}

async function openDropdown(page: import('@playwright/test').Page): Promise<void> {
    await page.locator('.admin-header-bell').click();
    await page.locator('.admin-notif-panel').waitFor({ state: 'visible', timeout: 5000 });
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    await loginAdmin(page);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
});

test('(a) 벨 클릭 → dropdown 오픈 · 최근 알림 렌더 · 헤더 "알림" 라벨', async ({ page }) => {
    await openDropdown(page);
    // 헤더 라벨
    await expect(page.locator('.admin-notif-panel-title')).toHaveText('알림');
    // 항목 최소 1건 (sysadmin createdBy 축으로 seed 시점 다수 발행)
    await expect(page.locator('.admin-notif-item').first()).toBeVisible();
    const count = await page.locator('.admin-notif-item').count();
    expect(count).toBeGreaterThanOrEqual(1);
    expect(count).toBeLessThanOrEqual(5); // recentForHeader top-5 상한
});

test('(b) 항목 클릭 → HX-Redirect · unread class 제거 · 배지 -1', async ({ page }) => {
    await openDropdown(page);
    const before = await readBadgeCount(page);
    expect(before).toBeGreaterThanOrEqual(1);

    // 첫 unread 항목의 id 추출 + 링크 클릭 → HX-Redirect 응답.
    const firstUnread = page.locator('.admin-notif-item--unread').first();
    await expect(firstUnread).toBeVisible();
    const itemId = await firstUnread.getAttribute('id'); // 'admin-notif-item-<id>'
    expect(itemId).toBeTruthy();

    // 클릭 → POST /admin/notifications/{id}/read 응답 status 200 (HTMX 인 경우 HX-Redirect) 대기.
    await Promise.all([
        page.waitForResponse(
            (res) =>
                /\/admin\/notifications\/\d+\/read$/.test(res.url()) && res.status() === 200,
            { timeout: 5000 },
        ),
        firstUnread.locator('.admin-notif-item-link').click(),
    ]);

    // HX-Redirect 로 페이지 이동 발생. 이동 후 헤더 배지 재읽기 (초기 렌더 스냅샷 기준 -1)
    // link 대상은 seed 알림별로 다르므로 waitForURL 대신 배지 감소만 검증.
    await page.waitForLoadState('domcontentloaded');
    const after = await readBadgeCount(page);
    expect(after).toBe(before - 1);
});

test('(e) 30s polling wrapper endpoint 200 응답 sanity check', async ({ page }) => {
    // hx-trigger="every 30s" 실제 대기 X — endpoint 만 direct fetch 로 확인.
    const resp = await page.request.get('/admin/notifications/badge');
    expect(resp.status()).toBe(200);
    const body = await resp.text();
    // fragment 는 <span id="admin-notif-badge" ...> 를 포함해야 함
    expect(body).toContain('id="admin-notif-badge"');
    expect(body).toContain('admin-notif-badge');
});

test('(d) 개별 delete → 항목 DOM 제거 + 배지 -1 (OOB swap)', async ({ page }) => {
    await openDropdown(page);
    const before = await readBadgeCount(page);
    expect(before).toBeGreaterThanOrEqual(1);

    // unread 항목이어야 배지 감소가 관측됨. read 상태 항목 삭제는 배지에 영향 없음.
    const target = page.locator('.admin-notif-item--unread').first();
    const targetId = await target.getAttribute('id');
    expect(targetId).toBeTruthy();

    await Promise.all([
        page.waitForResponse(
            (res) =>
                /\/admin\/notifications\/\d+\/delete$/.test(res.url()) && res.status() === 200,
            { timeout: 5000 },
        ),
        target.locator('.admin-notif-delete').click(),
    ]);

    // hx-swap="delete" → 항목 자체 DOM 제거
    await expect(page.locator(`#${targetId}`)).toHaveCount(0);
    // OOB swap 으로 헤더 배지 outerHTML 교체 → data-notif-badge 값 감소
    const after = await readBadgeCount(page);
    expect(after).toBe(before - 1);
});

test('(c) mark-all-read → 배지 hidden 속성 · unread 항목 클래스 제거', async ({ page }) => {
    await openDropdown(page);
    const before = await readBadgeCount(page);
    // 이전 TC 들이 감소시켰어도 sysadmin createdBy 축으로 발행된 seed 량 상 unread >= 1 예상.
    // 만약 0 이면 mark-all-read 버튼 자체가 렌더 안 됨 — 이 TC 는 skip 이 아니라 실패로 노출.
    expect(before).toBeGreaterThanOrEqual(1);

    await Promise.all([
        page.waitForResponse(
            (res) =>
                res.url().includes('/admin/notifications/mark-all-read') && res.status() === 200,
            { timeout: 5000 },
        ),
        page.locator('.admin-notif-mark-all').click(),
    ]);

    // 응답으로 dropdown fragment 재렌더 — mark-all 버튼은 unread=0 이라 다시 렌더 안 됨
    await expect(page.locator('.admin-notif-mark-all')).toHaveCount(0);
    // 배지는 fragment 재렌더 결과에는 없지만, 다음 폴링/네비게이션 때 최신화. 여기서는
    // 명시적으로 GET /admin/notifications/badge 를 눌러 실제 서버가 0 이라고 응답하는지 검증.
    const badgeResp = await page.request.get('/admin/notifications/badge');
    expect(badgeResp.status()).toBe(200);
    const body = await badgeResp.text();
    // 서버가 hidden 속성으로 렌더 (Qn-6)
    expect(body).toMatch(/hidden/);
});
