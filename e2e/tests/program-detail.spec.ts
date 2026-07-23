import { test, expect, type Page } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * 프로그램 상세 페이지 렌더 검증.
 *
 * 시드 프로그램 상태(DataInitializer 기준):
 *  - id=1 ACTIVE (28/30 = 93% → 마감임박)
 *  - id=4 CLOSED (endDate 지남)
 *  - id=5 UPCOMING (startDate 미래)
 *
 * detail.html 의 모집 상태 카드는 PR #90 (D5) 부터 fragments/capacity-bar.html 의
 * detailCapacityBar fragment 를 재사용한다. 구조:
 *   .detail-capacity-box(--emphasized | --muted)
 *     ├ .detail-capacity-header > .detail-capacity-headline--{color} + .detail-capacity-count
 *     ├ .capacity-bar > .capacity-bar-fill--{color} (style width:N%)
 *     └ .detail-capacity-subtext
 * colorClass 는 카드와 동일한 ProgramCardDto 임계값 (90%≤ error / 70%≤ warning / primary / CLOSED muted).
 */

async function gotoDetail(page: Page, id: number) {
    await page.goto(`/programs/${id}`, { waitUntil: 'commit' });
}

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
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
    const fill = page.locator('.detail-capacity-box .capacity-bar-fill');
    await expect(fill).toBeVisible();
    const style = await fill.getAttribute('style');
    // width 는 정수 % 로 렌더 (문자열 매칭)
    expect(style).toMatch(/width:\s*9[0-9]%/);
    // 93% ≥ 90% → 마감임박(error) 색상 클래스
    await expect(fill).toHaveClass(/capacity-bar-fill--error/);
    // 모집 상태 카드 우측의 신청/정원 표기 (28 / 30명)
    await expect(page.locator('.detail-capacity-count')).toContainText('28');
    await expect(page.locator('.detail-capacity-count')).toContainText('30');
});

test('CLOSED 프로그램(id=4) 은 신청 CTA 가 사라지고 "빈자리 알림 받기" 가 disabled 로 노출된다', async ({ page }) => {
    await gotoDetail(page, 4);
    // ACTIVE 신청 CTA (a.btn-primary.detail-cta) 는 없어야 함
    await expect(page.locator('a.detail-cta')).toHaveCount(0);
    const closedBtn = page.locator('button.detail-cta');
    await expect(closedBtn).toBeVisible();
    await expect(closedBtn).toContainText('빈자리 알림 받기');
    await expect(closedBtn).toBeDisabled();
    // 모집 상태 카드 — 마감 케이스는 배경 강조 없는 muted 박스 + "모집 마감" 헤드라인
    await expect(page.locator('.detail-capacity-box--muted')).toBeVisible();
    await expect(page.locator('.detail-capacity-headline')).toContainText('모집 마감');
});
