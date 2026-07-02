import { test, expect, type Page } from '@playwright/test';

/**
 * 프로그램 상세 페이지 렌더 검증.
 *
 * 시드 프로그램 상태(DataInitializer 기준):
 *  - id=1 ACTIVE (28/30 = 93% → 마감임박)
 *  - id=4 CLOSED (endDate 지남)
 *  - id=5 UPCOMING (startDate 미래)
 *
 * detail.html 의 CapacityBar 는 fragment 재사용이 아니라 detail 전용 마크업
 * (.detail-capacity-bar-fill) 로 width 만 반영. 색상 상태 클래스는 detail 은 붙지 않고
 * 카드 CapacityBar 는 capacity-bar-fill--error / --warning / --primary 로 구분됨.
 */

async function gotoDetail(page: Page, id: number) {
    await page.goto(`/programs/${id}`, { waitUntil: 'commit' });
}

test.beforeEach(async ({ page }) => {
    await page.route(/^https?:\/\/(?!localhost)/, route => route.abort());
});

test('프로그램 상세 기본 렌더 — 타이틀·상태 뱃지·기관·지역', async ({ page }) => {
    await gotoDetail(page, 1);
    await expect(page).toHaveTitle(/취업역량 강화 워크숍/);
    await expect(page.locator('.detail-title')).toContainText('취업역량 강화 워크숍');
    await expect(page.locator('.detail-subtitle')).toContainText('내일스퀘어 양평');
    // ACTIVE 뱃지 (label 은 ProgramStatus enum 참조)
    await expect(page.locator('.detail-badges .status-badge')).toBeVisible();
});

test('CapacityBar 가 신청 비율에 맞는 width 로 채워진다 (id=1, 28/30 ≈ 93%)', async ({ page }) => {
    await gotoDetail(page, 1);
    const fill = page.locator('.detail-capacity-bar-fill');
    await expect(fill).toBeVisible();
    const style = await fill.getAttribute('style');
    // width 는 정수 % 로 렌더 (문자열 매칭)
    expect(style).toMatch(/width:\s*9[0-9]%/);
    // 화면 우측 상단의 신청/정원 표기도 확인
    await expect(page.locator('.detail-capacity')).toContainText('28');
    await expect(page.locator('.detail-capacity')).toContainText('30');
});

test('CLOSED 프로그램(id=4) 은 신청 CTA 가 사라지고 "빈자리 알림 받기" 가 disabled 로 노출된다', async ({ page }) => {
    await gotoDetail(page, 4);
    // ACTIVE 신청 CTA (a.btn-primary.detail-cta) 는 없어야 함
    await expect(page.locator('a.detail-cta')).toHaveCount(0);
    const closedBtn = page.locator('button.detail-cta');
    await expect(closedBtn).toBeVisible();
    await expect(closedBtn).toContainText('빈자리 알림 받기');
    await expect(closedBtn).toBeDisabled();
    // 상태 카드 클래스 확인
    await expect(page.locator('.detail-status-card--closed')).toBeVisible();
});
