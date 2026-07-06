import { test, expect } from '@playwright/test';
import { abortExternal, waitForHtmx } from '../helpers';

/**
 * F0g 공지사항 목록·상세 여정 (인증 불필요, permitAll).
 *
 * 시드 데이터 (DataInitializer.seedNotices, 12건):
 *  - pinned 2건 (제1회 청년의 날 축제 / 2026년 상반기 운영 방침) — 상단 고정
 *  - 카테고리 EVENT/NOTICE/OPERATION/ETC 골고루
 *  - 10건/페이지 → 2 페이지 (page 0 = 10건, page 1 = 2건)
 *  - NoticeAttachment 4건 (일부 공지에만 부착)
 *
 * 정렬: ORDER BY pinned DESC, id DESC → 최신 pinned 가 최상단.
 * HTMX: 탭·페이지네이션 클릭 시 outerHTML swap (target=#notice-list-region), URL push.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    await page.goto('/notices', { waitUntil: 'commit' });
    await expect(page).toHaveTitle(/공지사항/);
    await waitForHtmx(page);
});

test('카테고리 pill 탭 5종 (전체 + 4카테고리) 이 렌더되고 초기는 "전체" active 이다', async ({ page }) => {
    const tabs = page.locator('.notice-tab');
    await expect(tabs).toHaveCount(5);
    // 첫 탭은 "전체" 이며 active
    await expect(tabs.first()).toContainText('전체');
    await expect(tabs.first()).toHaveClass(/active/);
    // 나머지 4개는 카테고리 라벨 (행사/공지/운영/기타 — enum label 순서)
    const restText = await tabs.nth(1).innerText() + await tabs.nth(2).innerText()
                   + await tabs.nth(3).innerText() + await tabs.nth(4).innerText();
    expect(restText).toContain('행사');
    expect(restText).toContain('공지');
    expect(restText).toContain('운영');
    expect(restText).toContain('기타');
});

test('pinned 공지 2건이 상단에 표시되고 📌 아이콘·notice-row--pinned 클래스가 붙는다', async ({ page }) => {
    const rows = page.locator('a.notice-row');
    // 시드 12건 중 첫 페이지 10건 렌더
    await expect(rows).toHaveCount(10);

    // 첫 2 행은 pinned (정렬 규칙: pinned DESC → id DESC)
    await expect(rows.nth(0)).toHaveClass(/notice-row--pinned/);
    await expect(rows.nth(1)).toHaveClass(/notice-row--pinned/);
    // 📌 아이콘도 첫 2 행에만 존재
    await expect(rows.nth(0).locator('.notice-pin-icon')).toBeVisible();
    await expect(rows.nth(1).locator('.notice-pin-icon')).toBeVisible();
    // 3번째 행부터는 pinned 없음
    await expect(rows.nth(2)).not.toHaveClass(/notice-row--pinned/);
    await expect(rows.nth(2).locator('.notice-pin-icon')).toHaveCount(0);
});

test('카테고리 탭 "행사" 클릭 시 URL 갱신 + 목록 필터 반영 (HTMX 부분 갱신)', async ({ page }) => {
    // "행사" pill 클릭 (전체=0, 행사=1)
    await page.locator('.notice-tab').nth(1).click();

    // HTMX push-url → URL 에 category=EVENT 반영
    await expect(page).toHaveURL(/category=EVENT/);

    // 남은 목록의 카테고리 뱃지가 전부 "행사" 인지 (렌더된 뱃지가 있으면)
    // 참고: HTMX swap 대상이 #notice-list-region (목록만) 이라 탭 자체의 active 클래스는 이 시점에 재렌더되지 않음.
    //      탭 활성 상태 검증은 페이지 전체 새로고침 후 시나리오로 별도 커버 (UX 후속 티켓).
    const badges = page.locator('a.notice-row .category-badge');
    const count = await badges.count();
    for (let i = 0; i < count; i++) {
        await expect(badges.nth(i)).toContainText('행사');
    }
});

test('직접 URL 진입 시 카테고리 탭 active 상태가 반영된다 (풀 페이지 진입 경로)', async ({ page }) => {
    await page.goto('/notices?category=EVENT', { waitUntil: 'commit' });
    await expect(page.locator('.notice-tab').nth(1)).toHaveClass(/active/);
    await expect(page.locator('.notice-tab').first()).not.toHaveClass(/active/);
});

test('12건 시드에서 페이지네이션이 나타나고 2페이지 클릭 시 URL·active 가 갱신된다', async ({ page }) => {
    // 총 12건 / 페이지 크기 10 → totalPages = 2 → 페이지네이션 노출
    const pagination = page.locator('.notice-pagination');
    await expect(pagination).toBeVisible();

    // 페이지 버튼 (1, 2) 확인
    const pageBtns = pagination.locator('a.page-btn').filter({ hasNotText: '‹' }).filter({ hasNotText: '›' });
    // 실 렌더는 [1] [2] (2개 or 그 이상 그룹)
    await expect(pageBtns.first()).toContainText('1');
    await expect(pageBtns.first()).toHaveClass(/active/);

    // 2 페이지 클릭
    await pageBtns.nth(1).click();
    await expect(page).toHaveURL(/page=1/);
    // 재렌더된 목록: 2 페이지엔 12 - 10 = 2 행
    await expect(page.locator('a.notice-row')).toHaveCount(2);
});

test('첫 공지 클릭 → 상세 페이지 이동 후 제목·본문·메타·목록으로 버튼이 렌더된다', async ({ page }) => {
    const firstRow = page.locator('a.notice-row').first();
    const firstTitle = await firstRow.locator('.notice-title-text').innerText();
    await firstRow.click();

    // URL /notices/{id}
    await expect(page).toHaveURL(/\/notices\/\d+/);

    // 상세 요소
    await expect(page.locator('h1.notice-detail-title')).toContainText(firstTitle);
    await expect(page.locator('.notice-detail-meta')).toContainText('작성일');
    await expect(page.locator('.notice-detail-meta')).toContainText('조회');
    // 본문 (th:utext 로 HTML 렌더) — 시드 본문에 <p> 태그 있음
    await expect(page.locator('.notice-detail-body p').first()).toBeVisible();

    // 목록으로 버튼 (하단 secondary + 상단 back arrow)
    await expect(page.locator('.notice-back-btn')).toBeVisible();
    await expect(page.locator('.notice-detail-actions a.btn-secondary')).toContainText('목록으로');
});

test('상세 페이지 이전글/다음글 네비게이션 — 맨 마지막 공지는 이전글 링크, 다음글 empty', async ({ page }) => {
    // 시드 id 오름차순 상 가장 오래된 공지 (id=1) 로 직접 진입.
    // 정렬은 id DESC 라 목록 마지막 행이 id 가장 낮음. 하지만 pinned 특성 때문에 목록 인덱스에 의존 X.
    // notice/{1} 은 시드 첫 번째 = "제1회 청년의 날 축제 안내" (pinned).
    await page.goto('/notices/1', { waitUntil: 'commit' });

    // id=1 은 가장 오래된 (id 기준 이전 없음) → 이전글 empty 상태
    const adjacentRows = page.locator('.notice-adjacent-row');
    await expect(adjacentRows).toHaveCount(2); // 이전/다음 2행 (empty 도 카운트)

    const prevRow = adjacentRows.first();
    await expect(prevRow).toContainText('이전글');
    await expect(prevRow).toHaveClass(/notice-adjacent-row--empty/);

    // 다음글은 id 더 큰 공지가 있으므로 링크 형태 (<a>)
    const nextRow = adjacentRows.nth(1);
    await expect(nextRow).toContainText('다음글');
    await expect(nextRow).not.toHaveClass(/notice-adjacent-row--empty/);

    // 다음글 클릭 → 다른 상세로 이동
    await nextRow.click();
    await expect(page).toHaveURL(/\/notices\/[2-9]|\/notices\/1[0-2]/);
});
