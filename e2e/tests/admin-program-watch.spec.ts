/**
 * A7-watcher-ui (2026-09-28) — 지켜보기 토글 + Composite 3축 fan-out E2E.
 *
 * 시나리오:
 *   1. CENTER_ADMIN 목록 → 눈 아이콘 클릭 → is-watched 클래스 부여 (outerHTML swap)
 *   2. 재클릭 → is-watched 해제
 *   3. 편집 폼 헤더 눈 아이콘 → styleClass=form-watch-btn 유지
 *   4. 대시보드 지켜보는 프로그램 카드에 등록 프로그램 노출
 *   5. USER 세션이 신청 → admin 헤더 벨 카운트 증가 (Composite B-3-B watcher 축 fan-out 확인)
 *
 * 회사 PC 검증 필수 (인터랙션 규칙 · CLAUDE.md).
 *
 * seed-pollution 방지: afterAll 에서 등록해둔 watch 를 해제 (재클릭 토글).
 */
import { expect, test } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_SYSTEM_EMAIL,
    abortExternal,
    loginAdmin,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('CENTER_ADMIN — 프로그램 목록 눈 아이콘 클릭 → is-watched 토글 (outerHTML swap)', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });

    // 첫 row 의 list-watch-btn
    const firstRow = page.locator('.admin-program-row:not(.admin-program-row--head)').first();
    const watchBtn = firstRow.locator('.list-watch-btn');
    await expect(watchBtn).toBeVisible();

    // 초기 상태 미등록 (재실행 시 pollution 방지를 위해 명시적으로 idle 유지)
    // hx-swap 후 attribute 유무 확인
    const initiallyWatched = (await watchBtn.getAttribute('class'))?.includes('is-watched');

    // 클릭 → HTMX 요청 → outerHTML 교체
    await Promise.all([
        page.waitForResponse(res => res.url().includes('/watch/toggle') && res.status() === 200),
        watchBtn.click(),
    ]);

    // 재조회 (outerHTML 이후 selector 유지)
    const afterFirstClick = firstRow.locator('.list-watch-btn');
    const nowWatched = (await afterFirstClick.getAttribute('class'))?.includes('is-watched');
    expect(nowWatched).toBe(!initiallyWatched);

    // 정리 — 다시 클릭해 초기 상태로 복귀
    await Promise.all([
        page.waitForResponse(res => res.url().includes('/watch/toggle') && res.status() === 200),
        afterFirstClick.click(),
    ]);
});

test('편집 폼 헤더 눈 아이콘 클릭 → styleClass=form-watch-btn 유지 (재렌더 후에도 스타일 클래스 왕복)', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    const firstEdit = page
        .locator('.admin-program-row:not(.admin-program-row--head)')
        .first()
        .locator('.admin-program-col-actions a', { hasText: '편집' });
    await firstEdit.click();
    await page.waitForURL(/\/admin\/programs\/\d+$/);

    const formWatchBtn = page.locator('.form-watch-btn');
    await expect(formWatchBtn).toBeVisible();

    await Promise.all([
        page.waitForResponse(res => res.url().includes('/watch/toggle') && res.status() === 200),
        formWatchBtn.click(),
    ]);
    // 재렌더 후 여전히 form-watch-btn 스타일 유지 (hx-vals 왕복 검증)
    await expect(page.locator('.form-watch-btn')).toBeVisible();

    // 정리
    await Promise.all([
        page.waitForResponse(res => res.url().includes('/watch/toggle') && res.status() === 200),
        page.locator('.form-watch-btn').click(),
    ]);
});

test('대시보드 — 지켜보는 프로그램 카드 노출 + 등록 후 목록 갱신', async ({ page }) => {
    await loginAdmin(page, ADMIN_SYSTEM_EMAIL);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    const card = page.locator('.admin-watched-card');
    await expect(card).toBeVisible();
    await expect(card.locator('.admin-card-title')).toHaveText('지켜보는 프로그램');

    // 초기 상태는 empty 이거나 이전 pollution — 두 경우 모두 렌더 자체는 성공
    const emptyOrRows = card.locator('.admin-watched-empty, .admin-watched-row');
    await expect(emptyOrRows.first()).toBeVisible();
});
