/**
 * A7-realtime (2026-10-15) — admin 알림 SSE 트랜스포트 디자인 계약 실행 spec.
 *
 * 벨 외형 계약 (visual-admin-header-notifications) 과 분리된 이유:
 *   1. 계약 단위 실패를 "트랜스포트" vs "외형" 축으로 분리해 리포트 가독성 확보
 *   2. SSE 는 모든 /admin/** 페이지에 공통 — 외형 계약은 벨 클릭 상태 전환이 있어 다른 생명주기를 가짐
 *
 * 이 spec 은 HTML DOM 선언(hx-ext, sse-connect, sse-swap)만 검사한다.
 * 실제 SSE wire 레벨 동작(이벤트 수신·fragment prepend·flash) 은 chromium 프로젝트의 기능 E2E spec
 * (admin-notifications-sse.spec.ts) 가 담당 — 그쪽은 bootRun + 실 서버 왕복이 필요하다.
 */
import { test } from '@playwright/test';
import { adminNotificationSseContract } from '../contracts/admin-notification-sse';
import { runContract, writeGapReport } from '../contracts/runner';
import { abortExternal, loginAdmin } from '../helpers';

test('admin 알림 SSE 트랜스포트 계약 (hx-ext=sse · sse-connect · sse-swap)', async ({ page }) => {
    await abortExternal(page);
    await page.setViewportSize({
        width: adminNotificationSseContract.viewport.width,
        height: adminNotificationSseContract.viewport.height,
    });
    await loginAdmin(page);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });

    const auth = await runContract(page, adminNotificationSseContract, 'auth');
    writeGapReport(adminNotificationSseContract, { auth });
});
