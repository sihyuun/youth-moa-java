import { test, expect } from '@playwright/test';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * F2 헤더 알림 종 여정.
 *
 * 시드 (DataInitializer.seedNotifications, F2 이후):
 *  - seed1 · seed30 에 각 4건 알림 (APPLICATION_APPROVED / PROGRAM_DEADLINE_NEAR / WELCOME / APPLICATION_CANCELLED)
 *  - 초기 read=false (unread 4)
 *
 * 다른 spec 과 충돌 최소화를 위해 이 spec 은 seed1 사용.
 * (find-account.spec 는 seed15, apply.spec 는 seed30 사용)
 *
 * ⚠️ Selector 주의: `.header-bell-dot` 는 헤더 종 아이콘 위 dot + 드롭다운 fragment OOB span
 * 두 위치에 존재. 헤더의 dot 만 잡으려면 `.header-bell-menu > button .header-bell-dot` 로 범위 축소.
 */

const USER_EMAIL = seedEmail(1);
const HEADER_DOT_SELECTOR = '.header-bell-menu > button .header-bell-dot:not(.header-bell-dot--hidden)';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('비로그인 상태에서는 헤더에 종 아이콘이 노출되지 않는다', async ({ page }) => {
    await page.goto('/', { waitUntil: 'commit' });
    await expect(page.locator('.header-bell-menu')).toHaveCount(0);
});

test('로그인 시 헤더에 종 + unread dot 이 노출된다', async ({ page }) => {
    await login(page, USER_EMAIL);
    await expect(page.locator('.header-bell-menu')).toBeVisible();
    // 헤더 종 자체의 dot 만 (드롭다운 fragment 내 OOB span 제외)
    await expect(page.locator(HEADER_DOT_SELECTOR)).toBeVisible();
});

test('종 클릭 시 드롭다운 열리고 4건 알림 + unread 뱃지 렌더', async ({ page }) => {
    await login(page, USER_EMAIL);
    // U-COMMON-02 (2026-07-16): hover → click 토글로 변경됨.
    await page.locator('.header-bell-trigger').click();

    await expect(page.locator('.notif-panel')).toBeVisible();
    await expect(page.locator('.notif-panel-badge').first()).toContainText('4');
    await expect(page.locator('.notif-item')).toHaveCount(4);
});

test('알림 항목 각각에 title · message · link 가 렌더된다', async ({ page }) => {
    await login(page, USER_EMAIL);
    // U-COMMON-02 (2026-07-16): hover → click 토글로 변경됨.
    await page.locator('.header-bell-trigger').click();
    await expect(page.locator('.notif-panel')).toBeVisible();

    // toContainText 는 auto-wait 지원 (innerText 는 visibility:hidden 상태 시 빈 문자열 반환)
    const panel = page.locator('.notif-panel');
    await expect(panel).toContainText('프로그램 신청 승인');
    await expect(panel).toContainText('마감 임박');
    await expect(panel).toContainText('공지사항');
    await expect(panel).toContainText('신청 취소 처리');
});

test('"모두 읽음" 클릭 시 HTMX OOB swap 으로 헤더 dot 이 사라진다', async ({ page }) => {
    await login(page, USER_EMAIL);
    // U-COMMON-02 (2026-07-16): hover → click 토글로 변경됨.
    await page.locator('.header-bell-trigger').click();
    await expect(page.locator('.notif-panel')).toBeVisible();

    // 초기 헤더 dot 존재 확인
    await expect(page.locator(HEADER_DOT_SELECTOR)).toBeVisible();

    // "모두 읽음" form submit — HTMX POST /notifications/read-all
    await page.locator('form.notif-inline-form button').click();

    // OOB swap 후 헤더의 dot 이 hidden 클래스로 전환 (attached 여부만 확인)
    await expect(
        page.locator('.header-bell-menu > button .header-bell-dot.header-bell-dot--hidden'),
    ).toBeAttached({ timeout: 3000 });
});

test('알림 전체보기 링크 → /notifications 페이지 (인증 필요)', async ({ page }) => {
    await login(page, USER_EMAIL);
    // U-COMMON-02 (2026-07-16): hover → click 토글로 변경됨.
    await page.locator('.header-bell-trigger').click();
    await expect(page.locator('.notif-panel')).toBeVisible();
    await page.locator('.notif-panel-footer').click();
    await page.waitForURL('/notifications');
    await expect(page.locator('.notif-page-title, h1, h2').first()).toBeVisible();
});
