/**
 * QA 리포트용 spec (A-admin-terms-crud, 2026-09-07) — spec §11 동적 검증 실측.
 * 각 시나리오는 콘솔로 실측값을 출력해 QA 리포트에 원본 인용한다.
 *
 * 격리: 새 세션이 필요할 때는 `browser.newContext()` 로 새 페이지를 만든다.
 * (page.context().clearCookies() 는 CDP 오류가 나는 경우가 있음.)
 */
import { expect, test } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    ADMIN_SYSTEM_EMAIL,
    abortExternal,
    login,
    loginAdmin,
    resetTerms,
    seedEmail,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    // 이전 시나리오가 생성한 신규 term 정리 (seed pollution 방지) — signup 회귀에 필요.
    // resetTerms 는 CSRF-exempt (__test__/**) 이므로 로그인 없이 가능.
    await page.request.post('/__test__/reset-terms').catch(() => {});
});

test.afterAll(async ({ browser }) => {
    const page = await browser.newPage();
    try {
        await loginAdmin(page);
        await resetTerms(page);
    } finally {
        await page.close();
    }
});

function alphaSuffix(): string {
    return String(Date.now()).slice(-8).replace(/[0-9]/g, d => String.fromCharCode(65 + Number(d)));
}

test('Qn-1 B RBAC — sysadmin GET/POST · center_admin GET+403 · USER GET 403', async ({ browser }) => {
    // 1) sysadmin: GET 200, POST 302
    let ctx = await browser.newContext();
    let page = await ctx.newPage();
    await abortExternal(page);
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL, ADMIN_SEED_PASS);
    let resp = await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });
    console.log('[Qn-1 sysadmin GET /admin/terms]', resp?.status());
    expect(resp?.status()).toBe(200);

    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const csrf = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfH = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';
    const code = `SYSA_${alphaSuffix()}`;
    const postResp = await page.request.post('/admin/terms', {
        form: { code, title: 'RBAC 검증', contentPath: '/terms', content: '<p>a</p>', sortOrder: '10', isActive: 'true' },
        headers: { [csrfH]: csrf },
        maxRedirects: 0,
    });
    console.log('[Qn-1 sysadmin POST /admin/terms] status=', postResp.status(), 'location=', postResp.headers()['location']);
    expect(postResp.status()).toBe(302);
    await ctx.close();

    // 2) center_admin: GET 200, POST 403
    ctx = await browser.newContext();
    page = await ctx.newPage();
    await abortExternal(page);
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    resp = await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });
    console.log('[Qn-1 center_admin GET /admin/terms]', resp?.status());
    expect(resp?.status()).toBe(200);

    await page.goto('/admin/terms/1', { waitUntil: 'domcontentloaded' });
    const csrf2 = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfH2 = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';
    const centerResp = await page.request.post('/admin/terms/1', {
        form: { title: '중앙관리자 시도', contentPath: '/terms', content: '<p>x</p>', sortOrder: '1', isActive: 'true' },
        headers: { [csrfH2]: csrf2 },
    });
    console.log('[Qn-1 center_admin POST /admin/terms/1]', centerResp.status());
    expect(centerResp.status()).toBe(403);
    await ctx.close();

    // 3) USER: GET 403
    ctx = await browser.newContext();
    page = await ctx.newPage();
    await abortExternal(page);
    await login(page, seedEmail(29));
    resp = await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });
    console.log('[Qn-1 USER GET /admin/terms]', resp?.status());
    expect(resp?.status()).toBe(403);
    await ctx.close();
});

test('Qn-2 B XSS sanitize — <script> 저장 시 제거 · signup 에 term.content data-* embed', async ({ browser }) => {
    // 등록
    const ctxA = await browser.newContext();
    const pageA = await ctxA.newPage();
    await abortExternal(pageA);
    await loginAdmin(pageA);
    await pageA.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const code = `XSS_${alphaSuffix()}`;
    await pageA.locator('input[name="code"]').fill(code);
    await pageA.locator('input[name="title"]').fill('XSS 테스트');
    await pageA.locator('input[name="contentPath"]').fill('/terms');
    await pageA.locator('textarea[name="content"]').fill('<p>정상</p><script>alert(\'xss\')</script>');
    await pageA.locator('input[name="sortOrder"]').fill('99');
    await pageA.locator('input[name="isActive"]').check();
    await pageA.locator('button[type="submit"]:has-text("등록")').click();
    await pageA.waitForURL(/\/admin\/terms\/\d+/);
    const savedContent = await pageA.locator('textarea[name="content"]').inputValue();
    console.log('[Qn-2 XSS] savedContent=', JSON.stringify(savedContent));
    expect(savedContent).toContain('정상');
    expect(savedContent).not.toContain('<script>');
    expect(savedContent).not.toContain('alert');
    await ctxA.close();

    // signup 화면 embed
    const ctxB = await browser.newContext();
    const pageB = await ctxB.newPage();
    await abortExternal(pageB);
    await pageB.goto('/signup', { waitUntil: 'domcontentloaded' });
    const embeds = await pageB.locator('[data-term-content]').count();
    console.log('[Qn-2 signup embed count]=', embeds);
    expect(embeds).toBeGreaterThanOrEqual(2);
    const firstEmbed = await pageB.locator('[data-term-content]').first().getAttribute('data-term-content');
    console.log('[Qn-2 firstEmbed head=]', firstEmbed?.slice(0, 80));
    await ctxB.close();
});

