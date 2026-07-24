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
 * detail.html 의 CapacityBar 는 fragment 재사용이 아니라 detail 전용 마크업
 * (.detail-capacity-bar-fill) 로 width 만 반영. 색상 상태 클래스는 detail 은 붙지 않고
 * 카드 CapacityBar 는 capacity-bar-fill--error / --warning / --primary 로 구분됨.
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
    // #90 CapacityBar 개편 (2026-07-13): 클래스명 detail-capacity-bar-fill → capacity-bar-fill,
    // 카운트 표시 컨테이너는 .detail-capacity-count (28/30).
    const fill = page.locator('.capacity-bar-fill');
    await expect(fill).toBeVisible();
    const style = await fill.getAttribute('style');
    // width 는 정수 % 로 렌더 (문자열 매칭)
    expect(style).toMatch(/width:\s*9[0-9]%/);
    // 신청/정원 표기 (.detail-capacity-count 안에 <strong>28</strong> · <span>30</span>)
    const count = page.locator('.detail-capacity-count');
    await expect(count).toContainText('28');
    await expect(count).toContainText('30');
});

test('ENDED 프로그램(id=4) 은 "비슷한 프로그램 보기" outline anchor 로 대체된다', async ({ page }) => {
    await gotoDetail(page, 4);
    // F0f-fix-3 (2026-07-20 PR #107): CLOSED → ENDED enum 리네임 + 종료 CTA 개편.
    // 신청 button 대신 outline anchor 로 프로그램 목록 유도.
    const cta = page.locator('a.btn-outline-primary.detail-cta');
    await expect(cta).toBeVisible();
    await expect(cta).toContainText('비슷한 프로그램 보기');
    await expect(cta).toHaveAttribute('href', /\/programs/);
    // 신청 button 은 없어야 함
    await expect(page.locator('button.detail-cta')).toHaveCount(0);
});
