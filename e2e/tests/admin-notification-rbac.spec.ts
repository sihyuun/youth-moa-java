/**
 * A7-e2e-suite (2026-09-28) — admin 알림 endpoint RBAC.
 *
 * SecurityConfig 의 /admin/** 매처 + AdminNotificationController 클래스 레벨 @PreAuthorize 로 이중 방어.
 * USER 세션이 admin 알림 endpoint 5개를 호출했을 때:
 *   - 미로그인 (anonymous) : /admin/login 으로 302 리다이렉트 (Spring Security 표준)
 *   - USER 로그인 (ROLE_USER 만 보유) : 403 (@PreAuthorize hasAnyRole SYSTEM_ADMIN, CENTER_ADMIN)
 *
 * endpoint 5개:
 *   GET  /admin/notifications/dropdown
 *   GET  /admin/notifications/badge
 *   POST /admin/notifications/mark-all-read
 *   POST /admin/notifications/{id}/read       — 임의 id (실 존재 무관, 인가가 먼저 걸린다)
 *   POST /admin/notifications/{id}/delete
 *
 * 알림 id 는 seed 로 발행된 값 대신 임의 정수 (999999) 사용 — 인가 차단이 존재 확인보다 먼저 일어나므로 status 만 검증.
 */
import { expect, test } from '@playwright/test';
import { abortExternal, login, seedEmail } from '../helpers';

const USER_EMAIL = seedEmail(30);
const DUMMY_ID = 999999;

const READ_ENDPOINTS = ['/admin/notifications/dropdown', '/admin/notifications/badge'];
const POST_ENDPOINTS = [
    '/admin/notifications/mark-all-read',
    `/admin/notifications/${DUMMY_ID}/read`,
    `/admin/notifications/${DUMMY_ID}/delete`,
];

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('anonymous — GET endpoint 는 302 로 admin 로그인 페이지로 리다이렉트', async ({ page }) => {
    for (const url of READ_ENDPOINTS) {
        const resp = await page.request.get(url, { maxRedirects: 0 });
        // Spring Security 는 anonymous access 를 302 로 login 페이지로 유도.
        // 401/403 인 경우도 명시적 차단이므로 통과. 200 만 아니면 됨.
        expect([302, 401, 403]).toContain(resp.status());
    }
});

test('anonymous — POST endpoint 는 302/401/403 (CSRF 이전 인증 차단)', async ({ page }) => {
    for (const url of POST_ENDPOINTS) {
        const resp = await page.request.post(url, { maxRedirects: 0 });
        expect([302, 401, 403]).toContain(resp.status());
    }
});

test('USER 세션 — GET endpoint 는 403 (@PreAuthorize hasAnyRole ADMIN)', async ({ page }) => {
    await login(page, USER_EMAIL);
    for (const url of READ_ENDPOINTS) {
        const resp = await page.request.get(url);
        expect(resp.status()).toBe(403);
    }
});

test('USER 세션 — POST endpoint 는 403', async ({ page }) => {
    await login(page, USER_EMAIL);
    for (const url of POST_ENDPOINTS) {
        const resp = await page.request.post(url);
        expect(resp.status()).toBe(403);
    }
});
