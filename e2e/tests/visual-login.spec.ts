import { expect, test } from '@playwright/test';
import { loginContract } from '../contracts/login';
import { runContract, writeGapReport } from '../contracts/runner';

test('로그인 화면 디자인 계약 — 비로그인', async ({ page }) => {
    await page.setViewportSize({
        width: loginContract.viewport.width,
        height: loginContract.viewport.height,
    });
    await page.goto(loginContract.path, { waitUntil: 'domcontentloaded' });
    const anon = await runContract(page, loginContract, 'anon');
    writeGapReport(loginContract, { anon });
});

/**
 * Q-3 (2026-08-10) — input filled 상태 검증.
 * prototype L105 inputStyle: value 있으면 배경 white + border primary.
 * static contract 로 표현 불가해 별도 spec 블록으로 인터랙티브 검증.
 */
test('로그인 화면 filled 상태 — input 에 값 주입 후 primary border + white bg', async ({ page }) => {
    await page.setViewportSize({
        width: loginContract.viewport.width,
        height: loginContract.viewport.height,
    });
    await page.goto(loginContract.path, { waitUntil: 'domcontentloaded' });

    const input = page.locator('input[name="username"]');
    await input.fill('user@example.com');
    // blur — filled 상태는 focus 여부와 독립이어야 하므로 다른 요소로 포커스 이동
    await page.locator('body').click({ position: { x: 10, y: 10 } });
    // transition 150ms 종료 대기 (border-color · background 전환). 여유 있게 350ms.
    await page.waitForTimeout(350);

    // 최신 Chromium 이 computed color 를 oklab(...) 로 반환하는 경우가 있어
    // 문자열 리터럴 비교는 불안정. canvas 2D 로 정규화해 픽셀 rgb 로 확인한다.
    const { bg, border } = await input.evaluate((el) => {
        const s = window.getComputedStyle(el);
        const toPixel = (cssColor: string): [number, number, number] => {
            const c = document.createElement('canvas');
            c.width = c.height = 1;
            const ctx = c.getContext('2d')!;
            ctx.fillStyle = cssColor;
            ctx.fillRect(0, 0, 1, 1);
            const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
            return [r, g, b];
        };
        return { bg: toPixel(s.backgroundColor), border: toPixel(s.borderColor) };
    });

    // soft assertion: 갭 있으면 리포트로 남기되 CI 블로킹은 하지 않음 (contracts 프로젝트 논블로킹)
    // canvas 를 통한 srgb ↔ oklab 라운드트립 오차를 허용해 ±5 tolerance 로 비교.
    const near = (px: [number, number, number], target: [number, number, number]) =>
        px.every((v, i) => Math.abs(v - target[i]) <= 5);
    expect.soft(near(bg, [255, 255, 255]), `filled bg=white (실제 ${bg.join(',')})`).toBe(true);
    expect.soft(near(border, [63, 48, 233]), `filled border=primary #3F30E9 (실제 ${border.join(',')})`).toBe(true);
});
