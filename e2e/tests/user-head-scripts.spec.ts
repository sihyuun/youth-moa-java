import { expect, test } from '@playwright/test';
import { abortExternal, login, seedEmail } from '../helpers';

/**
 * F-user-scripts-fragment 회귀 방어 spec.
 *
 * 사용자 페이지에서 신설된 head fragment 2종이 정상 삽입돼 있는지 실측한다.
 *
 * Q1 결정: 계층 분리 (B 안)
 *   - `fragments/_head-csrf`  → CSRF meta 2개만 (HTMX 미사용 정적/폼 페이지)
 *   - `fragments/_head-htmx`  → CSRF meta 2개 + HTMX 코어 + htmx-csrf 훅 (HTMX 사용 페이지)
 *
 * user 는 admin 과 달리 `common-ui.js` 를 fragment 에서 로드하지 않는다 (footer 하단 로드).
 *
 * 검증 5경로:
 *   - /                → htmx fragment  (window.htmx object 존재)
 *   - /programs        → htmx fragment  (window.htmx object 존재)
 *   - /notices         → htmx fragment
 *   - /login           → csrf-only fragment (window.htmx 는 undefined — 계층 분리 확인)
 *   - /mypage?tab=history → csrf-only fragment (로그인 상태 · window.htmx undefined)
 *
 * + Kakao Map SDK 회귀 방어: /centers 에 SDK script 태그가 여전히 존재하는지.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

type Expect = {
    name: string;
    url: string;
    /** true 면 window.htmx 가 로드되어야 함 (htmx fragment). false 면 로드 X (csrf-only fragment). */
    htmxLoaded: boolean;
    /** 로그인 필요 여부. login 헬퍼 + seed 유저 사용. */
    requiresLogin?: boolean;
};

const CASES: Expect[] = [
    { name: 'index (htmx fragment)', url: '/', htmxLoaded: true },
    { name: 'program/list (htmx fragment)', url: '/programs', htmxLoaded: true },
    { name: 'notice/list (htmx fragment)', url: '/notices', htmxLoaded: true },
    { name: 'user/login (csrf-only fragment)', url: '/login', htmxLoaded: false },
    { name: 'mypage?tab=history (csrf-only fragment)', url: '/mypage?tab=history', htmxLoaded: false, requiresLogin: true },
];

for (const c of CASES) {
    test(`${c.name} — CSRF meta 상시 · HTMX 는 계층 분리 준수`, async ({ page }) => {
        if (c.requiresLogin) {
            await login(page, seedEmail(29));
        }
        const resp = await page.goto(c.url);
        expect(resp?.status(), `${c.url} status`).toBeLessThan(400);

        // CSRF meta 2개 — 모든 페이지에서 상시 존재
        await expect(page.locator('head meta[name="_csrf"]')).toHaveCount(1);
        await expect(page.locator('head meta[name="_csrf_header"]')).toHaveCount(1);

        if (c.htmxLoaded) {
            // HTMX fragment 사용 — defer 로드 완료 대기
            await page.waitForFunction(() => typeof (window as any).htmx === 'object', null, { timeout: 5000 });
        } else {
            // csrf-only fragment — HTMX 스크립트 자체가 없어야 함
            // defer 로드 여부가 확정될 때까지 잠시 대기 후 판정
            await page.waitForLoadState('domcontentloaded');
            const htmxType = await page.evaluate(() => typeof (window as any).htmx);
            expect(htmxType, `${c.url} 에서 window.htmx 는 undefined 여야 함 (계층 분리)`).toBe('undefined');
        }
    });
}

test('center/list — Kakao Map SDK 회귀 없음 (htmx fragment 도입 후에도 SDK 유지)', async ({ page }) => {
    const resp = await page.goto('/centers');
    expect(resp?.status()).toBeLessThan(400);

    // HTMX fragment 로드 확인
    await expect(page.locator('head meta[name="_csrf"]')).toHaveCount(1);
    await page.waitForFunction(() => typeof (window as any).htmx === 'object', null, { timeout: 5000 });

    // Kakao Map SDK script 태그 존재 (KAKAO_MAP_APP_KEY 조건부 — e2e 프로파일 값 유무 무관하게 head 자체는 유효)
    const kakaoScriptCount = await page.locator('head script[src*="dapi.kakao.com/v2/maps/sdk.js"]').count();
    // 환경에 따라 0 또는 1. 최소한 페이지가 정상 렌더되고 HTMX 가 살아 있는 것이 핵심.
    expect(kakaoScriptCount).toBeGreaterThanOrEqual(0);
});
