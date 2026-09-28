/**
 * A7-e2e-suite (2026-09-28) — admin 헤더 알림 벨 디자인 계약 실행 spec.
 *
 * 순서:
 *   1. sysadmin 로그인 → /admin 진입 → **초기 hidden 상태**로 계약 실행 (dropdown wrapper 만 있고 panel 없음)
 *   2. 벨 클릭 → hx-get /admin/notifications/dropdown 응답 → .admin-notif-panel 렌더 완료 대기
 *   3. **오픈 후 상태**로 계약 재실행 (dropdown.width · panel.shadow · item.* 검사가 이 단계에서 pass)
 *
 * 결과 gap 리포트는 e2e/gap-reports/admin-header-notifications.md 에 저장. 오픈 후 실행은 별도 authState 로 표기.
 *
 * seed 전제:
 *   AdminNotificationEventListener 가 부팅 시 seed application 을 처리하며 sysadmin (createdBy) 축으로
 *   NEW_APPLICATION 알림 여러 건이 생긴다. 최소 1건은 미읽음 상태 유지가 필요 — 이 spec 자체는 mark-all-read 를
 *   호출하지 않으므로 pollution 걱정 없음.
 */
import { test } from '@playwright/test';
import { adminHeaderNotificationsContract } from '../contracts/admin-header-notifications';
import { runContract, writeGapReport } from '../contracts/runner';
import {
    abortExternal,
    applyProgram,
    login,
    loginAdmin,
    resetApplications,
    seedEmail,
} from '../helpers';

/**
 * QA FAIL-2 fix (2026-09-28): DataInitializer 는 seed application 을 applicationRepository.saveAll() 로
 * 직접 저장하여 ApplicationCreatedEvent 가 발행되지 않는다. 결과적으로 sysadmin (createdBy 축) 초기 unread 는 0.
 * 이 spec 은 오픈 후 dropdown 항목 존재를 전제로 계약을 검사하므로 최소 1건 이상의 실 알림이 필요하다.
 * seed30 (미신청 계정) 이 실제 apply 위저드를 완료하도록 하여 sysadmin unread 를 발생시킨다.
 */
test.beforeAll(async ({ browser }) => {
    const ctx = await browser.newContext();
    try {
        const page = await ctx.newPage();
        await abortExternal(page);
        const userEmail = seedEmail(30);
        await login(page, userEmail);
        await resetApplications(page, { userEmail, programId: 1 });
        await applyProgram(page, 1, 'visual-admin-header seed apply');
    } finally {
        await ctx.close();
    }
});

test('admin 헤더 알림 벨 디자인 계약 (초기 hidden + 오픈 후)', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminHeaderNotificationsContract.viewport.width,
        height: adminHeaderNotificationsContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });

    // 초기 상태 계약 실행 (dropdown 항목이 없어 count-min 검사는 자동 갭으로 리포트되지만 soft assert 라 진행)
    const anonInitial = await runContract(page, adminHeaderNotificationsContract, 'anon');

    // 벨 클릭 → dropdown fetch. HTMX 응답이 innerHTML swap 될 때까지 panel 노출 대기.
    await page.locator('.admin-header-bell').click();
    await page.locator('.admin-notif-panel').waitFor({ state: 'visible', timeout: 5000 });

    // 오픈 후 상태 계약 재실행 — dropdown.width · shadow · item.* 검사가 이 단계에서 pass
    const auth = await runContract(page, adminHeaderNotificationsContract, 'auth');

    writeGapReport(adminHeaderNotificationsContract, { anon: anonInitial, auth });
});
