import { test, expect } from '@playwright/test';
import { abortExternal, login, seedEmail, waitForHtmx } from '../helpers';

/**
 * ★ 즐겨찾기 토글 여정 — 카드·상세 양쪽 + 비로그인 리다이렉트 + 상태 일관성.
 *
 * 마크업:
 *  - 인증: <button class="bookmark-btn ..."> + HTMX POST /bookmarks/programs/{id}/toggle → outerHTML swap
 *  - 비인증: <a class="bookmark-btn ..." href="/login">
 *  - 활성 클래스: is-bookmarked
 *  - 카드: styleClass = card-bookmark-btn / 상세: styleClass = detail-action-icon
 */

const SEED_USER = seedEmail(30);
const PROGRAM_ID = 3;   // 마음건강 힐링 캠프 (6/20, ACTIVE, 시드 유저 seed30 미신청 = 즐겨찾기 초기값 false)

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('비로그인 상태에서 카드 ★ 클릭 시 /login 으로 이동한다', async ({ page }) => {
    // program id=3 은 가장 오래된 시드 그룹에 속함 (2026-07-27 pagination 데모용 15건 추가 후
// 3페이지 중 마지막 페이지). 카드 렌더 확인을 위해 명시적으로 page=2 로 이동.
await page.goto('/programs?page=2', { waitUntil: 'commit' });
    // 비인증 상태에서는 button 이 아닌 <a> 로 렌더되어 href 로 이동
    const firstBookmark = page.locator('a.card-bookmark-btn').first();
    await expect(firstBookmark).toHaveAttribute('href', /\/login/);
    await firstBookmark.click();
    await expect(page).toHaveURL(/\/login/);
});

test('로그인 후 카드 ★ 클릭 시 HTMX 로 is-bookmarked 클래스가 토글된다', async ({ page }) => {
    await login(page, SEED_USER);
    // program id=3 은 가장 오래된 시드 그룹에 속함 (2026-07-27 pagination 데모용 15건 추가 후
// 3페이지 중 마지막 페이지). 카드 렌더 확인을 위해 명시적으로 page=2 로 이동.
await page.goto('/programs?page=2', { waitUntil: 'commit' });
    await waitForHtmx(page);

    // program id=3 카드의 ★ 버튼 (아직 즐겨찾기 안 함 → is-bookmarked 없음)
    const bookmarkBtn = page.locator(`button.card-bookmark-btn[hx-post*="/bookmarks/programs/${PROGRAM_ID}/toggle"]`);
    await expect(bookmarkBtn).not.toHaveClass(/is-bookmarked/);

    await bookmarkBtn.click();
    // HTMX outerHTML swap 완료 대기 — 같은 셀렉터로 재조회 시 is-bookmarked 클래스가 붙어있어야 함
    await expect(page.locator(`button.card-bookmark-btn[hx-post*="/bookmarks/programs/${PROGRAM_ID}/toggle"]`))
        .toHaveClass(/is-bookmarked/, { timeout: 5000 });
});

test('상세 페이지 ★ 클릭 시 detail-action-icon 이 토글된다', async ({ page }) => {
    await login(page, SEED_USER);
    await page.goto(`/programs/${PROGRAM_ID}`, { waitUntil: 'commit' });
    await waitForHtmx(page);

    // 상세는 styleClass="detail-action-icon" 로 렌더
    const detailBookmark = page.locator('button.detail-action-icon.bookmark-btn');
    const initialClasses = await detailBookmark.getAttribute('class') ?? '';
    const wasBookmarked = initialClasses.includes('is-bookmarked');

    await detailBookmark.click();

    // 토글 결과 반전 확인
    const after = page.locator('button.detail-action-icon.bookmark-btn');
    if (wasBookmarked) {
        await expect(after).not.toHaveClass(/is-bookmarked/, { timeout: 5000 });
    } else {
        await expect(after).toHaveClass(/is-bookmarked/, { timeout: 5000 });
    }
});

test('목록에서 토글한 상태가 상세 페이지 진입 시에도 유지된다', async ({ page }) => {
    await login(page, SEED_USER);
    // program id=3 은 가장 오래된 시드 그룹에 속함 (2026-07-27 pagination 데모용 15건 추가 후
// 3페이지 중 마지막 페이지). 카드 렌더 확인을 위해 명시적으로 page=2 로 이동.
await page.goto('/programs?page=2', { waitUntil: 'commit' });
    await waitForHtmx(page);

    // 목록에서 program 3 을 즐겨찾기 (이미 이전 테스트로 켜졌을 수 있으니 상태 확인 후 반전)
    const cardBtn = page.locator(`button.card-bookmark-btn[hx-post*="/bookmarks/programs/${PROGRAM_ID}/toggle"]`);
    const isOn = (await cardBtn.getAttribute('class') ?? '').includes('is-bookmarked');
    if (!isOn) {
        await cardBtn.click();
        await expect(page.locator(`button.card-bookmark-btn[hx-post*="/bookmarks/programs/${PROGRAM_ID}/toggle"]`))
            .toHaveClass(/is-bookmarked/, { timeout: 5000 });
    }

    // 상세 페이지 진입 → 여전히 is-bookmarked
    await page.goto(`/programs/${PROGRAM_ID}`, { waitUntil: 'commit' });
    await expect(page.locator('button.detail-action-icon.bookmark-btn')).toHaveClass(/is-bookmarked/);
});
