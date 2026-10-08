import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * F-admin-scripts-fragment 회귀 방어 spec.
 *
 * admin 15개 페이지에 `admin/fragments/_head-scripts.html` 이 정상 삽입돼
 *   - CSRF meta 2개
 *   - HTMX 로드 (window.htmx)
 *   - common-ui 로드 (window.Toast)
 * 3가지가 모두 살아 있는지 순회 검증한다.
 *
 * + notice/form 인라인 CSRF 훅 제거(P-A) 회귀 방어:
 *   HTMX 요청 발송 시 X-CSRF-TOKEN 헤더가 자동 부착되는지 실측.
 * + dashboard 알림 벨 hx-get 정상 동작(A7-e2e-suite FAIL-1) 회귀 방어.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

/**
 * 프로그램/공지/약관/자격요건/동적필드/사용자 각 form 은 id 파라미터가 필요하지 않은 new 모드 URL 로 순회.
 * (실제 라우팅 없이 head 스크립트 로드만 검증하는 목적)
 */
const ADMIN_PAGES: { name: string; url: string }[] = [
    { name: 'dashboard', url: '/admin' },
    { name: 'stats', url: '/admin/stats' },
    { name: 'notice/list', url: '/admin/notices' },
    { name: 'notice/form(new)', url: '/admin/notices/new' },
    { name: 'program/list', url: '/admin/programs' },
    { name: 'program/form(new)', url: '/admin/programs/new' },
    // FOLLOW-admin-program-detail-readonly (2026-10-07): 상세·편집 분리 후 신규 경로 2종.
    { name: 'program/detail', url: '/admin/programs/1' },
    { name: 'program/form(edit)', url: '/admin/programs/1/edit' },
    { name: 'program-dynamic-field/list', url: '/admin/programs/1/dynamic-fields' },
    { name: 'program-dynamic-field/form(new)', url: '/admin/programs/1/dynamic-fields/new' },
    { name: 'program-eligibility/form', url: '/admin/programs/1/eligibility' },
    { name: 'term/list', url: '/admin/terms' },
    { name: 'term/form(new)', url: '/admin/terms/new' },
    { name: 'user/list', url: '/admin/users' },
    { name: 'user/new', url: '/admin/users/new' },
];

for (const p of ADMIN_PAGES) {
    test(`${p.name} — CSRF meta + HTMX + common-ui 로드`, async ({ page }) => {
        await loginAdmin(page);
        const resp = await page.goto(p.url);
        expect(resp?.status(), `${p.url} status`).toBeLessThan(400);

        // CSRF meta 2개
        await expect(page.locator('head meta[name="_csrf"]')).toHaveCount(1);
        await expect(page.locator('head meta[name="_csrf_header"]')).toHaveCount(1);

        // HTMX / common-ui 는 defer 로드라 스크립트 실행 완료 대기
        await page.waitForFunction(() => typeof (window as any).htmx === 'object', null, { timeout: 5000 });
        await page.waitForFunction(() => typeof (window as any).Toast === 'object', null, { timeout: 5000 });
    });
}

test('notice/form 인라인 CSRF 훅 제거 — HTMX 요청에 X-CSRF-TOKEN 헤더 자동 부착 (P-A)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices/new');
    await page.waitForFunction(() => typeof (window as any).htmx === 'object');

    // 임시 HTMX 요청 유도 → configRequest 이벤트에서 헤더 추출
    const csrfHeaderPresent = await page.evaluate(() => {
        return new Promise<boolean>((resolve) => {
            const headerMeta = document.querySelector('meta[name="_csrf_header"]') as HTMLMetaElement | null;
            const headerName = headerMeta?.content;
            if (!headerName) return resolve(false);
            document.body.addEventListener(
                'htmx:configRequest',
                (evt: any) => {
                    const headers = evt?.detail?.headers ?? {};
                    resolve(Boolean(headers[headerName]));
                },
                { once: true },
            );
            // htmx-csrf.js 는 GET/HEAD 를 skip 하므로 POST 로 유도
            (window as any).htmx.ajax('POST', '/admin/__csrf-probe__', { target: 'body', swap: 'none' });
            setTimeout(() => resolve(false), 3000);
        });
    });
    expect(csrfHeaderPresent, 'X-CSRF-TOKEN 헤더가 자동 부착돼야 함 (htmx-csrf.js 대체 동작)').toBe(true);
});

test('dashboard 알림 벨 — hx-get 트리거 후 dropdown 대체 (A7-e2e-suite FAIL-1)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin');
    await page.waitForFunction(() => typeof (window as any).htmx === 'object');

    const bell = page.locator('.admin-header-bell');
    await expect(bell).toBeVisible();

    // 벨 클릭 → hx-get /admin/notifications/dropdown 요청 완료 대기
    const respPromise = page.waitForResponse((r) => r.url().includes('/admin/notifications/dropdown') && r.status() === 200);
    await bell.click();
    await respPromise;

    // dropdown 컨테이너에 innerHTML swap 이 일어나 자식이 채워짐
    await expect
        .poll(async () => await page.locator('#admin-notif-dropdown').evaluate((el) => el.childElementCount), {
            timeout: 5000,
        })
        .toBeGreaterThan(0);
});
