import { test, expect } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * F0e-2 — Hero 배경 6장 크로스페이드 로테이션 E2E.
 *
 * TC1: .hero-bg 6개 존재
 * TC2: 최초 로드 시 첫 번째만 .is-active
 * TC3: 9초 대기 후 두 번째만 .is-active
 * TC4: prefers-reduced-motion: reduce → 9초 대기 후에도 첫 번째만 active
 * TC5: transition-duration: 1.2s
 */

test.describe('F0e-2 Hero rotation', () => {
    test('TC1: .hero-bg 6개 존재', async ({ page }) => {
        await abortExternal(page);
        await page.goto('/', { waitUntil: 'commit' });
        await expect(page.locator('.hero .hero-bg')).toHaveCount(6);
    });

    test('TC2: 최초 로드 시 첫 번째만 is-active', async ({ page }) => {
        await abortExternal(page);
        await page.goto('/', { waitUntil: 'commit' });
        await expect(page.locator('.hero .hero-bg.is-active')).toHaveCount(1);
        await expect(page.locator('.hero .hero-bg').first()).toHaveClass(/is-active/);
    });

    test('TC3: 9초 대기 후 두 번째가 is-active', async ({ page }) => {
        await abortExternal(page);
        await page.goto('/', { waitUntil: 'commit' });
        await expect(page.locator('.hero .hero-bg').first()).toHaveClass(/is-active/);
        // 8초 로테이션 + 1초 여유
        await page.waitForTimeout(9000);
        await expect(page.locator('.hero .hero-bg').nth(1)).toHaveClass(/is-active/);
        await expect(page.locator('.hero .hero-bg').first()).not.toHaveClass(/is-active/);
    });

    test('TC4: prefers-reduced-motion: reduce → 로테이션 스킵', async ({ page }) => {
        await abortExternal(page);
        await page.emulateMedia({ reducedMotion: 'reduce' });
        await page.goto('/', { waitUntil: 'commit' });
        await expect(page.locator('.hero .hero-bg').first()).toHaveClass(/is-active/);
        await page.waitForTimeout(9000);
        // 여전히 첫 번째만 active
        await expect(page.locator('.hero .hero-bg').first()).toHaveClass(/is-active/);
        await expect(page.locator('.hero .hero-bg').nth(1)).not.toHaveClass(/is-active/);
    });

    test('TC5: .hero-bg transition-duration: 1.2s', async ({ page }) => {
        await abortExternal(page);
        await page.goto('/', { waitUntil: 'commit' });
        const duration = await page.locator('.hero .hero-bg').first().evaluate(
            (el) => window.getComputedStyle(el).transitionDuration,
        );
        expect(duration).toBe('1.2s');
    });
});
