import { expect, test } from '@playwright/test';

/**
 * 정책 페이지 3종 모바일 대응 (< 768px) 회귀 테스트.
 * HANDOFF.md L643 "제목 헤더+본문+하단 관련 문서 링크".
 *
 * 데스크톱(1440) 에서는 사이드바 노출·모바일 헤더 숨김,
 * 모바일(375) 에서는 사이드바 숨김·모바일 헤더 노출·관련 문서 링크 2개(현재 페이지 제외).
 */
for (const [path, title] of [
    ['/privacy', '개인정보처리방침'],
    ['/terms', '이용약관'],
    ['/email-policy', '이메일 무단 수집거부'],
] as const) {
    test(`${title} — 데스크톱 1440: 사이드바 노출 · 모바일 헤더 숨김`, async ({ page }) => {
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.goto(path);
        await expect(page.locator('.policy-sidebar')).toBeVisible();
        await expect(page.locator('.policy-mobile-header')).toBeHidden();
        await expect(page.locator('.policy-mobile-related')).toBeHidden();
    });

    test(`${title} — 모바일 375: 사이드바 숨김 · 헤더 노출 · 관련 문서 2개`, async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 812 });
        await page.goto(path);
        await expect(page.locator('.policy-sidebar')).toBeHidden();

        // 모바일 상단 헤더: 뒤로가기 버튼 + 현재 페이지 타이틀
        const header = page.locator('.policy-mobile-header');
        await expect(header).toBeVisible();
        await expect(header.locator('.policy-mobile-title')).toHaveText(title);
        await expect(header.locator('.policy-mobile-back')).toBeVisible();

        // 하단 관련 문서: 현재 페이지 제외 2개 (전체 3개 중)
        const related = page.locator('.policy-mobile-related');
        await expect(related).toBeVisible();
        const links = related.locator('.policy-mobile-related-link');
        await expect(links).toHaveCount(2);
        // 현재 페이지 이름은 관련 문서에 없어야 함
        for (const t of ['개인정보처리방침', '이용약관', '이메일 무단 수집거부']) {
            if (t === title) {
                await expect(links.filter({ hasText: t })).toHaveCount(0);
            } else {
                await expect(links.filter({ hasText: t })).toHaveCount(1);
            }
        }
    });
}

/**
 * 브레이크포인트 경계 회귀 (CSS `@media (max-width: 768px)`).
 * 768 이하 → 모바일 layout, 769 이상 → 데스크톱 layout.
 * 표준값에 오타·off-by-one 방지 목적.
 */
test('브레이크포인트 경계 — 767: 모바일 / 768: 모바일 / 769: 데스크톱', async ({ page }) => {
    // 767 → 모바일
    await page.setViewportSize({ width: 767, height: 900 });
    await page.goto('/privacy');
    await expect(page.locator('.policy-sidebar')).toBeHidden();
    await expect(page.locator('.policy-mobile-header')).toBeVisible();

    // 768 (경계 포함) → 모바일
    await page.setViewportSize({ width: 768, height: 900 });
    await page.goto('/privacy');
    await expect(page.locator('.policy-sidebar')).toBeHidden();
    await expect(page.locator('.policy-mobile-header')).toBeVisible();

    // 769 → 데스크톱
    await page.setViewportSize({ width: 769, height: 900 });
    await page.goto('/privacy');
    await expect(page.locator('.policy-sidebar')).toBeVisible();
    await expect(page.locator('.policy-mobile-header')).toBeHidden();
});

test('푸터 3개 링크 실제 URL 연결', async ({ page }) => {
    await page.goto('/');
    const links = page.locator('.footer-links a');
    await expect(links).toHaveCount(3);
    await expect(links.nth(0)).toHaveAttribute('href', '/privacy');
    await expect(links.nth(1)).toHaveAttribute('href', '/terms');
    await expect(links.nth(2)).toHaveAttribute('href', '/email-policy');
});
