import { test, expect } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * F0f PR-1 QA — 사용자 시각 확인용 스크린샷 캡처.
 * 실제 회귀 assert 는 programs-calendar.spec.ts 에서 담당. 이 파일은 시각 산출물 전용.
 */

test.describe.configure({ mode: 'serial' });

test.describe('F0f QA 스크린샷', () => {
    test.beforeEach(async ({ page }) => {
        await abortExternal(page);
    });

    test('툴바 정중앙 캡처', async ({ page }) => {
        await page.goto('/programs?view=calendar', { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-toolbar')).toBeVisible();
        await page.locator('.program-calendar-toolbar').screenshot({
            path: 'screenshots/qa/f0f-캘린더-툴바정중앙.png',
        });
    });

    test('UPCOMING chip 오렌지 캡처 (M/D 오픈 · secondary color)', async ({ page }) => {
        // 다음 달로 이동하면 시드가 대부분 UPCOMING
        const now = new Date();
        let nextY = now.getFullYear();
        let nextM = now.getMonth() + 2;
        if (nextM > 12) { nextM -= 12; nextY += 1; }
        await page.goto(`/programs?view=calendar&year=${nextY}&month=${nextM}`, { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        const pillCell = page.locator('.program-calendar-cell[data-in-month="true"]:has(.program-calendar-pill)').first();
        const cnt = await pillCell.count();
        test.skip(cnt === 0, '다음 달 UPCOMING pill 없음');
        await pillCell.click();
        await expect(page.locator('#program-calendar-panel')).toBeVisible();
        await page.locator('.program-calendar-panel').screenshot({
            path: 'screenshots/qa/f0f-UPCOMING-chip-오렌지.png',
        });
    });

    test('패널 특정 날짜만 노출 캡처', async ({ page }) => {
        await page.goto('/programs?view=calendar', { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        const pillCell = page.locator('.program-calendar-cell').filter({
            has: page.locator('.program-calendar-pill'),
        }).first();
        await pillCell.click();
        await expect(page.locator('#program-calendar-panel')).toBeVisible();
        await page.locator('.program-calendar-panel').screenshot({
            path: 'screenshots/qa/f0f-패널-특정날짜만.png',
        });
    });

    test('캘린더 진입 데스크톱 전경 캡처 (밑줄 없음·시작일 라벨 없음·툴바 정렬)', async ({ page }) => {
        await page.goto('/programs?view=calendar', { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        // view-toggle 도 포함되도록 filter-bar + 캘린더 layout 통째로 캡처
        await page.locator('.programs-main').screenshot({
            path: 'screenshots/qa/f0f-캘린더-진입-데스크톱.png',
            fullPage: false,
        });
    });

    test('빈 달 배너 종료 탭 캡처 (탭 이름 문구 · 라벨 부재)', async ({ page }) => {
        await page.goto('/programs?view=calendar&status=ended&year=2100&month=1', { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-empty-banner')).toBeVisible();
        await page.locator('.program-calendar-main').screenshot({
            path: 'screenshots/qa/f0f-빈달배너-종료탭.png',
        });
    });

    test('빈 날짜 empty 문구 캡처', async ({ page }) => {
        // status=ended + 미래 달로 pill 이 아예 없는 42셀 확보
        await page.goto('/programs?view=calendar&status=ended&year=2100&month=1', { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.program-calendar-grid')).toBeVisible();
        // pill 없는 셀 (in-month) 찾기
        const emptyCell = page.locator('.program-calendar-cell[data-in-month="true"]').filter({
            hasNot: page.locator('.program-calendar-pill'),
        }).first();
        const count = await emptyCell.count();
        test.skip(count === 0, '빈 in-month 셀 없음');
        await emptyCell.click();
        await expect(page.locator('#program-calendar-panel-empty')).toBeVisible();
        await page.locator('.program-calendar-panel').screenshot({
            path: 'screenshots/qa/f0f-빈날짜-empty문구.png',
        });
    });
});