test('Qn-3 A — FK 없는 term 삭제 시 302 (FK 있는 케이스는 AdminTermServiceTest 로 계약 검증)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const code = `DEL_${alphaSuffix()}`;
    await page.locator('input[name="code"]').fill(code);
    await page.locator('input[name="title"]').fill('삭제 대상');
    await page.locator('input[name="contentPath"]').fill('/terms');
    await page.locator('textarea[name="content"]').fill('<p>d</p>');
    await page.locator('input[name="sortOrder"]').fill('50');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);
    const id = page.url().match(/\/admin\/terms\/(\d+)/)?.[1];
    const csrf = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfH = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';
    const delOk = await page.request.post(`/admin/terms/${id}/delete`, {
        headers: { [csrfH]: csrf },
        maxRedirects: 0,
    });
    console.log('[Qn-3 FK-free delete status=]', delOk.status(), 'location=', delOk.headers()['location']);
    expect(delOk.status()).toBe(302);
});

test('Qn-4 A empty state — 활성 term 0건 상태 /signup 200 (약관 섹션 렌더 스킵)', async ({ browser }) => {
    // 시드 SERVICE·PRIVACY 비활성화
    const ctxA = await browser.newContext();
    const pageA = await ctxA.newPage();
    await abortExternal(pageA);
    await loginAdmin(pageA);
    for (const id of [1, 2]) {
        await pageA.goto(`/admin/terms/${id}`, { waitUntil: 'domcontentloaded' });
        await pageA.locator('input[name="isActive"]').uncheck({ force: true }).catch(() => {});
        await pageA.locator('button[type="submit"]:has-text("수정 저장")').click();
        await pageA.waitForURL(/\/admin\/terms\/\d+/);
    }
    await ctxA.close();

    // 익명 사용자 signup 방문
    const ctxB = await browser.newContext();
    const pageB = await ctxB.newPage();
    await abortExternal(pageB);
    const resp = await pageB.goto('/signup', { waitUntil: 'domcontentloaded' });
    console.log('[Qn-4 /signup status=]', resp?.status());
    expect(resp?.status()).toBe(200);
    const rows = await pageB.locator('.signup-agree-row').count();
    console.log('[Qn-4 signup-agree-row count=]', rows);
    expect(rows).toBe(0);
    await ctxB.close();

    // 복원
    const ctxC = await browser.newContext();
    const pageC = await ctxC.newPage();
    await abortExternal(pageC);
    await loginAdmin(pageC);
    for (const id of [1, 2]) {
        await pageC.goto(`/admin/terms/${id}`, { waitUntil: 'domcontentloaded' });
        await pageC.locator('input[name="isActive"]').check({ force: true }).catch(() => {});
        await pageC.locator('button[type="submit"]:has-text("수정 저장")').click();
        await pageC.waitForURL(/\/admin\/terms\/\d+/);
    }
    await ctxC.close();
});

test('Qn-8 A reset-terms — 204 + seed 2건만 남음', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    const code = `RST_${alphaSuffix()}`;
    await page.locator('input[name="code"]').fill(code);
    await page.locator('input[name="title"]').fill('reset 검증');
    await page.locator('input[name="contentPath"]').fill('/terms');
    await page.locator('textarea[name="content"]').fill('<p>r</p>');
    await page.locator('input[name="sortOrder"]').fill('60');
    await page.locator('button[type="submit"]:has-text("등록")').click();
    await page.waitForURL(/\/admin\/terms\/\d+/);
    await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });
    const beforeCount = await page.locator('.admin-term-row:not(.admin-term-row--head)').count();
    console.log('[Qn-8 before reset rows=]', beforeCount);
    expect(beforeCount).toBeGreaterThan(2);

    const rst = await page.request.post('/__test__/reset-terms');
    console.log('[Qn-8 reset-terms status=]', rst.status());
    expect(rst.status()).toBe(204);

    await page.goto('/admin/terms', { waitUntil: 'domcontentloaded' });
    const afterCount = await page.locator('.admin-term-row:not(.admin-term-row--head)').count();
    console.log('[Qn-8 after reset rows=]', afterCount);
    expect(afterCount).toBe(2);
});
